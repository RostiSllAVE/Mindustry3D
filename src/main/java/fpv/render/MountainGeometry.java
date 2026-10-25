package fpv.render;

/** Bounded visual terraces over the unchanged canonical triangles. Reused chunk-build scratch only.
 * A continuous solid surface: top patches plus only exposed risers, never stacks of hidden cubes.
 */
public final class MountainGeometry {
    public static final float MAX_ERROR=.35f, STEP=.40f;
    private static final float EPS=.00001f;
    public interface Source {
        boolean natural(int x,int y);
        float corner(int x,int y,int corner);
        float maximum();
    }
    /** Three vertices, each XYZ + normalized atlas UV. Scratch is valid only during the callback. */
    public interface Sink {void triangle(float[] xyzuv,float shade,boolean top);}
    private final int chunk;
    private final float[] patches=new float[9*16],canonical=new float[4],triangle=new float[15];
    private final boolean[] solid=new boolean[9];
    private final float[] polygon=new float[24],clipped=new float[24];
    private int tileX,tileY;
    private Sink sink;
    public MountainGeometry(int chunk){if(chunk<4)throw new IllegalArgumentException("Chunk size");this.chunk=chunk;}
    public void emit(Source source,int x,int y,MountainStyle style,Sink target){
        if(style==MountainStyle.SMOOTH)throw new IllegalArgumentException("Use original smooth renderer");
        if(!source.natural(x,y))return;
        tileX=x;tileY=y;sink=target;
        float maximum=source.maximum();
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
            int slot=(dy+1)*3+dx+1;solid[slot]=source.natural(x+dx,y+dy);
            if(!solid[slot])continue;
            for(int c=0;c<4;c++)canonical[c]=source.corner(x+dx,y+dy,c);
            for(int sy=0;sy<2;sy++)for(int sx=0;sx<2;sx++)patch(x+dx,y+dy,sx,sy,slot*16+(sy*2+sx)*4,style,maximum);
        }
        boolean merge=true;
        for(int i=1;i<16;i++)if(Math.abs(patches[64+i]-patches[64])>EPS){merge=false;break;}
        if(merge)top(0,0,1,patches[64],patches[64],patches[64],patches[64],.90f);
        for(int sy=0;sy<2;sy++)for(int sx=0;sx<2;sx++){
            int p=4*16+(sy*2+sx)*4;float u=sx*.5f,v=sy*.5f;
            if(!merge)top(u,v,.5f,patches[p],patches[p+1],patches[p+2],patches[p+3],shade(p));
            edge(u,v,u+.5f,v,patches[p],patches[p+1],neighbor(sx,sy-1,3),neighbor(sx,sy-1,2),.65f);
            edge(u+.5f,v,u+.5f,v+.5f,patches[p+1],patches[p+2],neighbor(sx+1,sy,0),neighbor(sx+1,sy,3),.75f);
            edge(u+.5f,v+.5f,u,v+.5f,patches[p+2],patches[p+3],neighbor(sx,sy+1,1),neighbor(sx,sy+1,0),.8f);
            edge(u,v+.5f,u,v,patches[p+3],patches[p],neighbor(sx-1,sy,2),neighbor(sx-1,sy,1),.58f);
        }
        sink=null;
    }
    private void top(float u,float v,float size,float sw,float se,float ne,float nw,float shade){
        point(0,u,v,sw,u,1-v);point(1,u+size,v,se,u+size,1-v);point(2,u+size,v+size,ne,u+size,1-v-size);sink.triangle(triangle,shade,true);
        point(1,u+size,v+size,ne,u+size,1-v-size);point(2,u,v+size,nw,u,1-v-size);sink.triangle(triangle,shade,true);
    }
    private float neighbor(int sx,int sy,int c){
        int dx=Math.floorDiv(sx,2),dy=Math.floorDiv(sy,2),slot=(dy+1)*3+dx+1;
        return solid[slot]?patches[slot*16+(Math.floorMod(sy,2)*2+Math.floorMod(sx,2))*4+c]:0;
    }
    private void patch(int x,int y,int sx,int sy,int p,MountainStyle style,float maximum){
        float min=Float.POSITIVE_INFINITY,max=0;
        for(int c=0;c<4;c++){
            float u=(sx+(c==1||c==2?1:0))*.5f,v=(sy+(c>=2?1:0))*.5f;
            float h=interpolate(canonical,u,v);patches[p+c]=h;min=Math.min(min,h);max=Math.max(max,h);
        }
        boolean rocky=style==MountainStyle.BLOCKY||rocky(x+sx*.5f,y+sy*.5f);
        // A horizontal patch must fit the tolerance at EVERY point, not merely its center.
        if(!rocky||max-min>MAX_ERROR*2)return;
        float level=(float)(Math.rint(((min+max)*.5)/STEP)*STEP);
        level=Math.max(Math.max(0,max-MAX_ERROR),Math.min(Math.min(maximum,min+MAX_ERROR),level));
        for(int c=0;c<4;c++){
            int gx=x*2+sx+(c==1||c==2?1:0),gy=y*2+sy+(c>=2?1:0);
            // Exact canonical apron joins detailed/distant/fog chunks without skirts or cracks.
            if(Math.floorMod(gx,chunk*2)!=0&&Math.floorMod(gy,chunk*2)!=0)patches[p+c]=level;
        }
    }
    private static boolean rocky(float x,float y){
        return .5+.28*Math.sin(x*.17+Math.sin(y*.11)*1.4)+.20*Math.sin(y*.19+x*.047)>.52;
    }
    public static float interpolate(float[] c,float u,float v){
        return v<=u?c[0]*(1-u)+c[1]*(u-v)+c[2]*v:c[0]*(1-v)+c[2]*u+c[3]*(v-u);
    }
    private float shade(int p){return Math.max(.65f,Math.min(1.10f,.90f+(patches[p]+patches[p+3]-patches[p+1]-patches[p+2])*.12f+(patches[p]+patches[p+1]-patches[p+2]-patches[p+3])*.08f));}
    private void point(int i,float u,float v,float h,float tu,float tv){int at=i*5;triangle[at]=tileX-.5f+u;triangle[at+1]=h;triangle[at+2]=-tileY+.5f-v;triangle[at+3]=tu;triangle[at+4]=tv;}
    private void edge(float ax,float ay,float bx,float by,float a,float b,float lowA,float lowB,float shade){
        float da=a-lowA,db=b-lowB;
        if(da<=EPS&&db<=EPS)return; // Hidden adjoining volume, including equal-height cube faces.
        float start=0,end=1;
        if(da<0)start=da/(da-db);if(db<0)end=da/(da-db);
        float bottomA=lerp(lowA,lowB,start),bottomB=lerp(lowA,lowB,end),topA=lerp(a,b,start),topB=lerp(a,b,end);
        int first=(int)Math.floor(Math.min(bottomA,bottomB)),last=(int)Math.ceil(Math.max(topA,topB));
        // Tile vertically in world space; a thirty-tile cliff never stretches one atlas sprite.
        for(int band=first;band<last;band++){
            polygon[0]=start;polygon[1]=bottomA;polygon[2]=end;polygon[3]=bottomB;polygon[4]=end;polygon[5]=topB;polygon[6]=start;polygon[7]=topA;
            int n=clip(polygon,4,clipped,band,true);n=clip(clipped,n,polygon,band+1,false);
            for(int i=1;i<n-1;i++){
                sidePoint(0,polygon[0],polygon[1],ax,ay,bx,by,band);
                sidePoint(1,polygon[i*2],polygon[i*2+1],ax,ay,bx,by,band);
                sidePoint(2,polygon[(i+1)*2],polygon[(i+1)*2+1],ax,ay,bx,by,band);
                float area=(triangle[5]-triangle[0])*(triangle[11]-triangle[1])-(triangle[6]-triangle[1])*(triangle[10]-triangle[0]);
                if(Math.abs(bx-ax)<EPS)area=(triangle[7]-triangle[2])*(triangle[11]-triangle[1])-(triangle[6]-triangle[1])*(triangle[12]-triangle[2]);
                if(Math.abs(area)>1e-8f)sink.triangle(triangle,shade,false);
            }
        }
    }
    private void sidePoint(int i,float t,float h,float ax,float ay,float bx,float by,int band){
        float u=lerp(ax,bx,t),v=lerp(ay,by,t);point(i,u,v,h,Math.abs(bx-ax)>EPS?u:v,1-(h-band));
    }
    private static int clip(float[] in,int n,float[] out,float boundary,boolean above){
        int count=0;if(n==0)return 0;
        float px=in[(n-1)*2],py=in[(n-1)*2+1];boolean previous=above?py>=boundary:py<=boundary;
        for(int i=0;i<n;i++){
            float x=in[i*2],y=in[i*2+1];boolean inside=above?y>=boundary:y<=boundary;
            if(inside!=previous){float t=(boundary-py)/(y-py);out[count*2]=lerp(px,x,t);out[count++*2+1]=boundary;}
            if(inside){out[count*2]=x;out[count++*2+1]=y;}
            px=x;py=y;previous=inside;
        }
        return count;
    }
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
