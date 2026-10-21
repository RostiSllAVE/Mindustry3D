package fpv.render;

/** Cached, presentation-only underside band around real solid/space boundaries. Native tops stay at Z0. */
public final class SpaceIslandGeometry {
    public static final int REACH=8,PAD=10;
    @FunctionalInterface public interface Source {int kind(int x,int y);} // 0 cosmic,1 solid,-1 absent/liquid/hidden
    @FunctionalInterface public interface Sink {void face(int tileX,int tileY,float[] xyz,float shade);}
    private final int x,y,w,h,side;private final int[] kind;private final float[] distance;
    public SpaceIslandGeometry(int x,int y,int w,int h,Source source){
        this.x=x-PAD;this.y=y-PAD;this.w=w;this.h=h;side=w+PAD*2;int height=h+PAD*2;
        kind=new int[side*height];distance=new float[kind.length];
        for(int yy=0;yy<height;yy++)for(int xx=0;xx<side;xx++){int i=xx+yy*side;kind[i]=source.kind(this.x+xx,this.y+yy);distance[i]=kind[i]==0?0:1000;}
        for(int yy=1;yy<height;yy++)for(int xx=1;xx<side;xx++){int i=xx+yy*side;distance[i]=Math.min(distance[i],Math.min(Math.min(distance[i-1],distance[i-side])+1,Math.min(distance[i-side-1],xx+1<side?distance[i-side+1]:1000)+1.414214f));}
        for(int yy=height-2;yy>=0;yy--)for(int xx=side-2;xx>=0;xx--){int i=xx+yy*side;distance[i]=Math.min(distance[i],Math.min(Math.min(distance[i+1],distance[i+side])+1,Math.min(distance[i+side+1],xx>0?distance[i+side-1]:1000)+1.414214f));}
    }
    private int at(int x,int y){return(x-this.x)+(y-this.y)*side;}
    private int solid(int x,int y){return kind[at(x,y)]==1?1:0;}
    private float[] node(int x,int y){
        float d=Math.min(Math.min(distance[at(x,y)],distance[at(x-1,y)]),Math.min(distance[at(x,y-1)],distance[at(x-1,y-1)]));
        float depth=.45f+Math.min(REACH,d)*(.65f+.12f*(float)Math.sin(x*.37+y*.21));
        float dx=(solid(x,y)+solid(x,y-1)-solid(x-1,y)-solid(x-1,y-1))*.12f;
        float dy=(solid(x,y)+solid(x-1,y)-solid(x,y-1)-solid(x-1,y-1))*.12f;
        return new float[]{x-.5f+dx,-depth,-y+.5f-dy};
    }
    public void emit(Sink sink){
        for(int yy=y+PAD;yy<y+PAD+h;yy++)for(int xx=x+PAD;xx<x+PAD+w;xx++){
            if(kind[at(xx,yy)]!=1||distance[at(xx,yy)]>REACH)continue;
            float[][] b={node(xx,yy),node(xx+1,yy),node(xx+1,yy+1),node(xx,yy+1)};
            sink.face(xx,yy,quad(b[0],b[3],b[2],b[1]),.68f);
            int[][] edges={{0,-1,0,1},{1,0,1,2},{0,1,2,3},{-1,0,3,0}};
            float[][] top={{xx-.5f,0,-yy+.5f},{xx+.5f,0,-yy+.5f},{xx+.5f,0,-yy-.5f},{xx-.5f,0,-yy-.5f}};
            for(int[] e:edges)if(kind[at(xx+e[0],yy+e[1])]==0)sink.face(xx,yy,quad(top[e[2]],top[e[3]],b[e[3]],b[e[2]]),.82f);
        }
    }
    private static float[] quad(float[] a,float[] b,float[] c,float[] d){return new float[]{a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2],d[0],d[1],d[2]};}
}
