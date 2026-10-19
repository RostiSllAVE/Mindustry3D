package fpv.net;
import arc.Core;
import fpv.construction.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import static fpv.net.ConnectionNativeHandlersTest.*;
/** Actual authenticated request/state handlers; transport is in memory, no real two-PC acceptance. */
public final class UpperConstructionNetworkTest {
 static int checks;static void verify(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  System.setProperty("fpv.netSmokeRole","a2-09c-in-memory-fixture");
  byte[] partial,worldDone,empty;
  try(var f=new Fixture()){
   flag("active",true);flag("server",true);call(f.net,"resetWorld",new Class[]{});Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
   var a=ConstructionPriorityNetworkTest.peer(f,10,10);var b=ConstructionPriorityNetworkTest.peer(f,12,10);
   Vars.world.setGenerating(true);Vars.world.tile(5,5).setBlock(Blocks.coreShard,Team.sharded,0);var core=(CoreBuild)Vars.world.build(5,5);Team.sharded.data().cores.clear();Team.sharded.data().cores.add(core);core.items.set(Items.copper,100);Vars.state.rules.infiniteResources=false;
   var action=LayerActions.Action.place(18,18,2,"copper-wall",false,0,LayerComponent.Mode.FREE);byte[] packet=f.net.fixtureConstructionPacket(action);
   VolumeClipboardNetworkTest.send(f,a,packet);verify(f.layers.pastes().pending(a)==1&&f.layers.grid().size()==0&&core.items.get(Items.copper)==100,"authenticated manual request is unpaid queued work, never client-forced completion");
   VolumeClipboardNetworkTest.send(f,a,packet);verify(f.layers.pastes().pending(a)==1,"replay cannot duplicate manual work");
   VolumeClipboardNetworkTest.send(f,a,f.net.fixtureConstructionPacket(action));verify(f.layers.pastes().pending(a)==1,"different request sequence still cannot duplicate occupied pending footprint");
   a.unit().updateBuilding(false);for(int i=0;i<10;i++)f.layers.update(1.0/60);verify(f.layers.grid().size()==0&&core.items.get(Items.copper)==100,"server honors clientSnapshot F3 pause flag");a.unit().updateBuilding(true);
   for(int i=0;i<10;i++)f.layers.update(1.0/60);var target=f.layers.pastes().targets(a).get(0);double progress=f.layers.pastes().progress(a,target);verify(progress>0&&progress<1,"real authoritative partial progress");partial=f.layers.pastes().replication(a);verify(ConstructionPlanCodec.read(partial).progress().get(target)>0,"owner-private state includes progress");
   VolumeClipboardNetworkTest.send(f,b,f.net.fixtureConstructionPacket(LayerActions.Action.prioritize(target)));VolumeClipboardNetworkTest.send(f,b,f.net.fixtureConstructionPacket(LayerActions.Action.cancelPaste()));verify(f.layers.pastes().pending(a)==1&&f.layers.pastes().progress(a,target)==progress,"other authenticated peer cannot modify owner's work");
   for(int i=0;i<25;i++)f.layers.update(1.0/60);verify(f.layers.grid().size()==0,"server waits full vanilla duration");f.layers.update(1.0/60);verify(f.layers.grid().size()==1&&core.items.get(Items.copper)==94,"one authoritative completion and one exact recipe charge");
   worldDone=f.layers.replication();empty=f.layers.pastes().replication(a);for(int i=0;i<50;i++)f.layers.update(1.0/60);verify(f.layers.grid().size()==1&&core.items.get(Items.copper)==94,"subsequent frames do not repeat completion");
   VolumeClipboardNetworkTest.send(f,b,new byte[]{1,2,3});verify(f.layers.grid().size()==1,"malformed packet cannot forge work/progress");
  }
  try(var f=new Fixture()){
   f.connect();f.world();f.ack(10);f.synchronize(10);f.state("construction-plans",partial,10);var target=f.layers.pastes().targets(Vars.player).get(0);verify(f.layers.pastes().progress(Vars.player,target)>0&&f.layers.grid().size()==0,"client receives progress only, not an operational building");
   f.state("layered-world",worldDone,10);verify(f.layers.grid().size()==1,"real layered-world handler materializes completed geometry");f.state("construction-plans",empty,10);verify(f.layers.pastes().targets(Vars.player).isEmpty(),"independent owner snapshot removes completed ghost");
   int count=f.layers.grid().size();var r=f.layers.acceptConstruction(Vars.player,LayerActions.Action.place(20,20,2,"copper-wall",false,0,LayerComponent.Mode.FREE));verify(!r.accepted()&&f.layers.grid().size()==count,"client cannot call server construction entry point");
  }
  System.out.println("UpperConstructionNetworkTest: "+checks+" checks PASS; actual handlers/in-memory transport, physical multiplayer NOT TESTED.");
 }
}
