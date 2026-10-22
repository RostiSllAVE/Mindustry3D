package fpv.render;

import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.graphics.g3d.Camera3D;import arc.mock.*;
import java.lang.reflect.*;import java.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.core.*;import mindustry.game.*;import mindustry.gen.*;

/** Checks actual streamed geometry/materials, not changes to physical building height or state. */
public final class ModelSurfaceRegressionTest {
    static int checks;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static class Tex extends Texture {Tex(){super();width=height=32;}}
    public static void main(String[] args)throws Exception{
        arc.util.ArcNativesLoader.load();Core.gl=new MockGL20();Core.graphics=new MockGraphics();Core.settings=new MockSettings();
        var pix=new Pixmap(32,32);pix.fill(0x8090a0ff);pix.fillRect(12,12,8,8,0);
        var region=new TextureAtlas.AtlasRegion(new Tex(),0,0,32,32);region.name="opaque-metal-alpha-hole";
        Core.atlas=new TextureAtlas(){@Override public PixmapRegion getPixmap(TextureAtlas.AtlasRegion r){return new PixmapRegion(pix);}};Core.atlas.addRegion("white",region);
        var material=MechanicalMaterial.color(region);check(material.r>.4f&&material.b>.5f,"fallback is sampled opaque metal, not transparent black");
        check(material==MechanicalMaterial.color(region),"paint cached between instances");
        var geometry=new MechanicalBlockGeometry.Builder();geometry.box(-.5f,0,-.5f,.5f,1,.5f,true);
        geometry.face(-.5f,0,-.5f,.5f,0,-.5f,.5f,0,.5f,-.5f,0,.5f,0,-1,0);
        float[] template=geometry.finish(),saved=template.clone();var batch=new NativeAtlasMesh.Builder(false);double[] basis=new double[9];SpriteExtrusionGeometry.basis(90,0,0,basis);
        batch.extrusion(region,template,0,7,0,2,1.5f,2,basis,1,1,1);int unfiltered=LogisticsModelNativeTest.vertices(batch).length/9;
        batch.clear();batch.eyeHeight=9;batch.solidExtrusion(region,template,0,7,0,2,1.5f,2,basis,true);var vertices=LogisticsModelNativeTest.vertices(batch);
        check(vertices.length/9==unfiltered-6,"only inward underside omitted above support plane");
        boolean opaqueRoof=false,contactSide=false;for(int i=0;i<vertices.length;i+=9){
            check(vertices[i+1]>=7&&vertices[i+1]<=8.5f,"exact original anchor/envelope retained on elevated support");
            if(vertices[i+6]<-1){opaqueRoof=true;check(vertices[i+1]==8.5f,"opaque fallback only on solid roof");}
            if(vertices[i+1]==7)contactSide=true;
        }
        check(opaqueRoof&&contactSide,"solid metal roof and grounded side faces both present");check(Arrays.equals(saved,template),"immutable template untouched");
        batch.clear();batch.eyeHeight=6;batch.solidExtrusion(region,template,0,7,0,2,1.5f,2,basis,true);check(LogisticsModelNativeTest.vertices(batch).length/9==unfiltered,"complete Free Mode underside retained below support plane");
        batch.clear();batch.eyeHeight=Float.NaN;batch.solidExtrusion(region,template,0,7,0,2,1.5f,2,basis,true);check(LogisticsModelNativeTest.vertices(batch).length/9==unfiltered,"offline builders retain complete geometry");
        var sprite=SpriteExtrusionGeometry.generate(new byte[]{-1},1,1,1,1,32,.2,100).vertices();
        batch.clear();batch.eyeHeight=9;batch.seatedExtrusion(region,sprite,0,7.75f,0,2,1.5f,2,basis,1,1,1);check(LogisticsModelNativeTest.vertices(batch).length/9==30,"centred native base support plane resolved correctly");
        batch.clear();batch.eyeHeight=6;batch.seatedExtrusion(region,sprite,0,7.75f,0,2,1.5f,2,basis,1,1,1);check(LogisticsModelNativeTest.vertices(batch).length/9==36,"native base underside intact from below");
        batch.clear();batch.extrusion(region,template,0,9,0,2,1.5f,2,basis,1,1,1);check(LogisticsModelNativeTest.vertices(batch).length/9==unfiltered,"airborne/ordinary mesh keeps exposed underside");
        for(float h:new float[]{.22f,.88f,1.64f,2.02f})check(TransferBeamPose.endpoint(h)>h*.8f+.04f,"beam clears pad gantry without moving pad");
        check(TransferBeamPose.OUTER>.16f&&TransferBeamPose.CORE>.045f,"stronger bounded beam widths");
        for(boolean send:new boolean[]{true,false})for(int i=0;i<=100;i++){
            float t=TransferBeamPose.tip(i/100f,send),tail=TransferBeamPose.tail(t,send);
            check(t>=0&&t<=64&&tail>=0&&tail<=64,"bounded real-event travel");check(Math.abs(t-tail)<=12.0001,"finite pulse length");
            if(i>0)check(send?t>TransferBeamPose.tip((i-1)/100f,send):t<TransferBeamPose.tip((i-1)/100f,send),"preserved launch/arrival direction");
        }
        var field=new ForceProjectorRenderer();var method=ForceProjectorRenderer.class.getDeclaredMethod("shell",double.class,double.class,double.class,double.class,double.class,int.class,double.class,float.class,boolean.class);method.setAccessible(true);
        method.invoke(field,0d,0d,7d,10d,5d,6,0d,0f,true);
        var data=ForceProjectorRenderer.class.getDeclaredField("data");data.setAccessible(true);var count=ForceProjectorRenderer.class.getDeclaredField("count");count.setAccessible(true);float[] f=(float[])data.get(field);float minimum=100;
        for(int i=0;i<count.getInt(field);i+=7){minimum=Math.min(minimum,f[i+1]);check(f[i+1]>=7.0249f,"no field triangles or lower edge on support plane");check(Math.hypot(f[i],f[i+2])<5.1,"truthful original shield footprint");}
        check(Math.abs(minimum-7.025)<.0001,"small presentation gap, not a moved shield");
        // Read-only culling envelope sees the gun above a high construction plane.
        Vars.headless=true;Core.app=new MockApplication();Core.bundle=arc.util.I18NBundle.createEmptyBundle();Core.audio=new MockAudio();
        mindustry.graphics.CacheLayer.init();Vars.tree=new mindustry.core.FileTree();Vars.state=new GameState();Vars.net=new mindustry.net.Net(null);
        Groups.init();Vars.content=new ContentLoader();Vars.content.createBaseContent();var b=Blocks.wave.buildType.get();b.block=Blocks.wave;
        var camera=new Camera3D();camera.resize(960,600);camera.fov=60;camera.near=.04f;camera.far=100;camera.position.set(0,30,8);camera.lookAt(0,31,0);camera.update();
        check(BlockVisualBounds.visible(camera,b,0,30,0,1.5f,1.5f),"upper-floor pitched gun envelope visible");
        check(!BlockVisualBounds.visible(camera,b,500,30,0,1.5f,1.5f),"offscreen culling remains effective");
        batch.dispose();pix.dispose();System.out.println("ModelSurfaceRegressionTest: "+checks+" opaque paint/foundation/beam/shield/culling checks PASS; no physical acceptance");
    }
}
