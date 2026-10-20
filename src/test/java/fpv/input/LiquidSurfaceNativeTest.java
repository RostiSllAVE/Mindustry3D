package fpv.input;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import fpv.render.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.Floor;
import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
import static fpv.input.TerrainStructureRenderTest.get;
import static fpv.input.TerrainStructureRenderTest.set;
import static fpv.input.TerrainStructureRenderTest.call;

/** Native content and actual CPU meshes, no physical visual acceptance or GPU claims. */
public final class LiquidSurfaceNativeTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static void near(float a,float b,String s){check(Math.abs(a-b)<.00001f,s+": "+a+" / "+b);}
    static class TextureStub extends Texture{TextureStub(){super();width=height=256;}}
    static float[] data(FpvWorldRenderer r,String field)throws Exception{
        var map=(Map<?,?>)get(get(r,field),"data");for(Object d:map.values())if((int)get(d,"count")>0)return Arrays.copyOf((float[])get(d,"values"),(int)get(d,"count"));return new float[0];
    }
    public static void main(String[] args)throws Exception{
        arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();init();
        Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return new AtlasRegion();}};
        Vars.world.tiles=new Tiles(48,48);Vars.world.tiles.fill();Vars.world.setGenerating(true);
        for(Tile t:Vars.world.tiles)t.setFloor(Blocks.water.asFloor());
        var region=new TextureRegion(new TextureStub(),32,32,32,32);
        var renderer=new FpvWorldRenderer();set(renderer,"worldWidth",48);set(renderer,"worldHeight",48);
        try{
            var elevation=new fpv.terrain.TerrainElevation();long physical=elevation.mapFingerprint();double support=elevation.heightAt(8,-8);
            for(var block:new Block[]{Blocks.water,Blocks.deepwater,Blocks.sandWater,Blocks.darksandWater,Blocks.taintedWater,Blocks.deepTaintedWater,Blocks.darksandTaintedWater,Blocks.tar,Blocks.slag,Blocks.cryofluid,Blocks.arkyciteFloor}){
                check(block.asFloor().isLiquid,"native floor is liquid: "+block.name);Vars.world.tile(8,8).setFloor(block.asFloor());
                check(LiquidSurfaceVisual.motion(block.asFloor())>0,"explicit material identity: "+block.name);
                near(LiquidSurfaceVisual.center(Vars.world.tile(8,8)),-.16f,"common liquid level prevents liquid-liquid cracks");
            }
            var custom=new Floor("safe-modded-liquid");custom.isLiquid=true;near(LiquidSurfaceVisual.motion(custom),0,"unsupported liquid static fallback");
            Vars.world.tile(8,8).setFloor(Blocks.water.asFloor());
            for(int y=2;y<46;y++)for(int x=2;x<46;x++){
                near(LiquidSurfaceVisual.corner(x,y),-.16f,"continuous open ocean");
                for(float time:new float[]{0,3,60}){float h=LiquidSurfaceVisual.height(x-.5f,-y+.5f,time,true);check(h>=-.28001&&h<=-.03999,"small bounded waves below physical ground");near(h,LiquidSurfaceVisual.height(x-.5f,-y+.5f,time,true),"deterministic phase");}
            }
            check(Math.abs(LiquidSurfaceVisual.height(7,-7,0,true)-LiquidSurfaceVisual.height(7,-7,3,true))>.001,"real surface height animates");
            near(LiquidSurfaceVisual.height(7,-7,3,false),-.16f,"disabled-wave level");
            for(int y=1;y<47;y++){near(LiquidSurfaceVisual.cornerMotion(16,y),0,"quiet near/far chunk boundary");near(LiquidSurfaceVisual.height(15.5f,-y+.5f,5,true),-.16f,"static near/far join");}
            Vars.world.tile(7,8).setFloor(Blocks.sand.asFloor());
            near(LiquidSurfaceVisual.corner(8,8),0,"beach shared vertex exactly at land");near(LiquidSurfaceVisual.cornerMotion(8,8),0,"no shore-edge animation gap");
            Vars.world.tile(8,8).setBlock(Blocks.copperWall,mindustry.game.Team.sharded,0);
            near(LiquidSurfaceVisual.center(Vars.world.tile(8,8)),0,"occupied foundation remains anchored");near(LiquidSurfaceVisual.centerMotion(Vars.world.tile(8,8)),0,"no waves beneath occupied foundation");
            Vars.world.tile(8,8).setAir();Vars.world.tile(7,8).setFloor(Blocks.water.asFloor());
            check(LiquidSurfaceVisual.coarse(Vars.world.tile(8,8),6,6,4,4),"homogeneous ocean aggregates");
            Vars.world.tile(5,5).setFloor(Blocks.stone.asFloor());check(!LiquidSurfaceVisual.coarse(Vars.world.tile(8,8),6,6,4,4),"coarse halo detects nearby shore");Vars.world.tile(5,5).setFloor(Blocks.water.asFloor());
            call(renderer,"floorSurface",new Class[]{Tile.class,TextureRegion.class,float.class,boolean.class},Vars.world.tile(8,8),region,0f,false);
            float[] first=data(renderer,"staticAtlas");check(first.length==108,"one cached four-triangle liquid fan");
            for(int i=0;i<first.length;i+=9){check(first[i+1]<0&&first[i+1]>=-1.8f,"actual lowered mesh");check(first[i+6]==0,"opaque bed material, separate animated surface");}
            call(get(renderer,"staticAtlas"),"clear",new Class[]{});call(renderer,"floorSurface",new Class[]{Tile.class,TextureRegion.class,float.class,boolean.class},Vars.world.tile(8,8),region,0f,false);check(Arrays.equals(first,data(renderer,"staticAtlas")),"time does not regenerate or alter CPU mesh");
            for(var type:NavalHullProfile.types()){
                var p=NavalHullProfile.forType(type);double line=p.waterline(.2,.2,0,true);
                near((float)(line+LiquidSurfaceVisual.navalOffset(0,0,true,8,-8,0,false)),-.16f,"naval keel line follows visual water: "+type.name);
                near((float)LiquidSurfaceVisual.navalOffset(3,0,true,8,-8,0,true),0,"lifted ship retains authoritative height");
                near((float)LiquidSurfaceVisual.navalOffset(0,0,false,8,-8,0,true),0,"airborne ship unaffected");
            }
            Vars.world.tile(8,8).setFloor(Blocks.water.asFloor());
            near((float)elevation.heightAt(8,-8),(float)support,"physical support unchanged");
            // All source floors restored before checking the physical fingerprint.
            check(elevation.mapFingerprint()==physical,"visual liquid state creates no terrain authority");
            Vars.world.tile(10,10).setBlock(Blocks.metalWall1);
            call(renderer,"coarseChunk",new Class[]{int.class,int.class,int.class},0,0,4);
            check((int)get(get(renderer,"dynamic"),"count")>=36*7,"actual mixed-liquid coarse pass retains artificial wall with linear tile index");
            Vars.world.tile(10,10).setAir();
            var submerged=new arc.graphics.g3d.Camera3D();submerged.resize(800,600);submerged.near=.02f;submerged.far=20;submerged.position.set(8,-1.0f,-8);submerged.up.set(0,0,-1);submerged.lookAt(8,-1.8f,-8);submerged.update();set(renderer,"camera",submerged);
            var visible=renderer.getClass().getDeclaredMethod("chunkVisible",int.class,int.class);visible.setAccessible(true);check((boolean)visible.invoke(renderer,0,0),"deep bed remains inside chunk culling bounds when looking down underwater");
            near(LiquidSurfaceVisual.WAVE_HEIGHT,.12f,"documented bounded wave amplitude");
            System.out.println("LiquidSurfaceNativeTest: "+checks+" checks PASS; actual native liquids/meshes/coasts/seams/fallback/ten naval profiles; physical gameplay NOT TESTED.");
        }finally{renderer.dispose();input.uninstallProjection();ConstructionPriorityNativeTest.renderer.close();runtime.close();altitude.close();}
    }
}
