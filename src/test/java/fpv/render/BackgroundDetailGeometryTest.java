package fpv.render;
import fpv.background.*;
public final class BackgroundDetailGeometryTest {
    static int checks;static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        var p=BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){public int width(){return 96;}public int height(){return 64;}public BorderBiomeProfile.Cell cell(int x,int y){return BackgroundPreset.OCEAN.cell();}},8);
        var s=new BackgroundStyle();s.mode=BackgroundStyle.Mode.CUSTOM;var mask=s.paint(96,64);mask.stroke(BackgroundPaint.Brush.ISLAND,180,.8,1,12,(x,y)->0).line(-260,32,-260,32);mask.stroke(BackgroundPaint.Brush.INDUSTRIAL,130,.8,1,0,(x,y)->0).line(-260,32,-260,32);
        var geometry=HorizonGeometry.build(p,a->0,HorizonComposition.EMPTY,a->Double.NaN,s);check(geometry.structureCount>0&&geometry.structureCount<=6,"painted industrial material creates bounded real mesh motifs without native composition");
        mask.clear();mask.stroke(BackgroundPaint.Brush.ISLAND,180,.8,1,12,(x,y)->0).line(-260,32,-260,32);mask.stroke(BackgroundPaint.Brush.GRASS,130,.8,1,0,(x,y)->0).line(-260,32,-260,32);geometry=HorizonGeometry.build(p,a->0,HorizonComposition.EMPTY,a->Double.NaN,s);check(geometry.treeCount>0&&geometry.treeCount<=384,"grass paints sparse shrub LOD");
        mask.clear();mask.stroke(BackgroundPaint.Brush.MOUNTAINS,100,.9,1,240,(x,y)->0).line(-4,32,-4,32);var field=new HorizonLandscape(p,HorizonComposition.EMPTY,a->Double.NaN,s);var at=HorizonGeometry.coordinates(p,-4,-32);var surface=new HorizonLandscape.Surface();field.sample(at.along(),at.distance(),-4,-32,surface);check(surface.height<1,"extreme painted mountain cannot form a wall at native map boundary");
        var repeat=HorizonGeometry.build(p,a->0,HorizonComposition.EMPTY,a->Double.NaN,s);for(int i=0;i<repeat.vertices.length;i++)check(Float.isFinite(repeat.vertices[i]),"extreme safe finite geometry");
        System.out.println("BackgroundDetailGeometryTest: "+checks+" industrial/shrub/height-limit/finite geometry checks PASS");
    }
}
