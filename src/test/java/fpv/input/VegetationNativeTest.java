package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;
import fpv.render.*;import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.world.*;import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
import static fpv.input.TerrainStructureRenderTest.get;
import static fpv.input.TerrainStructureRenderTest.set;
import static fpv.input.TerrainStructureRenderTest.call;

/** Native content/cache/actual mesh integration; physical gameplay and Linux graphics are separate. */
public final class VegetationNativeTest {
    static int checks;
    static Object invoke(Object o,String n,Class<?>[] types,Object...args)throws Exception{var m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(o,args);}
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static class Stub extends Texture{Stub(){super();width=height=64;}}
    static VegetationInstances.Instance pose(VegetationInstances v,Tile t){return v.get(t,1,fpv.net.HeadlessGeometry.heightFor(t.block()),n->n.block().solid?3:0);}
    static Tile put(int x,int y,Block b){var t=Vars.world.tile(x,y);t.setBlock(b,mindustry.game.Team.sharded,0);return t;}
    static long mapHash(){long h=17;for(var t:Vars.world.tiles)h=h*31+t.blockID()*71L+t.floorID()*17L+t.overlayID();return h;}
    public static void main(String[] args)throws Exception{init();try{
        Vars.world.tiles=new Tiles(64,64);Vars.world.tiles.fill();Vars.world.setGenerating(true);
        for(var t:Vars.world.tiles)t.setFloor(Blocks.grass.asFloor());
        var catalog=new Block[]{Blocks.pine,Blocks.snowPine,Blocks.sporePine,Blocks.whiteTree,Blocks.whiteTreeDead,Blocks.shrubs,Blocks.sporeCluster,Blocks.redweed,Vars.content.block("pur-bush"),Blocks.yellowCoral,Blocks.redDiamondWall};
        var seen=EnumSet.noneOf(VegetationInstances.Form.class);
        var instances=new VegetationInstances();
        for(int i=0;i<catalog.length;i++){Tile t=put(4+i*4,8,catalog[i]);var p=pose(instances,t);seen.add(p.form());check(p.width()>0&&p.height()>0,"native profile: "+catalog[i].name);check(Math.abs(p.dx())<=.161&&Math.abs(p.dz())<=.161,"bounded local root nudge");}
        check(seen.size()==8,"all eight native form profiles exercised");
        check(VegetationInstances.form(Blocks.boulder)==null&&VegetationInstances.form(Blocks.stoneWall)==null&&VegetationInstances.form(Blocks.conveyor)==null,"rocks, geology, industrial blocks excluded");
        var isolated=put(30,30,Blocks.pine);var single=pose(instances,isolated);
        for(int y=26;y<=34;y++)for(int x=26;x<=34;x++)put(x,y,Blocks.pine);
        instances.clear();var dense=new ArrayList<VegetationInstances.Instance>();var tiles=new ArrayList<Tile>();
        for(int y=26;y<=34;y++)for(int x=26;x<=34;x++){var t=Vars.world.tile(x,y);tiles.add(t);dense.add(pose(instances,t));}
        check(pose(instances,isolated).width()<single.width()*.5,"dense canopy contracts without deleting native plants");
        double before=0,after=0;Set<Float> heights=new HashSet<>(),angles=new HashSet<>();
        for(int i=0;i<dense.size();i++){var a=dense.get(i);heights.add(a.height());angles.add(a.yaw());for(int j=0;j<i;j++){
            var b=dense.get(j);var at=tiles.get(i);var bt=tiles.get(j);double distance=Math.hypot(at.x-bt.x,at.y-bt.y);
            before+=Math.pow(Math.max(0,4.7*.495*2-distance),2);after+=Math.pow(Math.max(0,(a.width()+b.width())*.495-distance),2);
        }}
        check(after<before*.25,"deterministic crown-overlap proxy substantially reduced");check(heights.size()>60&&angles.size()>60,"height/yaw diversity within same tree species");
        var secondClient=new VegetationInstances();for(int i=tiles.size()-1;i>=0;i--)check(dense.get(i).equals(pose(secondClient,tiles.get(i))),"same world appearance regardless of client evaluation order");
        var old=Vars.world.tiles;var clone=new Tiles(64,64);clone.fill();Vars.world.tiles=clone;
        for(var t:old)Vars.world.tile(t.x,t.y).setBlock(t.block(),t.team(),0);
        for(int i=0;i<tiles.size();i++)check(dense.get(i).equals(pose(instances,Vars.world.tile(tiles.get(i).x,tiles.get(i).y))),"world replacement retains coordinate/content-derived transforms");
        var wallTree=put(12,40,Blocks.pine);var open=pose(instances,wallTree);put(13,40,Blocks.metalWall1);instances.invalidate(12,40,3);var constrained=pose(instances,wallTree);check(constrained.width()<open.width(),"tall nearby wall reduces visual crown reach");
        var grid=new LayerWorld(LayerCoordinates.DEFAULT);instances.structures(grid);var under=put(45,40,Blocks.pine);
        grid.add(new LayerComponent(1,LayerComponent.Kind.PLATFORM,45,40,2,"platform",1,0,1,LayerComponent.Mode.FREE,100,100,1,.16,LayerComponent.SupportProfile.NONE,null,null));
        check(pose(instances,under).height()<2.8-.16,"visual crown stays below platform; no native tile movement");
        long h=mapHash();for(int repeat=0;repeat<100;repeat++)pose(instances,under);check(h==mapHash(),"presentation does not alter native map");long builds=instances.builds();for(int repeat=0;repeat<100;repeat++)pose(instances,under);check(builds==instances.builds(),"no neighbour/surface recalculation on cache hit");
        arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();
        var pix=new Pixmap(16,16);pix.fill(0x67aa88ff);var tex=new Stub();var region=new TextureAtlas.AtlasRegion(tex,0,0,16,16);
        Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return new AtlasRegion();}@Override public PixmapRegion getPixmap(AtlasRegion r){return new PixmapRegion(pix);}};
        for(var b:catalog){b.region=region;b.variantRegions=new TextureRegion[]{region};}Blocks.grass.region=region;Blocks.grass.variantRegions=new TextureRegion[]{region};
        var draw=new FpvWorldRenderer();draw.setConstruction(runtime);set(draw,"worldWidth",64);set(draw,"worldHeight",64);draw.terrain().ensureWorld();
        Object mesh=invoke(draw,"buildChunk",new Class[]{int.class,int.class},1,1);check((long)get(mesh,"bytes")>0,"actual batched native atlas mesh built");call(mesh,"dispose",new Class[0]);
        var organic=(OrganicPropRenderer)get(draw,"organicProps");long cached=organic.instances().builds();
        mesh=invoke(draw,"buildChunk",new Class[]{int.class,int.class},1,1);check(organic.instances().builds()==cached,"rebuilding chunk reuses cached per-instance transforms");call(mesh,"dispose",new Class[0]);
        var target=Vars.world.tile(30,30);var neighbour=Vars.world.tile(31,30);var prior=pose(organic.instances(),neighbour);target.setBlock(Blocks.air);draw.invalidate(target);check(!prior.equals(pose(organic.instances(),neighbour)),"native tile edit invalidates dependent neighbours across cached chunks");
        draw.dispose();pix.dispose();
        System.out.println("VegetationNativeTest: "+checks+" native profile/packing/cache/multi-floor/geometry checks PASS; overlap proxy "+before+" -> "+after+"; physical visuals NOT TESTED");
    }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}}
}
