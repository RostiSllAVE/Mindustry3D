package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.Gl;import arc.graphics.g3d.Camera3D;import arc.util.*;import fpv.render.*;import fpv.units.*;import fpv.world.Unit3DState;import mindustry.*;import mindustry.content.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.game.*;import mindustry.core.GameState.State;

/** Native aircraft bodies plus shipping exhaust renderer: no flight/controller simulation. */
final class SmokeHarness {
 final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();FpvWorldRenderer draw;UnitAltitudeSystem altitude;Unit pilot,unit;boolean started,done;int frame,active=-1,checks;long nanos;int measured;StringBuilder times=new StringBuilder();final java.util.ArrayList<Double> samples=new java.util.ArrayList<>();
 final mindustry.type.UnitType[] types={UnitTypes.flare,UnitTypes.zenith,UnitTypes.antumbra,UnitTypes.mono,UnitTypes.oct,UnitTypes.quell,UnitTypes.disrupt};
 final java.util.IdentityHashMap<Unit,Unit3DState> poses=new java.util.IdentityHashMap<>();final java.util.ArrayList<Unit> fleet=new java.util.ArrayList<>();
 SmokeHarness(FpvMod m){mod=m;}void update(){}
 void start(){try{
  if(!Vars.dataDirectory.absolutePath().startsWith(out.absolutePath())||Vars.steam)throw new AssertionError("Disposable standalone only");
  Core.settings.put("fpscap",120);Core.settings.put("sfxvol",0);Core.settings.put("musicvol",0);Vars.logic.reset();Vars.world.loadGenerator(96,96,tiles->{for(int y=0;y<96;y++)for(int x=0;x<96;x++)tiles.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));tiles.get(4,4).setBlock(Blocks.coreShard,Team.sharded,0);});
  Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","Aircraft exhaust visual fixture"));Vars.player.team(Team.sharded);pilot=UnitTypes.gamma.create(Team.sharded);pilot.set(4*8,4*8);pilot.add();Vars.player.unit(pilot);Vars.logic.play();Vars.state.set(State.paused);Vars.state.rules.fog=false;Vars.state.rules.canGameOver=false;Vars.state.rules.waves=false;
  var auto=FpvMod.class.getDeclaredField("autoEnable");auto.setAccessible(true);auto.set(mod,false);mod.setEnabled(false);
  Vars.state.rules.unitCap=1000; // Disposable benchmark only: the core's native cap is otherwise eight Flares.
  draw=new FpvWorldRenderer();altitude=new UnitAltitudeSystem(draw.terrain()::heightAt);altitude.setExternalStateProvider(poses::get);draw.setUnitAltitudes(altitude);draw.setCombat(mod.combat);started=true;
 }catch(Throwable t){fail(t);}}
 void activate(int type){if(active==type)return;Vars.player.unit(pilot);if(unit!=null)unit.remove();active=type;unit=types[type].create(Team.sharded);unit.elevation=1;unit.set(48*8,48*8);unit.rotation=0;unit.add();}
 void pose(Unit u,float x,float y,float pitch,float roll){u.set(x*8,y*8);poses.put(u,new Unit3DState(u.id,u.type.id,u.team.id,x,y,10,3,0,0,0,pitch,roll,10,u.hitSize/16,.3,true));}
 int engines()throws Exception{try{var stats=draw.getClass().getMethod("flyingEngineStats").invoke(draw);return (int)stats.getClass().getMethod("engines").invoke(stats);}catch(NoSuchMethodException baseline){return 0;}}
 void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 void afterDraw(){if(!started||done)return;try{
  int normal=types.length*5,total=normal+3;boolean benchmark=Boolean.getBoolean("fpv.exhaustBenchmark");int stage=benchmark?normal+2:frame/30,tick=benchmark?frame:frame%30;String name;
  Time.time+=1;camera.resize(Core.graphics.getWidth(),Core.graphics.getHeight());camera.near=.04f;camera.far=500;camera.fov=55;camera.up.set(0,1,0);draw.setFirstPersonActive(false);draw.setShowControlledUnit(true);Vars.player.unit(pilot);
  if(stage<normal){activate(stage/5);int view=stage%5;float radius=Math.max(.8f,unit.hitSize/8f),distance=Math.max(3,radius*3);
   pose(unit,48,48,view==3?30:0,view==3?95:0);unit.vel.set(unit.type.speed*.8f,0);name=unit.type.name+"-"+new String[]{"top","side","rear","bank","fpv-other"}[view];
   if(view==0){camera.position.set(48,10+distance*1.7f,-48);camera.up.set(0,0,-1);}else if(view==2)camera.position.set(48-distance,10+radius*.4f,-48);else camera.position.set(48-distance*.65f,10+radius*.6f,-48+distance);
   camera.lookAt(48,10,-48);if(view==4){draw.setFirstPersonActive(true);draw.setShowControlledUnit(false);}
  }else if(stage==normal){activate(0);pose(unit,48,48,0,0);Vars.player.unit(unit);draw.setFirstPersonActive(true);draw.setShowControlledUnit(false);name="own-fpv";camera.position.set(48,10,-48);camera.lookAt(52,10,-48);}
  else if(stage==normal+1){activate(0);pose(unit,48,48,0,0);name="dead-no-exhaust";unit.dead=true;camera.position.set(44,11,-44);camera.lookAt(48,10,-48);}
  else{if(fleet.isEmpty()){if(unit!=null)unit.remove();for(int i=0;i<50;i++){var u=UnitTypes.flare.create(Team.sharded);u.elevation=1;u.add();pose(u,38+i%10*2,44+i/10*2,0,0);fleet.add(u);}}name="fleet-50";camera.up.set(0,0,-1);camera.position.set(47,32,-48);camera.lookAt(47,10,-48);}
  camera.update();long start=System.nanoTime();draw.render(camera);long elapsed=System.nanoTime()-start;if(tick>=(benchmark?100:10)){nanos+=elapsed;measured++;if(benchmark)samples.add(elapsed/1e6);}
  check(Gl.getError()==0,"GL error in "+name);boolean baseline=Boolean.getBoolean("fpv.exhaustBaseline");
  if(stage==normal+2&&tick==0){var diagnostic=new StringBuilder("id,x,y,height,elevation,health,flying,frustum,submitted\n");for(var u:fleet){var p=altitude.pose(u);float h=p==null?-1:(float)p.height();diagnostic.append(u.id+","+u.x/8+","+u.y/8+","+h+","+u.elevation+","+u.health+","+u.isFlying()+","+camera.frustum.containsBounds(u.x/8,h,-u.y/8,1,1,1)+","+draw.wasUnitBodySubmitted(u)+"\n");}out.child("fleet-diagnostic.csv").writeString(diagnostic.toString());}
  if(!baseline){if(stage==normal||stage==normal+1)check(engines()==0,"no self-FPV/dead exhaust overlay");else check(engines()>0,"actual exhaust submitted in "+name);if(stage==normal+2)check(engines()==50,"all fifty visible fleet engines submitted: "+engines()+" / "+Groups.unit.size()+" native units");}
  if(tick==(benchmark?50:20))ScreenUtils.saveScreenshot(out.child(name+".png"));if(!benchmark&&tick==29){times.append(name+","+nanos/1e6/measured+","+engines()+"\n");nanos=0;measured=0;}
  if(++frame==(benchmark?600:total*30)){done=true;
   if(benchmark){var sorted=new java.util.ArrayList<>(samples);java.util.Collections.sort(sorted);double median=sorted.get(sorted.size()/2),p95=sorted.get((int)(sorted.size()*.95));out.child("benchmark.json").writeString("{\"units\":50,\"samples\":"+samples.size()+",\"meanCpuMs\":"+nanos/1e6/measured+",\"medianCpuMs\":"+median+",\"p95CpuMs\":"+p95+",\"engines\":"+engines()+",\"gpuTimeMeasured\":false}");for(double sample:samples)times.append("fleet-50,").append(sample).append(',').append(engines()).append('\n');}
   out.child("render-times.csv").writeString("scene,cpuMs,engines\n"+times);out.child("result.json").writeString("{\"status\":\"PASS\",\"java\":\""+System.getProperty("java.version")+"\",\"scenes\":"+(benchmark?1:total)+",\"checks\":"+checks+",\"glErrors\":0,\"physicalAcceptance\":false}");draw.dispose();altitude.close();Core.app.post(Core.app::exit);}
 }catch(Throwable t){fail(t);}}
 void fail(Throwable t){done=true;out.child("failed.txt").writeString(t.toString());Log.err(t);Core.app.post(Core.app::exit);}
}
