import fpv.net.*;
import mindustry.Vars;
import mindustry.net.Net;
import mindustry.gen.ClientBinaryPacketReliableCallPacket;
import java.nio.ByteBuffer;
import java.util.Locale;

/** Native serialized packets delivered at a deterministic service rate, no socket or sleeping. */
public final class SlowAssemblyProbe {
    public static void main(String[] args){
        Vars.net=new Net(null);
        System.out.println("mode,units,bytesPerSecond,frameCompleted,frameDeliverySeconds,bitExactIfCompleted");
        for(boolean compact:new boolean[]{false,true})for(int n:new int[]{100,1000,4096})for(int rate:new int[]{32000,125000,500000}){
            var expected=SnapshotTrafficBenchmark.scene(n,"moving");
            var packets=SnapshotTrafficBenchmark.packets(SnapshotTrafficBenchmark.encode(expected,compact));
            var assembly=new NetProtocol.Assembly();double now=.125;byte[] complete=null;
            for(byte[] b:packets.nativePackets()){
                now+=b.length/(double)rate;
                var p=(ClientBinaryPacketReliableCallPacket)SnapshotTrafficBenchmark.serializer.read(ByteBuffer.wrap(b));p.handled();
                byte[] frame=assembly.accept(NetProtocol.chunk(NetProtocol.decode(p.contents).payload()),now);
                if(frame!=null)complete=frame;
            }
            boolean exact=complete!=null&&SnapshotTrafficBenchmark.decode(complete,compact).units().equals(expected.units());
            if(complete!=null&&!exact)throw new AssertionError("Corrupt modeled frame");
            System.out.printf(Locale.ROOT,"%s,%d,%d,%s,%.6f,%s%n",compact?"after":"before",n,rate,complete!=null,now-.125,exact);
        }
    }
}
