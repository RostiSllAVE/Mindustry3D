package fpv.net;

import arc.Core;
import mindustry.Vars;
import java.nio.file.*;
import static fpv.net.NetworkDiagnostics.Metric.*;
import static fpv.net.NetworkDiagnostics.Counter.*;

/** Opt-in local aggregate report, at most one small write per 15 seconds or when stopped. */
final class NetworkDiagnosticSession {
    static final String SETTING="fpv-net-diagnostics";
    final NetworkDiagnostics data=new NetworkDiagnostics();
    ConnectionLifecycle connection;
    private long poll,nextExport,started,lastFrame,lastInputSend,lastSnapshot,lastPingAt,simulation,draw;
    private int previousPing=-1;
    private Path output;
    private boolean verified;
    private String identity="unverified";
    private final double[][] trend=new double[64][7];
    private int trendAt,trendSize;
    void identity(String value){if(value!=null&&value.matches("payload1-[0-9a-f]{64}"))identity=value;}
    void update(boolean verification){
        verified=verification;long now=System.nanoTime();
        if(now>=poll){
            poll=now+1_000_000_000L;
            boolean wanted=Boolean.getBoolean("fpv.netDiagnostics")||Core.settings!=null&&Core.settings.getBool(SETTING,false);
            if(wanted&&!data.enabled()){
                data.enabled(true);if(started==0)started=now;
                lastFrame=lastInputSend=lastSnapshot=simulation=draw=0;nextExport=now+15_000_000_000L;
            }else if(!wanted&&data.enabled()){flush();data.enabled(false);}
        }
        if(!data.enabled())return;
        if(lastFrame!=0){double ms=(now-lastFrame)/1e6;data.sample(FRAME_INTERVAL_MS,ms);if(ms>100)data.count(FRAME_STALLS_OVER_100MS);}lastFrame=now;
        if(Vars.net.client()&&Vars.netClient!=null&&now-lastPingAt>=1_000_000_000L){
            lastPingAt=now;int ping=Vars.netClient.getPing();
            if(ping>=0){data.sample(APP_RTT_MS,ping);if(previousPing>=0)data.sample(APP_RTT_ABS_CHANGE_MS,Math.abs(ping-previousPing));previousPing=ping;}
        }
        if(now>=nextExport){nextExport=now+15_000_000_000L;flush();}
    }
    long start(){return data.enabled()?System.nanoTime():0;}
    void end(NetworkDiagnostics.Metric metric,long start){if(start!=0)data.sample(metric,(System.nanoTime()-start)/1e6);}
    void simulationStart(){if(data.enabled())simulation=System.nanoTime();}
    void simulationEnd(){if(simulation!=0){double ms=(System.nanoTime()-simulation)/1e6;data.sample(SIMULATION_TRIGGER_MS,ms);if(ms>100)data.count(SIMULATION_STALLS_OVER_100MS);simulation=0;}}
    void drawStart(){if(data.enabled())draw=System.nanoTime();}
    void drawEnd(){if(draw!=0){end(DRAW_CPU_SPAN_MS,draw);draw=0;}}
    void inputSent(){if(!data.enabled())return;long now=System.nanoTime();if(lastInputSend!=0)data.sample(INPUT_SEND_GAP_MS,(now-lastInputSend)/1e6);lastInputSend=now;data.count(INPUT_TX);}
    void snapshot(){if(!data.enabled())return;long now=System.nanoTime();if(lastSnapshot!=0)data.sample(SNAPSHOT_COMPLETE_GAP_MS,(now-lastSnapshot)/1e6);lastSnapshot=now;data.count(SNAPSHOTS_APPLIED);}
    void localAge(){if(data.enabled()&&lastSnapshot!=0)data.sample(SNAPSHOT_LOCAL_AGE_MS,(System.nanoTime()-lastSnapshot)/1e6);}
    void flush(){
        if(!data.enabled()||Vars.dataDirectory==null)return;
        long begin=System.nanoTime();
        try{
            if(output==null){Path directory=Vars.dataDirectory.file().toPath().resolve("fpv-diagnostics");Files.createDirectories(directory);output=directory.resolve("network-"+java.util.UUID.randomUUID()+".json");}
            int role=!Vars.net.active()?0:Vars.net.client()?1:Vars.headless?3:2;
            double[] row=trend[trendAt];row[0]=(begin-started)/1e9;row[1]=data.recentP95(FRAME_INTERVAL_MS);row[2]=data.recentP95(APP_RTT_MS);row[3]=data.recentP95(SNAPSHOT_LOCAL_AGE_MS);row[4]=data.value(TX_PAYLOAD_BYTES);row[5]=data.value(RX_PAYLOAD_BYTES);row[6]=data.recentP95(SIMULATION_TRIGGER_MS);
            trendAt=(trendAt+1)%trend.length;trendSize=Math.min(trend.length,trendSize+1);
            String base=data.json(row[0],role,verified,(int)fpv.generated.NetworkConfig.PROTOCOL_VERSION).trim();
            StringBuilder json=new StringBuilder(base.substring(0,base.length()-1))
                .append(",\"unreleasedDiagnostics\":true,\"sourceBaseline\":\"0.18.31\",\"payloadIdentity\":\"").append(identity)
                .append("\",\"hostBuild\":").append(mindustry.core.Version.build).append(",\"hostRevision\":").append(mindustry.core.Version.revision)
                .append(",\"javaFeature\":").append(Runtime.version().feature());
            if(connection!=null)json.append(",\"connection\":").append(role>=2?"{\"stage\":\"HOST\",\"clientReadinessGate\":false}":connection.json());
            json.append(",\"trend\":[");
            for(int n=0;n<trendSize;n++){if(n>0)json.append(',');json.append(java.util.Arrays.toString(trend[(trendAt-trendSize+n+trend.length)%trend.length]));}
            Files.writeString(output,json.append("]}\n").toString());
        }catch(Exception failure){data.count(EXPORT_FAILURES);} // Never log paths or abort gameplay.
        finally{data.sample(EXPORT_MS,(System.nanoTime()-begin)/1e6);}
    }
}
