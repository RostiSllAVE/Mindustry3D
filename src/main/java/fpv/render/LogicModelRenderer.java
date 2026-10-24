package fpv.render;

import arc.Core;
import arc.graphics.g2d.*;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.logic.*;
import java.util.IdentityHashMap;

/** Read-only geometry/state. Screens sample the native framebuffer; no new logic simulation. */
final class LogicModelRenderer {
    private static final double[] BASIS={1,0,0,0,1,0,0,0,1};
    private static final class Model{TextureRegion art;final float[][][] parts=new float[3][][];}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private Object atlas;private long builds,bytes;
    static boolean supports(Block b){return LogicModelProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}
    void clear(){cache.clear();atlas=null;builds=bytes=0;}
    static float activity(Building b){
        if(b.dead||b.health<=0||!b.enabled)return 0;
        if(b instanceof LogicBlock.LogicBuild logic)return logic.executor!=null&&logic.executor.initialized()?1:0;
        return 1; // Memory/control power indicator, never a fabricated read/write clock.
    }
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float h,float distance,TextureRegion live){
        var p=LogicModelProfile.forBlock(b.block);if(p==null)return false;
        if(b.dead||b.health<=0)return true;if(!NativeAtlasMesh.valid(b.block.region))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        Model m=cache.get(b.block);if(m==null||m.art!=b.block.region){m=create(p,b.block.region);cache.put(b.block,m);builds++;}
        int lod=ManufacturingModelProfile.lod(distance);float size=b.block.size;var a=m.parts[lod];
        out.solidExtrusion(m.art,a[0],x,base,z,size,h,size,BASIS,true);
        float enabled=activity(b),r=.18f+enabled*.22f,g=.22f+enabled*.67f,blue=.25f+enabled*.62f;
        if(p==LogicModelProfile.SWITCH){r=.28f;g=enabled>.5f?1:.16f;blue=enabled>.5f?.45f:.12f;
            out.solidExtrusion(m.art,a[b.enabled?2:3],x,base,z,size,h,size,BASIS,false);
            if(b.enabled&&b.block instanceof SwitchBlock sw&&NativeAtlasMesh.valid(sw.onRegion))out.top(sw.onRegion,x,base+h*.26f+.009f,z,size*.8f,size*.8f,0,1,1,1,false);
        }
        out.extrusion(Core.atlas.white(),a[1],x,base,z,size,h,size,BASIS,r,g,blue);
        if(p.display()){
            boolean visible=NativeAtlasMesh.valid(live)&&(mindustry.Vars.renderer==null||mindustry.Vars.renderer.drawDisplays);
            screen(out,visible?live:null,p,x,base,z,h,LogicModelGeometry.screenWidth(p),.013f);
        }
        return true;
    }
    /** North/top edge is native +Y (-render Z); the host-provided flipped V region is retained. */
    static void screen(NativeAtlasMesh.Builder out,TextureRegion live,LogicModelProfile p,float x,float base,float z,float h,float width,float gap){
        float half=width*.5f,depth=half*(float)Math.cos(LogicModelGeometry.pitch(p));
        float north=base+h*LogicModelGeometry.roofY(p,-depth/p.block().size)+gap;
        float south=base+h*LogicModelGeometry.roofY(p,depth/p.block().size)+gap;
        if(live!=null)out.screen(live,x-half,north,z-depth,x+half,north,z-depth,x+half,south,z+depth,x-half,south,z+depth);
        else out.face(Core.atlas.white(),x-half,north,z-depth,x+half,north,z-depth,x+half,south,z+depth,x-half,south,z+depth,0);
    }
    private Model create(LogicModelProfile p,TextureRegion art){
        var m=new Model();m.art=art;float u=.1f,v=.1f;
        if(art instanceof TextureAtlas.AtlasRegion region){var pix=Core.atlas.getPixmap(region);if(pix!=null&&!pix.pixmap.isDisposed()){
            double best=Double.POSITIVE_INFINITY;for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>220){double d=x*x+y*y;if(d<best){best=d;u=x/(float)Math.max(1,pix.width-1);v=y/(float)Math.max(1,pix.height-1);}}
        }}
        for(int lod=0;lod<3;lod++){
            var raw=LogicModelGeometry.model(p,lod);m.parts[lod]=new float[][]{material(raw.body(),u,v,false),material(raw.lights(),u,v,true),material(raw.controlOn(),u,v,false),material(raw.controlOff(),u,v,false)};
            for(var a:m.parts[lod])bytes+=(long)a.length*4;
        }
        return m;
    }
    private static float[] material(float[] raw,float u,float v,boolean white){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(white||a[i+6]<0){a[i+6]=white?.5f:u;a[i+7]=white?.5f:v;}return a;}
}
