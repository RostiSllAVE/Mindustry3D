package fpv.net;

import arc.Core;
import arc.struct.*;
import fpv.construction.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.gen.*;
import java.util.*;
import static fpv.net.ConnectionNativeHandlersTest.*;

/** Real mod/native handlers, reliable chunks and authenticated peers; transport remains in-memory. */
public final class ConstructionPriorityNetworkTest {
    static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static Player peer(Fixture f,int x,int y)throws Exception{
        var p=Player.create();p.team(Team.sharded);p.con=new Con();var u=UnitTypes.alpha.create(Team.sharded);u.set(x*8,y*8);u.add();p.unit(u);p.add();
        call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.HELLO,0,1,out->out.writeUTF(f.hash)));return p;
    }
    static mindustry.game.Schematic schematic(){return new mindustry.game.Schematic(Seq.with(new mindustry.game.Schematic.Stile(Blocks.copperWall,0,0,null,(byte)0),new mindustry.game.Schematic.Stile(Blocks.copperWall,2,0,null,(byte)0)),new StringMap(),3,1);}
    static List<ConstructionPriority.Target> privatePlans(Player p)throws Exception{
        for(Object packet:((Con)p.con).sent){byte[] bytes=wire(packet);if(bytes.length<6)continue;
            try{var m=NetProtocol.decode(bytes);if(m.kind()!=NetProtocol.CHANNEL)continue;var chunk=NetProtocol.chunk(m.payload());if(chunk.channel().equals("construction-plans")){check(chunk.count()==1,"small owner snapshot is one reliable chunk");return ConstructionPlanCodec.decode(chunk.data());}}catch(IllegalArgumentException ignored){}
        }throw new AssertionError("No owner construction snapshot");
    }
    static int privateCount(Player p)throws Exception{int count=0;for(Object packet:((Con)p.con).sent)try{var m=NetProtocol.decode(wire(packet));if(m.kind()==NetProtocol.CHANNEL&&NetProtocol.chunk(m.payload()).channel().equals("construction-plans"))count++;}catch(IllegalArgumentException ignored){}return count;}
    public static void main(String[] args)throws Exception{
        System.setProperty("fpv.netSmokeRole","a2-08b-in-memory-fixture");
        try(var f=new Fixture()){
            flag("active",true);flag("server",true);call(f.net,"resetWorld",new Class[]{});Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
            var a=peer(f,10,10);var b=peer(f,12,10);check(f.layers.apply(a,LayerActions.Action.paste(new LayerSchematic.Stamp(schematic(),18,18),2,LayerComponent.Mode.FREE)).accepted(),"peer A owns a real upper queue");check(f.layers.apply(b,LayerActions.Action.paste(new LayerSchematic.Stamp(schematic(),26,18),3,LayerComponent.Mode.FREE)).accepted(),"peer B owns a distinct upper queue");
            ((Con)a.con).sent.clear();((Con)b.con).sent.clear();call(f.net,"after",new Class[]{});
            var av=privatePlans(a);var bv=privatePlans(b);check(av.size()==2&&av.stream().allMatch(t->t.unitId()==a.unit().id&&t.layer()==2),"A receives only A's pending plans, not B's queue");check(bv.size()==2&&bv.stream().allMatch(t->t.unitId()==b.unit().id&&t.layer()==3),"B receives only B's pending plans, not A's queue");
            int packets=privateCount(a);var channels=(Map<?,?>)get(f.net,"channels");put(channels.get("construction-plans"),"next",0d);call(f.net,"after",new Class[]{});check(privateCount(a)==packets,"unchanged private plans are cached; no repeated idle traffic");
            var target=av.get(1);byte[] action=f.net.fixtureConstructionPacket(LayerActions.Action.prioritize(target));
            call(f.net,"receiveConstructionServer",new Class[]{Player.class,byte[].class},a,action);check(f.layers.priority().target(a).equals(target)&&f.layers.pastes().targets(a).get(0).equals(target),"authenticated reliable priority action reaches server and reorders only owner's existing queue");
            check(f.layers.pastes().targets(b).equals(bv)&&f.layers.pastes().pending(a)==2,"other player's queue unchanged; no plan loss/duplication");
            var first=f.layers.pastes().targets(a);int rejected=f.net.rejectedPackets();call(f.net,"receiveConstructionServer",new Class[]{Player.class,byte[].class},a,action);check(f.net.rejectedPackets()>rejected&&f.layers.pastes().targets(a).equals(first),"replayed priority request rejected without duplicate work");
            byte[] forged=f.net.fixtureConstructionPacket(LayerActions.Action.prioritize(bv.get(1)));call(f.net,"receiveConstructionServer",new Class[]{Player.class,byte[].class},a,forged);check(f.layers.priority().target(a).equals(target)&&f.layers.priority().target(b)==null,"authenticated A cannot prioritize B's unit even with B's coordinates and ID");
            put(channels.get("construction-plans"),"next",0d);((Con)a.con).sent.clear();call(f.net,"after",new Class[]{});check(privatePlans(a).get(0).equals(target),"changed order is reliably synchronized back to the owner");
            var peers=(Map<?,?>)get(f.net,"peers");((Map<?,?>)get(peers.get(a),"channels")).clear();put(channels.get("construction-plans"),"next",0d);((Con)a.con).sent.clear();call(f.net,"after",new Class[]{});check(privatePlans(a).equals(first),"initial-state replay resends even an unchanged owner queue");
        }
        try(var f=new Fixture()){
            f.connect();f.world();f.ack(10);f.synchronize(10);check(f.net.allowed(),"new private channel participates in existing A2-02 initial readiness");
            var t=new ConstructionPriority.Target(Vars.player.unit().id,18,18,2,false,Blocks.duo,0);f.state("construction-plans",ConstructionPlanCodec.encode(List.of(t)),10);
            check(f.layers.pastes().targets(Vars.player).equals(List.of(t)),"client receives read-only upper blueprint view through existing atomic chunk application");
            byte[] grid=f.layers.replication();f.layers.applyReplication(grid);check(f.layers.pastes().targets(Vars.player).equals(List.of(t)),"ordinary layered-world replication does not erase the independent transient view");
            check(f.layers.grid().components().isEmpty()&&Vars.player.unit().plans.isEmpty(),"receiving blueprint metadata creates no operational buildings, costs or native plans");
            byte[] malformed=Arrays.copyOf(ConstructionPlanCodec.encode(List.of(t)),17);try{f.layers.pastes().applyReplication(malformed);throw new AssertionError("bad view accepted");}catch(IllegalArgumentException expected){check(f.layers.pastes().targets(Vars.player).equals(List.of(t)),"malformed view cannot partially replace accepted owner state");}
            f.world();check(f.layers.pastes().targets(Vars.player).isEmpty()&&!f.net.allowed(),"world reset clears owner view and readiness");
        }
        System.out.println("ConstructionPriorityNetworkTest: "+checks+" checks PASS; native/mod handlers and private reliable snapshots; real sockets/two computers NOT TESTED.");
    }
}
