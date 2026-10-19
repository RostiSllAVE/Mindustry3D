package fpv.render;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Tile;
import java.util.function.Predicate;

/** Presentation-only corner sampler. Never used by terrain, collision, picking or save code. */
public final class FloorSurfaceRelief {
    public static final float MAX_HEIGHT = .12f;
    /** Also covers neighbouring chunks when a footprint changes beside a cached chunk. */
    public static final int ANCHOR_RADIUS = 2;
    public static final float ORE_OFFSET = .006f;
    private final int chunk;
    private final float[] corners;
    private final float[] materials;
    private int originX, originY;

    public FloorSurfaceRelief(int chunk) {
        if(chunk<4)throw new IllegalArgumentException("Chunk must contain a relief interior");
        this.chunk=chunk;
        corners=new float[(chunk*2+1)*(chunk*2+1)];
        materials=new float[(chunk+4)*(chunk+4)];
    }

    /** Reused scratch data, evaluated only when the existing detailed mesh is rebuilt. */
    public void prepare(int chunkX,int chunkY,Predicate<Tile> visible) {
        originX=chunkX*chunk;originY=chunkY*chunk;
        for(int y=-2;y<=chunk+1;y++)for(int x=-2;x<=chunk+1;x++)
            materials[(y+2)*(chunk+4)+x+2]=weight(Vars.world.tile(originX+x,originY+y),visible);
        for(int y=0;y<=chunk*2;y++)for(int x=0;x<=chunk*2;x++)
            corners[y*(chunk*2+1)+x]=sample(originX+x*.5,originY+y*.5,visible,true);
    }
    public float corner(int x,int y){return node((x-originX)*2,(y-originY)*2);}
    public float center(int x,int y){return node((x-originX)*2+1,(y-originY)*2+1);}
    public float shade(int x,int y,boolean center){
        int nx=(x-originX)*2+(center?1:0),ny=(y-originY)*2+(center?1:0);
        if(nx==0||ny==0||nx==chunk*2||ny==chunk*2||node(nx,ny)==0)return 1;
        // Shared half-tile samples provide coherent vertex lighting, including ore overlays.
        float dx=node(nx+1,ny)-node(nx-1,ny),dy=node(nx,ny+1)-node(nx,ny-1);
        // Native pixel-art floors have strong baked detail: a restrained physical Lambert term
        // disappears inside that texture. Amplify only the visual slope contrast, not elevation.
        return Math.max(.76f,Math.min(1.18f,1+dx*5f+dy*3.4f));
    }
    private float node(int x,int y){return corners[y*(chunk*2+1)+x];}
    public int scratchBytes(){return (corners.length+materials.length)*Float.BYTES;}

    /** Corner (x,y) is at native (x-.5,y-.5). Shared coordinates produce identical heights. */
    public float sample(double x,double y,Predicate<Tile> visible) {
        return sample(x,y,visible,false);
    }
    private float sample(double x,double y,Predicate<Tile> visible,boolean cached) {
        // The far tier stays flat. A smooth two-tile apron joins ANY near/far/fog chunk mix
        // without skirts, extra triangles, camera-dependent mesh rebuilds or T-junctions.
        double lx=x-Math.floor(x/chunk)*chunk,ly=y-Math.floor(y/chunk)*chunk;
        float apron=smooth((float)(Math.min(Math.min(lx,chunk-lx),Math.min(ly,chunk-ly))*.5));
        if(apron==0)return 0;
        int bx=(int)Math.floor(x-.5),by=(int)Math.floor(y-.5);
        double fx=x-.5-bx,fy=y-.5-by;
        double material=lerp(lerp(weight(bx,by,visible,cached),weight(bx+1,by,visible,cached),fx),
            lerp(weight(bx,by+1,visible,cached),weight(bx+1,by+1,visible,cached),fx),fy);
        if(material==0)return 0;
        float clearance=1;
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
        for(int dy=-2;dy<=1;dy++)for(int dx=-2;dx<=1;dx++) {
            int tx=ix+dx,ty=iy+dy;
            if(weight(tx,ty,visible,cached)!=0)continue;
            double ax=Math.max(0,Math.max(tx-x,x-tx-1)),ay=Math.max(0,Math.max(ty-y,y-ty-1));
            clearance=Math.min(clearance,smooth((float)Math.sqrt(ax*ax+ay*ay)));
        }
        double noise=.72*noise(x*.19,y*.19)+.28*noise(x*.39+37.25,y*.39-19.5);
        return (float)(MAX_HEIGHT*material*apron*clearance*noise);
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
