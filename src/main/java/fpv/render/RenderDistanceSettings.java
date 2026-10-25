package fpv.render;

import arc.Core;

/** Local presentation limits only. Never supplied to aim, placement, physics or networking. */
public final class RenderDistanceSettings {
    public static final String WORLD="fpv-world-draw-distance",DETAIL="fpv-detail-distance";
    public static final int DEFAULT_WORLD=8192,DEFAULT_DETAIL=80;
    private RenderDistanceSettings(){}
    public static int world(){return bounded(WORLD,DEFAULT_WORLD,64,16384);}
    public static int detail(){return bounded(DETAIL,DEFAULT_DETAIL,20,240);}
    private static int bounded(String key,int fallback,int min,int max){return Math.max(min,Math.min(max,Core.settings==null?fallback:Core.settings.getInt(key,fallback)));}
    public static float normalizedSquared(float distanceSquared){float scale=DEFAULT_DETAIL/(float)detail();return distanceSquared*scale*scale;}
    public static float far(float original){return Math.min(original,world());}
}
