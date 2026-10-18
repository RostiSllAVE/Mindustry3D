import fpv.net.*;
import fpv.world.Unit3DState;
import java.util.*;

/** Developer-only codec CPU/allocation benchmark. No socket, real latency or gameplay FPS claim. */
public final class NetworkCodecBenchmark {
    public static void main(String[] args){
        var bean=java.lang.management.ManagementFactory.getThreadMXBean();
        com.sun.management.ThreadMXBean allocation=bean instanceof com.sun.management.ThreadMXBean b&&b.isThreadAllocatedMemorySupported()?b:null;
        if(allocation!=null)allocation.setThreadAllocatedMemoryEnabled(true);
        System.out.println("units,bytes,chunks,encodeMeanMs,encodeP95Ms,decodeMeanMs,decodeP95Ms,allocatedBytesPerEncodeDecode");
        for(int n:new int[]{0,100,1000,4096,16384}){
            var units=new ArrayList<Unit3DState>();for(int i=0;i<n;i++)units.add(new Unit3DState(i,1,1,10,20,4,0,0,0,0,0,0,4,.5,.2,true));
            var state=new NetProtocol.Snapshot(units,null);
            for(int i=0;i<10;i++)NetProtocol.snapshot(NetProtocol.snapshot(state));
            double[] encode=new double[30],decode=new double[30];long bytes=0,tid=Thread.currentThread().getId();int length=0;
            for(int i=0;i<30;i++){
                long allocated=allocation==null?0:allocation.getThreadAllocatedBytes(tid),start=System.nanoTime();
                byte[] wire=NetProtocol.snapshot(state);encode[i]=(System.nanoTime()-start)/1e6;length=wire.length;
                start=System.nanoTime();var restored=NetProtocol.snapshot(wire);decode[i]=(System.nanoTime()-start)/1e6;
                if(restored.units().size()!=n)throw new AssertionError("Invalid benchmark decode");
                if(allocation!=null)bytes+=allocation.getThreadAllocatedBytes(tid)-allocated;
            }
            System.out.printf(java.util.Locale.ROOT,"%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%d%n",n,length,Math.max(1,(length+11999)/12000),mean(encode),p95(encode),mean(decode),p95(decode),allocation==null?-1:bytes/30);
        }
    }
    static double mean(double[] a){return Arrays.stream(a).average().orElse(0);}
    static double p95(double[] a){double[] b=a.clone();Arrays.sort(b);return b[(int)Math.ceil(b.length*.95)-1];}
}
