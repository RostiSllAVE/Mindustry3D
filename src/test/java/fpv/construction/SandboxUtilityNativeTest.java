package fpv.construction;

import arc.util.Time;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;
import mindustry.world.*;import mindustry.world.blocks.sandbox.*;import mindustry.world.blocks.payloads.*;

/** Real native inventories, configuration and void consumption, with layer-local neighbors. */
public final class SandboxUtilityNativeTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static long id=50000;static LayerBuildingAdapter registry;
    static Building create(Block b,int x,int y,int floor){
        if(floor==0){Vars.world.tile(x,y).setBlock(b,Team.sharded,0);return Vars.world.build(x,y);}
        return registry.create(++id,x,y,floor,b,Team.sharded,0);
    }
    static void update(Building... buildings){for(int i=0;i<60;i++){Time.time++;for(var b:buildings){b.efficiency=1;b.updateTile();}}}
    public static void main(String[] args)throws Exception{LayerCampaignNativeTest.main(args);Vars.netServer=new mindustry.core.NetServer();registry=new LayerBuildingAdapter(null);
        try{for(int floor:new int[]{0,1,5}){
            registry.clear();for(var t:Vars.world.tiles)t.setBlock(Blocks.air);
            var source=(ItemSource.ItemSourceBuild)create(Blocks.itemSource,8,8,floor);source.outputItem=Items.copper;
            var storage=create(Blocks.container,9,8,floor);source.updateProximity();storage.updateProximity();update(source,storage);check(storage.items.get(Items.copper)>0,"native item source -> inventory at Z"+floor);
            var iv=create(Blocks.itemVoid,12,8,floor);check(iv.acceptItem(source,Items.copper),"item void accepts");iv.handleItem(source,Items.copper);check(iv.items==null||iv.items.total()==0,"item void destroys, not stores");
            var ls=(LiquidSource.LiquidSourceBuild)create(Blocks.liquidSource,8,14,floor);ls.source=Liquids.water;
            var tank=create(Blocks.liquidRouter,9,14,floor);ls.updateProximity();tank.updateProximity();update(ls,tank);check(tank.liquids.get(Liquids.water)>0,"native configured liquid source transfers at Z"+floor);
            var lv=create(Blocks.liquidVoid,12,14,floor);check(lv.acceptLiquid(ls,Liquids.water),"liquid void accepts");lv.handleLiquid(ls,Liquids.water,10);check(lv.liquids==null||lv.liquids.get(Liquids.water)==0,"liquid void consumes actual water");
            var ps=(PayloadSource.PayloadSourceBuild)create(Blocks.payloadSource,18,22,floor);ps.unit=UnitTypes.flare;
            var pv=create(Blocks.payloadVoid,18+(Blocks.payloadSource.size+Blocks.payloadVoid.size)/2,22,floor);ps.updateProximity();pv.updateProximity();
            ps.updateTile();check(ps.payload instanceof UnitPayload&&((UnitPayload)ps.payload).unit.type==UnitTypes.flare,"real configured unit payload created at Z"+floor);
            Unit unit=((UnitPayload)ps.payload).unit;ps.unit=null;update(ps,pv);check(ps.payload==null&&!unit.isAdded(),"payload void consumes without spawning duplicate actor at Z"+floor);
            var power=create(Blocks.powerSource,8,28,floor);check(power.getPowerProduction()>0,"native power source generation retained");
            var powerVoid=create(Blocks.powerVoid,9,28,floor);check(powerVoid.block.consPower!=null&&powerVoid.block.consPower.usage>0,"native power void consumption retained");
        }}finally{registry.clear();}
        System.out.println("SandboxUtilityNative: "+checks+" real source/void/config/transfer checks at Z0/Z1/Z5 PASS");
    }
}
