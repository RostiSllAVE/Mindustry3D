package fpv.render;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.*;
import java.util.IdentityHashMap;

/** Read-only native transport presentation; the same path handles Z0 and layer-local native buildings. */
final class LogisticsModelRenderer {
    private static final class Model {TextureRegion region;final float[][][] parts=new float[48][][];float u,v;}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private final double[] basis=new double[9];private final TextureRegion clipped=new TextureRegion();
    private Object atlas;private long builds,bytes;
    static boolean supports(Block b){return LogisticsModelProfile.forBlock(b)!=null;}
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}
    void clear(){cache.clear();atlas=null;bytes=builds=0;}
    static int openings(Building b){
        int output=b.rotation&3,inputs=0;
        for(int side=0;side<4;side++)if(side!=output){
            var other=b.nearby(side);if(other==null||other.team!=b.team)continue;
            if(other instanceof Conveyor.ConveyorBuild||other instanceof StackConveyor.StackConveyorBuild){if((other.rotation&3)!=((side+2)&3))continue;}
            else if(!other.block.outputsItems())continue;
            boolean valid=b instanceof Conveyor.ConveyorBuild?((Conveyor)b.block).blends(b.tile,b.rotation,other.tileX(),other.tileY(),other.rotation,other.block):other instanceof StackConveyor.StackConveyorBuild||((StackConveyor.StackConveyorBuild)b).state==1;
            if(valid)inputs|=1<<side;
        }
        // Only use the conventional rear mouth when there is no actual input. A bend must
        // close its unused rear edge instead of looking like a three-way open chassis.
        int mask=(1<<output)|(inputs==0?1<<((output+2)&3):inputs);
        return ((mask>>>output)|(mask<<(4-output)))&15;
    }
    static boolean cargoConnection(Building b,int side){
        var other=b.nearby(side);
        if(!(other instanceof Conveyor.ConveyorBuild)||other.team!=b.team)return false;
        boolean outgoing=(b.rotation&3)==side,incoming=(other.rotation&3)==((side+2)&3);
        if(outgoing==incoming)return false; // no parallel or head-on pseudo-connection
        var type=(Conveyor)b.block;var target=(Conveyor)other.block;
        return type.blends(b.tile,b.rotation,other.tileX(),other.tileY(),other.rotation,other.block)&&target.blends(other.tile,other.rotation,b.tileX(),b.tileY(),b.rotation,b.block);
    }
    /** Packed links are resolved through the owning Tile view, never a same-XY Z0 building. */
    static Tile stackSource(StackConveyor.StackConveyorBuild b){return b.link==-1||b.tile==null?null:b.tile.nearby(Point2.x(b.link)-b.tile.x,Point2.y(b.link)-b.tile.y);}
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float ignoredSize,float height,float distanceSquared){
        var profile=LogisticsModelProfile.forBlock(b.block);if(profile==null)return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        TextureRegion region=NativeRegions.block(b.block);if(!NativeAtlasMesh.valid(region))return false;
        var model=cache.get(b.block);if(model==null||model.region!=region){model=new Model();model.region=region;sample(model);cache.put(b.block,model);builds++;}
        int lod=LogisticsModelProfile.lod(distanceSquared),mask=profile.belt?openings(b):15,index=lod*16+mask;
        var parts=model.parts[index];
        if(parts==null){var raw=LogisticsModelGeometry.model(profile,mask,lod);parts=new float[][]{material(raw.body(),model.u,model.v),material(raw.trim(),model.u,model.v),material(raw.lining(),model.u,model.v)};model.parts[index]=parts;for(var a:parts)bytes+=(long)a.length*4;}
        // Exact native footprint joins adjacent cells; the old generic Z0 .93 inset is not used for belts.
        float size=b.block.size,angle=profile.belt?b.rotation*90:0;SpriteExtrusionGeometry.basis(90+angle,0,0,basis);
        out.extrusion(region,parts[0],x,base,z,size,height,size,basis,1,1,1);
        out.extrusion(region,parts[1],x,base,z,size,height,size,basis,profile.heavy?.8f:1,profile.heavy?.85f:1,1);
        out.extrusion(region,parts[2],x,base,z,size,height,size,basis,.14f,.14f,.14f);
        if(b instanceof Conveyor.ConveyorBuild belt){
            var type=(Conveyor)b.block;float top=base+height*LogisticsModelGeometry.BELT_SURFACE+.002f;
            var frames=type.regions;int blend=Math.max(0,Math.min(frames.length-1,belt.blendbits));
            out.top(frames[blend][NativeBlockAnimationAdapter.conveyorFrame(belt)],x,top,z,size*belt.blendsclx,size*belt.blendscly,-b.rotation*90,1,1,1,false);
            float dx=Mathf.cosDeg(b.rotation*90),dy=Mathf.sinDeg(b.rotation*90);
            float left=x-size*.5f,right=x+size*.5f,near=z-size*.5f,far=z+size*.5f;
            // Preserve full cargo across a real same-floor belt seam. Keep clipping against
            // walls, closed ends, unrelated neighbors and other floors. No depth bypass.
            if(cargoConnection(b,0))right+=.15f;if(cargoConnection(b,1))near-=.15f;
            if(cargoConnection(b,2))left-=.15f;if(cargoConnection(b,3))far+=.15f;
            for(int i=0;i<belt.len;i++)if(belt.ids[i]!=null){float along=belt.ys[i]-.5f,across=belt.xs[i]*.5f;
                itemBounds(out,belt.ids[i].fullIcon,x+dx*along-dy*across,top+.025f,z-dy*along-dx*across,.30f,left,near,right,far);
            }
        }else if(b instanceof StackConveyor.StackConveyorBuild stack){
            var type=(StackConveyor)b.block;float top=base+height*LogisticsModelGeometry.BELT_SURFACE+.002f;
            out.top(type.regions[Math.max(0,Math.min(type.regions.length-1,stack.state))],x,top,z,size,size,-b.rotation*90,1,1,1,false);
            for(int side=0;side<4;side++)if((stack.blendprox&(1<<side))==0)out.top(type.edgeRegion,x,top+.002f,z,size,size,-(b.rotation-side)*90,1,1,1,false);
            var source=stackSource(stack);
            if(source!=null&&source.build instanceof StackConveyor.StackConveyorBuild&&source.build.team==b.team&&stack.lastItem!=null&&stack.items!=null&&stack.items.total()>0){float f=Float.isFinite(stack.cooldown)?Mathf.clamp(1-stack.cooldown):1;
                float sx=source.build==null?source.worldx()/mindustry.Vars.tilesize:source.build.x/mindustry.Vars.tilesize,sz=source.build==null?-source.worldy()/mindustry.Vars.tilesize:-source.build.y/mindustry.Vars.tilesize;
                // Invalid/stale links do not place cargo in a remote or different-floor cell.
                if(Math.abs(sx-x)<=1.01f&&Math.abs(sz-z)<=1.01f){float px=Mathf.lerp(sx,x,f),pz=Mathf.lerp(sz,z,f),fraction=Math.min(1,stack.items.total()/(float)Math.max(1,b.block.itemCapacity));
                    float tray=.70f;out.top(type.stackRegion,px,top+.012f,pz,tray,tray,-b.rotation*90,1,1,1,false);
                    item(out,stack.lastItem.fullIcon,px,top+.028f,pz,Mathf.lerp(.625f,.25f,fraction),x,z,size+2*(1-f));
                }
            }
        }else{
            float top=base+height+.002f;
            if(b instanceof Sorter.SorterBuild sorter){
                if(sorter.sortItem!=null){var c=sorter.sortItem.color;out.top(Core.atlas.white(),x,top-.001f,z,size*.96f,size*.96f,0,c.r,c.g,c.b,false);}
                else out.top(((Sorter)b.block).cross,x,top-.001f,z,size,size,0,1,1,1,false);
            }
            out.top(region,x,top,z,size*.98f,size*.98f,b.block.rotate?-b.rotation*90:0,1,1,1,false);
        }
        return true;
    }
    private void item(NativeAtlasMesh.Builder out,TextureRegion icon,float x,float y,float z,float width,float cellX,float cellZ,float cellSize){
        itemBounds(out,icon,x,y,z,width,cellX-cellSize*.5f,cellZ-cellSize*.5f,cellX+cellSize*.5f,cellZ+cellSize*.5f);
    }
    private void itemBounds(NativeAtlasMesh.Builder out,TextureRegion icon,float x,float y,float z,float width,float minX,float minZ,float maxX,float maxZ){
        if(!NativeAtlasMesh.valid(icon))return;float half=width*.5f,left=Math.max(x-half,minX),right=Math.min(x+half,maxX),near=Math.max(z-half,minZ),far=Math.min(z+half,maxZ);if(right<=left||far<=near)return;
        clipped.set(icon);clipped.u=Mathf.lerp(icon.u,icon.u2,(left-x+half)/width);clipped.u2=Mathf.lerp(icon.u,icon.u2,(right-x+half)/width);clipped.v=Mathf.lerp(icon.v,icon.v2,(near-z+half)/width);clipped.v2=Mathf.lerp(icon.v,icon.v2,(far-z+half)/width);
        out.top(clipped,(left+right)*.5f,y,(near+far)*.5f,right-left,far-near,0,1,1,1,false);
    }
    private static float[] material(float[] in,float u,float v){var a=in.clone();for(int i=0;i<a.length;i+=8)if(a[i+6]<0){a[i+6]=u;a[i+7]=v;}return a;}
    private static void sample(Model m){m.u=.14f;m.v=.14f;if(!(m.region instanceof arc.graphics.g2d.TextureAtlas.AtlasRegion r))return;
        var pix=Core.atlas.getPixmap(r);if(pix==null||pix.pixmap.isDisposed())return;double best=Double.POSITIVE_INFINITY;
        for(int yy=0;yy<pix.height;yy++)for(int xx=0;xx<pix.width;xx++)if(pix.getA(xx,yy)>=200){float u=xx/(float)Math.max(1,pix.width-1),v=yy/(float)Math.max(1,pix.height-1);double d=(u-.14)*(u-.14)+(v-.14)*(v-.14);if(d<best){best=d;m.u=u;m.v=v;}}
    }
}
