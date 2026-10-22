package fpv.render;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.world.blocks.production.Drill;
import java.util.IdentityHashMap;

/** Read-only drill profiles submitted into the existing opaque atlas/depth pass on every floor. */
final class DrillModelRenderer {
    private static final class Model {
        TextureRegion base,rotor,cap;
        final float[][] bodies=new float[3][],pistons=new float[3][];
        final float[][] foundations=new float[2][],tunnels=new float[2][],lights=new float[3][];
        float[] rotorNear,capNear,rotorMid,capMid;
    }
    private final IdentityHashMap<Drill,Model> cache=new IdentityHashMap<>();
    private Object atlas;private long builds,bytes;private final double[] basis=new double[9],rotorBasis=new double[9];
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}
    void clear(){cache.clear();atlas=null;bytes=0;}
    static boolean supports(mindustry.world.Block block){return DrillModelProfile.forBlock(block)!=null;}
    static float activity(Drill.DrillBuild b){
        if(!b.enabled||b.efficiency<=0||b.dominantItem==null||b.dominantItems<=0||b.items.total()>=b.block.itemCapacity)return 0;
        return Float.isFinite(b.warmup)?Mathf.clamp(b.warmup):0;
    }
    static float rotorAngle(Drill.DrillBuild b){float time=b.timeDrilled,speed=((Drill)b.block).rotateSpeed;return Float.isFinite(time)&&Float.isFinite(speed)?(float)((time*(double)speed)%360):0;}
    static float pistonOffset(Drill.DrillBuild b,DrillModelProfile profile){return Float.isFinite(b.timeDrilled)?Mathf.sin(b.timeDrilled*.12f)*profile.stroke*activity(b):0;}
    boolean render(Building building,NativeAtlasMesh.Builder out,float x,float base,float z,float size,float height,float distanceSquared){
        if(!(building instanceof Drill.DrillBuild b))return false;
        var profile=DrillModelProfile.forBlock(b.block);if(profile==null)return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        var type=(Drill)b.block;var model=cache.get(type);
        if(model==null||model.base!=type.region||model.rotor!=type.rotatorRegion||model.cap!=type.topRegion){
            model=create(type,profile);if(model==null)return false;cache.put(type,model);builds++;
            bytes=0;for(var m:cache.values()){for(var list:new float[][][]{m.bodies,m.pistons,m.foundations,m.tunnels,m.lights})for(var a:list)bytes+=(long)a.length*4;bytes+=(long)(m.rotorNear.length+m.capNear.length+m.rotorMid.length+m.capMid.length)*4;}
        }
        int lod=DrillModelProfile.lod(distanceSquared);float angle=type.rotate&&type.rotateDraw?b.rotation*90f:0;
        SpriteExtrusionGeometry.basis(90+angle,0,0,basis);
        out.foundationExtrusion(model.base,model.bodies[lod],x,base,z,size,height,size,basis,1,1,1);
        if(lod==2){
            out.top(type.fullIcon,x,base+height*.65f,z,size,size,-angle,1,1,1,false);return true;
        }
        out.foundationExtrusion(model.base,model.foundations[lod],x,base,z,size,height,size,basis,1,1,1);
        out.extrusion(model.base,model.tunnels[lod],x,base,z,size,height,size,basis,.16f,.16f,.16f);
        float active=activity(b),light=statusLight(b);
        out.extrusion(model.base,model.lights[lod],x,base,z,size,height,size,basis,light,light,light);
        out.extrusion(model.base,model.pistons[lod],x,base+pistonOffset(b,profile)*height,z,size,height,size,basis,.9f,.94f,1);
        SpriteExtrusionGeometry.basis(90+angle+rotorAngle(b),0,0,rotorBasis);
        out.extrusion(model.rotor,lod==0?model.rotorNear:model.rotorMid,x,base+height*profile.rotor,z,size*ratio(model.rotor,model.base),height*.075f,size*depthRatio(model.rotor,model.base),rotorBasis,1,1,1);
        out.extrusion(model.cap,lod==0?model.capNear:model.capMid,x,base+height*profile.cap,z,size*ratio(model.cap,model.base),height*profile.capDepth,size*depthRatio(model.cap,model.base),basis,1,1,1);
        if(type.drawRim&&active>.001f&&NativeAtlasMesh.valid(type.rimRegion)){
            var c=type.heatColor;float rimLight=active*.75f;
            out.top(type.rimRegion,x,base+height*(profile.deck+.008f),z,size*ratio(type.rimRegion,model.base),size*depthRatio(type.rimRegion,model.base),-angle,c.r*rimLight,c.g*rimLight,c.b*rimLight,false);
        }
        if(type.drawMineItem&&b.dominantItem!=null&&NativeAtlasMesh.valid(type.itemRegion)){
            var c=b.dominantItem.color;out.top(type.itemRegion,x,base+height*(profile.cap+profile.capDepth*.5f)+.003f,z,size*ratio(type.itemRegion,model.base),size*depthRatio(type.itemRegion,model.base),-angle,c.r,c.g,c.b,false);
        }
        return true;
    }
    static float ratio(TextureRegion part,TextureRegion base){return part.width/(float)Math.max(1,base.width);}
    static float statusLight(Drill.DrillBuild b){return .18f+.82f*activity(b);}
    static float depthRatio(TextureRegion part,TextureRegion base){return part.height/(float)Math.max(1,base.height);}
    private Model create(Drill type,DrillModelProfile profile){
        if(!NativeAtlasMesh.valid(type.region)||!NativeAtlasMesh.valid(type.rotatorRegion)||!NativeAtlasMesh.valid(type.topRegion))return null;
        if(!(type.region instanceof AtlasRegion base)||!(type.rotatorRegion instanceof AtlasRegion rotor)||!(type.topRegion instanceof AtlasRegion cap)||base.rotate||rotor.rotate||cap.rotate)return null;
        var model=new Model();model.base=base;model.rotor=rotor;model.cap=cap;
        var pix=Core.atlas.getPixmap(base);if(pix==null||pix.pixmap.isDisposed())return null;
        // Cache an opaque edge-color texel for body sides; never sample another atlas region.
        float su=.14f,sv=.14f;double nearest=Double.POSITIVE_INFINITY;
        for(int yy=0;yy<pix.height;yy++)for(int xx=0;xx<pix.width;xx++)if(pix.getA(xx,yy)>=200){
            double u=xx/(double)Math.max(1,pix.width-1),v=yy/(double)Math.max(1,pix.height-1),d=(u-.14)*(u-.14)+(v-.14)*(v-.14);
            if(d<nearest){nearest=d;su=(float)u;sv=(float)v;}
        }
        for(int lod=0;lod<3;lod++){
            var shape=MechanicalBlockGeometry.drill(profile,lod);model.bodies[lod]=material(shape.body(),su,sv);model.pistons[lod]=material(shape.pistons(),su,sv);model.lights[lod]=material(shape.lights(),su,sv);
            if(lod<2){var ports=ResourcePortGeometry.generate(type.size,profile.deck,lod==0);model.foundations[lod]=material(ports.shell(),su,sv);model.tunnels[lod]=material(ports.lining(),su,sv);}
        }
        model.rotorNear=rotating(silhouette(rotor,12,360));model.capNear=silhouette(cap,12,360);
        model.rotorMid=rotating(silhouette(rotor,4,180));model.capMid=silhouette(cap,4,180);
        return model;
    }
    private static float[] rotating(float[] vertices){
        // Conservative alpha-grid cells must not swing outside the original multiblock.
        double radius=.49;for(int i=0;i<vertices.length;i+=8)radius=Math.max(radius,Math.hypot(vertices[i],vertices[i+2]));
        float scale=(float)(.49/radius);for(int i=0;i<vertices.length;i+=8){vertices[i]*=scale;vertices[i+2]*=scale;}return vertices;
    }
    private static float[] material(float[] original,float u,float v){
        var out=original.clone();for(int i=0;i<out.length;i+=8)if(out[i+6]<0){boolean accent=out[i+6]==-2;out[i+6]=accent?.5f:u;out[i+7]=accent?.10f:v;}return out;
    }
    private static float[] silhouette(AtlasRegion region,int side,int budget){
        var pixels=Core.atlas.getPixmap(region);var alpha=new byte[region.width*region.height];
        for(int y=0;y<region.height;y++)for(int x=0;x<region.width;x++)alpha[x+y*region.width]=(byte)pixels.getA(region.isFlipX()?region.width-1-x:x,region.isFlipY()?region.height-1-y:y);
        return SpriteExtrusionGeometry.generate(alpha,region.width,region.height,side,2,32,.2,budget).vertices();
    }
}
