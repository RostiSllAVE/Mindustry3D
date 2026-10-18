package fpv.net;

import fpv.flight.*;
import fpv.world.Unit3DState;
import fpv.generated.NetworkConfig;

/** Local presentation only. No native unit, weapon, inventory, AI or world mutator is reachable here.
 * Bounded input history is replayed from a complete authoritative pose + paired server receipt. */
public final class LocalPilotPrediction {
    public static final int CAPACITY=512;
    public static final double MAX_REPLAY_SECONDS=.6,STALE_SECONDS=.75;
    @FunctionalInterface public interface Contact extends FlightController.StepObserver {}
    private final double[] begin=new double[CAPACITY],end=new double[CAPACITY],sentAt=new double[CAPACITY];
    private final long[] sentSequence=new long[CAPACITY];
    private final NetworkControls[] controls=new NetworkControls[CAPACITY];
    private int at,size,sentHead,sentSize,unitId=-1,typeId=-1,teamId=-1,kind=-1;
    private FlightController simulation;
    private final StabilizedFlight stabilized=new StabilizedFlight();
    private BuildNavigator builder;
    private PilotCheckpoint checkpoint;
    private final FlightController.State display=new FlightController.State(),oldDisplay=new FlightController.State();
    private double lastFrame,lastAuthority,predictUntil,offsetX,offsetY,offsetZ,rotationWeight;
    private double correctionQx,correctionQy,correctionQz,correctionQw=1;
    private long revision=-1;
    private boolean active;
    private double lastError,lastAngle,lastCorrection;
    private int corrections,hardCorrections,replayed;
    public void clear(){
        at=size=sentHead=sentSize=0;java.util.Arrays.fill(controls,null);unitId=typeId=teamId=kind=-1;revision=-1;
        active=false;checkpoint=null;simulation=null;builder=null;lastFrame=lastAuthority=0;offsetX=offsetY=offsetZ=rotationWeight=0;
    }
    public boolean active(int unit,int type,int team){return active&&unitId==unit&&typeId==type&&teamId==team;}
    public boolean fresh(double now){return active&&checkpoint!=null&&checkpoint.heldSeconds()<=NetworkConfig.INPUT_TIMEOUT_SECONDS
        &&now>=lastAuthority&&now-lastAuthority<=STALE_SECONDS&&now<=predictUntil;}
    public double positionError(){return lastError;}
    public double orientationErrorDegrees(){return lastAngle;}
    public double correctionMagnitude(){return lastCorrection;}
    public int corrections(){return corrections;}
    public int hardCorrections(){return hardCorrections;}
    public int replayedSteps(){return replayed;}
    public int retainedFrames(){return size;}
    public void sent(long sequence,double now){
        if(sequence<0||!Double.isFinite(now))throw new IllegalArgumentException("Finite sent input required");
        sentSequence[sentHead]=sequence;sentAt[sentHead]=now;sentHead=(sentHead+1)%CAPACITY;sentSize=Math.min(CAPACITY,sentSize+1);
    }
    public boolean accept(NetProtocol.Flight flight,Unit3DState body,PilotCheckpoint receipt,double now,Contact contact){
        if(flight==null||body==null||receipt==null||!Double.isFinite(now)||receipt.revision()<=revision
            ||flight.playerId()!=receipt.playerId()||flight.unitId()!=receipt.unitId()||body.unitId()!=receipt.unitId()
            ||body.typeId()!=receipt.typeId()||body.teamId()!=receipt.teamId())return false;
        // A short replay-lead expiry is not a possession change; reconcile the held display.
        boolean same=active(receipt.unitId(),receipt.typeId(),receipt.teamId())&&kind==receipt.kind()
            &&now>=lastAuthority&&now-lastAuthority<=STALE_SECONDS;
        if(same)NetProtocol.copy(display,oldDisplay);
        else {at=size=0;java.util.Arrays.fill(controls,null);offsetX=offsetY=offsetZ=rotationWeight=0;}
        if(simulation==null||!same){simulation=new FlightController(flight.profile());builder=new BuildNavigator(flight.profile().metersPerTile);}
        else simulation.configureProfile(flight.profile());
        NetProtocol.copy(flight.state(),simulation.state());simulation.restoreMemory(receipt.memory());
        checkpoint=receipt;unitId=receipt.unitId();typeId=receipt.typeId();teamId=receipt.teamId();kind=receipt.kind();revision=receipt.revision();
        active=true;lastAuthority=now;replayed=0;
        double base=Double.NaN;
        for(int n=0;n<sentSize;n++){int i=(sentHead-1-n+CAPACITY)%CAPACITY;if(sentSequence[i]==receipt.inputSequence()){base=sentAt[i]+receipt.heldSeconds();break;}}
        predictUntil=Double.isFinite(base)?Math.min(now+STALE_SECONDS,base+MAX_REPLAY_SECONDS):now+.1;
        // Never guess an unsent ACK, extrapolate over missing history or replay a multi-second backlog.
        boolean covered=size>0&&base>=begin[(at-size+CAPACITY)%CAPACITY]-1e-6;
        if(same&&covered&&Double.isFinite(base)&&now-base<=MAX_REPLAY_SECONDS&&base<=now+.1){
            for(int n=0;n<size;n++){int i=(at-size+n+CAPACITY)%CAPACITY;double dt=end[i]-Math.max(begin[i],base);
                if(dt>0){integrate(Math.min(.1,dt),controls[i],contact);replayed++;}}
        }
        var s=simulation.state();lastError=same?distance(oldDisplay,s):0;lastAngle=same?angle(oldDisplay,s):0;
        boolean hard=!same||lastError>Math.max(2,body.radius()*2)||kind==PilotCheckpoint.LANDING||kind==PilotCheckpoint.FIXED;
        if(hard){offsetX=offsetY=offsetZ=rotationWeight=0;if(same)hardCorrections++;}
        else {
            offsetX=oldDisplay.x-s.x;offsetY=oldDisplay.y-s.y;offsetZ=oldDisplay.z-s.z;
            // Quaternion correction is left-multiplied on the live orientation: new mouse input is not delayed.
            multiply(oldDisplay.qx,oldDisplay.qy,oldDisplay.qz,oldDisplay.qw,-s.qx,-s.qy,-s.qz,s.qw);
            if(correctionQw<0){correctionQx=-correctionQx;correctionQy=-correctionQy;correctionQz=-correctionQz;correctionQw=-correctionQw;}
            rotationWeight=1;
        }
        lastCorrection=lastError;if(same&&lastError>1e-5)corrections++;if(!same)lastFrame=now;compose(0,contact);return true;
    }
    public boolean step(double now,double dt,NetworkControls input,Contact contact){
        if(!Double.isFinite(now)||!Double.isFinite(dt)||dt<0||dt>.1||input==null)throw new IllegalArgumentException("Bounded prediction frame required");
        if(!fresh(now)){if(active){offsetX=offsetY=offsetZ=rotationWeight=0;}return false;}
        if(now<lastFrame)return false;
        double start=Math.max(lastFrame,now-dt),seconds=Math.min(.1,now-start);
        if(seconds>0){begin[at]=start;end[at]=now;controls[at]=input;at=(at+1)%CAPACITY;size=Math.min(CAPACITY,size+1);integrate(seconds,input,contact);}
        lastFrame=now;compose(seconds,contact);return true;
    }
    public boolean copy(FlightController target){
        if(!active)return false;target.configureProfile(simulation.profile());NetProtocol.copy(display,target.state());return true;
    }
    public Unit3DState body(Unit3DState reference){
        if(reference==null||!active(reference.unitId(),reference.typeId(),reference.teamId()))return null;
        var b=Unit3DState.fromRenderFlight(unitId,typeId,teamId,display,reference.radius(),reference.halfHeight());
        return new Unit3DState(b.unitId(),b.typeId(),b.teamId(),b.x(),b.y(),b.z(),b.vx(),b.vy(),b.vz(),b.yaw(),
            kind==PilotCheckpoint.GROUND?reference.pitch():b.pitch(),kind==PilotCheckpoint.GROUND?reference.roll():b.roll(),
            reference.desiredAltitude(),reference.radius(),reference.halfHeight(),reference.flying());
    }
    private void integrate(double dt,NetworkControls c,Contact contact){
        var s=simulation.state();var p=simulation.profile();
        if(kind==PilotCheckpoint.FIXED)return;
        if(kind==PilotCheckpoint.LANDING){ // Continue only server-confirmed velocity, never decide touchdown/permission.
            double x=s.x,y=s.y,z=s.z;s.x+=s.vx*dt;s.y+=s.vy*dt;s.z+=s.vz*dt;
            if(contact!=null)contact.afterStep(s,x,y,z,dt,0);return;
        }
        if(kind==PilotCheckpoint.GROUND){
            double f=c.active()&&c.mode()!=2?c.forward():0,r=c.active()&&c.mode()!=2?c.right():0,n=Math.max(1,Math.hypot(f,r));
            double speed=checkpoint.speedMps()/p.metersPerTile,h=c.headingRadians();
            double tx=(f*Math.sin(h)+r*Math.cos(h))*speed/n,tz=(-f*Math.cos(h)+r*Math.sin(h))*speed/n;
            double dx=tx-s.vx,dz=tz-s.vz,len=Math.hypot(dx,dz),kick=checkpoint.groundAcceleration()*Math.hypot(tx,tz)*dt*60;
            double blend=len>0?Math.min(1,kick/len):0;s.vx+=dx*blend;s.vz+=dz*blend;
            double x=s.x,y=s.y,z=s.z;s.x+=s.vx*dt;s.z+=s.vz*dt;
            double drag=Math.max(0,1-checkpoint.groundDrag()*dt*60);s.vx*=drag;s.vz*=drag;
            if(Math.hypot(tx,tz)>1e-6){double heading=Math.atan2(tx,-tz);s.qx=s.qz=0;s.qy=Math.sin(-heading*.5);s.qw=Math.cos(heading*.5);}
            if(contact!=null)contact.afterStep(s,x,y,z,dt,0);return;
        }
        if(kind==PilotCheckpoint.CREATOR){
            boolean moving=c.active()&&c.mode()!=2;
            stabilized.step(s,p,dt,moving?c.forward():0,moving?c.right():0,moving?c.climb():0,checkpoint.speedMps(),c.headingRadians(),contact);return;
        }
        if(!c.active())stabilized.step(s,p,dt,0,0,0,checkpoint.speedMps(),-2*Math.atan2(s.qy,s.qw),contact);
        else if(c.mode()==0&&kind!=PilotCheckpoint.CREATOR)simulation.step(dt,c.throttle(),c.pitch(),c.roll(),c.yaw(),contact);
        else if(c.mode()==2){double x=s.x,y=s.y,z=s.z;builder.step(s,dt,c.forward(),c.right(),c.climb(),c.turn(),checkpoint.buildSpeedMps());if(contact!=null)contact.afterStep(s,x,y,z,dt,0);}
        else stabilized.step(s,p,dt,c.forward(),c.right(),c.mode()==3?0:c.climb(),checkpoint.speedMps(),c.headingRadians(),contact);
    }
    private void compose(double dt,Contact contact){
        var s=simulation.state();NetProtocol.copy(s,display);double decay=Math.exp(-dt/.08);offsetX*=decay;offsetY*=decay;offsetZ*=decay;rotationWeight*=decay;
        display.x+=offsetX;display.y+=offsetY;display.z+=offsetZ;
        double x=correctionQx*rotationWeight,y=correctionQy*rotationWeight,z=correctionQz*rotationWeight,w=1+(correctionQw-1)*rotationWeight;
        double norm=Math.sqrt(x*x+y*y+z*z+w*w);x/=norm;y/=norm;z/=norm;w/=norm;
        display.qx=w*s.qx+x*s.qw+y*s.qz-z*s.qy;display.qy=w*s.qy-x*s.qz+y*s.qw+z*s.qx;
        display.qz=w*s.qz+x*s.qy-y*s.qx+z*s.qw;display.qw=w*s.qw-x*s.qx-y*s.qy-z*s.qz;
        if(contact!=null)contact.afterStep(display,s.x,s.y,s.z,0,0);
    }
    private void multiply(double ax,double ay,double az,double aw,double bx,double by,double bz,double bw){
        correctionQx=aw*bx+ax*bw+ay*bz-az*by;correctionQy=aw*by-ax*bz+ay*bw+az*bx;
        correctionQz=aw*bz+ax*by-ay*bx+az*bw;correctionQw=aw*bw-ax*bx-ay*by-az*bz;
    }
    private static double distance(FlightController.State a,FlightController.State b){return Math.sqrt(Math.pow(a.x-b.x,2)+Math.pow(a.y-b.y,2)+Math.pow(a.z-b.z,2));}
    private static double angle(FlightController.State a,FlightController.State b){return Math.toDegrees(2*Math.acos(Math.min(1,Math.abs(a.qx*b.qx+a.qy*b.qy+a.qz*b.qz+a.qw*b.qw))));}
}
