package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;
import mindustry.world.blocks.units.*;import fpv.construction.*;import java.util.*;

/** Native factory state + streamed geometry. Rendering must never manufacture or spend items. */
public final class UnitUpgradeNativeTest {
    static int checks,poses;static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();LayerCampaignNativeTest.main(args);
        Core.gl=new MockGL20();Core.graphics=new MockGraphics();var pix=new Pixmap(32,32);pix.fill(0x9999aaff);pix.fillRect(12,12,8,8,0);
        var art=new TextureAtlas.AtlasRegion(new Tex(),0,0,32,32);art.name="factory-fixture";
        Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion r){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",art);
        Block[] types={Blocks.additiveReconstructor,Blocks.multiplicativeReconstructor,Blocks.exponentialReconstructor,Blocks.tetrativeReconstructor,Blocks.tankRefabricator,Blocks.shipRefabricator,Blocks.mechRefabricator,Blocks.primeRefabricator};
        var renderer=new UnitUpgradeRenderer();var stream=new NativeAtlasMesh.Builder(false);var owner=new LayerBuildingAdapter(null);long id=7000;
        var hashes=new HashSet<Integer>();
        try{
            for(var block:types){
                var type=(Reconstructor)block;check(type.size>=3&&type.size<=9&&type.rotate,"actual native footprint and orientation");
                int hp=type.health;var plans=type.upgrades;var requirements=type.requirements;
                type.region=type.topRegion=type.outRegion=type.fullIcon=art;
                var unitArt=new TextureRegion(art);unitArt.width=unitArt.height=64;
                for(var p:type.upgrades){p[0].fullIcon=unitArt;p[1].fullIcon=unitArt;}
                var profile=UnitUpgradeProfile.forBlock(type);
                for(int lod=0;lod<3;lod++){
                    var g=UnitUpgradeGeometry.model(profile,type.size,lod);check(g==UnitUpgradeGeometry.model(profile,type.size,lod),"immutable geometry shared between instances");
                    if(lod==0)check(hashes.add(Arrays.hashCode(g.body())),"eight distinct upgrade silhouettes");
                    for(var a:new float[][]{g.body(),g.lining(),g.lights(),g.tools(),g.carriages()[0],g.carriages()[1]})for(int i=0;i<a.length;i+=8){
                        check(Math.abs(a[i])<=.491f&&Math.abs(a[i+2])<=.491f&&a[i+1]>=0&&a[i+1]<=1,"model fits original XYZ envelope");
                        check(!(a[i]>.32f&&Math.abs(a[i+2])<.20f&&a[i+1]>.22f),"native exit corridor is open above deck");
                    }
                }
                for(int floor:new int[]{0,1,3,5})for(int rotation=0;rotation<4;rotation++){
                    owner.clear();Reconstructor.ReconstructorBuild b;
                    if(floor==0){Vars.world.tile(20,20).setBlock(type,Team.sharded,rotation);b=(Reconstructor.ReconstructorBuild)Vars.world.build(20,20);}
                    else b=(Reconstructor.ReconstructorBuild)owner.create(id++,20,20,floor,type,Team.sharded,rotation);
                    b.payload=new mindustry.world.blocks.payloads.UnitPayload(type.upgrades.first()[0].create(Team.sharded));b.payVector.setZero();b.efficiency=0;b.updateTile();b.progress=type.constructTime*.45f;b.time=71;b.speedScl=1;
                    for(int state=0;state<5;state++){
                        b.payload=new mindustry.world.blocks.payloads.UnitPayload(type.upgrades.first()[0].create(Team.sharded));b.payVector.setZero();b.efficiency=0;b.updateTile();b.progress=type.constructTime*.45f;b.time=71;b.speedScl=1;
                        b.enabled=state!=1;b.efficiency=state==2?0:1;
                        if(state==3)b.payload=null;
                        if(state==4)b.payVector.trns(b.rotdeg(),type.size*4f);
                        if(b.payload!=null)b.updatePayload();
                        check(UnitUpgradeRenderer.activity(b)==(state==0?1:0),"actual enabled, supplied, arrived upgrade state");
                        var before=LogisticsModelNativeTest.state(b);float progress=b.progress,time=b.time;int units=Groups.unit.size(),builds=Groups.build.size();
                        for(float distance:new float[]{0,900,10000}){
                            float h=FpvWorldRenderer.heightFor(type),base=floor*2.5f,x=b.x/8,z=-b.y/8;stream.clear();stream.eyeHeight=base+h+2;
                            check(renderer.render(b,stream,x,base,z,h,distance),"native and upper factory render");var vertices=LogisticsModelNativeTest.vertices(stream);check(vertices.length>0,"real body volume");
                            for(int i=0;i<vertices.length;i+=9){
                                for(int j=0;j<9;j++)check(Float.isFinite(vertices[i+j]),"finite XYZ/UV/material");
                                float extent=state==4?type.size:type.size*.501f;
                                check(Math.abs(vertices[i]-x)<=extent&&Math.abs(vertices[i+2]-z)<=extent,"body stays in footprint; native outgoing payload may overhang");
                                check(vertices[i+1]>=base-.001&&vertices[i+1]<=base+h+.009,"seated floor and bounded height");
                                check(vertices[i+7]>=0&&vertices[i+7]<=.5&&vertices[i+8]>=0&&vertices[i+8]<=.5,"atlas UVs do not bleed");
                            }
                            if(state==0&&rotation==0&&distance==0){
                                stream.clear();stream.eyeHeight=base-1;renderer.render(b,stream,x,base,z,h,distance);
                                check(LogisticsModelNativeTest.vertices(stream).length==vertices.length+54,"only inward foundation bottom culled from above");
                                var camera=new arc.graphics.g3d.Camera3D();camera.resize(100,100);camera.position.set(x,base+h,z+6);camera.lookAt(x,base+h,z);camera.update();
                                check(BlockVisualBounds.visible(camera,b,x,base,z,h,h),"model and real payload are visible");camera.position.add(10000,0,0);camera.lookAt(x+10000,base+h,z);camera.update();check(!BlockVisualBounds.visible(camera,b,x,base,z,h,h),"offscreen model AND payload are culled");
                            }
                            if(state==4&&rotation==0&&distance==0&&type.size==3){
                                var camera=new arc.graphics.g3d.Camera3D();camera.resize(100,100);camera.fov=.05f;camera.near=.04f;camera.far=10;
                                float tip=x+2.49f,deck=UnitAssemblyParts.surface(base,h)+.01f;
                                camera.position.set(tip,deck,z+5);camera.lookAt(tip,deck,z);camera.update();
                                check(BlockVisualBounds.visible(camera,b,x,base,z,h,h),"outgoing native payload tip is not culled at screen edge");
                            }
                            poses++;
                        }
                        check(Arrays.equals(before,LogisticsModelNativeTest.state(b))&&b.progress==progress&&b.time==time,"drawing cannot change costs, payload, progress, config or save state");
                        check(units==Groups.unit.size()&&builds==Groups.build.size(),"no fake actors or duplicate production");
                    }
                    b.payload=new mindustry.world.blocks.payloads.UnitPayload(type.upgrades.first()[0].create(Team.sharded));b.payVector.setZero();b.efficiency=0;b.updateTile();b.speedScl=1;b.enabled=true;b.efficiency=1;
                    b.time=0;float first=UnitUpgradeRenderer.carriage(b,0);b.time=50;check(first!=UnitUpgradeRenderer.carriage(b,0),"native clock animates carriages");
                    check(Math.abs(UnitUpgradeRenderer.carriage(b,0)+UnitUpgradeRenderer.carriage(b,1))<.0001,"opposed assembly motion");
                    b.time=Float.NaN;check(Float.isFinite(UnitUpgradeRenderer.carriage(b,0)),"defensive finite native clock");
                }
                check(type.health==hp&&type.upgrades==plans&&type.requirements==requirements,"host balance and recipes unchanged");
            }
            check(renderer.builds()==8&&hashes.size()==8,"one cache entry per factory, no per-frame generation");
            for(var excluded:new Block[]{Blocks.groundFactory,Blocks.tankAssembler,Blocks.constructor,Blocks.coreNucleus})check(!UnitUpgradeRenderer.supports(excluded),"other model families unchanged");
            System.out.println("UnitUpgradeNativeTest: "+checks+" geometry/XYZ/state/UV/cache checks, "+poses+" poses PASS");
        }finally{owner.clear();renderer.clear();stream.dispose();pix.dispose();}
    }
}
