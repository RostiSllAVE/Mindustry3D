package fpv.render;
import java.util.*;
import static fpv.render.BorderBiomeProfile.Biome.*;

/** Synthetic border-only grids also cover huge logical maps without creating simulation tiles. */
public final class HorizonGeometryTest {
    static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static BorderBiomeProfile.Source source(int w,int h,BorderBiomeProfile.Biome biome){return new BorderBiomeProfile.Source(){
        final BorderBiomeProfile.Cell c=new BorderBiomeProfile.Cell(biome,.3f,.4f,.5f,biome==MOUNTAINS,biome==OCEAN,biome==FOREST?1:0,.1f,.35f,.15f,0);
        public int width(){return w;}public int height(){return h;}
        public BorderBiomeProfile.Cell cell(int x,int y){check(x<8||y<8||x>=w-8||y>=h-8,"only a bounded edge strip is visited");return c;}
    };}
    public static void main(String[] args){
        for(var biome:BorderBiomeProfile.Biome.values()){
            var p=BorderBiomeProfile.analyze(source(64,48,biome),8);var q=BorderBiomeProfile.analyze(source(64,48,biome),8);
            check(p.fingerprint()==q.fingerprint(),"profile deterministic");
            var a=HorizonGeometry.build(p,s->0);var b=HorizonGeometry.build(q,s->0);check(Arrays.equals(a.vertices,b.vertices),"geometry deterministic");
            check(a.vertices.length%27==0,"triangular XYZ/RGBA/meta topology");
            float[] join=new float[9];for(int s=0;s<(int)p.perimeter;s++){a.row(s,1,join);check(join[8]==0,"all fine/coarse LOD join points stay fixed under wave displacement");}
            for(int i=0;i<a.vertices.length;i+=9){
                for(int j=0;j<9;j++)check(Float.isFinite(a.vertices[i+j]),"finite background vertex");
                check(a.vertices[i]<-.49||a.vertices[i]>63.49||a.vertices[i+2]>.49||a.vertices[i+2]<-47.49,"no geometry inside playable map");
            }
            if(biome==SPACE)check(a.vertices.length==0,"space is empty scenery, no portal/tiles");
            if(biome==OCEAN)for(int i=0;i<a.vertices.length;i+=9)check(Math.abs(a.vertices[i+1]+.16)<.00001,"ocean remains flat shallow visual baseline before cheap shader waves");
            if(biome==FOREST)check(a.treeCount>0,"forest has lightweight decorative proxies");
            for(double s:new double[]{0,64,112,176,224})for(double d:new double[]{0,2,64,2048}){
                var start=HorizonGeometry.point(p,s,d);var close=HorizonGeometry.point(p,s+p.perimeter,d);check(start.equals(close),"shared corner/perimeter closure");
                var left=new BorderBiomeProfile.Sample();var right=new BorderBiomeProfile.Sample();p.sample(s-1e-7,left);p.sample(s+1e-7,right);for(int i=0;i<left.weights.length;i++)check(Math.abs(left.weights[i]-right.weights[i])<.0001,"cyclic smooth material seam");
            }
        }
        var large=BorderBiomeProfile.analyze(source(8192,8192,OCEAN),8);check(large.sections().size()<=512,"far detail bounded on huge logical grids");
        var mesh=HorizonGeometry.build(large,s->0);check(mesh.vertices.length/9<=large.perimeter*6+25000,"linear collar seam plus bounded far bands");
        var tiny=BorderBiomeProfile.analyze(source(1,1,FOREST),8);check(HorizonGeometry.build(tiny,s->0).vertices.length>0,"one-cell world remains finite");
        check(HorizonGeometry.boundRadius(32,32)>2048,"far bound encloses small-map high-altitude continuation");
        System.out.println("HorizonGeometryTest: "+checks+" deterministic/finite/outside-only/corner/huge-grid checks PASS; synthetic grids, not physical Linux acceptance");
    }
}
