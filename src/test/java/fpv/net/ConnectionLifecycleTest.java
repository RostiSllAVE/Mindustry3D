package fpv.net;

import java.util.Set;
import arc.util.serialization.Jval;

public final class ConnectionLifecycleTest {
    static int checks;
    static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    public static void main(String[] args){
        var c=new ConnectionLifecycle();var required=Set.of("layered-world","ground-profiles");
        c.begin(0,required);c.acknowledged(1,1);c.tick(1,false,true,true);
        check(!c.ready()&&!c.acceptsState(),"ACK before native world cannot unlock FPV");
        c.worldLoaded(2);c.tick(2,true,true,true);check(c.stage()==ConnectionLifecycle.Stage.SYNCHRONIZING,"world and ACK alone insufficient");
        c.poses();c.applied("ground-profiles");c.tick(3,true,true,true);check(!c.ready()&&c.missing()==1,"one missing channel blocks READY");
        c.applied("layered-world");c.tick(4,true,true,false);check(c.stage()==ConnectionLifecycle.Stage.WAITING_FOR_UNIT,"missing own pose blocks READY");
        c.tick(5,true,true,true);check(c.ready()&&c.readyMs()==5000,"all successful state plus own pose reaches READY");
        c.acknowledged(1,6);check(c.ready(),"duplicate same-epoch ACK preserves readiness");
        c.tick(7,true,false,false);check(c.initialStateComplete()&&!c.ready(),"death/spectator is not false FPV readiness");
        c.tick(600,true,false,false);check(c.failure()==ConnectionLifecycle.Failure.NONE,"dead player need not time out a valid world");
        c.tick(601,true,true,true);check(c.ready(),"respawn can become ready without reconnecting");
        c.acknowledged(2,602);check(!c.ready()&&!c.acceptsState()&&c.missing()==2,"new epoch invalidates old world and applied state");
        c.tick(603,false,true,false);check(c.failure()==ConnectionLifecycle.Failure.NONE,"new epoch before WorldLoad gets a fresh loading budget");
        check(c.json().contains("POSES"),"missing initial pose snapshot has a clear diagnostic label");
        c.resetWorld(1000,required);c.tick(1001,false,true,false);check(c.failure()==ConnectionLifecycle.Failure.NONE,"new world has its own native loading budget");
        c.worldLoaded(1002);c.acknowledged(3,1003);c.poses();c.applied("ground-profiles");c.tick(1003,true,true,true);
        c.tick(1049,true,true,true);check(c.failure()==ConnectionLifecycle.Failure.INITIAL_STATE_TIMEOUT,"missing channel cannot wait forever");
        c.resetWorld(1050,required);c.offline();check(c.stage()==ConnectionLifecycle.Stage.FAILED,"disconnect/reset retain actionable failure until new attempt");
        c.begin(2000,required);check(c.failure()==ConnectionLifecycle.Failure.NONE&&c.missing()==2,"new attempt clears failure and all ready marks");
        c.worldLoaded(2001);c.tick(2017,true,true,false);check(c.failure()==ConnectionLifecycle.Failure.HANDSHAKE_TIMEOUT,"missing ACK has useful bounded timeout");
        c.begin(3000,required);c.tick(3121,false,false,false);check(c.failure()==ConnectionLifecycle.Failure.NATIVE_WORLD_TIMEOUT,"native world delay separately classified");
        c.begin(4000,Set.of());c.worldLoaded(4000);c.acknowledged(5,4000);c.poses();c.tick(4001,true,true,false);c.tick(4047,true,true,false);
        check(c.failure()==ConnectionLifecycle.Failure.OWN_POSE_TIMEOUT,"missing live own pose is bounded");
        c.begin(5000,required);c.fail(ConnectionLifecycle.Failure.STATE_APPLY_FAILED,"PRIVATE@USER C:\\secret");
        String json=c.json();check(!json.contains("PRIVATE")&&!json.contains("secret")&&!c.message().contains("PRIVATE"),"failure output cannot expose arbitrary channel text");
        check(Jval.read(json).getString("failedChannel").equals("OTHER"),"failure uses fixed enum label");
        check(c.message().contains("reconnect"),"state failure gives next action");
        c.begin(5500,required);c.worldLoaded(5500);c.acknowledged(8,5500);c.tick(5500,true,true,false);
        for(int i=1;i<=5;i++){c.progress("layered-world",5500+i*30);c.tick(5500+i*30,true,true,false);check(c.failure()==ConnectionLifecycle.Failure.NONE,"genuine initial transfer progress gets time beyond 45 seconds");}
        c.progress("layered-world",5681);c.tick(5681,true,true,false);check(c.failure()==ConnectionLifecycle.Failure.INITIAL_STATE_TIMEOUT,"progress cannot evade total initial sync deadline");
        for(int i=0;i<20;i++){c.begin(6000+i,required);check(!c.ready()&&c.missing()==2,"repeated join has no stale ready state");c.offline();check(c.stage()==ConnectionLifecycle.Stage.OFFLINE,"ordinary disconnect clears lifecycle");}
        System.out.println("ConnectionLifecycleTest: "+checks+" deterministic readiness/timeout/rejoin/privacy checks PASS");
    }
}
