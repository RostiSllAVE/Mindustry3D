package fpv.render;

import java.util.*;
import java.util.function.DoubleUnaryOperator;
import static fpv.render.BorderBiomeProfile.Biome.*;

/** Cached visual-only annulus; its inner collar never contains a playable tile. */
public final class HorizonGeometry {
    public static final int STRIDE=15;
    private static final int NODE=18;
    public static final float COLLAR=4;
    public final float[] vertices;
    public final double[] positions,distances,seamPositions;
    private final float[][][] rows;
    public final BorderBiomeProfile profile;
    public final int treeCount;
    public final HorizonLandscape landscape;
    public final int islandCount,outcropCount,structureCount;
    public record Point(float x,float z){}
    private HorizonGeometry(BorderBiomeProfile profile,double[] positions,double[] distances,double[] seamPositions,float[][][] rows,float[] vertices,int trees,HorizonLandscape landscape,int islands,int rocks,int structures){this.profile=profile;this.positions=positions;this.distances=distances;this.seamPositions=seamPositions;this.rows=rows;this.vertices=vertices;this.treeCount=trees;this.landscape=landscape;islandCount=islands;outcropCount=rocks;structureCount=structures;}
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
    public static HorizonGeometry build(BorderBiomeProfile p,DoubleUnaryOperator seam){return build(p,seam,HorizonComposition.EMPTY);}
    public static HorizonGeometry build(BorderBiomeProfile p,DoubleUnaryOperator seam,HorizonComposition composition){
        double outer=outerDistance(p.width,p.height);var landscape=new HorizonLandscape(p,composition);
        var bands=new ArrayList<Double>();bands.add((double)COLLAR);bands.add(12d);
        for(double d=24;d<outer;d*=1.75)bands.add(d);bands.add(outer);
        double[] distances=bands.stream().mapToDouble(Double::doubleValue).toArray();
        var knots=new TreeSet<Double>();
        for(int si=0;si<p.sections().size();si++){var section=p.sections().get(si);
            knots.add(section.start());
            if(p.readableBiome(si)!=p.readableBiome((si+p.sections().size()-1)%p.sections().size())){
                double width=Math.min(section.end()-section.start(),8)*.05;
                if(section.start()+width<p.perimeter)knots.add(section.start()+width);
                if(section.start()-width>=0)knots.add(section.start()-width);
            }
        }
        double offset=0;for(int length:new int[]{p.width,p.height,p.width,p.height}){
            int n=Math.min(length,16);for(int i=0;i<n;i++)knots.add(offset+length*(double)i/n);offset+=length;
        }knots.add(p.perimeter);double[] positions=knots.stream().mapToDouble(Double::doubleValue).toArray();
        // Both collar joins share this exact polyline, including fractional material knots.
        // A linear merge keeps construction proportional to perimeter without boxed tile entries.
        double[] seamPositions=seamPositions(positions,(int)p.perimeter);
        float[][][] rows=new float[distances.length][positions.length][];var sample=new BorderBiomeProfile.Sample();var surface=new HorizonLandscape.Surface();
        for(int r=0;r<rows.length;r++)for(int i=0;i<positions.length;i++){
            double along=positions[i],distance=distances[r];Point pt=point(p,along,distance);landscape.sample(along,distance,pt.x,pt.z,surface);
            float h=surface.height;
            if(r==0){h=(float)(seam.applyAsDouble(along)*.65+h*.35);if(surface.coast>=0)h=Math.min(h,HorizonLandscape.SEA-surface.depth*.35f);}
            rows[r][i]=new float[]{pt.x,h,pt.z,surface.r,surface.g,surface.b,surface.space,0,surface.depth,surface.sand,surface.ice,0,1,0,surface.coast,surface.waterR,surface.waterG,surface.waterB};
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
        Builder b=new Builder(),water=new Builder();var geometry=new HorizonGeometry(p,positions,distances,seamPositions,rows,null,0,landscape,0,0,0);
        // Tile-resolution first band closes the collar without T-junctions; far rings are bounded.
        float[] a=new float[NODE],c=new float[NODE],d=new float[NODE],e=new float[NODE];
        for(int i=0;i<seamPositions.length-1;i++){double s=seamPositions[i],end=seamPositions[i+1];geometry.row(s,0,a);geometry.row(end,0,c);geometry.row(end,1,d);geometry.row(s,1,e);b.coastalQuad(a,c,d,e,water);}
        for(int r=1;r<rows.length-1;r++)for(int i=0;i<positions.length-1;i++)b.coastalQuad(rows[r][i],rows[r][i+1],rows[r+1][i+1],rows[r+1][i],water);
        int trees=0;
        int candidates=Math.min(192,Math.max(12,(int)p.perimeter/5));
        for(int i=0;i<candidates;i++){
            double seed=(composition.seed()&65535)*.0001;double s=(i+.15+.7*hash(i+(int)(composition.seed()&8191),81))*p.perimeter/candidates;
            double distance=12+200*Math.pow(hash(i+(int)(composition.seed()&8191),117),1.7);landscape.sample(s,distance,point(p,s,distance).x,point(p,s,distance).z,surface);p.sampleReadable(landscape.warpedAlong(s,distance),sample);
            Point pt=point(p,s,distance);double density=.35+.65*(noise(pt.x*.07,pt.z*.07)+1)*.5;
            if(surface.coast>-.7f||surface.space>.1f||surface.forest<.25f||hash(i,73)>surface.forest*density)continue;
            int r=0;while(r<distances.length-2&&distances[r+1]<distance)r++;
            geometry.row(s,r,a);geometry.row(s,r+1,c);float t=(float)((distance-distances[r])/(distances[r+1]-distances[r]));
            float size=(float)((8+10*hash(i,13))*(1-.002*distance));
            b.tree(pt.x,geometry.surfaceHeight(s,distance,pt.x,pt.z)-.01f,pt.z,size,sample.form,sample.tr,sample.tg,sample.tb,(float)(hash(i,7)*Math.PI*2),(float)(.7+.6*hash(i,43)));trees++;
            if(hash(i,271)>.30){
                // A smaller neighbour makes irregular canopy clusters rather than isolated identical tokens.
                double ss=s+(hash(i,191)-.5)*8,dd=distance+3+4*hash(i,103);
                ss=ss-Math.floor(ss/p.perimeter)*p.perimeter;Point companion=point(p,ss,dd);
                landscape.sample(ss,dd,companion.x,companion.z,surface);
                if(surface.coast>-.7f||surface.space>.1f||surface.forest<.25f)continue;
                p.sampleReadable(landscape.warpedAlong(ss,dd),sample);
                b.tree(companion.x,geometry.surfaceHeight(ss,dd,companion.x,companion.z)-.01f,companion.z,size*(float)(.55+.3*hash(i,59)),sample.form,sample.tr*.94f,sample.tg*.98f,sample.tb*.94f,(float)(hash(i,197)*Math.PI*2),(float)(.8+.4*hash(i,53)));trees++;
            }
        }
        var features=HorizonDistantFeatures.build(geometry);b.append(features.vertices());
        b.append(Arrays.copyOf(water.data,water.count));
        return new HorizonGeometry(p,positions,distances,seamPositions,rows,Arrays.copyOf(b.data,b.count),trees,landscape,features.islands(),features.outcrops(),features.structures());
    }
    private static double[] seamPositions(double[] coarse,int perimeter){
        double[] values=new double[perimeter+1+coarse.length];int tile=0,knot=0,count=0;
        while(tile<=perimeter||knot<coarse.length){
            double value;if(knot==coarse.length||tile<=perimeter&&tile<coarse[knot])value=tile++;
            else{value=coarse[knot++];if(tile<=perimeter&&value==tile)tile++;}
            if(count==0||values[count-1]!=value)values[count++]=value;
        }
        return Arrays.copyOf(values,count);
    }
    public void row(double s,int ring,float[] out){
        s=Math.max(0,Math.min(profile.perimeter,s));int at=Arrays.binarySearch(positions,s);
        if(at>=0){System.arraycopy(rows[ring][at],0,out,0,Math.min(out.length,NODE));return;}
        int right=-at-1,left=right-1;float t=(float)((s-positions[left])/(positions[right]-positions[left]));
        for(int i=0;i<Math.min(out.length,NODE);i++)out[i]=rows[ring][left][i]+(rows[ring][right][i]-rows[ring][left][i])*t;
        float ca=rows[ring][left][14],cb=rows[ring][right][14];
        if(ca*cb<0){float join=ca/(ca-cb);out[1]=t<join?rows[ring][left][1]+(HorizonLandscape.SEA-rows[ring][left][1])*t/join:HorizonLandscape.SEA+(rows[ring][right][1]-HorizonLandscape.SEA)*(t-join)/(1-join);}

        float length=(float)Math.sqrt(out[11]*out[11]+out[12]*out[12]+out[13]*out[13]);
        if(length>1e-8f)for(int i=11;i<14;i++)out[i]/=length;
    }
    /** Exact support on the two submitted triangles, not bilinear interpolation above a warped quad. */
    public float surfaceHeight(double along,double distance,float x,float z){
        int at=Arrays.binarySearch(positions,along);int i=Math.min(positions.length-2,at>=0?at:-at-2);
        int r=0;while(r<distances.length-2&&distances[r+1]<distance)r++;
        float[] a=rows[r][i],b=rows[r][i+1],c=rows[r+1][i+1],d=rows[r+1][i];
        for(float[][] triangle:new float[][][]{new float[][]{a,b,c},new float[][]{a,c,d}}){
            for(boolean wet:new boolean[]{false,true}){var polygon=clip(triangle,wet);for(int k=1;k+1<polygon.size();k++){
                float height=triangleHeight(polygon.get(0),polygon.get(k),polygon.get(k+1),x,z,true);if(Float.isFinite(height))return height;
            }}
        }
        return triangleHeight(a,b,c,x,z,false);
    }
    private static java.util.List<float[]> clip(float[][] triangle,boolean wet){
        var result=new ArrayList<float[]>(4);float[] previous=triangle[triangle.length-1];boolean old=wet?previous[14]>=0:previous[14]<=0;
        for(float[] current:triangle){boolean in=wet?current[14]>=0:current[14]<=0;
            if(in!=old){float t=previous[14]/(previous[14]-current[14]);float[] v=new float[NODE];for(int j=0;j<NODE;j++)v[j]=previous[j]+(current[j]-previous[j])*t;v[1]=HorizonLandscape.SEA;v[8]=0;v[14]=0;
                float length=(float)Math.sqrt(v[11]*v[11]+v[12]*v[12]+v[13]*v[13]);if(length>0)for(int j=11;j<14;j++)v[j]/=length;result.add(v);}
            if(in)result.add(current);previous=current;old=in;
        }return result;
    }
    private static float triangleHeight(float[] a,float[] b,float[] c,float x,float z,boolean inside){
        float det=(b[2]-c[2])*(a[0]-c[0])+(c[0]-b[0])*(a[2]-c[2]);
        if(Math.abs(det)<1e-10f)return a[1];
        float u=((b[2]-c[2])*(x-c[0])+(c[0]-b[0])*(z-c[2]))/det;
        float v=((c[2]-a[2])*(x-c[0])+(a[0]-c[0])*(z-c[2]))/det,w=1-u-v;
        return inside&&(u<-.0001f||v<-.0001f||w<-.0001f)?Float.NaN:u*a[1]+v*b[1]+w*c[1];
    }
    public long fingerprint(){long h=17;for(float f:vertices)h=h*31+Float.floatToIntBits(f);return h;}
    public static double hash(int x,int y){int h=x*0x1f123bb5+y*0x5f356495;h^=h>>>16;h*=0x7feb352d;h^=h>>>15;return (h&0x7fffffff)/2147483647d;}
    private static double noise(double x,double y){int a=(int)Math.floor(x),b=(int)Math.floor(y);double u=x-a,v=y-b;u=u*u*(3-2*u);v=v*v*(3-2*v);return ((1-v)*((1-u)*hash(a,b)+u*hash(a+1,b))+v*((1-u)*hash(a,b+1)+u*hash(a+1,b+1)))*2-1;}
    private static final class Builder {
        float[] data=new float[65536];int count;final float[] scratch=new float[NODE];
        void vertex(float[] v){if(count+STRIDE>data.length)data=Arrays.copyOf(data,data.length*2);System.arraycopy(v,0,data,count,STRIDE);count+=STRIDE;}
        void append(float[] values){while(count+values.length>data.length)data=Arrays.copyOf(data,data.length*2);System.arraycopy(values,0,data,count,values.length);count+=values.length;}
        void coastalQuad(float[] a,float[] b,float[] c,float[] d,Builder water){coastalTriangle(a,b,c,water);coastalTriangle(a,c,d,water);}
        void coastalTriangle(float[] a,float[] b,float[] c,Builder water){
            if(a[6]>.995f&&b[6]>.995f&&c[6]>.995f)return;
            if(a[14]<0&&b[14]<0&&c[14]<0){triangle(a,b,c);return;}
            if(a[14]>0&&b[14]>0&&c[14]>0){triangle(a,b,c);water.waterTriangle(a,b,c);return;}
            float[][] triangle={a,b,c};
            for(boolean wet:new boolean[]{false,true}){
                var polygon=clip(triangle,wet);for(int i=1;i+1<polygon.size();i++){
                    triangle(polygon.get(0),polygon.get(i),polygon.get(i+1));
                    if(wet)water.waterTriangle(polygon.get(0),polygon.get(i),polygon.get(i+1));
                }
            }
        }
        void waterTriangle(float[] a,float[] b,float[] c){waterVertex(a);waterVertex(b);waterVertex(c);}
        void waterVertex(float[] v){System.arraycopy(v,0,scratch,0,NODE);scratch[1]=HorizonLandscape.SEA;scratch[3]=v[15];scratch[4]=v[16];scratch[5]=v[17];scratch[7]=1;scratch[9]=scratch[10]=0;scratch[11]=scratch[13]=0;scratch[12]=1;vertex(scratch);}
        void quad(float[] a,float[] b,float[] c,float[] d){
            if(a[6]>.995f&&b[6]>.995f&&c[6]>.995f&&d[6]>.995f)return;
            vertex(a);vertex(b);vertex(c);vertex(a);vertex(c);vertex(d);
        }
        void triangle(float[] a,float[] b,float[] c){vertex(a);vertex(b);vertex(c);}
        float[] v(float x,float y,float z,float r,float g,float b,float shade){return new float[]{x,y,z,r*shade,g*shade,b*shade,0,0,0,0,0,0,0,0,-1};}
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
