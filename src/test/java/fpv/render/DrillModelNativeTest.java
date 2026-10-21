package fpv.render;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.mock.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.net.Net;
import mindustry.world.*;
import mindustry.world.blocks.production.Drill;
import java.util.*;

/** Real native drill update/save fields; mock atlas/GL validates emitted CPU geometry, not visuals. */
public final class DrillModelNativeTest {
    static int checks;static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static class Tex extends Texture {Tex(){super();width=height=64;}}
    static float[] vertices(NativeAtlasMesh.Builder batch)throws Exception{
        var f=batch.getClass().getDeclaredField("data");f.setAccessible(true);var map=(Map<?,?>)f.get(batch);
        if(map.isEmpty())return new float[0];var data=map.values().iterator().next();var values=data.getClass().getDeclaredField("values");var count=data.getClass().getDeclaredField("count");values.setAccessible(true);count.setAccessible(true);return Arrays.copyOf((float[])values.get(data),(int)count.get(data));
    }
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();Core.gl=new MockGL20();
        Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new Net(null);Groups.init();Vars.world=new World();Vars.world.tiles=new Tiles(64,64);Vars.world.setGenerating(true);Vars.world.tiles.fill();Vars.indexer=new mindustry.ai.BlockIndexer();Groups.resize(0,0,512,512);Time.clear();Time.setDeltaProvider(()->1);Vars.content.init();Vars.state.set(GameState.State.playing);
        var pix=new Pixmap(32,32);for(int y=2;y<30;y++)for(int x=2;x<30;x++)if(Math.abs(x-16)+Math.abs(y-16)<23)pix.set(x,y,0x9d897aff);
        var tex=new Tex();var region=new TextureAtlas.AtlasRegion(tex,0,0,32,32);region.name="drill-fixture";
        Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion r){return new PixmapRegion(pix);}};
        var draw=new DrillModelRenderer();var batch=new NativeAtlasMesh.Builder(false);
        try{
            Block[] types={Blocks.mechanicalDrill,Blocks.pneumaticDrill,Blocks.laserDrill,Blocks.blastDrill};
            for(int typeIndex=0;typeIndex<4;typeIndex++){
                var type=(Drill)types[typeIndex];var profile=DrillModelProfile.forBlock(type);check(profile!=null,"native profile available");
                type.region=type.rotatorRegion=type.topRegion=region;type.itemRegion=type.rimRegion=null;
                int x=8+typeIndex*12,y=16;for(int yy=y-3;yy<=y+4;yy++)for(int xx=x-3;xx<=x+4;xx++){var t=Vars.world.tile(xx,yy);t.setFloor(Blocks.stone.asFloor());t.setOverlay(Blocks.oreCopper);}
                var tile=Vars.world.tile(x,y);tile.setBlock(type,Team.sharded,0);var build=(Drill.DrillBuild)tile.build;build.onProximityUpdate();build.efficiency=1;build.warmup=1;build.timeDrilled=17;build.progress=13;
                float speed=type.drillTime,power=type.liquidBoostIntensity,hp=build.health;int resources=build.items.total(),ore=build.dominantItems;float progress=build.progress,time=build.timeDrilled;
                check(DrillModelRenderer.activity(build)>0,"actual operating state "+type.name+" enabled="+build.enabled+" efficiency="+build.efficiency+" ore="+build.dominantItem+" cells="+build.dominantItems+" items="+build.items.total()+" capacity="+type.itemCapacity+" drop="+tile.drop()+" block="+tile.block()+" static="+tile.block().isStatic()+" tier="+type.tier+" canMine="+type.canMine(tile));
                for(int layer:new int[]{0,2})for(int rotation=0;rotation<4;rotation++)for(int lod=0;lod<3;lod++){
                    build.rotation=rotation;batch.clear();float base=layer*2.5f,height=fpv.net.HeadlessGeometry.heightFor(type),size=type.size;
                    check(draw.render(build,batch,0,base,0,size,height,lod==0?0:lod==1?900:10000),"both floors and orientations rendered");
                    var values=vertices(batch);check(values.length>0,"real volume submitted");
                    for(int i=0;i<values.length;i+=9){check(Float.isFinite(values[i])&&Float.isFinite(values[i+1])&&Float.isFinite(values[i+2]),"finite emitted XYZ");check(Math.abs(values[i])<=size*.501&&Math.abs(values[i+2])<=size*.501,"rotation stays inside native footprint");check(values[i+1]>=base-1e-5&&values[i+1]<=base+height+.004,"geometry stays inside existing height");check(values[i+7]>=region.u&&values[i+7]<=region.u2&&values[i+8]>=region.v&&values[i+8]<=region.v2,"atlas-safe sides/top");}
                }
                check(build.progress==progress&&build.timeDrilled==time&&build.items.total()==resources&&build.health==hp&&build.dominantItems==ore&&type.drillTime==speed&&type.liquidBoostIntensity==power,"rendering changes no native production/costs/balance fields");
                float angle=DrillModelRenderer.rotorAngle(build);build.timeDrilled+=23;check(DrillModelRenderer.rotorAngle(build)!=angle,"native drill time rotates head");
                build.enabled=false;check(DrillModelRenderer.activity(build)==0&&DrillModelRenderer.pistonOffset(build,profile)==0,"disabled has no active piston motion");build.enabled=true;
                build.efficiency=0;check(DrillModelRenderer.activity(build)==0,"unpowered/blocked consumption inactive");build.efficiency=1;
                build.items.set(Items.copper,type.itemCapacity);check(DrillModelRenderer.activity(build)==0,"full output inactive");build.items.clear();
                var nativeBytes=new java.io.ByteArrayOutputStream();build.write(arc.util.io.Writes.get(new java.io.DataOutputStream(nativeBytes)));float savedProgress=build.progress,savedWarmup=build.warmup;build.progress=0;build.warmup=0;build.read(arc.util.io.Reads.get(new java.io.DataInputStream(new java.io.ByteArrayInputStream(nativeBytes.toByteArray()))),build.version());check(build.progress==savedProgress&&build.warmup==savedWarmup,"native progress/warmup save format unchanged");
                long cached=draw.builds();batch.clear();for(int i=0;i<100;i++)draw.render(build,batch,i*5,0,0,type.size,1,10000);check(draw.builds()==cached,"large fields reuse model without regeneration");
                // Production is still performed only by the original native update.
                int before=build.items.total();for(int tick=0;tick<1200&&build.items.total()==before;tick++){build.efficiency=1;Time.delta=1;Time.update();build.updateTile();}
                check(build.items.total()>before,"real native mining still produces ore");
            }
            check(draw.entries()==4&&draw.builds()==4,"one cached model per native type");check(draw.bytes()<1024*1024,"bounded cache memory");
            check(!DrillModelRenderer.supports(Blocks.impactDrill)&&!DrillModelRenderer.supports(Blocks.graphitePress),"unrelated families untouched");
            var small=new TextureRegion(tex,0,0,12,18);var original=new TextureRegion(tex,0,0,64,64);
            check(DrillModelRenderer.ratio(small,original)==12/64f&&DrillModelRenderer.depthRatio(small,original)==18/64f,"native part proportions are not stretched to the full base footprint");
            System.out.println("DrillModelNativeTest: "+checks+" native state/CPU atlas geometry/Z0-Z2/rotation/LOD/idle/cache/save/real-production checks PASS; mock GL, physical acceptance separate");
        }finally{draw.clear();batch.dispose();pix.dispose();}
    }
}
