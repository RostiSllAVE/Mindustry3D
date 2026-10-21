package fpv.render;

import java.util.Arrays;

public final class ResourcePortGeometryTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int cells:new int[]{1,2,3,4,8,16})for(boolean detail:new boolean[]{false,true}){
            float height=.24f;var shape=ResourcePortGeometry.generate(cells,height,detail);
            check(shape.ports()==4*cells&&shape.perSide()==cells,"one actual opening per cell on all four sides");
            check(Arrays.equals(shape.shell(),ResourcePortGeometry.generate(cells,height,detail).shell()),"deterministic reusable mesh");
            check(shape.shell().length/8<=cells*4*36+6&&shape.lining().length/8<=cells*4*42,"bounded triangles per bay");
            for(var v:new float[][]{shape.shell(),shape.lining()})for(int i=0;i<v.length;i+=8){
                for(int j=0;j<8;j++)check(Float.isFinite(v[i+j]),"finite tunnel vertex/UV");
                check(Math.abs(v[i])<=.490001&&Math.abs(v[i+2])<=.490001,"inside original footprint");
                check(v[i+1]>=0&&v[i+1]<=height,"foundation contact/height preserved");
            }
            for(int side=0;side<4;side++)for(int cell=0;cell<cells;cell++){
                float center=-.49f+(cell+.5f)*(.98f/cells);boolean blocked=false,recessed=false;
                for(int i=0;i<shape.shell().length;i+=24){var v=shape.shell();float a=side==0?v[i]:side==1?-v[i+2]:side==2?-v[i]:v[i+2];
                    float b=side==0?v[i+8]:side==1?-v[i+10]:side==2?-v[i+8]:v[i+10];
                    float c=side==0?v[i+16]:side==1?-v[i+18]:side==2?-v[i+16]:v[i+18];
                    // At the middle of the mouth, no front-side triangle may cover the low aperture.
                    float front0=side==0?v[i+2]:side==1?v[i]:side==2?-v[i+2]:-v[i];
                    float front1=side==0?v[i+10]:side==1?v[i+8]:side==2?-v[i+10]:-v[i+8];
                    float front2=side==0?v[i+18]:side==1?v[i+16]:side==2?-v[i+18]:-v[i+16];
                    if(front0>.4899&&front1>.4899&&front2>.4899&&Math.min(a,Math.min(b,c))<center&&Math.max(a,Math.max(b,c))>center&&Math.min(v[i+1],Math.min(v[i+9],v[i+17]))<height*.2)blocked=true;
                }
                for(int i=0;i<shape.lining().length;i+=8){float front=side==0?shape.lining()[i+2]:side==1?shape.lining()[i]:side==2?-shape.lining()[i+2]:-shape.lining()[i];if(front>.40&&front<.489)recessed=true;}
                check(!blocked&&recessed,"visible front aperture and recessed inner lining on every side");
            }
        }
        try{ResourcePortGeometry.generate(0,.2f,true);throw new AssertionError("zero cells accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("ResourcePortGeometryTest: "+checks+" aperture counts/recess/finite/footprint/height/determinism/budget checks PASS");
    }
}
