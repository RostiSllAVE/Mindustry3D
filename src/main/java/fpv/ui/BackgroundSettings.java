package fpv.ui;

import fpv.background.*;
import fpv.render.BorderBiomeProfile;
import mindustry.ui.dialogs.BaseDialog;
import java.util.function.Consumer;

/** Standard modal scene UI: ordinary FPV suspension releases input while it is open. */
public final class BackgroundSettings {
    private BackgroundSettings(){}
    public static void open(){
        var dialog=new BaseDialog("Background scenery");dialog.cont.defaults().pad(5).width(520);
        dialog.cont.add("Local visual scenery only. The playable map and server rules do not change.").wrap().row();
        dialog.cont.table(modes->{for(var mode:BackgroundStyle.Mode.values())modes.button(mode.name(),()->{var style=BackgroundProfiles.current().copy();style.mode=mode;BackgroundProfiles.apply(style);}).width(240).height(42);}).row();
        dialog.cont.label(()->"Background Mode: "+BackgroundProfiles.current().mode).row();
        dialog.cont.button("Whole-world preset",()->choose(false,p->{var style=BackgroundProfiles.current().copy();style.whole=p;BackgroundProfiles.apply(style);})).update(b->b.setText("Whole world: "+BackgroundProfiles.current().whole.label)).height(42).row();
        for(var side:BorderBiomeProfile.Side.values())dialog.cont.button(side.name(),()->choose(true,p->{var style=BackgroundProfiles.current().copy();if(p==null)style.sides.remove(side);else style.sides.put(side,p);BackgroundProfiles.apply(style);})).update(b->b.setText(side.name()+": "+(BackgroundProfiles.current().sides.containsKey(side)?BackgroundProfiles.current().side(side).label:"Inherit whole world"))).height(38).row();
        dialog.cont.add("Auto retains the border-derived generator. Manual uses the presets above.\nChanges preview immediately; only cached background geometry is rebuilt.").wrap().fontScale(.85f).row();
        dialog.addCloseButton();dialog.show();
    }
    private static void choose(boolean inherit,Consumer<BackgroundPreset> selected){
        var dialog=new BaseDialog("Choose a background biome");dialog.cont.defaults().size(210,48).pad(4);int i=0;
        if(inherit){dialog.cont.button("Inherit whole world",()->{selected.accept(null);dialog.hide();}).colspan(3).width(638).row();}
        for(var preset:BackgroundPreset.values()){dialog.cont.button(preset.label,()->{selected.accept(preset);dialog.hide();});if(++i%3==0)dialog.cont.row();}
        dialog.addCloseButton();dialog.show();
    }
}
