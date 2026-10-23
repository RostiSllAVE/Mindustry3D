package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;
import mindustry.world.blocks.units.*;import fpv.construction.*;import java.util.*;

/** Native factory state + streamed geometry. Rendering must never manufacture or spend items. */
public final class UnitProductionNativeTest {
    static int checks,poses;static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();LayerCampaignNativeTest.main(args);
        Core.gl=new MockGL20();Core.graphics=new MockGraphics();var pix=new Pixmap(32,32);pix.fill(0x9999aaff);pix.fillRect(12,12,8,8,0);
        var art=new TextureAtlas.AtlasRegion(new Tex(),0,0,32,32);art.name="factory-fixture";
        Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion r){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",art);
        Block[] types={Blocks.groundFactory,Blocks.airFactory,Blocks.navalFactory,Blocks.tankFabricator,Blocks.shipFabricator,Blocks.mechFabricator};
        var renderer=new UnitProductionRenderer();var stream=new NativeAtlasMesh.Builder(false);var owner=new LayerBuildingAdapter(null);long id=7000;
        var hashes=new HashSet<Integer>();
        try{
            for(var block:types){
                var type=(UnitFactory)block;check(type.size==3&&type.rotate,"actual native footprint and orientation");
                int hp=type.health;var plans=type.plans;var requirements=type.requirements;
                type.region=type.topRegion=type.outRegion=type.fullIcon=art;
                var unitArt=new TextureRegion(art);unitArt.width=unitArt.height=64;
                for(var p:type.plans)p.unit.fullIcon=unitArt;
                var profile=UnitProductionProfile.forBlock(type);
                for(int lod=0;lod<3;lod++){
                    var g=UnitProductionGeometry.model(profile,lod);check(g==UnitProductionGeometry.model(profile,lod),"immutable geometry shared between instances");
                    if(lod==0)check(hashes.add(Arrays.hashCode(g.body())),"six distinct production silhouettes");
                    for(var a:new float[][]{g.body(),g.lining(),g.lights(),g.tools(),g.carriages()[0],g.carriages()[1]})for(int i=0;i<a.length;i+=8){
                        check(Math.abs(a[i])<=.491f&&Math.abs(a[i+2])<=.491f&&a[i+1]>=0&&a[i+1]<=1,"model fits original XYZ envelope");
                        check(!(a[i]>.32f&&Math.abs(a[i+2])<.20f&&a[i+1]>.22f),"native exit corridor is open above deck");
                    }
                }
                for(int floor:new int[]{0,1,3,5})for(int rotation=0;rotation<4;rotation++){
                    owner.clear();UnitFactory.UnitFactoryBuild b;
                    if(floor==0){Vars.world.tile(20,20).setBlock(type,Team.sharded,rotation);b=(UnitFactory.UnitFactoryBuild)Vars.world.build(20,20);}
                    else b=(UnitFactory.UnitFactoryBuild)owner.create(id++,20,20,floor,type,Team.sharded,rotation);
                    b.currentPlan=0;b.progress=type.plans.first().time*.45f;b.time=71;b.speedScl=1;
                    for(int state=0;state<5;state++){
                        b.enabled=state!=1;b.efficiency=state==2?0:1;b.currentPlan=state==3?-1:0;
                        b.payload=state==4?new mindustry.world.blocks.payloads.UnitPayload(type.plans.first().unit.create(Team.sharded)):null;
                        if(b.payload!=null){b.payVector.trns(b.rotdeg(),12);b.updatePayload();}
                        check(UnitProductionRenderer.activity(b)==(state==0?1:0),"no machinery at full power while disabled, starved, unconfigured or output-blocked");
                        var before=LogisticsModelNativeTest.state(b);float progress=b.progress,time=b.time;int units=Groups.unit.size(),builds=Groups.build.size();
                        for(float distance:new float[]{0,900,10000}){
                            float h=FpvWorldRenderer.heightFor(type),base=floor*2.5f,x=b.x/8,z=-b.y/8;stream.clear();stream.eyeHeight=base+h+2;
                            check(renderer.render(b,stream,x,base,z,h,distance),"native and upper factory render");var vertices=LogisticsModelNativeTest.vertices(stream);check(vertices.length>0,"real body volume");
                            for(int i=0;i<vertices.length;i+=9){
                                for(int j=0;j<9;j++)check(Float.isFinite(vertices[i+j]),"finite XYZ/UV/material");
                                float extent=state==4?3:1.501f;
                                check(Math.abs(vertices[i]-x)<=extent&&Math.abs(vertices[i+2]-z)<=extent,"body stays in footprint; native outgoing payload may overhang");
                                check(vertices[i+1]>=base-.001&&vertices[i+1]<=base+h+.009,"seated floor and bounded height");
                                check(vertices[i+7]>=0&&vertices[i+7]<=.5&&vertices[i+8]>=0&&vertices[i+8]<=.5,"atlas UVs do not bleed");
                            }
                            if(state==0&&rotation==0&&distance==0){
                                stream.clear();stream.eyeHeight=base-1;renderer.render(b,stream,x,base,z,h,distance);
                                check(LogisticsModelNativeTest.vertices(stream).length==vertices.length+54,"only inward foundation bottom culled from above");
                                var camera=new arc.graphics.g3d.Camera3D();camera.resize(100,100);camera.position.set(x,base+h,z+6);camera.lookAt(x,base+h,z);camera.update();
                                check(BlockVisualBounds.visible(camera,b,x,base,z,h,h)&&!BlockVisualBounds.visible(camera,b,10000,base,0,h,h),"complete visual envelope culls safely");
                            }
                            if(state==4&&rotation==0&&distance==0){
                                var camera=new arc.graphics.g3d.Camera3D();camera.resize(100,100);camera.fov=.05f;camera.near=.04f;camera.far=10;
                                float tip=x+2.49f,deck=base+h*UnitProductionGeometry.DECK+.022f;
                                camera.position.set(tip,deck,z+5);camera.lookAt(tip,deck,z);camera.update();
                                check(BlockVisualBounds.visible(camera,b,x,base,z,h,h),"outgoing native payload tip is not culled at screen edge");
                            }
                            poses++;
                        }
                        check(Arrays.equals(before,LogisticsModelNativeTest.state(b))&&b.progress==progress&&b.time==time,"drawing cannot change costs, payload, progress, config or save state");
                        check(units==Groups.unit.size()&&builds==Groups.build.size(),"no fake actors or duplicate production");
                    }
                    b.payload=null;b.currentPlan=0;b.enabled=true;b.efficiency=1;
                    b.time=0;float first=UnitProductionRenderer.carriage(b,0);b.time=50;check(first!=UnitProductionRenderer.carriage(b,0),"native clock animates carriages");
                    check(Math.abs(UnitProductionRenderer.carriage(b,0)+UnitProductionRenderer.carriage(b,1))<.0001,"opposed assembly motion");
                    b.time=Float.NaN;check(Float.isFinite(UnitProductionRenderer.carriage(b,0)),"defensive finite native clock");
                }
                check(type.health==hp&&type.plans==plans&&type.requirements==requirements,"host balance and recipes unchanged");
            }
            check(renderer.builds()==6&&hashes.size()==6,"one cache entry per factory, no per-frame generation");
            for(var excluded:new Block[]{Blocks.additiveReconstructor,Blocks.tankAssembler,Blocks.constructor,Blocks.coreNucleus})check(!UnitProductionRenderer.supports(excluded),"other model families unchanged");
            System.out.println("UnitProductionNativeTest: "+checks+" geometry/XYZ/state/UV/cache checks, "+poses+" poses PASS");
        }finally{owner.clear();renderer.clear();stream.dispose();pix.dispose();}
    }
}
