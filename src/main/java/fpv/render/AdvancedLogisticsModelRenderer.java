package fpv.render;

import arc.Core;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.math.geom.Point2;
import fpv.construction.*;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.*;
import java.util.IdentityHashMap;

/** Read-only machinery in the existing depth/atlas stream, shared by ground and upper floors. */
final class AdvancedLogisticsModelRenderer {
    private static final class Model {
        TextureRegion base,top;float[][][] parts=new float[3][][];float[][] gun=new float[2][];
    }
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();
    private final IdentityHashMap<Building,Building> external=new IdentityHashMap<>();
    private final IdentityHashMap<Building,Building> externalIncoming=new IdentityHashMap<>();
    private final NativeBlockAnimationAdapter filters=new NativeBlockAnimationAdapter();
    private final double[] basis=new double[9];private Object atlas;private long builds,bytes;
    static boolean supports(Block block){return AdvancedLogisticsGeometry.kind(block)!=null;}
    /** Presentation envelope only. Distribution's native collision slab remains unchanged. */
    static float visualHeight(Block block,float physical){
        var kind=AdvancedLogisticsGeometry.kind(block);if(kind==null)return physical;
        return Math.max(physical,switch(kind){case DRIVER->.88f;case PHASE->.65f;case BRIDGE->.58f;case UNLOADER->.32f;case DIRECTIONAL->.30f;});
    }
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}
    void clear(){cache.clear();external.clear();externalIncoming.clear();filters.clear();atlas=null;bytes=builds=0;}
    /** Reuse only explicitly authorized existing cross-floor bridge edges. Never infer one from XY. */
    void begin(LayerRuntime runtime){
        external.clear();externalIncoming.clear();if(runtime==null)return;
        for(var edge:runtime.transfers().links())if(edge.kind()==LayerVerticalTransfers.Kind.BRIDGE_ITEMS){
            var from=endpoint(runtime,edge.from());var to=endpoint(runtime,edge.to());
            if(from!=null&&to!=null&&from!=to&&from.block==to.block&&from.block instanceof ItemBridge type&&from.team==to.team&&!from.dead&&!to.dead&&from.tile!=null&&to.tile!=null&&from.tile.build==from&&to.tile.build==to){
                double dx=(from.x-to.x)/(double)Vars.tilesize,dy=(from.y-to.y)/(double)Vars.tilesize;
                int a=from.tile instanceof LayerTile t?t.layer:0,c=to.tile instanceof LayerTile t?t.layer:0;
                double dz=runtime.planeHeight(a)-runtime.planeHeight(c);
                if(a!=c&&Math.abs(a-c)<=runtime.transfers().maxLayerGap()&&dx*dx+dy*dy+dz*dz<=type.range*(double)type.range+1e-6){external.put(from,to);externalIncoming.putIfAbsent(to,from);}
            }
        }
    }
    private static Building endpoint(LayerRuntime runtime,long id){
        if(id>0)return runtime.building(id);long packed=-id-1;
        return packed>=0&&packed<=0xffffffffL?Vars.world.build((int)packed):null;
    }
    /** Native XY packed links resolve through the owner's tile view, including same-XY stacked worlds. */
    static Building linked(Building b){
        int packed=b instanceof ItemBridge.ItemBridgeBuild bridge?bridge.link:b instanceof MassDriver.MassDriverBuild driver?driver.link:-1;
        if(packed==-1||b.tile==null)return null;Tile tile=b.tile.nearby(Point2.x(packed)-b.tile.x,Point2.y(packed)-b.tile.y);
        Building other=tile==null?null:tile.build;
        if(other==null||other==b||other.dead||other.team!=b.team||other.tile==null||other.tile.build!=other)return null;
        if(b.block instanceof ItemBridge bridge)return bridge.linkValid(b.tile,tile)?other:null;
        return b.block instanceof MassDriver type&&other instanceof MassDriver.MassDriverBuild&&other.block==type&&b.within(other,type.range)?other:null;
    }
    float heading(Building b){
        Building other=external.get(b);if(other==null)other=linked(b);if(other!=null)return b.angleTo(other);
        other=externalIncoming.get(b);if(other!=null)return other.angleTo(b);
        if(b instanceof ItemBridge.ItemBridgeBuild bridge)for(int i=0;i<bridge.incoming.size;i++){
            int packed=bridge.incoming.get(i);var t=b.tile.nearby(Point2.x(packed)-b.tile.x,Point2.y(packed)-b.tile.y);
            if(t!=null&&t.build instanceof ItemBridge.ItemBridgeBuild incoming&&linked(incoming)==b)return incoming.angleTo(b);
        }
        return b.rotation*90;
    }
    static float activity(Building b){
        if(!b.enabled||b.efficiency<=0)return 0;
        if(b instanceof ItemBridge.ItemBridgeBuild bridge)return finite(bridge.warmup)*finite(bridge.timeSpeed);
        if(b instanceof MassDriver.MassDriverBuild driver)return driver.state==MassDriver.DriverState.shooting&&linked(b)!=null&&b.items.total()>=((MassDriver)b.block).minDistribute?1-finite(driver.reloadCounter):0;
        return 0;
    }
    private static float finite(float v){return Float.isFinite(v)?Mathf.clamp(v):0;}
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float size,float height,float distanceSquared){
        var kind=AdvancedLogisticsGeometry.kind(b.block);if(kind==null)return false;
        height=visualHeight(b.block,height);
        if(atlas!=Core.atlas){cache.clear();filters.clear();atlas=Core.atlas;bytes=0;}
        var top=b.block.region;var baseRegion=b.block instanceof MassDriver driver?driver.baseRegion:top;
        if(!NativeAtlasMesh.valid(baseRegion)||!NativeAtlasMesh.valid(top))return false;
        var model=cache.get(b.block);if(model==null||model.base!=baseRegion||model.top!=top){
            model=new Model();model.base=baseRegion;model.top=top;float[] sample=sample(baseRegion);
            for(int lod=0;lod<3;lod++){var raw=AdvancedLogisticsGeometry.model(kind,lod);model.parts[lod]=new float[][]{material(raw.body(),sample),material(raw.lining(),sample),material(raw.mechanism(),sample)};for(var a:model.parts[lod])bytes+=(long)a.length*4;}
            if(kind==AdvancedLogisticsGeometry.Kind.DRIVER)for(int lod=0;lod<2;lod++){model.gun[lod]=silhouette(top,lod==0?16:6,lod==0?1800:420);bytes+=(long)model.gun[lod].length*4;}
            cache.put(b.block,model);builds++;
        }
        int lod=LogisticsModelProfile.lod(distanceSquared);var p=model.parts[lod];float yaw=b instanceof MassDriver.MassDriverBuild driver?(Float.isFinite(driver.rotation)?driver.rotation:90):heading(b);
        SpriteExtrusionGeometry.basis(90,0,0,basis);out.foundationExtrusion(baseRegion,p[0],x,base,z,size,height,size,basis,1,1,1);out.extrusion(baseRegion,p[1],x,base,z,size,height,size,basis,.13f,.13f,.13f);
        float recoil=b instanceof MassDriver.MassDriverBuild driver?finite(driver.reloadCounter)*((MassDriver)b.block).knockback/Vars.tilesize:0;
        float mx=x-Mathf.cosDeg(yaw)*recoil,mz=z+Mathf.sinDeg(yaw)*recoil;
        SpriteExtrusionGeometry.basis(90+yaw,0,0,basis);out.extrusion(baseRegion,p[2],mx,base,mz,size,height,size,basis,1,1,1);
        if(kind==AdvancedLogisticsGeometry.Kind.DRIVER){
            out.top(baseRegion,x,base+height*.285f,z,size,size,0,1,1,1,false);
            if(lod<2){SpriteExtrusionGeometry.basis(yaw,0,0,basis);float width=top.width*top.scl()/Vars.tilesize,depth=top.height*top.scl()/Vars.tilesize;
                out.extrusion(top,model.gun[lod],mx,base+height+.01f,mz,width,height*.24f,depth,basis,1,1,1);
            }else out.top(top,mx,base+height+.01f,mz,size,size,90-yaw,1,1,1,false);
            float charge=activity(b),flash=b instanceof MassDriver.MassDriverBuild driver?Math.max(0,(finite(driver.reloadCounter)-.88f)/.12f):0;
            if(lod<2){float light=.18f+.6f*charge+.22f*flash;out.top(Core.atlas.white(),mx,base+height*1.14f,mz,size*.19f,size*.10f,90-yaw,light,light*.72f,light*.3f,false);}
        }else if(kind==AdvancedLogisticsGeometry.Kind.UNLOADER){filters.render(b,out,x,base,z,size,height);}
        else if(kind==AdvancedLogisticsGeometry.Kind.DIRECTIONAL){
            var type=(DirectionalUnloader)b.block;var build=(DirectionalUnloader.DirectionalUnloaderBuild)b;
            out.top(top,x,base+height*.725f,z,size,size,0,1,1,1,false);out.top(type.topRegion,x,base+height+.006f,z,size,size,-b.rotation*90,1,1,1,false);
            var item=build.unloadItem;if(item!=null){var c=item.color;out.top(type.centerRegion,x,base+height+.01f,z,size,size,0,c.r,c.g,c.b,false);}
            else out.top(type.arrowRegion,x,base+height+.01f,z,size,size,-b.rotation*90,1,1,1,false);
        }else{
            var type=(ItemBridge)b.block;var bridge=(ItemBridge.ItemBridgeBuild)b;
            out.top(top,x,base+height*.385f,z,size,size,0,1,1,1,false);
            out.top(type.endRegion,x,base+height*1.015f,z,size*.80f,size*.80f,90-yaw,1,1,1,false);
            if(linked(b)!=null||external.containsKey(b)){
                float glow=kind==AdvancedLogisticsGeometry.Kind.PHASE?.3f+.7f*activity(b):.7f;
                out.top(type.arrowRegion,x,base+height*1.03f,z,size*.48f,size*.48f,-yaw,glow,glow,glow,false);
                if(kind==AdvancedLogisticsGeometry.Kind.PHASE&&lod<2){float pulse=Float.isFinite(bridge.time)?Mathf.sin(bridge.time*.15f)*.5f+.5f:0;float w=size*(.35f+.10f*activity(b)*pulse);
                    var c=mindustry.content.Items.phaseFabric.color;out.top(Core.atlas.white(),x,base+height*.80f,z,w,w,-yaw,c.r*glow,c.g*glow,c.b*glow,false);}
            }
        }
        return true;
    }
    private static float[] material(float[] raw,float[] sample){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(a[i+6]<0){a[i+6]=sample[0];a[i+7]=sample[1];}return a;}
    private static float[] sample(TextureRegion r){
        float u=.14f,v=.14f;if(r instanceof TextureAtlas.AtlasRegion a){var pix=Core.atlas.getPixmap(a);if(pix!=null&&!pix.pixmap.isDisposed()){double best=Double.POSITIVE_INFINITY;
            for(int yy=0;yy<pix.height;yy++)for(int xx=0;xx<pix.width;xx++)if(pix.getA(xx,yy)>=200){double d=Math.pow(xx/(double)Math.max(1,pix.width-1)-.14,2)+Math.pow(yy/(double)Math.max(1,pix.height-1)-.14,2);if(d<best){best=d;u=xx/(float)Math.max(1,pix.width-1);v=yy/(float)Math.max(1,pix.height-1);}}
        }}return new float[]{u,v};
    }
    private static float[] silhouette(TextureRegion r,int cells,int budget){
        if(r instanceof TextureAtlas.AtlasRegion a){var pix=Core.atlas.getPixmap(a);if(pix!=null&&!pix.pixmap.isDisposed()){var alpha=new byte[r.width*r.height];for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++)alpha[x+y*r.width]=(byte)pix.getA(r.isFlipX()?r.width-1-x:x,r.isFlipY()?r.height-1-y:y);return SpriteExtrusionGeometry.generate(alpha,r.width,r.height,cells,2,32,.2,budget).vertices();}}
        return SpriteExtrusionGeometry.generate(new byte[]{-1},1,1,1,1,32,.2,budget).vertices();
    }
}
