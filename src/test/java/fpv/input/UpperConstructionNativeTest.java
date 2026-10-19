package fpv.input;

import arc.Core;
import fpv.construction.*;
import fpv.terrain.TerrainElevation;
import fpv.world.World3DAdapter;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.units.BuildPlan;
import mindustry.game.*;import mindustry.core.GameState;
import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Real 160.7 content, queues, core inventories and builder logic; mocked input/GL, no physical acceptance. */
public final class UpperConstructionNativeTest {
 static int checks;
 static void verify(boolean v,String s){checks++;if(!v)throw new AssertionError(s+"; "+runtime.status());}
 static LayerActions.Action at(int x,int z,String name){return LayerActions.Action.place(x,30,z,name,false,0,LayerComponent.Mode.FREE);}
 static void reset(){unit.plans.clear();runtime.reset();Vars.player.unit(unit);unit.updateBuilding(true);input.isBuilding=true;unit.set(32*8,32*8);Vars.state.set(GameState.State.playing);Vars.state.rules.infiniteResources=false;Vars.state.rules.instantBuild=false;Vars.state.rules.buildCostMultiplier=1;unit.buildSpeedMultiplier=1;Team.sharded.rules().buildSpeedMultiplier=1;for(var item:Vars.content.items())core.items.set(item,10000);}
 static LayerComponent block(int x,int z){return runtime.grid().at(x,30,z,LayerComponent.Kind.BLOCK);}
 static ConstructionPriority.Target targetAt(int x,int z,String name){return new ConstructionPriority.Target(unit.id,x,30,z,false,Vars.content.getByName(mindustry.ctype.ContentType.block,name),0);}
 static int finish(int x,int z,int max){int n=0;while(block(x,z)==null&&n<max){tick();n++;}return n;}
 static VolumeClipboard.Cell c(int x,int z,String name,boolean platform){return new VolumeClipboard.Cell(x,0,z,name,1,0,platform,new byte[]{0});}
 static LayerActions.Action copy(List<VolumeClipboard.Cell> cells,int x,int z,int width,int floors){return LayerActions.Action.volume(new VolumeClipboard.Placement(new VolumeClipboard.Clip(1.4,width,1,floors,cells,List.of()),x,30,z),LayerComponent.Mode.FREE);}
 public static void main(String[] args)throws Exception{
  System.setProperty("fpv.construction.timing","true");init();runtime.close();runtime=new LayerRuntime(new World3DAdapter(new TerrainElevation(),(x,z)->{var t=Vars.world.tile((int)Math.floor(x+.5),(int)Math.floor(-z+.5));return t!=null&&t.build!=null?fpv.net.HeadlessGeometry.heightFor(t.block()):0;},altitude));renderer.setRuntime(runtime);
  try{duration();pauseRange();starvation();budget();support();large();codec();bounded();System.out.println("UpperConstructionNativeTest: "+checks+" checks PASS; physical Windows/Linux/gameplay NOT TESTED.");}
  finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
 }
 static void duration(){
  for(int z:new int[]{1,2,5}){reset();long start=System.nanoTime();var result=runtime.acceptConstruction(Vars.player,at(30,z,"copper-wall"));double admission=(System.nanoTime()-start)/1e6;
   verify(result.accepted()&&runtime.pastes().pending(Vars.player)==1&&block(30,z)==null,"manual Z"+z+" queues unpaid survival work");int money=core.items.get(Items.copper);int expected=(int)Math.ceil(Blocks.copperWall.buildTime/unit.type.buildSpeed);
   for(int i=0;i<expected-1;i++)tick();verify(block(30,z)==null&&runtime.pastes().progress(Vars.player,targetAt(30,z,"copper-wall"))>.9,"normal vanilla work before completion");tick();verify(block(30,z)!=null&&core.items.get(Items.copper)==money-Blocks.copperWall.requirements[0].amount,"exact tick and once-only recipe");
   renderReady();long rev=runtime.grid().geometryRevision();tick();verify(runtime.grid().geometryRevision()==rev&&runtime.pastes().targets(Vars.player).isEmpty(),"completion invalidates geometry and removes ghost without confirmation wait");
   System.out.printf(Locale.ROOT,"A209c manual Z%d request-to-queue %.3f ms; queue-to-commit %d ticks (%.3f simulation seconds); render/display NOT MEASURED%n",z,admission,expected,expected/60.0);
  }
  reset();Vars.state.rules.buildCostMultiplier=2;unit.buildSpeedMultiplier=2;Team.sharded.rules().buildSpeedMultiplier=2;
  verify(runtime.acceptConstruction(Vars.player,at(31,2,"copper-wall")).accepted(),"modifiers admission");int n=finish(31,2,1000);verify(n==18&&core.items.get(Items.copper)==10000-2*Blocks.copperWall.requirements[0].amount,"cost multiplier and both builder-speed modifiers match native formula");
  reset();Vars.state.rules.infiniteResources=true;var pasted=copy(List.of(c(0,0,"copper-wall",false)),31,2,1,1);verify(runtime.acceptConstruction(Vars.player,pasted).accepted()&&block(31,2)==null,"sandbox still validates and queues paste");tick();verify(block(31,2)!=null,"sandbox completes next authoritative update, not 36 ticks later");
  verify(runtime.requestPlace(30,30,1,"copper-wall",false,0,LayerComponent.Mode.FREE).accepted()&&block(30,1)!=null,"sandbox manual immediate placement preserved");
  reset();Vars.state.rules.instantBuild=true;verify(runtime.requestPlace(30,30,2,"copper-wall",false,0,LayerComponent.Mode.FREE).accepted()&&block(30,2)!=null&&core.items.get(Items.copper)==9994,"instant-build rule still charges normal finite recipe");
 }
 static void pauseRange(){
  reset();runtime.acceptConstruction(Vars.player,at(30,2,"copper-wall"));for(int i=0;i<10;i++)tick();var t=targetAt(30,2,"copper-wall");double p=runtime.pastes().progress(Vars.player,t);int money=core.items.get(Items.copper);input.toggleConstruction();for(int i=0;i<50;i++)tick();verify(runtime.pastes().progress(Vars.player,t)==p&&core.items.get(Items.copper)==money,"F3 keeps partial work and spends nothing while paused");input.toggleConstruction();
  unit.set(120*8,120*8);for(int i=0;i<100;i++)tick();verify(runtime.pastes().progress(Vars.player,t)==p,"no out-of-range accumulated completion burst");unit.set(32*8,32*8);
  Vars.state.set(GameState.State.paused);for(int i=0;i<20;i++)runtime.update(1.0/60);verify(runtime.pastes().progress(Vars.player,t)==p,"global pause stops authoritative progress");Vars.state.set(GameState.State.playing);
  var other=UnitTypes.alpha.create(Team.sharded);other.set(unit.x,unit.y);other.add();Vars.player.unit(other);for(int i=0;i<50;i++)tick();verify(runtime.pastes().progress(Vars.player,t)==p,"switching units retains unpaid work without other unit completing it");Vars.player.unit(unit);verify(finish(30,2,100)==26,"resume exactly remaining work");other.remove();
  reset();core.items.set(Items.copper,0);verify(runtime.acceptConstruction(Vars.player,at(30,2,"copper-wall")).accepted(),"missing resources admit a blueprint, not an operational block");for(int i=0;i<100;i++)tick();verify(block(30,2)==null&&runtime.pastes().progress(Vars.player,targetAt(30,2,"copper-wall"))==0,"resources gate work");core.items.set(Items.copper,6);verify(finish(30,2,100)==36&&core.items.get(Items.copper)==0,"delivery resumes normal duration and exact payment");
 }
 static void starvation(){
  reset();core.items.set(Items.copper,0);var a=copy(List.of(c(0,0,"copper-wall",false),c(2,0,"titanium-wall",false)),34,2,3,1);verify(runtime.acceptConstruction(Vars.player,a).accepted(),"blocked-head batch admitted");int n=finish(36,2,300);verify(block(36,2)!=null&&block(34,2)==null&&runtime.pastes().pending(Vars.player)==1,"unavailable head no longer starves independently buildable next entry");verify(runtime.pastes().progress(Vars.player,targetAt(34,2,"copper-wall"))==0,"blocked work cannot bank ticks");core.items.set(Items.copper,6);verify(finish(34,2,100)==36,"previously blocked entry resumes normally");System.out.println("A209c ready second entry completes at tick "+n+"; baseline still absent after 300 ticks.");
  reset();core.items.set(Items.copper,0);var mixed=copy(List.of(c(0,0,"copper-wall",false),c(2,2,"titanium-wall",false)),34,0,3,3);verify(runtime.acceptConstruction(Vars.player,mixed).accepted(),"native/upper independent batch admitted");verify(finish(36,2,200)==72&&Vars.world.tile(34,30).block()==Blocks.air,"resource-blocked native foundation does not starve an independent free upper entry");
  reset();runtime.acceptConstruction(Vars.player,at(30,2,"copper-wall"));runtime.acceptConstruction(Vars.player,at(32,3,"copper-wall"));for(int i=0;i<10;i++)tick();double p=runtime.pastes().progress(Vars.player,targetAt(30,2,"copper-wall"));verify(runtime.requestPriority(targetAt(32,3,"copper-wall")).accepted(),"priority switches upper floors");verify(finish(32,3,100)==36&&runtime.pastes().progress(Vars.player,targetAt(30,2,"copper-wall"))==p,"selected priority completes without resetting old progress");verify(finish(30,2,100)==26,"old progress resumes after priority");
  reset();runtime.acceptConstruction(Vars.player,at(30,2,"copper-wall"));for(int i=0;i<10;i++)tick();runtime.submit(LayerActions.Action.cancelPaste());for(int i=0;i<60;i++)tick();verify(block(30,2)==null&&runtime.pastes().pending(Vars.player)==0&&core.items.get(Items.copper)==10000,"explicit queue cancellation removes work without spending or completion");
 }
 static void budget(){
  reset();runtime.acceptConstruction(Vars.player,at(30,1,"copper-wall"));runtime.acceptConstruction(Vars.player,at(31,2,"copper-wall"));for(int i=0;i<35;i++)tick();verify(block(30,1)==null&&block(31,2)==null,"multiple jobs cannot each receive full builder budget");tick();verify(block(30,1)!=null&&block(31,2)==null,"one normal completion at 36 ticks");verify(finish(31,2,100)==36,"second job uses another 36 ticks");
  reset();runtime.acceptConstruction(Vars.player,at(30,2,"copper-wall"));unit.plans.addLast(new BuildPlan(28,30,0,Blocks.copperWall));for(int i=0;i<10;i++)tick();verify(runtime.pastes().progress(Vars.player,targetAt(30,2,"copper-wall"))==0,"native and upper work never accrue simultaneously");for(int i=0;i<1000&&!unit.plans.isEmpty();i++)tick();verify(Vars.world.tile(28,30).block()==Blocks.copperWall,"Z0 native builder preserved");verify(finish(30,2,100)==36,"upper work begins after native ownership ends");Vars.world.tile(28,30).setAir();
 }
 static void support(){
  reset();Core.settings.put(LayerRuntime.FREE_PERMISSION,false);var unsupported=LayerActions.Action.place(40,30,2,"copper-wall",false,0,LayerComponent.Mode.STRUCTURAL);verify(!runtime.acceptConstruction(Vars.player,unsupported).accepted()&&runtime.pastes().pending(Vars.player)==0,"unsupported manual position rejected before queue");Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
  Vars.world.tile(30,30).setBlock(Blocks.titaniumWall,Team.sharded,0);var cells=List.of(c(0,0,"titanium-wall",false),c(0,1,LayerCatalog.PLATFORM,true),c(0,1,"conveyor",false));var clip=new VolumeClipboard.Clip(1.4,1,1,2,cells,List.of());var a=LayerActions.Action.volume(new VolumeClipboard.Placement(clip,30,30,1),LayerComponent.Mode.STRUCTURAL);verify(runtime.acceptConstruction(Vars.player,a).accepted(),"structural column/platform/block admission uses support-first order");for(int i=0;i<2000&&runtime.pastes().pending(Vars.player)>0;i++){tick();if(block(30,2)!=null)verify(block(30,1)!=null&&runtime.grid().at(30,30,2,LayerComponent.Kind.PLATFORM)!=null,"dependent block never appears before supports");}verify(block(30,2)!=null&&runtime.solver().support(block(30,2).id()).supported(),"survival factory completes on real supported Z2");Vars.world.tile(30,30).setAir();
 }
 static void large(){
  reset();Vars.state.rules.infiniteResources=true;var cells=new ArrayList<VolumeClipboard.Cell>();for(int x=0;x<16;x++)for(int y=0;y<16;y++)cells.add(new VolumeClipboard.Cell(x,y,0,"copper-wall",1,0,false,new byte[]{0}));
  var a=LayerActions.Action.volume(new VolumeClipboard.Placement(new VolumeClipboard.Clip(1.4,16,16,1,cells,List.of()),45,45,3),LayerComponent.Mode.FREE);verify(runtime.acceptConstruction(Vars.player,a).accepted(),"large 256-entry sandbox blueprint admitted");tick();verify(runtime.grid().size()==32&&runtime.pastes().pending(Vars.player)==224,"instant mode retains bounded 32-completion frame budget");int n=1;while(runtime.pastes().pending(Vars.player)>0&&n<20){tick();n++;}verify(runtime.grid().size()==256&&n==8,"large sandbox job completes in exactly eight bounded updates, no hidden timer");System.out.println("A209c sandbox256: first32 in 1 update, all256 in 8 updates (0.133 simulation seconds at60Hz).");
 }
 static void renderReady(){
  // No texture pixels/GPU claim: exercise the real visibility/revision/chunk-cache preparation.
  Core.atlas=new arc.graphics.g2d.TextureAtlas(){@Override public arc.graphics.g2d.TextureAtlas.AtlasRegion white(){return new arc.graphics.g2d.TextureAtlas.AtlasRegion();}};
  try{var visibility=renderer.getClass().getDeclaredMethod("refreshVisibility",LayerWorld.class);var sync=renderer.getClass().getDeclaredMethod("sync",LayerWorld.class);visibility.setAccessible(true);sync.setAccessible(true);long count=renderer.meshBuilds();visibility.invoke(renderer,runtime.grid());sync.invoke(renderer,runtime.grid());verify(renderer.meshBuilds()>count,"completed block enters render cache on next sync without visibility timeout");verify(runtime.timings().samples().stream().anyMatch(s->s.stage().equals("COMMIT_TO_RENDER_READY")),"opt-in commit-to-render-ready measured separately from intended build time");}catch(Exception e){throw new AssertionError(e);}
 }
 static void codec(){
  reset();runtime.acceptConstruction(Vars.player,at(30,2,"copper-wall"));for(int i=0;i<10;i++)tick();var t=targetAt(30,2,"copper-wall");var snapshot=ConstructionPlanCodec.read(runtime.pastes().replication(Vars.player));verify(snapshot.targets().equals(List.of(t))&&Math.abs(snapshot.progress().get(t)-10.0/36)<1.0/255,"owner-only snapshot carries authoritative bounded progress");
  byte[] bytes=runtime.pastes().replication(Vars.player);bytes[bytes.length-1]=(byte)255;try{ConstructionPlanCodec.read(bytes);throw new AssertionError("forged complete ghost");}catch(IllegalArgumentException good){verify(true,"invalid completed progress rejected atomically");}
  byte[] save=runtime.replication();verify(java.nio.ByteBuffer.wrap(save).getInt()==5,"world save format unchanged");
 }
 static void bounded(){
  reset();Vars.state.rules.instantBuild=true;for(int x=10;x<35;x++)for(int z=1;z<=12;z++)runtime.acceptConstruction(Vars.player,at(x,z,"copper-wall"));verify(runtime.timings().samples().size()<=256,"opt-in timing ring/log budget bounded");
 }
}

