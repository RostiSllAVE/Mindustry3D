package fpv.units;

import arc.util.Time;import fpv.construction.*;import fpv.terrain.TerrainElevation;import fpv.world.World3DAdapter;
import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;
import mindustry.world.blocks.units.UnitFactory;import mindustry.world.blocks.payloads.*;

/** Actual production -> conveyor -> router -> world, including upper layer native proxies. */
public final class PayloadChainNativeTest {
    static int checks,births;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        NavalFactoryNativeTest.main(args);Vars.netServer=new mindustry.core.NetServer();Vars.collisions=new mindustry.entities.EntityCollisions();
        var altitude=new UnitAltitudeSystem((x,y)->0);var world=new World3DAdapter(new TerrainElevation(),(x,y)->0,altitude);
        var safety=new FactorySpawnSafety(world,altitude);safety.install();var grid=new LayerWorld(LayerCoordinates.DEFAULT);
        var registry=new LayerBuildingAdapter(new LayerBuildingAdapter.Events(){@Override public boolean releaseUnit(Building b,Unit u,double x,double y){return safety.releaseUpper(b,u,x,y);}});
        world.setConstruction(new LayerPhysicalQueries(()->grid,(id,d)->{},registry::building));
        try{long id=100;
            for(int floor:new int[]{0,1,5})for(var block:new Block[]{Blocks.groundFactory,Blocks.airFactory}){
                registry.clear();grid.clear();for(var u:Groups.unit.copy())u.remove();
                for(var t:Vars.world.tiles){t.setBlock(Blocks.air);t.setFloor(Blocks.stone.asFloor());}
                Building[] builds=new Building[3];Block[] types={block,Blocks.payloadConveyor,Blocks.payloadRouter};
                for(int i=0;i<3;i++){
                    int x=20+i*3;long key=id++;
                    if(floor==0){Vars.world.tile(x,30).setBlock(types[i],Team.sharded,0);builds[i]=Vars.world.build(x,30);}
                    else{builds[i]=registry.create(key,x,30,floor,types[i],Team.sharded,0);var b=builds[i];grid.add(new LayerComponent(key,LayerComponent.Kind.BLOCK,x,30,floor,b.block.name,b.block.size,0,b.team.id,LayerComponent.Mode.FREE,b.health,b.maxHealth,10,fpv.net.HeadlessGeometry.heightFor(b.block),LayerComponent.SupportProfile.NONE,null,null));}
                }
                var factory=(UnitFactory.UnitFactoryBuild)builds[0];var conveyor=(PayloadConveyor.PayloadConveyorBuild)builds[1];var router=(PayloadConveyor.PayloadConveyorBuild)builds[2];
                for(var b:builds){b.updateProximity();b.onProximityUpdate();}
                // Router's only valid direction is the open forward exit; side payload receivers
                // are absent. Native selection, timings and acceptPayload remain in control.
                factory.currentPlan=0;factory.efficiency=1;for(var item:Vars.content.items())factory.items.add(item,10000);
                factory.progress=((UnitFactory)block).plans.first().time;factory.updateTile();Unit unit=factory.payload.unit;int cost=factory.items.total(),before=safety.releases();
                boolean beltSeen=false,routerSeen=false;
                for(int tick=0;tick<1600&&!unit.isAdded();tick++){Time.time++;factory.moveOutPayload();conveyor.updateTile();router.updateTile();beltSeen|=conveyor.item instanceof UnitPayload;routerSeen|=router.item instanceof UnitPayload;}
                check(beltSeen&&routerSeen,"native factory chain transports "+unit.type.name+" at Z"+floor+" belt="+beltSeen+" router="+routerSeen+" added="+unit.isAdded()+" held="+(factory.payload!=null));
                check(unit.isAdded()&&factory.payload==null&&conveyor.item==null&&router.item==null,"actual unit released once at Z"+floor);
                check(safety.releases()==before+1&&factory.items.total()==cost,"single server birth and no repeat production cost");
                check(altitude.unitState(unit).z()>=grid.coordinates().plane(floor),"birth altitude retains source floor");
                unit.move(2,0);check(unit.x>router.x,"released actor can move away");births++;
            }
            System.out.println("PayloadChainNative: "+checks+" checks, "+births+" real ground/flying chain releases at Z0/Z1/Z5 PASS");
        }finally{registry.clear();safety.close();altitude.close();}
    }
}
