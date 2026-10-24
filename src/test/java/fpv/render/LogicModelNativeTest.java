package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;
import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.world.blocks.logic.*;
import java.util.*;

/** Native definitions/state, floor contact, cache and screen-space geometry. GPU pixels tested separately. */
public final class LogicModelNativeTest {
    static int checks,poses;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();LayerCampaignNativeTest.main(args);Vars.logicVars=new mindustry.logic.GlobalVars();Vars.logicVars.init();
        Core.gl=new MockGL20();Core.graphics=new MockGraphics();var pix=new Pixmap(32,32);pix.fill(0x92909aff);
        var art=new TextureAtlas.AtlasRegion(new Tex(),0,0,32,32);art.name="logic-fixture";
        Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion ignored){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",art);
        var renderer=new LogicModelRenderer();var stream=new NativeAtlasMesh.Builder(false);var registry=new LayerBuildingAdapter(null);long id=9000;
        Set<Integer> shapes=new HashSet<>();
        try{
            for(var profile:LogicModelProfile.values()){
                Block type=profile.block();int hp=type.health;var recipe=type.requirements;type.region=type.fullIcon=art;if(type instanceof SwitchBlock sw)sw.onRegion=art;
                check(!type.rotate,"native non-rotating logic orientation retained");
                var raw=LogicModelGeometry.model(profile,0);shapes.add(Arrays.hashCode(raw.body()));
                check(raw==LogicModelGeometry.model(profile,0),"immutable template reused across instances");
                if(profile.display()){
                    float half=LogicModelGeometry.screenWidth(profile)/type.size*.5f,depth=half*(float)Math.cos(LogicModelGeometry.pitch(profile));
                    var body=raw.body();for(int i=0;i<body.length;i+=24)if(body[i+4]>.5f&&body[i+1]>.2f){
                        float x=(body[i]+body[i+8]+body[i+16])/3,z=(body[i+2]+body[i+10]+body[i+18])/3;
                        check(Math.abs(x)>=half-.0001f||Math.abs(z)>=depth-.0001f,"bezel is open under live pixels, not a z-fighting metal roof");
                    }
                }
                for(int layer:new int[]{0,1,3,5}){
                    Building b;if(layer==0){Vars.world.tile(20,20).setBlock(type,Team.sharded,0);b=Vars.world.build(20,20);}else b=registry.create(id++,20,20,layer,type,Team.sharded,0);
                    if(b instanceof LogicBlock.LogicBuild p)p.updateCode("set a 7\nend");
                    for(boolean enabled:new boolean[]{true,false}){
                        b.enabled=enabled;byte[] saved=LogisticsModelNativeTest.state(b);int actors=Groups.all.size();
                        for(float distance:new float[]{0,900,10000}){
                            float base=layer*2.5f,h=FpvWorldRenderer.heightFor(type),x=b.x/8,z=-b.y/8;stream.clear();stream.eyeHeight=base+h+1;
                            check(renderer.render(b,stream,x,base,z,h,distance,null),"supported native building renders");var v=LogisticsModelNativeTest.vertices(stream);
                            check(v.length>54,"volumetric geometry");
                            for(int i=0;i<v.length;i+=9){
                                for(int k=0;k<9;k++)check(Float.isFinite(v[i+k]),"finite vertex/UV/material");
                                check(Math.abs(v[i]-x)<=type.size*.501f&&Math.abs(v[i+2]-z)<=type.size*.501f,"footprint preserved");
                                check(v[i+1]>=base-.0001f&&v[i+1]<=base+h+.02f,"seated/bounded body");
                                check(v[i+7]>=0&&v[i+7]<=.5f&&v[i+8]>=0&&v[i+8]<=.5f,"atlas bounds/insets");
                            }
                            if(profile.display()){
                                stream.clear();LogicModelRenderer.screen(stream,art,profile,0,base,0,h,type.size*.8f,.013f);var face=LogisticsModelNativeTest.vertices(stream);
                                float width=distance(face,0,9),depth=distance(face,9,18);
                                check(Math.abs(width-depth)<.0001,"square content has equal world-space surface dimensions");
                                check(face[1]>face[19]&&face[2]<face[20],"north/top edge above south; no X/Y inversion");
                                check(face[6]==-.5f&&face[15]==-.5f,"live screen uses emissive material only");
                            }
                            poses++;
                        }
                        check(Arrays.equals(saved,LogisticsModelNativeTest.state(b))&&actors==Groups.all.size(),"render does not execute logic, spend, change saves or spawn actors");
                    }
                    if(b instanceof SwitchBlock.SwitchBuild){stream.clear();b.enabled=true;renderer.render(b,stream,0,0,0,FpvWorldRenderer.heightFor(type),0,null);var on=LogisticsModelNativeTest.vertices(stream);stream.clear();b.enabled=false;renderer.render(b,stream,0,0,0,FpvWorldRenderer.heightFor(type),0,null);check(!Arrays.equals(on,LogisticsModelNativeTest.vertices(stream)),"real ON/OFF changes geometry and indicator");}
                }
                check(type.health==hp&&type.requirements==recipe,"no balance/recipe changes");
                registry.clear();
            }
            check(renderer.builds()==9&&shapes.size()==9,"nine distinct bodies; one material build per type across floors");
            for(Block other:new Block[]{Blocks.worldProcessor,Blocks.canvas,Blocks.tileLogicDisplay,Blocks.coreNucleus,Blocks.tankFabricator})check(!LogicModelRenderer.supports(other),"explicit model scope; old families untouched");
            System.out.println("LogicModelNativeTest: "+checks+" native/state/geometry/UV/cache checks, "+poses+" poses PASS");
        }finally{renderer.clear();registry.clear();stream.dispose();pix.dispose();}
    }
    static float distance(float[] v,int a,int b){float x=v[a]-v[b],y=v[a+1]-v[b+1],z=v[a+2]-v[b+2];return (float)Math.sqrt(x*x+y*y+z*z);}
}
