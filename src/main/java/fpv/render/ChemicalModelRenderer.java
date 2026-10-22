package fpv.render;

import arc.Core;import arc.graphics.g2d.*;import arc.math.Mathf;
import mindustry.content.*;import mindustry.gen.Building;import mindustry.type.Liquid;import mindustry.world.Block;import mindustry.world.blocks.production.*;
import java.util.IdentityHashMap;

/** Presentation only: reads native clocks, consumers and liquid modules without advancing them. */
final class ChemicalModelRenderer {
    private static final class Model {TextureRegion region;int size;final float[][][] parts=new float[3][][];}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();private Object atlas;private long builds,bytes;
    private final double[] basis=new double[9],moving=new double[9];
    static boolean supports(Block b){return ChemicalModelProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}
    void clear(){cache.clear();atlas=null;builds=bytes=0;}
    private static float finite(float v){return Float.isFinite(v)?Mathf.clamp(v):0;}
    static float clock(Building b){float t=b instanceof SolidPump.SolidPumpBuild pump?pump.pumpTime:b.totalProgress();return Float.isFinite(t)?t:0;}
    static float activity(Building b){if(!b.enabled||!(b.efficiency>0)||b.block.hasLiquids&&b.liquids==null||b.block.hasItems&&b.items==null||!b.shouldConsume())return 0;return b instanceof SolidPump.SolidPumpBuild pump?(pump.lastPump>0?finite(pump.warmup):0):finite(b.warmup());}
    static float stroke(Building b){return activity(b)*(.5f+.5f*Mathf.sin(clock(b)/6f));}
    static float fill(Building b,Liquid liquid){return b.liquids==null||liquid==null?0:finite(b.liquids.get(liquid)/Math.max(.001f,b.block.liquidCapacity));}
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float ignoredSize,float h,float distanceSquared){
        var p=ChemicalModelProfile.forBlock(b.block);if(p==null||!(b instanceof GenericCrafter.GenericCrafterBuild||b instanceof SolidPump.SolidPumpBuild)||!NativeAtlasMesh.valid(b.block.region))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}var m=cache.get(b.block);if(m==null||m.region!=b.block.region||m.size!=b.block.size){m=create(b.block,p);cache.put(b.block,m);builds++;}
        int lod=ManufacturingModelProfile.lod(distanceSquared);float size=b.block.size,yaw=b.block.rotate&&b.block.rotateDraw?b.rotation*90:0;SpriteExtrusionGeometry.basis(90+yaw,0,0,basis);var a=m.parts[lod];
        out.foundationExtrusion(m.region,a[0],x,base,z,size,h,size,basis,1,1,1);out.extrusion(m.region,a[1],x,base,z,size,h,size,basis,.16f,.16f,.16f);
        if(lod==2){out.top(m.region,x,base+h*.915f,z,size*.96f,size*.96f,-yaw,1,1,1,false);return true;}
        float angle=(float)((clock(b)*(double)(b.block instanceof SolidPump pump?pump.rotateSpeed:p==ChemicalModelProfile.PULVERIZER?2:3))%360),stroke=stroke(b);
        if(p==ChemicalModelProfile.CRYO||p==ChemicalModelProfile.WATER){
            SpriteExtrusionGeometry.basis(90+yaw+angle,0,0,moving);
            if(p==ChemicalModelProfile.CRYO)for(int side=-1;side<=1;side+=2)out.extrusion(m.region,a[2],x+(float)basis[0]*side*.22f*size,base,z+(float)basis[2]*side*.22f*size,size,h,size,moving,1,1,1);
            else out.extrusion(m.region,a[2],x+(float)(basis[0]*-.19f+basis[6]*.18f)*size,base,z+(float)(basis[2]*-.19f+basis[8]*.18f)*size,size,h,size,moving,1,1,1);
        }else if(p==ChemicalModelProfile.OIL){
            float tilt=-12+24*stroke;SpriteExtrusionGeometry.basis(90+yaw,0,tilt,moving);out.extrusion(m.region,a[4],x,base+h*.71f,z,size,h,size,moving,1,1,1);
            out.extrusion(m.region,a[3],x,base+h*.28f,z,size,h*.43f-size*.325f*(float)moving[1],size,basis,1,1,1);
        }else{
            SpriteExtrusionGeometry.basis(90+yaw+angle,0,0,moving);out.extrusion(m.region,a[2],x,base,z,size,h,size,moving,1,1,1);
        }
        float shift=p==ChemicalModelProfile.PRESS?-.19f*stroke:p==ChemicalModelProfile.WATER?.10f*stroke:p==ChemicalModelProfile.OIL?-.08f+.16f*stroke:p==ChemicalModelProfile.PULVERIZER?-.12f*stroke:0;
        if(p!=ChemicalModelProfile.OIL)out.extrusion(m.region,a[3],x,base+h*shift,z,size,h,size,basis,1,1,1);
        if(p==ChemicalModelProfile.CULTIVATOR){float growth=.63f+.28f*finite(b.progress())*activity(b),brightness=.30f+.60f*activity(b);var c=Items.sporePod.color;out.extrusion(Core.atlas.white(),a[6],x,base+h*.12f,z,size,h*growth,size,basis,c.r*brightness,c.g*brightness,c.b*brightness);}
        else if(p==ChemicalModelProfile.MELTER){float light=.08f+.8f*finite(b.warmup());out.extrusion(Core.atlas.white(),a[6],x,base,z,size,h,size,basis,light,light*.42f,light*.12f);}
        switch(p){
            case CRYO->{liquid(b,out,a[5],x,base,z,size,h,-.22f,0,.09f,.24f,.40f,Liquids.water);liquid(b,out,a[5],x,base,z,size,h,.22f,0,.09f,.24f,.40f,Liquids.cryofluid);}
            case MELTER->liquid(b,out,a[5],x,base,z,size,h,0,0,.18f,.23f,.46f,Liquids.slag);
            case PRESS->liquid(b,out,a[5],x,base,z,size,h,0,.34f,.06f,.25f,.18f,Liquids.oil);
            case CENTRIFUGE->liquid(b,out,a[5],x,base,z,size,h,0,0,.18f,.22f,.20f,Liquids.oil);
            case CULTIVATOR->liquid(b,out,a[5],x,base,z,size,h,0,0,.22f,.22f,.13f,Liquids.water);
            case WATER->liquid(b,out,a[5],x,base,z,size,h,.23f,.12f,.08f,.23f,.30f,Liquids.water);
            case OIL->liquid(b,out,a[5],x,base,z,size,h,.25f,.25f,.08f,.23f,.29f,Liquids.oil);
            default->{}
        }
        return true;
    }
    private void liquid(Building b,NativeAtlasMesh.Builder out,float[] mesh,float x,float base,float z,float size,float h,float dx,float dz,float half,float bottom,float range,Liquid liquid){
        float amount=fill(b,liquid);if(amount<=.001f)return;var c=liquid.color;
        out.extrusion(Core.atlas.white(),mesh,x+(float)(basis[0]*dx+basis[6]*dz)*size,base+h*bottom,z+(float)(basis[2]*dx+basis[8]*dz)*size,size*half,h*range*amount,size*half,basis,c.r,c.g,c.b);
    }
    private Model create(Block type,ChemicalModelProfile p){
        var m=new Model();m.region=type.region;m.size=type.size;float u=.14f,v=.14f;
        if(m.region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){double best=Double.POSITIVE_INFINITY;for(int yy=0;yy<pix.height;yy++)for(int xx=0;xx<pix.width;xx++)if(pix.getA(xx,yy)>=200){float su=xx/(float)Math.max(1,pix.width-1),sv=yy/(float)Math.max(1,pix.height-1);double d=(su-.14)*(su-.14)+(sv-.14)*(sv-.14);if(d<best){best=d;u=su;v=sv;}}}}
        for(int lod=0;lod<3;lod++){var g=ChemicalModelGeometry.model(p,type.size,lod);m.parts[lod]=new float[][]{material(g.body(),u,v),material(g.lining(),u,v),material(g.rotor(),u,v),material(g.piston(),u,v),material(g.arm(),u,v),white(g.fluid()),white(g.accent())};for(var a:m.parts[lod])bytes+=(long)a.length*4;}return m;
    }
    private static float[] white(float[] raw){var a=raw.clone();for(int i=0;i<a.length;i+=8){a[i+6]=.5f;a[i+7]=.5f;}return a;}
    private static float[] material(float[] raw,float u,float v){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(a[i+6]<0){a[i+6]=u;a[i+7]=v;}return a;}
}
