package fpv.render;

import arc.struct.IntMap;
import fpv.construction.LayerCoordinates.Bounds;
import fpv.construction.LayerWorld;
import fpv.world.NaturalProps;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.*;
import java.util.function.ToDoubleFunction;

/** Derived, local-only presentation. No tile edits, native RNG, physics or serialization. */
public final class VegetationInstances {
    public enum Form { PINE, LEAF, DEAD, SHRUB, SPORE, FROND, CORAL, DIAMOND }
    public static final int NEIGHBOUR_RADIUS=3;
    public static final float MAX_HEIGHT_SCALE=1.20f,MAX_WIDTH_SCALE=1.12f,MAX_OFFSET=.16f,MAX_LEAN=.15f;
    public record Instance(Form form,int variant,float dx,float dz,float width,float height,float depth,float yaw,float leanX,float leanZ){}
    private final IntMap<Instance> cache=new IntMap<>();
    private Object tiles;
    private LayerWorld structures;
    private long builds;
    public long builds(){return builds;}
    public int size(){return cache.size;}
    public void structures(LayerWorld value){structures=value;clear();}
    public void clear(){cache.clear();tiles=Vars.world==null?null:Vars.world.tiles;}
    public void invalidate(int x,int y,int radius){
        for(int yy=y-radius;yy<=y+radius;yy++)for(int xx=x-radius;xx<=x+radius;xx++)cache.remove(arc.math.geom.Point2.pack(xx,yy));
    }
    public static Form form(Block b){
        var kind=NaturalProps.kind(b);if(kind==null||kind==NaturalProps.Kind.ROCK)return null;
        if(b==Blocks.redDiamondWall)return Form.DIAMOND;
        if(b==Blocks.sporeCluster)return Form.SPORE;
        if(b instanceof Seaweed)return Form.FROND;
        if(b==Blocks.yellowCoral)return Form.CORAL;
        return switch(kind){case PINE->Form.PINE;case LEAF_TREE->Form.LEAF;case DEAD_TREE->Form.DEAD;default->Form.SHRUB;};
    }
    /** Stable name hashing avoids dependence on mod load order/content numeric IDs. */
    public static int seed(int x,int y,String content){return mix(x*0x1f123bb5^y*0x5f356495^content.hashCode());}
    private static int mix(int s){s^=s>>>16;s*=0x7feb352d;s^=s>>>15;s*=0x846ca68b;return s^(s>>>16);}
    private static float random(int seed,int salt){return (mix(seed+salt)&0xffffff)/16777215f;}
    public Instance get(Tile t,float width,float height,ToDoubleFunction<Tile> obstacleHeight){
        if(tiles!=Vars.world.tiles){clear();tiles=Vars.world.tiles;}
        Instance cached=cache.get(t.pos());if(cached!=null)return cached;
        Form form=form(t.block());if(form==null)throw new IllegalArgumentException("Vegetation required");
        int seed=seed(t.x,t.y,t.block().name);float density=0,awayX=0,awayY=0;
        for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)if(x!=0||y!=0){
            Tile n=Vars.world.tile(t.x+x,t.y+y);if(n==null)continue;
            float weight=x==0||y==0?1:.5f;
            if(form(n.block())!=null){density+=weight;awayX-=x*weight;awayY-=y*weight;}
            else if(n.block().solid){awayX-=x*2*weight;awayY-=y*2*weight;}
        }
        float dx=clamp((random(seed,31)-.5f)*.16f+awayX*.035f,-MAX_OFFSET,MAX_OFFSET);
        float dy=clamp((random(seed,61)-.5f)*.16f+awayY*.035f,-MAX_OFFSET,MAX_OFFSET);
        // Crowns get narrower, not deleted; every logical plant still has a visible instance.
        float packing=Math.max(.34f,1/(1+.35f*density));
        float w=width*5*(.82f+.30f*random(seed,101))*packing;
        float d=w*(.88f+.12f*random(seed,211));
        float h=height*5*(.82f+.38f*random(seed,307))*(.92f+.08f*packing);
        if(form==Form.SHRUB)h*=.48f;
        float leanX=(random(seed,401)-.5f)*MAX_LEAN*2,leanZ=(random(seed,503)-.5f)*MAX_LEAN*2;
        float extent=Math.max(w,d)*.495f+MAX_LEAN;
        // Only tall neighbouring solids constrain crowns. Low walls may have natural overhang.
        float reach=extent;
        for(int y=-NEIGHBOUR_RADIUS;y<=NEIGHBOUR_RADIUS;y++)for(int x=-NEIGHBOUR_RADIUS;x<=NEIGHBOUR_RADIUS;x++){
            Tile n=Vars.world.tile(t.x+x,t.y+y);if(n==null||n==t||NaturalProps.supported(n.block())&&!n.block().solid)continue;
            if(form(n.block())!=null)continue;
            if(obstacleHeight.applyAsDouble(n)<h*.28f)continue;
            double ax=Math.max(0,Math.abs(x-dx)-.5),ay=Math.max(0,Math.abs(y-dy)-.5);
            reach=Math.min(reach,Math.max(.25f,(float)Math.hypot(ax,ay)-.04f));
        }
        if(reach<extent){float fit=reach/extent;w*=fit;d*=fit;leanX*=fit;leanZ*=fit;}
        if(structures!=null&&structures.size()>0){
            float r=Math.max(w,d)*.495f+Math.max(Math.abs(leanX),Math.abs(leanZ));
            for(var c:structures.query(new Bounds(t.x+dx-r,t.y+dy-r,0,t.x+dx+r,t.y+dy+r,h))){
                var b=structures.coordinates().bounds(c);
                if(b.minZ()>.05)h=Math.min(h,Math.max(.10f,(float)b.minZ()-.05f));
            }
        }
        Instance result=new Instance(form,seed&7,dx,-dy,w,h,d,random(seed,601)*360,leanX,leanZ);
        cache.put(t.pos(),result);builds++;return result;
    }
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
}
