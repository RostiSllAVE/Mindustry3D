package fpv.net;

import arc.util.Log;
import fpv.flight.*;
import fpv.generated.FlightProfiles;
import fpv.world.Unit3DState;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.content.UnitTypes;
import mindustry.game.Team;
import java.util.*;
import static fpv.net.ConnectionNativeHandlersTest.*;

/** Added read-only native collision-query cost in a dense synthetic 40x40 scene; not gameplay FPS. */
public final class PilotContactBenchmark {
    public static void main(String[] args)throws Exception{
        Log.logger=(level,message)->{};
        var allocation=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();allocation.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().getId();
        System.out.println("nativeUnits,cached,batchContacts,batchMeanUs,batchP95Us,allocatedBytesPerBatch");
        for(int n:new int[]{100,1000,4096})try(var fixture=new Fixture()){
            Groups.unit.clear();Groups.resize(0,0,320,320);
            for(int i=0;i<n;i++){var u=UnitTypes.flare.create(Team.crux);u.set((float)(1+i%64*.5)*8,(float)(1+i/64*.5)*8);u.add();Groups.unit.tree().insert(u);}
            var pilot=UnitTypes.flare.create(Team.sharded);pilot.set(80,80);
            var contact=new PredictionContact(fixture.net.fixtureWorld());contact.configure(pilot,new Unit3DState(pilot.id,pilot.type.id,pilot.team.id,10,10,8,0,0,0,90,0,0,8,.3,.2,true),PilotCheckpoint.AIR);
            var state=new FlightController(FlightProfiles.create()).state();
            for(int contacts:new int[]{1,48})for(boolean cached:new boolean[]{false,true}){
            for(int i=0;i<100;i++){if(cached)contact.begin();for(int j=0;j<contacts;j++){state.x=10.02;state.y=8;state.z=-10;state.vx=2;contact.afterStep(state,10,8,-10,.01,0);}if(cached)contact.end();}
            double[] time=new double[200];long bytes=0;
            for(int i=0;i<time.length;i++){long a=allocation.getThreadAllocatedBytes(thread),t=System.nanoTime();if(cached)contact.begin();for(int j=0;j<contacts;j++){state.x=10.02;state.y=8;state.z=-10;state.vx=2;contact.afterStep(state,10,8,-10,.01,0);}if(cached)contact.end();time[i]=(System.nanoTime()-t)/1000d;bytes+=allocation.getThreadAllocatedBytes(thread)-a;}
            double mean=Arrays.stream(time).average().orElse(0);Arrays.sort(time);
            System.out.printf(Locale.ROOT,"%d,%s,%d,%.3f,%.3f,%d%n",n,cached,contacts,mean,time[189],bytes/time.length);
            }
        }
    }
}
