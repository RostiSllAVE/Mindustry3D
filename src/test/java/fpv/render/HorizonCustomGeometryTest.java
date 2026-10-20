package fpv.render;
import fpv.background.*;
import java.util.Arrays;

public final class HorizonCustomGeometryTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static BorderBiomeProfile profile(){return BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){public int width(){return 96;}public int height(){return 64;}public BorderBiomeProfile.Cell cell(int x,int y){return BackgroundPreset.OCEAN.cell();}},8);}
    public static void main(String[] args){
        var p=profile();var style=new BackgroundStyle();style.mode=BackgroundStyle.Mode.CUSTOM;style.paint=new BackgroundPaint(96,64);
        var auto=HorizonGeometry.build(p,s->0);check(Arrays.equals(auto.vertices,HorizonGeometry.build(p,s->0,HorizonComposition.EMPTY,s->Double.NaN,style).vertices),"empty Custom on Auto is exact original geometry");
        var paint=style.paint;paint.line(48,160,48,160,65,.8,BackgroundPaint.Brush.ISLAND,14);paint.line(48,160,48,160,65,.8,BackgroundPaint.Brush.FOREST,0);paint.line(-180,32,-180,32,80,.7,BackgroundPaint.Brush.MOUNTAINS,90);
        var g=HorizonGeometry.build(p,s->0,HorizonComposition.EMPTY,s->Double.NaN,style);var repeat=HorizonGeometry.build(p,s->0,HorizonComposition.EMPTY,s->Double.NaN,style);check(Arrays.equals(g.vertices,repeat.vertices),"custom geometry deterministic across regeneration");
        var sample=new HorizonLandscape.Surface();var pos=HorizonGeometry.coordinates(p,48,-160);g.landscape.sample(pos.along(),pos.distance(),48,-160,sample);check(sample.coast<-.9&&sample.height>10&&sample.forest>.9,"island rises out of actual ocean field and retains forest/height");
        pos=HorizonGeometry.coordinates(p,-180,-32);g.landscape.sample(pos.along(),pos.distance(),-180,-32,sample);check(sample.height>80,"painted mountain ridge height follows art mask");
        int water=0,land=0;for(int i=0;i<g.vertices.length;i+=HorizonGeometry.STRIDE){for(int j=0;j<HorizonGeometry.STRIDE;j++)check(Float.isFinite(g.vertices[i+j]),"finite custom mesh");if(g.vertices[i+7]>.5f){water++;check(g.vertices[i+1]==HorizonLandscape.SEA,"water always remains a horizontal layer");}else if(g.vertices[i+1]>2)land++;check(g.vertices[i]<-.49||g.vertices[i]>95.49||g.vertices[i+2]>.49||g.vertices[i+2]<-63.49,"no custom vertices inside playable map");}
        check(water>0&&land>0&&g.islandCount==0,"painted islands are terrain, no independent floating island objects");check(g.treeCount>0&&g.treeCount<=384,"painted forest gets bounded cached tree silhouettes");check(g.vertices.length/HorizonGeometry.STRIDE<300_000,"small custom map mesh stays bounded");
        paint.line(-180,32,-180,32,100,.8,BackgroundPaint.Brush.VOID,0);var field=new HorizonLandscape(p,HorizonComposition.EMPTY,s->Double.NaN,style);pos=HorizonGeometry.coordinates(p,-180,-32);field.sample(pos.along(),pos.distance(),-180,-32,sample);check(sample.space>.99,"Void removes selected scenery, no map-floor portal");
        for(double along:new double[]{0,48,96,128,160,224,280})for(double distance:new double[]{4,12,150,1800}){var point=HorizonGeometry.point(p,along,distance);var inverse=HorizonGeometry.coordinates(p,point.x(),point.z());check(Math.abs(inverse.distance()-distance)<.001,"shared painter/scenery world coordinate inverse");}
        System.out.println("HorizonCustomGeometryTest: "+checks+" custom field/island/forest/mountain/void/coast/water/bounds/determinism checks PASS");
    }
}
