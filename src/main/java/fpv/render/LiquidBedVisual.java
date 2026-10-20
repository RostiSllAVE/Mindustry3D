package fpv.render;

import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.world.Tile;

/** Cached bathymetry for presentation only. Never consulted by LiquidBody/flight/collision. */
public final class LiquidBedVisual {
    public static final int MAX_AXIS=1024;
    public static final float DEEP_DEPTH=6f, TRANSITION_TILES=8f;
    private static Object tiles,content;
    private static int width,height,gw,gh;
    private static float[] distance;
    private static short[] floors;
    private static long builds;
    private static double generationMs;
    private LiquidBedVisual(){}
    public static void clear(){tiles=content=null;distance=null;floors=null;}
    public static long builds(){return builds;}
    public static double generationMs(){return generationMs;}
    public static int cachedBytes(){return distance==null?0:distance.length*6;}
    public static boolean deep(Tile t){return t!=null&&t.floor().isDeep()&&t.floor().liquidDrop==Liquids.water;}
    /** Outside neighbours continue the boundary material; actual land/structures still pin shores. */
    public static Tile edgeTile(int x,int y){
        if(Vars.world==null||Vars.world.width()==0||Vars.world.height()==0)return null;
        Tile actual=Vars.world.tile(x,y);if(actual!=null)return actual;
        Tile edge=Vars.world.tile(Math.max(0,Math.min(Vars.world.width()-1,x)),Math.max(0,Math.min(Vars.world.height()-1,y)));
        return edge.floor().liquidDrop==Liquids.water?edge:null;
    }
    public static void invalidate(Tile tile){
        if(distance==null||tile==null)return;
        int x=(int)((long)tile.x*(gw-1)/Math.max(1,width-1)),y=(int)((long)tile.y*(gh-1)/Math.max(1,height-1));
        if(floors[y*gw+x]!=tile.floorID())clear();
    }
    private static void ensure(){
        if(tiles==Vars.world.tiles&&content==Vars.content&&distance!=null)return;
        long start=System.nanoTime();width=Vars.world.width();height=Vars.world.height();gw=Math.min(MAX_AXIS,width);gh=Math.min(MAX_AXIS,height);
        if(gw<1||gh<1)return;
        int count=gw*gh;distance=new float[count];floors=new short[count];int[] queue=new int[count];int head=0,tail=0;
        for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){
            Tile t=Vars.world.tile(coord(x,gw,width),coord(y,gh,height));int at=y*gw+x;floors[at]=t.floorID();
            distance[at]=deep(t)?Float.POSITIVE_INFINITY:0;
            if(!deep(t))queue[tail++]=at;
        }
        // Unit-cost distance transform, scaled to world tiles on exceptionally large maps.
        while(head<tail){int at=queue[head++],x=at%gw,y=at/gw;float d=distance[at]+1;
            if(x>0&&distance[at-1]==Float.POSITIVE_INFINITY){distance[at-1]=d;queue[tail++]=at-1;}
            if(x+1<gw&&distance[at+1]==Float.POSITIVE_INFINITY){distance[at+1]=d;queue[tail++]=at+1;}
            if(y>0&&distance[at-gw]==Float.POSITIVE_INFINITY){distance[at-gw]=d;queue[tail++]=at-gw;}
            if(y+1<gh&&distance[at+gw]==Float.POSITIVE_INFINITY){distance[at+gw]=d;queue[tail++]=at+gw;}
        }
        float scale=Math.min(width/(float)gw,height/(float)gh);
        for(int i=0;i<count;i++)distance[i]=Math.min(TRANSITION_TILES,distance[i]*scale);
        tiles=Vars.world.tiles;content=Vars.content;builds++;generationMs=(System.nanoTime()-start)/1e6;
    }
    private static int coord(int i,int n,int length){return n==1?0:(int)((long)i*(length-1)/(n-1));}
    public static float depth(Tile tile){
        if(tile==null||!tile.floor().isLiquid)return 0;
        if(!deep(tile))return (float)fpv.environment.LiquidBody.depth(tile.floor());
        ensure();float x=tile.x*(gw-1f)/Math.max(1,width-1),y=tile.y*(gh-1f)/Math.max(1,height-1);
        int ix=(int)x,iy=(int)y,jx=Math.min(gw-1,ix+1),jy=Math.min(gh-1,iy+1);float tx=x-ix,ty=y-iy;
        float d=(1-ty)*((1-tx)*distance[iy*gw+ix]+tx*distance[iy*gw+jx])+ty*((1-tx)*distance[jy*gw+ix]+tx*distance[jy*gw+jx]);
        float t=Math.max(0,Math.min(1,d/TRANSITION_TILES));t=t*t*(3-2*t);
        return .95f+(DEEP_DEPTH-.95f)*t;
    }
    public static float corner(int x,int y){float d=0;
        for(int j=-1;j<=0;j++)for(int i=-1;i<=0;i++){Tile tile=edgeTile(x+i,y+j);if(!LiquidSurfaceVisual.open(tile))return 0;d+=depth(tile);}
        return -d*.25f;
    }
    /** Aggregation is safe only on a level bed. Gradient regions retain native tile fans. */
    public static boolean flat(int x,int y,int w,int h){
        float d=-depth(Vars.world.tile(x,y));
        for(int yy=y;yy<=y+h;yy++)for(int xx=x;xx<=x+w;xx++)if(Math.abs(corner(xx,yy)-d)>.0001f)return false;
        return true;
    }
}
