package fpv.background;

import fpv.render.BorderBiomeProfile;
import java.util.*;

/** Client-local art direction. Never serialized into a world or a multiplayer packet. */
public final class BackgroundStyle {
    public enum Mode { AUTO, MANUAL, CUSTOM }
    private static final BorderBiomeProfile.Side[] SIDES=BorderBiomeProfile.Side.values();
    public Mode mode=Mode.AUTO;
    public BackgroundPreset whole=BackgroundPreset.OCEAN;
    public boolean customBaseManual;
    public BackgroundPaint paint;
    public final EnumMap<BorderBiomeProfile.Side,BackgroundPreset> sides=new EnumMap<>(BorderBiomeProfile.Side.class);
    public BackgroundStyle copy(){var out=new BackgroundStyle();out.mode=mode;out.whole=whole;out.customBaseManual=customBaseManual;out.paint=paint==null?null:paint.copy();out.sides.putAll(sides);return out;}
    public BackgroundPaint paint(int width,int height){if(paint==null||paint.width!=width||paint.height!=height)paint=new BackgroundPaint(width,height);return paint;}
    public BackgroundPreset side(BorderBiomeProfile.Side side){return sides.getOrDefault(side,whole);}
    public boolean all(BackgroundPreset preset){if(mode==Mode.AUTO||(mode==Mode.CUSTOM&&!customBaseManual))return false;for(var side:SIDES)if(side(side)!=preset)return false;return true;}
    public long fingerprint(){long h=(mode.ordinal()*31L+whole.ordinal())*31+(customBaseManual?1:0);for(var side:SIDES)h=h*31+(sides.containsKey(side)?sides.get(side).ordinal()+1:0);return paint==null?h:h*31+paint.fingerprint();}
    public BorderBiomeProfile profile(BorderBiomeProfile original){
        if(mode==Mode.AUTO||(mode==Mode.CUSTOM&&!customBaseManual))return original;
        var overrides=new EnumMap<BorderBiomeProfile.Side,BorderBiomeProfile.Cell>(BorderBiomeProfile.Side.class);
        for(var side:BorderBiomeProfile.Side.values())overrides.put(side,side(side).cell());
        return original.withSides(overrides);
    }
}
