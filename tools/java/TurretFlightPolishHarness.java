package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.Gl;import arc.graphics.g3d.Camera3D;import arc.util.*;import fpv.construction.*;import fpv.render.*;import mindustry.*;import mindustry.content.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.game.*;import mindustry.core.GameState.State;

/** Native standalone gallery: identical fixture usable on the accepted baseline and polish JAR. */
final class SmokeHarness {
 final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();FpvWorldRenderer draw;boolean started,done;int frame;long nanos;int measured;StringBuilder results=new StringBuilder();
 final Block[] types={Blocks.duo,Blocks.repairPoint,Blocks.repairTurret,Blocks.parallax,Blocks.segment,Blocks.lancer,Blocks.arc,Blocks.meltdown,Blocks.wave,Blocks.tsunami,Blocks.hail,Blocks.ripple,Blocks.salvo,Blocks.cyclone};
 final java.util.ArrayList<Building> buildings=new java.util.ArrayList<>();String gpu="NOT RUN";
 SmokeHarness(FpvMod m){mod=m;}void update(){}
 void start(){try{
  if(!Vars.dataDirectory.absolutePath().startsWith(out.absolutePath())||Vars.steam)throw new AssertionError("Isolated standalone only");
  Core.settings.put("fpscap",120);Core.settings.put("sfxvol",0);Core.settings.put("musicvol",0);Vars.logic.reset();
  Vars.world.loadGenerator(128,128,tiles->{for(int y=0;y<128;y++)for(int x=0;x<128;x++)tiles.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));tiles.get(8,8).setBlock(Blocks.coreShard,Team.sharded,0);});
  Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","Turret-flight polish disposable scene"));Vars.player.team(Team.sharded);var u=UnitTypes.gamma.create(Team.sharded);u.set(64*8,64*8);u.add();Vars.player.unit(u);Vars.logic.play();Vars.state.set(State.paused);Vars.state.rules.editor=true;Vars.state.rules.infiniteResources=true;Vars.state.rules.instantBuild=true;Vars.state.rules.fog=false;Vars.state.rules.waves=false;Vars.state.rules.canGameOver=false;Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
  var auto=FpvMod.class.getDeclaredField("autoEnable");auto.setAccessible(true);auto.set(mod,false);mod.setEnabled(false);draw=new FpvWorldRenderer();draw.setUnitAltitudes(mod.unitAltitudes);draw.setCombat(mod.combat);draw.setConstruction(mod.construction);
  for(int i=0;i<types.length;i++){Vars.world.tile(x(i),y(i)).setBlock(types[i],Team.sharded,0);var b=Vars.world.build(x(i),y(i));buildings.add(b);yaw(b,35);}
  for(int floor:new int[]{1,3,5})for(int i=0;i<3;i++){
   var block=types[i];int x=24+i*14,y=100+floor*3,low=-(block.size-1)/2;
   for(int dy=0;dy<block.size;dy++)for(int dx=0;dx<block.size;dx++)place(x+low+dx,y+low+dy,floor,Blocks.air,true);
   var b=mod.construction.building(place(x,y,floor,block,false));buildings.add(b);yaw(b,35);
  }
  started=true;
 }catch(Throwable t){fail(t);}}
 int x(int i){return 20+i%4*24;}int y(int i){return 24+i/4*20;}
 long place(int x,int y,int floor,Block block,boolean slab){var r=mod.construction.apply(Vars.player,LayerActions.Action.place(x,y,floor,block.name,slab,0,LayerComponent.Mode.FREE));if(!r.accepted())throw new AssertionError("fixture: "+r.reason());return r.componentId();}
 static void yaw(Building b,float angle){if(b instanceof mindustry.world.blocks.defense.turrets.BaseTurret.BaseTurretBuild t)t.rotation=angle;else if(b instanceof mindustry.world.blocks.units.RepairTurret.RepairPointBuild r)r.rotation=angle;}
 void afterDraw(){if(!started||done)return;try{
  if(frame==0&&Boolean.getBoolean("fpv.polishGeometry"))Class.forName("fpv.render.TurretPolishGeometryProbe").getMethod("run",Fi.class).invoke(null,out);
  if(frame==0&&Boolean.getBoolean("fpv.polishGpu"))gpu=(String)Class.forName("fpv.render.TurretPolishGpuChecks").getMethod("run",fpv.units.UnitAltitudeSystem.class,Fi.class).invoke(null,mod.unitAltitudes,out);
  int stage=frame/30,tick=frame%30,total=types.length*3+3,index=stage/3;String name;
  camera.resize(Core.graphics.getWidth(),Core.graphics.getHeight());camera.near=.04f;camera.far=2000;camera.fov=50;camera.up.set(0,1,0);
  if(stage<types.length*3){int view=stage%3;var b=buildings.get(index);float x=b.x/8,z=-b.y/8,h=FpvWorldRenderer.heightFor(b.block),d=Math.max(1,b.block.size*.8f);name=b.block.name+"-"+(view==0?"top":view==1?"oblique":"side");
   if(view==0){camera.position.set(x,h+5*d,z);camera.up.set(0,0,-1);}else camera.position.set(x+2.6f*d,h+(view==1?2.4f:.08f)*d,z+3.5f*d);camera.lookAt(x,h*.85f,z);
  }else{int floor=new int[]{1,3,5}[stage-types.length*3];float base=(float)mod.construction.planeHeight(floor);name="repair-floor"+floor;camera.position.set(49,base+12,-82-floor*3);camera.lookAt(38,base+.7f,-100-floor*3);}
  camera.update();long time=System.nanoTime();draw.render(camera);if(tick>=10){nanos+=System.nanoTime()-time;measured++;}
  if(Gl.getError()!=0)throw new AssertionError("GL error: "+name);if(tick==20)ScreenUtils.saveScreenshot(out.child(name+".png"));
  if(tick==29){results.append(name+","+nanos/1e6/measured+"\n");nanos=0;measured=0;}
  if(++frame==total*30){done=true;out.child("render-times.csv").writeString("scene,cpuMs\n"+results);out.child("gpu-checks.txt").writeString(gpu);out.child("result.json").writeString("{\"status\":\"PASS\",\"java\":\""+System.getProperty("java.version")+"\",\"scenes\":"+total+",\"glErrors\":0,\"physicalAcceptance\":false}");draw.dispose();Core.app.post(Core.app::exit);}
 }catch(Throwable t){fail(t);}}
 void fail(Throwable t){done=true;out.child("failed.txt").writeString(t.toString());Log.err(t);Core.app.post(Core.app::exit);}
}
