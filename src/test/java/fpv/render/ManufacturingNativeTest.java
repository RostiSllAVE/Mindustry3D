package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.core.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.world.blocks.production.*;
import fpv.construction.*;import java.util.*;

/** Installed 160.7 crafters and submitted mesh data. Mock GL is not visual acceptance. */
public final class ManufacturingNativeTest {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();Core.gl=new MockGL20();Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new mindustry.net.Net(null);Groups.init();Vars.world=new World();Vars.world.tiles=new Tiles(128,96);Vars.world.setGenerating(true);Vars.world.tiles.fill();Vars.indexer=new mindustry.ai.BlockIndexer();Groups.resize(0,0,1024,768);Time.clear();Time.setDeltaProvider(()->1);Vars.content.init();Vars.state.set(GameState.State.playing);
        var pix=new Pixmap(32,32);pix.fill(0xa0a0a0ff);var region=new TextureAtlas.AtlasRegion(new Tex(),0,0,32,32);region.name="manufacturing-fixture";Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion a){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",region);
        Block[] types={Blocks.graphitePress,Blocks.multiPress,Blocks.siliconSmelter,Blocks.siliconCrucible,Blocks.pyratiteMixer,Blocks.blastMixer};
        check(Blocks.siliconCrucible instanceof AttributeCrafter,"preserve actual attribute crafter subtype");
        check(!ManufacturingModelRenderer.supports(Blocks.graphitePress==null?null:Blocks.kiln)&&!ManufacturingModelRenderer.supports(Blocks.laserDrill),"explicit six-block scope");
        for(var type:types){check(type instanceof GenericCrafter,"native production class");type.region=type.fullIcon=region;}
        var batch=new NativeAtlasMesh.Builder(false);var renderer=new ManufacturingModelRenderer();long id=20;
        try{
            for(var p:ManufacturingModelProfile.values())for(int lod=0;lod<3;lod++){
                int size=p==ManufacturingModelProfile.MULTI||p==ManufacturingModelProfile.CRUCIBLE?3:2;var m=ManufacturingModelGeometry.model(p,size,lod);check(m==ManufacturingModelGeometry.model(p,size,lod),"shared immutable templates");
                var arrays=new ArrayList<float[]>();arrays.addAll(List.of(m.body(),m.lining(),m.rotor(),m.heat()));arrays.addAll(Arrays.asList(m.heads()));arrays.addAll(Arrays.asList(m.rods()));int vertices=0;
                for(var a:arrays){vertices+=a.length/8;check(a.length%24==0,"triangle stream");for(int i=0;i<a.length;i+=8){for(int j=0;j<8;j++)check(Float.isFinite(a[i+j]),"finite geometry");check(Math.abs(a[i])<=.501f&&Math.abs(a[i+2])<=.501f&&a[i+1]>=0&&a[i+1]<=1,"within unchanged footprint and physical height");check(a[i+6]<0||a[i+6]>=0&&a[i+6]<=1&&a[i+7]>=0&&a[i+7]<=1,"atlas crop/sampled-side UV");}}
                check(vertices<4000,"bounded detailed mesh");check(lod<2||vertices<=36,"far LOD budget");
            }
            for(int floor:new int[]{0,1,3,4,5})for(int t=0;t<types.length;t++)for(int rotation=0;rotation<4;rotation++){
                var owner=new LayerBuildingAdapter(null);int x=12+rotation*24,y=10+t*12;var b=(GenericCrafter.GenericCrafterBuild)AdvancedLogisticsNativeTest.put(owner,id++,x,y,floor,types[t],rotation);var p=ManufacturingModelProfile.forBlock(b.block);b.enabled=true;b.efficiency=1;b.warmup=1;b.progress=.5f;b.totalProgress=21;
                for(var item:Vars.content.items())b.items.set(item,0);
                check(ManufacturingModelRenderer.activity(b)==1,"active native warmup");
                if(p.press()){check(Math.abs(ManufacturingModelRenderer.pressOffset(b,p,0)+p.stroke)<.00001,"press uses real cycle phase");check(p.headBottom()-p.stroke>=.34f-.00001f,"head never crosses bed");check(.84f-(p.headTop()-p.stroke)>0,"piston spans actual fixed beam and moving head");}
                if(p.mixer()){float a=ManufacturingModelRenderer.rotorAngle(b,p);Time.time+=200;check(a==ManufacturingModelRenderer.rotorAngle(b,p),"rotor never advances from global time alone");b.totalProgress++;check(a!=ManufacturingModelRenderer.rotorAngle(b,p),"native accumulated progress rotates mixer");}
                for(float distance:new float[]{0,900,10000}){byte[] before=LogisticsModelNativeTest.state(b);batch.clear();float height=fpv.net.HeadlessGeometry.heightFor(b.block);check(renderer.render(b,batch,0,floor*1.4f,0,b.block.size,height,distance),"all floors/rotations/LOD");var emitted=LogisticsModelNativeTest.vertices(batch);check(emitted.length>0,"mesh actually submitted");for(float value:emitted)check(Float.isFinite(value),"finite transformed mesh");for(int i=0;i<emitted.length;i+=9)check(emitted[i+7]>=0&&emitted[i+7]<=.5f&&emitted[i+8]>=0&&emitted[i+8]<=.5f,"submitted native and white-material UVs stay inside their atlas regions");check(Arrays.equals(before,LogisticsModelNativeTest.state(b)),"render is read-only for inventories/progress/config/serialization");}
                b.enabled=false;check(ManufacturingModelRenderer.activity(b)==0,"disabled press stops immediately");b.enabled=true;b.efficiency=0;check(ManufacturingModelRenderer.activity(b)==0,"power/input loss stops working strokes");b.efficiency=1;
                var output=((GenericCrafter)b.block).outputItems[0];b.items.set(output.item,b.block.itemCapacity);check(ManufacturingModelRenderer.activity(b)==0,"output blockage cannot animate full working strokes");b.items.clear();
            }
            var owner=new LayerBuildingAdapter(null);for(int t=0;t<types.length;t++){
                var b=(GenericCrafter.GenericCrafterBuild)owner.create(id++,12+t*5,86,3,types[t],Team.sharded,0);var type=(GenericCrafter)b.block;for(var item:Vars.content.items())b.items.set(item,Math.max(1,b.block.itemCapacity));for(var liquid:Vars.content.liquids())if(b.liquids!=null)b.liquids.set(liquid,100);if(b.power!=null)b.power.status=1;b.enabled=true;b.efficiency=1;b.progress=0;b.totalProgress=0;
                var output=type.outputItems[0];b.items.set(output.item,0);int nativeOutput=0;for(int tick=0;tick<200;tick++){Time.delta=1;b.updateTile();nativeOutput=b.items.get(output.item);if(nativeOutput>=output.amount)break;}
                check(nativeOutput>=output.amount,"actual installed native crafting still produces "+type.name);long built=renderer.builds();byte[] before=LogisticsModelNativeTest.state(b);for(int frame=0;frame<25;frame++){batch.clear();renderer.render(b,batch,0,4.2f,0,type.size,fpv.net.HeadlessGeometry.heightFor(type),0);}check(Arrays.equals(before,LogisticsModelNativeTest.state(b)),"25 rendered frames cannot produce/consume items");check(renderer.builds()==built,"no instance/frame/cycle mesh regeneration");
            }
            check(renderer.entries()==6&&renderer.builds()==6,"one atlas/materialized cache entry per supported type");check(renderer.bytes()<2_000_000,"bounded shared mesh memory");
            System.out.println("ManufacturingNativeTest: "+checks+" metadata/geometry/LOD/state/read-only/native-output/cache checks PASS; mock GL, physical acceptance separate");
        }finally{renderer.clear();batch.dispose();pix.dispose();}
    }
}
