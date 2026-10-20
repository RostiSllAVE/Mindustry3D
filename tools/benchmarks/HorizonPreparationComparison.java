package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;import fpv.render.*;import mindustry.*;import mindustry.content.*;import mindustry.world.*;import java.lang.reflect.*;import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
/** Common compiled baseline/candidate preparation probe; native Mesh buffers through MockGL. */
public final class HorizonPreparationComparison {
 static class Stub extends Texture {Stub(){super();width=height=64;}}
 static Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static long allocation(){try{Object b=Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null);return (long)Class.forName("com.sun.management.ThreadMXBean").getMethod("getThreadAllocatedBytes",long.class).invoke(b,Thread.currentThread().getId());}catch(Exception e){return -1;}}
 static Object stat(Object stats,String field)throws Exception{return stats.getClass().getMethod(field).invoke(stats);}
 public static void main(String[] args)throws Exception{init();arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();try{
  var region=new TextureAtlas.AtlasRegion(new Stub(),0,0,16,16);Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return region;}};
  for(var b:new Block[]{Blocks.water,Blocks.stone,Blocks.grass,Blocks.sand,Blocks.snow,Blocks.pine,Blocks.stoneWall}){b.region=region;b.variantRegions=new TextureRegion[]{region};}
  for(int size:new int[]{64,512})for(String scene:new String[]{"ocean","mixed"}){
   Vars.world.tiles=new Tiles(size,size);Vars.world.tiles.fill();Vars.world.setGenerating(true);
   for(var t:Vars.world.tiles){t.setFloor(scene.equals("ocean")?Blocks.water.asFloor():Blocks.stone.asFloor());if(scene.equals("mixed")){if(t.y<8)t.setFloor(Blocks.sand.asFloor());else if(t.y>=size-8)t.setFloor(Blocks.water.asFloor());else if(t.x<8)t.setBlock(Blocks.stoneWall);else if(t.x>=size-8){t.setFloor(Blocks.grass.asFloor());t.setBlock(Blocks.pine);}}}
   FpvWorldRenderer draw=new FpvWorldRenderer();draw.terrain().ensureWorld();Object env=draw.environment(),auto=null;Method ensure,invalidate=null;
   try{auto=env.getClass().getMethod("automaticHorizon").invoke(env);ensure=auto.getClass().getMethod("ensure");invalidate=auto.getClass().getMethod("invalidate");}catch(NoSuchMethodException old){ensure=env.getClass().getDeclaredMethod("rebuildMountains");ensure.setAccessible(true);}
   Object receiver=auto==null?env:auto;double[] ms=new double[7],alloc=new double[7];long bytes=0,vertices=0,profileHash=0,geometryHash=0;int batches=0;double cold=0;
   for(int i=-3;i<7;i++){if(invalidate!=null)invalidate.invoke(auto);long a=allocation(),start=System.nanoTime();ensure.invoke(receiver);double elapsed=(System.nanoTime()-start)/1e6;long end=allocation();if(i==-3)cold=elapsed;if(i>=0){ms[i]=elapsed;alloc[i]=a<0||end<0?-1:end-a;}}
   if(auto!=null){Object s=auto.getClass().getMethod("stats").invoke(auto);bytes=(long)stat(s,"meshBytes");vertices=(int)stat(s,"vertices");geometryHash=(long)stat(s,"fingerprint");Object p=auto.getClass().getMethod("profile").invoke(auto);profileHash=(long)p.getClass().getMethod("fingerprint").invoke(p);Object collar=get(auto,"collar");batches=((List<?>)get(collar,"parts")).size()+(get(auto,"far")==null?0:1);}else{Mesh m=(Mesh)get(env,"mountains");vertices=m.getNumVertices();bytes=vertices*28;batches=1;}
   long t=System.nanoTime();if(auto!=null)for(int i=0;i<10000;i++)ensure.invoke(auto);double cache=auto==null?0:(System.nanoTime()-t)/1e6/10000;
   Arrays.sort(ms);Arrays.sort(alloc);System.out.printf(Locale.ROOT,"HORIZON,%d,%s,medianMs=%.6f,minMs=%.6f,maxMs=%.6f,coldMs=%.6f,allocatedBytes=%.0f,meshBytes=%d,vertices=%d,triangles=%d,materialBatches=%d,cacheHitMs=%.6f,profileFingerprint=%d,geometryFingerprint=%d%n",size,scene,ms[3],ms[0],ms[6],cold,alloc[3],bytes,vertices,vertices/3,batches,cache,profileHash,geometryHash);draw.dispose();
  }
 }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}}
}
