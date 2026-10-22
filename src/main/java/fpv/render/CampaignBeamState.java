package fpv.render;

import fpv.construction.LayerCampaignFamilies;
import fpv.construction.LayerTile;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.world.blocks.campaign.*;
import java.util.*;

/** Presentation-only shipment evidence. Never infers a shipment from a timer alone. */
public final class CampaignBeamState {
    private static final class Launch{float counter;int seen;LaunchPayload payload;}
    private final IdentityHashMap<Building,Launch> launches=new IdentityHashMap<>();
    private final ArrayList<LaunchPayload> payloads=new ArrayList<>();
    private final ArrayList<Building> elevated=new ArrayList<>();private boolean scanned;private int frame;private Object tiles;
    public void clear(){launches.clear();payloads.clear();elevated.clear();tiles=null;frame=0;}
    public void begin(fpv.construction.LayerRuntime construction){
        if(Vars.world==null||tiles!=Vars.world.tiles){clear();tiles=Vars.world==null?null:Vars.world.tiles;}
        frame++;scanned=false;payloads.clear();elevated.clear();
        if(construction!=null)for(var entry:construction.nativeBuildings().entries())if(entry.building() instanceof LaunchPad.LaunchPadBuild)elevated.add(entry.building());
    }
    public void end(){var it=launches.values().iterator();while(it.hasNext())if(it.next().seen!=frame)it.remove();}
    public float progress(Building b){
        if(b instanceof LandingPad.LandingPadBuild v)return v.arriving!=null&&Float.isFinite(v.arrivingTimer)?clamp(v.arrivingTimer):-1;
        if(!(b instanceof LaunchPad.LaunchPadBuild v))return -1;
        if(b.tile instanceof LayerTile){float timer=LayerCampaignFamilies.launchPodTimer(b);return timer>0&&Float.isFinite(timer)?clamp(1-timer/120f):-1;}
        var old=launches.get(b);
        if(old==null){old=new Launch();launches.put(b,old);if(!ambiguous(b))old.payload=payload(b,false);}
        // The last item may enter and launch between render frames; a full prior snapshot is not required.
        else if(Float.isFinite(v.launchCounter)&&v.launchCounter<old.counter)old.payload=payload(b,true);
        old.counter=v.launchCounter;old.seen=frame;var p=old.payload;
        return valid(p)&&p.team==b.team&&Math.abs(p.x-b.x)<.01&&Math.abs(p.y-b.y)<.01?clamp(p.time/p.lifetime):-1;
    }
    private boolean ambiguous(Building b){for(var upper:elevated)if(upper.team==b.team&&Math.abs(upper.x-b.x)<.01&&Math.abs(upper.y-b.y)<.01)return true;return false;}
    private LaunchPayload payload(Building b,boolean fresh){
        // Lazy once per render only on first observation or a native launch-counter reset.
        // Ordinary idle frames never scan Groups.all. The payload proves real resources left.
        if(!scanned){for(var e:Groups.all)if(e instanceof LaunchPayload p&&valid(p))payloads.add(p);scanned=true;}
        LaunchPayload best=null;float maxAge=Math.max(2,Float.isFinite(arc.util.Time.delta)?arc.util.Time.delta*2:2);
        for(var p:payloads)if(p.team==b.team&&Math.abs(p.x-b.x)<.01&&Math.abs(p.y-b.y)<.01&&(!fresh||p.time<=maxAge)&&(best==null||p.time<best.time))best=p;
        return best;
    }
    private static boolean valid(LaunchPayload p){return p!=null&&p.isAdded()&&p.stacks!=null&&!p.stacks.isEmpty()&&Float.isFinite(p.time)&&Float.isFinite(p.lifetime)&&p.lifetime>0&&p.time>=0&&p.time<p.lifetime;}
    private static float clamp(float p){return Math.max(0,Math.min(1,p));}
}
