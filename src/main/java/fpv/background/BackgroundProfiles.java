package fpv.background;

import arc.Core;
import fpv.render.BorderBiomeProfile;

/** Cached presentation preferences. The renderer reads this without allocating or scanning maps. */
public final class BackgroundProfiles {
    public static final String PREFIX="fpv-background-";
    private static BackgroundStyle current,preview;
    private static final String[] SIDE_KEYS={PREFIX+"south",PREFIX+"east",PREFIX+"north",PREFIX+"west"};
    private static final BorderBiomeProfile.Side[] SIDES=BorderBiomeProfile.Side.values();
    private static long loaded=Long.MIN_VALUE,revision;
    private BackgroundProfiles(){}
    public static BackgroundStyle current(){
        if(preview!=null)return preview;
        long key=key();
        if(current==null||key!=loaded){var style=new BackgroundStyle();int mode=Core.settings.getInt(PREFIX+"mode",0);style.mode=mode>=0&&mode<BackgroundStyle.Mode.values().length?BackgroundStyle.Mode.values()[mode]:BackgroundStyle.Mode.AUTO;style.whole=BackgroundPreset.safe(Core.settings.getInt(PREFIX+"whole",0));style.customBaseManual=Core.settings.getBool(PREFIX+"custom-base",false);if(current!=null)style.paint=current.paint;
            for(var side:SIDES){int value=Core.settings.getInt(SIDE_KEYS[side.ordinal()],-1);if(value>=0&&value<BackgroundPreset.values().length)style.sides.put(side,BackgroundPreset.safe(value));}
            current=style;loaded=key;revision++;
        }return current;
    }
    public static long revision(){current();return revision;}
    private static long key(){long key=(Core.settings.getInt(PREFIX+"mode",0)*31L+Core.settings.getInt(PREFIX+"whole",0))*31+(Core.settings.getBool(PREFIX+"custom-base",false)?1:0);for(String name:SIDE_KEYS)key=key*31+Core.settings.getInt(name,-1);return key;}
    public static void preview(BackgroundStyle style){preview=style.copy();Core.settings.put(fpv.render.AutomaticHorizon.SETTING,true);revision++;}
    public static void revertPreview(){if(preview!=null){preview=null;revision++;}}
    public static void apply(BackgroundStyle style){
        Core.settings.put(PREFIX+"mode",style.mode.ordinal());Core.settings.put(PREFIX+"whole",style.whole.ordinal());
        Core.settings.put(PREFIX+"custom-base",style.customBaseManual);
        for(var side:SIDES)Core.settings.put(SIDE_KEYS[side.ordinal()],style.sides.containsKey(side)?style.sides.get(side).ordinal():-1);
        preview=null;current=style.copy();loaded=key();revision++;Core.settings.put(fpv.render.AutomaticHorizon.SETTING,true);
    }
}
