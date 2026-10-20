package fpv.input;
import arc.Core;import arc.graphics.*;import arc.graphics.g2d.*;import fpv.background.*;import fpv.render.*;import mindustry.*;import mindustry.content.*;import mindustry.world.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

public final class BackgroundPainterNativeTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static class Stub extends Texture{Stub(){super();width=height=64;}}
    static long hash(){long h=17;for(var tile:Vars.world.tiles)h=h*31+tile.floorID()*17L+tile.blockID()*71L+tile.overlayID();return h;}
    public static void main(String[] args)throws Exception{init();arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();try{
        var region=new TextureAtlas.AtlasRegion(new Stub(),0,0,16,16);Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return region;}};
        Vars.world.tiles=new Tiles(64,64);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(var t:Vars.world.tiles)t.setFloor(Blocks.sand.asFloor());Blocks.sand.region=region;Blocks.sand.variantRegions=new TextureRegion[]{region};
        var original=new BackgroundStyle();original.mode=BackgroundStyle.Mode.MANUAL;original.whole=BackgroundPreset.OCEAN;BackgroundProfiles.apply(original);var horizon=new AutomaticHorizon();horizon.ensure();long before=horizon.stats().fingerprint(),builds=horizon.stats().builds(),map=hash();int nativeUnits=mindustry.gen.Groups.unit.size(),nativeBuilds=mindustry.gen.Groups.build.size();
        var draft=BackgroundProfiles.current().copy();draft.customBaseManual=true;draft.mode=BackgroundStyle.Mode.CUSTOM;var paint=draft.paint(64,64);paint.line(32,150,32,150,65,.8,BackgroundPaint.Brush.ISLAND,14);paint.line(32,150,32,150,65,.8,BackgroundPaint.Brush.FOREST,0);
        for(int i=0;i<100;i++)horizon.ensure();check(horizon.stats().builds()==builds&&horizon.stats().fingerprint()==before,"staged brush movement never regenerates 3D geometry");
        BackgroundProfiles.preview(draft);horizon.ensure();check(horizon.stats().builds()==builds+1&&horizon.stats().fingerprint()!=before,"Preview applies real custom geometry once");
        long custom=horizon.stats().fingerprint();paint.clear();horizon.ensure();check(horizon.stats().fingerprint()==custom,"preview owns a stable snapshot independent of further strokes");
        BackgroundProfiles.revertPreview();horizon.ensure();check(horizon.stats().fingerprint()==before,"closing preview restores original committed background");
        draft.paint.line(32,150,32,150,65,.8,BackgroundPaint.Brush.MOUNTAINS,70);BackgroundProfiles.apply(draft);horizon.ensure();check(horizon.stats().fingerprint()!=before,"Apply commits actual custom terrain");
        check(hash()==map&&nativeUnits==mindustry.gen.Groups.unit.size()&&nativeBuilds==mindustry.gen.Groups.build.size(),"no native world/units/buildings affected by painting/apply/undo");
        BackgroundProfiles.apply(original);horizon.dispose();System.out.println("BackgroundPainterNativeTest: "+checks+" native staging/preview/isolation/apply/scope checks PASS; physical UI and multiplayer separate");
    }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}}
}
