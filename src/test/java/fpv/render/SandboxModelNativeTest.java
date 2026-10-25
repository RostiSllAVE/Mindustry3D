package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.mock.*;import arc.util.*;
import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;

/** Every requested native utility: cached geometry, floor contact and read-only state. */
public final class SandboxModelNativeTest {
    static int checks,poses;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=64;}}
    public static void main(String[] args)throws Exception{
        ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();LayerCampaignNativeTest.main(args);
        Core.gl=new MockGL20();Core.graphics=new MockGraphics();Core.settings=new MockSettings();
        var pix=new Pixmap(32,32);pix.fill(0x92909aff);
        var art=new TextureAtlas.AtlasRegion(new Tex(),0,0,32,32);art.name="utility-fixture";Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion region){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",art);
        var models=new SandboxModelRenderer();var out=new NativeAtlasMesh.Builder(false);var registry=new LayerBuildingAdapter(null);long id=80000;
        for(var p:SandboxModelProfile.values()){
            registry.clear();
            var block=p.block();check(block!=null&&SandboxModelRenderer.supports(block),"native definition "+p);block.region=block.fullIcon=art;
            var raw=SandboxModelGeometry.model(p,0);check(raw==SandboxModelGeometry.model(p,0),"shared immutable geometry");
            check(raw.vertices()>=24,"volumetric model "+p);
            for(int floor:new int[]{0,1,3,5}){
                Building b;if(floor==0){Vars.world.tile(20,20).setBlock(block,Team.sharded,0);b=Vars.world.build(20,20);}else b=registry.create(id++,20,20,floor,block,Team.sharded,0);
                check(b!=null,"eligible upper native block "+p);byte[] state=LogisticsModelNativeTest.state(b);int actors=Groups.all.size();
                for(int lod:new int[]{0,900,10000}){out.clear();float base=floor*1.4f,h=FpvWorldRenderer.heightFor(block);check(models.render(b,out,20,base,-20,h,lod,null),"renderer supports "+p);var v=LogisticsModelNativeTest.vertices(out);
                    for(int i=0;i<v.length;i+=9){check(Float.isFinite(v[i]+v[i+1]+v[i+2]),"finite vertices");check(v[i+1]>=base-.001f,"no below-floor geometry");}
                    check(java.util.Arrays.equals(state,LogisticsModelNativeTest.state(b))&&actors==Groups.all.size(),"render cannot change native state or actors");poses++;
                }
            }
        }
        check(models.builds()==14,"one model build per native block, reused across instances/floors");
        System.out.println("SandboxModelNative: "+checks+" assertions; "+poses+" poses; cache bytes="+models.bytes()+"; 14 profiles PASS");registry.clear();models.clear();
    }
}
