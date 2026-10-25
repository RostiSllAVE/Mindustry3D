package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.Gl;import arc.graphics.g3d.Camera3D;import arc.util.*;
import fpv.render.*;import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.game.Team;import mindustry.gen.*;import mindustry.world.Tile;import mindustry.world.blocks.units.RepairTurret;

/** Real native HP changes followed by the full production world effect pass. */
final class SmokeHarness {
    final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();FpvWorldRenderer draw;boolean started,done;int frame;
    RepairTurret.RepairPointBuild point;Unit target;float hp;String state;
    SmokeHarness(FpvMod m){mod=m;}void update(){}
    void start(){try{
        if(!Vars.dataDirectory.absolutePath().startsWith(out.absolutePath())||Vars.steam)throw new AssertionError("isolated standalone");
        Core.settings.put("fpscap",120);Vars.logic.reset();Vars.world.loadGenerator(96,96,tiles->{for(int y=0;y<96;y++)for(int x=0;x<96;x++)tiles.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));tiles.get(8,8).setBlock(Blocks.coreShard,Team.sharded,0);tiles.get(40,40).setBlock(Blocks.repairTurret,Team.sharded,0);});
        Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","Repair beam diagnostic"));Vars.player.team(Team.sharded);var pilot=UnitTypes.gamma.create(Team.sharded);pilot.set(64,64);pilot.add();Vars.player.unit(pilot);Vars.logic.play();Vars.state.set(State.paused);Vars.state.rules.fog=false;Vars.state.rules.waves=false;Vars.state.rules.unitCap=1000;
        var auto=FpvMod.class.getDeclaredField("autoEnable");auto.setAccessible(true);auto.set(mod,false);mod.setEnabled(false);
        point=(RepairTurret.RepairPointBuild)Vars.world.build(40,40);target=UnitTypes.zenith.create(Team.sharded);target.set(point.x+32,point.y+8);target.add();target.health=50;mod.unitAltitudes.initializePayloadRelease(target,3);
        point.target=target;point.enabled=true;point.efficiency=point.potentialEfficiency=1;point.rotation=point.angleTo(target);point.timer.reset(((RepairTurret)point.block).timerTarget,0);hp=target.health;float delta=Time.delta;Time.delta=1;for(int i=0;i<12;i++)point.updateTile();Time.delta=delta;
        state="hp="+hp+"->"+target.health+" target="+(point.target==target)+" strength="+point.strength+" efficiency="+point.efficiency+" enabled="+point.enabled+" pose="+mod.unitAltitudes.unitState(target)+" inUpdater="+Groups.build.contains(b->b==point);
        draw=new FpvWorldRenderer();draw.setUnitAltitudes(mod.unitAltitudes);draw.setConstruction(mod.construction);draw.setCombat(mod.combat);started=true;
    }catch(Throwable t){fail(t);}}
    void afterDraw(){if(!started||done)return;try{
        camera.resize(Core.graphics.getWidth(),Core.graphics.getHeight());camera.near=.04f;camera.far=300;camera.up.set(0,1,0);camera.position.set(43,8,-30);camera.lookAt(42,1,-40);camera.update();draw.render(camera);
        if(frame==10){var f=SupportEffectsRenderer.class.getDeclaredField("candidates");f.setAccessible(true);var changes=SupportEffectsRenderer.class.getDeclaredField("tileChanges");changes.setAccessible(true);var map=FpvWorldRenderer.class.getDeclaredField("placedBuildings");map.setAccessible(true);out.child("diagnostic.txt").writeString(state+"\ncurrent efficiency="+point.efficiency+" strength="+point.strength+" pose="+mod.unitAltitudes.unitState(target)+"\nstats="+draw.supportEffects().stats()+"\nbeam="+draw.supportEffects().healingBeam(point)+"\nplaced="+draw.placedBuildingCount()+" candidates="+f.get(draw.supportEffects())+" client="+Vars.net.client()+" sourceFog="+point.inFogTo(Vars.player.team())+" targetFog="+target.inFogTo(Vars.player.team())+" changes="+Vars.world.tileChanges+" cached="+changes.get(draw.supportEffects())+" map="+map.get(draw));ScreenUtils.saveScreenshot(out.child("repair-beam.png"));}
        if(Gl.getError()!=0)throw new AssertionError("GL error");
        if(++frame==30){done=true;out.child("result.json").writeString("{\"status\":\"OBSERVED\",\"healing\":"+(target.health>hp)+",\"beams\":"+draw.supportEffects().stats().healingBeams()+"}");draw.dispose();Core.app.post(Core.app::exit);}
    }catch(Throwable t){fail(t);}}
    void fail(Throwable t){done=true;out.child("failed.txt").writeString(t.toString());Log.err(t);Core.app.post(Core.app::exit);}
}
