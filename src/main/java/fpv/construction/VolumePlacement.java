package fpv.construction;

import java.util.*;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.world.blocks.defense.Wall;

/** Atomic dry-run of a whole clipboard, including proposed Z0 foundations; no live world writes. */
public final class VolumePlacement {
    public record Preview(VolumeClipboard.Placement placement,List<VolumeClipboard.Plan> plans,boolean[] valid,List<Integer> order,String reason){
        public boolean accepted(){for(boolean b:valid)if(!b)return false;return !plans.isEmpty()&&order.size()==plans.size();}
        public LayerActions.Result result(){return accepted()?new LayerActions.Result(true,false,"3D construction positions valid; ordinary costs apply",0):LayerActions.Result.rejected(reason.length()>240?reason.substring(0,240):reason.isBlank()?"Invalid 3D structure":reason);}
    }
    private VolumePlacement(){}
    public static Preview preview(LayerRuntime runtime,Player player,LayerActions.Action action,boolean filters){
        var clip=VolumeClipboard.decode(action.configuration());var pos=new VolumeClipboard.Placement(clip,action.x(),action.y(),action.layer());
        if(Math.abs(clip.spacing()-runtime.grid().coordinates().spacing())>1e-7)throw new IllegalArgumentException("Clipboard floor spacing differs from this world");
        if(pos.x()+clip.width()>Vars.world.width()||pos.y()+clip.depth()>Vars.world.height()||pos.z()+clip.floors()-1>runtime.grid().coordinates().maxLayers())throw new IllegalArgumentException("3D footprint / floors outside world bounds");
        if(action.mode()==LayerComponent.Mode.FREE&&!runtime.allowedFree())throw new IllegalArgumentException("Free construction must be enabled by the host");
        var plans=pos.plans();validateLinks(runtime,clip,plans);boolean[] valid=new boolean[plans.size()];Arrays.fill(valid,true);String reason="";
        var trial=new LayerWorld(runtime.grid().coordinates());trial.apply(runtime.grid().snapshot());var added=new LinkedHashMap<Integer,LayerComponent>();
        var foundations=new HashMap<Long,StructuralSolver.Foundation>();
        for(var p:plans)if(p.floor==0){
            var check=p.platform?LayerActions.Result.rejected("Platforms require Z1 or higher"):runtime.volumeGround(player,p,filters);
            if(!check.accepted()){valid[p.index]=false;if(reason.isBlank())reason="Z0: "+check.reason();continue;}
            if(p.block instanceof Wall){var profile=runtime.catalog().profile(p.block);if(profile.capacity()>0){
                var root=new StructuralSolver.Foundation(Long.MIN_VALUE+p.index,player.team().id,Math.min(fpv.net.HeadlessGeometry.heightFor(p.block),runtime.planeHeight(1)),profile.span(),profile.capacity());
                int x=LayerCoordinates.minCell(p.x,p.block.size),y=LayerCoordinates.minCell(p.y,p.block.size);for(int tx=x;tx<x+p.block.size;tx++)for(int ty=y;ty<y+p.block.size;ty++)foundations.put(cell(tx,ty),root);
            }}
        }
        for(var p:plans)if(p.floor>0){
            try{var a=LayerPasteQueue.place(p,p.floor,action.mode());var check=runtime.previewSchematicCell(player,a,trial);
                if(!check.accepted()||filters&&!runtime.volumeFilter(player,p)){valid[p.index]=false;if(reason.isBlank())reason="Z"+p.floor+": "+(check.accepted()?"Server action filter denied construction":check.reason());continue;}
                var c=runtime.schematicComponent(a,trial.nextId(),player.team().id);trial.add(c);added.put(p.index,c);
            }catch(IllegalArgumentException bad){valid[p.index]=false;if(reason.isBlank())reason="Overlapping or invalid 3D footprint";}
        }
        // Proposed ground buildings must also clear the entire proposed elevated volume.
        for(var ground:plans)if(ground.floor==0&&!ground.platform)for(var c:added.values()){
            double cx=LayerCoordinates.center(ground.x,ground.block.size),cy=LayerCoordinates.center(ground.y,ground.block.size);
            var b=new LayerCoordinates.Bounds(cx-ground.block.size*.5,cy-ground.block.size*.5,0,cx+ground.block.size*.5,cy+ground.block.size*.5,fpv.net.HeadlessGeometry.heightFor(ground.block));
            var upper=trial.coordinates().bounds(c);boolean joint=c.kind()==LayerComponent.Kind.PLATFORM&&ground.block instanceof Wall&&Math.abs(b.maxZ()-upper.maxZ())<1e-7;
            if(b.overlaps(upper)&&!joint){valid[ground.index]=false;if(reason.isBlank())reason="Copied upper structure intersects a copied Z0 building";}
        }
        validateShafts(runtime,clip,plans,trial,added);
        StructuralSolver.Foundations source=(x,y,team)->{var f=foundations.get(cell(x,y));return f==null?runtime.foundation(x,y,team):f;};
        try(var solver=new StructuralSolver(trial,source)){
            solver.solveDirty();for(var pair:added.entrySet())if(action.mode()==LayerComponent.Mode.STRUCTURAL&&!solver.support(pair.getValue().id()).supported()){valid[pair.getKey()]=false;if(reason.isBlank())reason="Unsupported: "+solver.support(pair.getValue().id()).reason();}
            if(action.mode()==LayerComponent.Mode.STRUCTURAL)for(var c:runtime.grid().components())if(runtime.solver().support(c.id()).supported()&&!solver.support(c.id()).supported()){Arrays.fill(valid,false);reason="Copied load exceeds existing structural capacity";break;}
        }
        if(!runtime.schematicBudget(new ArrayList<>(added.values()))){Arrays.fill(valid,false);reason="Construction save/network budget exceeded";}
        var order=new ArrayList<Integer>();for(var p:plans)if(p.floor==0)order.add(p.index);
        // Check that the final supported structure has an actually constructible prefix order.
        var prefix=new LayerWorld(runtime.grid().coordinates());prefix.apply(runtime.grid().snapshot());var remaining=new ArrayList<>(added.keySet());
        remaining.sort(Comparator.comparingInt((Integer i)->plans.get(i).floor).thenComparingInt(i->added.get(i).column()?0:plans.get(i).platform?1:2));
        boolean all=true;for(boolean v:valid)all&=v;
        if(all)try(var solver=new StructuralSolver(prefix,source)){
            while(!remaining.isEmpty()){
                int before=remaining.size();for(var it=remaining.iterator();it.hasNext();){int i=it.next();var c=added.get(i);if(action.mode()==LayerComponent.Mode.FREE||solver.supports(c)){prefix.add(c);order.add(i);it.remove();}}
                if(remaining.size()==before){for(int i:remaining)valid[i]=false;reason="No safe support-first construction order; add supporting columns";break;}
            }
        }
        return new Preview(pos,plans,valid,List.copyOf(order),reason);
    }
    private static void validateShafts(LayerRuntime runtime,VolumeClipboard.Clip clip,List<VolumeClipboard.Plan> plans,LayerWorld trial,Map<Integer,LayerComponent> added){
        for(var link:clip.links())if(link.kind()==VolumeClipboard.LinkKind.ITEMS||link.kind()==VolumeClipboard.LinkKind.LIQUIDS){
            var a=plans.get(link.from());var b=plans.get(link.to());var lower=a.floor<b.floor?a:b;var upper=lower==a?b:a;
            var e=runtime.catalog().entry(lower.block.name,false,clip.spacing());double bottom=runtime.planeHeight(lower.floor)+(lower.floor==0?fpv.net.HeadlessGeometry.heightFor(lower.block):e==null?1:e.height()),top=runtime.planeHeight(upper.floor),x=LayerCoordinates.center(a.x,a.block.size),y=LayerCoordinates.center(a.y,a.block.size);
            if(top<bottom||runtime.volumeTerrain((int)Math.round(x),(int)Math.round(y))>bottom+1e-6)throw new IllegalArgumentException("Terrain blocks a copied vertical shaft");
            var shaft=new LayerCoordinates.Bounds(x-.12,y-.12,bottom,x+.12,y+.12,top);var ca=added.get(a.index);var cb=added.get(b.index);
            for(var c:trial.query(shaft))if(c.kind()!=LayerComponent.Kind.PLATFORM&&(ca==null||c.id()!=ca.id())&&(cb==null||c.id()!=cb.id())&&shaft.overlaps(trial.coordinates().bounds(c)))throw new IllegalArgumentException("A structure blocks a copied vertical shaft");
        }
    }
    private static void validateLinks(LayerRuntime runtime,VolumeClipboard.Clip clip,List<VolumeClipboard.Plan> plans){
        var nodeCounts=new HashMap<Integer,Integer>();var outputs=new HashSet<String>();
        for(var link:clip.links()){
            var a=plans.get(link.from());var b=plans.get(link.to());double ax=LayerCoordinates.center(a.x,a.block.size),ay=LayerCoordinates.center(a.y,a.block.size),bx=LayerCoordinates.center(b.x,b.block.size),by=LayerCoordinates.center(b.y,b.block.size),dz=(a.floor-b.floor)*clip.spacing();
            if(link.kind()==VolumeClipboard.LinkKind.POINT)continue;
            if(a.floor!=b.floor&&Math.abs(a.floor-b.floor)>runtime.transfers().maxLayerGap())throw new IllegalArgumentException("Internal link exceeds host floor-gap limit");
            if(link.kind()==VolumeClipboard.LinkKind.POWER){
                if(!a.block.hasPower||!b.block.hasPower||!a.block.connectedPower||!b.block.connectedPower||!powerReach(a,b,ax-bx,ay-by,dz)&&!powerReach(b,a,ax-bx,ay-by,dz))throw new IllegalArgumentException("Invalid internal XYZ power range / endpoints");
                for(int i:new int[]{link.from(),link.to()})if(plans.get(i).block instanceof mindustry.world.blocks.power.PowerNode node&&nodeCounts.merge(i,1,Integer::sum)>node.maxNodes)throw new IllegalArgumentException("Copied power node connection limit exceeded");
                continue;
            }
            if(a.floor==b.floor)throw new IllegalArgumentException("Vertical connection requires different floors");
            boolean liquid=link.kind()==VolumeClipboard.LinkKind.LIQUIDS||link.kind()==VolumeClipboard.LinkKind.BRIDGE_LIQUIDS;
            if(liquid?(!a.block.hasLiquids||!b.block.hasLiquids):(!a.block.hasItems||!b.block.hasItems))throw new IllegalArgumentException("Incompatible copied inventory endpoints");
            if(!outputs.add(link.from()+":"+liquid))throw new IllegalArgumentException("Duplicate copied inventory output");
            if(link.kind()==VolumeClipboard.LinkKind.BRIDGE_ITEMS||link.kind()==VolumeClipboard.LinkKind.BRIDGE_LIQUIDS){
                double range=a.block instanceof mindustry.world.blocks.distribution.ItemBridge bridge?bridge.range:a.block instanceof mindustry.world.blocks.distribution.DirectionBridge bridge?bridge.range:0;
                if(a.block!=b.block||range<=0||(ax-bx)*(ax-bx)+(ay-by)*(ay-by)+dz*dz>range*range+1e-6)throw new IllegalArgumentException("Invalid internal XYZ bridge range / types");
            }else if(Math.abs(ax-bx)>1e-7||Math.abs(ay-by)>1e-7)throw new IllegalArgumentException("Copied vertical connectors must align");
        }
    }
    private static boolean powerReach(VolumeClipboard.Plan a,VolumeClipboard.Plan b,double dx,double dy,double dz){
        if(!(a.block instanceof mindustry.world.blocks.power.PowerNode node)||node.sameBlockConnection&&a.block!=b.block)return false;
        dx=Math.max(0,Math.abs(dx)-b.block.size*.5);dy=Math.max(0,Math.abs(dy)-b.block.size*.5);return dx*dx+dy*dy+dz*dz<=node.laserRange*node.laserRange+1e-7;
    }
    private static long cell(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
}
