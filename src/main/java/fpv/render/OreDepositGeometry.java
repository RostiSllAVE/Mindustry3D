package fpv.render;

import java.util.*;

/** Cached alpha-island outlines, clipped inside the existing floor's planar triangles.
 * No assets, GL, random state or gameplay dependencies. */
public final class OreDepositGeometry {
    private OreDepositGeometry(){}
    public static final int MAX_PATCHES=8,MAX_PATCH_VERTICES=4,MAX_RAISED_PATCHES=2;
    public static final int MAX_ADDED_VERTICES=MAX_RAISED_PATCHES*MAX_PATCH_VERTICES*9;
    public record Patch(float[] uv,float sideU,float sideV,float area){public int points(){return uv.length/2;}}
    public record Template(List<Patch> patches){public Template{patches=List.copyOf(patches);}}
    private record Point(float x,float y){}
    private static final Point C=new Point(.5f,.5f);
    private static final Point[][] PLANES={
        {new Point(0,1),new Point(1,1),C},{new Point(1,1),new Point(1,0),C},
        {new Point(1,0),new Point(0,0),C},{new Point(0,0),new Point(0,1),C}};

    public static Template generate(byte[] alpha,int width,int height){
        if(alpha==null||width<1||height<1||width>256||height>256||(long)width*height!=alpha.length)
            throw new IllegalArgumentException("Bounded ore alpha image required");
        boolean[] used=new boolean[alpha.length];int[] queue=new int[alpha.length];
        ArrayList<Patch> patches=new ArrayList<>();
        for(int start=0;start<alpha.length;start++){
            if(used[start]||(alpha[start]&255)<160)continue;
            int head=0,tail=1;queue[0]=start;used[start]=true;float cx=0,cy=0;
            ArrayList<Point> points=new ArrayList<>();
            while(head<tail){
                int p=queue[head++],x=p%width,y=p/width;cx+=x+.5f;cy+=y+.5f;
                points.add(new Point(x/(float)width,y/(float)height));points.add(new Point((x+1f)/width,y/(float)height));
                points.add(new Point((x+1f)/width,(y+1f)/height));points.add(new Point(x/(float)width,(y+1f)/height));
                for(int direction=0;direction<4;direction++){
                    int nx=x+(direction==0?-1:direction==1?1:0),ny=y+(direction==2?-1:direction==3?1:0);
                    if(nx<0||ny<0||nx>=width||ny>=height)continue;int n=ny*width+nx;
                    if(!used[n]&&(alpha[n]&255)>=160){used[n]=true;queue[tail++]=n;}
                }
            }
            if(tail<2)continue;
            cx/=tail;cy/=tail;int sample=queue[0];float best=Float.MAX_VALUE;
            for(int i=0;i<tail;i++){int p=queue[i];float dx=p%width+.5f-cx,dy=p/width+.5f-cy,d=dx*dx+dy*dy;if(d<best){best=d;sample=p;}}
            List<Point> hull=hull(points);
            for(Point[] plane:PLANES){
                List<Point> polygon=hull;
                for(int i=0;i<3;i++)polygon=clip(polygon,plane[i],plane[(i+1)%3]);
                polygon=new ArrayList<>(polygon);
                while(polygon.size()>MAX_PATCH_VERTICES){
                    int remove=0;float least=Float.MAX_VALUE;
                    for(int i=0;i<polygon.size();i++){float a=Math.abs(cross(polygon.get((i+polygon.size()-1)%polygon.size()),polygon.get(i),polygon.get((i+1)%polygon.size())));if(a<least){least=a;remove=i;}}
                    polygon.remove(remove);
                }
                float area=area(polygon);if(polygon.size()<3||area<.001f)continue;
                float[] uv=new float[polygon.size()*2];for(int i=0;i<polygon.size();i++){uv[2*i]=polygon.get(i).x;uv[2*i+1]=polygon.get(i).y;}
                patches.add(new Patch(uv,(sample%width+.5f)/width,(sample/width+.5f)/height,area));
            }
        }
        patches.sort(Comparator.comparingDouble((Patch p)->-p.area));
        return new Template(patches.subList(0,Math.min(MAX_PATCHES,patches.size())));
    }
    private static List<Point> hull(ArrayList<Point> input){
        input.sort(Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y));ArrayList<Point> h=new ArrayList<>();
        for(Point p:input){while(h.size()>1&&cross(h.get(h.size()-2),h.get(h.size()-1),p)<=0)h.remove(h.size()-1);h.add(p);}
        int lower=h.size();for(int i=input.size()-2;i>=0;i--){Point p=input.get(i);while(h.size()>lower&&cross(h.get(h.size()-2),h.get(h.size()-1),p)<=0)h.remove(h.size()-1);h.add(p);}
        if(h.size()>1)h.remove(h.size()-1);return h;
    }
    private static List<Point> clip(List<Point> input,Point a,Point b){
        ArrayList<Point> result=new ArrayList<>();if(input.isEmpty())return result;
        Point prev=input.get(input.size()-1);float dp=cross(a,b,prev);
        for(Point p:input){float d=cross(a,b,p);if((d<=0)!=(dp<=0)){float t=dp/(dp-d);result.add(new Point(prev.x+(p.x-prev.x)*t,prev.y+(p.y-prev.y)*t));}if(d<=0)result.add(p);prev=p;dp=d;}
        return result;
    }
    private static float cross(Point a,Point b,Point c){return (b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x);}
    private static float area(List<Point> p){float sum=0;for(int i=0;i<p.size();i++){Point a=p.get(i),b=p.get((i+1)%p.size());sum+=a.x*b.y-a.y*b.x;}return Math.abs(sum)*.5f;}
}
