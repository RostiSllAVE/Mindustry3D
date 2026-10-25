package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.Gl;import arc.graphics.g3d.Camera3D;import arc.util.*;
import fpv.render.*;import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.game.Team;import mindustry.gen.*;import mindustry.world.Tile;

/** Reproducible seven-scene release comparison. Only a disposable fixture overlay. */
final class SmokeHarness {
    final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();
    final fpv.profile.VisualGpuTimer gpu=new fpv.profile.VisualGpuTimer();
    final String[] names={"empty","industrial","dense-factory","mountains","flying-256","multi-floor","power-liquid"};
    final int warmup=120,samples=600,duration=warmup+samples;
    final StringBuilder csv=new StringBuilder("scenario,renderMeanMs,renderP95Ms,simulationMeanMs,loopMeanMs,loopFps,heapUsedBytes,renderAllocatedBytesPerFrame,worldLoadMs,gpuMeanMs,gpuSamples,cachedChunkBytes\n");
    final java.util.ArrayList<Double> render=new java.util.ArrayList<>();
    FpvWorldRenderer draw;boolean started,done;int frame,active=-1;long loadNanos,renderNanos,simNanos,loopNanos,lastFrame,allocTotal,allocSamples;
    Object bean;java.lang.reflect.Method allocated;java.lang.reflect.Field auto;String input,motion;
    SmokeHarness(FpvMod m){mod=m;}
    void update(){if(started&&!done)try{auto.set(mod,false);if(mod.enabled)mod.setEnabled(false);}catch(Throwable t){fail(t);}}
    void start(){try{
        if(Vars.steam||!Vars.dataDirectory.absolutePath().startsWith(out.absolutePath()))throw new AssertionError("Isolated standalone required");
        Core.settings.put("fpscap",120);Core.settings.put("fpv-auto-horizon",false);Core.settings.put("sfxvol",0);Core.settings.put("musicvol",0);
        auto=FpvMod.class.getDeclaredField("autoEnable");auto.setAccessible(true);auto.set(mod,false);mod.setEnabled(false);
        try{bean=Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null);var type=Class.forName("com.sun.management.ThreadMXBean");allocated=type.getMethod("getThreadAllocatedBytes",long.class);}catch(Throwable unavailable){bean=null;}
        started=true;
    }catch(Throwable t){fail(t);}}
    long bytes(){try{return bean==null?-1:((Number)allocated.invoke(bean,Thread.currentThread().getId())).longValue();}catch(Throwable absent){return -1;}}
    void scene(int kind){
        long start=System.nanoTime();if(draw!=null)draw.dispose();Vars.logic.reset();active=kind;
        Vars.world.loadGenerator(128,128,tiles->{
            for(int y=0;y<128;y++)for(int x=0;x<128;x++)tiles.set(x,y,new Tile(x,y,kind==6&&x>80?Blocks.water:Blocks.stone,Blocks.air,kind==3&&x>24&&y>32&&y<112?Blocks.stoneWall:Blocks.air));
            tiles.get(8,8).setBlock(Blocks.coreShard,Team.sharded,0);
            if(kind==1||kind==2){int step=kind==1?10:5;var blocks=new mindustry.world.Block[]{Blocks.graphitePress,Blocks.siliconSmelter,Blocks.multiPress,Blocks.mechanicalDrill,Blocks.cryofluidMixer,Blocks.groundFactory};for(int y=26;y<105;y+=step)for(int x=26;x<105;x+=step)tiles.get(x,y).setBlock(blocks[(x+y)%blocks.length],Team.sharded,0);}
            if(kind==6){for(int y=26;y<105;y+=4)for(int x=26;x<105;x+=4)tiles.get(x,y).setBlock(x>80?Blocks.mechanicalPump:((x+y)%8==0?Blocks.powerNode:Blocks.battery),Team.sharded,0);for(int y=27;y<104;y+=4)for(int x=26;x<105;x++)tiles.get(x,y).setBlock(Blocks.liquidRouter,Team.sharded,0);}
        });
        Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","Release perf "+names[kind]));Vars.player.team(Team.sharded);Vars.state.rules.unitCap=2000;Vars.state.rules.fog=false;Vars.state.rules.waves=false;Vars.state.rules.canGameOver=false;
        var pilot=UnitTypes.gamma.create(Team.sharded);pilot.set(8*8,8*8);pilot.add();Vars.player.unit(pilot);Vars.logic.play();Vars.state.set(State.paused);
        Vars.state.rules.editor=true;Vars.state.rules.infiniteResources=true;Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
        if(kind==4)for(int y=0;y<16;y++)for(int x=0;x<16;x++){var u=UnitTypes.flare.create(Team.sharded);u.set((28+x*4)*8,(28+y*4)*8);u.add();}
        if(kind==5)for(int z=1;z<=5;z++)for(int y=42;y<=58;y+=4)for(int x=42;x<=58;x+=4){var slab=mod.construction.apply(Vars.player,LayerActions.Action.place(x,y,z,"air",true,0,LayerComponent.Mode.FREE));if(!slab.accepted())throw new AssertionError(slab.reason());var b=mod.construction.apply(Vars.player,LayerActions.Action.place(x,y,z,"copper-wall",false,0,LayerComponent.Mode.FREE));if(!b.accepted())throw new AssertionError(b.reason());}
        try{auto.set(mod,false);}catch(IllegalAccessException impossible){throw new AssertionError(impossible);}mod.setEnabled(false);
        draw=new FpvWorldRenderer();draw.setUnitAltitudes(mod.unitAltitudes);draw.setConstruction(mod.construction);draw.setCombat(mod.combat);
        camera.resize(Core.graphics.getWidth(),Core.graphics.getHeight());camera.near=.04f;camera.far=650;camera.fov=65;camera.up.set(0,1,0);camera.position.set(72,kind==3?45:kind==5?35:32,-6);camera.lookAt(64,kind==3?7:4,-65);camera.update();
        loadNanos=System.nanoTime()-start;render.clear();renderNanos=simNanos=loopNanos=allocTotal=allocSamples=0;lastFrame=0;gpu.reset();
    }
    void afterDraw(){if(!started||done)return;try{
        int kind=frame/duration,tick=frame%duration;if(active!=kind)scene(kind);
        long now=System.nanoTime();if(tick>warmup)loopNanos+=now-lastFrame;lastFrame=now;
        long before=bytes(),start=System.nanoTime();if(tick>=warmup)gpu.begin();draw.render(camera);if(tick>=warmup)gpu.end();long elapsed=System.nanoTime()-start;
        if(tick>=warmup){renderNanos+=elapsed;render.add(elapsed/1e6);long after=bytes();if(before>=0&&after>=before){allocTotal+=after-before;allocSamples++;}}
        start=System.nanoTime();Vars.state.set(State.playing);Vars.logic.update();Vars.state.set(State.paused);if(tick>=warmup)simNanos+=System.nanoTime()-start;
        if(Gl.getError()!=0)throw new AssertionError("OpenGL error in "+names[kind]);
        if(tick==40)ScreenUtils.saveScreenshot(out.child(names[kind]+".png"));
        if(tick==duration-1){gpu.poll();java.util.Collections.sort(render);double loop=loopNanos/((samples-1)*1e6);csv.append(String.format(java.util.Locale.ROOT,"%s,%.6f,%.6f,%.6f,%.6f,%.3f,%d,%.3f,%.3f,%.6f,%d,%d%n",names[kind],renderNanos/(samples*1e6),render.get((int)(samples*.95)),simNanos/(samples*1e6),loop,1000/loop,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(),allocSamples==0?-1:allocTotal/(double)allocSamples,loadNanos/1e6,gpu.meanMs(),gpu.samples(),draw.stats().cachedBytes()));out.child("measurements.csv").writeString(csv.toString());}
        if(++frame==names.length*duration){done=true;draw.dispose();gpu.close();out.child("result.json").writeString("{\"status\":\"PASS\",\"scenarios\":7,\"frames\":"+frame+",\"warmupPerScene\":"+warmup+",\"samplesPerScene\":"+samples+",\"glErrors\":0,\"physicalGameplay\":false}");Core.app.post(Core.app::exit);}
    }catch(Throwable t){fail(t);}}
    void fail(Throwable t){done=true;out.child("failed.txt").writeString(t.toString());Log.err(t);Core.app.post(Core.app::exit);}
}
