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
    static BorderBiomeProfile mixedForestCoast(){return BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){
        public int width(){return 64;}public int height(){return 48;}
        public BorderBiomeProfile.Cell cell(int x,int y){var biome=x<32?FOREST:OCEAN;return new BorderBiomeProfile.Cell(biome,.2f,.4f,.5f,false,biome==OCEAN,biome==FOREST?1:0,.1f,.35f,.15f,VegetationInstances.Form.PINE.ordinal());}
    },8);}
    static float outside(HorizonGeometry g,float x,float z){return Math.max(Math.max(-.5f-x,x-g.profile.width+.5f),Math.max(z-.5f,-g.profile.height+.5f-z));}
    static boolean bandVertex(HorizonGeometry g,float[] expected,float near,float far){
        int stride=HorizonGeometry.STRIDE;
        for(int i=0;i<g.vertices.length;i+=stride*3){float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
            if(g.vertices[i+7]>.5f||g.vertices[i+11]==0&&g.vertices[i+12]==0&&g.vertices[i+13]==0)continue;
            for(int v=0;v<3;v++){int at=i+v*stride;float d=outside(g,g.vertices[at],g.vertices[at+2]);min=Math.min(min,d);max=Math.max(max,d);}
            if(min<near-.001f||max>far+.001f||max-min<.01f)continue;
            for(int v=0;v<3;v++){int at=i+v*stride;if(Math.abs(g.vertices[at]-expected[0])<.0001f&&Math.abs(g.vertices[at+1]-expected[1])<.0001f&&Math.abs(g.vertices[at+2]-expected[2])<.0001f)return true;}
        }return false;
    }
    static void fractionalSeams(){
        var p=mixedForestCoast();var geometry=HorizonGeometry.build(p,s->2+Math.sin(s*.13));int fractional=0;
        for(int i=0;i<geometry.seamPositions.length;i++){
            if(i>0)check(geometry.seamPositions[i]-geometry.seamPositions[i-1]<=1.00001,"shared seam segments never span multiple native tiles");
        }
        for(double s:geometry.positions){
            check(Arrays.binarySearch(geometry.seamPositions,s)>=0,"every coarse knot also belongs to both collar joins");
            if(s==Math.floor(s))continue;fractional++;
            var vertex=new float[HorizonGeometry.STRIDE];geometry.row(s,0,vertex);
            check(bandVertex(geometry,vertex,4,12),"d4 first band retains its fractional biome vertex: "+s);
            geometry.row(s,1,vertex);check(bandVertex(geometry,vertex,4,12),"d12 fine band retains fractional biome vertex: "+s);
            check(bandVertex(geometry,vertex,12,24),"d12 coarse band shares identical fractional biome vertex: "+s);
        }
        check(fractional>=4,"mixed coast fixture exercises fractional material knots");
    }
    static void treeSites(){
        var p=mixedForestCoast();int tested=0;
        for(int seed=0;seed<8;seed++){
            var composition=new HorizonComposition(0,0,0,0,0,0,0,0,0,0,0,seed);var geometry=HorizonGeometry.build(p,s->0,composition);
            var treeVertices=new ArrayList<Integer>();int stride=HorizonGeometry.STRIDE;
            for(int i=0;i<geometry.vertices.length;i+=stride)if(geometry.vertices[i+11]==0&&geometry.vertices[i+12]==0&&geometry.vertices[i+13]==0)treeVertices.add(i);
            check(treeVertices.size()==geometry.treeCount*36,"pine proxy vertex groups agree with actual tree count");
            for(int tree=0;tree<treeVertices.size();tree+=36){
                int a=treeVertices.get(tree),b=treeVertices.get(tree+1);float x=(geometry.vertices[a]+geometry.vertices[b])*.5f,z=(geometry.vertices[a+2]+geometry.vertices[b+2])*.5f;
                double d=outside(geometry,x,z),along;
                if(Math.abs(z-.5-d)<.001)along=(x+.5+d)/(p.width+2*d)*p.width;
                else if(Math.abs(x-p.width+.5-d)<.001)along=p.width+(-z+.5+d)/(p.height+2*d)*p.height;
                else if(Math.abs(-z-p.height+.5-d)<.001)along=p.width+p.height+(p.width-.5+d-x)/(p.width+2*d)*p.width;
                else along=2d*p.width+p.height+(p.height-.5+d+z)/(p.height+2*d)*p.height;
                var surface=new HorizonLandscape.Surface();geometry.landscape.sample(along,d,x,z,surface);
                check(surface.coast<=-.699f&&surface.space<=.101f&&surface.forest>=.249f,"every primary/companion tree is rooted on eligible forest land, not seabed");tested++;
            }
        }
        check(tested>100,"mixed-coast test exercises many emitted primary and companion proxies");
    }
    public static void main(String[] args){
        for(var biome:BorderBiomeProfile.Biome.values()){
            var p=BorderBiomeProfile.analyze(source(64,48,biome),8);var q=BorderBiomeProfile.analyze(source(64,48,biome),8);
            check(p.fingerprint()==q.fingerprint(),"profile deterministic");
            var a=HorizonGeometry.build(p,s->0);var b=HorizonGeometry.build(q,s->0);check(Arrays.equals(a.vertices,b.vertices),"geometry deterministic");
            check(a.distances.length>=10,"multi-scale terrain has geometric distance bands instead of five giant strips");
            if(biome==FOREST||biome==MOUNTAINS)for(int r=1;r<a.distances.length;r++)for(double s:a.positions){
                if(s==p.perimeter)continue;float[] vertex=new float[HorizonGeometry.STRIDE];a.row(s,r,vertex);
                check(Math.abs(a.surfaceHeight(s,a.distances[r],vertex[0],vertex[2])-vertex[1])<.001,"proxy support matches actual submitted triangle vertices");
            }
            check(a.vertices.length%(HorizonGeometry.STRIDE*3)==0,"triangular XYZ/RGBA/meta/material/normal topology");
            float[] join=new float[HorizonGeometry.STRIDE];for(int s=0;s<(int)p.perimeter;s++){a.row(s,1,join);check(Float.isFinite(join[1])&&join[14]>=-1.001&&join[14]<=1.001,"coast field and geometry share bounded join metadata");}
            for(int i=0;i<a.vertices.length;i+=HorizonGeometry.STRIDE){
                for(int j=0;j<HorizonGeometry.STRIDE;j++)check(Float.isFinite(a.vertices[i+j]),"finite background vertex");
                check(a.vertices[i]<-.49||a.vertices[i]>63.49||a.vertices[i+2]>.49||a.vertices[i+2]<-47.49,"no geometry inside playable map");
                float n=a.vertices[i+11]*a.vertices[i+11]+a.vertices[i+12]*a.vertices[i+12]+a.vertices[i+13]*a.vertices[i+13];
                check(n==0||Math.abs(n-1)<.001,"terrain has unit smooth normals; proxies explicitly bypass terrain shading");
            }
            if(biome==SPACE){check(a.vertices.length>0,"space has a batched cosmic mask");for(int at=0;at<a.vertices.length;at+=HorizonGeometry.STRIDE)check(a.vertices[at+6]>.995f&&a.vertices[at+7]==0,"cosmic mask contains no ordinary land/water material");}
            if(biome==OCEAN)for(int i=0;i<a.vertices.length;i+=HorizonGeometry.STRIDE)check(a.vertices[i+7]>.5?Math.abs(a.vertices[i+1]+.16)<.00001:a.vertices[i+1]<-.16,"separate horizontal ocean and deeper seabed, never water relief");
            if(biome==FOREST){check(a.treeCount>0&&a.treeCount<=384,"forest has bounded lightweight decorative proxies");
                var roots=new HashSet<Integer>();for(int i=0;i<a.vertices.length;i+=HorizonGeometry.STRIDE)if(a.vertices[i+12]==0)roots.add(Float.floatToIntBits(a.vertices[i+2]));
                check(roots.size()>4,"varied proxy positions do not occupy two synchronized rows");}
            for(double s:new double[]{0,64,112,176,224})for(double d:new double[]{0,2,64,2048}){
                var start=HorizonGeometry.point(p,s,d);var close=HorizonGeometry.point(p,s+p.perimeter,d);check(start.equals(close),"shared corner/perimeter closure");
                var left=new BorderBiomeProfile.Sample();var right=new BorderBiomeProfile.Sample();p.sample(s-1e-7,left);p.sample(s+1e-7,right);for(int i=0;i<left.weights.length;i++)check(Math.abs(left.weights[i]-right.weights[i])<.0001,"cyclic smooth material seam");
            }
        }
        var large=BorderBiomeProfile.analyze(source(8192,8192,OCEAN),8);check(large.sections().size()<=512,"far detail bounded on huge logical grids");
        var mesh=HorizonGeometry.build(large,s->0);check(mesh.vertices.length/HorizonGeometry.STRIDE<=large.perimeter*12+100000,"two visual ocean layers remain linear in perimeter with bounded far bands");
        var tiny=BorderBiomeProfile.analyze(source(1,1,FOREST),8);check(HorizonGeometry.build(tiny,s->0).vertices.length>0,"one-cell world remains finite");
        check(HorizonGeometry.boundRadius(32,32)>2048,"far bound encloses small-map high-altitude continuation");
        fractionalSeams();treeSites();
        System.out.println("HorizonGeometryTest: "+checks+" deterministic/finite/outside-only/corner/huge-grid checks PASS; synthetic grids, not physical Linux acceptance");
    }
}
