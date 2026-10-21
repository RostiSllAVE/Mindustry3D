package fpv.input;

import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.world.Block;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Approved Free Mode does not bypass footprint/headroom/range, clipboard or native builder rules. */
public final class ManufacturingConstructionTest {
    static int checks;static void verify(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static long place(int x,int y,int z,Block b,boolean platform,int rotation){var r=runtime.apply(Vars.player,LayerActions.Action.place(x,y,z,b.name,platform,rotation,LayerComponent.Mode.FREE));verify(r.accepted(),b.name+" Z"+z+": "+r.reason());return r.componentId();}
    static void factory(int x,int y,int layer,Block b,int rotation){int low=-(b.size-1)/2;for(int dy=0;dy<b.size;dy++)for(int dx=0;dx<b.size;dx++)place(x+low+dx,y+low+dy,layer,Blocks.air,true,0);long id=place(x,y,layer,b,false,rotation);verify(runtime.building(id).block==b,"actual native crafter exists on approved platform");}
    public static void main(String[] args)throws Exception{
        init();try{
            Vars.state.rules.instantBuild=true;unit.set(32*8,34*8);Block[] types={Blocks.graphitePress,Blocks.multiPress,Blocks.siliconSmelter,Blocks.siliconCrucible,Blocks.pyratiteMixer,Blocks.blastMixer};
            for(int floor=1;floor<=5;floor++)for(int i=0;i<types.length;i++)factory(20+i*4,20+floor*4,floor,types[i],i%4);
            for(int floor:new int[]{1,3,5})for(int i=0;i<types.length;i++)factory(20+i*4,46,floor,types[i],i%4);
            var blocked=runtime.apply(Vars.player,LayerActions.Action.place(20,46,2,Blocks.air.name,true,0,LayerComponent.Mode.FREE));verify(!blocked.accepted(),"adjacent-floor ceiling through a physical factory must remain invalid");
            var selection=VolumeClipboardNativeTest.select(18,22,43,48,1,5);var clip=VolumeClipboard.capture(runtime,selection,Vars.player).clip();verify(clip.floors()==5,"true five-floor clipboard");verify(clip.cells().stream().filter(c->!c.platform()).count()==48,"48 factory entries captured once");clip=VolumeClipboard.decode(VolumeClipboard.encode(clip));var rotation=VolumeClipboard.rotate(clip);verify(rotation.cells().stream().filter(c->!c.platform()).count()==48,"rotation preserves all machine entries");
            var action=LayerActions.Action.volume(new VolumeClipboard.Placement(clip,60,55,1),LayerComponent.Mode.FREE);unit.set(73*8,68*8);var preview=runtime.pastes().previewVolume(Vars.player,action,false);verify(preview.accepted(),"actual footprint/headroom preview: "+preview.reason());verify(runtime.apply(Vars.player,action).accepted(),"existing construction queue accepts approved volume");
            for(int t=0;t<24000&&runtime.pastes().pending(Vars.player)>0;t++)tick();verify(runtime.pastes().pending(Vars.player)==0,"native/upper scheduler completes queued factories: "+runtime.status());verify(runtime.grid().components().stream().filter(c->c.kind()==LayerComponent.Kind.BLOCK).count()==96,"copy creates 48 factories and preserves 48 sources");
            for(var e:runtime.nativeBuildings().entries())if(e.building() instanceof mindustry.world.blocks.production.GenericCrafter.GenericCrafterBuild b)verify(b.progress==0&&b.items.total()==0,"schematic never clones runtime production or inventory");
            var bytes=runtime.replication();var restore=LayerRuntime.class.getDeclaredMethod("restore",byte[].class,boolean.class);restore.setAccessible(true);restore.invoke(runtime,bytes,false);verify(runtime.grid().components().stream().filter(c->c.kind()==LayerComponent.Kind.BLOCK).count()==96,"existing save/replication roundtrip retains all machine placements");
            System.out.println("ManufacturingConstructionTest: "+checks+" authorized Z1-Z5/stacked Z1,Z3,Z5/headroom/3D clipboard/builder/save checks PASS; physical UI separate");
        }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
}
