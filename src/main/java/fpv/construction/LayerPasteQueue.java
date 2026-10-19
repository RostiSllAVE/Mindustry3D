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
        VolumeClipboard.Clip volume;final Map<Integer,Long> volumeMade=new HashMap<>();final Set<Integer> configuredCells=new HashSet<>(),configuredLinks=new HashSet<>();long nextConfigure;
        Job(Player p,int layer,LayerComponent.Mode mode){player=p;unit=p.unit();team=p.team().id;this.layer=layer;this.mode=mode;}
    }
    private final LayerRuntime runtime;private final ArrayList<Job> jobs=new ArrayList<>();
    private final Map<Player,List<ConstructionPriority.Target>> cachedTargets=new IdentityHashMap<>();
    private final Map<Player,ArrayDeque<Long>> volumeRequests=new IdentityHashMap<>();
    public boolean claimVolume(Player p){long now=arc.util.Time.millis();var times=volumeRequests.computeIfAbsent(p,k->new ArrayDeque<>());while(!times.isEmpty()&&now-times.peekFirst()>=1000)times.removeFirst();if(times.size()>=2)return false;times.addLast(now);return true;}
    private static int floor(Job j,BuildPlan p){return p instanceof VolumeClipboard.Plan v?v.floor:j.layer;}
    private final Map<Unit,List<BuildPlan>> rangeParked=new IdentityHashMap<>();
    public void suspendGroundRange(){
        restoreGroundRange();for(var j:jobs)if(j.volume!=null&&j.player.unit()==j.unit&&j.unit.isAdded())for(var p:j.pending)if(floor(j,p)==0&&!runtime.withinConstruction(j.player,p.x,p.y,p.block.size,0)){
            for(var actual:j.unit.plans)if(actual.x==p.x&&actual.y==p.y&&!actual.breaking&&actual.block==p.block){rangeParked.computeIfAbsent(j.unit,u->new ArrayList<>()).add(actual);j.unit.plans.remove(actual);break;}
        }
    }
    public void restoreGroundRange(){for(var e:rangeParked.entrySet())if(e.getKey().isAdded()&&!e.getKey().dead)for(var p:e.getValue())if(!e.getKey().plans.contains(p))e.getKey().plans.addLast(p);rangeParked.clear();}
    private List<ConstructionPriority.Target> remoteTargets=List.of();
    public LayerPasteQueue(LayerRuntime runtime){this.runtime=runtime;}
    public int pending(Player player){int total=0;for(var j:jobs)if(j.player==player)total+=j.pending.size();return total;}
    public void clear(){restoreGroundRange();for(var j:jobs)if(j.volume!=null)for(var p:j.pending)if(floor(j,p)==0)removeNative(j,p);jobs.clear();volumeRequests.clear();cachedTargets.clear();remoteTargets=List.of();}
    private void removeNative(Job j,BuildPlan p){var parked=rangeParked.get(j.unit);if(parked!=null)parked.removeIf(q->q.x==p.x&&q.y==p.y&&q.block==p.block&&q.rotation==p.rotation&&!q.breaking);for(var q:j.unit.plans)if(q.x==p.x&&q.y==p.y&&q.block==p.block&&q.rotation==p.rotation&&!q.breaking){j.unit.plans.remove(q);break;}}
    public void cancel(Player player){jobs.removeIf(j->{if(j.player!=player)return false;if(j.volume!=null)for(var p:j.pending)if(floor(j,p)==0)removeNative(j,p);return true;});cachedTargets.clear();runtime.priority().clear(player);}
    public List<ConstructionPriority.Target> targets(Player player){
        if(Vars.net.client())return remoteTargets;
        var cached=cachedTargets.get(player);if(cached!=null)return cached;
        var list=new ArrayList<ConstructionPriority.Target>();for(var j:jobs)if(j.player==player&&j.team==player.team().id&&!j.unit.dead)for(var q:j.pending)list.add(target(j,q));
        var result=List.copyOf(list);if(!result.isEmpty())cachedTargets.put(player,result);return result;
    }
    private ConstructionPriority.Target target(Job j,BuildPlan p){return new ConstructionPriority.Target(j.unit.id,p.x,p.y,floor(j,p),LayerSchematic.platform(p),LayerSchematic.platform(p)?null:p.block,p.rotation);}
    public byte[] replication(Player player){return ConstructionPlanCodec.encode(targets(player));}
    public void applyReplication(byte[] bytes){remoteTargets=ConstructionPlanCodec.decode(bytes);}
    public boolean contains(Player player,ConstructionPriority.Target t){for(var j:jobs)if(j.player==player&&j.team==player.team().id&&j.unit==player.unit()&&j.unit.id==t.unitId() )for(var p:j.pending)if(floor(j,p)==t.layer()&&t.matches(p))return true;return false;}
    public LayerActions.Result validatePriority(Player player,ConstructionPriority.Target t){
        if(!contains(player,t))return LayerActions.Result.rejected("Blueprint is no longer pending in your upper queue");
        for(var j:jobs)if(j.player==player&&j.unit==player.unit() )for(var p:j.pending)if(floor(j,p)==t.layer()&&t.matches(p))return runtime.preview(player,place(p,floor(j,p),j.mode));
        return LayerActions.Result.rejected("Blueprint is no longer pending");
    }
    void materializeGround(Player player,ConstructionPriority.Target t){for(var j:jobs)if(j.volume!=null&&j.player==player&&j.unit==player.unit())for(var p:j.pending)if(floor(j,p)==0&&t.matches(p)){boolean exists=false;for(var q:j.unit.plans)if(t.matches(q)){exists=true;break;}if(!exists)j.unit.addBuild(p);return;}}
    public void promote(Player player,ConstructionPriority.Target t){
        for(var j:jobs)if(j.player==player&&j.unit==player.unit() )for(var p:j.pending)if(floor(j,p)==t.layer()&&t.matches(p)){
            if(j.pending.peek()!=p){j.parkedWork.put(j.pending.peek(),j.work);j.pending.remove(p);j.pending.addFirst(p);j.work=j.parkedWork.getOrDefault(p,0.0);j.parkedWork.remove(p);cachedTargets.clear();}return;
        }
    }
    private void advance(Job j){var p=j.pending.remove();runtime.priority().completed(j.player,target(j,p));j.parkedWork.remove(p);if(!j.pending.isEmpty()){var next=j.pending.peek();j.work+=j.parkedWork.getOrDefault(next,0.0);j.parkedWork.remove(next);}cachedTargets.clear();}
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
        if(job.pending.isEmpty())return LayerActions.Result.rejected("These positions are already queued");jobs.add(job);cachedTargets.clear();
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
        jobs.add(job);cachedTargets.clear();
        return new LayerActions.Result(true,false,"Queued "+job.pending.size()+" 3D entries; ordinary resources / build speed",0);
    }
    private static boolean overlap(BuildPlan a,BuildPlan b){int as=LayerSchematic.platform(a)?1:a.block.size,bs=LayerSchematic.platform(b)?1:b.block.size;int ax=LayerCoordinates.minCell(a.x,as),ay=LayerCoordinates.minCell(a.y,as),bx=LayerCoordinates.minCell(b.x,bs),by=LayerCoordinates.minCell(b.y,bs);return LayerSchematic.platform(a)==LayerSchematic.platform(b)&&ax<bx+bs&&bx<ax+as&&ay<by+bs&&by<ay+as;}
    private void observeGround(Job job){
        for(var it=job.pending.iterator();it.hasNext();){var p=it.next();if(floor(job,p)!=0)continue;
            boolean queued=false;for(var q:job.unit.plans)if(q.x==p.x&&q.y==p.y){queued=true;break;}if(!queued)job.unit.addBuild(p);
            var b=Vars.world.build(p.x,p.y);if(b!=null&&b.tileX()==p.x&&b.tileY()==p.y&&b.team.id==job.team&&b.block==p.block&&b.rotation==p.rotation){
                job.volumeMade.put(((VolumeClipboard.Plan)p).index,LayerVerticalTransfers.groundId(p.x,p.y));removeNative(job,p);runtime.priority().completed(job.player,target(job,p));it.remove();job.parkedWork.remove(p);cachedTargets.clear();
            }
        }
    }
    public void update(double dt){
        for(var iterator=jobs.iterator();iterator.hasNext();){var job=iterator.next();
            if(job.player.team().id!=job.team||job.unit.dead||!job.unit.isAdded()||!job.unit.canBuild()||job.volume!=null&&job.player.con!=null&&job.player.con.hasDisconnected){if(job.volume!=null)for(var p:job.pending)if(floor(job,p)==0)removeNative(job,p);iterator.remove();cachedTargets.clear();continue;}
            // Native clientSnapshot already carries updateBuilding for this player's owned unit.
            // Suspend work and costs without deleting plans or accumulating a resume-time burst.
            if(job.player.dead()||job.player.unit()!=job.unit||!job.unit.updateBuilding())continue;
            if(job.volume!=null){observeGround(job);if(job.pending.isEmpty()){if(configureVolume(job))iterator.remove();continue;}if(floor(job,job.pending.peek())==0){var ground=job.pending.peek();if(!job.unit.plans.contains(ground))job.unit.addBuild(ground);job.work=0;continue;}
                var allowed=runtime.preview(job.player,place(job.pending.peek(),floor(job,job.pending.peek()),job.mode));if(!allowed.accepted()){runtime.acceptResult(allowed);continue;}}
            if(!runtime.priority().allowsUpper(job.player,job.unit,job.pending.peek(),floor(job,job.pending.peek())))continue;
            double speed=job.unit.type.buildSpeed*job.unit.buildSpeedMultiplier*Vars.state.rules.buildSpeed(job.player.team());
            if(!Double.isFinite(speed)||speed<=0)continue;job.work+=dt*60*speed;int completed=0;
            while(!job.pending.isEmpty()&&completed<32){var plan=job.pending.peek();if(job.volume!=null&&floor(job,plan)==0)break;var a=place(plan,floor(job,plan),job.mode);var allowed=runtime.preview(job.player,a);
                if(!runtime.priority().allowsUpper(job.player,job.unit,plan,floor(job,plan)))break;
                if(!allowed.accepted()){
                    runtime.acceptResult(allowed);
                    if(job.volume!=null||allowed.reason().startsWith("Insufficient")||allowed.reason().startsWith("Outside builder range"))break;
                    advance(job);continue;
                }
                double time=LayerSchematic.platform(plan)?60:Math.max(1,plan.block.buildTime*Vars.state.rules.buildCostMultiplier);
                if(!Vars.state.rules.instantBuild&&job.work<time)break;
                var result=runtime.apply(job.player,a);runtime.acceptResult(result);
                if(!result.accepted()){if(job.volume!=null||result.reason().startsWith("Insufficient")||result.reason().startsWith("Outside builder range"))break;advance(job);continue;}
                job.work=Math.max(0,job.work-time);advance(job);job.made.put(Point2.pack(plan.x,plan.y),result.componentId());job.completed.add(plan);if(job.volume!=null)job.volumeMade.put(((VolumeClipboard.Plan)plan).index,result.componentId());completed++;
            }
            if(job.pending.isEmpty()){if(job.volume==null){configure(job);iterator.remove();}else if(configureVolume(job))iterator.remove();}
        }
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
