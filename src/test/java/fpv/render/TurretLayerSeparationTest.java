package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import mindustry.content.*;
import mindustry.entities.part.RegionPart;import mindustry.world.blocks.defense.turrets.*;import mindustry.world.draw.DrawTurret;
import java.util.*;

/** Native draw callbacks, with each mask on a separate mock texture so streamed faces can be
 * checked independently. Reproduces both liquid/top coplanarity and Cyclone's sliding slabs. */
public final class TurretLayerSeparationTest {
    static int checks,poses;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=32;}}
    static TextureRegion art(String name){var texture=new Tex();var pix=new Pixmap(32,32);pix.fill(0xffffffff);Core.atlas.getPixmaps().put(texture,pix);var r=new TextureAtlas.AtlasRegion(texture,0,0,32,32);r.name=name;return r;}
    static float[] vertices(NativeAtlasMesh.Builder mesh,TextureRegion region)throws Exception{
        var f=mesh.getClass().getDeclaredField("data");f.setAccessible(true);var d=((Map<?,?>)f.get(mesh)).get(region.texture);
        if(d==null)return new float[0];var a=d.getClass().getDeclaredField("values");a.setAccessible(true);var n=d.getClass().getDeclaredField("count");n.setAccessible(true);return Arrays.copyOf((float[])a.get(d),(int)n.get(d));
    }
    static float[] heights(float[] vertices,double[] basis,float pivot){
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(int i=0;i<vertices.length;i+=9){float h=(float)(basis[3]*vertices[i]+basis[4]*(vertices[i+1]-pivot)+basis[5]*vertices[i+2]);min=Math.min(min,h);max=Math.max(max,h);}
        return new float[]{min,max};
    }
    public static void main(String[] args)throws Exception{
        ModelSurfaceRegressionTest.main(args);
        Core.batch=new Batch(){@Override protected void flush(){}@Override protected void draw(Texture t,float[] v,int o,int n){}@Override protected void draw(TextureRegion r,float x,float y,float ox,float oy,float w,float h,float rotation){}};
        var model=new TurretModelRenderer();var mesh=new NativeAtlasMesh.Builder(false);double[] basis=new double[9];
        for(var block:new mindustry.world.Block[]{Blocks.wave,Blocks.tsunami,Blocks.cyclone,Blocks.salvo,Blocks.spectre}){
            var type=(Turret)block;var drawer=(DrawTurret)type.drawer;type.region=art(type.name);drawer.base=art(type.name+"-base");
            drawer.top=art(type.name+"-top");drawer.liquid=art(type.name+"-liquid");drawer.heat=art(type.name+"-heat");
            var parts=new ArrayList<TextureRegion>();
            for(var p:drawer.parts){var r=(RegionPart)p;r.regions=new TextureRegion[]{art(type.name+r.suffix),art(type.name+r.suffix+"-mirror")};r.outline=false;r.heat=r.light=new TextureRegion();parts.add(r.regions[0]);}
            var b=(Turret.TurretBuild)type.buildType.get();b.block=type;b.team=mindustry.game.Team.sharded;b.liquids=new mindustry.world.modules.LiquidModule();b.liquids.add(Liquids.water,type.liquidCapacity);b.heat=1;
            float thickness=TurretModelProfile.forTurret(type).thickness(type.size),pivot;
            for(float base:new float[]{0,7.5f})for(int yaw=0;yaw<360;yaw+=45)for(int pitch:new int[]{-55,0,70})for(float recoil:new float[]{0,1}){
                b.rotation=yaw;b.curRecoil=recoil;b.shootWarmup=recoil;b.recoilOffset.trns(yaw+180,recoil*type.recoil);if(b.curRecoils!=null)Arrays.fill(b.curRecoils,recoil);
                mesh.clear();check(model.render(b,mesh,0,base,0,1.5f,pitch),"native rendering");pivot=base+1.5f+.006f;SpriteExtrusionGeometry.basis(yaw,pitch,0,basis);
                float previous=thickness*.5f;
                for(var region:new TextureRegion[]{drawer.liquid,drawer.top,drawer.heat}){
                    var v=vertices(mesh,region);check(v.length==6*9,"full-mask decal is one roof, without side/bottom faces");
                    var h=heights(v,basis,pivot);check(h[1]-h[0]<.00001,"decal remains planar along pitched mount normal");
                    check(h[0]>previous+.0038f,"native liquid -> top -> heat are strictly separated");previous=h[1];
                }
                if(type==Blocks.cyclone){
                    float previousTop=Float.NEGATIVE_INFINITY,min=0,max=0;
                    for(int i=0;i<parts.size();i++){var h=heights(vertices(mesh,parts.get(i)),basis,pivot);check(h[0]>previousTop+.0038f,"Cyclone overlapping barrel volumes do not intersect at any recoil");previousTop=h[1];if(i==0)min=h[0];max=h[1];}
                    // Native Mathf planar offsets use float lookup trig, while mesh bases use doubles.
                    check(Math.abs(min+thickness*.705f)<.0001&&Math.abs(max+thickness*.055f)<.0001,"Cyclone retains previous combined barrel envelope: "+min+".."+max+" thickness="+thickness+" yaw="+yaw+" pitch="+pitch);
                }
                poses++;
            }
            long builds=model.builds();model.render(b,mesh,0,0,0,1.5f,0);check(builds==model.builds(),"no per-frame mesh generation");
        }
        mesh.dispose();model.clear();System.out.println("TurretLayerSeparationTest: "+checks+" checks, "+poses+" native yaw/pitch/recoil/floor poses PASS");
    }
}
