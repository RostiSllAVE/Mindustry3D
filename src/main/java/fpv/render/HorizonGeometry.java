package fpv.render;

import java.util.*;
import java.util.function.DoubleUnaryOperator;
import static fpv.render.BorderBiomeProfile.Biome.*;

/** Cached visual-only annulus; its inner collar never contains a playable tile. */
public final class HorizonGeometry {
    public static final int STRIDE=9;
    public static final float COLLAR=4;
    public final float[] vertices;
    public final double[] positions,distances;
    private final float[][][] rows;
    public final BorderBiomeProfile profile;
    public final int treeCount;
    public record Point(float x,float z){}
    private HorizonGeometry(BorderBiomeProfile profile,double[] positions,double[] distances,float[][][] rows,float[] vertices,int trees){this.profile=profile;this.positions=positions;this.distances=distances;this.rows=rows;this.vertices=vertices;treeCount=trees;}
    public static double outerDistance(int w,int h){return Math.max(2048,Math.hypot(w,h)*1.15+96);}
    public static double boundRadius(int w,int h){double d=outerDistance(w,h);return Math.hypot(w*.5+d,h*.5+d)+200;}
    /** A single rectangular annulus gives adjacent directions the exact same corner coordinates. */
    public static Point point(BorderBiomeProfile p,double along,double distance){
        double s=along-Math.floor(along/p.perimeter)*p.perimeter,u,x,y;
        if(s<p.width){u=s/p.width;x=-.5+u*p.width+(2*u-1)*distance;y=-.5-distance;}
        else if((s-=p.width)<p.height){u=s/p.height;x=p.width-.5+distance;y=-.5+u*p.height+(2*u-1)*distance;}
        else if((s-=p.height)<p.width){u=s/p.width;x=p.width-.5-u*p.width+(1-2*u)*distance;y=p.height-.5+distance;}
        else{u=(s-p.width)/p.height;x=-.5-distance;y=p.height-.5-u*p.height+(1-2*u)*distance;}
        return new Point((float)x,(float)-y);
    }
    public static HorizonGeometry build(BorderBiomeProfile p,DoubleUnaryOperator seam){
        double diag=Math.hypot(p.width,p.height),outer=outerDistance(p.width,p.height);
        double[] distances={COLLAR,12,24+diag*.08,48+diag*.24,96+diag*.55,outer};
        double[] positions=new double[p.sections().size()+1];for(int i=0;i<p.sections().size();i++)positions[i]=p.sections().get(i).start();positions[positions.length-1]=p.perimeter;
        float[][][] rows=new float[distances.length][positions.length][];var sample=new BorderBiomeProfile.Sample();
        for(int r=0;r<rows.length;r++)for(int i=0;i<positions.length;i++){
            double s=positions[i],d=distances[r];p.sample(s+drift(p,s,r,d),sample);Point pt=point(p,s,d);
            float h=height(p,sample,pt.x,pt.z,r);
            if(r==0){double water=sample.weight(OCEAN);h=(float)(seam.applyAsDouble(s)*.45*(1-water)+h*(.55+.45*water));}
            float shade=(float)(.91+.06*noise(pt.x*.031,pt.z*.031));
            float red=sample.r*shade,green=sample.g*shade,blue=sample.b*shade;
            if(sample.weight(UNKNOWN)>.5){red=.36f;green=.36f;blue=.42f;}
            // Ring1 joins tile-resolution and coarse edges. Its wave weight must be zero,
            // otherwise non-linear displacement opens T-junctions between their vertices.
            rows[r][i]=new float[]{pt.x,h,pt.z,red,green,blue,sample.weight(SPACE),sample.weight(OCEAN),r==1?0:1};
        }
        Builder b=new Builder();var geometry=new HorizonGeometry(p,positions,distances,rows,null,0);
        // Tile-resolution first band closes the collar without T-junctions; far rings are bounded.
        float[] a=new float[9],c=new float[9],d=new float[9],e=new float[9];
        for(int s=0;s<(int)p.perimeter;s++){geometry.row(s,0,a);geometry.row(s+1,0,c);geometry.row(s+1,1,d);geometry.row(s,1,e);b.quad(a,c,d,e);}
        for(int r=1;r<rows.length-1;r++)for(int i=0;i<positions.length-1;i++)b.quad(rows[r][i],rows[r][i+1],rows[r+1][i+1],rows[r+1][i]);
        int trees=0;
        for(int r=1;r<=2;r++)for(int i=0;i<positions.length-1;i+=3){
            double s=(positions[i]+positions[i+1])*.5;p.sample(s+drift(p,s,r,distances[r]),sample);
            if(sample.weight(FOREST)<.18f||sample.weight(SPACE)>.3f)continue;
            geometry.row(s,r,a);float size=(r==1?6:3.5f)*(float)(.8+.4*hash(i,r));
            b.tree(a[0],a[1]-.03f,a[2],size,sample.form,sample.tr,sample.tg,sample.tb);trees++;
        }
        return new HorizonGeometry(p,positions,distances,rows,Arrays.copyOf(b.data,b.count),trees);
    }
    public void row(double s,int ring,float[] out){
        s=Math.max(0,Math.min(profile.perimeter,s));int at=Arrays.binarySearch(positions,s);
        if(at>=0){System.arraycopy(rows[ring][at],0,out,0,9);return;}
        int right=-at-1,left=right-1;float t=(float)((s-positions[left])/(positions[right]-positions[left]));
        for(int i=0;i<9;i++)out[i]=rows[ring][left][i]+(rows[ring][right][i]-rows[ring][left][i])*t;
    }
    private static double drift(BorderBiomeProfile p,double s,int ring,double distance){
        if(ring<2)return 0;double angle=(s%p.perimeter)/p.perimeter*Math.PI*2;
        return Math.min(p.perimeter*.08,distance*.30)*StrictMath.sin(angle*3+ring*.8);
    }
    private static float height(BorderBiomeProfile p,BorderBiomeProfile.Sample s,float x,float z,int ring){
        double diag=Math.hypot(p.width,p.height),n=noise(x*.035,z*.035),fine=noise(x*.09+31,z*.09-17);
        double amplitude=Math.min(120,Math.max(8,diag*.14));
        double layer=switch(ring){case 0->.015;case 1->.08;case 2->1;case 3->.55;case 4->.25;default->0;};
        double mountain=s.weight(MOUNTAINS)*amplitude*layer*(.35+.65*Math.abs(n));
        double rocky=(s.weight(WASTELAND)*2+s.weight(VOLCANIC)*4)*Math.abs(n)*Math.min(1,ring*.4);
        double dunes=s.weight(DESERT)*Math.min(7,diag*.012)*(n*.65+fine*.25);
        double ice=s.weight(ICE)*(ring<2?.2:Math.abs(fine)*Math.min(20,diag*.04));
        double forest=s.weight(FOREST)*(n+1)*1.3;
        return (float)(mountain+rocky+dunes+ice+forest-.16*s.weight(OCEAN));
    }
    public long fingerprint(){long h=17;for(float f:vertices)h=h*31+Float.floatToIntBits(f);return h;}
    public static double hash(int x,int y){int h=x*0x1f123bb5+y*0x5f356495;h^=h>>>16;h*=0x7feb352d;h^=h>>>15;return (h&0x7fffffff)/2147483647d;}
    private static double noise(double x,double y){int a=(int)Math.floor(x),b=(int)Math.floor(y);double u=x-a,v=y-b;u=u*u*(3-2*u);v=v*v*(3-2*v);return ((1-v)*((1-u)*hash(a,b)+u*hash(a+1,b))+v*((1-u)*hash(a,b+1)+u*hash(a+1,b+1)))*2-1;}
    private static final class Builder {
        float[] data=new float[65536];int count;
        void vertex(float[] v){if(count+9>data.length)data=Arrays.copyOf(data,data.length*2);System.arraycopy(v,0,data,count,9);count+=9;}
        void quad(float[] a,float[] b,float[] c,float[] d){
            if(a[6]>.995f&&b[6]>.995f&&c[6]>.995f&&d[6]>.995f)return;
            vertex(a);vertex(b);vertex(c);vertex(a);vertex(c);vertex(d);
        }
        void triangle(float[] a,float[] b,float[] c){vertex(a);vertex(b);vertex(c);}
        float[] v(float x,float y,float z,float r,float g,float b,float shade){return new float[]{x,y,z,r*shade,g*shade,b*shade,0,0,0};}
        void tree(float x,float y,float z,float h,int form,float r,float g,float b){
            if(form==VegetationInstances.Form.SHRUB.ordinal()||form==VegetationInstances.Form.SPORE.ordinal()||form==VegetationInstances.Form.FROND.ordinal()||form==VegetationInstances.Form.CORAL.ordinal())h*=.45f;
            boolean pine=form==VegetationInstances.Form.PINE.ordinal(),dead=form==VegetationInstances.Form.DEAD.ordinal(),diamond=form==VegetationInstances.Form.DIAMOND.ordinal();
            float crown=(pine?.24f:.40f)*h;
            float[] root=v(x,y,z,r,g,b,.48f),top=v(x,y+h,z,r,g,b,.8f);
            for(int i=0;i<4;i++){
                double a=i*Math.PI/2,c=(i+1)*Math.PI/2;
                if(dead){float[] branch=v(x+(float)StrictMath.cos(a)*h*.3f,y+h*.7f,z+(float)StrictMath.sin(a)*h*.3f,r,g,b,1);triangle(root,branch,top);continue;}
                float yy=y+h*(diamond?.45f:.4f);
                float[] left=v(x+(float)StrictMath.cos(a)*crown,yy,z+(float)StrictMath.sin(a)*crown,r,g,b,.8f+i*.045f),right=v(x+(float)StrictMath.cos(c)*crown,yy,z+(float)StrictMath.sin(c)*crown,r,g,b,.8f+i*.045f);
                triangle(left,right,top);triangle(left,root,right);
            }
        }
    }
}
