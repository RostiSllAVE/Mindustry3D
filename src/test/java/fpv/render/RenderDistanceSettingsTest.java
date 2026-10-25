package fpv.render;
import arc.Core;import arc.mock.MockSettings;
public final class RenderDistanceSettingsTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args){Core.settings=new MockSettings();
        check(RenderDistanceSettings.world()==8192&&RenderDistanceSettings.detail()==80,"unchanged generous defaults");
        int original=ManufacturingModelProfile.lod(30*30);Core.settings.put(RenderDistanceSettings.DETAIL,20);
        check(ManufacturingModelProfile.lod(900)>original&&RenderDistanceSettings.world()==8192,"detail independent of world range");
        Core.settings.put(RenderDistanceSettings.DETAIL,240);check(ManufacturingModelProfile.lod(900)<original,"higher model detail distance");
        Core.settings.put(RenderDistanceSettings.WORLD,64);check(RenderDistanceSettings.far(2000)==64&&RenderDistanceSettings.detail()==240,"world independent of detail");
        Core.settings.put(RenderDistanceSettings.WORLD,-99);Core.settings.put(RenderDistanceSettings.DETAIL,Integer.MAX_VALUE);
        check(RenderDistanceSettings.world()==64&&RenderDistanceSettings.detail()==240,"safe bounds even for edited settings");
        System.out.println("RenderDistanceSettings: PASS defaults, independent persisted keys, model LOD and clamps");
    }
}
