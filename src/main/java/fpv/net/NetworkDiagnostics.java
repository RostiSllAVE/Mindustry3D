package fpv.net;

import java.util.Arrays;

/** Bounded, numeric-only observations. No packet contents, identities, paths or new wire traffic. */
public final class NetworkDiagnostics {
    public static final int WINDOW=512;
    public enum Metric {
        FRAME_INTERVAL_MS, DRAW_CPU_SPAN_MS, SIMULATION_TRIGGER_MS, NETWORK_BEFORE_MS, NETWORK_AFTER_MS,
        APP_RTT_MS, APP_RTT_ABS_CHANGE_MS, INPUT_SEND_GAP_MS, INPUT_ARRIVAL_GAP_MS,
        INPUT_ARRIVAL_TO_STEP_MS, INPUT_SILENCE_MS, SNAPSHOT_COMPLETE_GAP_MS, SNAPSHOT_LOCAL_AGE_MS,
        INTERPOLATION_ALPHA, OWNER_CORRECTION_TILES, CHANNEL_ENCODE_MS, CHANNEL_APPLY_MS,
        CHANNEL_LOGICAL_BYTES, ASSEMBLY_CHANNELS, ASSEMBLY_RETAINED_BYTES, EXPORT_MS,
        CONNECTION_HANDSHAKE_MS, CONNECTION_READY_MS, FIRST_INPUT_ACCEPTED_MS, FIRST_USABLE_INPUT_MS,
        POSE_COLLECT_MS, POSE_ENCODE_MS, POSE_CHUNK_SUBMIT_MS, POSE_DECODE_MS, POSE_APPLY_MS,
        PREDICTION_MS, PREDICTION_POSITION_ERROR_TILES, PREDICTION_ANGLE_ERROR_DEGREES, PREDICTION_CORRECTION_TILES
    }
    public enum Counter {
        TX_PACKETS, TX_PAYLOAD_BYTES, TX_RELIABLE, TX_UNRELIABLE, RX_PACKETS, RX_PAYLOAD_BYTES,
        INPUT_TX, INPUT_ACCEPTED, INPUT_STALE_OR_REORDERED, INPUT_SEQUENCE_GAP_UNATTRIBUTED,
        INPUT_TIMEOUT_POLLS, HELLO_TX, HELLO_RX, ACK_RX, WORLD_RESETS, CONNECT_RESETS,
        REJECT_SIZE, REJECT_RATE, REJECT_VERIFICATION, REJECT_PROTOCOL_OR_PAYLOAD, REJECT_EPOCH_OR_KIND,
        CHUNKS_INCOMPLETE_OR_STALE, CHUNKS_STALE_OR_REPLAY, SNAPSHOTS_APPLIED, CHANNEL_APPLIES, CHANNEL_ENCODE_FAILURE,
        FRAME_STALLS_OVER_100MS, SIMULATION_STALLS_OVER_100MS, EXPORT_FAILURES,
        CONNECTION_READY, CONNECTION_FAILED, EARLY_STATE_IGNORED, STATE_APPLY_FAILURE,
        FIRST_INPUT_TX, FIRST_INPUT_ACCEPTED, FIRST_USABLE_INPUT_ACCEPTED,
        POSE_QUERIES, POSE_QUERY_REUSE, POSE_VISIBLE_STATES, PREDICTION_REPLAY_FRAMES, PREDICTION_STALE_HOLDS,
        PREDICTION_CORRECTIONS, PREDICTION_HARD_RESETS
    }
    // Fixed export keys: no attacker-supplied channel names can enter the report.
    public enum Channel {
        POSES, COMBAT, MECH_PITCH, UNIT_RANGES, EXPLOSIONS, SUPPORT_EFFECTS, GROUND_PROFILES,
        CREATOR_FLIGHT, PROJECTION, FORCE_AIRCRAFT, FORCE_PROJECTORS, LAYERED_WORLD, LAYER_TURRETS, OTHER;
        public static Channel of(String name){
            return switch(name){
                case "poses"->POSES;case "combat"->COMBAT;case "mech-pitch"->MECH_PITCH;
                case "unit-ranges"->UNIT_RANGES;case "explosions"->EXPLOSIONS;case "support-effects"->SUPPORT_EFFECTS;
                case "ground-profiles"->GROUND_PROFILES;case "creator-flight"->CREATOR_FLIGHT;case "projection"->PROJECTION;
                case "force-aircraft"->FORCE_AIRCRAFT;case "force-projectors"->FORCE_PROJECTORS;
                case "layered-world"->LAYERED_WORLD;case "layer-turrets"->LAYER_TURRETS;default->OTHER;
            };
        }
    }
    private final Series[] metrics=new Series[Metric.values().length];
    private final long[] counters=new long[Counter.values().length];
    private final long[][] channels=new long[Channel.values().length][4];
    private boolean enabled;
    public boolean enabled(){return enabled;}
    public void enabled(boolean value){enabled=value;}
    public void sample(Metric metric,double value){
        if(!enabled||!Double.isFinite(value)||value<0)return;
        int i=metric.ordinal();if(metrics[i]==null)metrics[i]=new Series();metrics[i].add(value);
    }
    public void count(Counter counter){add(counter,1);}
    public void add(Counter counter,long value){if(enabled&&value>0)counters[counter.ordinal()]+=value;}
    public long value(Counter counter){return counters[counter.ordinal()];}
    public double recentP95(Metric metric){Series s=metrics[metric.ordinal()];return s==null?0:s.p95();}
    public void packet(boolean outgoing,boolean reliable,int bytes){
        if(!enabled)return;
        count(outgoing?Counter.TX_PACKETS:Counter.RX_PACKETS);
        add(outgoing?Counter.TX_PAYLOAD_BYTES:Counter.RX_PAYLOAD_BYTES,bytes);
        if(outgoing)count(reliable?Counter.TX_RELIABLE:Counter.TX_UNRELIABLE);
    }
    public void channel(String name,boolean outgoing,int bytes){
        if(!enabled)return;long[] values=channels[Channel.of(name).ordinal()];
        int at=outgoing?0:2;values[at]++;values[at+1]+=Math.max(0,bytes);
    }
    public String json(double elapsedSeconds,int role,boolean verified,int protocol){
        // Role: 0 offline, 1 client, 2 listen host, 3 dedicated host. No free-form data.
        double seconds=Math.max(.001,Double.isFinite(elapsedSeconds)?elapsedSeconds:0);
        StringBuilder out=new StringBuilder(8192);
        out.append("{\n\"schema\":1,\"elapsedSeconds\":").append(seconds)
            .append(",\"role\":").append(Math.max(0,Math.min(3,role)))
            .append(",\"payloadVerified\":").append(verified).append(",\"protocol\":").append(protocol)
            .append(",\"percentileWindowSamples\":").append(WINDOW)
            .append(",\"metrics\":{");
        boolean first=true;
        for(Metric m:Metric.values())if(metrics[m.ordinal()]!=null){
            if(!first)out.append(',');first=false;out.append('\n').append('"').append(m.name()).append("\":");metrics[m.ordinal()].json(out);
        }
        out.append("\n},\"counters\":{");first=true;
        for(Counter c:Counter.values()){
            if(!first)out.append(',');first=false;out.append('\n').append('"').append(c.name()).append("\":").append(counters[c.ordinal()]);
        }
        out.append("\n},\"channels\":{");first=true;
        for(Channel c:Channel.values()){
            long[] v=channels[c.ordinal()];if(v[0]+v[2]==0)continue;
            if(!first)out.append(',');first=false;out.append('\n').append('"').append(c.name()).append("\":{")
                .append("\"txMessages\":").append(v[0]).append(",\"txBytes\":").append(v[1])
                .append(",\"rxMessages\":").append(v[2]).append(",\"rxBytes\":").append(v[3]).append('}');
        }
        return out.append("\n}}\n").toString();
    }
    static final class Series {
        final double[] values=new double[WINDOW];int at,size;long count;double sum,max;
        void add(double value){values[at]=value;at=(at+1)%WINDOW;size=Math.min(WINDOW,size+1);count++;sum+=value;max=Math.max(max,value);}
        void json(StringBuilder out){
            double[] sorted=Arrays.copyOf(values,size);Arrays.sort(sorted);
            out.append("{\"count\":").append(count).append(",\"mean\":").append(sum/count)
                .append(",\"p50\":").append(percentile(sorted,.5)).append(",\"p95\":").append(percentile(sorted,.95))
                .append(",\"p99\":").append(percentile(sorted,.99)).append(",\"max\":").append(max).append('}');
        }
        double p95(){double[] sorted=Arrays.copyOf(values,size);Arrays.sort(sorted);return percentile(sorted,.95);}
        static double percentile(double[] values,double p){return values[Math.max(0,(int)Math.ceil(values.length*p)-1)];}
    }
}
