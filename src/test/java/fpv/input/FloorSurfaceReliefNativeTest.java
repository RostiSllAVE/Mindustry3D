package fpv.input;

import arc.Core;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import fpv.render.*;
import fpv.terrain.TerrainElevation;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.world.*;
import java.util.*;
import java.util.function.Predicate;
import static fpv.input.ConstructionPriorityNativeTest.*;
import static fpv.input.TerrainStructureRenderTest.get;
import static fpv.input.TerrainStructureRenderTest.set;
import static fpv.input.TerrainStructureRenderTest.call;

/** Real host content + renderer CPU buffers. No GPU pixels, physical input or FPS claim. */
public final class FloorSurfaceReliefNativeTest {
    static int checks;
    static final Predicate<Tile> visible=t->true;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static void world(){Vars.world.tiles=new Tiles(48,48);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(var t:Vars.world.tiles)t.setFloor(Blocks.stone.asFloor());}
    static long mapHash(){long h=1;for(var t:Vars.world.tiles){h=h*31+t.floorID();h=h*31+t.blockID();h=h*31+t.overlayID();}return h;}
    static long reliefHash(FloorSurfaceRelief r){long h=1;for(int y=0;y<=48;y++)for(int x=0;x<=48;x++)h=h*31+Float.floatToIntBits(r.sample(x,y,visible));return h;}
    static final class TextureStub extends Texture {TextureStub(){super();width=256;height=256;}}
    static float[] atlasValues(FpvWorldRenderer draw)throws Exception{
        var data=(Map<?,?>)get(get(draw,"staticAtlas"),"data");check(data.size()==1,"floor and ore remain in the same atlas material batch");
        return (float[])get(data.values().iterator().next(),"values");
    }
    public static void main(String[] args)throws Exception{
        arc.util.ArcNativesLoader.load();
        init();Core.atlas=new arc.graphics.g2d.TextureAtlas(){@Override public AtlasRegion white(){return new AtlasRegion();}};
        var draw=new FpvWorldRenderer();
        try{
            world();var r=new FloorSurfaceRelief(16);long original=mapHash(),h=reliefHash(r);float peak=0,jump=0;
            for(int y=0;y<=48;y++)for(int x=0;x<=48;x++){
                float z=r.sample(x,y,visible);peak=Math.max(peak,Math.abs(z));
                check(Float.isFinite(z)&&Math.abs(z)<=FloorSurfaceRelief.MAX_HEIGHT,"finite bounded visual height");
                if(x>0)jump=Math.max(jump,Math.abs(z-r.sample(x-1,y,visible)));
                if(x%16==0||y%16==0)check(z==0,"detail/coarse/fog chunk border is exactly flat");
            }
            check(peak>.23f&&jump<.20f,"subtle coherent relief exists without independent tile spikes");
            for(int cy=0;cy<3;cy++)for(int cx=0;cx<3;cx++){
                r.prepare(cx,cy,visible);
                for(int y=0;y<=16;y++)for(int x=0;x<=16;x++)check(r.corner(cx*16+x,cy*16+y)==r.sample(cx*16+x,cy*16+y,visible),"cached corner equals shared global sample across every tile/chunk");
            }
            r.prepare(0,0,visible);
            for(int y=1;y<16;y++)for(int x=1;x<16;x++){
                float dx=(r.sample(x+.5,y,visible)-r.sample(x-.5,y,visible))*(.12f/FloorSurfaceRelief.MAX_HEIGHT);
                float dy=(r.sample(x,y+.5,visible)-r.sample(x,y-.5,visible))*(.12f/FloorSurfaceRelief.MAX_HEIGHT);
                float oldShade=Math.max(.76f,Math.min(1.18f,1+dx*5+dy*3.4f));
                check(Math.abs(r.shade(x,y,false)-1)<=Math.abs(oldShade-1)+1e-6,"owner depth increase never amplifies old slope contrast");
                check(r.surface(x,y,0,1)==r.corner(x,y)&&r.surface(x,y,.5f,.5f)==r.center(x,y),"ore surface interpolation shares exact floor anchors");
            }
            check(reliefHash(new FloorSurfaceRelief(16))==h&&mapHash()==original,"independent client instance regenerates same visual field without writing tiles");
            float stone=r.sample(7,7,visible);
            for(var t:Vars.world.tiles)t.setFloor(Blocks.sand.asFloor());check(Math.abs(r.sample(7,7,visible)-stone*.65f)<1e-7,"sand has a smaller amplitude");
            for(var t:Vars.world.tiles)t.setFloor(Blocks.ice.asFloor());check(Math.abs(r.sample(7,7,visible)-stone*.3f)<1e-7,"ice retains only very small relief");
            for(var floor:new Block[]{Blocks.metalFloor,Blocks.metalFloorDamaged,Blocks.metalTiles1,Blocks.coloredFloor,Blocks.coreZone,Blocks.rhyoliteVent,Blocks.water,Blocks.deepwater,Blocks.sandWater,Blocks.space,Blocks.empty}){
                check(FloorSurfaceRelief.amplitude(floor)==0,"artificial/liquid/authored/special surface remains flat: "+floor.name);
            }
            for(var floor:Vars.content.blocks())if(floor instanceof mindustry.world.blocks.environment.Floor f&&f.isLiquid)check(FloorSurfaceRelief.amplitude(f)==0,"all native liquids remain flat");
            check(FloorSurfaceRelief.amplitude(new mindustry.world.blocks.environment.Floor("unknown-floor"))==0,"unknown third-party floors conservatively remain flat");
            world();for(int y=0;y<48;y++)for(int x=24;x<48;x++)Vars.world.tile(x,y).setFloor(Blocks.sand.asFloor());
            r.prepare(1,0,visible);for(int y=0;y<16;y++)check(r.corner(24,y)==r.sample(24,y,visible),"natural material seam uses identical shared corners");
            for(var blocker:new Block[]{Blocks.stoneWall,Blocks.metalWall1,Blocks.pine,Blocks.shrubs,Blocks.conveyor,Blocks.copperWall,Blocks.graphitePress}){
                world();Vars.world.tile(7,7).setBlock(blocker,Team.sharded,0);
                for(var tile:Vars.world.tiles)if(tile.block()!=Blocks.air)for(int dy=0;dy<=1;dy++)for(int dx=0;dx<=1;dx++)check(r.sample(tile.x+dx,tile.y+dy,visible)==0,"every actual footprint cell is anchored: "+blocker.name);
                check(r.sample(6,7,visible)!=0,"neighbour relief blends back outside fixed footprint");
            }
            world();Vars.world.tile(7,7).setFloor(Blocks.water.asFloor());for(int y=7;y<=8;y++)for(int x=7;x<=8;x++)check(r.sample(x,y,visible)==0,"shoreline shares exact flat water edge");
            world();Vars.world.tile(7,7).setOverlay(Blocks.oreCopper);check(r.sample(7,7,visible)==stone,"ore does not flatten or extrude underlying surface");
            Vars.world.tile(7,7).setOverlay(Blocks.pebbles);check(r.sample(7,7,visible)==0,"special floor markings/decorative overlays stay anchored");
            world();check(r.sample(7,7,t->t.x!=7||t.y!=7)==0,"hidden floor cannot leave a crack against flat fog");
            var terrain=new TerrainElevation();long mask=terrain.mapFingerprint(),rev=terrain.revision();double physical=terrain.heightAt(7,-7);r.prepare(0,0,visible);
            check(physical==0&&terrain.heightAt(7,-7)==physical&&terrain.mapFingerprint()==mask&&terrain.revision()==rev,"visual sampling never changes physical terrain or classification");
            check(r.scratchBytes()==5956,"one reused33x33 half-tile height and20x20 material scratch buffer, no per-chunk retained arrays");

            // Exercise the actual detail path, native UVs, overlay offset, and unchanged coarse tier.
            set(draw,"worldWidth",48);set(draw,"worldHeight",48);
            Object mesh=build(draw);check((long)get(mesh,"bytes")==16L*16*12*7*4,"natural detail uses only four triangles/tile, no general world tessellation");
            float[] fallback=(float[])get(get(draw,"terrain"),"vertices");int count=(int)get(get(draw,"terrain"),"count");float max=0;
            for(int i=1;i<count;i+=7)max=Math.max(max,Math.abs(fallback[i]));check(max>.015,"real renderer fallback vertices are displaced");
            call(mesh,"dispose",new Class[]{});
            Object dyn=get(draw,"dynamic");call(dyn,"clear",new Class[]{});call(draw,"coarseChunk",new Class[]{int.class,int.class,int.class},0,0,4);
            float[] coarse=(float[])get(dyn,"vertices");int coarseCount=(int)get(dyn,"count");check(coarseCount==16*6*7,"coarse topology remains unchanged");for(int i=1;i<coarseCount;i+=7)check(coarse[i]==0,"coarse floor remains flat, no per-frame noise generation");
            var texture=new TextureStub();var region=new TextureRegion(texture,32,48,32,32);
            call(get(draw,"staticAtlas"),"clear",new Class[]{});((FloorSurfaceRelief)get(draw,"floorRelief")).prepare(0,0,visible);
            call(draw,"floorSurface",new Class[]{Tile.class,TextureRegion.class,float.class,boolean.class},Vars.world.tile(7,7),region,0f,false);
            float[] v=atlasValues(draw);float[] base=Arrays.copyOf(v,108);
            float u=region.u+.5f/256,vNorth=region.v+.5f/256,vSouth=region.v2-.5f/256;
            check(v[0]==6.5f&&v[2]==-6.5f&&v[7]==u&&v[8]==vSouth,"southwest vertex retains correct native texture orientation and texel inset");
            check(v[36]==7.5f&&v[38]==-7.5f&&v[44]==vNorth,"north texture edge remains native +Y/render -Z");
            float offset=FloorSurfaceRelief.ORE_OFFSET;
            call(draw,"floorSurface",new Class[]{Tile.class,TextureRegion.class,float.class,boolean.class},Vars.world.tile(7,7),region,offset,true);v=atlasValues(draw);
            for(int i=0;i<108;i+=9){check(Math.abs(v[108+i+1]-base[i+1]-offset)<1e-7,"ore shares floor geometry with only existing visual separation");check(v[108+i]==base[i]&&v[108+i+2]==base[i+2]&&v[108+i+7]==base[i+7]&&v[108+i+8]==base[i+8],"overlay XY/UV/triangle correspondence");}
            var data=(Map<?,?>)get(get(draw,"staticAtlas"),"data");check((int)get(data.values().iterator().next(),"count")==216,"one floor plus overlay:24 vertices, one atlas batch");

            float brightest=0,darkest=2;
            for(int i=3;i<108;i+=9){brightest=Math.max(brightest,base[i]);darkest=Math.min(darkest,base[i]);}
            check(brightest-darkest>.01f,"real textured vertices carry visible slope lighting instead of uniform white");
            world();for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})Vars.world.tile(7+d[0],7+d[1]).setBlock(Blocks.conveyor,Team.sharded,0);
            r.prepare(0,0,visible);for(int y=7;y<=8;y++)for(int x=7;x<=8;x++)check(r.corner(x,y)==0,"neighbour foundations retain flat edges in a one-cell gap");
            check(Math.abs(r.center(7,7))>.003,"open ground between conveyors has real interior relief even with all four corners anchored");
            Vars.world.tile(7,7).setBlock(Blocks.conveyor,Team.sharded,0);r.prepare(0,0,visible);
            check(r.center(7,7)==0,"building's own interior remains completely flat");

            // Invalidate a structure just across a chunk boundary: apron and anchor neighbours rebuild.
            call(get(draw,"staticAtlas"),"clear",new Class[]{});world();Object left=build(draw);((Map<Integer,Object>)get(draw,"chunks")).put(0,left);Object revision=get(draw,"revision");
            Vars.world.tile(16,7).setBlock(Blocks.copperWall,Team.sharded,0);draw.invalidate(Vars.world.tile(16,7));
            check((long)get(left,"revision")==-1&&revision.equals(get(draw,"revision")),"adjacent cached chunk invalidates locally, no global terrain rebuild");
            world();check(reliefHash(r)==h,"world replacement restores exact deterministic corner field");
            System.out.println("FloorSurfaceReliefNativeTest: "+checks+" checks PASS; peak="+peak+", maxAdjacentDelta="+jump+" tiles; real CPU mesh/UV tests, physical visuals NOT TESTED.");
        }finally{draw.dispose();input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
    static Object build(FpvWorldRenderer draw)throws Exception{var m=draw.getClass().getDeclaredMethod("buildChunk",int.class,int.class);m.setAccessible(true);return m.invoke(draw,0,0);}
}
