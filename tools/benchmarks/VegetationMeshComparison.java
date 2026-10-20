package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;import fpv.render.FpvWorldRenderer;import mindustry.*;import mindustry.content.*;import mindustry.world.*;import java.lang.reflect.*;import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
/** Sequential A2-13b/A2-14 native chunk benchmark; MockGL, not FPS. Warm caches and bounded source maps. */
public class VegetationMeshComparison {
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static class TextureStub extends Texture{TextureStub(){super();width=256;height=256;}}
 static java.lang.reflect.Field liquidField,deferredField;
 static long allocated(){try{Object bean=Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null);return (long)Class.forName("com.sun.management.ThreadMXBean").getMethod("getThreadAllocatedBytes",long.class).invoke(bean,Thread.currentThread().getId());}catch(Exception e){return -1;}}
 public static void main(String[] args)throws Exception{
  arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();init();
  try{liquidField=Class.forName("fpv.render.FpvWorldRenderer$ChunkMesh").getDeclaredField("liquid");liquidField.setAccessible(true);}catch(NoSuchFieldException oldVersion){}
  try{deferredField=FpvWorldRenderer.class.getDeclaredField("deferredLiquid");deferredField.setAccessible(true);}catch(NoSuchFieldException oldVersion){}
  var pix=new Pixmap(16,16);pix.fill(0x67aa88ff);
  Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return new AtlasRegion();}@Override public PixmapRegion getPixmap(AtlasRegion r){return new PixmapRegion(pix);}};
  try{for(String scenario:new String[]{"forest","mixed","rocks"}){
   Vars.world.tiles=new Tiles(64,64);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(var t:Vars.world.tiles)t.setFloor(Blocks.grass.asFloor());
   var r=new TextureAtlas.AtlasRegion(new TextureStub(),32,48,16,16);r.name="vegetation-probe";Blocks.grass.region=r;Blocks.grass.variantRegions=new TextureRegion[]{r};
   mindustry.world.Block[] flora={Blocks.pine,Blocks.snowPine,Blocks.whiteTree,Blocks.shrubs,Blocks.sporeCluster,Blocks.redweed,Blocks.yellowCoral,Blocks.redDiamondWall};
   for(var f:flora){f.region=r;f.variantRegions=new TextureRegion[]{r};}Blocks.boulder.region=r;Blocks.boulder.variantRegions=new TextureRegion[]{r};
   int oreCount=0;for(var t:Vars.world.tiles){if((t.x+t.y)%5!=0){t.setBlock(scenario.equals("forest")?Blocks.pine:scenario.equals("rocks")?Blocks.boulder:flora[(t.x*37+t.y*17)%flora.length]);oreCount++;}}
   var draw=new FpvWorldRenderer();set(draw,"worldWidth",64);set(draw,"worldHeight",64);draw.terrain().ensureWorld();
   Method build=draw.getClass().getDeclaredMethod("buildChunk",int.class,int.class);build.setAccessible(true);
   Method coarse=draw.getClass().getDeclaredMethod("coarseChunk",int.class,int.class,int.class);coarse.setAccessible(true);
   Object dynamic=get(draw,"dynamic"),stream=get(draw,"streamAtlas");var clear=dynamic.getClass().getDeclaredMethod("clear");clear.setAccessible(true);var clearAtlas=stream.getClass().getDeclaredMethod("clear");clearAtlas.setAccessible(true);
   double[] times=new double[7],allocs=new double[7],coarseTimes=new double[7];long meshBytes=0,vertexCount=0;int batches=0;double coldMs=0;
   for(int pass=-5;pass<7;pass++){
    long a=allocated(),start=System.nanoTime();long bytes=0,vertices=0;int batch=0;
    for(int y=0;y<4;y++)for(int x=0;x<4;x++){
     Object mesh=build.invoke(draw,x,y);bytes+=(long)get(mesh,"bytes");Object atlas=get(mesh,"atlas");var parts=(List<?>)get(atlas,"parts");batch+=parts.size()+(get(mesh,"mesh")==null?0:1);
     if(get(mesh,"mesh") instanceof Mesh plain)vertices+=plain.getNumVertices();
     for(Object part:parts)vertices+=((Mesh)get(part,"mesh")).getNumVertices();
     if(liquidField!=null){Object liquid=liquidField.get(mesh);if(liquid!=null){var waterParts=(List<?>)get(liquid,"parts");batch+=waterParts.size();for(Object part:waterParts)vertices+=((Mesh)get(part,"mesh")).getNumVertices();}}
     Method dispose=mesh.getClass().getDeclaredMethod("dispose");dispose.setAccessible(true);dispose.invoke(mesh);
    }
    double ms=(System.nanoTime()-start)/1e6;if(pass==-5)coldMs=ms;long end=allocated();
    if(pass>=0){times[pass]=ms;allocs[pass]=a<0||end<0?-1:(end-a)/16.0;meshBytes=bytes;vertexCount=vertices;batches=batch;}
    clear.invoke(dynamic);clearAtlas.invoke(stream);start=System.nanoTime();for(int y=0;y<4;y++)for(int x=0;x<4;x++){coarse.invoke(draw,x,y,4);clear.invoke(dynamic);clearAtlas.invoke(stream);if(deferredField!=null)((arc.struct.IntSeq)deferredField.get(draw)).clear();}if(pass>=0)coarseTimes[pass]=(System.nanoTime()-start)/1e6;
   }
   Arrays.sort(times);Arrays.sort(allocs);Arrays.sort(coarseTimes);
   System.out.printf(Locale.ROOT,"MESH,%s,chunks=16,tiles=4096,props=%d,medianMs=%.6f,minMs=%.6f,maxMs=%.6f,perChunkMs=%.6f,allocatedBytesPerChunk=%.1f,meshBytes=%d,vertices=%d,triangles=%d,materialBatches=%d,coarse16MedianMs=%.6f,coldMs=%.6f%n",scenario,oreCount,times[3],times[0],times[6],times[3]/16,allocs[3],meshBytes,vertexCount,vertexCount/3,batches,coarseTimes[3],coldMs);
   if(scenario.equals("rocks")){var crc=new java.util.zip.CRC32();for(int y=0;y<4;y++)for(int x=0;x<4;x++){Object cm=build.invoke(draw,x,y);for(Object part:(List<?>)get(get(cm,"atlas"),"parts")){Mesh mesh=(Mesh)get(part,"mesh");var data=mesh.getVerticesBuffer();for(int i=0;i<data.limit();i++){int bits=Float.floatToIntBits(data.get(i));for(int k=0;k<4;k++)crc.update(bits>>>(k*8));}}Method dispose=cm.getClass().getDeclaredMethod("dispose");dispose.setAccessible(true);dispose.invoke(cm);}System.out.println("ROCK_MESH_CRC32,"+Long.toHexString(crc.getValue()));}draw.dispose();
  }}finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
 }
}
