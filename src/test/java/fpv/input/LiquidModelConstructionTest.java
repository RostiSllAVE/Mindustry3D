package fpv.input;

import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.world.Block;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Approved Free Mode does not bypass footprint/headroom/range, clipboard or native builder rules. */
public final class LiquidModelConstructionTest {
    static int checks;static void verify(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static long place(int x,int y,int z,Block b,boolean platform,int rotation){var r=runtime.apply(Vars.player,LayerActions.Action.place(x,y,z,b.name,platform,rotation,LayerComponent.Mode.FREE));verify(r.accepted(),b.name+" Z"+z+": "+r.reason());return r.componentId();}
    static void factory(int x,int y,int layer,Block b,int rotation){int low=-(b.size-1)/2;for(int dy=0;dy<b.size;dy++)for(int dx=0;dx<b.size;dx++)place(x+low+dx,y+low+dy,layer,Blocks.air,true,0);long id=place(x,y,layer,b,false,rotation);verify(runtime.building(id).block==b,"actual native liquid block exists on approved platform");}
    public static void main(String[] args)throws Exception{
        init();try{
            Vars.state.rules.instantBuild=true;Vars.state.rules.infiniteResources=true;unit.set(32*8,34*8);Block[] types={Blocks.conduit,Blocks.pulseConduit,Blocks.platedConduit,Blocks.liquidRouter,Blocks.liquidJunction,Blocks.bridgeConduit,Blocks.phaseConduit,Blocks.liquidContainer,Blocks.liquidTank,Blocks.mechanicalPump,Blocks.rotaryPump,Blocks.impulsePump};
            for(int floor=1;floor<=5;floor++)for(int i=0;i<types.length;i++)factory(20+i*4,20+floor*4,floor,types[i],i%4);
            for(int floor:new int[]{1,3,5})for(int i=0;i<types.length;i++)factory(20+i*4,46,floor,types[i],i%4);
            var blocked=runtime.apply(Vars.player,LayerActions.Action.place(20,46,1,Blocks.conduit.name,false,0,LayerComponent.Mode.FREE));verify(!blocked.accepted(),"occupied floor still rejected");
            var from=runtime.grid().at(44,46,1,LayerComponent.Kind.BLOCK);var to=runtime.grid().at(44,46,3,LayerComponent.Kind.BLOCK);
            verify(runtime.apply(Vars.player,LayerActions.Action.link(from.id(),to.id(),LayerVerticalTransfers.Kind.BRIDGE_LIQUIDS,false)).accepted(),"existing authorized phase-liquid bridge");
            for(var e:runtime.nativeBuildings().entries())if(e.building().liquids!=null)e.building().liquids.set(Liquids.water,e.building().block.liquidCapacity*.5f);
            var selection=VolumeClipboardNativeTest.select(18,22,67,48,1,5);var clip=VolumeClipboard.capture(runtime,selection,Vars.player).clip();verify(!clip.links().isEmpty(),"clipboard captures actual internal liquid bridge");verify(clip.floors()==5,"true five-floor clipboard");verify(clip.cells().stream().filter(c->!c.platform()).count()==96,"96 liquid-block entries captured once");clip=VolumeClipboard.decode(VolumeClipboard.encode(clip));var rotation=VolumeClipboard.rotate(clip);verify(rotation.cells().stream().filter(c->!c.platform()).count()==96,"rotation preserves all machine entries");
            var action=LayerActions.Action.volume(new VolumeClipboard.Placement(clip,60,55,1),LayerComponent.Mode.FREE);unit.set(90*8,68*8);var preview=runtime.pastes().previewVolume(Vars.player,action,false);verify(preview.accepted(),"actual footprint/headroom preview: "+preview.reason());verify(runtime.apply(Vars.player,action).accepted(),"existing construction queue accepts approved volume");
            for(int t=0;t<60000&&runtime.pastes().pending(Vars.player)>0;t++)tick();verify(runtime.pastes().pending(Vars.player)==0,"native/upper scheduler completes queued factories: "+runtime.status());verify(runtime.grid().components().stream().filter(c->c.kind()==LayerComponent.Kind.BLOCK).count()==192,"copy creates 96 liquid blocks and preserves 96 sources");
            verify(runtime.transfers().links().size()>=2,"queued paste restores independent source and copied XYZ liquid edges");
            for(var e:runtime.nativeBuildings().entries()){var b=e.building();if(b.tile.y>=55){verify(b.liquids==null||b.liquids.currentAmount()==0,"copied storage must not clone source liquids");if(b instanceof mindustry.world.blocks.production.Pump.PumpBuild pump)verify(pump.liquidDrop==null,"upper pumps require actual liquid-bearing surface");}}
            var bytes=runtime.replication();var restore=LayerRuntime.class.getDeclaredMethod("restore",byte[].class,boolean.class);restore.setAccessible(true);restore.invoke(runtime,bytes,false);verify(runtime.grid().components().stream().filter(c->c.kind()==LayerComponent.Kind.BLOCK).count()==192,"existing save/replication roundtrip retains all machine placements");
            System.out.println("LiquidModelConstructionTest: "+checks+" authorized Z1-Z5/stacked Z1,Z3,Z5/headroom/3D clipboard/builder/save checks PASS; physical UI separate");
        }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
}
