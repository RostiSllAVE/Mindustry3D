package fpv.construction;

import java.util.*;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.*;
import mindustry.world.Block;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;

/** Selects work in the existing queues. No independent plan queue, costs or movement. */
public final class ConstructionPriority {
    public record Target(int unitId,int x,int y,int layer,boolean platform,Block block,int rotation){
        public Target{if(unitId<0||x<0||y<0||x>32767||y>32767||layer<0||layer>512||rotation<0||rotation>3||platform&&(layer==0||block!=null)||!platform&&block==null)throw new IllegalArgumentException("Invalid blueprint target");}
        public int size(){return platform?1:block.size;}
        public boolean matches(BuildPlan p){return p.x==x&&p.y==y&&LayerSchematic.platform(p)==platform&&(platform||p.block==block)&&p.rotation==rotation&&!p.breaking;}
        public LayerCoordinates.Bounds bounds(LayerRuntime runtime){
            double cx=LayerCoordinates.center(x,size()),cy=LayerCoordinates.center(y,size()),z=runtime.planeHeight(layer);
            double h=platform?LayerCoordinates.PLATFORM_THICKNESS:layer==0?fpv.net.HeadlessGeometry.heightFor(block):runtime.catalog().entry(block.name,false,runtime.grid().coordinates().spacing()).height();
            return new LayerCoordinates.Bounds(cx-size()*.5,cy-size()*.5,platform?z-h:z,cx+size()*.5,cy+size()*.5,platform?z:z+h);
        }
        public BuildingMousePicker.Hit highlight(LayerRuntime runtime){var b=bounds(runtime);return new BuildingMousePicker.Hit(0,null,layer,b.centerX(),b.centerY(),b.maxZ(),b,platform,block);}
    }
    private record Focus(Unit unit,Target target){}
    private final LayerRuntime runtime;
    private final Map<Player,Focus> focused=new IdentityHashMap<>();
    private final Map<Unit,Boolean> suspended=new IdentityHashMap<>();
    public ConstructionPriority(LayerRuntime runtime){this.runtime=runtime;}
    public Target target(Player player){var f=focused.get(player);return f==null?null:f.target;}
    public void clear(Player player){focused.remove(player);}
    public void clear(){afterNative();focused.clear();}
    private boolean owner(Player p,Focus f){return p!=null&&!p.dead()&&(p.con==null||!p.con.hasDisconnected)&&p.unit()==f.unit&&p.team()==f.unit.team&&!f.unit.dead&&f.unit.isAdded()&&f.unit.canBuild();}
    public LayerActions.Result validate(Player player,Target target){
        if(player==null||player.dead()||!player.isBuilder()||!player.unit().canBuild()||player.unit().id!=target.unitId())return LayerActions.Result.rejected("Priority requires your current living builder");
        if(!runtime.withinConstruction(player,target.x(),target.y(),target.size(),target.layer()))return LayerActions.Result.rejected("Outside builder range in XYZ");
        if(target.layer()>0)return runtime.pastes().validatePriority(player,target);
        if(target.platform())return LayerActions.Result.rejected("No ground platform blueprint");
        for(var p:player.unit().plans)if(p.x==target.x()&&p.y==target.y()&&p.breaking)return LayerActions.Result.rejected("Cancel the explicit removal request first");
        BuildPlan plan=find(player.unit(),target);
        var tile=Vars.world.tile(target.x(),target.y());
        boolean physical=tile!=null&&tile.build instanceof ConstructBuild c&&c.team==player.team()&&c.tile==tile&&c.current==target.block()&&c.rotation==target.rotation();
        if(!physical&&!runtime.pastes().contains(player,target)&&(plan==null||plan.isDone()))return LayerActions.Result.rejected("Blueprint is no longer pending in your queue");
        if(tile==null||tile.build!=null&&tile.build.team!=player.team())return LayerActions.Result.rejected("Blueprint belongs to another team");
        return new LayerActions.Result(true,false,"Construction priority selected on Z0",0);
    }
    public LayerActions.Result select(Player player,Target target){
        var result=validate(player,target);if(!result.accepted())return result;
        if(target.layer()==0){runtime.pastes().materializeGround(player,target);promoteGround(player.unit(),target);}
        else runtime.pastes().promote(player,target);
        focused.put(player,new Focus(player.unit(),target));
        return new LayerActions.Result(true,false,"Priority: "+(target.platform()?"platform":target.block().localizedName)+" on Z"+target.layer()+" (pause unchanged)",0);
    }
    /** Native local-player queue edits are submitted by the host's ordinary clientSnapshot. */
    public void promoteGround(Unit unit,Target target){
        BuildPlan p=find(unit,target);
        if(p!=null){if(unit.plans.first()!=p){unit.plans.remove(p);unit.plans.addFirst(p);}return;}
        var b=Vars.world.build(target.x(),target.y());
        if(b instanceof ConstructBuild c&&c.team==unit.team&&c.current==target.block())unit.addBuild(new BuildPlan(c.tileX(),c.tileY(),c.rotation,c.current,c.lastConfig),false);
    }
    private BuildPlan find(Unit u,Target t){for(var p:u.plans)if(t.matches(p))return p;return null;}
    /** Temporarily suspend only native work while an upper priority owns this builder. */
    public void beforeNative(){
        afterNative();if(Vars.net.client())return;
        focused.entrySet().removeIf(e->{var f=e.getValue();return !owner(e.getKey(),f)||(f.target.layer()>0?!runtime.pastes().contains(e.getKey(),f.target):find(f.unit,f.target)==null||f.unit.plans.first()!=find(f.unit,f.target));});
        for(var e:focused.entrySet()){
            var f=e.getValue();var t=f.target;
            if(t.layer()>0){if(runtime.pastes().contains(e.getKey(),t)){suspended.put(f.unit,f.unit.updateBuilding());f.unit.updateBuilding(false);}}
        }
        runtime.pastes().suspendGroundRange();runtime.pastes().observeNativeBuilders();
    }
    /** Restore the user's F3 flag before upper work, snapshots, menus or any next frame. */
    public void afterNative(){for(var e:suspended.entrySet())e.getKey().updateBuilding(e.getValue());suspended.clear();runtime.pastes().restoreGroundRange();runtime.pastes().finishNativeBuilders();}
    public boolean allowsUpper(Player player,Unit unit,BuildPlan plan,int layer){
        var f=focused.get(player);if(f==null)return true;
        if(!owner(player,f)){focused.remove(player);return true;}
        if(f.target.layer()==0){var selected=find(unit,f.target);if(selected==null||unit.plans.first()!=selected){focused.remove(player);return true;}return false;}
        if(!runtime.pastes().contains(player,f.target)){focused.remove(player);return true;}
        return f.target.layer()==layer&&f.target.matches(plan);
    }
    public void completed(Player player,Target target){var f=focused.get(player);if(f!=null&&f.target.equals(target))focused.remove(player);}
}
