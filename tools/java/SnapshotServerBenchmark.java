package fpv.net;

import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.content.UnitTypes;
import mindustry.game.Team;
import mindustry.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import static fpv.net.ConnectionNativeHandlersTest.*;

/** Actual private server sender, native units/filter/world queries and native LZ4 submission.
 * Uses existing in-memory fixture/token; not live AI battle, native transport, FPS or link latency. */
public final class SnapshotServerBenchmark {
    static long nativeBytes,fpvBytes;static int packets;
    static final class Con extends NetConnection {
        final ArcNetProvider.PacketSerializer serializer=new ArcNetProvider.PacketSerializer();final ByteBuffer buffer=ByteBuffer.allocate(32768);
        Con(){super("in-memory");}
        public void send(Object p,boolean reliable){try{fpvBytes+=wire(p).length;buffer.clear();serializer.write(buffer,p);nativeBytes+=buffer.position();packets++;}catch(Exception e){throw new RuntimeException(e);}}
        public void close(){}
    }
    public static void main(String[] args)throws Exception{
        Log.logger=(level,message)->{};
        var a=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();a.setThreadAllocatedMemoryEnabled(true);long tid=Thread.currentThread().getId();
        System.out.println("units,recipients,fpvBytes,nativeLz4Bytes,packets,sendMeanMs,sendP95Ms,allocatedBytes,poseQueriesMeasuredOrDerived,cacheReuse");
        for(int n:new int[]{100,1000,4096})for(int peers:new int[]{1,4})try(var f=new Fixture()){
            Groups.unit.clear();flag("active",true);flag("server",true);
            for(int i=0;i<n;i++){var u=(i%2==0?UnitTypes.flare:UnitTypes.dagger).create(Team.sharded);u.set(16+(i%32)*8,16+(i/32%32)*8);u.add();}
            for(int i=0;i<peers;i++){Player p=Player.create();p.team(Team.sharded);p.con=new Con();call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.HELLO,0,i,o->o.writeUTF(f.hash)));}
            for(int i=0;i<20;i++)call(f.net,"sendSnapshots",new Class[]{});
            double[] times=new double[40];long allocation=0;
            for(int i=0;i<40;i++){nativeBytes=fpvBytes=0;packets=0;long before=a.getThreadAllocatedBytes(tid),t=System.nanoTime();call(f.net,"sendSnapshots",new Class[]{});times[i]=(System.nanoTime()-t)/1e6;allocation+=a.getThreadAllocatedBytes(tid)-before;}
            long queries=n*(long)peers,reuse=0;
            long measuredNative=nativeBytes,measuredFpv=fpvBytes;int measuredPackets=packets;
            try{
                var d=((NetworkDiagnosticSession)get(f.net,"diagnostic")).data;d.enabled(true);call(f.net,"sendSnapshots",new Class[]{});
                queries=d.value(NetworkDiagnostics.Counter.valueOf("POSE_QUERIES"));reuse=d.value(NetworkDiagnostics.Counter.valueOf("POSE_QUERY_REUSE"));d.enabled(false);
            }catch(IllegalArgumentException absentInBaseline){} // Baseline query count derived from its unmodified all-visible loop.
            double mean=Arrays.stream(times).average().orElse(0);Arrays.sort(times);
            System.out.printf(Locale.ROOT,"%d,%d,%d,%d,%d,%.6f,%.6f,%d,%d,%d%n",n,peers,measuredFpv,measuredNative,measuredPackets,mean,times[37],allocation/40,queries,reuse);
        }
    }
}
