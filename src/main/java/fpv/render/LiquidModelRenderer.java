package fpv.render;

import arc.Core;import arc.graphics.g2d.*;import arc.math.Mathf;import arc.util.Time;import java.util.IdentityHashMap;
import mindustry.gen.Building;import mindustry.world.Block;import mindustry.world.blocks.liquid.Conduit;import mindustry.world.blocks.production.Pump;import mindustry.world.blocks.power.ThermalGenerator;

/** Read-only native quantities/flow observations. Never starts flow polling or changes liquid/heat state. */
final class LiquidModelRenderer {
    private static final class Model{TextureRegion region;float u,v;final float[][][] parts=new float[48][][];}
    private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();private Object atlas;private long builds,bytes;private final double[] basis=new double[9],moving=new double[9];
    static boolean supports(Block b){return LiquidModelProfile.forBlock(b)!=null;}
    static float visualHeight(Block b,float physical){var p=LiquidModelProfile.forBlock(b);return p==null?physical:Math.max(physical,p.height);}
    long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}void clear(){cache.clear();atlas=null;builds=bytes=0;}
    static float finite(float v){return Float.isFinite(v)?Mathf.clamp(v):0;}
    static float fill(Building b){return b.liquids==null?0:finite(b.liquids.currentAmount()/Math.max(.001f,b.block.liquidCapacity));}
    static float flow(Building b){if(b.liquids==null||fill(b)<=0)return 0;float rate=b.liquids.getFlowRate(b.liquids.current());return Float.isFinite(rate)&&rate>0?Math.min(1,rate):0;}
    static float activity(Building b){if(!b.enabled||!(b.efficiency>0))return 0;if(b instanceof Pump.PumpBuild p)return p.liquidDrop!=null&&p.amount>0&&p.shouldConsume()?finite(p.warmup):0;if(b instanceof ThermalGenerator.ThermalGeneratorBuild g)return finite(g.productionEfficiency);return 0;}
    static float clock(Building b){return b instanceof Pump.PumpBuild p&&Float.isFinite(p.totalProgress)?p.totalProgress:0;}
    static int openings(Building b){
        if(!(b.block instanceof Conduit type))return 15;int mask=1<<(b.rotation&3);
        for(int side=0;side<4;side++){var other=b.nearby(side);if(other!=null&&other.team==b.team&&type.blends(b.tile,b.rotation,other.tileX(),other.tileY(),other.rotation,other.block))mask|=1<<side;}
        return Integer.bitCount(mask)==1?mask|1<<((b.rotation+2)&3):mask;
    }
    boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float ignored,float physical,float distance){
        var p=LiquidModelProfile.forBlock(b.block);var region=NativeRegions.block(b.block);if(p==null||!NativeAtlasMesh.valid(region))return false;if(atlas!=Core.atlas){clear();atlas=Core.atlas;}var m=cache.get(b.block);if(m==null||m.region!=region){m=create(region);cache.put(b.block,m);builds++;}
        int lod=LogisticsModelProfile.lod(distance),mask=openings(b),index=lod*16+mask;var a=m.parts[index];if(a==null){var g=LiquidModelGeometry.model(p,mask,lod);a=new float[][]{material(g.body(),m,false),material(g.lining(),m,false),material(g.rotor(),m,false),material(g.fluid(),m,true),material(g.lights(),m,true)};m.parts[index]=a;for(var v:a)bytes+=(long)v.length*4;}
        float size=b.block.size,h=visualHeight(b.block,physical);SpriteExtrusionGeometry.basis(90,0,0,basis);out.foundationExtrusion(m.region,a[0],x,base,z,size,h,size,basis,1,1,1);out.extrusion(m.region,a[1],x,base,z,size,h,size,basis,.14f,.14f,.14f);
        if(p.pump()&&lod<2){SpriteExtrusionGeometry.basis(90+clock(b)*3%360,0,0,moving);out.extrusion(m.region,a[2],x,base,z,size,h,size,moving,1,1,1);}
        if(lod==2||p==LiquidModelProfile.THERMAL)out.top(m.region,x,base+h*(lod==2?.835f:.825f),z,size*(p==LiquidModelProfile.THERMAL?.34f:.86f),size*(p==LiquidModelProfile.THERMAL?.34f:.86f),0,1,1,1,false);
        float amount=fill(b);if(amount>.001f&&b.liquids!=null){var c=b.liquids.current().color;
            if(p.pipe()){
                fluid(out,a[3],x,base+.045f,z,.15f,.075f*amount,.15f,c);
                for(int side=0;side<4;side++)if((mask&(1<<side))!=0){boolean east=side==0||side==2;float dx=side==0?.325f:side==2?-.325f:0,dz=side==1?-.325f:side==3?.325f:0;fluid(out,a[3],x+dx,base+.045f,z+dz,east?.175f:.15f,.075f*amount,east?.15f:.175f,c);}
            }else if(p.storage())fluid(out,a[3],x,base+h*.13f,z,size*.24f,h*.59f*amount,size*.24f,c);
            else if(lod<2)fluid(out,a[3],x,base+h*.12f,z,size*.13f,h*.35f*amount,size*.13f,c);
        }
        float active=p.pump()||p==LiquidModelProfile.THERMAL?activity(b):flow(b),time=Float.isFinite(Time.time)?Time.time:0,pulse=active>0?.80f+.15f*Mathf.sin(time/10f):1;
        float glow=.08f+active*.75f*pulse;var tint=p==LiquidModelProfile.THERMAL?mindustry.graphics.Pal.lightOrange:b.liquids!=null&&amount>0?b.liquids.current().color:mindustry.graphics.Pal.powerLight;
        out.extrusion(Core.atlas.white(),a[4],x,base,z,size,h,size,basis,tint.r*glow,tint.g*glow,tint.b*glow);return true;
    }
    private void fluid(NativeAtlasMesh.Builder out,float[] mesh,float x,float base,float z,float halfX,float height,float halfZ,arc.graphics.Color c){out.extrusion(Core.atlas.white(),mesh,x,base,z,halfX,height,halfZ,basis,c.r,c.g,c.b);}
    private Model create(TextureRegion region){var m=new Model();m.region=region;m.u=m.v=.14f;if(m.region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){double best=Double.POSITIVE_INFINITY;for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>=200){float u=x/(float)Math.max(1,pix.width-1),v=y/(float)Math.max(1,pix.height-1);double d=(u-.14)*(u-.14)+(v-.14)*(v-.14);if(d<best){best=d;m.u=u;m.v=v;}}}}return m;}
    private static float[] material(float[] raw,Model m,boolean white){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(white||a[i+6]<0){a[i+6]=white?.5f:m.u;a[i+7]=white?.5f:m.v;}return a;}
}
