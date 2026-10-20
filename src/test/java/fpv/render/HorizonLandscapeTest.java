package fpv.render;
import java.util.*;
import static fpv.render.BorderBiomeProfile.Biome.*;

public final class HorizonLandscapeTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static BorderBiomeProfile.Cell cell(BorderBiomeProfile.Biome biome){return new BorderBiomeProfile.Cell(biome,biome==OCEAN?.2f:.7f,biome==FOREST?.6f:.4f,biome==OCEAN?.8f:.2f,biome==MOUNTAINS,biome==OCEAN,biome==FOREST?1:0,.1f,.4f,.15f,0);}
    static BorderBiomeProfile profile(boolean mixed){return BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){public int width(){return 96;}public int height(){return 64;}public BorderBiomeProfile.Cell cell(int x,int y){return HorizonLandscapeTest.cell(!mixed||y>=56?OCEAN:x<12?MOUNTAINS:DESERT);}},8);}
    public static void main(String[] args){
        var p=profile(true);var field=new HorizonLandscape(p,HorizonComposition.EMPTY);var out=new HorizonLandscape.Surface();
        var a=HorizonGeometry.build(p,s->20);var b=HorizonGeometry.build(p,s->20);check(Arrays.equals(a.vertices,b.vertices),"coastal split geometry deterministic");
        int water=0,bed=0;
        for(int i=0;i<a.vertices.length;i+=HorizonGeometry.STRIDE){
            if(a.vertices[i+7]>.5f){water++;check(a.vertices[i+1]==HorizonLandscape.SEA,"water surfaces never inherit rocky/mountain heights");check(a.vertices[i+12]==1&&a.vertices[i+11]==0&&a.vertices[i+13]==0,"water normals remain horizontal");check(a.vertices[i+14]>=-.00001,"water geometry clipped to actual coastal field");}
            else if(a.vertices[i+8]>0&&a.vertices[i+14]>.01){bed++;check(a.vertices[i+1]<=HorizonLandscape.SEA+.00001,"wet ground remains under surface even against a tall native seam");}
        }
        check(water>0&&bed>0,"water and seabed are separate layers");
        var pure=new HorizonLandscape(profile(false),HorizonComposition.EMPTY);float old=0;
        for(double d:new double[]{4,12,30,100,400,900,2000}){var pt=HorizonGeometry.point(p,200,d);pure.sample(200,d,pt.x(),pt.z(),out);check(out.coast==1&&out.ocean,"unbroken water edge is open ocean");check(out.height<HorizonLandscape.SEA&&out.depth>=old,"ocean bottom deepens monotonically");old=out.depth;}
        BorderBiomeProfile lake=BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){public int width(){return 96;}public int height(){return 64;}public BorderBiomeProfile.Cell cell(int x,int y){return HorizonLandscapeTest.cell(y>=56&&x>=40&&x<48?OCEAN:DESERT);}},8);
        var lakeField=new HorizonLandscape(lake,HorizonComposition.EMPTY);double along=96+64+96-44;
        var near=HorizonGeometry.point(lake,along,12);lakeField.sample(along,12,near.x(),near.z(),out);check(out.coast>.9&&!out.ocean&&out.style==HorizonLandscape.Coast.BEACH,"small sand-edged water run is a beach basin");
        var far=HorizonGeometry.point(lake,along,400);lakeField.sample(along,400,far.x(),far.z(),out);check(out.coast<0,"small enclosed basin does not become an infinite sea wedge");
        for(var type:new BorderBiomeProfile.Biome[]{MOUNTAINS,DESERT,FOREST,ICE,VOLCANIC}){
            var pp=BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){public int width(){return 64;}public int height(){return 64;}public BorderBiomeProfile.Cell cell(int x,int y){return HorizonLandscapeTest.cell(type);}},8);
            var f=new HorizonLandscape(pp,HorizonComposition.EMPTY);var pt=HorizonGeometry.point(pp,32,1800);f.sample(32,1800,pt.x(),pt.z(),out);check(out.height>5,"land horizon retains raised closure for "+type);
        }
        check(a.vertices.length<600000,"small coastal scenery stays bounded");
        System.out.println("HorizonLandscapeTest: "+checks+" flat water/separate basin/depth/coast/closure/determinism checks PASS");
    }
}
