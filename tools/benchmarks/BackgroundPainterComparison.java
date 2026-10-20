package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;import fpv.render.*;import fpv.background.*;import mindustry.*;import mindustry.content.*;import mindustry.world.*;import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Developer-only CPU/memory probe. MockGL mesh uploads, no FPS or physical gameplay claim. */
public final class BackgroundPainterComparison {
    static class Stub extends Texture{Stub(){super();width=height=64;}}
    static long allocated(){try{Object bean=Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null);return (long)Class.forName("com.sun.management.ThreadMXBean").getMethod("getThreadAllocatedBytes",long.class).invoke(bean,Thread.currentThread().getId());}catch(Exception unavailable){return -1;}}
    static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    public static void main(String[] args)throws Exception{init();arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();try{
        var region=new TextureAtlas.AtlasRegion(new Stub(),0,0,16,16);Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return region;}};for(var b:new Block[]{Blocks.water,Blocks.stone,Blocks.grass,Blocks.sand,Blocks.snow,Blocks.pine,Blocks.stoneWall}){b.region=region;b.variantRegions=new TextureRegion[]{region};}
        for(int size:new int[]{96,512})for(String mode:new String[]{"auto","ocean","forest","custom","large"}){
            Vars.world.tiles=new Tiles(size,size);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(var tile:Vars.world.tiles)tile.setFloor(Blocks.water.asFloor());
            var style=new BackgroundStyle();if(!mode.equals("auto")){style.mode=BackgroundStyle.Mode.MANUAL;style.whole=mode.equals("forest")?BackgroundPreset.FOREST:BackgroundPreset.OCEAN;}
            if(mode.equals("custom")||mode.equals("large")){style.mode=BackgroundStyle.Mode.CUSTOM;style.customBaseManual=true;var paint=style.paint(size,size);paint.line(size*.5,size+100,size*.5,size+180,70,.8,BackgroundPaint.Brush.ISLAND,14);paint.line(size*.5,size+100,size*.5,size+180,50,.7,BackgroundPaint.Brush.FOREST,0);paint.line(-120,20,-120,160,80,.7,BackgroundPaint.Brush.MOUNTAINS,90);}
            if(mode.equals("large")){for(int y=-1800;y<size+1800;y+=256){style.paint.stroke(BackgroundPaint.Brush.FOREST,512,.7,.8,0,(x,z)->0).line(-1800,y,size+1800,y);style.paint.stroke(BackgroundPaint.Brush.RAISE,512,.6,.5,60,(x,z)->0).line(-1800,y,size+1800,y);}}
            BackgroundProfiles.apply(style);var draw=new FpvWorldRenderer();draw.terrain().ensureWorld();var horizon=draw.environment().automaticHorizon();horizon.ensure();
            double[] times=new double[7],allocations=new double[7];for(int i=-3;i<7;i++){horizon.invalidate();long before=allocated(),start=System.nanoTime();horizon.ensure();long end=allocated();if(i>=0){times[i]=(System.nanoTime()-start)/1e6;allocations[i]=before<0||end<0?-1:end-before;}}
            Arrays.sort(times);Arrays.sort(allocations);long start=System.nanoTime();for(int i=0;i<10000;i++)horizon.ensure();double hit=(System.nanoTime()-start)/1e6/10000;var stats=horizon.stats();var collar=get(horizon,"collar");int batches=((List<?>)get(collar,"parts")).size()+(get(horizon,"far")==null?0:1);
            System.out.printf(Locale.ROOT,"PAINTER_MESH,%d,%s,medianMs=%.6f,minMs=%.6f,maxMs=%.6f,allocatedBytes=%.0f,meshBytes=%d,vertices=%d,triangles=%d,materialBatches=%d,cacheHitMs=%.6f%n",size,mode,times[3],times[0],times[6],allocations[3],stats.meshBytes(),stats.vertices(),stats.vertices()/3,batches,hit);draw.dispose();
        }
        var paint=new BackgroundPaint(96,64);var history=new BackgroundPaintHistory(paint);double[] stroke=new double[300];long before=allocated();for(int i=0;i<stroke.length;i++){long start=System.nanoTime();history.begin();paint.line(-200,20,-80,70,32,.7,i%2==0?BackgroundPaint.Brush.FOREST:BackgroundPaint.Brush.OCEAN,0);history.end();stroke[i]=(System.nanoTime()-start)/1e6;}long end=allocated();Arrays.sort(stroke);System.out.printf(Locale.ROOT,"PAINTER_EDIT,300,medianStrokeMs=%.6f,p95StrokeMs=%.6f,maxStrokeMs=%.6f,allocatedBytes=%.0f,historyMaxBytes=%d,maskBytes=%d%n",stroke[150],stroke[285],stroke[299],before<0||end<0?-1d:(double)(end-before),history.maxBytes(),paint.bytes());
        var style=new BackgroundStyle();style.mode=BackgroundStyle.Mode.CUSTOM;style.paint=paint;byte[] bytes=null;long start=System.nanoTime();for(int i=0;i<100;i++){bytes=BackgroundProfileStore.encode("a".repeat(64),96,64,style);BackgroundProfileStore.decode(bytes,"a".repeat(64),96,64);}System.out.printf(Locale.ROOT,"PAINTER_CODEC,roundTripMeanMs=%.6f,profileBytes=%d%n",(System.nanoTime()-start)/1e6/100,bytes.length);
    }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}}
}
