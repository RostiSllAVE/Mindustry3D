package fpv.render;

import arc.*;import arc.math.Mathf;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.core.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.world.blocks.distribution.*;
import fpv.construction.*;import java.util.*;

/** Installed 160.7 metadata, real native state and submitted geometry; mock GL, not visual acceptance. */
public final class AdvancedLogisticsNativeTest {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    static Building put(LayerBuildingAdapter owner,long id,int x,int y,int floor,Block type,int r){if(floor>0)return owner.create(id,x,y,floor,type,Team.sharded,r);Vars.world.tile(x,y).setBlock(type,Team.sharded,r);return Vars.world.tile(x,y).build;}
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();Core.gl=new MockGL20();Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new mindustry.net.Net(null);Groups.init();Vars.world=new World();Vars.world.tiles=new Tiles(128,96);Vars.world.setGenerating(true);Vars.world.tiles.fill();Vars.indexer=new mindustry.ai.BlockIndexer();Groups.resize(0,0,1024,768);Time.clear();Time.setDeltaProvider(()->1);Vars.content.init();Vars.state.set(GameState.State.playing);
        var pix=new Pixmap(32,32);pix.fill(0xa0a0a0ff);var texture=new Tex();var region=new TextureAtlas.AtlasRegion(texture,0,0,32,32);region.name="advanced-fixture";Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion a){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",region);Core.atlas.addRegion("cross-full",region);
        Block[] types={Blocks.itemBridge,Blocks.phaseConveyor,Blocks.unloader,Blocks.ductUnloader,Blocks.massDriver};
        check(Blocks.itemBridge.name.equals("bridge-conveyor")&&Blocks.itemBridge instanceof BufferedItemBridge,"actual buffered bridge, not invented name");check(AdvancedLogisticsModelRenderer.visualHeight(Blocks.massDriver,.3f)>.8f&&AdvancedLogisticsModelRenderer.visualHeight(Blocks.conveyor,.22f)==.22f,"visual envelope fixes native flat distribution category without changing other belts");check(Blocks.ductUnloader instanceof DirectionalUnloader,"actual directional unloader exists");check(((ItemBridge)Blocks.itemBridge).range==4&&((ItemBridge)Blocks.phaseConveyor).range==12&&Blocks.massDriver.size==3,"installed native range/footprint");
        for(var type:types){type.region=type.fullIcon=region;if(type instanceof ItemBridge t)t.endRegion=t.arrowRegion=t.bridgeRegion=region;if(type instanceof MassDriver t)t.baseRegion=region;if(type instanceof mindustry.world.blocks.storage.Unloader t)t.centerRegion=region;if(type instanceof DirectionalUnloader t)t.centerRegion=t.topRegion=t.arrowRegion=region;}
        var renderer=new AdvancedLogisticsModelRenderer();var batch=new NativeAtlasMesh.Builder(false);long id=10;
        for(var kind:AdvancedLogisticsGeometry.Kind.values())for(int lod=0;lod<3;lod++){
            var shape=AdvancedLogisticsGeometry.model(kind,lod);check(shape==AdvancedLogisticsGeometry.model(kind,lod),"immutable template reused");int count=0;
            for(var v:new float[][]{shape.body(),shape.lining(),shape.mechanism()}){count+=v.length/8;check(v.length%24==0,"triangle format");for(int i=0;i<v.length;i+=8){for(int j=0;j<8;j++)check(Float.isFinite(v[i+j]),"finite cached XYZ/normal/UV");check(Math.abs(v[i])<=.501f&&Math.abs(v[i+2])<=.501f&&v[i+1]>=0&&v[i+1]<=1.14f,"compact footprint/height");}}
            check(count<3000,"bounded low-poly budget");check(lod<2||count<100,"far LOD budget");
        }
        try{
            for(int floor:new int[]{0,1,3,5})for(int t=0;t<types.length;t++)for(int r=0;r<4;r++){
                var owner=new LayerBuildingAdapter(null);int x=14+r*24,y=12+t*16;var b=put(owner,id++,x,y,floor,types[t],r);b.enabled=true;b.efficiency=1;
                if(b instanceof ItemBridge.ItemBridgeBuild bridge){var target=put(owner,id++,x+3,y,floor,types[t],0);bridge.link=target.pos();bridge.warmup=bridge.timeSpeed=1;bridge.time=20;check(AdvancedLogisticsModelRenderer.linked(b)==target,"bridge target uses actual owning floor");check(Math.abs(Mathf.mod(renderer.heading(b)+180,360)-180)<.1f,"bridge head follows real link");check(AdvancedLogisticsModelRenderer.activity(b)==1,"actual transport animation state");bridge.link=-1;check(AdvancedLogisticsModelRenderer.linked(b)==null,"disconnect removes visual target");bridge.link=target.pos();}
                if(b instanceof MassDriver.MassDriverBuild driver){var target=put(owner,id++,x+5,y,floor,types[t],0);driver.link=target.pos();driver.rotation=r*90+12;driver.reloadCounter=.9f;driver.items.add(Items.copper,20);driver.state=MassDriver.DriverState.shooting;check(AdvancedLogisticsModelRenderer.linked(b)==target,"mass driver cannot select same XY on Z0");check(AdvancedLogisticsModelRenderer.activity(b)>0,"real loaded shooting state drives charge");}
                if(b instanceof mindustry.world.blocks.storage.Unloader.UnloaderBuild u)u.sortItem=Items.lead;
                if(b instanceof DirectionalUnloader.DirectionalUnloaderBuild u)u.unloadItem=Items.lead;
                for(float distance:new float[]{0,900,10000}){var before=LogisticsModelNativeTest.state(b);batch.clear();check(renderer.render(b,batch,0,floor*1.4f,0,types[t].size,.5f,distance),"shared model on all floors/rotations/LOD");var vertices=LogisticsModelNativeTest.vertices(batch);check(vertices.length>0,"actual geometry emitted");for(float value:vertices)check(Float.isFinite(value),"finite transformed vertices");check(Arrays.equals(before,LogisticsModelNativeTest.state(b)),"drawing cannot mutate native inventories/config/reload/serialization");if(b instanceof mindustry.world.blocks.storage.Unloader.UnloaderBuild||b instanceof DirectionalUnloader.DirectionalUnloaderBuild)check(LogisticsPolishNativeTest.tint(vertices,Items.lead),"native selected-item color retained by both unloader models");}
                b.enabled=false;check(AdvancedLogisticsModelRenderer.activity(b)==0,"disabled machinery not fully active");
            }
            var owner=new LayerBuildingAdapter(null);var bridge=(ItemBridge.ItemBridgeBuild)put(owner,id++,15,88,3,Blocks.phaseConveyor,0);var ground=put(owner,id++,18,88,0,Blocks.phaseConveyor,0);bridge.link=ground.pos();check(AdvancedLogisticsModelRenderer.linked(bridge)==null,"same XY ground target never leaks into upper view");
            var target=put(owner,id++,18,88,3,Blocks.phaseConveyor,0);check(AdvancedLogisticsModelRenderer.linked(bridge)==target,"same-floor native link restored");target.team=Team.crux;check(AdvancedLogisticsModelRenderer.linked(bridge)==null,"enemy link invisible");target.team=Team.sharded;((ItemBridge.ItemBridgeBuild)target).link=bridge.pos();check(AdvancedLogisticsModelRenderer.linked(bridge)==null,"native reverse/double link rejected");
            bridge.link=put(owner,id++,18,89,3,Blocks.phaseConveyor,0).pos();check(AdvancedLogisticsModelRenderer.linked(bridge)==null,"native diagonal link rejected");bridge.link=put(owner,id++,40,88,3,Blocks.phaseConveyor,0).pos();check(AdvancedLogisticsModelRenderer.linked(bridge)==null,"over-range link rejected");
            long cached=renderer.builds();batch.clear();renderer.render(ground,batch,0,0,0,1,.5f,0);check(renderer.builds()==cached,"instances/time/pose reuse model cache");
            System.out.println("AdvancedLogisticsNativeTest: "+checks+" actual metadata/geometry/LOD/native link/floor isolation/filter/read-only/cache checks PASS; mock GL, physical acceptance separate");
        }finally{renderer.clear();batch.dispose();pix.dispose();}
    }
}
