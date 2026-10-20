package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;import fpv.background.*;import fpv.render.*;import mindustry.*;import mindustry.content.*;import mindustry.world.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

public final class BackgroundPresetNativeTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static class Stub extends Texture{Stub(){super();width=height=64;}}
    public static void main(String[] args)throws Exception{init();arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();
        try{var region=new TextureAtlas.AtlasRegion(new Stub(),0,0,16,16);Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return region;}};
            Vars.world.tiles=new Tiles(48,48);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(var t:Vars.world.tiles)t.setFloor(Blocks.sand.asFloor());Blocks.sand.region=region;Blocks.sand.variantRegions=new TextureRegion[]{region};
            var auto=new BackgroundStyle();BackgroundProfiles.apply(auto);var horizon=new AutomaticHorizon();horizon.ensure();long old=horizon.stats().fingerprint(),builds=horizon.stats().builds();
            for(int i=0;i<100;i++)horizon.ensure();check(horizon.stats().builds()==builds,"cached frames do not regenerate");
            var manual=auto.copy();manual.mode=BackgroundStyle.Mode.MANUAL;manual.whole=BackgroundPreset.OCEAN;BackgroundProfiles.apply(manual);horizon.ensure();check(horizon.stats().builds()==builds+1&&horizon.stats().fingerprint()!=old,"preset changes visible without restarting");
            for(var s:horizon.profile().sections())check(s.value().dominant()==BorderBiomeProfile.Biome.OCEAN,"manual sea overrides sand background only");
            check(Vars.world.tile(24,24).floor()==Blocks.sand,"native floor stays sand");
            BackgroundProfiles.apply(auto);horizon.ensure();check(horizon.stats().fingerprint()==old,"revert Auto exactly restores cached procedural appearance");
            Vars.state.set(mindustry.core.GameState.State.menu);Core.settings.put(BackgroundProfiles.PREFIX+"mode",999);Core.settings.put(BackgroundProfiles.PREFIX+"whole",999);Core.settings.put(BackgroundProfiles.PREFIX+"north",999);check(BackgroundProfiles.current().mode==BackgroundStyle.Mode.AUTO,"unknown default mode safely falls back to Auto");check(BackgroundProfiles.current().whole==BackgroundPreset.OCEAN&&!BackgroundProfiles.current().sides.containsKey(BorderBiomeProfile.Side.NORTH),"invalid default preset fallback");
            horizon.dispose();System.out.println("BackgroundPresetNativeTest: "+checks+" native cache/live settings/Auto restoration/scope checks PASS");
        }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
}
