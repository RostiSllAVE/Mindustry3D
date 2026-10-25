package fpv.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.units.RepairTurret;
import java.util.IdentityHashMap;

/** Presentation only: original native configuration, payload and repair state. No fake output. */
final class SandboxModelRenderer {
    private record Model(TextureRegion art,float[][][] parts){}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private final double[] basis=new double[9],movingBasis=new double[9];private Object atlas;private long bytes,builds;
    static boolean supports(Block b){return SandboxModelProfile.forBlock(b)!=null;}
    long bytes(){return bytes;}long builds(){return builds;}void clear(){cache.clear();atlas=null;bytes=builds=0;}
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float h,float distance,TextureRegion live){
        var p=SandboxModelProfile.forBlock(b.block);if(p==null)return false;if(b.dead||b.health<=0)return true;
        if(!NativeAtlasMesh.valid(b.block.region))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        var model=cache.get(b.block);if(model==null||model.art!=b.block.region){model=create(p);cache.put(b.block,model);builds++;}
        float size=b.block.size;int lod=ManufacturingModelProfile.lod(distance);var a=model.parts[lod];
        SpriteExtrusionGeometry.basis(90+(p.belt()?b.rotdeg():0),0,0,basis);
        out.solidExtrusion(model.art,a[0],x,base,z,size,h,size,basis,true);
        out.extrusion(Core.atlas.white(),a[1],x,base,z,size,h,size,basis,.12f,.14f,.18f);
        Color c=b.block.mapColor;
        if(b instanceof ItemSource.ItemSourceBuild item&&item.outputItem!=null)c=item.outputItem.color;
        if(b instanceof LiquidSource.LiquidSourceBuild liquid&&liquid.source!=null)c=liquid.source.color;
        float active=b.enabled?1:.18f;if(p.sink())active*=.48f;
        if(b instanceof RepairTurret.RepairPointBuild repair){
            active=.18f+.82f*Math.max(0,Math.min(1,repair.strength));
            SpriteExtrusionGeometry.basis(90+repair.rotation,0,0,movingBasis);
            out.solidExtrusion(model.art,a[3],x,base,z,size,h,size,movingBasis,false);
            out.extrusion(Core.atlas.white(),a[2],x,base,z,size,h,size,movingBasis,.3f*active,active,.68f*active);
        }else{
            out.extrusion(Core.atlas.white(),a[2],x,base,z,size,h,size,basis,c.r*active,c.g*active,c.b*active);
            out.solidExtrusion(model.art,a[3],x,base,z,size,h,size,basis,false);
        }
        if(p==SandboxModelProfile.TILED_DISPLAY){
            if(NativeAtlasMesh.valid(live))out.screen(live,x-size*.5f,base+h*.68f+.012f,z-size*.5f,x+size*.5f,base+h*.68f+.012f,z-size*.5f,x+size*.5f,base+h*.68f+.012f,z+size*.5f,x-size*.5f,base+h*.68f+.012f,z+size*.5f);
            var tiled=(mindustry.world.blocks.logic.TileableLogicDisplay.TileableLogicDisplayBuild)b;
            var type=(mindustry.world.blocks.logic.TileableLogicDisplay)b.block;
            if(type.tileRegion!=null){var frame=type.tileRegion[mindustry.world.blocks.TileBitmask.values[tiled.bits]];
                out.top(frame,x,base+h*.68f+.020f,z,size,size,0,1,1,1,false);}
        }else if(p.belt()||p==SandboxModelProfile.PAYLOAD_SOURCE||p==SandboxModelProfile.PAYLOAD_VOID)payload(b,out,base+h*.32f+.02f,size);
        return true;
    }
    private void payload(Building b,NativeAtlasMesh.Builder out,float y,float size){
        var cargo=b.getPayload();if(cargo==null)return;
        TextureRegion art=cargo instanceof UnitPayload u?u.unit.type.fullIcon:cargo instanceof BuildPayload build?build.block().fullIcon:null;
        if(!NativeAtlasMesh.valid(art))return;
        float width=art.width/(4f*Vars.tilesize),length=art.height/(4f*Vars.tilesize),scale=Math.min(1,size*.82f/Math.max(width,length));
        out.top(art,cargo.x()/Vars.tilesize,y,-cargo.y()/Vars.tilesize,width*scale,length*scale,90-cargo.rotation(),1,1,1,false);
    }
    private Model create(SandboxModelProfile p){
        float[][][] parts=new float[3][][];
        float u=.14f,v=.14f;var art=p.block().region;
        if(art instanceof TextureAtlas.AtlasRegion region){var pix=Core.atlas.getPixmap(region);if(pix!=null&&!pix.pixmap.isDisposed()){
            double best=Double.POSITIVE_INFINITY;
            for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>220){
                float a=x/(float)Math.max(1,pix.width-1),b=y/(float)Math.max(1,pix.height-1);double d=(a-.14)*(a-.14)+(b-.14)*(b-.14);
                if(d<best){best=d;u=a;v=b;}
            }
        }}
        for(int lod=0;lod<3;lod++){var raw=SandboxModelGeometry.model(p,lod);parts[lod]=new float[][]{material(raw.body(),u,v),material(raw.dark(),.5f,.5f),material(raw.lights(),.5f,.5f),material(raw.moving(),u,v)};for(var a:parts[lod])bytes+=(long)a.length*4;}
        return new Model(p.block().region,parts);
    }
    private static float[] material(float[] a,float u,float v){var copy=a.clone();for(int i=0;i<copy.length;i+=8)if(copy[i+6]<0){copy[i+6]=u;copy[i+7]=v;}return copy;}
}
