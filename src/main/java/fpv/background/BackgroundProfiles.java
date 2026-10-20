package fpv.background;

import arc.Core;
import fpv.render.BorderBiomeProfile;

/** Cached presentation preferences. The renderer reads this without allocating or scanning maps. */
public final class BackgroundProfiles {
    public static final String PREFIX="fpv-background-";
    private static BackgroundStyle current;
    private static long loaded=Long.MIN_VALUE,revision;
    private BackgroundProfiles(){}
    public static BackgroundStyle current(){
        long key=Core.settings.getInt(PREFIX+"mode",0)*31L+Core.settings.getInt(PREFIX+"whole",0);
        for(var side:BorderBiomeProfile.Side.values())key=key*31+Core.settings.getInt(PREFIX+side.name().toLowerCase(java.util.Locale.ROOT),-1);
        if(current==null||key!=loaded){var style=new BackgroundStyle();style.mode=Core.settings.getInt(PREFIX+"mode",0)==1?BackgroundStyle.Mode.MANUAL:BackgroundStyle.Mode.AUTO;style.whole=BackgroundPreset.safe(Core.settings.getInt(PREFIX+"whole",0));
            for(var side:BorderBiomeProfile.Side.values()){int value=Core.settings.getInt(PREFIX+side.name().toLowerCase(java.util.Locale.ROOT),-1);if(value>=0&&value<BackgroundPreset.values().length)style.sides.put(side,BackgroundPreset.safe(value));}
            current=style;loaded=key;revision++;
        }return current;
    }
    public static long revision(){current();return revision;}
    public static void apply(BackgroundStyle style){
        Core.settings.put(PREFIX+"mode",style.mode.ordinal());Core.settings.put(PREFIX+"whole",style.whole.ordinal());
        for(var side:BorderBiomeProfile.Side.values())Core.settings.put(PREFIX+side.name().toLowerCase(java.util.Locale.ROOT),style.sides.containsKey(side)?style.sides.get(side).ordinal():-1);
        Core.settings.put(fpv.render.AutomaticHorizon.SETTING,true);loaded=Long.MIN_VALUE;current();
    }
}
