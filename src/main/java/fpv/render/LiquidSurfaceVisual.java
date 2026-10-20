package fpv.render;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

/** Presentation only. Never use these levels for physics, picking, navigation or weapon origins. */
public final class LiquidSurfaceVisual {
    public static final String WAVES_SETTING="fpv-water-waves";
    public static final float DEPTH=.16f, WAVE_HEIGHT=.12f;
    private static final float CHUNK=(float)fpv.generated.RenderConfig.CHUNK_TILES;
    private LiquidSurfaceVisual(){}
    public static boolean open(Tile t){return t!=null&&t.floor().isLiquid&&t.block()==Blocks.air;}
    /** Unknown/modded liquids keep their own native texture and a static, lowered surface. */
    public static float motion(Floor f){
        if(f==Blocks.water||f==Blocks.deepwater||f==Blocks.taintedWater||f==Blocks.deepTaintedWater
            ||f==Blocks.sandWater||f==Blocks.darksandWater||f==Blocks.darksandTaintedWater)return 1;
        if(f==Blocks.cryofluid)return .4f;
        if(f==Blocks.tar)return .16f;
        if(f==Blocks.slag||f==Blocks.arkyciteFloor)return .22f;
        return 0;
    }
    /** Shared corner belongs to four native tiles. Land/structures pin it to physical Z0. */
    public static float corner(int x,int y){
        return open(LiquidBedVisual.edgeTile(x-1,y-1))&&open(LiquidBedVisual.edgeTile(x,y-1))
            &&open(LiquidBedVisual.edgeTile(x-1,y))&&open(LiquidBedVisual.edgeTile(x,y))?-DEPTH:0;
    }
    public static float center(Tile t){return open(t)?-DEPTH:0;}
    public static float cornerMotion(int x,int y){
        if(corner(x,y)==0)return 0;
        int kind=fpv.environment.LiquidBody.kind(LiquidBedVisual.edgeTile(x-1,y-1).floor());
        if(fpv.environment.LiquidBody.kind(LiquidBedVisual.edgeTile(x,y-1).floor())!=kind||fpv.environment.LiquidBody.kind(LiquidBedVisual.edgeTile(x-1,y).floor())!=kind||fpv.environment.LiquidBody.kind(LiquidBedVisual.edgeTile(x,y).floor())!=kind)return 0;
        float a=motion(LiquidBedVisual.edgeTile(x-1,y-1).floor()),b=motion(LiquidBedVisual.edgeTile(x,y-1).floor());
        float c=motion(LiquidBedVisual.edgeTile(x-1,y).floor()),d=motion(LiquidBedVisual.edgeTile(x,y).floor());
        return Math.min(Math.min(a,b),Math.min(c,d))*apron(x-.5f,-y+.5f);
    }
    public static float centerMotion(Tile t){
        if(!open(t))return 0;
        // An isolated pool can gently move at its centre while its shore remains exactly anchored.
        return motion(t.floor())*apron(t.x,-t.y);
    }
    /** Quiet chunk edges join the flat low-detail wave fallback without moving T-junctions. */
    public static float apron(float x,float z){
        float lx=mod(x+.5f,CHUNK),ly=mod(-z+.5f,CHUNK);
        float t=Math.min(1,Math.min(Math.min(lx,CHUNK-lx),Math.min(ly,CHUNK-ly))*.5f);
        return t*t*(3-2*t);
    }
    private static float mod(float n,float d){return n-(float)Math.floor(n/d)*d;}
    /** Same phase as the GLSL vertex shader; world coordinates, simulation seconds. */
    public static float wave(float x,float z,float seconds){
        return (float)(WAVE_HEIGHT*(.75+.25*Math.sin(x*.11+z*.09))*(.65*Math.sin(x*.73+z*.41+seconds*.9)+.35*Math.sin(x*-.37+z*.83-seconds*.63)));
    }
    public static float interpolate(float sw,float se,float ne,float nw,float c,float u,float v){
        if(v>=u&&v>=1-u)return sw*(v-u)+se*(u+v-1)+c*2*(1-v);
        if(v<=u&&v<=1-u)return nw*(1-u-v)+ne*(u-v)+c*2*v;
        if(u>=v&&u>=1-v)return se*(u+v-1)+ne*(u-v)+c*2*(1-u);
        return sw*(v-u)+nw*(1-u-v)+c*2*u;
    }
    public static float height(float x,float z,float seconds,boolean waves){
        int tx=(int)Math.floor(x+.5f),ty=(int)Math.floor(-z+.5f);Tile t=Vars.world.tile(tx,ty);
        if(t==null||!t.floor().isLiquid)return 0;
        float u=x-tx+.5f,v=z+ty+.5f;
        return interpolate(node(tx,ty,seconds,waves),node(tx+1,ty,seconds,waves),node(tx+1,ty+1,seconds,waves),
            node(tx,ty+1,seconds,waves),center(t)+(waves?centerMotion(t)*wave(tx,-ty,seconds):0),u,v);
    }
    private static float node(int x,int y,float seconds,boolean waves){return corner(x,y)+(waves?cornerMotion(x,y)*wave(x-.5f,-y+.5f,seconds):0);}
    /** Coarse cells may aggregate only a homogeneous open liquid area plus its one-tile halo. */
    public static boolean coarse(Tile sample,int x,int y,int width,int depth){
        if(!open(sample))return false;
        for(int yy=y-1;yy<=y+depth;yy++)for(int xx=x-1;xx<=x+width;xx++){
            Tile t=Vars.world.tile(xx,yy);if(!open(t)||t.floor()!=sample.floor()||t.overlay()!=Blocks.air)return false;
        }
        return LiquidBedVisual.flat(x,y,width,depth);
    }
    public static double navalOffset(double support,double physicalSurface,boolean afloat,float x,float z,float seconds,boolean waves){
        return afloat&&Math.abs(support-physicalSurface)<.10?height(x,z,seconds,waves):0;
    }
}
