import fpv.net.*;
import fpv.flight.*;
import fpv.generated.FlightProfiles;
import fpv.world.Unit3DState;
import java.util.*;

/** Deterministic controller/receipt MODEL, not TCP/Steam or two-computer gameplay.
 * Loss drops whole completed frames/inputs; real reliable snapshot loss retransmits/queues instead. */
public final class PilotPredictionBenchmark {
    static final double DT=1d/120,DURATION=20,ONSET=1.037;
    static volatile Object visibleBody;
    static final LocalPilotPrediction.Contact FLOOR=(s,x,y,z,dt,e)->{if(s.y<.3){s.y=.3;s.vy=Math.max(0,s.vy);}};
    static final com.sun.management.ThreadMXBean ALLOC=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
    record Input(double at,long sequence,NetworkControls controls){}
    record Sample(double at,long revision,NetProtocol.Snapshot snapshot,PilotCheckpoint receipt){}
    static FlightController flight(){var f=new FlightController(FlightProfiles.create());f.reset(10,30,-10,0);return f;}
    static NetworkControls controls(String mode,double t){
        boolean on=t>=ONSET&&t<17;double f=on?Math.sin((t-ONSET)*.8)*.3+.7:0,h=on?Math.sin((t-ONSET)*.5)*.4:0;
        int m=mode.equals("ACRO")?0:mode.equals("BUILD")?2:mode.equals("GROUND")||mode.equals("WALKER")?3:1;
        if(mode.equals("BOOST")&&t<ONSET+.4)f=0;
        return new NetworkControls(true,m,.62,on?.15*Math.cos((t-ONSET)*.3):0,0,on?.06:0,f,0,mode.equals("BUILD")&&on?.1:0,0,h,0,1,0,false,false,mode.equals("BOOST")&&t>=ONSET);
    }
    static int kind(String mode,double t){return mode.equals("GROUND")||mode.equals("WALKER")||mode.equals("BOOST")&&t<1.25?PilotCheckpoint.GROUND:PilotCheckpoint.AIR;}
    static void serverStep(FlightController f,String mode,double t,NetworkControls c){
        int kind=kind(mode,t);var s=f.state();var p=f.profile();
        if(kind==PilotCheckpoint.GROUND){
            double speed=20/p.metersPerTile,h=c.headingRadians(),n=Math.max(1,Math.hypot(c.forward(),c.right()));
            double tx=(c.forward()*Math.sin(h)+c.right()*Math.cos(h))*speed/n,tz=(-c.forward()*Math.cos(h)+c.right()*Math.sin(h))*speed/n;
            double dx=tx-s.vx,dz=tz-s.vz,len=Math.hypot(dx,dz),blend=len>0?Math.min(1,.1*Math.hypot(tx,tz)*(DT*2)*60/len):0;
            s.vx+=dx*blend;s.vz+=dz*blend;s.x+=s.vx*DT*2;s.z+=s.vz*DT*2;s.vx*=1-.04*DT*2*60;s.vz*=1-.04*DT*2*60;
            if(Math.hypot(tx,tz)>1e-6){double heading=Math.atan2(tx,-tz);s.qx=s.qz=0;s.qy=Math.sin(-heading*.5);s.qw=Math.cos(heading*.5);}
            if(mode.equals("WALKER"))s.y=30+Math.sin(t*.6)*.4;return;
        }
        if(c.mode()==0)f.step(DT*2,c.throttle(),c.pitch(),c.roll(),c.yaw(),FLOOR);
        else if(c.mode()==2){double x=s.x,y=s.y,z=s.z;new BuildNavigator(p.metersPerTile).step(s,DT*2,c.forward(),c.right(),c.climb(),c.turn(),12);FLOOR.afterStep(s,x,y,z,DT*2,0);}
        else new StabilizedFlight().step(s,p,DT*2,c.forward(),c.right(),c.climb(),20,c.headingRadians(),FLOOR);
    }
    static double delay(Random r,int rtt,boolean impaired){return Math.max(0,rtt/2000d+(impaired?(r.nextDouble()*2-1)*.015:0));}
    static double dist(FlightController.State a,FlightController.State b){return Math.sqrt(Math.pow(a.x-b.x,2)+Math.pow(a.y-b.y,2)+Math.pow(a.z-b.z,2));}
    static double angle(FlightController.State a,FlightController.State b){return Math.toDegrees(2*Math.acos(Math.min(1,Math.abs(a.qx*b.qx+a.qy*b.qy+a.qz*b.qz+a.qw*b.qw))));}
    static double p95(List<Double> values){double[] a=values.stream().mapToDouble(x->x).sorted().toArray();return a.length==0?0:a[(int)Math.ceil(a.length*.95)-1];}
    static double mean(List<Double> values){return values.stream().mapToDouble(x->x).average().orElse(0);}
    public static void main(String[] args){
        ALLOC.setThreadAllocatedMemoryEnabled(true);
        System.out.println("mode,rttMs,link,baselineMovementMs,predictedMovementMs,baselinePositionMean,predictedPositionMean,predictedPositionP95,baselineAngleMean,predictedAngleMean,predictedAngleP95,correctionsPerSecond,maxCorrection,hardCorrections,baselineStepP95,predictedStepP95,baselineAngleStepP95,predictedAngleStepP95,baselineCpuMeanUs,predictionCpuMeanUs,predictionCpuP95Us,baselineAllocatedBytesPerFrame,predictionAllocatedBytesPerFrame,maxHistoryFrames,completeFramesDropped,inputsDropped,predictionHoldFrames,baselineMaxStep,predictedMaxStep,baselineDeltaChangeP95,predictedDeltaChangeP95,predictedMaxDeltaChange");
        for(String mode:new String[]{"ACRO","STABILIZED","BUILD","GROUND","WALKER","BOOST"})for(int rtt:new int[]{0,50,100,200,400})for(boolean impaired:new boolean[]{false,true})run(mode,rtt,impaired);
    }
    static void run(String mode,int rtt,boolean impaired){
        var random=new Random(2505+rtt+mode.hashCode());var input=new PriorityQueue<Input>(Comparator.comparingDouble(Input::at));var samples=new PriorityQueue<Sample>(Comparator.comparingDouble(Sample::at));
        var server=flight();var predicted=flight();var baseline=flight();var previousBaseline=flight();var previousPredicted=flight();var remote=new RemotePoses();var prediction=new LocalPilotPrediction();
        NetworkControls held=controls(mode,0);long accepted=-1,applied=-1,revision=0,sent=0;double heldSeconds=0,baselineLatency=-1,predictedLatency=-1,maxCorrection=0;
        var oldPos=new ArrayList<Double>();var newPos=new ArrayList<Double>();var oldAngle=new ArrayList<Double>();var newAngle=new ArrayList<Double>();var oldStep=new ArrayList<Double>();var newStep=new ArrayList<Double>();var oldTurn=new ArrayList<Double>();var newTurn=new ArrayList<Double>();var cpu=new ArrayList<Double>();
        var oldChange=new ArrayList<Double>();var newChange=new ArrayList<Double>();double oldDx=0,oldDy=0,oldDz=0,newDx=0,newDy=0,newDz=0;
        long tid=Thread.currentThread().getId(),baselineNanos=0,predictionNanos=0,baselineBytes=0,predictionBytes=0;int measured=0,history=0,inputsDropped=0,framesDropped=0,fallback=0;
        for(int tick=0;tick<=DURATION/DT;tick++){
            double t=tick*DT;var current=controls(mode,t);
            if(tick%4==0){prediction.sent(sent,t);if(impaired&&random.nextDouble()<.05)inputsDropped++;else input.add(new Input(t+delay(random,rtt,impaired),sent,current));sent++;}
            while(!input.isEmpty()&&input.peek().at<=t){var c=input.remove();if(c.sequence>accepted){held=c.controls;accepted=c.sequence;}}
            if(tick%2==0){if(accepted!=applied){applied=accepted;heldSeconds=0;}heldSeconds+=DT*2;serverStep(server,mode,t,held);}
            if(tick%12==0){
                var state=new FlightController.State();NetProtocol.copy(server.state(),state);var f=new NetProtocol.Flight(7,9,server.profile(),state);var body=Unit3DState.fromRenderFlight(9,2,1,state,.3,.2);
                var receipt=new PilotCheckpoint(++revision,7,9,2,1,applied,heldSeconds,kind(mode,t),20,12,.1,.04,server.memory());
                // Exercise the same actual compact codec and bounded adjunct; never use mutable server state in a sample.
                var frame=PoseSnapshotCodec.decode(PoseSnapshotCodec.encode(new NetProtocol.Snapshot(List.of(body),f)));var r=PilotCheckpoint.decode(receipt.encode());
                if(impaired&&random.nextDouble()<.05)framesDropped++;
                else {double at=t+delay(random,rtt,impaired);samples.add(new Sample(at,revision,frame,r));if(impaired&&random.nextDouble()<.03)samples.add(new Sample(at+.02,revision,frame,r));}
            }
            long sampleNanos=0,sampleBytes=0,oldSampleNanos=0,oldSampleBytes=0;
            while(!samples.isEmpty()&&samples.peek().at<=t){var s=samples.remove();long a=ALLOC.getThreadAllocatedBytes(tid),n=System.nanoTime();
                prediction.accept(s.snapshot.local(),s.snapshot.units().get(0),s.receipt,t,FLOOR);sampleNanos+=System.nanoTime()-n;sampleBytes+=ALLOC.getThreadAllocatedBytes(tid)-a;
                // Legacy interpolation also accepts only a fresh complete revision, as native Assembly does.
                if(s.revision>baselineRevision){a=ALLOC.getThreadAllocatedBytes(tid);n=System.nanoTime();remote.accept(s.snapshot,t);oldSampleNanos+=System.nanoTime()-n;oldSampleBytes+=ALLOC.getThreadAllocatedBytes(tid)-a;baselineRevision=s.revision;}
                maxCorrection=Math.max(maxCorrection,prediction.correctionMagnitude());
            }
            long a=ALLOC.getThreadAllocatedBytes(tid),n=System.nanoTime();remote.copyFlight(baseline,t);visibleBody=remote.display(9,t);long oldNs=System.nanoTime()-n;long oldBytes=ALLOC.getThreadAllocatedBytes(tid)-a;
            a=ALLOC.getThreadAllocatedBytes(tid);n=System.nanoTime();
            if(!prediction.step(t,DT,current,FLOOR)&&t>=.5)fallback++;
            if(prediction.copy(predicted))visibleBody=prediction.body(remote.latest(9));
            else if(remote.latestFlight()!=null){predicted.configureProfile(remote.latestFlight().profile());NetProtocol.copy(remote.latestFlight().state(),predicted.state());visibleBody=remote.latest(9);}
            long newNs=System.nanoTime()-n;long newBytes=ALLOC.getThreadAllocatedBytes(tid)-a;
            double onset=ONSET+(mode.equals("BOOST")?.4:0);
            if(t>=onset){
                if(baselineLatency<0&&Math.hypot(baseline.state().x-10,baseline.state().z+10)>1e-4)baselineLatency=(t-onset)*1000;
                if(predictedLatency<0&&Math.hypot(predicted.state().x-10,predicted.state().z+10)>1e-4)predictedLatency=(t-onset)*1000;
            }
            if(t>=2){
                oldPos.add(dist(server.state(),baseline.state()));newPos.add(dist(server.state(),predicted.state()));oldAngle.add(angle(server.state(),baseline.state()));newAngle.add(angle(server.state(),predicted.state()));
                baselineNanos+=oldNs+oldSampleNanos;predictionNanos+=newNs+sampleNanos;baselineBytes+=oldBytes+oldSampleBytes;predictionBytes+=newBytes+sampleBytes;cpu.add((newNs+sampleNanos)/1000d);measured++;
            }
            double ox=baseline.state().x-previousBaseline.state().x,oy=baseline.state().y-previousBaseline.state().y,oz=baseline.state().z-previousBaseline.state().z;
            double nx=predicted.state().x-previousPredicted.state().x,ny=predicted.state().y-previousPredicted.state().y,nz=predicted.state().z-previousPredicted.state().z;
            if(t>=.5){oldStep.add(dist(previousBaseline.state(),baseline.state()));newStep.add(dist(previousPredicted.state(),predicted.state()));oldTurn.add(angle(previousBaseline.state(),baseline.state()));newTurn.add(angle(previousPredicted.state(),predicted.state()));
                oldChange.add(Math.sqrt(Math.pow(ox-oldDx,2)+Math.pow(oy-oldDy,2)+Math.pow(oz-oldDz,2)));newChange.add(Math.sqrt(Math.pow(nx-newDx,2)+Math.pow(ny-newDy,2)+Math.pow(nz-newDz,2)));}
            oldDx=ox;oldDy=oy;oldDz=oz;newDx=nx;newDy=ny;newDz=nz;
            NetProtocol.copy(baseline.state(),previousBaseline.state());NetProtocol.copy(predicted.state(),previousPredicted.state());history=Math.max(history,prediction.retainedFrames());
        }
        System.out.printf(Locale.ROOT,"%s,%d,%s,%.3f,%.3f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.3f,%.6f,%d,%.6f,%.6f,%.6f,%.6f,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f%n",
            mode,rtt,impaired?"jitter15ms_loss5pct":"fixed",baselineLatency,predictedLatency,mean(oldPos),mean(newPos),p95(newPos),mean(oldAngle),mean(newAngle),p95(newAngle),prediction.corrections()/DURATION,maxCorrection,prediction.hardCorrections(),p95(oldStep),p95(newStep),p95(oldTurn),p95(newTurn),baselineNanos/1000d/measured,predictionNanos/1000d/measured,p95(cpu),baselineBytes/measured,predictionBytes/measured,history,framesDropped,inputsDropped,fallback,oldStep.stream().mapToDouble(x->x).max().orElse(0),newStep.stream().mapToDouble(x->x).max().orElse(0),p95(oldChange),p95(newChange),newChange.stream().mapToDouble(x->x).max().orElse(0));
        baselineRevision=0;
    }
    static long baselineRevision;
}
