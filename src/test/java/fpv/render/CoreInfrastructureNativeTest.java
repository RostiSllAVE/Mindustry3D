package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock;import mindustry.world.blocks.defense.Radar;
import fpv.construction.*;import java.util.*;

/** Actual native building instances and streamed geometry. Candidate upper cores are inert
 * adapter fixtures, not permission to place/upgrade cores outside native/server rules. */
public final class CoreInfrastructureNativeTest {
    static int checks,poses;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();LayerCampaignNativeTest.main(args);
        Core.gl=new MockGL20();Core.graphics=new MockGraphics();var pix=new Pixmap(32,32);pix.fill(0x9999aaff);pix.fillRect(12,12,8,8,0);
        var region=new TextureAtlas.AtlasRegion(new Tex(),0,0,32,32);region.name="core-infrastructure-fixture";
        Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion r){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",region);
        Block[] types={Blocks.coreShard,Blocks.coreFoundation,Blocks.coreNucleus,Blocks.coreBastion,Blocks.coreCitadel,Blocks.coreAcropolis,Blocks.radar};
        int[] sizes={3,4,5,4,5,6,1},capacities={4000,9000,13000,2000,3000,4000,10};
        var models=new CoreInfrastructureRenderer();var stream=new NativeAtlasMesh.Builder(false);var owner=new LayerBuildingAdapter(null);long id=9000;
        var shapes=new HashSet<Integer>();
        try{
            for(int index=0;index<types.length;index++){
                var type=types[index];check(type.size==sizes[index]&&type.itemCapacity==capacities[index],"installed 160.7 metadata retained: "+type.name);
                int hp=type.health,cap=type.unitCapModifier;var costs=type.requirements;boolean zone=type instanceof CoreBlock c&&c.requiresCoreZone;
                type.region=type.fullIcon=type.teamRegion=region;type.teamRegions=null;
                if(type instanceof Radar radar)radar.baseRegion=radar.glowRegion=region;
                var p=CoreInfrastructureProfile.forBlock(type);check(p!=null,"explicit target coverage");
                for(int lod=0;lod<3;lod++){
                    var g=CoreInfrastructureGeometry.model(p,type.size,lod);check(g==CoreInfrastructureGeometry.model(p,type.size,lod),"shared immutable template");
                    for(var a:new float[][]{g.body(),g.lining(),g.lights(),g.moving(),g.movingLights()})for(int i=0;i<a.length;i+=8){
                        check(a[i]>=-.491&&a[i]<=.491&&a[i+2]>=-.491&&a[i+2]<=.491,"template respects native footprint");
                        check(a[i+1]>=0&&a[i+1]<=1,"template respects unchanged physical height");
                    }
                    if(p.core()&&lod==0)check(shapes.add(Arrays.hashCode(g.body())),"six distinct unscaled chassis templates");
                }
                for(int floor:new int[]{0,1,3,5})for(int rotation=0;rotation<4;rotation++)for(Team team:new Team[]{Team.sharded,Team.crux,Team.green}){
                    owner.clear();Building b;
                    if(floor==0){Vars.world.tile(20,20).setBlock(type,team,rotation);b=Vars.world.build(20,20);}else b=owner.create(id++,20,20,floor,type,team,rotation);
                    b.health=b.maxHealth*.7f;if(b.items!=null)b.items.set(Items.copper,17);if(b instanceof Radar.RadarBuild radar){radar.totalProgress=123.5f;radar.smoothEfficiency=.75f;radar.progress=.6f;}
                    if(b instanceof CoreBlock.CoreBuild core)core.thrusterTime=.4f;
                    for(boolean active:new boolean[]{false,true}){
                        b.enabled=active;check(CoreInfrastructureRenderer.activity(b)==(active?(b instanceof Radar.RadarBuild?.75f:1):0),"actual enabled/readiness state");
                        var before=LogisticsModelNativeTest.state(b);float health=b.health;int items=b.items==null?0:b.items.get(Items.copper),coreCount=team.cores().size;Time.time=100;
                        float scan=b instanceof Radar.RadarBuild radar?CoreInfrastructureRenderer.scanAngle(radar):0;
                        for(float distance:new float[]{0,900,10000}){
                            float h=FpvWorldRenderer.heightFor(type),base=floor*2.5f;stream.clear();stream.eyeHeight=base+h+2;
                            check(models.render(b,stream,0,base,0,h,distance),"native profile handles presentation");var vertices=LogisticsModelNativeTest.vertices(stream);check(vertices.length>0,"volumetric geometry present");
                            for(int i=0;i<vertices.length;i+=9){
                                for(int j=0;j<9;j++)check(Float.isFinite(vertices[i+j]),"finite XYZ/UV/materials");
                                check(Math.abs(vertices[i])<=type.size*.501&&Math.abs(vertices[i+2])<=type.size*.501,"rotating parts remain in native footprint");
                                check(vertices[i+1]>=base-.001&&vertices[i+1]<=base+h+.009,"correct per-floor anchor/envelope");
                                check(vertices[i+7]>=0&&vertices[i+7]<=.5&&vertices[i+8]>=0&&vertices[i+8]<=.5,"UV stays inside original atlas region");
                            }
                            if(distance==0&&rotation==0&&team==Team.sharded&&active){
                                int high=0;for(int i=0;i<vertices.length;i+=9)if(vertices[i+1]>vertices[high+1])high=i;
                                var camera=new arc.graphics.g3d.Camera3D();camera.resize(100,100);camera.fov=1;camera.near=.04f;camera.far=10;
                                camera.position.set(vertices[high],vertices[high+1],vertices[high+2]+5);camera.lookAt(vertices[high],vertices[high+1],vertices[high+2]);camera.update();
                                check(BlockVisualBounds.visible(camera,b,0,base,0,h,h),"narrow frustum retains actual highest model point");
                                check(!BlockVisualBounds.visible(camera,b,10000,base,0,h,h),"offscreen infrastructure is culled");
                                stream.clear();stream.eyeHeight=base-1;models.render(b,stream,0,base,0,h,distance);
                                check(LogisticsModelNativeTest.vertices(stream).length==vertices.length+6*9,"only inward foundation bottom culled from above; unsupported underside retained below");
                            }
                            poses++;
                        }
                        check(Arrays.equals(before,LogisticsModelNativeTest.state(b))&&health==b.health&&items==(b.items==null?0:b.items.get(Items.copper))&&coreCount==team.cores().size,"rendering cannot alter inventories, HP, native save state or team/core registration");
                        if(b instanceof Radar.RadarBuild radar){Time.time+=500;check(CoreInfrastructureRenderer.scanAngle(radar)==scan,"radar phase uses native progress, not render clock");radar.totalProgress+=100;check(CoreInfrastructureRenderer.scanAngle(radar)!=scan,"native scanning phase advances");}
                        if(b instanceof CoreBlock.CoreBuild core){check(CoreInfrastructureRenderer.landing(b)==.4f,"real native thruster timer, no fabricated spawn action");core.thrusterTime=0;check(CoreInfrastructureRenderer.landing(b)==0,"native landing glow stops");core.thrusterTime=.4f;}
                    }
                    b.dead=true;stream.clear();check(models.render(b,stream,0,0,0,1,0)&&LogisticsModelNativeTest.vertices(stream).length==0,"removed/dead core model cannot linger");b.dead=false;
                }
                check(type.health==hp&&type.unitCapModifier==cap&&type.requirements==costs&&(type instanceof CoreBlock c&&c.requiresCoreZone)==zone,"no balance, spawn cap, cost or core-zone changes");
            }
            check(shapes.size()==6&&models.entries()==7&&models.builds()==7,"six independent cores and radar cached once across poses");
            check(!CoreInfrastructureRenderer.supports(Blocks.illuminator)&&!CoreInfrastructureRenderer.supports(Blocks.interplanetaryAccelerator)&&!CoreInfrastructureRenderer.supports(Blocks.forceProjector),"accepted and deferred families retain their own paths");
            lifecycle(models,stream);
            System.out.println("CoreInfrastructureNativeTest: "+checks+" native geometry/XYZ/UV/state/team/cache checks, "+poses+" poses PASS; candidate upper cores do not certify placement permissions");
        }finally{owner.clear();models.clear();stream.dispose();pix.dispose();}
    }
    static void lifecycle(CoreInfrastructureRenderer models,NativeAtlasMesh.Builder stream)throws Exception{
        Vars.world.setGenerating(false);Vars.state.rules.editor=false;Vars.state.rules.coreBuildAndConfig=false;
        Vars.state.rules.canGameOver=false;Vars.state.rules.infiniteResources=true;
        var tile=Vars.world.tile(48,48);
        for(var block:new Block[]{Blocks.coreShard,Blocks.coreFoundation,Blocks.coreNucleus,Blocks.coreBastion,Blocks.coreCitadel,Blocks.coreAcropolis}){
            var type=(CoreBlock)block;tile.setBlock(block,Team.sharded,0);var b=(CoreBlock.CoreBuild)tile.build;b.onProximityUpdate();
            var item=CoreInfrastructureProfile.forBlock(block).erekir()?Items.beryllium:Items.copper;
            int before=b.items.get(item);b.handleStack(item,7,null);check(b.items.get(item)==before+7,"native core stack input works");
            check(b.removeStack(item,3)==3&&b.items.get(item)==before+4,"native unloading removes exactly requested items");
            Player player=Player.create();Vars.player=player;player.team(Team.sharded);int units=Groups.unit.size();
            CoreBlock.playerSpawn(tile,player);
            check(player.unit().type==type.unitType&&player.unit().spawnedByCore&&Groups.unit.size()==units+1,"native respawn creates original core unit exactly once");
            var state=LogisticsModelNativeTest.state(b);stream.clear();models.render(b,stream,0,0,0,FpvWorldRenderer.heightFor(block),0);
            check(Arrays.equals(state,LogisticsModelNativeTest.state(b))&&Groups.unit.size()==units+1,"drawing a spawned core cannot respawn/spend again");player.unit().remove();
        }
        tile.setBlock(Blocks.air);tile.setFloor((mindustry.world.blocks.environment.Floor)Blocks.stone);
        check(!((CoreBlock)Blocks.coreBastion).canPlaceOn(tile,Team.sharded,0),"native normal ground is not an empty core zone");
        for(int y=44;y<=52;y++)for(int x=44;x<=52;x++)Vars.world.tile(x,y).setFloor((mindustry.world.blocks.environment.Floor)Blocks.coreZone);
        check(((CoreBlock)Blocks.coreBastion).canPlaceOn(tile,Team.sharded,0),"native complete core zone permits placement");
        tile.setBlock(Blocks.coreShard,Team.sharded,0);var upgrade=(CoreBlock)Blocks.coreFoundation;
        Vars.state.rules.infiniteResources=false;
        var source=(CoreBlock.CoreBuild)tile.build;source.items.clear();
        for(var stack:upgrade.requirements)source.items.set(stack.item,stack.amount+23);
        check(upgrade.canPlaceOn(tile,Team.sharded,0),"native paid upgrade is valid");
        upgrade.beforePlaceBegan(tile,Blocks.coreShard);upgrade.placeBegan(tile,Blocks.coreShard,null);
        check(tile.block()==upgrade&&CoreInfrastructureProfile.forBlock(tile.block())==CoreInfrastructureProfile.FOUNDATION,"native upgrade replaces old model identity");
        for(var stack:upgrade.requirements)check(tile.build.items.get(stack.item)==23,"native upgrade cost charged once");
        var upgraded=tile.build;var state=LogisticsModelNativeTest.state(upgraded);stream.clear();models.render(upgraded,stream,0,0,0,FpvWorldRenderer.heightFor(upgrade),0);
        check(Arrays.equals(state,LogisticsModelNativeTest.state(upgraded)),"upgraded core rendering preserves native costs/state");
        Vars.state.rules.infiniteResources=true;
    }
}
