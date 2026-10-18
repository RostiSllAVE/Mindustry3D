package fpv.net;

import mindustry.Vars;
import mindustry.gen.*;
import mindustry.content.UnitTypes;
import mindustry.game.Team;
import mindustry.net.NetConnection;
import java.util.*;
import static fpv.net.ConnectionNativeHandlersTest.*;

/** Real server send handler/native content, in-memory connection, no socket or physical fog/UI proof. */
public final class SnapshotNativeBatchTest {
    static int checks;
    static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    static final class Con extends NetConnection {
        final List<byte[]> packets=new ArrayList<>();boolean allReliable=true;
        Con(){super("in-memory");}
        public void send(Object p,boolean reliable){try{packets.add(wire(p));allReliable&=reliable;}catch(Exception e){throw new RuntimeException(e);}}
        public void close(){}
    }
    static final class SelectivelyHidden extends UnitEntity {
        @Override public boolean isSyncHidden(Team team){return team==Team.crux;}
    }
    static Player peer(Fixture f,Team team)throws Exception{
        Player p=Player.create();p.team(team);p.con=new Con();
        call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.HELLO,0,1,o->o.writeUTF(f.hash)));return p;
    }
    static NetProtocol.Snapshot received(Con con){
        var a=new NetProtocol.Assembly();NetProtocol.Snapshot result=null;
        for(byte[] bytes:con.packets){var p=NetProtocol.decode(bytes);if(p.kind()!=NetProtocol.CHANNEL)continue;
            var c=NetProtocol.chunk(p.payload());if(!c.channel().equals("poses"))continue;
            byte[] complete=a.accept(c,1);if(complete!=null)result=PoseSnapshotCodec.decode(complete);
        }return result;
    }
    public static void main(String[] args)throws Exception{
        try(var f=new Fixture()){
            Groups.unit.clear();flag("active",true);flag("server",true);
            for(int i=0;i<1000;i++){var u=UnitTypes.flare.create(Team.sharded);u.set(24+(i%25)*8,24+(i/25)*4);u.add();}
            var hidden=new SelectivelyHidden();hidden.setType(UnitTypes.flare);hidden.team(Team.sharded);hidden.set(80,80);hidden.add();
            Player a=peer(f,Team.sharded),b=peer(f,Team.crux);var d=((NetworkDiagnosticSession)get(f.net,"diagnostic")).data;d.enabled(true);
            call(f.net,"sendSnapshots",new Class[]{});
            var sa=received((Con)a.con);var sb=received((Con)b.con);
            check(sa!=null&&sb!=null,"complete compact frames reach both native recipients");
            check(sa.units().size()==1001&&sb.units().size()==1000,"visibility remains per-recipient, not shared from another team");
            check(sa.units().stream().anyMatch(s->s.unitId()==hidden.id)&&sb.units().stream().noneMatch(s->s.unitId()==hidden.id),"hidden state never leaks through query reuse");
            check(d.value(NetworkDiagnostics.Counter.POSE_QUERIES)==1001,"one physical query per union-visible unit");
            check(d.value(NetworkDiagnostics.Counter.POSE_QUERY_REUSE)==1000,"second recipient reuses immutable poses");
            check(((Con)a.con).allReliable&&((Con)b.con).allReliable,"snapshot delivery still reliable");
            check(((Map<?,?>)get(f.net,"poseFrame")).isEmpty(),"batch releases unit references after send");
            long before=d.value(NetworkDiagnostics.Counter.POSE_QUERIES),reuse=d.value(NetworkDiagnostics.Counter.POSE_QUERY_REUSE);
            arc.Events.fire(new mindustry.game.EventType.PlayerLeave(b));call(f.net,"sendSnapshots",new Class[]{});
            check(d.value(NetworkDiagnostics.Counter.POSE_QUERIES)-before==1001&&d.value(NetworkDiagnostics.Counter.POSE_QUERY_REUSE)==reuse,"single recipient avoids cache work");
        }
        System.out.println("SnapshotNativeBatchTest: "+checks+" actual native send/visibility/reliable/cache checks PASS; in-memory provider/fixture digest, no socket proof");
    }
}
