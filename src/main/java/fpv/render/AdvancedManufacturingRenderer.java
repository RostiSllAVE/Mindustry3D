package fpv.render;

import arc.Core;import arc.graphics.Color;import arc.graphics.g2d.*;import arc.math.Mathf;
import mindustry.content.Items;import mindustry.gen.Building;import mindustry.world.Block;import mindustry.world.blocks.production.*;import mindustry.world.draw.*;
import java.util.IdentityHashMap;

/** Read-only native crafter/separator presentation. No production, consumer or RNG calls. */
final class AdvancedManufacturingRenderer {
    private static final class Model {TextureRegion region,detail,flame;Color flameColor;int size;final float[][][] parts=new float[3][][];final float[][] detailMesh=new float[2][];}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();private Object atlas;private long builds,bytes;
    private final double[] basis=new double[9],movingBasis=new double[9];
    static boolean supports(Block b){return AdvancedManufacturingProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}
    void clear(){cache.clear();atlas=null;builds=bytes=0;}
    private static float finite(float v){return Float.isFinite(v)?Mathf.clamp(v):0;}
    static float activity(Building b){return b.enabled&&b.efficiency>0&&b.shouldConsume()?finite(b.warmup()):0;}
    static float heat(Building b){return finite(b.warmup());}
    static float clock(Building b){return Float.isFinite(b.totalProgress())?b.totalProgress():0;}
    static float angle(Building b,AdvancedManufacturingProfile p){return (float)((clock(b)*(double)(p==AdvancedManufacturingProfile.WEAVER?1:3))%360);}
    static float compression(Building b){double v=Float.isFinite(b.progress())?b.progress()-Math.floor(b.progress()):0;return .12f*activity(b)*(float)((1-Math.cos(v*Math.PI*2))*.5);}
    static float shuttle(Building b){return Mathf.sin(clock(b)/6f)*.22f*activity(b);}
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float ignoredSize,float height,float distanceSquared){
        var p=AdvancedManufacturingProfile.forBlock(b.block);if(p==null||!(b instanceof GenericCrafter.GenericCrafterBuild||b instanceof Separator.SeparatorBuild)||!NativeAtlasMesh.valid(b.block.region))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}var m=cache.get(b.block);if(m==null||m.region!=b.block.region||m.size!=b.block.size){m=create(b.block,p);cache.put(b.block,m);builds++;}
        int lod=ManufacturingModelProfile.lod(distanceSquared);float size=b.block.size,yaw=b.block.rotate&&b.block.rotateDraw?b.rotation*90:0;SpriteExtrusionGeometry.basis(90+yaw,0,0,basis);var a=m.parts[lod];
        out.extrusion(m.region,a[0],x,base,z,size,height,size,basis,1,1,1);out.extrusion(m.region,a[1],x,base,z,size,height,size,basis,.14f,.14f,.14f);
        if(lod==2){out.top(m.region,x,base+height*.915f,z,size*.96f,size*.96f,-yaw,1,1,1,false);return true;}
        if(p==AdvancedManufacturingProfile.COMPRESSOR){float stroke=compression(b);
            for(int side=-1;side<=1;side+=2){ // Mirror the same cached ram; its rod ends stay in the fixed housing.
                float sign=-side;out.extrusion(m.region,a[3],x+(float)basis[0]*stroke*size*sign,base,z+(float)basis[2]*stroke*size*sign,size*sign,height,size,basis,1,1,1);
                out.extrusion(m.region,a[4],x+(float)basis[0]*(-.42f)*size*sign,base,z+(float)basis[2]*(-.42f)*size*sign,size*(.13f+stroke)*sign,height,size,basis,.90f,.94f,1);
            }
            var c=Items.plastanium.color;float light=.15f+.65f*heat(b);out.extrusion(Core.atlas.white(),a[5],x,base,z,size,height,size,basis,c.r*light,c.g*light,c.b*light);
        }else if(p==AdvancedManufacturingProfile.WEAVER){float offset=shuttle(b);out.extrusion(m.region,a[3],x+(float)basis[0]*offset*size,base,z+(float)basis[2]*offset*size,size,height,size,basis,1,1,1);
            SpriteExtrusionGeometry.basis(90+yaw+angle(b,p),0,0,movingBasis);out.extrusion(m.region,a[2],x,base,z,size,height,size,movingBasis,1,1,1);detail(m,out,lod,x,base+height*.46f,z,size*.57f,height*.04f,movingBasis);
            var c=Items.phaseFabric.color;float light=.12f+.75f*activity(b);out.extrusion(Core.atlas.white(),a[5],x,base,z,size,height,size,basis,c.r*light,c.g*light,c.b*light);
        }else if(p.furnace()){float warm=heat(b),pulse=activity(b)>0?1+Mathf.sin(clock(b)*.21f)*.05f:1,light=.06f+.94f*warm*pulse;var c=m.flameColor;
            out.extrusion(Core.atlas.white(),a[5],x,base,z,size,height,size,basis,c.r*light,c.g*.5f*light,c.b*.3f*light);
            if(warm>.001f&&NativeAtlasMesh.valid(m.flame))out.top(m.flame,x,base+height*(p==AdvancedManufacturingProfile.KILN?.84f:.81f)+.003f,z,size*m.flame.width/Math.max(1f,m.region.width),size*m.flame.height/Math.max(1f,m.region.height),-yaw,c.r*warm,c.g*warm,c.b*warm,false);
        }else{
            if(p==AdvancedManufacturingProfile.SEPARATOR){SpriteExtrusionGeometry.basis(90+yaw+angle(b,p),0,0,movingBasis);out.extrusion(m.region,a[2],x,base,z,size,height,size,movingBasis,1,1,1);detail(m,out,lod,x,base+height*.72f,z,size*.56f,height*.06f,movingBasis);}
            else for(int side=-1;side<=1;side+=2){SpriteExtrusionGeometry.basis(90+yaw,0,side*angle(b,p),movingBasis);out.extrusion(m.region,a[2],x+(float)basis[0]*side*.17f*size,base+height*.61f,z+(float)basis[2]*side*.17f*size,height*1.2f,height*1.2f,size,movingBasis,.94f,.96f,1);}
            if(p==AdvancedManufacturingProfile.DISASSEMBLER){SpriteExtrusionGeometry.basis(90+yaw+angle(b,p),0,0,movingBasis);detail(m,out,lod,x,base+height*.89f,z,size*.24f,height*.05f,movingBasis);}
            if(b.liquids!=null&&b.liquids.currentAmount()>.001f){var c=b.liquids.current().color;float fill=finite(b.liquids.currentAmount()/Math.max(.001f,b.block.liquidCapacity));out.extrusion(Core.atlas.white(),a[5],x,base,z,size,height,size,basis,c.r*(.25f+.4f*fill),c.g*(.25f+.4f*fill),c.b*(.25f+.4f*fill));}
        }
        return true;
    }
    private void detail(Model m,NativeAtlasMesh.Builder out,int lod,float x,float y,float z,float size,float height,double[] transform){if(NativeAtlasMesh.valid(m.detail))out.extrusion(m.detail,m.detailMesh[lod],x,y,z,size,height,size,transform,1,1,1);}
    private Model create(Block type,AdvancedManufacturingProfile p){
        var m=new Model();m.region=type.region;m.size=type.size;DrawBlock drawer=type instanceof GenericCrafter crafter?crafter.drawer:((Separator)type).drawer;var flame=find(drawer,DrawFlame.class);m.flame=flame==null?null:flame.top;m.flameColor=flame==null?Color.orange:flame.flameColor;
        if(p==AdvancedManufacturingProfile.WEAVER){var weave=find(drawer,DrawWeave.class);m.detail=weave==null?null:weave.weave;}else if(p.separates())m.detail=spinner(drawer);
        float u=.14f,v=.14f;if(m.region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){double best=Double.POSITIVE_INFINITY;for(int yy=0;yy<pix.height;yy++)for(int xx=0;xx<pix.width;xx++)if(pix.getA(xx,yy)>=200){float su=xx/(float)Math.max(1,pix.width-1),sv=yy/(float)Math.max(1,pix.height-1);double d=(su-.14)*(su-.14)+(sv-.14)*(sv-.14);if(d<best){best=d;u=su;v=sv;}}}}
        for(int lod=0;lod<3;lod++){var g=AdvancedManufacturingGeometry.model(p,type.size,lod);m.parts[lod]=new float[][]{material(g.body(),u,v),material(g.lining(),u,v),material(g.rotor(),u,v),material(g.slider(),u,v),material(g.rods(),u,v),material(g.energy(),.5f,.5f)};for(var a:m.parts[lod])bytes+=(long)a.length*4;
            if(lod<2){m.detailMesh[lod]=NativeAtlasMesh.valid(m.detail)?silhouette(m.detail,lod==0?8:4,lod==0?480:180):new float[0];bytes+=(long)m.detailMesh[lod].length*4;}
        }return m;
    }
    private static <T extends DrawBlock>T find(DrawBlock drawer,Class<T> type){if(type.isInstance(drawer))return type.cast(drawer);if(drawer instanceof DrawMulti multi)for(var child:multi.drawers){var result=find(child,type);if(result!=null)return result;}return null;}
    private static TextureRegion spinner(DrawBlock drawer){if(drawer instanceof DrawRegion r&&"-spinner".equals(r.suffix))return r.region;if(drawer instanceof DrawMulti m)for(var c:m.drawers){var found=spinner(c);if(found!=null)return found;}return null;}
    private static float[] silhouette(TextureRegion region,int side,int budget){if(region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){byte[] alpha=new byte[region.width*region.height];for(int y=0;y<region.height;y++)for(int x=0;x<region.width;x++)alpha[x+y*region.width]=(byte)pix.getA(region.isFlipX()?region.width-1-x:x,region.isFlipY()?region.height-1-y:y);var a=SpriteExtrusionGeometry.generate(alpha,region.width,region.height,side,2,32,.2,budget).vertices();double radius=.49;for(int i=0;i<a.length;i+=8)radius=Math.max(radius,Math.hypot(a[i],a[i+2]));float scale=(float)(.49/radius);for(int i=0;i<a.length;i+=8){a[i]*=scale;a[i+2]*=scale;}return a;}}return SpriteExtrusionGeometry.generate(new byte[]{-1},1,1,1,1,32,.2,budget).vertices();}
    private static float[] material(float[] raw,float u,float v){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(a[i+6]<0){a[i+6]=u;a[i+7]=v;}return a;}
}
