package fpv.net;

import fpv.flight.*;
import fpv.generated.FlightProfiles;
import fpv.world.Unit3DState;
import java.util.*;

/** Deterministic local ghost/receipt/replay tests. No socket or physical mouse/gameplay proof. */
public final class LocalPilotPredictionTest {
    static int checks;
    static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static void reject(Runnable r){try{r.run();throw new AssertionError("Invalid input accepted");}catch(IllegalArgumentException expected){checks++;}}
    static FlightController flight(){var f=new FlightController(FlightProfiles.create());f.reset(10,8,-10,0);return f;}
    static NetworkControls input(int mode,double forward,double pitch){return new NetworkControls(true,mode,.62,pitch,0,0,forward,0,0,0,0,0,1,0,true,false,true);}
    static NetProtocol.Flight authority(FlightController f){var s=new FlightController.State();NetProtocol.copy(f.state(),s);return new NetProtocol.Flight(7,9,f.profile(),s);}
    static Unit3DState body(FlightController f){return Unit3DState.fromRenderFlight(9,2,1,f.state(),.3,.2);}
    static PilotCheckpoint receipt(long rev,long seq,double held,int kind,FlightController f){return new PilotCheckpoint(rev,7,9,2,1,seq,held,kind,20,12,.1,.04,f.memory());}
    static byte[] state(FlightController f){return NetProtocol.bytes(o->NetProtocol.state(o,f.state()));}
    public static void main(String[] args){
        var f=flight();for(int i=0;i<33;i++)f.step(.007,.61,.2,-.3,.1);
        var replay=flight();NetProtocol.copy(f.state(),replay.state());replay.restoreMemory(f.memory());
        for(int i=0;i<60;i++){f.step(.011,.65,.1,.2,-.3);replay.step(.011,.65,.1,.2,-.3);}
        check(Arrays.equals(state(f),state(replay)),"Acro full motor/remainder checkpoint replays bit exactly");
        final var checkpointFlight=f;
        reject(()->new FlightController.Memory(Double.NaN,0));reject(()->new FlightController.Memory(0,1.1));reject(()->checkpointFlight.restoreMemory(new FlightController.Memory(.1,.5)));
        f=flight();final var seed=f;var p=new LocalPilotPrediction();var view=flight();var c=input(1,1,0);
        check(!p.step(0,.01,c,null),"no authority means no guessed unit");
        check(p.accept(authority(f),body(f),receipt(1,-1,0,PilotCheckpoint.AIR,f),0,null),"matching complete checkpoint seeds local display");
        p.step(1d/60,1d/60,c,null);p.copy(view);check(view.state().z< -10,"first next frame responds without RTT or100ms interpolation");
        check(!p.accept(authority(f),body(f),receipt(1,-1,0,PilotCheckpoint.AIR,f),.02,null),"duplicate cannot rewind display");
        check(!p.accept(authority(f),body(f),receipt(0,-1,0,PilotCheckpoint.AIR,f),.02,null),"reordered snapshot cannot rewind display");
        check(!p.accept(authority(f),body(f),new PilotCheckpoint(2,8,9,2,1,-1,0,0,20,12,.1,.04,f.memory()),.02,null),"wrong player cannot seed prediction");
        double before=view.state().z;p.step(.8,.01,c,null);p.copy(view);check(view.state().z==before&&!p.fresh(.8),"stale stream stops speculative integration");
        p.clear();check(!p.copy(view)&&p.retainedFrames()==0,"world/disconnect/death clear drops all ghost state");
        p.accept(authority(f),body(f),receipt(3,-1,0,PilotCheckpoint.GROUND,f),1,null);
        var climb=new NetworkControls(true,3,0,0,0,0,1,0,1,0,0,0,1,0,true,false,true);
        p.step(1.02,.02,climb,null);p.copy(view);check(view.state().z< -10&&view.state().y==8,"ground XY responds; boost request/QE cannot invent airborne stance");
        p.accept(authority(f),body(f),receipt(4,-1,0,PilotCheckpoint.FIXED,f),1.03,null);p.step(1.05,.02,c,null);p.copy(view);check(view.state().z== -10,"stationary turret never translates");
        f.state().vy=-.5;p.accept(authority(f),body(f),receipt(5,-1,0,PilotCheckpoint.LANDING,f),1.06,null);p.step(1.08,.02,c,null);p.copy(view);
        check(view.state().y<8&&view.state().z== -10,"landing continues only server-confirmed velocity, ignores movement/shooting/boost request");
        f=flight();p.clear();p.accept(authority(f),body(f),receipt(1,-1,0,0,f),0,null);p.sent(10,0);
        for(int i=1;i<=10;i++)p.step(i*.01,.01,c,null);
        p.accept(authority(f),body(f),receipt(2,10,.02,0,f),.1,null);check(p.replayedSteps()>0,"acknowledged input clock replays only later history");
        p.step(.11,.01,c,null);p.copy(view);check(view.state().z< -10,"reconciliation retains new input response");
        f.state().x=30;p.accept(authority(f),body(f),receipt(3,-1,0,0,f),.12,null);p.copy(view);check(view.state().x==30,"large correction immediately obeys authority");
        p.clear();f=flight();p.accept(authority(f),body(f),receipt(1,-1,0,0,f),0,null);
        LocalPilotPrediction.Contact wall=(s,x,y,z,dt,e)->{if(s.z< -10.01){s.z=-10.01;s.vz=0;}};
        for(int i=1;i<=30;i++)p.step(i*.01,.01,c,wall);p.copy(view);check(view.state().z>= -10.01,"presentation clipping takes precedence over lead and residual smoothing");
        for(int kind=0;kind<=PilotCheckpoint.CREATOR;kind++){
            var receipt=receipt(5,10,.03,kind,f);check(PilotCheckpoint.decode(receipt.encode()).equals(receipt),"receipt round trip "+kind);
            reject(()->PilotCheckpoint.decode(Arrays.copyOf(receipt.encode(),receipt.encode().length+1)));
        }
        reject(()->PilotCheckpoint.decode(new byte[129]));reject(()->new PilotCheckpoint(1,7,9,2,1,0,0,7,20,12,.1,.04,new FlightController.Memory(0,0)));
        p.clear();p.sent(0,0);p.accept(authority(f),body(f),receipt(1,0,0,0,f),0,null);
        for(int i=1;i<=1000;i++){double time=i*.0005;if(i%60==0)p.sent(i,time);p.step(time,.0005,c,null);}
        check(p.retainedFrames()==LocalPilotPrediction.CAPACITY,"history has fixed memory under high render FPS");
        p.accept(authority(f),body(f),receipt(2,0,0,0,f),.5,null);
        check(p.replayedSteps()==0,"history evicted at extreme FPS is not silently replayed as a complete interval");
        reject(()->p.step(Double.NaN,.01,c,null));reject(()->p.step(.5,.2,c,null));
        p.clear();f=flight();p.sent(1,0);p.accept(authority(f),body(f),receipt(1,1,0,0,f),0,null);
        var acro=input(0,0,.4);
        for(int i=1;i<=24;i++)p.step(i*.005,.005,acro,null);
        f.state().x+=.04;p.accept(authority(f),body(f),receipt(2,1,.01,0,f),.12,null);p.copy(view);
        double oldQx=view.state().qx;p.step(.125,.005,acro,null);p.copy(view);
        check(Math.abs(view.state().qx-oldQx)>1e-8,"rotation correction does not filter out new Acro input");
        double norm=view.state().qx*view.state().qx+view.state().qy*view.state().qy+view.state().qz*view.state().qz+view.state().qw*view.state().qw;
        check(Math.abs(norm-1)<1e-9,"smoothed orientation remains normalized");
        p.accept(authority(f),body(f),receipt(3,1,0,0,f),2,null);
        check(!p.step(2.01,.01,acro,null),"freshly received but ancient acknowledged input cannot extend replay horizon");
        p.clear();f=flight();p.sent(1,0);p.accept(authority(f),body(f),receipt(1,1,0,0,f),0,null);
        for(int i=1;i<=30;i++)p.step(i*.005,.005,c,null);p.copy(view);
        var shot=state(view);p.step(.1,.01,c,null);p.copy(view);check(Arrays.equals(shot,state(view)),"backwards local clock cannot replay movement twice");
        var wrongBody=new Unit3DState(9,3,1,10,10,8,0,0,0,0,0,0,8,.3,.2,true);
        check(!p.accept(authority(f),wrongBody,receipt(9,1,0,0,f),.2,null),"mismatched type cannot initialize an old respawn pose");
        p.accept(authority(f),body(f),receipt(10,1,.6,0,f),.21,null);
        check(!p.step(.22,.01,c,null),"server input-timeout hold is never predicted as completed movement");
        p.clear();f=flight();p.sent(1,0);p.accept(authority(f),body(f),receipt(1,1,0,PilotCheckpoint.CREATOR,f),0,null);
        p.step(.01,.01,input(2,1,0),null);p.copy(view);check(view.state().z== -10,"creator Build mode holds like its authoritative controller");
        p.step(.02,.01,climb,null);p.copy(view);check(view.state().y>8,"already authorized creator uses its original vertical control in native mode");
        p.clear();f=flight();p.sent(1,0);p.accept(authority(f),body(f),receipt(1,1,0,0,f),0,null);
        var gentle=input(1,.2,0);for(int i=1;i<=120;i++)p.step(i*.005,.005,gentle,null);p.copy(view);double heldX=view.state().x,heldY=view.state().y,heldZ=view.state().z;
        check(!p.step(.605,.005,gentle,null),"replay lead expiry holds rather than integrates indefinitely");
        p.sent(2,.4);p.accept(authority(f),body(f),receipt(2,2,.01,0,f),.61,null);p.copy(view);
        check(Math.abs(view.state().x-heldX)+Math.abs(view.state().y-heldY)+Math.abs(view.state().z-heldZ)<1e-9&&p.replayedSteps()>0,"short outage recovery preserves visible pose while replay reconciles new ACK");
        var remote=new RemotePoses();remote.accept(new NetProtocol.Snapshot(List.of(body(f)),null),1);f.state().x=12;remote.accept(new NetProtocol.Snapshot(List.of(body(f)),null),1.1);
        check(remote.display(9,1.1).x()==10,"other-unit100ms interpolation is unchanged");
        check(state(seed).length==104,"flight wire state length unchanged; protocol4 remains original");
        System.out.println("LocalPilotPredictionTest: "+checks+" memory/receipt/authority/replay/bounds/contact/modes checks PASS; no socket proof");
    }
}
