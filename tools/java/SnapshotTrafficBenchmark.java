import fpv.net.*;
import fpv.world.Unit3DState;
import fpv.flight.FlightController;
import fpv.generated.FlightProfiles;
import mindustry.Vars;
import mindustry.net.*;
import mindustry.gen.ClientBinaryPacketReliableCallPacket;
import java.nio.ByteBuffer;
import java.util.*;

/** Developer-only actual codec/native LZ4/apply benchmark. No socket or measured link latency. */
public final class SnapshotTrafficBenchmark {
    static final ArcNetProvider.PacketSerializer serializer=new ArcNetProvider.PacketSerializer();
    static final ByteBuffer nativeBuffer=ByteBuffer.allocate(32768);
    static final com.sun.management.ThreadMXBean allocations=allocationBean();
    static com.sun.management.ThreadMXBean allocationBean(){
        var b=java.lang.management.ManagementFactory.getThreadMXBean();
        if(b instanceof com.sun.management.ThreadMXBean a&&a.isThreadAllocatedMemorySupported()){a.setThreadAllocatedMemoryEnabled(true);return a;}return null;
    }
    public static NetProtocol.Snapshot scene(int n,String scene){
        var rand=new Random(2404+n);var poses=new ArrayList<Unit3DState>(n);
        for(int i=0;i<n;i++){
            if(scene.equals("entropy")){
                double[] v=new double[12];for(int j=0;j<12;j++)v[j]=rand.nextDouble()*100;
                poses.add(new Unit3DState(i+1000,i%40,i%3,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],v[10],v[11],true));continue;
            }
            boolean moving=scene.equals("moving"),dense=scene.equals("dense");
            double x=(float)(rand.nextFloat()*900),y=(float)(rand.nextFloat()*900),z=i%2==0?4+Math.sin(i*.37):.35;
            poses.add(new Unit3DState(i+1000,i%40,i%3,x,y,z,
                moving?(float)(rand.nextFloat()*12-6):dense?rand.nextDouble():0,
                moving?(float)(rand.nextFloat()*12-6):dense?rand.nextDouble():0,
                moving&&i%2==0?Math.cos(i*.17)*.8:dense?rand.nextDouble():0,
                (float)(rand.nextFloat()*360),moving&&i%2==0?Math.sin(i*.13)*12:0,
                moving&&i%2==0?Math.cos(i*.23)*14:0,z,.3+(i%8)*.125,.12+(i%5)*.04,i%2==0));
        }
        var pilot=new FlightController(FlightProfiles.create());pilot.reset(100,4,-100,30);
        return new NetProtocol.Snapshot(poses,new NetProtocol.Flight(1,1000,pilot.profile(),pilot.state()));
    }
    static byte[] encode(NetProtocol.Snapshot s,boolean compact){
        if(!compact)return NetProtocol.snapshot(s);
        return PoseSnapshotCodec.encode(s);
    }
    static NetProtocol.Snapshot decode(byte[] b,boolean compact){
        if(!compact)return NetProtocol.snapshot(b);
        return PoseSnapshotCodec.decode(b);
    }
    record Packets(List<byte[]> nativePackets,int fpvBytes){}
    static Packets packets(byte[] data){
        int count=Math.max(1,(data.length+11999)/12000),fpv=0;var result=new ArrayList<byte[]>(count);
        for(int i=0;i<count;i++){
            int offset=i*12000;var c=new NetProtocol.Chunk("poses",1,i,count,data.length,Arrays.copyOfRange(data,offset,Math.min(data.length,offset+12000)));
            byte[] envelope=NetProtocol.encode(NetProtocol.CHANNEL,1,i,out->NetProtocol.chunk(out,c));fpv+=envelope.length;
            var packet=new ClientBinaryPacketReliableCallPacket();packet.type=FpvNetwork.PACKET;packet.contents=envelope;
            nativeBuffer.clear();serializer.write(nativeBuffer,packet);result.add(Arrays.copyOf(nativeBuffer.array(),nativeBuffer.position()));
        }return new Packets(result,fpv);
    }
    static byte[] reassemble(Packets packets){
        var assembly=new NetProtocol.Assembly();byte[] full=null;
        for(byte[] b:packets.nativePackets){
            var p=(ClientBinaryPacketReliableCallPacket)serializer.read(ByteBuffer.wrap(b));p.handled();
            full=assembly.accept(NetProtocol.chunk(NetProtocol.decode(p.contents).payload()),1);
        }if(full==null)throw new AssertionError("Incomplete benchmark snapshot");return full;
    }
    public static void main(String[] args){
        Vars.net=new Net(null);boolean compact=args.length>0&&args[0].equals("compact");
        System.out.println("mode,scene,units,logicalBytes,chunks,fpvBytes,nativeLz4Bytes,encodeMeanMs,encodeP95Ms,chunkNativeMeanMs,receiveDecodeMeanMs,applyMeanMs,allocatedBytesTotal");
        for(String scene:new String[]{"idle","moving","dense","entropy"})for(int n:new int[]{100,1000,4096}){
            var s=scene(n,scene);var remote=new RemotePoses();
            for(int i=0;i<20;i++)remote.accept(decode(reassemble(packets(encode(s,compact))),compact),i*.1);
            double[] enc=new double[40],chunk=new double[40],dec=new double[40],apply=new double[40];long allocated=0;
            int logical=0,fpv=0,nativeBytes=0,chunks=0;long tid=Thread.currentThread().getId();
            for(int i=0;i<40;i++){
                long before=allocations==null?0:allocations.getThreadAllocatedBytes(tid),t=System.nanoTime();
                byte[] b=encode(s,compact);enc[i]=(System.nanoTime()-t)/1e6;logical=b.length;t=System.nanoTime();
                var p=packets(b);chunk[i]=(System.nanoTime()-t)/1e6;fpv=p.fpvBytes;chunks=p.nativePackets.size();nativeBytes=p.nativePackets.stream().mapToInt(x->x.length).sum();t=System.nanoTime();
                var restored=decode(reassemble(p),compact);dec[i]=(System.nanoTime()-t)/1e6;t=System.nanoTime();
                remote.accept(restored,i*.1+10);apply[i]=(System.nanoTime()-t)/1e6;
                if(!s.units().equals(restored.units()))throw new AssertionError("Precision/count mismatch");
                if(allocations!=null)allocated+=allocations.getThreadAllocatedBytes(tid)-before;
            }
            System.out.printf(Locale.ROOT,"%s,%s,%d,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%d%n",compact?"compact":"legacy",scene,n,logical,chunks,fpv,nativeBytes,mean(enc),p95(enc),mean(chunk),mean(dec),mean(apply),allocations==null?-1:allocated/40);
        }
    }
    static double mean(double[] a){return Arrays.stream(a).average().orElse(0);}
    static double p95(double[] a){double[] b=a.clone();Arrays.sort(b);return b[(int)Math.ceil(b.length*.95)-1];}
}
