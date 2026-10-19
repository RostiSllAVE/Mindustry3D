package fpv.net;
import arc.Core;
import fpv.construction.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.gen.Player;
import java.util.*;
import static fpv.net.ConnectionNativeHandlersTest.*;
/** Native authenticated handlers with in-memory transport; no real socket acceptance. */
public final class VolumeClipboardNetworkTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static VolumeClipboard.Clip clip(){return new VolumeClipboard.Clip(1.4,3,1,3,List.of(new VolumeClipboard.Cell(0,0,0,"copper-wall",1,0,false,new byte[]{0}),new VolumeClipboard.Cell(2,0,2,"copper-wall",1,0,false,new byte[]{0})),List.of());}
 static LayerActions.Action action(int x){return LayerActions.Action.volume(new VolumeClipboard.Placement(clip(),x,18,1),LayerComponent.Mode.FREE);}
 static void send(Fixture f,Player p,byte[] bytes)throws Exception{call(f.net,"receiveConstructionServer",new Class[]{Player.class,byte[].class},p,bytes);}
 public static void main(String[] args)throws Exception{
  System.setProperty("fpv.netSmokeRole","a2-09b-in-memory-fixture");
  try(var f=new Fixture()){
   flag("active",true);flag("server",true);call(f.net,"resetWorld",new Class[]{});Core.settings.put(LayerRuntime.FREE_PERMISSION,true);var a=ConstructionPriorityNetworkTest.peer(f,10,10);var b=ConstructionPriorityNetworkTest.peer(f,12,10);
   byte[] request=f.net.fixtureConstructionPacket(action(18));send(f,a,request);check(f.layers.pastes().pending(a)==2&&f.layers.pastes().pending(b)==0,"authenticated request queues only actor-owned unpaid work");check(f.layers.grid().size()==0,"RPC creates no instant operational buildings");
   int rejected=f.net.rejectedPackets();send(f,a,request);check(f.net.rejectedPackets()>rejected&&f.layers.pastes().pending(a)==2,"replayed sequence never duplicates jobs");
   send(f,b,f.net.fixtureConstructionPacket(LayerActions.Action.cancelPaste()));check(f.layers.pastes().pending(a)==2,"other peer cannot cancel actor queue");
   send(f,a,f.net.fixtureConstructionPacket(action(24)));send(f,a,f.net.fixtureConstructionPacket(action(30)));check(f.layers.pastes().pending(a)==4,"two-per-second 3D admission limit bounds expensive work");
   f.layers.pastes().clear();var ground=new VolumeClipboard.Clip(1.4,1,1,1,List.of(new VolumeClipboard.Cell(0,0,0,"copper-wall",1,0,false,new byte[]{0})),List.of());
   send(f,a,f.net.fixtureConstructionPacket(LayerActions.Action.volume(new VolumeClipboard.Placement(ground,18,25,0),LayerComponent.Mode.STRUCTURAL)));check(a.unit().plans.size==1&&f.layers.pastes().targets(a).get(0).layer()==0,"authoritative Z0 job uses native builder queue and private XYZ metadata");
   var target=f.layers.pastes().targets(a).get(0);a.unit().plans.clear();send(f,a,f.net.fixtureConstructionPacket(LayerActions.Action.prioritize(target)));check(a.unit().plans.size==1&&f.layers.priority().target(a).equals(target),"owner priority restores a server volume plan omitted by native client snapshot");
  }
  try(var f=new Fixture()){
   flag("active",true);flag("server",true);call(f.net,"resetWorld",new Class[]{});Core.settings.put(LayerRuntime.FREE_PERMISSION,true);var a=ConstructionPriorityNetworkTest.peer(f,10,10);
   Vars.netServer.admins.addActionFilter(p->p.tile.x!=20);send(f,a,f.net.fixtureConstructionPacket(action(18)));check(f.layers.pastes().pending(a)==0&&f.layers.grid().size()==0,"denied second entry rejects whole batch without partial queue/world mutation");
   f.layers.pastes().clear();Core.settings.put(LayerRuntime.FREE_PERMISSION,false);send(f,a,f.net.fixtureConstructionPacket(action(25)));check(f.layers.pastes().pending(a)==0,"forged Free request rejected by host permission");
   f.layers.pastes().clear();Core.settings.put(LayerRuntime.FREE_PERMISSION,true);var data=VolumeClipboard.encode(clip());data[4]=99;var invalid=new LayerActions.Action(LayerActions.Op.VOLUME_PASTE,25,18,1,LayerComponent.Kind.BLOCK,"",0,LayerComponent.Mode.FREE,0,-1,0,0,data);send(f,a,f.net.fixtureConstructionPacket(invalid));check(f.layers.pastes().pending(a)==0,"unknown clipboard version rejected safely by actual handler");
   int rejected=f.net.rejectedPackets();send(f,a,new byte[LayerConfiguration.MAX_BYTES+1025]);check(f.net.rejectedPackets()>rejected,"oversize request rejected before decoding");
   f.layers.pastes().clear();a.unit().dead=true;send(f,a,f.net.fixtureConstructionPacket(action(25)));check(f.layers.pastes().pending(a)==0,"dead builder cannot construct");
  }
  try(var f=new Fixture()){
   f.connect();f.world();f.ack(10);f.synchronize(10);var t=new ConstructionPriority.Target(Vars.player.unit().id,18,18,0,false,Blocks.copperWall,0);f.state("construction-plans",ConstructionPlanCodec.encode(List.of(t)),10);int before=f.provider.sent.size();
   var result=f.layers.requestPriority(t);check(result.pending()&&f.provider.sent.size()>before&&Vars.player.unit().plans.isEmpty(),"client selects server-owned Z0 pending blueprint without fabricated local native plans");
  }
  System.out.println("VolumeClipboardNetworkTest: "+checks+" checks PASS; authenticated in-memory handlers, real network NOT TESTED.");
 }
}
