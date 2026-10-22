package fpv.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.defense.ShockMine;
import mindustry.world.blocks.power.LightBlock;
import java.util.IdentityHashMap;

/** Read-only native utility state. The transfer pass alone draws real in-flight shipments. */
final class UtilityModelRenderer {
    private static final class Model{TextureRegion region,top;final float[][][] parts=new float[3][][];}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private final double[] basis=new double[9];private final Color color=new Color();private Object atlas;private long builds,bytes;
    static boolean supports(Block b){return UtilityModelProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}
    void clear(){cache.clear();atlas=null;builds=bytes=0;}
    static float level(Building b){
        if(!b.enabled)return 0;
        if(b instanceof ShockMine.ShockMineBuild)return 1; // Passive deployed contacts; no invented trigger flash.
        if(b instanceof LightBlock.LightBuild)return SupportModelRenderer.finite(b.efficiency);
        if(b instanceof LaunchPad.LaunchPadBuild v)return b.efficiency>0?SupportModelRenderer.finite(v.launchCounter/Math.max(1,((LaunchPad)b.block).launchTime)):0;
        if(b instanceof LandingPad.LandingPadBuild v)return v.arriving!=null?1:SupportModelRenderer.finite(1-v.cooldown)*SupportModelRenderer.finite(v.efficiency);
        return 0;
    }
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float h,float distance){
        var p=UtilityModelProfile.forBlock(b.block);if(p==null)return false;
        var region=NativeRegions.block(b.block);if(!NativeAtlasMesh.valid(region))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}var m=cache.get(b.block);
        if(m==null||m.region!=region){m=create(b.block,p,region);cache.put(b.block,m);builds++;}
        int lod=ManufacturingModelProfile.lod(distance);float size=b.block.size;var a=m.parts[lod];SpriteExtrusionGeometry.basis(90,0,0,basis);
        out.extrusion(m.region,a[0],x,base,z,size,h,size,basis,1,1,1);out.extrusion(m.region,a[1],x,base,z,size,h,size,basis,.14f,.14f,.14f);
        float cap=p==UtilityModelProfile.MINE?.701f:p==UtilityModelProfile.LIGHT?.947f:.435f;
        float width=p==UtilityModelProfile.LIGHT?.39f:p==UtilityModelProfile.MINE?.46f:.85f;
        out.top(m.top,x,base+h*cap+.004f,z,size*width,size*width,0,1,1,1,false);
        if(b instanceof LightBlock.LightBuild v)color.set(v.color);
        else if(b.block instanceof ShockMine type)color.set(type.lightningColor);
        else if(b.block instanceof LaunchPad type)color.set(type.lightColor);
        else if(b instanceof LandingPad.LandingPadBuild v&&v.config!=null)color.set(v.config.color);
        else color.set(b.team.color);
        float glow=.04f+.85f*level(b);
        out.extrusion(Core.atlas.white(),a[2],x,base,z,size,h,size,basis,color.r*glow,color.g*glow,color.b*glow);
        return true;
    }
    private Model create(Block b,UtilityModelProfile p,TextureRegion region){
        var m=new Model();m.region=region;m.top=b instanceof LightBlock t?t.topRegion:region;if(!NativeAtlasMesh.valid(m.top))m.top=region;
        float u=.14f,v=.14f;
        if(region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){double best=Double.POSITIVE_INFINITY;for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>=200){float U=x/(float)Math.max(1,pix.width-1),V=y/(float)Math.max(1,pix.height-1);double d=(U-.14)*(U-.14)+(V-.14)*(V-.14);if(d<best){best=d;u=U;v=V;}}}}
        for(int lod=0;lod<3;lod++){var g=UtilityModelGeometry.model(p,b.size,lod);m.parts[lod]=new float[][]{material(g.body(),u,v,false),material(g.lining(),u,v,false),material(g.lights(),u,v,true)};for(var part:m.parts[lod])bytes+=(long)part.length*4;}return m;
    }
    private static float[] material(float[] raw,float u,float v,boolean white){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(white||a[i+6]<0){a[i+6]=white?.5f:u;a[i+7]=white?.5f:v;}return a;}
}
