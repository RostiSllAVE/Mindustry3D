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
        dialog.cont.table(modes->{for(var mode:BackgroundStyle.Mode.values())modes.button(mode.name(),()->{var style=BackgroundProfiles.current().copy();if(mode==BackgroundStyle.Mode.CUSTOM)style.customBaseManual=style.mode==BackgroundStyle.Mode.MANUAL||style.mode==BackgroundStyle.Mode.CUSTOM&&style.customBaseManual;style.mode=mode;BackgroundProfiles.apply(style);}).width(160).height(42);}).row();
        dialog.cont.label(()->"Background Mode: "+BackgroundProfiles.current().mode).row();
        dialog.cont.label(BackgroundProfiles::status).wrap().fontScale(.75f).row();
        dialog.cont.button("Whole-world preset",()->choose(false,p->{var style=BackgroundProfiles.current().copy();style.whole=p;BackgroundProfiles.apply(style);})).update(b->b.setText("Whole world: "+BackgroundProfiles.current().whole.label)).height(42).row();
        for(var side:BorderBiomeProfile.Side.values())dialog.cont.button(side.name(),()->choose(true,p->{var style=BackgroundProfiles.current().copy();if(p==null)style.sides.remove(side);else style.sides.put(side,p);BackgroundProfiles.apply(style);})).update(b->b.setText(side.name()+": "+(BackgroundProfiles.current().sides.containsKey(side)?BackgroundProfiles.current().side(side).label:"Inherit whole world"))).height(38).row();
        dialog.cont.button("Sky / map appearance",()->{var style=BackgroundProfiles.current().copy();BackgroundSkyControls.open(style,()->BackgroundProfiles.apply(style));}).height(40).row();
        dialog.cont.button("Open Background Editor",BackgroundEditor::open).height(44).disabled(b->!mindustry.Vars.state.isGame()).row();
        dialog.cont.add("Auto uses the map. Manual uses these presets. Custom paints over the mode\nyou entered from. Editor strokes are staged until Preview / Apply.").wrap().fontScale(.85f).row();
        dialog.addCloseButton();dialog.show();
    }
    private static void choose(boolean inherit,Consumer<BackgroundPreset> selected){
        var dialog=new BaseDialog("Choose a background biome");dialog.cont.defaults().size(210,48).pad(4);int i=0;
        if(inherit){dialog.cont.button("Inherit whole world",()->{selected.accept(null);dialog.hide();}).colspan(3).width(638).row();}
        for(var preset:BackgroundPreset.values()){dialog.cont.button(preset.label,()->{selected.accept(preset);dialog.hide();});if(++i%3==0)dialog.cont.row();}
        dialog.addCloseButton();dialog.show();
    }
}
