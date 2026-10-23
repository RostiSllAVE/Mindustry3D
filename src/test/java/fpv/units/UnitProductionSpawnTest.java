package fpv.units;

import arc.util.Time;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;
import mindustry.world.blocks.units.UnitFactory;import fpv.construction.*;import fpv.terrain.TerrainElevation;import fpv.world.World3DAdapter;

/** All ten actual 160.7 factory plans, four directions and Z0/Z1/Z3/Z5. No synthetic unit.add. */
public final class UnitProductionSpawnTest {
    static int checks,spawns,restricted;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        NavalFactoryNativeTest.main(args);Vars.state.rules.unitCap=1000;Vars.world.setGenerating(true);
        var grid=new LayerWorld(LayerCoordinates.DEFAULT);var altitude=new UnitAltitudeSystem((x,z)->0);
        var world=new World3DAdapter(new TerrainElevation(),(x,z)->0,altitude);var safety=new FactorySpawnSafety(world,altitude);safety.install();
        var registry=new LayerBuildingAdapter(new LayerBuildingAdapter.Events(){
            @Override public boolean releaseUnit(Building source,Unit unit,double x,double y){return safety.releaseUpper(source,unit,x,y);}
            @Override public double unitAltitude(Unit unit){return altitude.unitState(unit).z();}
            @Override public double buildingAltitude(Building building){return world.buildingBase(building);}
        });
        world.setConstruction(new LayerPhysicalQueries(()->grid,(id,d)->{},registry::building));
        try{
            long id=10000;
            for(Block block:new Block[]{Blocks.groundFactory,Blocks.airFactory,Blocks.navalFactory,Blocks.tankFabricator,Blocks.shipFabricator,Blocks.mechFabricator}){
                var type=(UnitFactory)block;
                for(int floor:new int[]{0,1,3,5})for(int rotation=0;rotation<4;rotation++)for(int plan=0;plan<type.plans.size;plan++){
                    registry.clear();grid.clear();for(var u:Groups.unit.copy())u.remove();
                    for(var tile:Vars.world.tiles){tile.setBlock(Blocks.air);tile.setFloor(block==Blocks.navalFactory?Blocks.water.asFloor():Blocks.stone.asFloor());}
                    UnitFactory.UnitFactoryBuild b;long cid=id++;double base=LayerCoordinates.DEFAULT.plane(floor);
                    if(floor==0){Vars.world.tile(30,30).setBlock(block,Team.sharded,rotation);b=(UnitFactory.UnitFactoryBuild)Vars.world.build(30,30);}
                    else{
                        b=(UnitFactory.UnitFactoryBuild)registry.create(cid,30,30,floor,block,Team.sharded,rotation);
                        grid.add(new LayerComponent(cid,LayerComponent.Kind.BLOCK,30,30,floor,block.name,block.size,rotation,Team.sharded.id,LayerComponent.Mode.FREE,b.health,b.maxHealth,10,fpv.net.HeadlessGeometry.heightFor(block),LayerComponent.SupportProfile.NONE,null,null));
                    }
                    var recipe=type.plans.get(plan);for(var stack:recipe.requirements)b.items.set(stack.item,stack.amount);
                    b.currentPlan=plan;b.progress=recipe.time;b.efficiency=1;Time.delta=1;b.updateTile();
                    check(b.payload!=null&&b.payload.unit.type==recipe.unit&&!b.payload.unit.isAdded(),"actual native production: "+block.name+" -> "+recipe.unit.name);
                    for(var stack:recipe.requirements)check(b.items.get(stack.item)==0,"exact native recipe consumed once");
                    var unit=b.payload.unit;int before=safety.releases(),units=Groups.unit.size();float health=unit.health;
                    for(int tick=0;tick<300&&b.payload!=null;tick++){Time.time++;b.moveOutPayload();}
                    if(block==Blocks.navalFactory&&floor>0){
                        check(b.payload!=null&&!unit.isAdded()&&safety.releases()==before&&b.items.total()==0,"upper slabs are not water: retain native naval payload without repeated costs");restricted++;continue;
                    }
                    check(b.payload==null&&unit.isAdded()&&!unit.dead,"native release clears output: "+block.name+" floor="+floor+" rot="+rotation);
                    check(safety.releases()==before+1&&Groups.unit.size()==units+1,"one authoritative native release");
                    double dx=Math.cos(Math.toRadians(b.rotdeg())),dy=Math.sin(Math.toRadians(b.rotdeg()));var pose=altitude.unitState(unit);
                    check((pose.x()-b.x/8d)*dx+(pose.y()-b.y/8d)*dy>block.size*.5+pose.radius(),"full body clears decorative factory envelope");
                    check(Double.isFinite(pose.z())&&pose.z()>=base-.02,"canonical output height matches source floor");
                    check(unit.health==health,"no fictitious damage or spawn protection");
                    for(int tick=0;tick<4;tick++){Time.time++;b.moveOutPayload();}check(Groups.unit.size()==units+1&&b.items.total()==0,"repeated presentation/output cannot duplicate actor or spend twice");
                    spawns++;unit.remove();
                }
            }
            System.out.println("UnitProductionSpawnTest: "+checks+" native cost/XYZ/exit checks, "+spawns+" real plan releases, "+restricted+" dry upper naval outputs safely held PASS; upper adapter fixtures do not relax placement rules");
        }finally{safety.close();altitude.close();registry.clear();Groups.clear();}
    }
}
