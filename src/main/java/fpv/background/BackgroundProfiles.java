package fpv.background;

import arc.Core;
import fpv.render.BorderBiomeProfile;
import mindustry.Vars;

/** One loaded map profile and one staged preview; cache hits never scan maps or touch disk. */
public final class BackgroundProfiles {
    public static final String PREFIX="fpv-background-";
    private static BackgroundStyle current,preview;
    private static final String[] SIDE_KEYS={PREFIX+"south",PREFIX+"east",PREFIX+"north",PREFIX+"west"};
    private static final BorderBiomeProfile.Side[] SIDES=BorderBiomeProfile.Side.values();
    private static Object world,map,sector,directory;
    private static boolean game;
    private static String key="",status="";
    private static BackgroundProfileStore store;
    private static long loaded=Long.MIN_VALUE,revision,context;
    private BackgroundProfiles(){}
    private static void ensure(){
        boolean inGame=Vars.world!=null&&Vars.state!=null&&Vars.state.isGame()&&Vars.world.width()>0&&Vars.world.height()>0;
        Object tiles=inGame?Vars.world.tiles:null,scene=inGame?Vars.state.map:null,area=inGame?Vars.state.getSector():null,dir=Vars.dataDirectory;
        if(current==null||tiles!=world||scene!=map||area!=sector||dir!=directory||game!=inGame){
            world=tiles;map=scene;sector=area;directory=dir;game=inGame;preview=null;context++;revision++;status="";
            store=inGame&&Vars.dataDirectory!=null?new BackgroundProfileStore(Vars.dataDirectory.child("mindustry3-backgrounds").file().toPath()):null;
            key=inGame?BackgroundMapKey.current():"";
            if(store!=null){var saved=store.load(key,Vars.world.width(),Vars.world.height());current=saved.style();status=saved.warning();}
            else current=inGame?new BackgroundStyle():defaults();loaded=preferenceKey();
        }else if(!game&&loaded!=preferenceKey()){current=defaults();loaded=preferenceKey();revision++;}
    }
    private static BackgroundStyle defaults(){
        var style=new BackgroundStyle();int mode=Core.settings.getInt(PREFIX+"mode",0);style.mode=mode>=0&&mode<BackgroundStyle.Mode.values().length?BackgroundStyle.Mode.values()[mode]:BackgroundStyle.Mode.AUTO;
        if(style.mode==BackgroundStyle.Mode.CUSTOM)style.mode=BackgroundStyle.Mode.AUTO;
        style.whole=BackgroundPreset.safe(Core.settings.getInt(PREFIX+"whole",0));style.customBaseManual=Core.settings.getBool(PREFIX+"custom-base",false);
        for(var side:SIDES){int value=Core.settings.getInt(SIDE_KEYS[side.ordinal()],-1);if(value>=0&&value<BackgroundPreset.values().length)style.sides.put(side,BackgroundPreset.safe(value));}return style;
    }
    public static BackgroundStyle current(){ensure();return preview!=null?preview:current;}
    public static long revision(){ensure();return revision;}
    public static long context(){ensure();return context;}
    public static String mapKey(){ensure();return key;}
    public static String status(){ensure();return status;}
    private static long preferenceKey(){long value=Core.settings.getInt(PREFIX+"mode",0)*31L+Core.settings.getInt(PREFIX+"whole",0);for(String name:SIDE_KEYS)value=value*31+Core.settings.getInt(name,-1);return value;}
    public static void preview(BackgroundStyle style){ensure();preview=style.copy();Core.settings.put(fpv.render.AutomaticHorizon.SETTING,true);revision++;}
    public static void revertPreview(){ensure();if(preview!=null){preview=null;revision++;}}
    public static void revertPreview(long owner){ensure();if(owner==context)revertPreview();}
    public static void apply(BackgroundStyle style){
        ensure();preview=null;current=style.copy();revision++;Core.settings.put(fpv.render.AutomaticHorizon.SETTING,true);
        if(game){
            if(store==null){status="Applied for this session; no local profile directory is available.";return;}
            try{store.save(key,Vars.world.width(),Vars.world.height(),current);status="Saved locally for this map.";}
            catch(java.io.IOException|RuntimeException failed){status="Applied for this session only: local background could not be saved (check disk space/profile limit).";}
        }else{
            Core.settings.put(PREFIX+"mode",style.mode.ordinal());Core.settings.put(PREFIX+"whole",style.whole.ordinal());Core.settings.put(PREFIX+"custom-base",style.customBaseManual);
            for(var side:SIDES)Core.settings.put(SIDE_KEYS[side.ordinal()],style.sides.containsKey(side)?style.sides.get(side).ordinal():-1);loaded=preferenceKey();status="Choose a background inside a map to save it for that map.";
        }
    }
    /** Explicit cold reload for local profile tools/tests; not used per-frame or per stroke. */
    public static void reload(){current=null;preview=null;ensure();}
}
