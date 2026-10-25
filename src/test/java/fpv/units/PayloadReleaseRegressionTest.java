package fpv.units;

import arc.Events;
import arc.util.Time;
import fpv.terrain.TerrainElevation;
import fpv.world.World3DAdapter;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.blocks.payloads.*;

/** Real native dump must use XYZ occupancy; restored conveyor payloads need the same boundary. */
public final class PayloadReleaseRegressionTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] args){
        NavalFactoryNativeTest.main(args);
        Vars.netServer=new mindustry.core.NetServer();
        for(var t:Vars.world.tiles){t.setBlock(Blocks.air);t.setFloor(Blocks.stone.asFloor());}
        var altitude=new UnitAltitudeSystem((x,y)->0);
        var world=new World3DAdapter(new TerrainElevation(),(x,y)->0,altitude);
        var safety=new FactorySpawnSafety(world,altitude);safety.install();
        try{
            var tile=Vars.world.tile(30,30);tile.setBlock(Blocks.groundFactory,Team.sharded,0);
            var source=(PayloadBlock.PayloadBlockBuild<?>)tile.build;
            Unit out=UnitTypes.dagger.create(Team.sharded);out.set(source.x+12,source.y);
            set(source,new UnitPayload(out));Events.fire(new EventType.WorldLoadEvent());
            var pose=safety.placement(out,source);check(pose!=null,"clear native factory exit");
            Unit overhead=UnitTypes.dagger.create(Team.sharded);overhead.set((float)(pose.x()*8),(float)(pose.y()*8));overhead.add();altitude.initializePayloadRelease(overhead,12);
            Groups.unit.tree().insert(overhead);
            check(safety.placement(out,source)!=null,"overhead unit does not intersect XYZ hull");
            Vars.state.rules.unitCap=0;check(!source.payload.dump()&&!out.isAdded(),"native unit cap still blocks authoritative creation");Vars.state.rules.unitCap=1000;
            check(source.payload.dump(),"ground factory release must not be blocked by a different Z floor");
            check(out.isAdded()&&safety.releases()==1,"single authoritative tracked factory birth");
            source.payload.dump();check(safety.releases()==1,"duplicate dump cannot create or announce a second actor");
            out.remove();overhead.remove();source.payload=null;tile.setBlock(Blocks.air);
            Vars.world.tile(40,40).setBlock(Blocks.payloadConveyor,Team.sharded,0);
            var belt=(PayloadConveyor.PayloadConveyorBuild)Vars.world.build(40,40);
            Unit cargo=UnitTypes.flare.create(Team.sharded);belt.handlePayload(belt,new UnitPayload(cargo));belt.updatePayload();
            Events.fire(new EventType.WorldLoadEvent());int births=safety.releases();
            for(int i=0;i<500&&belt.item!=null;i++){Time.time++;belt.updateTile();}
            check(belt.item==null&&cargo.isAdded(),"loaded native conveyor unloads real unit");
            check(safety.releases()==births+1,"loaded conveyor uses XYZ release boundary");
            cargo.remove();System.out.println("PayloadReleaseRegression: PASS cross-floor occupancy and restored conveyor release");
        }finally{safety.close();altitude.close();}
    }
    @SuppressWarnings({"unchecked","rawtypes"}) static void set(PayloadBlock.PayloadBlockBuild b,UnitPayload p){b.payload=p;}
}
