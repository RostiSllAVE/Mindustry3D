package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.core.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.world.blocks.distribution.*;import fpv.construction.*;
import java.util.*;

/** Native state and layer-local geometry checks; mock GL is not physical visual acceptance. */
public final class LogisticsModelNativeTest {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    static float[] vertices(NativeAtlasMesh.Builder b)throws Exception{var f=b.getClass().getDeclaredField("data");f.setAccessible(true);var map=(Map<?,?>)f.get(b);if(map.isEmpty())return new float[0];var d=map.values().iterator().next();var a=d.getClass().getDeclaredField("values");var n=d.getClass().getDeclaredField("count");a.setAccessible(true);n.setAccessible(true);return Arrays.copyOf((float[])a.get(d),(int)n.get(d));}
    static byte[] state(Building b)throws Exception{var data=new java.io.ByteArrayOutputStream();b.write(arc.util.io.Writes.get(new java.io.DataOutputStream(data)));return data.toByteArray();}
    static void cargo(Building b,TextureRegion r){
        if(b instanceof Conveyor.ConveyorBuild c){c.len=3;for(int i=0;i<3;i++){c.ids[i]=new mindustry.type.Item[]{Items.copper,Items.lead,Items.coal}[i];c.ys[i]=.1f+i*.4f;c.xs[i]=0;}c.enabled=true;c.efficiency=1;}
        if(b instanceof StackConveyor.StackConveyorBuild c){c.lastItem=Items.silicon;c.items.set(Items.silicon,6);c.link=c.pos();c.cooldown=.4f;}
        if(b instanceof Sorter.SorterBuild c)c.sortItem=Items.lead;
    }
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();Core.gl=new MockGL20();Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new mindustry.net.Net(null);Groups.init();Vars.world=new World();Vars.world.tiles=new Tiles(96,64);Vars.world.setGenerating(true);Vars.world.tiles.fill();Vars.indexer=new mindustry.ai.BlockIndexer();Groups.resize(0,0,768,512);Time.clear();Time.setDeltaProvider(()->1);Vars.content.init();Vars.state.set(GameState.State.playing);
        var pix=new Pixmap(32,32);pix.fill(0x8c899aff);var tex=new Tex();var r=new TextureAtlas.AtlasRegion(tex,0,0,32,32);r.name="logistics-fixture";Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion ignored){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",r);
        Block[] types={Blocks.conveyor,Blocks.titaniumConveyor,Blocks.plastaniumConveyor,Blocks.armoredConveyor,Blocks.junction,Blocks.router,Blocks.distributor,Blocks.sorter,Blocks.invertedSorter,Blocks.overflowGate,Blocks.underflowGate};
        for(var type:types){type.region=type.fullIcon=r;if(type instanceof Conveyor c){c.regions=new TextureRegion[5][4];for(var row:c.regions)Arrays.fill(row,r);}if(type instanceof StackConveyor c){c.regions=new TextureRegion[]{r,r,r};c.edgeRegion=c.stackRegion=r;}if(type instanceof Sorter c)c.cross=r;}
        for(var item:new mindustry.type.Item[]{Items.copper,Items.lead,Items.coal,Items.silicon})item.fullIcon=r;
        var renderer=new LogisticsModelRenderer();var batch=new NativeAtlasMesh.Builder(false);var registry=new LayerBuildingAdapter(null);
        try{
            for(int i=0;i<types.length;i++){
                var type=types[i];check(LogisticsModelRenderer.supports(type),"explicit native profile "+type.name);int x=8+i*7,y=24;
                Vars.world.tile(x,y).setBlock(type,Team.sharded,i%4);var ground=Vars.world.tile(x,y).build;
                for(int layer=0;layer<=5;layer++){
                    var b=layer==0?ground:registry.create(100+layer*20+i,x,y,layer,type,Team.sharded,i%4);cargo(b,r);var before=state(b);int total=b.items==null?0:b.items.total();float hp=b.health;
                    for(int rotation=0;rotation<4;rotation++)for(int lod=0;lod<3;lod++){
                        b.rotation=rotation;if(b instanceof StackConveyor.StackConveyorBuild c)check(LogisticsModelRenderer.stackSource(c)==b.tile,"packed stack source remains on layer "+layer);
                        batch.clear();float h=fpv.net.HeadlessGeometry.heightFor(type),base=(float)LayerCoordinates.DEFAULT.plane(layer);check(renderer.render(b,batch,0,base,0,type.size,h,lod==0?0:lod==1?900:10000),"shared render Z0-Z5");var v=vertices(batch);check(v.length>0,"real geometry emitted");
                        for(int j=0;j<v.length;j+=9){check(Float.isFinite(v[j]+v[j+1]+v[j+2]),"finite XYZ");check(Math.abs(v[j])<=type.size*.501&&Math.abs(v[j+2])<=type.size*.501,"items/geometry clipped to footprint");check(v[j+1]>=base-.0001&&v[j+1]<=base+h+.04,"unchanged floor and usable surface height");}
                    }
                    b.rotation=i%4;check(Arrays.equals(before,state(b))&&(b.items==null?0:b.items.total())==total&&b.health==hp,"render reads but does not alter inventory/native save/health");
                }
            }
            check(renderer.entries()==11&&renderer.builds()==11,"one model per type, not per building/floor");long built=renderer.builds();batch.clear();renderer.render(Vars.world.tile(8,24).build,batch,0,0,0,1,.22f,0);check(renderer.builds()==built,"stable frames reuse cache");
            check(!LogisticsModelRenderer.supports(Blocks.itemBridge)&&!LogisticsModelRenderer.supports(Blocks.phaseConveyor)&&!LogisticsModelRenderer.supports(Blocks.duct)&&!LogisticsModelRenderer.supports(Blocks.unloader),"explicit deferred scope");
            // Authoritative upper-floor transport and serialization retain native timing and isolation.
            var transport=new LayerBuildingAdapter(null);var a=(Conveyor.ConveyorBuild)transport.create(9001,20,48,3,Blocks.conveyor,Team.sharded,0);var b=(Conveyor.ConveyorBuild)transport.create(9002,21,48,3,Blocks.conveyor,Team.sharded,0);var isolated=(Conveyor.ConveyorBuild)transport.create(9003,21,48,4,Blocks.conveyor,Team.sharded,0);
            a.handleItem(a,Items.copper);for(int t=0;t<40;t++){a.efficiency=b.efficiency=1;Time.delta=1;Time.update();transport.tick();}check(b.len>0&&isolated.len==0,"real native queue remains on owning floor");
            var saved=transport.writeState(9002);check(saved!=null,"upper native state still serializes");
            System.out.println("LogisticsModelNativeTest: "+checks+" native profiles/Z0-Z5/rotations/LOD/real items/read-only save/cache/layer isolation checks PASS; mock GL, physical visuals separate");
        }finally{renderer.clear();batch.dispose();pix.dispose();}
    }
}
