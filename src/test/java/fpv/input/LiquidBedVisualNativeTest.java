package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;import fpv.render.*;import mindustry.*;import mindustry.content.*;import mindustry.world.*;import fpv.terrain.TerrainElevation;
import static fpv.input.ConstructionPriorityNativeTest.*;
import static fpv.input.TerrainStructureRenderTest.get;

public final class LiquidBedVisualNativeTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void near(double a,double b,String message){check(Math.abs(a-b)<.00002,message+": "+a+" / "+b);}
    static class Stub extends Texture{Stub(){super();width=height=64;}}
    public static void main(String[] args)throws Exception{init();arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();try{
        var region=new TextureAtlas.AtlasRegion(new Stub(),0,0,16,16);Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return region;}};
        Vars.world.tiles=new Tiles(80,64);Vars.world.tiles.fill();Vars.world.setGenerating(true);
        for(var t:Vars.world.tiles)t.setFloor((t.x<8?Blocks.sandWater:t.x<16?Blocks.water:Blocks.deepwater).asFloor());
        for(var b:new mindustry.world.Block[]{Blocks.water,Blocks.sandWater,Blocks.deepwater}){b.region=region;b.variantRegions=new TextureRegion[]{region};}
        var terrain=new TerrainElevation();long nativeMap=terrain.mapFingerprint();double physical=fpv.environment.LiquidBody.bed(60,-32);float previous=0;
        for(int x=16;x<30;x++){float value=LiquidBedVisual.depth(Vars.world.tile(x,32));check(value>=previous&&value<=6,"deep water gradually descends away from shallow water");previous=value;}
        near(previous,6,"deep interior maximum");near(LiquidBedVisual.depth(Vars.world.tile(10,32)),.95,"normal water retained");near(LiquidBedVisual.depth(Vars.world.tile(3,32)),.55,"shallow shelf retained");
        near(LiquidBedVisual.corner(80,32),-6,"native ocean edge continues its real seabed, no raised wall");near(LiquidSurfaceVisual.corner(80,32),HorizonLandscape.SEA,"native water surface aligns to generated sea");
        check(!LiquidSurfaceVisual.coarse(Vars.world.tile(19,32),17,30,4,4),"gradient bed retains matching native tile fans");check(LiquidSurfaceVisual.coarse(Vars.world.tile(50,32),48,30,4,4),"level deep bed can use cheap coarse representation");
        long builds=LiquidBedVisual.builds();for(int i=0;i<1000;i++)LiquidBedVisual.corner(80,32);check(LiquidBedVisual.builds()==builds,"no per-frame distance-field generation");check(LiquidBedVisual.cachedBytes()<=LiquidBedVisual.MAX_AXIS*LiquidBedVisual.MAX_AXIS*6,"bounded cached bathymetry");
        var horizon=new AutomaticHorizon();horizon.setTerrain(terrain);horizon.ensure();var method=AutomaticHorizon.class.getDeclaredMethod("waterBedHeight",double.class);method.setAccessible(true);
        double along=80+32;near(((Number)method.invoke(horizon,along)).doubleValue(),-6,"background anchor equals native bed");
        var field=new HorizonLandscape(horizon.profile(),horizon.composition(),s->{try{return((Number)method.invoke(horizon,s)).doubleValue();}catch(Exception e){throw new AssertionError(e);}});var out=new HorizonLandscape.Surface();float oldDepth=0;
        for(double d:new double[]{0,4,12,100,400,1200,2000}){var pt=HorizonGeometry.point(horizon.profile(),along,d);field.sample(along,d,pt.x(),pt.z(),out);check(out.depth>=oldDepth&&out.height<=-6+.0001,"generated ocean continues deeper, never rises at edge");oldDepth=out.depth;}
        near(oldDepth,64,"far ocean is substantially deeper than previous sixteen-tile basin");
        var collar=get(horizon,"collar");boolean bed=false,surface=false;for(var part:(java.util.List<?>)get(collar,"parts")){var mesh=(Mesh)get(part,"mesh");var vertices=mesh.getVerticesBuffer();for(int i=0;i<mesh.getNumVertices()*17;i+=17)if(Math.abs(vertices.get(i)-79.5f)<.0001&&Math.abs(vertices.get(i+2)+31.5f)<.0001){if(vertices.get(i+6)<.5)bed|=Math.abs(vertices.get(i+1)+6)<.0001;else surface|=Math.abs(vertices.get(i+1)-HorizonLandscape.SEA)<.0001;}}
        check(bed&&surface,"submitted native/collar seam has both matching bed and matching water plane");
        near(fpv.environment.LiquidBody.bed(60,-32),physical,"authoritative underwater floor unchanged");check(terrain.mapFingerprint()==nativeMap,"no gameplay floor/block change");
        Vars.world.tile(60,32).setFloor(Blocks.water.asFloor());LiquidBedVisual.invalidate(Vars.world.tile(60,32));LiquidBedVisual.depth(Vars.world.tile(62,32));check(LiquidBedVisual.builds()==builds+1,"floor edits invalidate visual distance cache");
        horizon.dispose();System.out.println("LiquidBedVisualNativeTest: "+checks+" progressive/native-edge/deep-ocean/cache/mesh/physics-invariance checks PASS");
    }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}}
}
