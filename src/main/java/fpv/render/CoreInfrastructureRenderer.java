package fpv.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Radar;
import mindustry.world.blocks.storage.CoreBlock;
import java.util.IdentityHashMap;

/** Six native core silhouettes plus radar, streamed through the existing atlas batch.
 * Reads native state only; no spawning, inventory, visibility or gameplay calls occur here. */
final class CoreInfrastructureRenderer {
    private static final class Model {
        TextureRegion region,dish;float dishWidth,dishLength;final float[][][] parts=new float[3][][];
        final float[][] roofs=new float[3][],dishVolume=new float[3][];
    }
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private final double[] basis=new double[9],rotor=new double[9];
    private final Color tint=new Color();private Object atlas;private long builds,bytes;
    static boolean supports(Block b){return CoreInfrastructureProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}
    void clear(){cache.clear();atlas=null;builds=bytes=0;}
    static float scanAngle(Radar.RadarBuild b){float angle=b.totalProgress*((Radar)b.block).rotateSpeed;return Float.isFinite(angle)?angle%360:0;}
    static float activity(Building b){
        if(b.dead||!(b.health>0)||!b.enabled)return 0;
        if(b instanceof Radar.RadarBuild radar)return SupportModelRenderer.finite(radar.smoothEfficiency);
        return 1; // Cores are passive command facilities, not efficiency-driven factories.
    }
    static float landing(Building b){return b instanceof CoreBlock.CoreBuild core?SupportModelRenderer.finite(core.thrusterTime):0;}
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float h,float distance){
        var p=CoreInfrastructureProfile.forBlock(b.block);if(p==null)return false;
        if(b.dead||!(b.health>0))return true;
        var region=b.block instanceof Radar type&&NativeAtlasMesh.valid(type.baseRegion)?type.baseRegion:NativeRegions.block(b.block);
        if(!NativeAtlasMesh.valid(region)||!p.core()&&!NativeAtlasMesh.valid(NativeRegions.block(b.block)))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        var m=cache.get(b.block);
        if(m==null||m.region!=region||!p.core()&&m.dish!=NativeRegions.block(b.block)){
            m=create(b.block,p,region);cache.put(b.block,m);builds++;
        }
        int lod=ManufacturingModelProfile.lod(distance);float size=b.block.size;var parts=m.parts[lod];
        float orientation=b.block.rotate&&b.block.rotateDraw?b.rotation*90f:0;
        SpriteExtrusionGeometry.basis(90+orientation,0,0,basis);
        out.solidExtrusion(m.region,parts[0],x,base,z,size,h,size,basis,true);
        out.extrusion(m.region,parts[1],x,base,z,size,h,size,basis,.14f,.14f,.14f);
        float active=activity(b);
        if(p.core()){
            // Use native precoloured variants when present, otherwise tint the original team mask.
            TextureRegion team=b.block.teamRegion;
            if(b.block.teamRegions!=null&&b.team.id<b.block.teamRegions.length&&NativeAtlasMesh.valid(b.block.teamRegions[b.team.id]))team=b.block.teamRegions[b.team.id];
            tint.set(team==b.block.teamRegion?b.team.color:Color.white);
            if(NativeAtlasMesh.valid(team))out.extrusion(team,m.roofs[lod],x,base+.006f,z,size,h,size,basis,tint.r,tint.g,tint.b);
            tint.set(b.team.color);
            // A restrained ambient status light, not an invented resource/progress meter.
            float clock=Float.isFinite(Time.time)?Time.time:0;
            float glow=.08f+active*(.40f+.06f*Mathf.sin(clock/110f+b.pos()*.017f))+.22f*landing(b);
            out.extrusion(Core.atlas.white(),parts[2],x,base,z,size,h,size,basis,tint.r*glow,tint.g*glow,tint.b*glow);
            if(lod<2){
                SpriteExtrusionGeometry.basis(90+orientation+clock*.075f,0,0,rotor);
                out.extrusion(Core.atlas.white(),parts[4],x,base,z,size,h,size,rotor,tint.r*active,tint.g*active,tint.b*active);
            }
        }else{
            var type=(Radar)b.block;float angle=scanAngle((Radar.RadarBuild)b);
            SpriteExtrusionGeometry.basis(90+angle,0,0,rotor);
            out.solidExtrusion(m.region,parts[3],x,base,z,size,h,size,rotor,false);
            float width=m.dishWidth,length=m.dishLength;
            // The atlas pass is opaque. Painting the native additive mask as an opaque roof
            // would cover the dish's white details with dark orange. Keep its original art;
            // a mild powered highlight and separate pedestal lamps provide the status cue.
            float shine=.15f*active;var glowColor=type.glowColor;
            out.extrusion(m.dish,m.dishVolume[lod],x,base+h*.79f,z,width,h*.18f,length,rotor,
                1+glowColor.r*shine,1+glowColor.g*shine,1+glowColor.b*shine);
            float glow=.06f+.65f*active;var c=type.glowColor;
            out.extrusion(Core.atlas.white(),parts[2],x,base,z,size,h,size,basis,c.r*glow,c.g*glow,c.b*glow);
        }
        return true;
    }
    private Model create(Block b,CoreInfrastructureProfile p,TextureRegion region){
        var m=new Model();m.region=region;m.dish=NativeRegions.block(b);float[] swatch=swatch(region);
        byte[] alpha=null;
        if(!p.core()){
            float aspect=m.dish.height/Math.max(1f,m.dish.width);
            m.dishWidth=Math.min((float)(b.size*.98/Math.hypot(1,aspect)),m.dish.width*m.dish.scl()/Vars.tilesize);
            m.dishLength=m.dishWidth*aspect;
            alpha=new byte[m.dish.width*m.dish.height];
            if(m.dish instanceof TextureAtlas.AtlasRegion a){
                var pixels=Core.atlas.getPixmap(a);
                for(int y=0;y<pixels.height;y++)for(int x=0;x<pixels.width;x++)alpha[x+y*pixels.width]=(byte)pixels.getA(x,y);
            }else java.util.Arrays.fill(alpha,(byte)255);
        }
        for(int lod=0;lod<3;lod++){
            var g=CoreInfrastructureGeometry.model(p,b.size,lod);
            m.parts[lod]=new float[][]{material(g.body(),swatch,false),material(g.lining(),swatch,false),material(g.lights(),swatch,true),material(g.moving(),swatch,false),material(g.movingLights(),swatch,true)};
            m.roofs[lod]=p.core()?roof(g.body(),false):new float[0];
            for(var a:m.parts[lod])bytes+=(long)a.length*4;bytes+=(long)m.roofs[lod].length*4;
            if(alpha!=null){
                m.dishVolume[lod]=SpriteExtrusionGeometry.conservative(alpha,m.dish.width,m.dish.height,lod==0?24:lod==1?12:6,3,32,8190).vertices();
                bytes+=(long)m.dishVolume[lod].length*4;
            }
        }
        return m;
    }
    private static float[] roof(float[] source,boolean flatten){
        int n=0;for(int i=0;i<source.length;i+=8)if(source[i+4]>.5f)n+=8;
        float[] result=new float[n];n=0;
        for(int i=0;i<source.length;i+=8)if(source[i+4]>.5f){System.arraycopy(source,i,result,n,8);if(flatten)result[n+1]=0;n+=8;}
        return result;
    }
    private static float[] material(float[] source,float[] swatch,boolean white){
        float[] result=source.clone();for(int i=0;i<result.length;i+=8)if(white||result[i+6]<0){result[i+6]=white?.5f:swatch[0];result[i+7]=white?.5f:swatch[1];}return result;
    }
    private static float[] swatch(TextureRegion region){
        float u=.14f,v=.14f;
        if(region instanceof TextureAtlas.AtlasRegion a){var pix=Core.atlas.getPixmap(a);if(pix!=null&&!pix.pixmap.isDisposed()){
            double best=Double.POSITIVE_INFINITY;
            for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>=200){float U=x/(float)Math.max(1,pix.width-1),V=y/(float)Math.max(1,pix.height-1);double d=(U-.14)*(U-.14)+(V-.14)*(V-.14);if(d<best){best=d;u=U;v=V;}}
        }}
        return new float[]{u,v};
    }
}
