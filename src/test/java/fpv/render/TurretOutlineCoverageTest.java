package fpv.render;
import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import mindustry.content.Blocks;import mindustry.world.blocks.defense.turrets.Turret;import mindustry.world.draw.DrawTurret;import java.util.*;

/** A one-pixel muzzle tip must not vanish at the coarse alpha-cell coverage threshold. */
public final class TurretOutlineCoverageTest {
    static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
    static class Tex extends Texture{Tex(){super();width=height=98;}}
    public static void main(String[] args)throws Exception{
        ModelSurfaceRegressionTest.main(args);
        var pix=new Pixmap(98,98);pix.fillRect(28,28,40,40,0x8c899aff);pix.set(47,1,0xffffffff);pix.set(94,49,0xffffffff);
        var texture=new Tex();Core.atlas.getPixmaps().put(texture,pix);
        Core.batch=new Batch(){@Override protected void flush(){}@Override protected void draw(Texture texture,float[] vertices,int offset,int count){}@Override protected void draw(TextureRegion r,float x,float y,float ox,float oy,float w,float h,float rotation){}};
        var renderer=new TurretModelRenderer();var batch=new NativeAtlasMesh.Builder(false);int poses=0,coverage=0,cullingDefects=0;
        for(var type:new mindustry.world.Block[]{Blocks.wave,Blocks.tsunami,Blocks.cyclone}){
            var art=new TextureAtlas.AtlasRegion(texture,0,0,98,98);art.name=type.name;type.region=art;
            var drawer=new DrawTurret();drawer.base=art;drawer.top=drawer.liquid=drawer.heat=new TextureRegion();((Turret)type).drawer=drawer;
            var b=(Turret.TurretBuild)type.buildType.get();b.block=type;b.team=mindustry.game.Team.sharded;b.liquids=new mindustry.world.modules.LiquidModule();
            for(int yaw=0;yaw<360;yaw+=45)for(int pitch:new int[]{-50,0,70}){
                b.rotation=yaw;batch.clear();check(renderer.render(b,batch,0,7,0,1.5f,pitch),"native draw capture succeeds");
                var a=LogisticsModelNativeTest.vertices(batch);check(a.length>0,"gun and base emitted");for(float f:a)check(Float.isFinite(f),"finite pitched geometry");poses++;
                int high=0;for(int i=0;i<a.length;i+=9)if(a[i+1]>a[high+1])high=i;
                var camera=new arc.graphics.g3d.Camera3D();camera.resize(100,100);camera.fov=1;camera.near=.04f;camera.far=10;
                camera.position.set(a[high],a[high+1],a[high+2]+5);camera.lookAt(a[high],a[high+1],a[high+2]);camera.update();
                check(BlockVisualBounds.visible(camera,b,0,7,0,1.5f,1.5f),"real gun extrema survive narrow edge-of-frustum view");
                if(!camera.frustum.containsBounds(0,7+.75f,0,type.size,(1.5f+type.size)*.5f,type.size))cullingDefects++;
            }
            var field=TurretModelRenderer.class.getDeclaredField("cache");field.setAccessible(true);var entry=((Map<?,?>)field.get(renderer)).get(art);var shape=entry.getClass().getDeclaredField("shape");shape.setAccessible(true);var a=((SpriteExtrusionGeometry.Result)shape.get(entry)).vertices();
            for(int[] point:new int[][]{{47,1},{94,49},{40,40}}){
                float x=(point[0]+.5f)/98-.5f,z=(point[1]+.5f)/98-.5f;boolean found=false;
                for(int i=0;i<a.length;i+=48)if(a[i+4]>.5f){float xmin=1,xmax=-1,zmin=1,zmax=-1;for(int k=0;k<6;k++){xmin=Math.min(xmin,a[i+k*8]);xmax=Math.max(xmax,a[i+k*8]);zmin=Math.min(zmin,a[i+k*8+2]);zmax=Math.max(zmax,a[i+k*8+2]);}if(x>=xmin&&x<=xmax&&z>=zmin&&z<=zmax){found=true;break;}}
                check(found,"opaque native muzzle/edge centre retained for "+type.name);coverage++;
            }
        }
        check(cullingDefects>0,"reproduced actual old layered frustum bound rejecting visible gun tips");
        check(renderer.entries()==3&&renderer.builds()==3,"72 poses share three cached native silhouettes");batch.dispose();pix.dispose();
        System.out.println("TurretOutlineCoverageTest: "+poses+" yaw/pitch poses, "+coverage+" thin-tip/centre checks and "+cullingDefects+" reproduced old culling failures PASS; native damage and aim untouched");
    }
}

