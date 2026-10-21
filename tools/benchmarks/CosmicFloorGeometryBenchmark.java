package fpv.benchmarks;
import fpv.render.CosmicFloorGeometry;
import java.lang.management.ManagementFactory;
import java.util.function.IntBinaryOperator;
/** Cold CPU mask construction only. Not per-frame cost, GPU time or FPS. Run with full JDK17. */
public final class CosmicFloorGeometryBenchmark {
 static volatile float[] sink;
 public static void main(String[] args){
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().getId();
  for(String name:new String[]{"solid","space","mixed","checker"}){
   IntBinaryOperator mask=switch(name){case "solid"->(x,y)->0;case "space"->(x,y)->1;case "mixed"->(x,y)->x>=16?1:0;default->(x,y)->(x+y)%2;};
   for(int i=0;i<500;i++)sink=CosmicFloorGeometry.build(0,0,32,32,mask);
   long allocated=bean.getThreadAllocatedBytes(id),start=System.nanoTime();
   for(int i=0;i<2000;i++)sink=CosmicFloorGeometry.build(0,0,32,32,mask);
   double us=(System.nanoTime()-start)/2000.0/1000;long bytes=(bean.getThreadAllocatedBytes(id)-allocated)/2000;
   System.out.println("COSMIC,"+name+",coldChunkMeanUs="+us+",allocatedBytes="+bytes+",vertices="+sink.length/3+",triangles="+sink.length/9+",meshBytes="+sink.length*4+",draws="+(sink.length==0?0:1));
  }
 }
}
