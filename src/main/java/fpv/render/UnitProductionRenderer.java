package fpv.render;

import arc.Core;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.units.UnitFactory;
import java.util.IdentityHashMap;

/** Read-only assembly presentation. The native factory owns all progress, costs and releases. */
final class UnitProductionRenderer {
    private static final class Model {TextureRegion region,top;final float[][][] parts=new float[3][][];final float[][] roofs=new float[3][];}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private final NativeBlockAnimationAdapter contents=new NativeBlockAnimationAdapter();
    private final double[] basis=new double[9];private Object atlas;private long builds,bytes;
    static boolean supports(Block b){return UnitProductionProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}void clear(){cache.clear();contents.clear();atlas=null;builds=bytes=0;}
    static float activity(UnitFactory.UnitFactoryBuild b){
        return !b.dead&&b.health>0&&b.enabled&&b.efficiency>0&&Float.isFinite(b.efficiency)&&
            b.currentPlan>=0&&b.currentPlan<((UnitFactory)b.block).plans.size&&b.shouldConsume()?SupportModelRenderer.finite(b.speedScl):0;
    }
    static float carriage(UnitFactory.UnitFactoryBuild b,int side){
        // The native production clock stops when starved/disabled. No independent wall-clock motion.
        float t=Float.isFinite(b.time)?b.time:0;return activity(b)*.15f*Mathf.sin(t*.025f+side*Mathf.PI);
    }
    boolean render(Building building,NativeAtlasMesh.Builder out,float x,float base,float z,float h,float distance){
        if(!(building instanceof UnitFactory.UnitFactoryBuild b))return false;
        var p=UnitProductionProfile.forBlock(b.block);if(p==null)return false;if(b.dead||!(b.health>0))return true;
        if(!NativeAtlasMesh.valid(b.block.region))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        var type=(UnitFactory)b.block;var m=cache.get(type);
        if(m==null||m.region!=type.region||m.top!=type.topRegion){m=create(type,p);cache.put(type,m);builds++;}
        int lod=ManufacturingModelProfile.lod(distance);float size=type.size;var parts=m.parts[lod];
        SpriteExtrusionGeometry.basis(90+b.rotdeg(),0,0,basis);
        out.solidExtrusion(m.region,parts[0],x,base,z,size,h,size,basis,true);
        out.extrusion(m.region,parts[1],x,base,z,size,h,size,basis,.24f,.25f,.28f);
        if(NativeAtlasMesh.valid(m.top))out.extrusion(m.top,m.roofs[lod],x,base+.004f,z,size,h,size,basis,1,1,1);
        // Same original assembly sprite/payload, clipped by native progress. No fake unit entities.
        contents.renderFactoryContents(b,out,x,base+h*UnitProductionGeometry.DECK+.012f,z,size);
        float active=activity(b),clock=Float.isFinite(b.time)?b.time:0;
        float pulse=.08f+active*(.64f+.12f*Mathf.sin(clock*.09f));
        float r=p.erekir()?1:.52f,g=p.erekir()?.61f:.77f,bl=p.erekir()?.27f:1;
        out.extrusion(Core.atlas.white(),parts[2],x,base,z,size,h,size,basis,r*pulse,g*pulse,bl*pulse);
        if(lod<2){
            for(int side=0;side<2;side++){
                float along=carriage(b,side)*size;
                out.solidExtrusion(m.region,parts[3+side],x+(float)basis[0]*along,base,z+(float)basis[2]*along,size,h,size,basis,false);
            }
            out.extrusion(Core.atlas.white(),parts[5],x,base,z,size,h,size,basis,r*active,g*active,bl*active);
        }
        return true;
    }
    private Model create(UnitFactory type,UnitProductionProfile profile){
        var m=new Model();m.region=type.region;m.top=type.topRegion;
        float u=.14f,v=.14f;
        if(m.region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){
            double best=Double.POSITIVE_INFINITY;for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>=200){float a=x/(float)Math.max(1,pix.width-1),b=y/(float)Math.max(1,pix.height-1);double d=(a-.14)*(a-.14)+(b-.14)*(b-.14);if(d<best){best=d;u=a;v=b;}}
        }}
        for(int lod=0;lod<3;lod++){
            var raw=UnitProductionGeometry.model(profile,lod);
            m.parts[lod]=new float[][]{material(raw.body(),u,v,false),material(raw.lining(),u,v,false),material(raw.lights(),u,v,true),material(raw.carriages()[0],u,v,false),material(raw.carriages()[1],u,v,false),material(raw.tools(),u,v,true)};
            int count=0;for(int i=0;i<raw.body().length;i+=8)if(raw.body()[i+4]>.5f&&raw.body()[i+1]>UnitProductionGeometry.DECK+.01f)count+=8;
            var roof=m.roofs[lod]=new float[count];count=0;for(int i=0;i<raw.body().length;i+=8)if(raw.body()[i+4]>.5f&&raw.body()[i+1]>UnitProductionGeometry.DECK+.01f){System.arraycopy(raw.body(),i,roof,count,8);count+=8;}
            for(var a:m.parts[lod])bytes+=(long)a.length*4;bytes+=(long)roof.length*4;
        }
        return m;
    }
    private static float[] material(float[] a,float u,float v,boolean white){var out=a.clone();for(int i=0;i<out.length;i+=8)if(white||out[i+6]<0){out[i+6]=white?.5f:u;out[i+7]=white?.5f:v;}return out;}
}
