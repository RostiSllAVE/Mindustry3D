package fpv.background;

import fpv.render.BorderBiomeProfile;
import java.util.*;

/** Client-local art direction. Never serialized into a world or a multiplayer packet. */
public final class BackgroundStyle {
    public enum Mode { AUTO, MANUAL }
    public Mode mode=Mode.AUTO;
    public BackgroundPreset whole=BackgroundPreset.OCEAN;
    public final EnumMap<BorderBiomeProfile.Side,BackgroundPreset> sides=new EnumMap<>(BorderBiomeProfile.Side.class);
    public BackgroundStyle copy(){var out=new BackgroundStyle();out.mode=mode;out.whole=whole;out.sides.putAll(sides);return out;}
    public BackgroundPreset side(BorderBiomeProfile.Side side){return sides.getOrDefault(side,whole);}
    public boolean all(BackgroundPreset preset){if(mode==Mode.AUTO)return false;for(var side:BorderBiomeProfile.Side.values())if(side(side)!=preset)return false;return true;}
    public long fingerprint(){long h=mode.ordinal()*31L+whole.ordinal();for(var side:BorderBiomeProfile.Side.values())h=h*31+(sides.containsKey(side)?sides.get(side).ordinal()+1:0);return h;}
    public BorderBiomeProfile profile(BorderBiomeProfile original){
        if(mode==Mode.AUTO)return original;
        var overrides=new EnumMap<BorderBiomeProfile.Side,BorderBiomeProfile.Cell>(BorderBiomeProfile.Side.class);
        for(var side:BorderBiomeProfile.Side.values())overrides.put(side,side(side).cell());
        return original.withSides(overrides);
    }
}
