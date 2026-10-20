package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;import fpv.render.*;import fpv.terrain.TerrainElevation;
import mindustry.*;import mindustry.content.*;import mindustry.world.*;import mindustry.game.Team;import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
import static fpv.input.TerrainStructureRenderTest.get;
import static fpv.render.BorderBiomeProfile.Biome.*;

public final class HorizonNativeTest {
    static int checks;static TerrainElevation terrain;
    static class Stub extends Texture{Stub(){super();width=height=64;}}
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static void world(int w,int h,Block floor){Vars.world.tiles=new Tiles(w,h);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(var t:Vars.world.tiles)t.setFloor(floor.asFloor());terrain=new TerrainElevation();}
    static BorderBiomeProfile profile(){return BorderBiomeProfile.analyze(BorderBiomeProfile.nativeSource(terrain),8);}
    static void dominant(BorderBiomeProfile.Biome wanted){for(var s:profile().sections())check(s.value().dominant()==wanted,"native dominant "+wanted+": "+s.value().dominant());}
    static long mapHash(){long h=17;for(var t:Vars.world.tiles)h=h*31+t.blockID()*71L+t.floorID()*17L+t.overlayID();return h;}
    static void fractionalCollar(TextureRegion region,AutomaticHorizon horizon)throws Exception{
        world(64,48,Blocks.sand);for(var t:Vars.world.tiles)if(t.x>=32)t.setFloor(Blocks.water.asFloor());
        Blocks.sand.region=Blocks.water.region=region;Blocks.sand.variantRegions=Blocks.water.variantRegions=new TextureRegion[]{region};
        horizon.setTerrain(terrain);long before=mapHash();horizon.ensure();
        var boundary=AutomaticHorizon.class.getDeclaredMethod("boundaryHeight",double.class);boundary.setAccessible(true);
        var waterBed=AutomaticHorizon.class.getDeclaredMethod("waterBedHeight",double.class);waterBed.setAccessible(true);
        var geometry=HorizonGeometry.build(horizon.profile(),s->{try{return((Number)boundary.invoke(horizon,s)).doubleValue();}catch(Exception e){throw new AssertionError(e);}},horizon.composition(),s->{try{return((Number)waterBed.invoke(horizon,s)).doubleValue();}catch(Exception e){throw new AssertionError(e);}});
        var collar=get(horizon,"collar");var parts=(List<?>)get(collar,"parts");int tested=0;
        for(double s:geometry.positions){if(s==Math.floor(s))continue;float[] target=new float[HorizonGeometry.STRIDE];geometry.row(s,0,target);boolean found=false;
            for(var part:parts){var mesh=(Mesh)get(part,"mesh");var data=mesh.getVerticesBuffer();
                for(int at=0;at<mesh.getNumVertices()*17;at+=17)if(data.get(at+6)<.5f&&Math.abs(data.get(at)-target[0])<.0001f&&Math.abs(data.get(at+1)-target[1])<.0001f&&Math.abs(data.get(at+2)-target[2])<.0001f){found=true;break;}
                if(found)break;
            }
            check(found,"d4 native collar contains exact fractional coarse/fine endpoint: "+s);tested++;
        }
        check(tested>=4,"native beach fixture exercises fractional d4 coastline joins");check(mapHash()==before,"fractional visual seam generation never alters native map");
    }
    public static void main(String[] args)throws Exception{init();try{
        world(48,32,Blocks.water);dominant(OCEAN);
        world(48,32,Blocks.stone);for(var t:Vars.world.tiles)t.setBlock(Blocks.stoneWall);dominant(MOUNTAINS);
        world(48,32,Blocks.grass);for(var t:Vars.world.tiles)t.setBlock(Blocks.pine);dominant(FOREST);
        world(48,32,Blocks.grass);for(var t:Vars.world.tiles)if((t.x+t.y)%4!=0)t.setBlock(Blocks.pine);dominant(FOREST);
        world(48,32,Blocks.sand);Vars.world.tile(10,0).setBlock(Blocks.pine);dominant(DESERT);
        world(48,32,Blocks.snow);dominant(ICE);world(48,32,Blocks.basalt);dominant(VOLCANIC);
        world(48,32,Blocks.space);dominant(SPACE);world(48,32,Blocks.empty);dominant(SPACE);
        world(48,32,Blocks.stone);dominant(WASTELAND);for(var t:Vars.world.tiles)t.setBlock(Blocks.metalWall1);dominant(WASTELAND);
        var unknown=new mindustry.world.blocks.environment.Floor("fpv-horizon-unknown");world(48,32,unknown);dominant(UNKNOWN);
        world(64,64,Blocks.stone);
        for(var t:Vars.world.tiles){if(t.y<8)t.setFloor(Blocks.sand.asFloor());else if(t.y>=56)t.setFloor(Blocks.water.asFloor());else if(t.x<8)t.setFloor(Blocks.snow.asFloor());else if(t.x>=56){t.setFloor(Blocks.grass.asFloor());t.setBlock(Blocks.pine);}}
        var p=profile();var sample=new BorderBiomeProfile.Sample();p.sample(32,sample);check(sample.dominant()==DESERT,"south desert");p.sample(96,sample);check(sample.dominant()==FOREST,"east forest");p.sample(160,sample);check(sample.dominant()==OCEAN,"north ocean");p.sample(224,sample);check(sample.dominant()==ICE,"west ice");
        // Distinct sections on one edge must survive; not one dominant biome per side.
        for(int y=56;y<64;y++)for(int x=32;x<64;x++)Vars.world.tile(x,y).setFloor(Blocks.sand.asFloor());p=profile();p.sample(136,sample);check(sample.dominant()==DESERT,"north-east desert region");p.sample(184,sample);check(sample.dominant()==OCEAN,"north-west ocean region");
        arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();var tex=new Stub();var region=new TextureAtlas.AtlasRegion(tex,8,8,16,16);Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return region;}};
        world(32,32,Blocks.stone);Blocks.stone.region=region;Blocks.stone.variantRegions=new TextureRegion[]{region};
        var minFilter=tex.getMinFilter();var magFilter=tex.getMagFilter();var wrapU=tex.getUWrap();var wrapV=tex.getVWrap();
        var horizon=new AutomaticHorizon();horizon.setTerrain(terrain);long before=mapHash();int units=mindustry.gen.Groups.unit.size();horizon.ensure();long count=horizon.stats().builds();
        check(tex.getMinFilter()==minFilter&&tex.getMagFilter()==magFilter&&tex.getUWrap()==wrapU&&tex.getVWrap()==wrapV,"host atlas sampling state unchanged");
        check(horizon.composition().sampledCells()>0&&horizon.composition().sampledCells()<=4096,"map context is bounded and cached with the shared CPU landscape");
        var materialClass=Class.forName("fpv.render.HorizonDetailTexture");var create=materialClass.getDeclaredMethod("create");create.setAccessible(true);var detail=(Texture)create.invoke(null);
        check(detail.width==32&&detail.height==32&&detail.getMinFilter()==Texture.TextureFilter.mipMapLinearLinear&&detail.getUWrap()==Texture.TextureWrap.repeat,"world detail has own mipmapped repeat sampling");detail.dispose();
        check(tex.getMinFilter()==minFilter&&tex.getUWrap()==wrapU,"detail creation never changes host atlas filtering or wrap");
        for(int i=0;i<100;i++){Vars.state.tick++;horizon.ensure();}check(horizon.stats().builds()==count,"unchanged frames have no rescan/regeneration");check(mapHash()==before&&units==mindustry.gen.Groups.unit.size(),"no native tiles/entities changed");
        var collar=get(horizon,"collar");var parts=(List<?>)get(collar,"parts");check(parts.size()==1,"single atlas batch");var mesh=(Mesh)get(parts.get(0),"mesh");var data=mesh.getVerticesBuffer();
        for(int at=0;at<mesh.getNumVertices()*17;at+=17){
            check(data.get(at+7)==region.u+.5f/64&&data.get(at+8)==region.v+.5f/64,"atlas repeat rectangle keeps minimum half texel inset");
            check(data.get(at+9)==region.u2-.5f/64&&data.get(at+10)==region.v2-.5f/64,"atlas repeat rectangle keeps maximum half texel inset");
        }
        Vars.world.tile(16,16).setFloor(Blocks.sand.asFloor());horizon.invalidate(Vars.world.tile(16,16));horizon.ensure();check(horizon.stats().builds()==count,"interior edits do not rebuild background");
        Vars.world.tile(0,16).setFloor(Blocks.water.asFloor());horizon.invalidate(Vars.world.tile(0,16));horizon.ensure();check(horizon.stats().builds()==++count,"border edits invalidate cache");
        Core.settings.put(AutomaticHorizon.STRIP_SETTING,16);horizon.ensure();check(horizon.stats().builds()==++count,"strip setting invalidates cache");
        var old=Vars.world.tiles;world(32,32,Blocks.stone);horizon.setTerrain(terrain);horizon.ensure();check(horizon.stats().builds()==++count&&Vars.world.tiles!=old,"world identity/lifecycle rebuild");
        var env=new EnvironmentRenderer();env.setTerrain(terrain);var camera=new arc.graphics.g3d.Camera3D();camera.position.set(16,720,-16);Core.settings.put(AutomaticHorizon.SETTING,false);float legacy=env.farPlane(camera),range=env.mapFarPlane(camera);Core.settings.put(AutomaticHorizon.SETTING,true);
        check(Math.abs(legacy-range)<.001,"legacy map/pilot range preserved");check(env.farPlane(camera)>legacy+1000&&env.mapFarPlane(camera)==range,"decorative far reach is separate from pilot range");
        fractionalCollar(region,horizon);
        horizon.clear();check(horizon.stats().meshBytes()==0&&horizon.profile()==null&&horizon.composition().sampledCells()==0,"old meshes/profile/context references released");horizon.dispose();env.dispose();
        System.out.println("HorizonNativeTest: "+checks+" native biome/region/seam/cache/scope/lifecycle checks PASS; actual visual acceptance separate");
    }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}}
}
