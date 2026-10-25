package fpv.render;

import arc.Core;
import arc.mock.MockSettings;

public final class MountainStyleTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] args){
        Core.settings=new MockSettings();check(MountainStyle.current()==MountainStyle.SMOOTH,"safe compatibility default");
        for(var s:MountainStyle.values()){Core.settings.put(MountainStyle.SETTING,s.ordinal());check(MountainStyle.current()==s,"persistent local graphics preference");}
        for(int bad:new int[]{-1,3,Integer.MAX_VALUE}){Core.settings.put(MountainStyle.SETTING,bad);check(MountainStyle.current()==MountainStyle.SMOOTH,"invalid setting falls back to smooth");}
        check(RenderDistanceSettings.detail()==80&&RenderDistanceSettings.world()==8192,"independent draw/detail settings unchanged");
        System.out.println("MountainStyle: PASS defaults, options, persistence key, invalid fallback and independent distances");
    }
}
