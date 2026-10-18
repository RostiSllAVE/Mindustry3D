package fpv.net;

import arc.Core;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.game.Team;
import fpv.flight.*;
import fpv.generated.FlightProfiles;
import fpv.terrain.TerrainElevation;
import fpv.units.UnitAltitudeSystem;
import fpv.world.*;
import static fpv.net.ConnectionNativeHandlersTest.*;
import java.util.*;

/** Native FpvNetwork handler and entity boundaries, explicit in-memory fixture identity only. */
public final class PredictionNativeIntegrationTest {
    static int checks;
    static void verify(boolean b,String text){checks++;if(!b)throw new AssertionError(text);}
    static void receipt(Fixture f,long epoch,int kind)throws Exception{
        var own=((RemotePoses)get(f.net,"remote")).latestFlight();var u=Vars.player.unit();
        var r=new PilotCheckpoint(f.revision-1,Vars.player.id,u.id,u.type.id,u.team.id,-1,0,kind,20,12,u.type.accel,u.drag,new FlightController.Memory(0,0));
        f.receive(NetProtocol.encode(NetProtocol.STATE,epoch,100,out->out.write(r.encode())));
    }
    public static void main(String[] args)throws Exception{
        try(var f=new Fixture()){
            f.connect();f.world();f.ack(10);f.unit(UnitTypes.flare);f.synchronize(10);
            verify(f.net.allowed(),"A2-02 READY requires no optional prediction receipt");
            receipt(f,10,PilotCheckpoint.AIR);
            var u=Vars.player.unit();float x=u.x,y=u.y,hp=u.health,vx=u.vel.x,vy=u.vel.y;int carried=u.stack.amount,bullets=Groups.bullet.size();
            float[] reload=new float[u.mounts.length];for(int i=0;i<reload.length;i++)reload[i]=u.mounts[i].reload;
            boolean shooting=Vars.player.shooting;int packets=f.provider.sent.size();var controller=u.controller();
            var display=new FlightController(FlightProfiles.create());var c=LocalPilotPredictionTest.input(1,1,0);
            f.net.predictLocalFlight(display,c,.016);Thread.sleep(3);f.net.predictLocalFlight(display,c,.016);
            verify(f.net.presentationBody(u)!=null,"local rendering receives a separate ghost");
            verify(display.state().z< -10,"current input moves ghost before native authority changes");
            verify(u.x==x&&u.y==y&&u.health==hp&&u.vel.x==vx&&u.vel.y==vy&&u.stack.amount==carried,"prediction never writes XYZ authority/HP/velocity/inventory");
            boolean sameReload=true;for(int i=0;i<reload.length;i++)sameReload&=u.mounts[i].reload==reload[i];
            verify(sameReload&&Groups.bullet.size()==bullets,"no duplicated bullet or reload execution during replay");
            verify(Vars.player.shooting==shooting&&u.controller()==controller&&f.provider.sent.size()==packets,"no duplicated shot/AI/control or network action execution");
            Core.settings.put(FpvNetwork.PREDICTION_SETTING,false);verify(f.net.presentationBody(u)==null,"A/B toggle hides ghost immediately");
            Core.settings.put(FpvNetwork.PREDICTION_SETTING,true);
            f.unit(UnitTypes.dagger);verify(f.net.presentationBody(Vars.player.unit())==null,"possession cannot leak previous pilot pose");
            f.poses(10);f.tick();receipt(f,10,PilotCheckpoint.GROUND);f.net.predictLocalFlight(display,c,.016);
            verify(f.net.presentationBody(Vars.player.unit())!=null,"ground owner restores separately without reconnect");
            f.world();verify(!f.net.allowed()&&f.net.presentationBody(Vars.player.unit())==null,"sector/world transition drops prediction and readiness");
            f.ack(11);f.synchronize(11);receipt(f,11,PilotCheckpoint.GROUND);f.net.predictLocalFlight(display,c,.016);
            verify(f.net.presentationBody(Vars.player.unit())!=null,"new epoch can initialize a fresh ground ghost");
            Vars.player.unit().dead=true;verify(f.net.presentationBody(Vars.player.unit())==null,"death hides ghost immediately");
        }
        try(var f=new Fixture()){
            var terrain=new TerrainElevation();var altitude=new UnitAltitudeSystem(terrain::heightAt);
            var world=new World3DAdapter(terrain,(x,z)->{var tile=Vars.world.tile((int)Math.round(x),(int)Math.round(-z));return tile!=null&&tile.block().solid?2:0;},altitude);
            Vars.world.tile(12,10).setBlock(Blocks.copperWall,Team.crux);var wall=Vars.world.tile(12,10).build;float hp=wall.health;
            var clipping=new PredictionContact(world);var u=UnitTypes.flare.create(Team.sharded);u.set(80,80);
            var flight=new FlightController(FlightProfiles.create());flight.reset(14,1,-10,0);flight.state().vx=40;
            clipping.configure(u,new Unit3DState(u.id,u.type.id,u.team.id,10,10,1,0,0,0,0,0,0,1,.3,.2,true),PilotCheckpoint.AIR);
            clipping.afterStep(flight.state(),10,1,-10,.1,0);
            verify(flight.state().x<12&&wall.health==hp,"real native solid clips air ghost without wall damage");
            var mech=UnitTypes.dagger.create(Team.sharded);mech.set(80,80);flight.reset(14,.2,-10,0);flight.state().vx=40;
            clipping.configure(mech,new Unit3DState(mech.id,mech.type.id,mech.team.id,10,10,.2,0,0,0,0,0,0,.2,.3,.2,false),PilotCheckpoint.GROUND);
            clipping.afterStep(flight.state(),10,.2,-10,.1,0);
            verify(flight.state().x==10&&wall.health==hp,"native ground passability conservatively rejects wall crossing; no ram/target impulse");
            Vars.world.tile(12,10).setBlock(Blocks.air);Groups.resize(0,0,320,320);
            var enemy=UnitTypes.flare.create(Team.crux);enemy.set(104,80);enemy.add();Groups.unit.tree().clear();Groups.unit.tree().insert(enemy);
            float targetHp=enemy.health;var target=new Unit3DState(enemy.id,enemy.type.id,enemy.team.id,13,10,1,0,0,0,0,0,0,1,.3,.2,true);
            altitude.setExternalStateProvider(v->v==enemy?target:null);
            flight.reset(14,1,-10,0);flight.state().vx=40;
            clipping.configure(u,new Unit3DState(u.id,u.type.id,u.team.id,10,10,1,0,0,0,0,0,0,1,.3,.2,true),PilotCheckpoint.AIR);
            clipping.afterStep(flight.state(),10,1,-10,.1,0);
            verify(flight.state().x<13&&enemy.x==104&&enemy.health==targetHp&&enemy.vel.isZero(),"unit contact clips only ghost; target HP and momentum untouched");
            altitude.setExternalStateProvider(v->v==enemy?new Unit3DState(enemy.id,enemy.type.id,enemy.team.id,13,10,10,0,0,0,0,0,0,10,.3,.2,true):null);
            flight.reset(14,1,-10,0);flight.state().vx=40;clipping.afterStep(flight.state(),10,1,-10,.1,0);
            verify(flight.state().x==14,"native indexed unit on another Z does not block the ghost");
            clipping.begin();flight.reset(14,1,-10,0);flight.state().vx=40;clipping.afterStep(flight.state(),10,1,-10,.1,0);
            verify(!((Map<?,?>)get(clipping,"poses")).isEmpty(),"read-only collision states are reused within one visual frame");
            clipping.end();verify(((Map<?,?>)get(clipping,"poses")).isEmpty(),"collision cache releases all unit references at batch end");
        }
        try(var f=new Fixture()){
            flag("active",true);flag("server",true);Time.delta=1;call(f.net,"resetWorld",new Class[]{});
            var p=Player.create();p.team(Team.sharded);p.con=new Con();var u=UnitTypes.flare.create(Team.sharded);u.set(80,80);u.add();p.unit(u);p.add();
            call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.HELLO,0,1,out->out.writeUTF(f.hash)));
            long epoch=f.net.fixtureEpoch();
            final long worldEpoch=epoch;
            call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.INPUT,worldEpoch,20,out->NetProtocol.controls(out,LocalPilotPredictionTest.input(1,1,0))));
            f.tick();
            ((Con)p.con).sent.clear();call(f.net,"sendSnapshots",new Class[]{});
            PilotCheckpoint r=null;long poseRevision=-1;var a=new NetProtocol.Assembly();NetProtocol.Snapshot frame=null;
            for(Object o:((Con)p.con).sent){var e=NetProtocol.decode(wire(o));
                if(e.kind()==NetProtocol.STATE)r=PilotCheckpoint.decode(e.payload());
                else if(e.kind()==NetProtocol.CHANNEL){var ch=NetProtocol.chunk(e.payload());if(!ch.channel().equals("poses"))continue;poseRevision=ch.revision();byte[] b=a.accept(ch,1);if(b!=null)frame=PoseSnapshotCodec.decode(b);}
            }
            verify(r!=null&&frame!=null&&r.revision()==poseRevision,"server receipt pairs only with same compact full pose revision");
            verify(r.inputSequence()==20&&r.heldSeconds()>0&&r.unitId()==u.id&&r.playerId()==p.id,"receipt echoes only applied authenticated input and duration");
            int rejected=f.net.rejectedPackets();
            call(f.net,"receiveServer",new Class[]{Player.class,byte[].class},p,NetProtocol.encode(NetProtocol.STATE,worldEpoch,21,out->out.write(new byte[1])));
            verify(f.net.rejectedPackets()==rejected+1,"client cannot submit server-only prediction checkpoint");
        }
        System.out.println("PredictionNativeIntegrationTest: "+checks+" native READY/identity/epoch/receipt/render-only/authority checks PASS; no socket or physical gameplay proof");
    }
}
