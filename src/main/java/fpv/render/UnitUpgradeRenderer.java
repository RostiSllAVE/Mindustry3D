package fpv.render;

import arc.Core;import arc.graphics.g2d.*;import arc.math.Mathf;
import mindustry.gen.Building;import mindustry.world.Block;import mindustry.world.blocks.units.Reconstructor;
import java.util.IdentityHashMap;

/** Native upgrade state is read, never advanced by rendering. No unit replacement or spending. */
final class UnitUpgradeRenderer {
    private static final class Model {TextureRegion region,top;final float[][][] parts=new float[3][][];final float[][] roofs=new float[3][];}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private final NativeBlockAnimationAdapter contents=new NativeBlockAnimationAdapter();
    private final double[] basis=new double[9];private Object atlas;private long builds,bytes;
    static boolean supports(Block b){return UnitUpgradeProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}void clear(){cache.clear();contents.clear();atlas=null;builds=bytes=0;}
    static float activity(Reconstructor.ReconstructorBuild b){return !b.dead&&b.health>0&&b.enabled&&b.efficiency>0&&Float.isFinite(b.efficiency)&&b.constructing()&&b.hasArrived()&&b.shouldConsume()?SupportModelRenderer.finite(b.speedScl):0;}
    static float carriage(Reconstructor.ReconstructorBuild b,int side){float t=Float.isFinite(b.time)?b.time:0;return activity(b)*.10f*Mathf.sin(t*.022f+side*Mathf.PI);}
    boolean render(Building building,NativeAtlasMesh.Builder out,float x,float base,float z,float h,float distance){
        if(!(building instanceof Reconstructor.ReconstructorBuild b))return false;
        var p=UnitUpgradeProfile.forBlock(b.block);if(p==null)return false;if(b.dead||!(b.health>0))return true;
        if(!NativeAtlasMesh.valid(b.block.region))return false;if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        var type=(Reconstructor)b.block;var m=cache.get(type);
        if(m==null||m.region!=type.region||m.top!=type.topRegion){m=create(type,p);cache.put(type,m);builds++;}
        int lod=ManufacturingModelProfile.lod(distance);float size=type.size;var parts=m.parts[lod];
        SpriteExtrusionGeometry.basis(90+b.rotdeg(),0,0,basis);
        out.solidExtrusion(m.region,parts[0],x,base,z,size,h,size,basis,true);
        out.extrusion(m.region,parts[1],x,base,z,size,h,size,basis,.24f,.25f,.28f);
        if(NativeAtlasMesh.valid(m.top))out.extrusion(m.top,m.roofs[lod],x,base+.006f,z,size,h,size,basis,1,1,1);
        contents.renderAssemblyContents(b,out,x,UnitAssemblyParts.surface(base,h),z,size);
        float active=activity(b),clock=Float.isFinite(b.time)?b.time:0,pulse=.08f+active*(.64f+.12f*Mathf.sin(clock*.09f));
        float r=p.erekir()?1:.52f,g=p.erekir()?.61f:.77f,bl=p.erekir()?.27f:1;
        out.extrusion(Core.atlas.white(),parts[2],x,base,z,size,h,size,basis,r*pulse,g*pulse,bl*pulse);
        if(lod<2)for(int side=0;side<2;side++){
            float along=carriage(b,side)*size,dx=(float)basis[0]*along,dz=(float)basis[2]*along;
            out.solidExtrusion(m.region,parts[3+side],x+dx,base,z+dz,size,h,size,basis,false);
            out.extrusion(Core.atlas.white(),parts[5+side],x+dx,base,z+dz,size,h,size,basis,r*active,g*active,bl*active);
        }
        return true;
    }
    private Model create(Reconstructor type,UnitUpgradeProfile p){
        var m=new Model();m.region=type.region;m.top=type.topRegion;float u=.14f,v=.14f;
        if(m.region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){
            double best=Double.POSITIVE_INFINITY;for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>=200){float a=x/(float)Math.max(1,pix.width-1),b=y/(float)Math.max(1,pix.height-1);double d=(a-.14)*(a-.14)+(b-.14)*(b-.14);if(d<best){best=d;u=a;v=b;}}
        }}
        for(int lod=0;lod<3;lod++){
            var raw=UnitUpgradeGeometry.model(p,type.size,lod);
            m.parts[lod]=new float[][]{material(raw.body(),u,v,false),material(raw.lining(),u,v,false),material(raw.lights(),u,v,true),material(raw.carriages()[0],u,v,false),material(raw.carriages()[1],u,v,false),material(UnitProductionRenderer.toolSide(raw.tools(),0),u,v,true),material(UnitProductionRenderer.toolSide(raw.tools(),1),u,v,true)};
            int n=0;for(int i=0;i<raw.body().length;i+=8)if(raw.body()[i+4]>.5f&&raw.body()[i+1]>UnitAssemblyParts.BAY+.01f)n+=8;
            var roof=m.roofs[lod]=new float[n];n=0;for(int i=0;i<raw.body().length;i+=8)if(raw.body()[i+4]>.5f&&raw.body()[i+1]>UnitAssemblyParts.BAY+.01f){System.arraycopy(raw.body(),i,roof,n,8);n+=8;}
            for(var a:m.parts[lod])bytes+=(long)a.length*4;bytes+=(long)roof.length*4;
        }
        return m;
    }
    private static float[] material(float[] a,float u,float v,boolean white){var out=a.clone();for(int i=0;i<out.length;i+=8)if(white||out[i+6]<0){out[i+6]=white?.5f:u;out[i+7]=white?.5f:v;}return out;}
}
