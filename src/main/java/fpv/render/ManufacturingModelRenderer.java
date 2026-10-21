package fpv.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.*;
import java.util.IdentityHashMap;

/** Factory presentation only; the existing native crafter owns progress, consumers and output. */
final class ManufacturingModelRenderer {
    private static final class Model {
        TextureRegion region,flame;Color flameColor;int size;
        final float[][][] parts=new float[3][][],heads=new float[3][][],rods=new float[3][][];
    }
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private final double[] basis=new double[9],rotorBasis=new double[9];private Object atlas;private long builds,bytes;
    static boolean supports(Block b){return ManufacturingModelProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}
    void clear(){cache.clear();atlas=null;builds=bytes=0;}
    static float activity(GenericCrafter.GenericCrafterBuild b){return b.enabled&&b.efficiency>0&&b.shouldConsume()?finite(b.warmup):0;}
    static float heat(GenericCrafter.GenericCrafterBuild b){return finite(b.warmup);}
    private static float finite(float x){return Float.isFinite(x)?Mathf.clamp(x):0;}
    static float pressOffset(GenericCrafter.GenericCrafterBuild b,ManufacturingModelProfile p,int head){
        double progress=Float.isFinite(b.progress)?b.progress-Math.floor(b.progress):0;
        return -p.stroke*activity(b)*(float)((1-Math.cos((progress+head*.5)*Math.PI*2))*.5);
    }
    static float rotorAngle(GenericCrafter.GenericCrafterBuild b,ManufacturingModelProfile p){return Float.isFinite(b.totalProgress)?(float)((b.totalProgress*(p==ManufacturingModelProfile.BLAST?1.5:2.0))%360):0;}
    boolean render(Building building,NativeAtlasMesh.Builder out,float x,float base,float z,float ignoredSize,float height,float distanceSquared){
        if(!(building instanceof GenericCrafter.GenericCrafterBuild b))return false;
        var p=ManufacturingModelProfile.forBlock(b.block);if(p==null||!NativeAtlasMesh.valid(b.block.region))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        var model=cache.get(b.block);if(model==null||model.region!=b.block.region||model.size!=b.block.size){model=create((GenericCrafter)b.block,p);cache.put(b.block,model);builds++;}
        float size=b.block.size,angle=b.block.rotate&&b.block.rotateDraw?b.rotation*90:0;int lod=ManufacturingModelProfile.lod(distanceSquared);
        SpriteExtrusionGeometry.basis(90+angle,0,0,basis);var parts=model.parts[lod];
        out.extrusion(model.region,parts[0],x,base,z,size,height,size,basis,1,1,1);
        out.extrusion(model.region,parts[1],x,base,z,size,height,size,basis,.14f,.14f,.14f);
        if(lod==2){out.top(model.region,x,base+height*.905f,z,size*.96f,size*.96f,-angle,1,1,1,false);return true;}
        if(p.press())for(int i=0;i<p.heads;i++){
            float offset=pressOffset(b,p,i),headTop=p.headTop()+offset;
            out.extrusion(model.region,model.heads[lod][i],x,base+offset*height,z,size,height,size,basis,1,1,1);
            out.extrusion(model.region,model.rods[lod][i],x,base+headTop*height,z,size,(.84f-headTop)*height,size,basis,.9f,.94f,1);
        }
        if(p.mixer()){
            SpriteExtrusionGeometry.basis(90+angle+rotorAngle(b,p),0,0,rotorBasis);
            out.extrusion(model.region,parts[2],x,base,z,size,height,size,rotorBasis,1,1,1);
            if(b.items!=null&&b.items.total()>0){var c=p==ManufacturingModelProfile.PYRATITE?Items.pyratite.color:Items.blastCompound.color;
                out.extrusion(Core.atlas.white(),parts[3],x,base,z,size,height,size,basis,c.r*.55f,c.g*.55f,c.b*.55f);}
        }else if(p.furnace()){
            float warm=heat(b),pulse=activity(b)>0&&Float.isFinite(b.totalProgress)?1+Mathf.sin(b.totalProgress*.21f)*.06f:1;
            var c=model.flameColor;float light=.08f+.92f*warm*pulse;
            out.extrusion(Core.atlas.white(),parts[3],x,base,z,size,height,size,basis,c.r*light,c.g*.47f*light,c.b*.22f*light);
            if(warm>.001f&&NativeAtlasMesh.valid(model.flame)){
                float w=size*model.flame.width/(float)Math.max(1,model.region.width),d=size*model.flame.height/(float)Math.max(1,model.region.height),top=p==ManufacturingModelProfile.SMELTER?.855f:.835f;
                out.top(model.flame,x,base+height*top+.003f,z,w,d,-angle,c.r*warm,c.g*warm,c.b*warm,false);
            }
        }
        return true;
    }
    private Model create(GenericCrafter type,ManufacturingModelProfile profile){
        var m=new Model();m.region=type.region;m.size=type.size;var flame=flame(type.drawer);m.flame=flame==null?null:flame.top;m.flameColor=flame==null?Color.orange:flame.flameColor;
        float u=.14f,v=.14f;if(m.region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){double best=Double.POSITIVE_INFINITY;
            for(int yy=0;yy<pix.height;yy++)for(int xx=0;xx<pix.width;xx++)if(pix.getA(xx,yy)>=200){float su=xx/(float)Math.max(1,pix.width-1),sv=yy/(float)Math.max(1,pix.height-1);double dist=(su-.14)*(su-.14)+(sv-.14)*(sv-.14);if(dist<best){best=dist;u=su;v=sv;}}
        }}
        for(int lod=0;lod<3;lod++){
            var raw=ManufacturingModelGeometry.model(profile,type.size,lod);m.parts[lod]=new float[][]{material(raw.body(),u,v),material(raw.lining(),u,v),material(raw.rotor(),u,v),material(raw.heat(),.5f,.5f)};
            m.heads[lod]=new float[raw.heads().length][];m.rods[lod]=new float[raw.rods().length][];
            for(int i=0;i<m.heads[lod].length;i++){m.heads[lod][i]=material(raw.heads()[i],u,v);m.rods[lod][i]=material(raw.rods()[i],u,v);}
            for(var a:m.parts[lod])bytes+=(long)a.length*4;for(var a:m.heads[lod])bytes+=(long)a.length*4;for(var a:m.rods[lod])bytes+=(long)a.length*4;
        }return m;
    }
    private static DrawFlame flame(DrawBlock drawer){if(drawer instanceof DrawFlame flame)return flame;if(drawer instanceof DrawMulti multi)for(var child:multi.drawers){var result=flame(child);if(result!=null)return result;}return null;}
    private static float[] material(float[] raw,float u,float v){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(a[i+6]<0){a[i+6]=u;a[i+7]=v;}return a;}
}
