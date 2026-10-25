package fpv.render;

import fpv.terrain.TerrainHeightField;
import java.util.*;

public final class MountainGeometryTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static final TerrainHeightField.Parameters PARAMS=new TerrainHeightField.Parameters(.85,1.15,30,3,.23,18);
    static class Source implements MountainGeometry.Source {
        final TerrainHeightField field;
        Source(int size,boolean[] natural){field=TerrainHeightField.generate(size,size,natural,PARAMS);}
        public boolean natural(int x,int y){return field.isNatural(x,y);}
        public float corner(int x,int y,int c){return field.tileCornerHeight(x,y,c);}
        public float maximum(){return field.maxHeight();}
    }
    static class Capture implements MountainGeometry.Sink {
        final ArrayList<float[]> tops=new ArrayList<>(),sides=new ArrayList<>();
        public void triangle(float[] v,float shade,boolean top){(top?tops:sides).add(v.clone());check(shade>=.56&&shade<=1.13,"bounded coherent lighting");}
    }
    public static void main(String[] args){
        for(int scenario=0;scenario<4;scenario++){
            int n=48;boolean[] mask=new boolean[n*n];
            for(int y=0;y<n;y++)for(int x=0;x<n;x++)mask[y*n+x]=switch(scenario){case 0->x>2&&x<44&&y>3&&y<44;case 1->(x*37+y*17)%23<4;case 2->x>1&&y>1&&(x<18||y<12||x>30);default->true;};
            var src=new Source(n,mask);var geometry=new MountainGeometry(16);
            for(var style:new MountainStyle[]{MountainStyle.BLOCKY,MountainStyle.HYBRID}){
                long hash=0;int sides=0;
                for(int y=0;y<n;y++)for(int x=0;x<n;x++)if(src.natural(x,y)){
                    var cap=new Capture();geometry.emit(src,x,y,style,cap);check(!cap.tops.isEmpty(),"every mountain cell has a roof");
                    for(var face:cap.tops){
                        hash=hash*31+Arrays.hashCode(face);
                        for(int i=0;i<15;i+=5){
                            check(face[i]>=x-.50001&&face[i]<=x+.50001&&face[i+2]>=-y-.50001&&face[i+2]<=-y+.50001,"no overhang into structures/plants/open cells");
                            double u=face[i]-x+.5,v=-face[i+2]-y+.5;
                            float[] c={src.corner(x,y,0),src.corner(x,y,1),src.corner(x,y,2),src.corner(x,y,3)};
                            double canonical=MountainGeometry.interpolate(c,(float)u,(float)v);
                            check(Math.abs(face[i+1]-canonical)<=MountainGeometry.MAX_ERROR+2e-5,"roof remains within canonical collision tolerance");
                            check(face[i+1]>=0&&face[i+1]<=src.maximum()+1e-5,"no maximum height increase");
                            if(Math.abs((face[i]+.5)/16-Math.rint((face[i]+.5)/16))<1e-6||Math.abs((-face[i+2]+.5)/16-Math.rint((-face[i+2]+.5)/16))<1e-6)check(Math.abs(face[i+1]-canonical)<2e-5,"exact shared chunk apron including coarse LOD");
                        }
                    }
                    for(var face:cap.sides){sides++;for(int i=0;i<15;i+=5){check(face[i+1]>=-1e-5&&face[i+1]<=src.maximum()+1e-5,"side vertically bounded");check(face[i+3]>=-1e-5&&face[i+3]<=1.00001&&face[i+4]>=-1e-5&&face[i+4]<=1.00001,"side UV inside one tile, no atlas repeat leakage");}}
                    // A second client / reload starts with empty scratch and gets identical triangles.
                    var repeat=new Capture();new MountainGeometry(16).emit(src,x,y,style,repeat);
                    check(cap.tops.size()==repeat.tops.size()&&cap.sides.size()==repeat.sides.size(),"deterministic topology");
                    for(int i=0;i<cap.tops.size();i++)check(Arrays.equals(cap.tops.get(i),repeat.tops.get(i)),"deterministic vertices/UVs");
                    for(int i=0;i<cap.sides.size();i++)check(Arrays.equals(cap.sides.get(i),repeat.sides.get(i)),"deterministic exposed risers");
                }
                check(sides>0&&hash!=0,"real vertical cliff geometry");
            }
        }
        MountainGeometry.Source plateau=new MountainGeometry.Source(){public boolean natural(int x,int y){return true;}public float corner(int x,int y,int c){return 4;}public float maximum(){return 4;}};
        var flat=new Capture();new MountainGeometry(16).emit(plateau,5,5,MountainStyle.BLOCKY,flat);
        check(flat.tops.size()==2&&flat.sides.isEmpty(),"merge compatible roof patches and cull every hidden internal face");
        MountainGeometry.Source tall=new MountainGeometry.Source(){public boolean natural(int x,int y){return y==5&&(x==5||x==6);}public float corner(int x,int y,int c){return 18;}public float maximum(){return 18;}};
        for(int x=5;x<=6;x++){
            var cliff=new Capture();new MountainGeometry(16).emit(tall,x,5,MountainStyle.BLOCKY,cliff);
            for(var f:cliff.sides){
                float min=Float.POSITIVE_INFINITY,max=0;boolean internal=true;
                for(int i=0;i<15;i+=5){min=Math.min(min,f[i+1]);max=Math.max(max,f[i+1]);internal&=f[i]==5.5f;}
                check(max-min<=1.00001,"tall cliffs tiled in world-height bands, not stretched");check(!internal,"no hidden face between adjacent tall cubes");
            }
        }
        System.out.println("MountainGeometry: "+checks+" deterministic/bounds/collision-tolerance/chunk-seam/UV/hidden-face/merged-roof checks PASS");
    }
}
