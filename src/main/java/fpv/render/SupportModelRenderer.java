package fpv.render;
import arc.Core;import arc.graphics.Color;import arc.graphics.g2d.*;import arc.math.Mathf;import mindustry.Vars;import mindustry.gen.Building;import mindustry.world.Block;import mindustry.world.blocks.defense.*;import mindustry.world.blocks.storage.StorageBlock;import java.util.IdentityHashMap;

/** Native readiness/charge/work state, with no target scans, healing calls or invented influence volumes. */
final class SupportModelRenderer {
    private static final class Model{TextureRegion region,top;final float[][][] parts=new float[3][][];}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();private Object atlas;private long builds,bytes;private final double[] basis=new double[9],moving=new double[9];private final Color tint=new Color();
    static boolean supports(Block b){return SupportModelProfile.forBlock(b)!=null;}long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}void clear(){cache.clear();atlas=null;builds=bytes=0;}
    static float finite(float v){return Float.isFinite(v)?Mathf.clamp(v):0;}
    /** Most-filled native item slot, not a fictitious aggregate capacity. Linked core limits remain native. */
    static float fullness(Building b){if(!(b instanceof StorageBlock.StorageBuild)||b.items==null||Vars.content==null)return 0;float ratio=0;for(int i=0;i<Vars.content.items().size;i++){var item=Vars.content.item(i);int cap=b.getMaximumAccepted(item);if(cap>0)ratio=Math.max(ratio,finite(b.items.get(item)/(float)cap));}return ratio;}
    static float level(Building b){
        if(b instanceof StorageBlock.StorageBuild)return fullness(b);
        if(!b.enabled)return 0;
        if(b instanceof ForceProjector.ForceBuild f){if(f.broken)return 0;var type=(ForceProjector)b.block;return finite(f.warmup)*finite(f.radscl)*finite(1-f.buildup/Math.max(1,type.shieldHealth+type.phaseShieldBoost*f.phaseHeat));}
        if(!(b.efficiency>0))return 0;
        if(b instanceof MendProjector.MendBuild m)return finite(m.heat)*finite(m.smoothEfficiency);
        if(b instanceof OverdriveProjector.OverdriveBuild o)return finite(o.heat)*finite(o.smoothEfficiency);
        if(b instanceof RegenProjector.RegenProjectorBuild r)return r.didRegen?finite(r.warmup):0;
        if(b instanceof BuildTurret.BuildTurretBuild t)return t.lastPlan!=null&&!t.lastPlan.removed||t.following!=null&&!t.following.dead&&t.following.plans.size>0?finite(t.warmup):0;return 0;
    }
    static float phase(Building b){if(b instanceof MendProjector.MendBuild m)return finite(m.charge/Math.max(1,((MendProjector)b.block).reload));if(b instanceof OverdriveProjector.OverdriveBuild o)return finite(o.charge/Math.max(1,((OverdriveProjector)b.block).reload));if(b instanceof RegenProjector.RegenProjectorBuild r)return Float.isFinite(r.totalTime)?r.totalTime%360/360:0;return 0;}
    static float booster(Building b){if(!b.enabled||!(b.efficiency>0))return 0;return b instanceof MendProjector.MendBuild m?finite(m.phaseHeat):b instanceof OverdriveProjector.OverdriveBuild o?finite(o.phaseHeat):b instanceof RegenProjector.RegenProjectorBuild?finite(b.optionalEfficiency):0;}
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float ignored,float h,float distance){
        var p=SupportModelProfile.forBlock(b.block);if(p==null)return false;TextureRegion region=b.block instanceof BuildTurret t&&NativeAtlasMesh.valid(t.baseRegion)?t.baseRegion:NativeRegions.block(b.block);if(!NativeAtlasMesh.valid(region))return false;if(atlas!=Core.atlas){clear();atlas=Core.atlas;}var m=cache.get(b.block);if(m==null||m.region!=region){m=create(b.block,p,region);cache.put(b.block,m);builds++;}
        int lod=ManufacturingModelProfile.lod(distance);float size=b.block.size;SpriteExtrusionGeometry.basis(90,0,0,basis);var a=m.parts[lod];out.extrusion(m.region,a[0],x,base,z,size,h,size,basis,1,1,1);out.extrusion(m.region,a[1],x,base,z,size,h,size,basis,.14f,.14f,.14f);
        float active=level(b),charge=phase(b),boost=booster(b),angle=charge*360;
        if(lod<2){if(b instanceof BuildTurret.BuildTurretBuild t){float yaw=t.buildRotation();SpriteExtrusionGeometry.basis(90+(Float.isFinite(yaw)?yaw:0),0,0,moving);out.extrusion(NativeRegions.block(b.block),a[2],x,base+h*.65f,z,size,h,size,moving,1,1,1);}else{SpriteExtrusionGeometry.basis(90+angle,0,0,moving);out.extrusion(m.region,a[2],x,base,z,size,h,size,moving,1,1,1);}}
        if(p.storage()||lod==2)out.top(m.top,x,base+h*.815f,z,size*.80f,size*.80f,0,1,1,1,false);
        else if(p!=SupportModelProfile.BUILD)out.top(m.top,x,base+h*.85f,z,size*.27f,size*.27f,0,1,1,1,false);
        if(b.block instanceof MendProjector t)tint.set(t.baseColor).lerp(t.phaseColor,boost);else if(b.block instanceof OverdriveProjector t)tint.set(t.baseColor).lerp(t.phaseColor,boost);else if(b.block instanceof RegenProjector t)tint.set(t.baseColor);else if(p==SupportModelProfile.FORCE)tint.set(b.team.color);else if(p.reinforced())tint.set(mindustry.graphics.Pal.berylShot);else tint.set(mindustry.graphics.Pal.powerLight);
        // Mend glow indicates readiness only. No outbound pulse falsely claims a heal occurred.
        float brightness=.08f+active*.75f;out.extrusion(Core.atlas.white(),a[3],x,p==SupportModelProfile.BUILD&&lod<2?base+h*.65f:base,z,size,h,size,p==SupportModelProfile.BUILD&&lod<2?moving:basis,tint.r*brightness,tint.g*brightness,tint.b*brightness);
        float bonus=.05f+boost*.7f;out.extrusion(Core.atlas.white(),a[4],x,base,z,size,h,size,basis,tint.r*bonus,tint.g*bonus,tint.b*bonus);return true;
    }
    private Model create(Block b,SupportModelProfile p,TextureRegion region){var m=new Model();m.region=region;m.top=b instanceof MendProjector t?t.topRegion:b instanceof OverdriveProjector t?t.topRegion:b instanceof ForceProjector t?t.topRegion:NativeRegions.block(b);if(!NativeAtlasMesh.valid(m.top))m.top=NativeRegions.block(b);float u=.14f,v=.14f;if(region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){double best=Double.POSITIVE_INFINITY;for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>=200){float U=x/(float)Math.max(1,pix.width-1),V=y/(float)Math.max(1,pix.height-1);double d=(U-.14)*(U-.14)+(V-.14)*(V-.14);if(d<best){best=d;u=U;v=V;}}}}
        for(int lod=0;lod<3;lod++){var g=SupportModelGeometry.model(p,b.size,lod);m.parts[lod]=new float[][]{material(g.body(),u,v,false),material(g.lining(),u,v,false),material(g.moving(),u,v,false),material(g.lights(),u,v,true),material(g.booster(),u,v,true)};for(var a:m.parts[lod])bytes+=(long)a.length*4;}return m;}
    private static float[] material(float[] raw,float u,float v,boolean white){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(white||a[i+6]<0){a[i+6]=white?.5f:u;a[i+7]=white?.5f:v;}return a;}
}
