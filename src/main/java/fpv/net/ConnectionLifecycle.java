package fpv.net;

import java.util.*;

/** Client session readiness, independent of transport and physics. Times are monotonic seconds. */
public final class ConnectionLifecycle {
    public enum Stage { OFFLINE, NATIVE_WORLD, VERIFYING, SYNCHRONIZING, WAITING_FOR_UNIT, READY, FAILED }
    public enum Failure { NONE, MOD_MISMATCH, INVALID_PROTOCOL, NATIVE_WORLD_TIMEOUT, HANDSHAKE_TIMEOUT,
        INITIAL_STATE_TIMEOUT, OWN_POSE_TIMEOUT, STATE_APPLY_FAILED, PAYLOAD_UNAVAILABLE }
    public static final double WORLD_TIMEOUT=120, HELLO_TIMEOUT=15, SYNC_TIMEOUT=45, MAX_SYNC_TIME=180;
    private Stage stage=Stage.OFFLINE;
    private Failure failure=Failure.NONE;
    private NetworkDiagnostics.Channel failedChannel=NetworkDiagnostics.Channel.OTHER;
    private final Set<String> required=new HashSet<>(),applied=new HashSet<>();
    private boolean world,ack,poses;
    private long epoch;
    private double started,worldStarted,worldAt=-1,ackAt=-1,syncAt=-1,syncProgress=-1,unitAt=-1,readyAt=-1;
    public void begin(double time,Collection<String> channels){
        required.clear();required.addAll(channels);applied.clear();world=ack=poses=false;epoch=0;
        failure=Failure.NONE;failedChannel=NetworkDiagnostics.Channel.OTHER;started=worldStarted=time;
        worldAt=ackAt=syncAt=syncProgress=unitAt=readyAt=-1;stage=Stage.NATIVE_WORLD;
    }
    public void resetWorld(double time,Collection<String> channels){
        if(stage==Stage.FAILED)return;
        double connectionStart=stage==Stage.OFFLINE?time:started;begin(time,channels);started=connectionStart;
    }
    public void offline(){if(stage!=Stage.FAILED){stage=Stage.OFFLINE;world=ack=poses=false;applied.clear();epoch=0;}}
    public void worldLoaded(double time){if(stage==Stage.FAILED)return;world=true;worldAt=time;refresh();}
    public void acknowledged(long value,double time){
        if(stage==Stage.FAILED||value==0)return;
        if(ack&&epoch==value)return;
        if(ack&&epoch!=value){world=false;worldStarted=time;worldAt=-1;poses=false;applied.clear();syncAt=syncProgress=readyAt=unitAt=-1;}
        if(!ack||epoch!=value)ackAt=time;
        ack=true;epoch=value;refresh();
    }
    public void applied(String channel){if(acceptsState()&&required.contains(channel))applied.add(channel);}
    public void poses(){if(acceptsState())poses=true;}
    public void progress(String channel,double time){
        if(acceptsState()&&(channel.equals("poses")?!poses:required.contains(channel)&&!applied.contains(channel)))syncProgress=time;
    }
    public boolean acceptsState(){return world&&ack&&stage!=Stage.FAILED&&stage!=Stage.OFFLINE;}
    public void tick(double time,boolean nativeUsable,boolean liveUnit,boolean ownPose){
        if(stage==Stage.OFFLINE||stage==Stage.FAILED)return;
        refresh();
        if(!world||!nativeUsable){stage=Stage.NATIVE_WORLD;if(time-worldStarted>WORLD_TIMEOUT)fail(Failure.NATIVE_WORLD_TIMEOUT,null);return;}
        if(!ack){stage=Stage.VERIFYING;if(time-worldAt>HELLO_TIMEOUT)fail(Failure.HANDSHAKE_TIMEOUT,null);return;}
        if(syncAt<0){syncAt=time;if(syncProgress<0)syncProgress=time;}
        if(!poses||!applied.containsAll(required)){stage=Stage.SYNCHRONIZING;if(time-syncProgress>SYNC_TIMEOUT||time-syncAt>MAX_SYNC_TIME)fail(Failure.INITIAL_STATE_TIMEOUT,null);return;}
        if(!liveUnit||!ownPose){
            stage=Stage.WAITING_FOR_UNIT;
            if(!liveUnit)unitAt=-1;else if(unitAt<0)unitAt=time;
            if(liveUnit&&time-unitAt>SYNC_TIMEOUT)fail(Failure.OWN_POSE_TIMEOUT,null);
            return;
        }
        stage=Stage.READY;if(readyAt<0)readyAt=time;unitAt=-1;
    }
    private void refresh(){if(stage==Stage.FAILED||stage==Stage.OFFLINE)return;stage=!world?Stage.NATIVE_WORLD:!ack?Stage.VERIFYING:Stage.SYNCHRONIZING;}
    public void fail(Failure reason,String channel){if(stage==Stage.FAILED)return;failure=reason;failedChannel=channel==null?NetworkDiagnostics.Channel.OTHER:NetworkDiagnostics.Channel.of(channel);stage=Stage.FAILED;}
    public Stage stage(){return stage;}
    public Failure failure(){return failure;}
    public boolean ready(){return stage==Stage.READY;}
    public boolean initialStateComplete(){return stage==Stage.READY||stage==Stage.WAITING_FOR_UNIT;}
    public int missing(){return Math.max(0,required.size()-applied.size());}
    public double handshakeMs(){return ackAt<0?-1:Math.max(0,ackAt-worldStarted)*1000;}
    public double readyMs(){return readyAt<0?-1:Math.max(0,readyAt-worldStarted)*1000;}
    public double worldMs(){return worldAt<0?-1:Math.max(0,worldAt-worldStarted)*1000;}
    private String missingChannels(){
        var names=new TreeSet<String>();if(!poses)names.add(NetworkDiagnostics.Channel.POSES.name());
        for(String name:required)if(!applied.contains(name))names.add(NetworkDiagnostics.Channel.of(name).name());
        return String.join(",",names);
    }
    public String message(){
        if(stage==Stage.FAILED)return switch(failure){
            case MOD_MISMATCH -> "FPV package mismatch. Install the exact same JAR on every participant, then reconnect.";
            case INVALID_PROTOCOL -> "FPV protocol is incompatible or invalid. Use the same diagnostic build on both computers, then reconnect.";
            case PAYLOAD_UNAVAILABLE -> "FPV cannot verify its installed files. Reinstall the complete matching JAR or Workshop package.";
            case NATIVE_WORLD_TIMEOUT -> "Native Mindustry world loading did not finish. Check the game's connection error and try reconnecting.";
            case HANDSHAKE_TIMEOUT -> "The server did not confirm FPV compatibility. Enable the same mod on the host, then reconnect.";
            case INITIAL_STATE_TIMEOUT -> "FPV initial state is incomplete ("+missingChannels()+"). Save both diagnostic reports, then reconnect.";
            case OWN_POSE_TIMEOUT -> "The server did not provide the controlled unit's FPV state. Save both reports, then reconnect.";
            case STATE_APPLY_FAILED -> "FPV state could not be applied ("+failedChannel.name()+"). Save both reports, then reconnect; do not continue this partial session.";
            default -> "FPV connection failed. Save both reports and reconnect.";
        };
        return switch(stage){
            case OFFLINE -> "FPV offline";
            case NATIVE_WORLD -> "FPV: waiting for native world loading";
            case VERIFYING -> "FPV: verifying matching mod";
            case SYNCHRONIZING -> "FPV: receiving initial state ("+missing()+" channels remaining)";
            case WAITING_FOR_UNIT -> "FPV world synchronized; waiting for a controllable unit";
            case READY -> "FPV READY / F5 view / Tab build";
            default -> "FPV connection failed";
        };
    }
    public String json(){return "{\"stage\":\""+stage.name()+"\",\"failure\":\""+failure.name()+"\",\"nativeWorldLoaded\":"+world+",\"compatibilityAck\":"+ack+",\"posesApplied\":"+poses+",\"requiredChannels\":"+required.size()+",\"appliedChannels\":"+applied.size()+",\"missingChannels\":\""+missingChannels()+"\",\"nativeWorldMs\":"+worldMs()+",\"handshakeMs\":"+handshakeMs()+",\"readyMs\":"+readyMs()+",\"failedChannel\":\""+failedChannel.name()+"\"}";}
}
