package fpv.render;

import java.util.*;

/** Cached dual-icosphere: thirty hexagonal and twelve pentagonal panels form a closed globe. */
public final class ShieldActivityGeometry {
    public record Model(float[] panels,float[] edges,float[] coarse,int hexagons,int pentagons){}
    private static final Model MODEL=generate();
    private ShieldActivityGeometry(){}
    public static Model model(){return MODEL;}
    private static Model generate(){
        double t=(1+Math.sqrt(5))/2;var vertices=new ArrayList<double[]>();
        for(double[] p:new double[][]{{-1,t,0},{1,t,0},{-1,-t,0},{1,-t,0},{0,-1,t},{0,1,t},{0,-1,-t},{0,1,-t},{t,0,-1},{t,0,1},{-t,0,-1},{-t,0,1}})vertices.add(unit(p));
        int[][] initial={{0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},{1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},{3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},{4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}};
        var midpoints=new HashMap<Long,Integer>();var faces=new ArrayList<int[]>();
        for(var f:initial){int a=middle(vertices,midpoints,f[0],f[1]),b=middle(vertices,midpoints,f[1],f[2]),c=middle(vertices,midpoints,f[2],f[0]);faces.add(new int[]{f[0],a,c});faces.add(new int[]{f[1],b,a});faces.add(new int[]{f[2],c,b});faces.add(new int[]{a,b,c});}
        var panels=new ArrayList<Float>();var edges=new ArrayList<Float>();var coarse=new ArrayList<Float>();
        for(var f:faces)for(int v:f)put(coarse,vertices.get(v));
        int hex=0,pent=0;
        for(int i=0;i<vertices.size();i++){
            var n=vertices.get(i);double[] u=unit(Math.abs(n[1])<.9?new double[]{-n[2],0,n[0]}:new double[]{0,n[2],-n[1]}),v=cross(n,u);
            var ring=new ArrayList<double[]>();
            for(var f:faces)if(f[0]==i||f[1]==i||f[2]==i){var a=vertices.get(f[0]);var b=vertices.get(f[1]);var c=vertices.get(f[2]);ring.add(unit(new double[]{a[0]+b[0]+c[0],a[1]+b[1]+c[1],a[2]+b[2]+c[2]}));}
            ring.sort(Comparator.comparingDouble(p->Math.atan2(dot(p,v),dot(p,u))));if(ring.size()==6)hex++;else if(ring.size()==5)pent++;else throw new IllegalStateException("Invalid geodesic cell");
            for(int j=0;j<ring.size();j++){
                double[] a=inset(ring.get(j),n),b=inset(ring.get((j+1)%ring.size()),n);put(panels,n);put(panels,a);put(panels,b);
                var normal=unit(new double[]{a[0]+b[0],a[1]+b[1],a[2]+b[2]});var side=unit(cross(new double[]{b[0]-a[0],b[1]-a[1],b[2]-a[2]},normal));
                double[] ap=offset(a,side,.006),am=offset(a,side,-.006),bp=offset(b,side,.006),bm=offset(b,side,-.006);
                put(edges,ap);put(edges,bp);put(edges,bm);put(edges,ap);put(edges,bm);put(edges,am);
            }
        }
        return new Model(array(panels),array(edges),array(coarse),hex,pent);
    }
    private static double[] inset(double[] p,double[] n){return unit(new double[]{p[0]*.94+n[0]*.06,p[1]*.94+n[1]*.06,p[2]*.94+n[2]*.06});}
    private static double[] offset(double[] p,double[] s,double d){return new double[]{p[0]*1.002+s[0]*d,p[1]*1.002+s[1]*d,p[2]*1.002+s[2]*d};}
    private static int middle(ArrayList<double[]> v,Map<Long,Integer> cache,int a,int b){long key=((long)Math.min(a,b)<<32)|Math.max(a,b);var old=cache.get(key);if(old!=null)return old;var p=v.get(a);var q=v.get(b);int id=v.size();v.add(unit(new double[]{p[0]+q[0],p[1]+q[1],p[2]+q[2]}));cache.put(key,id);return id;}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double[] unit(double[] p){double d=Math.sqrt(dot(p,p));return new double[]{p[0]/d,p[1]/d,p[2]/d};}
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static void put(ArrayList<Float> a,double[] p){for(double v:p)a.add((float)v);}
    private static float[] array(ArrayList<Float> a){var out=new float[a.size()];for(int i=0;i<out.length;i++)out[i]=a.get(i);return out;}
}
