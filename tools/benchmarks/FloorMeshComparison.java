package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;import fpv.render.FpvWorldRenderer;import mindustry.*;import mindustry.content.*;import mindustry.world.*;import java.lang.reflect.*;import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
/** Identical probe runs against A2-10 and A2-11; mock GL, actual native buffers, no GPU/FPS claim. */
public class FloorMeshComparison {
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static class TextureStub extends Texture{TextureStub(){super();width=256;height=256;}}
 static long allocated(){try{Object bean=Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null);return (long)Class.forName("com.sun.management.ThreadMXBean").getMethod("getThreadAllocatedBytes",long.class).invoke(bean,Thread.currentThread().getId());}catch(Exception e){return -1;}}
 public static void main(String[] args)throws Exception{
  arc.util.ArcNativesLoader.load();init();Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return new AtlasRegion();}};
  try{for(boolean textured:new boolean[]{false,true}){
   Vars.world.tiles=new Tiles(128,128);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(var t:Vars.world.tiles)t.setFloor(Blocks.stone.asFloor());
   if(textured){var r=new TextureRegion(new TextureStub(),32,48,32,32);Blocks.stone.region=r;Blocks.stone.variantRegions=new TextureRegion[]{r};}
   var draw=new FpvWorldRenderer();set(draw,"worldWidth",128);set(draw,"worldHeight",128);draw.terrain().ensureWorld();
   Method build=draw.getClass().getDeclaredMethod("buildChunk",int.class,int.class);build.setAccessible(true);
   Method coarse=draw.getClass().getDeclaredMethod("coarseChunk",int.class,int.class,int.class);coarse.setAccessible(true);
   Object dynamic=get(draw,"dynamic"),stream=get(draw,"streamAtlas");var clear=dynamic.getClass().getDeclaredMethod("clear");clear.setAccessible(true);var clearAtlas=stream.getClass().getDeclaredMethod("clear");clearAtlas.setAccessible(true);
   double[] times=new double[7],allocs=new double[7],coarseTimes=new double[7];long meshBytes=0,vertexCount=0;int batches=0;
   for(int pass=-5;pass<7;pass++){
    long a=allocated(),start=System.nanoTime();long bytes=0,vertices=0;int batch=0;
    for(int y=0;y<8;y++)for(int x=0;x<8;x++){
     Object mesh=build.invoke(draw,x,y);bytes+=(long)get(mesh,"bytes");Object atlas=get(mesh,"atlas");var parts=(List<?>)get(atlas,"parts");batch+=parts.size()+(get(mesh,"mesh")==null?0:1);
     if(get(mesh,"mesh") instanceof Mesh plain)vertices+=plain.getNumVertices();
     for(Object part:parts)vertices+=((Mesh)get(part,"mesh")).getNumVertices();
     Method dispose=mesh.getClass().getDeclaredMethod("dispose");dispose.setAccessible(true);dispose.invoke(mesh);
    }
    double ms=(System.nanoTime()-start)/1e6;long end=allocated();
    if(pass>=0){times[pass]=ms;allocs[pass]=a<0||end<0?-1:(end-a)/64.0;meshBytes=bytes;vertexCount=vertices;batches=batch;}
    clear.invoke(dynamic);clearAtlas.invoke(stream);start=System.nanoTime();for(int y=0;y<8;y++)for(int x=0;x<8;x++)coarse.invoke(draw,x,y,4);if(pass>=0)coarseTimes[pass]=(System.nanoTime()-start)/1e6;
   }
   Arrays.sort(times);Arrays.sort(allocs);Arrays.sort(coarseTimes);
   System.out.printf(Locale.ROOT,"MESH,%s,chunks=64,tiles=16384,medianMs=%.6f,minMs=%.6f,maxMs=%.6f,perChunkMs=%.6f,allocatedBytesPerChunk=%.1f,meshBytes=%d,vertices=%d,triangles=%d,materialBatches=%d,coarse64MedianMs=%.6f%n",textured?"atlas":"fallback",times[3],times[0],times[6],times[3]/64,allocs[3],meshBytes,vertexCount,vertexCount/3,batches,coarseTimes[3]);draw.dispose();
  }}finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
 }
}
