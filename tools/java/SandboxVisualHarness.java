package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.*;import arc.graphics.g3d.Camera3D;import arc.util.*;
import fpv.construction.*;import fpv.render.*;import fpv.profile.VisualGpuTimer;import mindustry.*;import mindustry.content.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.game.*;import mindustry.core.GameState.State;import mindustry.world.blocks.logic.*;

/** Actual standalone renderer and native state. Only disposable profiles; no physical-input claim. */
final class SmokeHarness {
    final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();final VisualGpuTimer gpu=new VisualGpuTimer();
    final boolean benchmark=Boolean.getBoolean("fpv.utilityBenchmark");final int duration=benchmark?180:45,warmup=benchmark?60:15;
    final Block[] types={Blocks.itemSource,Blocks.itemVoid,Blocks.liquidSource,Blocks.liquidVoid,Blocks.powerSource,Blocks.powerVoid,Blocks.payloadSource,Blocks.payloadVoid,Blocks.repairPoint,Blocks.repairTurret,Blocks.payloadConveyor,Blocks.payloadRouter,Blocks.tileLogicDisplay,Blocks.targetDummy};
    final String[] scenes=benchmark?new String[]{"top","oblique","dense","dense"}:new String[]{"top","oblique","low","item-source","item-void","liquid-source","liquid-void","power-source","power-void","payload-source","payload-void","repair-point","repair-turret","payload-conveyor","payload-router","tiled-display","dummy","upper1","upper3","upper5","world64","world8192","detail20","detail240","dense"};
    final java.util.ArrayList<Building> machines=new java.util.ArrayList<>();FpvWorldRenderer draw;int frame;boolean started,done;long nanos;int measured;String support,input,motion;StringBuilder rows=new StringBuilder();
    SmokeHarness(FpvMod m){mod=m;}void update(){}
    void start(){try{
        if(!Vars.dataDirectory.absolutePath().startsWith(out.absolutePath())||Vars.steam)throw new AssertionError("Isolated standalone host required");
        Core.settings.put("fpscap",120);Core.settings.put("sfxvol",0);Core.settings.put("musicvol",0);Vars.logic.reset();
        Vars.world.loadGenerator(160,160,tiles->{for(int y=0;y<160;y++)for(int x=0;x<160;x++)tiles.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));tiles.get(8,8).setBlock(Blocks.coreShard,Team.sharded,0);});
        Vars.player.team(Team.sharded);var u=UnitTypes.gamma.create(Team.sharded);u.set(64*8,64*8);u.add();Vars.player.unit(u);Vars.logic.play();Vars.state.set(State.paused);
        Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","A2-16o standalone utilities"));Vars.state.rules.editor=true;Vars.state.rules.infiniteResources=true;Vars.state.rules.instantBuild=true;Vars.state.rules.waves=false;Vars.state.rules.canGameOver=false;Vars.state.rules.fog=false;Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
        var f=FpvMod.class.getDeclaredField("autoEnable");f.setAccessible(true);f.set(mod,false);mod.setEnabled(true);input=fpv.input.InputSmokeChecks.run(mod.input);motion=fpv.flight.UnitMotionSmokeChecks.run();mod.setBuildMode(true);mod.setBuildMode(false);mod.setEnabled(false);
        draw=new FpvWorldRenderer();draw.setUnitAltitudes(mod.unitAltitudes);draw.setConstruction(mod.construction);
        support="NOT RUN in baseline"; if(Boolean.getBoolean("fpv.utilityGpu"))support=(String)Class.forName("fpv.render.SandboxGpuChecks").getMethod("run",FpvWorldRenderer.class,fpv.units.UnitAltitudeSystem.class,Fi.class).invoke(null,draw,mod.unitAltitudes,out);
        for(int i=0;i<types.length;i++){Vars.world.tile(x(i),y(i)).setBlock(types[i],Team.sharded,0);var b=Vars.world.build(x(i),y(i));machines.add(b);configure(b);}
        for(int floor:new int[]{1,3,5})for(int i=0;i<types.length;i++){
            int x=x(i),y=y(i)+floor*16+32;var block=types[i];int low=-(block.size-1)/2;
            for(int dy=0;dy<block.size;dy++)for(int dx=0;dx<block.size;dx++)approved(LayerActions.Action.place(x+low+dx,y+low+dy,floor,Blocks.air.name,true,0,LayerComponent.Mode.FREE));
            var b=mod.construction.building(approved(LayerActions.Action.place(x,y,floor,block.name,false,0,LayerComponent.Mode.FREE)));if(b==null)throw new AssertionError("Missing upper "+block.name);configure(b);machines.add(b);
        }
        for(int i=0;i<100;i++){Vars.world.tile(15+i%10*5,142+i/10).setBlock(Blocks.itemSource,Team.sharded,0);}
        started=true;
    }catch(Throwable t){fail(t);}}
    int x(int i){return 20+i%7*14;}int y(int i){return 24+i/7*16;}
    long approved(LayerActions.Action a){var r=mod.construction.apply(Vars.player,a);if(!r.accepted())throw new AssertionError("Fixture placement "+a.blockName()+": "+r.reason());return r.componentId();}
    void configure(Building b){
        if(b instanceof mindustry.world.blocks.logic.TileableLogicDisplay.TileableLogicDisplayBuild tiled)tiled.updateTile();
        if(b instanceof mindustry.world.blocks.sandbox.ItemSource.ItemSourceBuild s)s.outputItem=Items.copper;
        if(b instanceof mindustry.world.blocks.sandbox.LiquidSource.LiquidSourceBuild s)s.source=Liquids.cryofluid;
        if(b instanceof LogicDisplay.LogicDisplayBuild d){var n=32-2*((mindustry.world.blocks.logic.TileableLogicDisplay)b.block).frameSize;int half=n/2;d.commands.addLast(DisplayCmd.get(LogicDisplay.commandClear,15,15,25,0,0,0));d.commands.addLast(DisplayCmd.get(LogicDisplay.commandColor,240,35,45,255,0,0));d.commands.addLast(DisplayCmd.get(LogicDisplay.commandRect,0,0,half,half,0,0));d.commands.addLast(DisplayCmd.get(LogicDisplay.commandColor,30,225,70,255,0,0));d.commands.addLast(DisplayCmd.get(LogicDisplay.commandRect,half,half,half,half,0,0));}
        if(b instanceof mindustry.world.blocks.payloads.PayloadConveyor.PayloadConveyorBuild p)p.handlePayload(p,new mindustry.world.blocks.payloads.UnitPayload(UnitTypes.dagger.create(b.team)));
    }
    void afterDraw(){if(!started||done)return;try{
        int stage=frame/duration,tick=frame%duration;String name=scenes[stage];
        Core.settings.put("fpv-world-draw-distance",name.equals("world64")?64:8192);Core.settings.put("fpv-detail-distance",name.equals("detail20")?20:name.equals("detail240")?240:80);
        camera.resize(Core.graphics.getWidth(),Core.graphics.getHeight());camera.near=.04f;camera.far=2000;camera.up.set(0,1,0);camera.fov=55;
        float cx=62,cz=-32,cy=.7f;
        if(!benchmark&&stage>=3&&stage<17){int i=stage-3;cx=x(i);cz=-y(i);camera.position.set(cx+types[i].size*1.3f+2,cy+types[i].size*.9f+2,cz+types[i].size*1.9f+3);}
        else if(name.startsWith("upper")){int floor=Integer.parseInt(name.substring(5));cz-=floor*16+32;cy=(float)mod.construction.planeHeight(floor);camera.position.set(cx+42,cy+22,cz+34);}
        else if(name.equals("top")){camera.position.set(cx,68,cz);camera.up.set(0,0,-1);}
        else if(name.equals("low"))camera.position.set(62,1.5f,-9);
        else if(name.equals("dense")){cz=-146;camera.position.set(35,12,-125);}
        else camera.position.set(104,24,8);
        camera.lookAt(cx,cy,cz);camera.update();float far=camera.far;
        if(tick==warmup)gpu.reset();if(tick>=warmup)gpu.begin();long t=System.nanoTime();draw.render(camera);long elapsed=System.nanoTime()-t;if(tick>=warmup){gpu.end();nanos+=elapsed;measured++;}
        if(camera.far!=far)throw new AssertionError("Rendering changed picking camera range");
        if(Gl.getError()!=0)throw new AssertionError("OpenGL error in "+name);
        if(tick==30)ScreenUtils.saveScreenshot(out.child(name+".png"));
        if(tick==duration-1){gpu.poll();rows.append(name+","+nanos/1e6/measured+","+gpu.meanMs()+","+gpu.samples()+"\n");nanos=0;measured=0;}
        frame++;if(frame==scenes.length*duration){done=true;out.child("render-times.csv").writeString("scene,cpuMs,gpuMs,gpuSamples\n"+rows);out.child("result.json").writeString("{\"status\":\"PASS\",\"java\":\""+System.getProperty("java.version")+"\",\"steamHost\":false,\"scenes\":"+scenes.length+",\"frames\":"+frame+",\"glErrors\":0,\"nativeSupport\":\""+support.replace('"','\'').replace('\n',' ')+"\",\"physicalAcceptance\":false}");out.child("input-motion.txt").writeString(input+"\n"+motion);draw.dispose();gpu.close();Core.app.post(Core.app::exit);}
    }catch(Throwable t){fail(t);}}
    void fail(Throwable t){done=true;out.child("failed.txt").writeString(t.toString());Log.err(t);Core.app.post(Core.app::exit);}
}
