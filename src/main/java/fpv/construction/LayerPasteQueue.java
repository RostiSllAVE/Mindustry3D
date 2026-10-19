package fpv.construction;

import arc.math.geom.Point2;
import arc.struct.Seq;
import java.util.*;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.blocks.power.PowerNode;

/** Server-owned unpaid build plans. Complete via the ordinary PLACE validator, then configure new IDs only. */
public final class LayerPasteQueue {
    public record Preview(Seq<BuildPlan> plans,boolean[] valid,double[] distance,int ready,String reason){}
    private static final class Job {
        final Player player;final Unit unit;final int team,layer;final LayerComponent.Mode mode;
        final ArrayDeque<BuildPlan> pending=new ArrayDeque<>();final LinkedHashMap<Integer,Long> made=new LinkedHashMap<>();final ArrayList<BuildPlan> completed=new ArrayList<>();final IdentityHashMap<BuildPlan,Double> parkedWork=new IdentityHashMap<>();double work;
        boolean manual;VolumeClipboard.Clip volume;final Map<Integer,Long> volumeMade=new HashMap<>();final Set<Integer> configuredCells=new HashSet<>(),configuredLinks=new HashSet<>();long nextConfigure;
        Job(Player p,int layer,LayerComponent.Mode mode){player=p;unit=p.unit();team=p.team().id;this.layer=layer;this.mode=mode;}
    }
    private final LayerRuntime runtime;private final ArrayList<Job> jobs=new ArrayList<>();
    private final Map<Player,List<ConstructionPriority.Target>> cachedTargets=new IdentityHashMap<>();
    private final Map<Player,ArrayDeque<Long>> volumeRequests=new IdentityHashMap<>();
    public boolean claimVolume(Player p){long now=arc.util.Time.millis();var times=volumeRequests.computeIfAbsent(p,k->new ArrayDeque<>());while(!times.isEmpty()&&now-times.peekFirst()>=1000)times.removeFirst();if(times.size()>=2)return false;times.addLast(now);return true;}
    private static int floor(Job j,BuildPlan p){return p instanceof VolumeClipboard.Plan v?v.floor:j.layer;}
    /** Manual and schematic work share scheduling, pause, priority, validation and costs. */
    public LayerActions.Result enqueueManual(Player player,LayerActions.Action action){
        var allowed=runtime.previewWork(player,action,true);if(!allowed.accepted())return allowed;
        if(pending(player)>=LayerSchematic.MAX_PLANS||jobs.size()>=64&&jobs.stream().noneMatch(j->j.manual&&j.player==player&&j.unit==player.unit()&&j.layer==action.layer()&&j.mode==action.mode()))return LayerActions.Result.rejected("Construction queue is full");
        var p=action.kind()==LayerComponent.Kind.PLATFORM?new LayerSchematic.PlatformPlan(action.x(),action.y()):new BuildPlan(action.x(),action.y(),action.rotation(),Vars.content.getByName(mindustry.ctype.ContentType.block,action.blockName()));
        for(var j:jobs)for(var q:j.pending)if(floor(j,q)==action.layer()&&overlap(p,q))return LayerActions.Result.rejected("Construction already queued in this footprint");
        if(LayerRuntime.instantConstruction())return runtime.apply(player,action);
        Job job=null;for(var j:jobs)if(j.manual&&j.player==player&&j.unit==player.unit()&&j.layer==action.layer()&&j.mode==action.mode()){job=j;break;}
        if(job==null){job=new Job(player,action.layer(),action.mode());job.manual=true;jobs.add(job);}job.pending.add(p);runtime.timings().queued(target(job,p));invalidateTargets();
        return new LayerActions.Result(true,false,"Construction queued on Z"+action.layer()+"; F3 pauses, Alt-click selects priority",0);
    }
    private final Map<Player,Map<ConstructionPriority.Target,Double>> cachedProgress=new IdentityHashMap<>();
    private void invalidateTargets(){cachedTargets.clear();cachedProgress.clear();}
    public double progress(Player player,ConstructionPriority.Target t){
        if(Vars.net.client())return remoteProgress.getOrDefault(t,0.0);
        var values=cachedProgress.computeIfAbsent(player,owner->{
            var result=new HashMap<ConstructionPriority.Target,Double>();
            for(var j:jobs)if(j.player==owner)for(var p:j.pending){double work=j.pending.peek()==p?j.work:j.parkedWork.getOrDefault(p,0.0);if(work>0)result.put(target(j,p),Math.min(.999,Math.max(0,work/buildTime(p))));}
            return result;
        });return values.getOrDefault(t,0.0);
    }
    private static double buildTime(BuildPlan p){return Math.max(1,(LayerSchematic.platform(p)?60:p.block.buildTime)*Vars.state.rules.buildCostMultiplier);}
    private final Map<Unit,List<BuildPlan>> rangeParked=new IdentityHashMap<>();
    public void suspendGroundRange(){
        restoreGroundRange();for(var j:jobs)if(j.volume!=null&&j.player.unit()==j.unit&&j.unit.isAdded())for(var p:j.pending)if(floor(j,p)==0&&!runtime.withinConstruction(j.player,p.x,p.y,p.block.size,0)){
            for(var actual:j.unit.plans)if(actual.x==p.x&&actual.y==p.y&&!actual.breaking&&actual.block==p.block){rangeParked.computeIfAbsent(j.unit,u->new ArrayList<>()).add(actual);j.unit.plans.remove(actual);break;}
        }
    }
    public void restoreGroundRange(){for(var e:rangeParked.entrySet())if(e.getKey().isAdded()&&!e.getKey().dead)for(var p:e.getValue())if(!e.getKey().plans.contains(p))e.getKey().plans.addLast(p);rangeParked.clear();}
    private List<ConstructionPriority.Target> remoteTargets=List.of();
    private Map<ConstructionPriority.Target,Double> remoteProgress=Map.of();
    public LayerPasteQueue(LayerRuntime runtime){this.runtime=runtime;}
    public int pending(Player player){int total=0;for(var j:jobs)if(j.player==player)total+=j.pending.size();return total;}
    public void clear(){restoreGroundRange();for(var j:jobs)if(j.volume!=null)for(var p:j.pending)if(floor(j,p)==0)removeNative(j,p);jobs.clear();nativeBusy.clear();nativeBefore.clear();volumeRequests.clear();invalidateTargets();remoteTargets=List.of();remoteProgress=Map.of();}
    private void removeNative(Job j,BuildPlan p){var parked=rangeParked.get(j.unit);if(parked!=null)parked.removeIf(q->q.x==p.x&&q.y==p.y&&q.block==p.block&&q.rotation==p.rotation&&!q.breaking);for(var q:j.unit.plans)if(q.x==p.x&&q.y==p.y&&q.block==p.block&&q.rotation==p.rotation&&!q.breaking){j.unit.plans.remove(q);break;}}
    public void cancel(Player player){jobs.removeIf(j->{if(j.player!=player)return false;if(j.volume!=null)for(var p:j.pending)if(floor(j,p)==0)removeNative(j,p);return true;});invalidateTargets();runtime.priority().clear(player);}
    public List<ConstructionPriority.Target> targets(Player player){
        if(Vars.net.client())return remoteTargets;
        var cached=cachedTargets.get(player);if(cached!=null)return cached;
        var list=new ArrayList<ConstructionPriority.Target>();for(var j:jobs)if(j.player==player&&j.team==player.team().id&&!j.unit.dead)for(var q:j.pending)list.add(target(j,q));
        var result=List.copyOf(list);if(!result.isEmpty())cachedTargets.put(player,result);return result;
    }
    private ConstructionPriority.Target target(Job j,BuildPlan p){return new ConstructionPriority.Target(j.unit.id,p.x,p.y,floor(j,p),LayerSchematic.platform(p),LayerSchematic.platform(p)?null:p.block,p.rotation);}
    public byte[] replication(Player player){return ConstructionPlanCodec.encode(targets(player),t->progress(player,t));}
    public void applyReplication(byte[] bytes){var snapshot=ConstructionPlanCodec.read(bytes);remoteTargets=snapshot.targets();remoteProgress=snapshot.progress();}
    public boolean contains(Player player,ConstructionPriority.Target t){for(var j:jobs)if(j.player==player&&j.team==player.team().id&&j.unit==player.unit()&&j.unit.id==t.unitId() )for(var p:j.pending)if(floor(j,p)==t.layer()&&t.matches(p))return true;return false;}
    public LayerActions.Result validatePriority(Player player,ConstructionPriority.Target t){
        if(!contains(player,t))return LayerActions.Result.rejected("Blueprint is no longer pending in your upper queue");
        for(var j:jobs)if(j.player==player&&j.unit==player.unit() )for(var p:j.pending)if(floor(j,p)==t.layer()&&t.matches(p))return runtime.preview(player,place(p,floor(j,p),j.mode));
        return LayerActions.Result.rejected("Blueprint is no longer pending");
    }
    void materializeGround(Player player,ConstructionPriority.Target t){for(var j:jobs)if(j.volume!=null&&j.player==player&&j.unit==player.unit())for(var p:j.pending)if(floor(j,p)==0&&t.matches(p)){boolean exists=false;for(var q:j.unit.plans)if(t.matches(q)){exists=true;break;}if(!exists)j.unit.addBuild(p);return;}}
    public void promote(Player player,ConstructionPriority.Target t){
        for(var j:jobs)if(j.player==player&&j.unit==player.unit() )for(var p:j.pending)if(floor(j,p)==t.layer()&&t.matches(p)){
            if(j.pending.peek()!=p){j.parkedWork.put(j.pending.peek(),j.work);j.pending.remove(p);j.pending.addFirst(p);j.work=j.parkedWork.getOrDefault(p,0.0);j.parkedWork.remove(p);invalidateTargets();}return;
        }
    }
    private void advance(Job j){var p=j.pending.remove();runtime.priority().completed(j.player,target(j,p));j.parkedWork.remove(p);if(!j.pending.isEmpty()){var next=j.pending.peek();j.work+=j.parkedWork.getOrDefault(next,0.0);j.parkedWork.remove(next);}invalidateTargets();}
    public Preview preview(Player player,LayerActions.Action action){
        var s=LayerSchematic.decode(action.configuration());var plans=LayerSchematic.plans(s,action.x(),action.y());
        boolean[] valid=new boolean[plans.size];double[] distance=new double[plans.size];Arrays.fill(distance,Double.POSITIVE_INFINITY);
        var trial=new LayerWorld(runtime.grid().coordinates());trial.apply(runtime.grid().snapshot());var added=new ArrayList<LayerComponent>();String reason="";
        long id=Math.max(trial.snapshot().nextId(),1);int ready=0;
        for(int i=0;i<plans.size;i++)try{
            var a=place(plans.get(i),action.layer(),action.mode());var check=runtime.previewSchematicCell(player,a,trial);
            if(!check.accepted()){if(reason.isEmpty())reason=check.reason();continue;}
            var c=runtime.schematicComponent(a,id++,player.team().id);trial.add(c);added.add(c);valid[i]=true;
        }catch(IllegalArgumentException bad){if(reason.isEmpty())reason="Outside map or invalid schematic footprint";}
        try(var solver=new StructuralSolver(trial,runtime::foundation)){
            solver.solveDirty();int addedIndex=0;
            for(int i=0;i<plans.size;i++)if(valid[i]){var c=added.get(addedIndex++);var support=solver.support(c.id());distance[i]=support.distance();
                if(action.mode()==LayerComponent.Mode.STRUCTURAL&&!support.supported()){valid[i]=false;if(reason.isEmpty())reason="Unsupported: "+support.reason();}
            }
            // A preview cannot authorize an added load that makes an existing supported structure collapse.
            if(action.mode()==LayerComponent.Mode.STRUCTURAL)for(var c:runtime.grid().components())if(runtime.solver().support(c.id()).supported()&&!solver.support(c.id()).supported()){
                Arrays.fill(valid,false);reason="Support load capacity exceeded by copied structure";break;
            }
        }
        if(!runtime.schematicBudget(added)){Arrays.fill(valid,false);reason="Construction save/network budget exceeded";}
        for(boolean v:valid)if(v)ready++;
        return new Preview(plans,valid,distance,ready,reason);
    }
    public LayerActions.Result enqueue(Player player,LayerActions.Action action){
        var preview=preview(player,action);if(preview.ready()==0)return LayerActions.Result.rejected(preview.reason().isEmpty()?"No valid schematic positions":preview.reason());
        if(jobs.size()>=64||pending(player)+preview.ready()>LayerSchematic.MAX_PLANS)return LayerActions.Result.rejected("Schematic build queue is full");
        var job=new Job(player,action.layer(),action.mode());var indices=new ArrayList<Integer>();for(int i=0;i<preview.plans().size;i++)if(preview.valid()[i])indices.add(i);
        indices.sort(Comparator.comparingDouble((Integer i)->preview.distance()[i]).thenComparingInt(i->LayerSchematic.platform(preview.plans().get(i))?0:1));
        for(int i:indices){var p=preview.plans().get(i);boolean duplicate=false;for(var other:jobs)if(other.player==player)for(var q:other.pending)if(floor(other,q)==job.layer&&p.x==q.x&&p.y==q.y&&LayerSchematic.platform(p)==LayerSchematic.platform(q)){duplicate=true;break;}
            if(!duplicate)job.pending.add(p);
        }
        if(job.pending.isEmpty())return LayerActions.Result.rejected("These positions are already queued");jobs.add(job);for(var p:job.pending)runtime.timings().queued(target(job,p));invalidateTargets();
        String message="Queued "+job.pending.size()+" schematic entries on Z"+job.layer;
        if(preview.ready()<preview.plans().size)message+="; skipped "+(preview.plans().size-preview.ready())+": "+preview.reason();
        return new LayerActions.Result(true,false,message.length()>256?message.substring(0,256):message,0);
    }
    public VolumePlacement.Preview previewVolume(Player player,LayerActions.Action action,boolean filters){
        var preview=VolumePlacement.preview(runtime,player,action,filters);if(!preview.accepted())return preview;
        for(var p:preview.plans()){
            boolean conflict=false;for(var j:jobs)for(var q:j.pending)if(floor(j,q)==p.floor&&overlap(p,q))conflict=true;
            if(p.floor==0)for(var q:player.unit().plans)if(overlap(p,q))conflict=true;
            if(conflict){boolean[] valid=preview.valid().clone();valid[p.index]=false;return new VolumePlacement.Preview(preview.placement(),preview.plans(),valid,List.of(),"A pending construction footprint overlaps this paste");}
        }return preview;
    }
    public LayerActions.Result enqueueVolume(Player player,LayerActions.Action action){
        var preview=previewVolume(player,action,true);if(!preview.accepted())return preview.result();
        if(jobs.size()>=64||pending(player)+preview.plans().size()>LayerSchematic.MAX_PLANS)return LayerActions.Result.rejected("Construction queue is full");
        // Whole-batch admission: never silently skip occupied / already queued positions.
        for(var p:preview.plans()){
            for(var j:jobs)for(var q:j.pending)if(floor(j,q)==p.floor&&overlap(p,q))return LayerActions.Result.rejected("A pending construction footprint overlaps this paste");
            if(p.floor==0)for(var q:player.unit().plans)if(overlap(p,q))return LayerActions.Result.rejected("A native construction plan overlaps this paste");
        }
        var job=new Job(player,action.layer(),action.mode());job.volume=preview.placement().clip();
        for(int index:preview.order()){var p=preview.plans().get(index);if(p.floor==0)p.config=LayerConfiguration.decode(job.volume.cells().get(index).config());job.pending.add(p);}
        var previous=new ArrayList<BuildPlan>();for(var p:job.unit.plans)previous.add(p);
        try{for(var p:job.pending)if(floor(job,p)==0)job.unit.addBuild(p);}catch(RuntimeException failed){job.unit.plans.clear();for(var p:previous)job.unit.plans.addLast(p);throw new IllegalArgumentException("Native queue changed; retry the 3D paste",failed);}
        jobs.add(job);for(var p:job.pending)runtime.timings().queued(target(job,p));invalidateTargets();
        return new LayerActions.Result(true,false,"Queued "+job.pending.size()+" 3D entries; ordinary resources / build speed",0);
    }
    private static boolean overlap(BuildPlan a,BuildPlan b){int as=LayerSchematic.platform(a)?1:a.block.size,bs=LayerSchematic.platform(b)?1:b.block.size;int ax=LayerCoordinates.minCell(a.x,as),ay=LayerCoordinates.minCell(a.y,as),bx=LayerCoordinates.minCell(b.x,bs),by=LayerCoordinates.minCell(b.y,bs);return LayerSchematic.platform(a)==LayerSchematic.platform(b)&&ax<bx+bs&&bx<ax+as&&ay<by+bs&&by<ay+as;}
    private void observeGround(Job job){
        for(var it=job.pending.iterator();it.hasNext();){var p=it.next();if(floor(job,p)!=0)continue;
            boolean queued=false;for(var q:job.unit.plans)if(q.x==p.x&&q.y==p.y){queued=true;break;}if(!queued)job.unit.addBuild(p);
            var b=Vars.world.build(p.x,p.y);if(b!=null&&b.tileX()==p.x&&b.tileY()==p.y&&b.team.id==job.team&&b.block==p.block&&b.rotation==p.rotation){
                job.volumeMade.put(((VolumeClipboard.Plan)p).index,LayerVerticalTransfers.groundId(p.x,p.y));removeNative(job,p);runtime.priority().completed(job.player,target(job,p));
                boolean head=job.pending.peek()==p;it.remove();job.parkedWork.remove(p);
                if(head){job.work=job.pending.isEmpty()?0:job.parkedWork.getOrDefault(job.pending.peek(),0.0);if(!job.pending.isEmpty())job.parkedWork.remove(job.pending.peek());}
                invalidateTargets();
            }
        }
    }
    /** A blocked head must not starve independent ready work. Preserve its progress and blueprint. */
    private boolean ready(Job job){
        int limit=Math.min(64,job.pending.size());
        for(int i=0;i<limit;i++){
            var p=job.pending.peek();
            if(floor(job,p)>0&&runtime.priority().allowsUpper(job.player,job.unit,p,floor(job,p))){
                var allowed=runtime.preview(job.player,place(p,floor(job,p),job.mode));
                if(allowed.accepted())return true;
                runtime.acceptResult(allowed);
            }
            job.parkedWork.put(p,job.work);job.pending.remove();job.pending.addLast(p);
            job.work=job.parkedWork.getOrDefault(job.pending.peek(),0.0);job.parkedWork.remove(job.pending.peek());invalidateTargets();
        }
        return false;
    }
    private final Set<Unit> nativeBusy=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final class NativeWork {
        BuildPlan[] plans=new BuildPlan[16];float[] progress=new float[16];int count;
        void capture(Unit u){int n=u.plans.size;if(n>plans.length){int capacity=Math.max(n,plans.length*2);plans=new BuildPlan[capacity];progress=new float[capacity];}count=0;for(var p:u.plans){plans[count]=p;progress[count++]=p.progress;}}
        boolean worked(){boolean changed=false;for(int i=0;i<count;i++){var p=plans[i];if(p.progress!=progress[i]||p.isDone())changed=true;plans[i]=null;}count=0;return changed;}
    }
    private final Map<Unit,NativeWork> nativeBefore=new IdentityHashMap<>();
    public void observeNativeBuilders(){nativeBusy.clear();for(var j:jobs)if(j.unit.updateBuilding()&&!j.unit.plans.isEmpty()){var state=nativeBefore.computeIfAbsent(j.unit,u->new NativeWork());if(state.count==0)state.capture(j.unit);}}
    public void finishNativeBuilders(){for(var e:nativeBefore.entrySet())if(e.getValue().worked())nativeBusy.add(e.getKey());}
    public void update(double dt){
        cachedProgress.clear();
        // One upper-work budget per builder, never one full-speed budget per queued job.
        var remaining=new IdentityHashMap<Unit,Double>();
        for(var iterator=jobs.iterator();iterator.hasNext();){var job=iterator.next();
            if(job.player.team().id!=job.team||job.unit.dead||!job.unit.isAdded()||!job.unit.canBuild()||job.volume!=null&&job.player.con!=null&&job.player.con.hasDisconnected){if(job.volume!=null)for(var p:job.pending)if(floor(job,p)==0)removeNative(job,p);iterator.remove();invalidateTargets();continue;}
            if(job.player.dead()||job.player.unit()!=job.unit||!job.unit.updateBuilding())continue;
            if(job.volume!=null){observeGround(job);if(job.pending.isEmpty()){if(configureVolume(job))iterator.remove();continue;}}
            double speed=job.unit.type.buildSpeed*job.unit.buildSpeedMultiplier*Vars.state.rules.buildSpeed(job.player.team());
            if(!Double.isFinite(speed)||speed<=0)continue;
            double budget=remaining.computeIfAbsent(job.unit,u->dt*60*speed);int completed=0;
            while(!job.pending.isEmpty()&&completed<32){
                if(nativeBusy.contains(job.unit)||!ready(job))break;
                var plan=job.pending.peek();double time=buildTime(plan);
                if(!LayerRuntime.instantConstruction()){
                    double work=Math.min(Math.max(0,time-job.work),budget);job.work+=work;budget-=work;
                    if(job.work+1e-7<time)break;
                }
                var result=runtime.apply(job.player,place(plan,floor(job,plan),job.mode));runtime.acceptResult(result);
                if(!result.accepted())break; // revalidate/pay/mutate atomically; retain failed work
                job.work=0;advance(job);job.made.put(Point2.pack(plan.x,plan.y),result.componentId());job.completed.add(plan);if(job.volume!=null)job.volumeMade.put(((VolumeClipboard.Plan)plan).index,result.componentId());completed++;
                if(!LayerRuntime.instantConstruction()&&budget<=1e-9)break;
            }
            remaining.put(job.unit,budget);
            if(job.pending.isEmpty()){if(job.volume==null){configure(job);iterator.remove();}else if(configureVolume(job))iterator.remove();}
        }
        cachedProgress.clear();nativeBusy.clear();nativeBefore.keySet().removeIf(u->jobs.stream().noneMatch(j->j.unit==u));
    }
    private boolean configureVolume(Job job){
        long now=arc.util.Time.millis();if(now<job.nextConfigure)return false;job.nextConfigure=now+250;int failed=0;
        for(int i=0;i<job.volume.cells().size();i++){if(job.configuredCells.contains(i))continue;var c=job.volume.cells().get(i);Long id=job.volumeMade.get(i);if(id==null){failed++;continue;}Object value=LayerConfiguration.decode(c.config());
            if(c.platform()||value==null||runtime.volumeConfigure(job.player,id,value))job.configuredCells.add(i);else failed++;
        }
        for(int i=0;i<job.volume.links().size();i++){if(job.configuredLinks.contains(i))continue;var link=job.volume.links().get(i);Long a=job.volumeMade.get(link.from()),b=job.volumeMade.get(link.to());
            if(a!=null&&b!=null&&runtime.volumeLink(job.player,a,b,link.kind()))job.configuredLinks.add(i);else failed++;
        }
        if(failed>0)runtime.acceptResult(LayerActions.Result.rejected("3D structure built; "+failed+" configurations / links waiting for range, resources or valid endpoints"));return failed==0;
    }
    private void configure(Job job){
        int failed=0;var selected=job.made.keySet();
        for(var p:job.completed){if(LayerSchematic.platform(p)||p.config==null)continue;long id=job.made.get(Point2.pack(p.x,p.y));
            var live=runtime.grid().get(id);if(live==null||live.team()!=job.team)continue;
            Object value=LayerSchematic.internalConfig(p.block,p.config,p.x,p.y,selected);
            if(value instanceof Point2[] points&&p.block instanceof PowerNode){for(var point:points){Long target=job.made.get(Point2.pack(p.x+point.x,p.y+point.y));if(target==null||!runtime.schematicPowerLink(job.player,id,target))failed++;}}
            else if(value!=null&&!runtime.apply(job.player,LayerActions.Action.configure(id,value)).accepted())failed++;
        }
        if(failed>0)runtime.acceptResult(LayerActions.Result.rejected(failed+" schematic configurations/links could not be applied"));
    }
    public static LayerActions.Action place(BuildPlan p,int layer,LayerComponent.Mode mode){return LayerActions.Action.place(p.x,p.y,layer,p.block.name,LayerSchematic.platform(p),p.rotation,mode);}
}
