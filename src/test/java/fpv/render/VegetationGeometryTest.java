package fpv.render;
import java.util.*;

public final class VegetationGeometryTest {
    public static void main(String[] args){int checks=0;
        for(var form:VegetationInstances.Form.values())for(int variant=0;variant<8;variant++){
            float[] mesh=OrganicPropGeometry.vegetation(form,variant),copy=mesh.clone();
            if(mesh!=OrganicPropGeometry.vegetation(form,variant+8))throw new AssertionError("Reusable cached vegetation template");
            if(mesh.length%18!=0||mesh.length/6>512)throw new AssertionError("Bounded triangular topology");
            float min=1,max=0;
            for(int i=0;i<mesh.length;i+=6){for(int j=0;j<6;j++){if(!Float.isFinite(mesh[i+j]))throw new AssertionError("Finite geometry");checks++;}
                if(Math.hypot(mesh[i],mesh[i+2])>.496||mesh[i+1]<0||mesh[i+1]>1)throw new AssertionError("Local profile bounds");
                min=Math.min(min,mesh[i+1]);max=Math.max(max,mesh[i+1]);}
            if(min>.0001||max<.70)throw new AssertionError("Root anchored, actual vertical volume: "+form);
            if(!Arrays.equals(mesh,copy))throw new AssertionError("Immutable template");
        }
        if(Arrays.equals(OrganicPropGeometry.vegetation(VegetationInstances.Form.CORAL,0),OrganicPropGeometry.vegetation(VegetationInstances.Form.SHRUB,0)))throw new AssertionError("Distinct coral silhouette");
        System.out.println("VegetationGeometryTest: "+checks+" finite cached vertex checks PASS;8 profiles x8 variants, bounded topology and anchored roots");
    }
}
