package fpv.render;

import arc.Core;

/** Local graphics preference. Never read by terrain collision, save or network code. */
public enum MountainStyle {
    SMOOTH("Smooth"), BLOCKY("Blocky"), HYBRID("Hybrid");
    public static final String SETTING="fpv-mountain-geometry-style";
    private static final MountainStyle[] ALL=values();
    private final String label;
    MountainStyle(String label){this.label=label;}
    public String label(){return label;}
    public static MountainStyle of(int id){return id>=0&&id<ALL.length?ALL[id]:SMOOTH;}
    public static MountainStyle current(){
        try{return Core.settings==null?SMOOTH:of(Core.settings.getInt(SETTING,0));}
        catch(RuntimeException invalidPreference){return SMOOTH;}
    }
}
