package fpv.net;

import arc.*;
import arc.mock.*;
import arc.util.I18NBundle;
import fpv.combat.Combat3DSystem;
import fpv.construction.*;
import fpv.defense.*;
import fpv.effects.RadialExplosion;
import fpv.flight.*;
import fpv.generated.FlightProfiles;
import fpv.terrain.TerrainElevation;
import fpv.units.UnitAltitudeSystem;
import fpv.world.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.Team;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.world.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/** Actual FpvNetwork/Arc handlers with native content and an in-memory transport.
 * Fixture verification is explicit: not a real binary handshake, socket or physical UI test. */
public final class ConnectionNativeHandlersTest {
    static int checks;
    static final class App extends MockApplication {
        final List<Runnable> posted=new ArrayList<>();
        @Override public void post(Runnable r){posted.add(r);}
        void drain(){var ready=new ArrayList<>(posted);posted.clear();ready.forEach(Runnable::run);}
    }
    static final class Provider implements Net.NetProvider {
        final List<Object> sent=new ArrayList<>();
        public void connectClient(String a,int p,Runnable r){r.run();}
        public void sendClient(Object o,boolean reliable){sent.add(o);}
        public void disconnectClient(){}
        public void discoverServers(arc.func.Cons<Host> h,Runnable r){}
        public void pingHost(String a,int p,arc.func.Cons<Host> h,arc.func.Cons<Exception> e){}
        public void hostServer(int p){}
        public Iterable<NetConnection> getConnections(){return List.of();}
        public void closeServer(){}
    }
    static final class Client extends NetClient {
        int disconnected;
        @Override public void disconnectQuietly(){disconnected++;try{flag("active",false);}catch(Exception e){throw new RuntimeException(e);}}
    }
    static final class Con extends NetConnection {
        final List<Object> sent=new ArrayList<>();String rejection;
        Con(){super("in-memory");}
        public void send(Object p,boolean reliable){sent.add(p);}
        public void close(){}
        @Override public void kick(String reason,long duration){rejection=reason;kicked=true;}
    }
    static Object get(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void put(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    static Object call(Object o,String name,Class<?>[] signature,Object... args)throws Exception{var m=o.getClass().getDeclaredMethod(name,signature);m.setAccessible(true);try{return m.invoke(o,args);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}}
    static void flag(String key,boolean value)throws Exception{put(Vars.net,key,value);}
    static byte[] wire(Object packet)throws Exception{for(Field f:packet.getClass().getFields())if(f.getType()==byte[].class)return (byte[])f.get(packet);throw new AssertionError("No native custom-packet bytes");}
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static final class Fixture implements AutoCloseable {
        final App app=new App();final Provider provider=new Provider();final Client client;
        final FpvNetwork net;final LayerRuntime layers;final String hash="payload1-"+"a".repeat(64);
        long revision=1;
        Fixture()throws Exception{
            Core.app=app;Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();
            Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new Net(provider);Groups.init();Vars.netServer=new NetServer();Vars.netClient=client=new Client();
            Vars.world=new World();Vars.world.tiles=new Tiles(40,40);Vars.world.tiles.fill();Vars.world.setGenerating(false);Vars.content.init();
            Vars.state.rules.infiniteResources=true;Vars.state.rules.editor=true;Vars.state.set(GameState.State.playing);
            Vars.player=Player.create();Vars.player.team(Team.sharded);unit(UnitTypes.alpha);
            var t=new TerrainElevation();var a=new UnitAltitudeSystem(t::heightAt);var w=new World3DAdapter(t,(x,y)->0,a);var e=new RadialExplosion(w);var c=new Combat3DSystem(w,()->false,u->null);
            net=new FpvNetwork(w,a,c,e,(x,y)->0);net.registerProjection(new ProjectedWallSystem(w,()->false));net.registerForceProjectors(new ForceProjectorSystem(w,()->false));net.install();
            // Only a fixture token, never changed production verification policy.
            put(net,"verification",new ModFingerprint.Verification(hash,null));put(net,"fingerprint",hash);
            layers=new LayerRuntime(w);layers.install(net);
        }
        void unit(mindustry.type.UnitType type){Unit u=type.create(Team.sharded);u.set(80,80);u.add();Vars.player.unit(u);}
        void connect()throws Exception{flag("active",true);flag("server",false);Events.fire(new ClientServerConnectEvent("in-memory",1));}
        void world(){Events.fire(new WorldLoadEvent());}
        void ack(long epoch)throws Exception{receive(NetProtocol.encode(NetProtocol.ACK,epoch,1,out->out.writeUTF(hash)));}
        void receive(byte[] bytes)throws Exception{call(net,"receiveClient",new Class[]{byte[].class},(Object)bytes);}
        void tick()throws Exception{call(net,"before",new Class[]{});}
        void state(String name,byte[] data,long epoch)throws Exception{
            int count=Math.max(1,(data.length+11999)/12000);long rev=revision++;
            for(int i=0;i<count;i++){var chunk=new NetProtocol.Chunk(name,rev,i,count,data.length,Arrays.copyOfRange(data,i*12000,Math.min(data.length,(i+1)*12000)));receive(NetProtocol.encode(NetProtocol.CHANNEL,epoch,revision,out->NetProtocol.chunk(out,chunk)));}
        }
        @SuppressWarnings("unchecked") Map<String,byte[]> initial()throws Exception{
            var result=new LinkedHashMap<String,byte[]>();var channels=(Map<String,Object>)get(net,"channels");
            for(var item:channels.entrySet())result.put(item.getKey(),((Supplier<byte[]>)get(item.getValue(),"serverData")).get());return result;
        }
        void poses(long epoch)throws Exception{
            var u=Vars.player.unit();var f=new FlightController(FlightProfiles.create());f.reset(10,4,-10,0);
            var pose=new Unit3DState(u.id,u.type.id,u.team.id,10,10,4,0,0,0,0,0,0,4,.5,.2,u.type.flying);
            state("poses",PoseSnapshotCodec.encode(new NetProtocol.Snapshot(List.of(pose),new NetProtocol.Flight(Vars.player.id,u.id,f.profile(),f.state()))),epoch);
        }
        void synchronize(long epoch)throws Exception{var values=initial();for(var item:values.entrySet())state(item.getKey(),item.getValue(),epoch);poses(epoch);tick();}
        public void close(){net.close();layers.close();}
    }
    public static void main(String[] args)throws Exception{
        try(var f=new Fixture()){
            int[] calls={0};f.net.registerStateChannel("probe",()->new byte[]{1},bytes->calls[0]++);
            f.connect();f.ack(10);check(!f.net.allowed()&&f.net.receivedSnapshots()==0,"ACK-only never READY");
            f.state("probe",new byte[]{1},10);check(calls[0]==0,"early state cannot mutate an unready native world");
            f.world();f.ack(10);
            Field connecting=NetClient.class.getDeclaredField("connecting");connecting.setAccessible(true);connecting.setBoolean(f.client,true);
            f.state("probe",new byte[]{1},10);check(calls[0]==0,"state arriving before native finish is ignored");
            connecting.setBoolean(f.client,false);put(f.net,"nextHello",0d);f.tick();
            check(NetProtocol.decode(wire(f.provider.sent.get(f.provider.sent.size()-1))).kind()==NetProtocol.HELLO,"early drop requests initial-state replay through existing HELLO");
            f.ack(10);f.state("probe",new byte[]{1},10);check(calls[0]==1,"post-world state applies after re-handshake");
            f.synchronize(10);check(f.net.allowed()&&f.net.connectionStage().equals("READY"),"all native production channels plus own pose allow READY");
            check(Vars.netClient.getBinaryPacketHandlers(FpvNetwork.PACKET).size==1,"one client handler");
            f.ack(10);check(f.net.allowed(),"duplicate ACK does not regress ready session");
            long sequence=(long)get(f.net,"sequence");f.world();check(!f.net.allowed()&&(long)get(f.net,"sequence")==sequence,"world reload blocks and preserves monotonic connection sequence");
            f.ack(11);f.synchronize(11);check(f.net.allowed(),"same process reaches READY in another world");
            var display=new FlightController(FlightProfiles.create());check(f.net.copyLocalFlight(display),"controlled flight display available when ready");
            f.unit(UnitTypes.dagger);f.tick();check(!f.net.allowed(),"new unit waits for its own authoritative pose");f.poses(11);f.tick();check(f.net.allowed(),"ground unit possession recovers without reconnect");
            f.unit(UnitTypes.flare);f.poses(11);f.tick();check(f.net.allowed(),"aircraft pose initial sync");
            for(int i=0;i<5;i++){
                flag("active",false);f.tick();f.connect();check((long)get(f.net,"sequence")==0&&!f.net.allowed(),"fresh connection resets sequence and readiness");f.world();f.ack(100+i);f.synchronize(100+i);check(f.net.allowed(),"rejoin without game restart");
            }
            check(Vars.netClient.getBinaryPacketHandlers(FpvNetwork.PACKET).size==1,"repeated joins do not accumulate handlers");
        }
        check(Vars.netClient.getBinaryPacketHandlers(FpvNetwork.PACKET).isEmpty()&&Vars.netServer.getBinaryPacketHandlers(FpvNetwork.PACKET).isEmpty(),"close removes binary handlers");
        try(var f=new Fixture()){
            Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
            var placed=f.layers.apply(Vars.player,LayerActions.Action.place(16,16,2,"copper-wall",false,1,LayerComponent.Mode.FREE));
            check(placed.accepted(),"host-authorized elevated fixture created: "+placed.reason());
            var serverState=f.initial();f.connect();f.world();check(f.layers.grid().components().isEmpty(),"new client world starts without previous upper data");
            check(!f.net.submitConstruction(LayerActions.Action.remove(placed.componentId())).accepted(),"construction unavailable before full sync");
            f.ack(9);for(var item:serverState.entrySet())f.state(item.getKey(),item.getValue(),9);f.poses(9);f.tick();
            var actual=f.layers.grid().get(placed.componentId());check(f.net.allowed()&&actual!=null&&actual.layer()==2&&actual.rotation()==1,"actual layered-world decoder restores elevated initial state before READY");
            check(Vars.world.tile(16,16).build==null,"upper initial sync does not overwrite ground tile");
        }
        try(var f=new Fixture()){
            int[] attempts={0};f.net.registerStateChannel("broken",()->new byte[]{1},bytes->{if(attempts[0]++==0)throw new IllegalArgumentException("private C:\\path 192.0.2.1");});
            f.connect();f.world();f.ack(20);f.state("broken",new byte[]{1},20);
            check(!f.net.allowed()&&f.net.connectionStage().equals("FAILED"),"failed application never looks connected/READY");
            check(f.net.status().contains("reconnect")&&!f.net.status().contains("192.0.2.1")&&!f.net.status().contains("path"),"actionable privacy-safe state failure");
            f.connect();f.app.drain();check(f.client.disconnected==0,"queued old failure cannot disconnect a new attempt");
            f.world();f.ack(21);f.synchronize(21);check(f.net.allowed()&&attempts[0]==2,"fresh connection reapplies a previously failed channel revision and reaches READY");
        }
        try(var f=new Fixture()){
            f.connect();f.world();byte[] bad=NetProtocol.encode(NetProtocol.ACK,8,1,out->out.writeUTF(f.hash));bad[5]=99;f.receive(bad);
            check(!f.net.allowed()&&f.net.status().contains("protocol"),"wrong protocol rejected with actionable failure");
        }
        try(var f=new Fixture()){
            f.connect();f.world();f.receive(NetProtocol.encode(NetProtocol.ACK,30,1,out->out.writeUTF("payload1-"+"b".repeat(64))));
            check(!f.net.allowed()&&f.net.status().contains("mismatch"),"mismatching server digest fails explicitly");f.app.drain();check(f.client.disconnected==1,"current failed session disconnects once");
        }
        try(var f=new Fixture()){
            flag("active",true);flag("server",true);put(f.net,"epoch",0L);
            Player p=Player.create();p.team(Team.sharded);var con=new Con();p.con=con;
            Unit owned=UnitTypes.flare.create(Team.sharded);owned.set(80,80);owned.add();p.unit(owned);
            var diagnostics=(NetworkDiagnosticSession)get(f.net,"diagnostic");diagnostics.data.enabled(true);
            call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.HELLO,0,1,out->out.writeUTF(f.hash)));
            long epoch=NetProtocol.decode(wire(con.sent.get(0))).epoch();check(epoch!=0,"HELLO before host's first tick receives nonzero epoch");
            var controls=new NetworkControls(true,1,0,0,0,0,1,0,0,0,0,0,1,0,false);
            call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.INPUT,epoch,2,out->NetProtocol.controls(out,controls)));
            var peer=((Map<?,?>)get(f.net,"peers")).get(p);check(((NetworkControls)get(peer,"controls")).forward()==1,"valid input reaches authenticated peer");
            check(diagnostics.data.value(NetworkDiagnostics.Counter.FIRST_INPUT_ACCEPTED)==1&&diagnostics.data.value(NetworkDiagnostics.Counter.FIRST_USABLE_INPUT_ACCEPTED)==1,"host counts first accepted and first valid-owner active input");
            Events.fire(new WorldLoadEvent());check(!((NetworkControls)get(peer,"controls")).active()&&((NetworkControls)get(peer,"controls")).forward()==0,"new world clears old movement intent");
            int before=f.net.acceptedInputs();call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.INPUT,epoch,3,out->NetProtocol.controls(out,controls)));check(f.net.acceptedInputs()==before,"old epoch input rejected after world switch");
            long fresh=(long)get(f.net,"epoch");call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.INPUT,fresh,0,out->NetProtocol.controls(out,controls)));check(f.net.acceptedInputs()==before+1,"new epoch input accepted with reset server watermark");
            Events.fire(new PlayerLeave(p));check(((Map<?,?>)get(f.net,"peers")).isEmpty(),"leave removes authenticated peer");
            p.con=new Con();call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.HELLO,0,0,out->out.writeUTF("wrong")));check(((Con)p.con).kicked,"server rejects mismatched peer");
        }
        System.out.println("ConnectionNativeHandlersTest: "+checks+" actual handler/event checks PASS; in-memory provider and fixture digest, no socket/physical UI proof");
    }
}
