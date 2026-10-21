package fpv.render;
import fpv.background.*;
import java.util.*;
import static fpv.render.BorderBiomeProfile.Biome.*;

public final class BackgroundPresetTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        var p=BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){public int width(){return 64;}public int height(){return 48;}public BorderBiomeProfile.Cell cell(int x,int y){return new BorderBiomeProfile.Cell(DESERT,.7f,.6f,.4f,false,false,0,.1f,.3f,.1f,0);}},8);
        long before=p.fingerprint();var style=new BackgroundStyle();check(style.profile(p)==p,"Auto uses original generator object");
        style.mode=BackgroundStyle.Mode.MANUAL;
        for(var preset:BackgroundPreset.values()){
            style.whole=preset;var out=style.profile(p);check(p.fingerprint()==before,"presets never mutate automatic metadata");
            for(var side:out.sections())check(side.value().dominant()==preset.biome,"whole-world biome "+preset.label);
            var g=HorizonGeometry.build(out,s->0);check(g.vertices.length%(HorizonGeometry.STRIDE*3)==0,"valid cached geometry "+preset);
            if(preset==BackgroundPreset.SPACE||preset==BackgroundPreset.VOID){check(g.vertices.length>0,"empty preset has a cached cosmic mask");for(int i=0;i<g.vertices.length;i+=HorizonGeometry.STRIDE)check(g.vertices[i+6]>.995f&&g.vertices[i+7]==0,"mask has no ordinary landscape/water");}
        }
        style.whole=BackgroundPreset.DESERT;style.sides.put(BorderBiomeProfile.Side.NORTH,BackgroundPreset.OCEAN);style.sides.put(BorderBiomeProfile.Side.SOUTH,BackgroundPreset.FOREST);
        var out=style.profile(p);for(var section:out.sections())check(section.value().dominant()==switch(section.side()){case NORTH->OCEAN;case SOUTH->FOREST;default->DESERT;},"opposite sides remain independent");
        check(!style.all(BackgroundPreset.VOID),"mixed directions cannot force empty sky");
        var copy=style.copy();copy.sides.clear();check(style.sides.size()==2,"draft changes do not mutate applied side choices");
        style.mode=BackgroundStyle.Mode.AUTO;check(style.profile(p)==p&&p.fingerprint()==before,"Auto restoration exact");
        System.out.println("BackgroundPresetTest: "+checks+" automatic identity/manual palettes/directions/empty/copy checks PASS");
    }
}
