package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;import arc.math.geom.Geometry;
import mindustry.*;import mindustry.content.*;import mindustry.core.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.world.blocks.distribution.*;import fpv.construction.*;
import java.util.*;

/** Real native topology/configuration and emitted UVs; mock GL, not physical visual acceptance. */
public final class LogisticsPolishNativeTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    static Building put(LayerBuildingAdapter registry,long id,int x,int y,int floor,Block type,int rotation){if(floor>0)return registry.create(id,x,y,floor,type,Team.sharded,rotation);Vars.world.tile(x,y).setBlock(type,Team.sharded,rotation);return Vars.world.tile(x,y).build;}
    static void remove(LayerBuildingAdapter registry,long id,int x,int y,int floor){if(floor>0)registry.remove(id);else Vars.world.tile(x,y).setAir();}
    static float extent(float[] v,int axis){float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;for(int i=v.length-54;i<v.length;i+=9){min=Math.min(min,v[i+axis]);max=Math.max(max,v[i+axis]);}return max-min;}
    static boolean tint(float[] v,mindustry.type.Item item){for(int i=0;i<v.length;i+=9)if(Math.abs(v[i+3]-item.color.r)<1e-6&&Math.abs(v[i+4]-item.color.g)<1e-6&&Math.abs(v[i+5]-item.color.b)<1e-6)return true;return false;}
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();Core.gl=new MockGL20();Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new mindustry.net.Net(null);Groups.init();Vars.world=new World();Vars.world.tiles=new Tiles(96,72);Vars.world.setGenerating(true);Vars.world.tiles.fill();Vars.indexer=new mindustry.ai.BlockIndexer();Groups.resize(0,0,768,576);Time.clear();Time.setDeltaProvider(()->1);Vars.content.init();Vars.state.set(GameState.State.playing);
        var pix=new Pixmap(32,32);pix.fill(0x9a9a9aff);var tex=new Tex();var region=new TextureAtlas.AtlasRegion(tex,0,0,32,32);region.name="polish-fixture";Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion r){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",region);Core.atlas.addRegion("cross-full",region);
        Block[] belts={Blocks.conveyor,Blocks.titaniumConveyor,Blocks.armoredConveyor,Blocks.plastaniumConveyor};
        for(var type:belts){type.region=type.fullIcon=region;if(type instanceof Conveyor c){c.regions=new TextureRegion[5][4];for(var row:c.regions)Arrays.fill(row,region);}if(type instanceof StackConveyor c){c.regions=new TextureRegion[]{region,region,region};c.stackRegion=c.edgeRegion=region;}}
        Items.copper.fullIcon=region;var renderer=new LogisticsModelRenderer();var adapter=new NativeBlockAnimationAdapter();var batch=new NativeAtlasMesh.Builder(false);long id=100;
        try{
            for(int floor:new int[]{0,1,3,5})for(int index=0;index<belts.length;index++)for(int rotation=0;rotation<4;rotation++){
                var registry=new LayerBuildingAdapter(null);int x=16+rotation*16,y=12+index*14,r=rotation,back=(r+2)&3,side=(r+1)&3;
                long currentId=id++,nextId=id++,backId=id++,sideId=id++;
                var b=put(registry,currentId,x,y,floor,belts[index],r);put(registry,nextId,x+Geometry.d4x(r),y+Geometry.d4y(r),floor,belts[index],r);put(registry,backId,x+Geometry.d4x(back),y+Geometry.d4y(back),floor,belts[index],r);
                check(LogisticsModelRenderer.openings(b)==5,"straight guards open only along travel on floor "+floor);
                if(b instanceof Conveyor.ConveyorBuild c){
                    c.len=1;c.ids[0]=Items.copper;c.xs[0]=0;c.ys[0]=.99f;c.enabled=true;c.efficiency=1;var before=LogisticsModelNativeTest.state(b);
                    check(LogisticsModelRenderer.cargoConnection(b,r),"real forward seam detected");batch.clear();renderer.render(b,batch,0,floor*1.4f,0,1,.22f,0);var v=LogisticsModelNativeTest.vertices(batch);
                    check(Math.abs(extent(v,0)-.30f)<1e-5&&Math.abs(extent(v,2)-.30f)<1e-5,"full resource image crosses connected belt seam in all orientations");
                    check(Math.abs(extent(v,7)-(.5f-1f/64))<1e-5&&Math.abs(extent(v,8)-(.5f-1f/64))<1e-5,"complete source UVs survive seam, not a shifted/clamped half image");
                    check(Arrays.equals(before,LogisticsModelNativeTest.state(b)),"seam presentation cannot alter native queue/inventory");
                }
                remove(registry,backId,x+Geometry.d4x(back),y+Geometry.d4y(back),floor);put(registry,sideId,x+Geometry.d4x(side),y+Geometry.d4y(side),floor,belts[index],(side+2)&3);
                check(LogisticsModelRenderer.openings(b)==(1|2),"left turn closes unused rear edge, including plastanium");
                remove(registry,sideId,x+Geometry.d4x(side),y+Geometry.d4y(side),floor);side=(r+3)&3;put(registry,id++,x+Geometry.d4x(side),y+Geometry.d4y(side),floor,belts[index],(side+2)&3);
                check(LogisticsModelRenderer.openings(b)==(1|8),"right turn closes unused rear edge in every rotation");
                if(b instanceof Conveyor.ConveyorBuild c){remove(registry,nextId,x+Geometry.d4x(r),y+Geometry.d4y(r),floor);check(!LogisticsModelRenderer.cargoConnection(b,r),"closed end is not an imaginary transport corridor");batch.clear();renderer.render(b,batch,0,floor*1.4f,0,1,.22f,0);var v=LogisticsModelNativeTest.vertices(batch);check(extent(v,r%2==0?0:2)<.2,"closed end retains clipping against unrelated objects");}
            }
            // Distributor uses its actual two-cell vanilla side, not a guessed three-tile tier.
            check(Blocks.distributor.size==2,"native 160.7 distributor footprint is two");for(int lod=0;lod<2;lod++){var m=LogisticsModelGeometry.model(LogisticsModelProfile.DISTRIBUTOR,15,lod);var reference=ResourcePortGeometry.generate(2,1,lod==0);check(Arrays.equals(m.lining(),reference.lining()),"exactly two recessed mouths per distributor side");}
            var registry=new LayerBuildingAdapter(null);
            for(int floor:new int[]{0,2,5})for(var type:new Block[]{Blocks.unloader,Blocks.itemSource}){
                type.region=type.fullIcon=region;if(type instanceof mindustry.world.blocks.storage.Unloader u)u.centerRegion=region;
                var b=put(registry,id++,80,type==Blocks.unloader?16:22,floor,type,0);check(NativeBlockAnimationAdapter.supports(b),"native selected-item overlay supported on all floors");
                for(var item:new mindustry.type.Item[]{Items.copper,Items.lead,null}){
                    if(b instanceof mindustry.world.blocks.storage.Unloader.UnloaderBuild u)u.sortItem=item;else ((mindustry.world.blocks.sandbox.ItemSource.ItemSourceBuild)b).outputItem=item;
                    var before=LogisticsModelNativeTest.state(b);batch.clear();check(adapter.render(b,batch,0,floor*1.4f,0,1,.22f),"live configured color render");var v=LogisticsModelNativeTest.vertices(batch);
                    check(NativeBlockAnimationAdapter.configuredItem(b)==item,"native configuration is the source of visual truth");check(Arrays.equals(before,LogisticsModelNativeTest.state(b)),"render cannot modify source rate/unloader filter/inventory/save state");
                    if(item!=null)check(tint(v,item),"actual selected resource tint submitted");else check(!tint(v,Items.copper)&&!tint(v,Items.lead),"clearing native filter leaves no stale color");
                }
            }
            System.out.println("LogisticsPolishNativeTest: "+checks+" connected UV/seam/closed-end/left-right corner/Z0-Z5/distributor footprint/live source+unloader color/read-only checks PASS; mock GL, physical acceptance separate");
        }finally{renderer.clear();adapter.clear();batch.dispose();pix.dispose();}
    }
}
