package fpv.render;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Tile;
import java.util.function.Predicate;

/** Presentation-only corner sampler. Never used by terrain, collision, picking or save code. */
public final class FloorSurfaceRelief {
    public static final float MAX_HEIGHT = .065f;
    /** Also covers neighbouring chunks when a footprint changes beside a cached chunk. */
    public static final int ANCHOR_RADIUS = 2;
    private final int chunk;
    private final float[] corners;
    private final float[] materials;
    private int originX, originY;

    public FloorSurfaceRelief(int chunk) {
        if(chunk<4)throw new IllegalArgumentException("Chunk must contain a relief interior");
        this.chunk=chunk;
        corners=new float[(chunk+1)*(chunk+1)];
        materials=new float[(chunk+4)*(chunk+4)];
    }

    /** Reused scratch data, evaluated only when the existing detailed mesh is rebuilt. */
    public void prepare(int chunkX,int chunkY,Predicate<Tile> visible) {
        originX=chunkX*chunk;originY=chunkY*chunk;
        for(int y=-2;y<=chunk+1;y++)for(int x=-2;x<=chunk+1;x++)
            materials[(y+2)*(chunk+4)+x+2]=weight(Vars.world.tile(originX+x,originY+y),visible);
        for(int y=0;y<=chunk;y++)for(int x=0;x<=chunk;x++)
            corners[y*(chunk+1)+x]=sample(originX+x,originY+y,visible,true);
    }
    public float corner(int x,int y){return corners[(y-originY)*(chunk+1)+x-originX];}
    public int scratchBytes(){return (corners.length+materials.length)*Float.BYTES;}

    /** Corner (x,y) is at native (x-.5,y-.5). Shared coordinates produce identical heights. */
    public float sample(int x,int y,Predicate<Tile> visible) {
        return sample(x,y,visible,false);
    }
    private float sample(int x,int y,Predicate<Tile> visible,boolean cached) {
        // The far tier stays flat. A smooth two-tile apron joins ANY near/far/fog chunk mix
        // without skirts, extra triangles, camera-dependent mesh rebuilds or T-junctions.
        int lx=Math.floorMod(x,chunk),ly=Math.floorMod(y,chunk);
        float apron=smooth(Math.min(Math.min(lx,chunk-lx),Math.min(ly,chunk-ly))*.5f);
        if(apron==0)return 0;
        float material=0;
        for(int dy=-1;dy<=0;dy++)for(int dx=-1;dx<=0;dx++) {
            float w=weight(x+dx,y+dy,visible,cached);
            if(w==0)return 0;
            material+=w;
        }
        float clearance=1;
        // Distance to the actual fixed tile rectangle, not a random per-tile step.
        for(int dy=-2;dy<=1;dy++)for(int dx=-2;dx<=1;dx++) {
            if(weight(x+dx,y+dy,visible,cached)!=0)continue;
            int ax=Math.max(0,Math.max(dx,-dx-1)),ay=Math.max(0,Math.max(dy,-dy-1));
            clearance=Math.min(clearance,smooth((float)Math.sqrt(ax*ax+ay*ay)*.5f));
        }
        double noise=.72*noise(x*.19,y*.19)+.28*noise(x*.39+37.25,y*.39-19.5);
        return (float)(MAX_HEIGHT*material*.25*apron*clearance*noise);
    }

    private float weight(int x,int y,Predicate<Tile> visible,boolean cached){
        return cached?materials[(y-originY+2)*(chunk+4)+x-originX+2]:weight(Vars.world.tile(x,y),visible);
    }
    private static float weight(Tile t,Predicate<Tile> visible){
        if(t==null||t.block()!=Blocks.air||!visible.test(t)
            ||(t.overlay()!=Blocks.air&&!(t.overlay() instanceof mindustry.world.blocks.environment.OreBlock)))return 0;
        return amplitude(t.floor());
    }

    /** Native Floor has no natural/artificial flag. Opt in known native identities, never names/HP.
     * Unknown/modded, industrial, liquid, core-zone, vents and authored colour floors stay flat. */
    public static float amplitude(Block floor){
        if(floor==null||!(floor instanceof mindustry.world.blocks.environment.Floor f)||f.isLiquid)return 0;
        if(floor==Blocks.sand||floor==Blocks.darksand||floor==Blocks.snow||floor==Blocks.mud
            ||floor==Blocks.salt||floor==Blocks.iceSnow)return .65f;
        if(floor==Blocks.ice||floor==Blocks.redIce||floor==Blocks.crystalFloor)return .3f;
        if(floor==Blocks.stone||floor==Blocks.craters||floor==Blocks.charr||floor==Blocks.basalt
            ||floor==Blocks.hotrock||floor==Blocks.magmarock||floor==Blocks.dirt||floor==Blocks.dacite
            ||floor==Blocks.rhyolite||floor==Blocks.rhyoliteCrater||floor==Blocks.roughRhyolite
            ||floor==Blocks.regolith||floor==Blocks.yellowStone||floor==Blocks.carbonStone
            ||floor==Blocks.ferricStone||floor==Blocks.ferricCraters||floor==Blocks.beryllicStone
            ||floor==Blocks.crystallineStone||floor==Blocks.yellowStonePlates||floor==Blocks.redStone
            ||floor==Blocks.denseRedStone||floor==Blocks.arkyicStone||floor==Blocks.redmat
            ||floor==Blocks.bluemat||floor==Blocks.grass||floor==Blocks.shale||floor==Blocks.moss
            ||floor==Blocks.sporeMoss)return 1;
        return 0;
    }

    private static float smooth(float t){t=Math.max(0,Math.min(1,t));return t*t*t*(t*(t*6-15)+10);}
    private static double noise(double x,double y){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
        double u=smooth((float)(x-ix)),v=smooth((float)(y-iy));
        return lerp(lerp(hash(ix,iy),hash(ix+1,iy),u),lerp(hash(ix,iy+1),hash(ix+1,iy+1),u),v);
    }
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static double hash(int x,int y){int h=x*0x1f123bb5+y*0x5f356495+0x6c8e9cf5;h^=h>>>16;h*=0x7feb352d;h^=h>>>15;return (h&0x7fffffff)/1073741823.5-1;}
}
