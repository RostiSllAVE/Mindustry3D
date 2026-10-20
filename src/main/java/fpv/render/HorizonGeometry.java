package fpv.render;

import java.util.*;
import java.util.function.DoubleUnaryOperator;
import static fpv.render.BorderBiomeProfile.Biome.*;

/** Cached visual-only annulus; its inner collar never contains a playable tile. */
public final class HorizonGeometry {
    public static final int STRIDE=14;
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
        var bands=new ArrayList<Double>();bands.add((double)COLLAR);bands.add(12d);
        for(double d=24;d<outer;d*=1.75)bands.add(d);bands.add(outer);
        double[] distances=bands.stream().mapToDouble(Double::doubleValue).toArray();
        var knots=new TreeSet<Double>();for(var section:p.sections())knots.add(section.start());
        double offset=0;for(int length:new int[]{p.width,p.height,p.width,p.height}){
            int n=Math.min(length,16);for(int i=0;i<n;i++)knots.add(offset+length*(double)i/n);offset+=length;
        }knots.add(p.perimeter);double[] positions=knots.stream().mapToDouble(Double::doubleValue).toArray();
        float[][][] rows=new float[distances.length][positions.length][];var sample=new BorderBiomeProfile.Sample();
        for(int r=0;r<rows.length;r++)for(int i=0;i<positions.length;i++){
            double s=positions[i],d=distances[r];p.sample(s+drift(p,s,r,d),sample);Point pt=point(p,s,d);
            float h=height(p,sample,pt.x,pt.z,d);
            if(r==0){double water=sample.weight(OCEAN);h=(float)(seam.applyAsDouble(s)*.65*(1-water)+h*(.35+.65*water));}
            float red=sample.r,green=sample.g,blue=sample.b;
            if(sample.weight(UNKNOWN)>.5){red=.36f;green=.36f;blue=.42f;}
            // Ring1 joins tile-resolution and coarse edges. Its wave weight must be zero,
            // otherwise non-linear displacement opens T-junctions between their vertices.
            rows[r][i]=new float[]{pt.x,h,pt.z,red,green,blue,sample.weight(SPACE),sample.weight(OCEAN),r==1?0:1,sample.weight(DESERT),sample.weight(ICE),0,1,0};
        }
        // Smooth finite-difference normals from the actual cached geometry, not noisy per-frame lighting.
        for(int r=0;r<rows.length;r++)for(int i=0;i<positions.length-1;i++){
            int left=i==0?positions.length-2:i-1,right=(i+1)%(positions.length-1);
            float[] l=rows[r][left],v=rows[r][right],near=rows[Math.max(0,r-1)][i],far=rows[Math.min(rows.length-1,r+1)][i];
            float ax=v[0]-l[0],ay=v[1]-l[1],az=v[2]-l[2],bx=far[0]-near[0],by=far[1]-near[1],bz=far[2]-near[2];
            float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(ny<0){nx=-nx;ny=-ny;nz=-nz;}
            if(len>1e-8f){rows[r][i][11]=nx/len;rows[r][i][12]=ny/len;rows[r][i][13]=nz/len;}
        }
        for(int r=0;r<rows.length;r++)System.arraycopy(rows[r][0],11,rows[r][positions.length-1],11,3);
        Builder b=new Builder();var geometry=new HorizonGeometry(p,positions,distances,rows,null,0);
        // Tile-resolution first band closes the collar without T-junctions; far rings are bounded.
        float[] a=new float[STRIDE],c=new float[STRIDE],d=new float[STRIDE],e=new float[STRIDE];
        for(int s=0;s<(int)p.perimeter;s++){geometry.row(s,0,a);geometry.row(s+1,0,c);geometry.row(s+1,1,d);geometry.row(s,1,e);b.quad(a,c,d,e);}
        for(int r=1;r<rows.length-1;r++)for(int i=0;i<positions.length-1;i++)b.quad(rows[r][i],rows[r][i+1],rows[r+1][i+1],rows[r+1][i]);
        int trees=0;
        int candidates=Math.min(192,Math.max(12,(int)p.perimeter/5));
        for(int i=0;i<candidates;i++){
            double s=(i+.15+.7*hash(i,81))*p.perimeter/candidates;
            double distance=12+100*Math.pow(hash(i,117),1.7);p.sample(s,sample);
            Point pt=point(p,s,distance);double density=.35+.65*(noise(pt.x*.07,pt.z*.07)+1)*.5;
            if(sample.weight(FOREST)<.18f||sample.weight(SPACE)>.3f||hash(i,73)>sample.weight(FOREST)*density)continue;
            int r=0;while(r<distances.length-2&&distances[r+1]<distance)r++;
            geometry.row(s,r,a);geometry.row(s,r+1,c);float t=(float)((distance-distances[r])/(distances[r+1]-distances[r]));
            float size=(float)((8+10*hash(i,13))*(1-.002*distance));
            b.tree(pt.x,geometry.surfaceHeight(s,distance,pt.x,pt.z)-.01f,pt.z,size,sample.form,sample.tr,sample.tg,sample.tb,(float)(hash(i,7)*Math.PI*2),(float)(.7+.6*hash(i,43)));trees++;
            if(hash(i,271)>.30){
                // A smaller neighbour makes irregular canopy clusters rather than isolated identical tokens.
                double ss=s+(hash(i,191)-.5)*8,dd=distance+3+4*hash(i,103);
                ss=ss-Math.floor(ss/p.perimeter)*p.perimeter;Point companion=point(p,ss,dd);
                int rr=0;while(rr<distances.length-2&&distances[rr+1]<dd)rr++;
                geometry.row(ss,rr,a);geometry.row(ss,rr+1,c);float tt=(float)((dd-distances[rr])/(distances[rr+1]-distances[rr]));
                b.tree(companion.x,geometry.surfaceHeight(ss,dd,companion.x,companion.z)-.01f,companion.z,size*(float)(.55+.3*hash(i,59)),sample.form,sample.tr*.94f,sample.tg*.98f,sample.tb*.94f,(float)(hash(i,197)*Math.PI*2),(float)(.8+.4*hash(i,53)));trees++;
            }
        }
        return new HorizonGeometry(p,positions,distances,rows,Arrays.copyOf(b.data,b.count),trees);
    }
    public void row(double s,int ring,float[] out){
        s=Math.max(0,Math.min(profile.perimeter,s));int at=Arrays.binarySearch(positions,s);
        if(at>=0){System.arraycopy(rows[ring][at],0,out,0,STRIDE);return;}
        int right=-at-1,left=right-1;float t=(float)((s-positions[left])/(positions[right]-positions[left]));
        for(int i=0;i<STRIDE;i++)out[i]=rows[ring][left][i]+(rows[ring][right][i]-rows[ring][left][i])*t;
        float length=(float)Math.sqrt(out[11]*out[11]+out[12]*out[12]+out[13]*out[13]);
        if(length>1e-8f)for(int i=11;i<14;i++)out[i]/=length;
    }
    /** Exact support on the two submitted triangles, not bilinear interpolation above a warped quad. */
    public float surfaceHeight(double along,double distance,float x,float z){
        int at=Arrays.binarySearch(positions,along);int i=Math.min(positions.length-2,at>=0?at:-at-2);
        int r=0;while(r<distances.length-2&&distances[r+1]<distance)r++;
        float[] a=rows[r][i],b=rows[r][i+1],c=rows[r+1][i+1],d=rows[r+1][i];
        float height=triangleHeight(a,b,c,x,z,true);return Float.isNaN(height)?triangleHeight(a,c,d,x,z,false):height;
    }
    private static float triangleHeight(float[] a,float[] b,float[] c,float x,float z,boolean inside){
        float det=(b[2]-c[2])*(a[0]-c[0])+(c[0]-b[0])*(a[2]-c[2]);
        if(Math.abs(det)<1e-10f)return a[1];
        float u=((b[2]-c[2])*(x-c[0])+(c[0]-b[0])*(z-c[2]))/det;
        float v=((c[2]-a[2])*(x-c[0])+(a[0]-c[0])*(z-c[2]))/det,w=1-u-v;
        return inside&&(u<-.0001f||v<-.0001f||w<-.0001f)?Float.NaN:u*a[1]+v*b[1]+w*c[1];
    }
    private static double drift(BorderBiomeProfile p,double s,int ring,double distance){
        if(ring<2)return 0;double angle=(s%p.perimeter)/p.perimeter*Math.PI*2;
        // One continuous warp in distance, not unrelated angular offsets on each ring.
        return Math.min(p.perimeter*.10,distance*.22)*StrictMath.sin(angle*3+noise(distance*.003,17));
    }
    private static float height(BorderBiomeProfile p,BorderBiomeProfile.Sample s,float x,float z,double distance){
        double diag=Math.hypot(p.width,p.height),n=noise(x*.018,z*.018),fine=noise(x*.065+31,z*.065-17),grain=noise(x*.16-11,z*.16+37);
        double amplitude=Math.min(120,Math.max(14,diag*.28));
        double layer=smooth(4,38,distance)*(1-smooth(240,outerDistance(p.width,p.height),distance));
        double ridge=Math.max(0,Math.min(1,(n+.5)*1.3));
        double mountain=s.weight(MOUNTAINS)*amplitude*layer*(.10+.64*ridge*ridge+.20*(fine+.5)+.06*grain);
        double rocky=(s.weight(WASTELAND)*2+s.weight(VOLCANIC)*4)*(.5+.35*n+.15*fine)*smooth(4,24,distance);
        double dunes=s.weight(DESERT)*Math.min(7,diag*.012)*(n*.75+fine*.18);
        double ice=s.weight(ICE)*(.2+layer*(.5+.35*n+.15*fine)*Math.min(20,diag*.04));
        double forest=s.weight(FOREST)*(n+1)*1.3;
        return (float)(mountain+rocky+dunes+ice+forest-.16*s.weight(OCEAN));
    }
    private static double smooth(double a,double b,double x){double t=Math.max(0,Math.min(1,(x-a)/(b-a)));return t*t*(3-2*t);}
    public long fingerprint(){long h=17;for(float f:vertices)h=h*31+Float.floatToIntBits(f);return h;}
    public static double hash(int x,int y){int h=x*0x1f123bb5+y*0x5f356495;h^=h>>>16;h*=0x7feb352d;h^=h>>>15;return (h&0x7fffffff)/2147483647d;}
    private static double noise(double x,double y){int a=(int)Math.floor(x),b=(int)Math.floor(y);double u=x-a,v=y-b;u=u*u*(3-2*u);v=v*v*(3-2*v);return ((1-v)*((1-u)*hash(a,b)+u*hash(a+1,b))+v*((1-u)*hash(a,b+1)+u*hash(a+1,b+1)))*2-1;}
    private static final class Builder {
        float[] data=new float[65536];int count;
        void vertex(float[] v){if(count+STRIDE>data.length)data=Arrays.copyOf(data,data.length*2);System.arraycopy(v,0,data,count,STRIDE);count+=STRIDE;}
        void quad(float[] a,float[] b,float[] c,float[] d){
            if(a[6]>.995f&&b[6]>.995f&&c[6]>.995f&&d[6]>.995f)return;
            vertex(a);vertex(b);vertex(c);vertex(a);vertex(c);vertex(d);
        }
        void triangle(float[] a,float[] b,float[] c){vertex(a);vertex(b);vertex(c);}
        float[] v(float x,float y,float z,float r,float g,float b,float shade){return new float[]{x,y,z,r*shade,g*shade,b*shade,0,0,0,0,0,0,0,0};}
        void tree(float x,float y,float z,float h,int form,float r,float g,float b,float yaw,float width){
            if(form==VegetationInstances.Form.SHRUB.ordinal()||form==VegetationInstances.Form.SPORE.ordinal()||form==VegetationInstances.Form.FROND.ordinal()||form==VegetationInstances.Form.CORAL.ordinal())h*=.45f;
            boolean pine=form==VegetationInstances.Form.PINE.ordinal(),dead=form==VegetationInstances.Form.DEAD.ordinal(),diamond=form==VegetationInstances.Form.DIAMOND.ordinal();
            float crown=(pine?.24f:.40f)*h*width;
            float[] root=v(x,y,z,r,g,b,.48f),top=v(x,y+h,z,r,g,b,.8f);
            if(!dead){
                // Crossed trunk strips and open low-poly crowns read as plants, not floating diamonds.
                float stem=h*.035f;for(int i=0;i<2;i++){
                    double a=yaw+i*Math.PI/2;float dx=(float)StrictMath.cos(a)*stem,dz=(float)StrictMath.sin(a)*stem;
                    quad(v(x-dx,y,z-dz,r,g,b,.38f),v(x+dx,y,z+dz,r,g,b,.38f),v(x+dx,y+h*.65f,z+dz,r,g,b,.45f),v(x-dx,y+h*.65f,z-dz,r,g,b,.45f));
                }
                if(pine){cone(x,y+h*.20f,z,crown,y+h*.85f,yaw,r,g,b);cone(x,y+h*.52f,z,crown*.65f,y+h,yaw+.27f,r,g,b);return;}
            }
            for(int i=0;i<4;i++){
                double a=yaw+i*Math.PI/2,c=yaw+(i+1)*Math.PI/2;
                if(dead){float[] branch=v(x+(float)StrictMath.cos(a)*h*.3f,y+h*.7f,z+(float)StrictMath.sin(a)*h*.3f,r,g,b,1);triangle(root,branch,top);continue;}
                float yy=y+h*(diamond?.45f:.4f);
                float[] left=v(x+(float)StrictMath.cos(a)*crown,yy,z+(float)StrictMath.sin(a)*crown,r,g,b,.8f+i*.045f),right=v(x+(float)StrictMath.cos(c)*crown,yy,z+(float)StrictMath.sin(c)*crown,r,g,b,.8f+i*.045f);
                triangle(left,right,top);triangle(left,v(x,y+h*.25f,z,r,g,b,.65f),right);
            }
        }
        void cone(float x,float y,float z,float radius,float peak,float yaw,float r,float g,float b){
            float[] top=v(x,peak,z,r,g,b,.97f);
            for(int i=0;i<4;i++){double a=yaw+i*Math.PI/2,c=yaw+(i+1)*Math.PI/2;
                triangle(v(x+(float)StrictMath.cos(a)*radius,y,z+(float)StrictMath.sin(a)*radius,r,g,b,.78f+i*.04f),v(x+(float)StrictMath.cos(c)*radius,y,z+(float)StrictMath.sin(c)*radius,r,g,b,.78f+i*.04f),top);}
        }
    }
}
