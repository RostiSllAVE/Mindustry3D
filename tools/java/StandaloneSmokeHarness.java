package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.Gl;import arc.graphics.g3d.Camera3D;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.world.*;import mindustry.gen.*;import mindustry.game.*;import mindustry.core.GameState.State;

/** Disposable native host/API/graphics validation. It does not synthesize physical keyboard input. */
final class SmokeHarness {
    final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();int frame;boolean started,done;
    String input,motion,gpu;
    SmokeHarness(FpvMod mod){this.mod=mod;}void update(){}
    void start(){try{
        if(!Vars.dataDirectory.absolutePath().startsWith(out.absolutePath()))throw new IllegalStateException("Disposable profile required");
        if(Vars.steam)throw new AssertionError("Standalone must not initialize as a Steam host");
        Core.settings.put("fpscap",120);Core.settings.put("sfxvol",0);Core.settings.put("musicvol",0);
        Vars.logic.reset();Vars.state.rules.waves=false;Vars.state.rules.canGameOver=false;Vars.state.rules.fog=false;
        Vars.world.loadGenerator(96,96,tiles->{for(int y=0;y<96;y++)for(int x=0;x<96;x++)tiles.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));tiles.get(8,8).setBlock(Blocks.coreShard,Team.sharded,0);tiles.get(24,24).setBlock(Blocks.groundFactory,Team.sharded,0);tiles.get(32,24).setBlock(Blocks.mechanicalDrill,Team.sharded,0);tiles.get(40,24).setBlock(Blocks.hyperProcessor,Team.sharded,0);});
        Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","Standalone disposable runtime check"));Vars.player.team(Team.sharded);
        var unit=UnitTypes.gamma.create(Team.sharded);unit.set(32*8,32*8);unit.add();Vars.player.unit(unit);Vars.logic.play();Vars.state.set(State.paused);
        var flag=FpvMod.class.getDeclaredField("autoEnable");flag.setAccessible(true);flag.set(mod,false);
        mod.setEnabled(true);if(!mod.enabled||mod.input==null)throw new AssertionError("FPV initialization failed");
        input=fpv.input.InputSmokeChecks.run(mod.input);motion=fpv.flight.UnitMotionSmokeChecks.run();
        mod.setBuildMode(true);if(!mod.buildMode)throw new AssertionError("Build mode did not enter");mod.setBuildMode(false);
        started=true;
    }catch(Throwable t){fail(t.toString());Log.err(t);}}
    void afterDraw(){if(!started||done)return;try{
        if(frame==0){gpu=fpv.render.LogicDisplayGpuChecks.run(out)+"; "+fpv.render.LayerDisplayVisualChecks.run();}
        mod.enabled=false;camera.resize(960,600);camera.near=.04f;camera.far=300;camera.up.set(0,1,0);
        if(frame<30){camera.position.set(34,12,-6);camera.lookAt(32,1,-24);}else if(frame<60){camera.position.set(34,30,-24);camera.up.set(0,0,-1);camera.lookAt(34,0,-24);}else{camera.position.set(27,1,-18);camera.lookAt(24,1,-24);}
        camera.update();mod.renderer3d.render(camera);if(Gl.getError()!=0)throw new AssertionError("OpenGL error");
        if(frame==20||frame==50||frame==80)ScreenUtils.saveScreenshot(out.child("standalone-scene-"+frame+".png"));
        frame++;if(frame==90){done=true;var result=arc.util.serialization.Jval.newObject();result.add("status","PASS");result.add("java",System.getProperty("java.version"));result.add("frames",arc.util.serialization.Jval.valueOf(frame));result.add("viewportWidth",arc.util.serialization.Jval.valueOf(Core.graphics.getWidth()));result.add("viewportHeight",arc.util.serialization.Jval.valueOf(Core.graphics.getHeight()));result.add("glErrors",arc.util.serialization.Jval.valueOf(0));result.add("nativeInputApi",input);result.add("nativeMotionApi",motion);result.add("gpu",gpu);result.add("fpvModeApi",arc.util.serialization.Jval.valueOf(true));result.add("buildModeApi",arc.util.serialization.Jval.valueOf(true));result.add("physicalKeyboardAcceptance",arc.util.serialization.Jval.valueOf(false));result.add("steamHost",arc.util.serialization.Jval.valueOf(Vars.steam));result.add("dataDirectory",Vars.dataDirectory.absolutePath());out.child("result.json").writeString(result.toString(arc.util.serialization.Jval.Jformat.formatted));Core.app.post(Core.app::exit);}
    }catch(Throwable t){fail(t.toString());Log.err(t);}}
    void fail(String text){done=true;out.child("failed.txt").writeString(text);Core.app.post(Core.app::exit);}
}
