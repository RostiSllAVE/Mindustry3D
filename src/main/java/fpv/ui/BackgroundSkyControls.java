package fpv.ui;
import fpv.background.BackgroundStyle;
import mindustry.ui.dialogs.BaseDialog;

/** Local per-map visual controls; the caller decides whether changes are staged or applied. */
public final class BackgroundSkyControls {
    private BackgroundSkyControls(){}
    public static void open(BackgroundStyle style,Runnable changed){
        var d=new BaseDialog("Sky / map appearance");d.cont.defaults().pad(5).width(440);
        d.cont.add("Visual only. Editor changes use its Preview / Apply buttons.").wrap().row();
        d.cont.check("Show planet",style.showPlanet,v->{style.showPlanet=v;changed.run();}).get().name="fpv-sky-planet";d.cont.row();
        d.cont.check("Show sun",style.showSun,v->{style.showSun=v;changed.run();}).get().name="fpv-sky-sun";d.cont.row();
        d.cont.check("Animate day / night",style.cycleTime,v->{style.cycleTime=v;changed.run();}).get().name="fpv-sky-cycle";d.cont.row();
        d.cont.check("Full space environment",style.fullSpace,v->{style.fullSpace=v;changed.run();}).get().name="fpv-sky-space";d.cont.row();
        d.cont.check("Show map boundary",style.showMapBoundary,v->{style.showMapBoundary=v;changed.run();}).get().name="fpv-sky-border";d.cont.row();
        d.cont.label(()->"Fixed time: "+Math.round(style.fixedPhase*24)+":00 (disable day/night animation)").row();
        d.cont.slider(0,100,1,style.fixedPhase*100,v->{style.fixedPhase=v/100;changed.run();}).disabled(s->style.cycleTime).get().name="fpv-sky-time";d.cont.row();
        d.cont.add("Full space hides the atmosphere and landscape, retaining your paint for later. Planet and sun can be toggled separately. No simulation or server changes.").wrap().fontScale(.8f).row();
        d.addCloseButton();d.show();
    }
}
