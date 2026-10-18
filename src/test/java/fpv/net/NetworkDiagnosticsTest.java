package fpv.net;

import arc.util.serialization.Jval;
import static fpv.net.NetworkDiagnostics.Metric.*;
import static fpv.net.NetworkDiagnostics.Counter.*;

/** Bounded collection, disabled mode, fixed export vocabulary and actual reassembly accounting. */
public final class NetworkDiagnosticsTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        var d=new NetworkDiagnostics();d.sample(FRAME_INTERVAL_MS,999);d.packet(true,true,100);
        check(d.value(TX_PACKETS)==0,"disabled packet collection is inert");
        check(Jval.read(d.json(1,0,false,3)).get("metrics").asObject().size==0,"disabled collection allocates no sample series");
        d.enabled(true);for(int i=1;i<=1024;i++)d.sample(FRAME_INTERVAL_MS,i);
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1})d.sample(FRAME_INTERVAL_MS,bad);
        d.packet(true,true,100);d.packet(true,false,20);d.packet(false,true,200);
        d.channel("poses",true,100);d.channel("private@example.com C:\\secret\\save 192.0.2.1",false,12);
        Jval report=Jval.read(d.json(2,1,true,3)),series=report.get("metrics").get("FRAME_INTERVAL_MS");
        check(series.getInt("count",0)==1024,"invalid samples excluded");
        check(series.getDouble("mean",0)==512.5,"cumulative mean");
        check(series.getDouble("p50",0)==768&&series.getDouble("p95",0)==999,"percentiles use bounded most recent window");
        check(series.getDouble("max",0)==1024,"cumulative maximum");
        check(d.value(TX_PACKETS)==2&&d.value(TX_PAYLOAD_BYTES)==120&&d.value(RX_PACKETS)==1,"directional payload counters");
        check(d.value(TX_RELIABLE)==1&&d.value(TX_UNRELIABLE)==1,"outgoing transport categories");
        String json=d.json(2,1,true,3);check(!json.contains("secret")&&!json.contains("192.0.2.1")&&!json.contains("@"),"untrusted channel contents cannot leak");
        check(report.get("channels").has("OTHER"),"unknown names collapse to fixed key");
        d.enabled(false);d.count(TX_PACKETS);check(d.value(TX_PACKETS)==2,"stop is inert");
        var a=new NetProtocol.Assembly();byte[] first=new byte[12000],last=new byte[3];
        check(a.pendingBytes()==0,"initial retained bytes");
        a.accept(new NetProtocol.Chunk("poses",1,0,2,12003,first),1);check(a.pendingBytes()==12000,"partial bytes counted");
        a.accept(new NetProtocol.Chunk("poses",1,0,2,12003,first),1.1);check(a.pendingBytes()==12000,"duplicate does not inflate memory");
        check(a.accept(new NetProtocol.Chunk("poses",1,1,2,12003,last),1.2).length==12003&&a.pendingBytes()==0,"complete frees accounting");
        a.accept(new NetProtocol.Chunk("poses",2,0,2,12003,first),2);
        a.accept(new NetProtocol.Chunk("poses",3,1,2,12003,last),2.1);check(a.pendingBytes()==3,"new revision replaces retained old bytes");
        System.out.println("NetworkDiagnosticsTest: "+checks+" checks PASS");
    }
}
