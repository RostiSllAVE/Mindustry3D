package fpv.construction;

import java.util.*;

/** Opt-in, bounded lifecycle evidence. Render-ready means prepared geometry, not measured display latency. */
public final class ConstructionTimings {
    public record Sample(String stage,int x,int y,int layer,double milliseconds){}
    private record Cell(int x,int y,int layer,boolean platform){}
    private final boolean enabled=Boolean.getBoolean("fpv.construction.timing");
    private final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private final LinkedHashMap<Cell,Long> queued=new LinkedHashMap<>();
    private final LinkedHashMap<Long,Long> committed=new LinkedHashMap<>();
    private int logged;
    public long start(){return enabled?System.nanoTime():0;}
    public List<Sample> samples(){return List.copyOf(samples);}
    public void clear(){samples.clear();queued.clear();committed.clear();logged=0;}
    public void admission(LayerActions.Action a,long start){if(enabled&&start!=0)sample("REQUEST_TO_QUEUE_OR_COMMIT",a.x(),a.y(),a.layer(),start);}
    public void queued(ConstructionPriority.Target t){
        if(!enabled)return;var cell=new Cell(t.x(),t.y(),t.layer(),t.platform());queued.putIfAbsent(cell,System.nanoTime());bound(queued);
    }
    public void commit(LayerComponent c){
        if(!enabled)return;Long since=queued.remove(new Cell(c.x(),c.y(),c.layer(),c.kind()==LayerComponent.Kind.PLATFORM));
        if(since!=null)sample("QUEUE_TO_COMMIT_INCLUDING_BUILD_AND_WAIT",c.x(),c.y(),c.layer(),since);
        committed.put(c.id(),System.nanoTime());bound(committed);
    }
    public void renderReady(LayerComponent c){if(enabled){Long since=committed.remove(c.id());if(since!=null)sample("COMMIT_TO_RENDER_READY",c.x(),c.y(),c.layer(),since);}}
    public void replicaApplied(long start){if(enabled&&start!=0)sample("REPLICA_APPLY_CPU",0,0,0,start);}
    private static void bound(Map<?,?> map){while(map.size()>256)map.remove(map.keySet().iterator().next());}
    private void sample(String stage,int x,int y,int z,long since){
        var s=new Sample(stage,x,y,z,(System.nanoTime()-since)/1e6);if(samples.size()==256)samples.removeFirst();samples.addLast(s);
        if(logged++<256)arc.util.Log.info("[FPV construction timing] @ (@,@,Z@) @ ms",stage,x,y,z,s.milliseconds());
    }
}
