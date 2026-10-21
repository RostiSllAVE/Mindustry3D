package fpv.input;

import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.world.Block;import mindustry.game.Team;import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Real server-authorized platforms/placements and existing 3D clipboard; no GPU/input acceptance. */
public final class LogisticsConstructionNativeTest {
    static int checks;static void verify(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static long place(int x,int y,int z,Block b,boolean platform,int rotation){var result=runtime.apply(Vars.player,LayerActions.Action.place(x,y,z,b.name,platform,rotation,LayerComponent.Mode.FREE));verify(result.accepted(),b.name+" Z"+z+": "+result.reason());return result.componentId();}
    public static void main(String[] args)throws Exception{
        init();try{
            Vars.state.rules.instantBuild=true;unit.set(32*8,32*8);
            Block[] types={Blocks.conveyor,Blocks.titaniumConveyor,Blocks.plastaniumConveyor,Blocks.armoredConveyor,Blocks.junction,Blocks.router,Blocks.distributor,Blocks.sorter,Blocks.invertedSorter,Blocks.overflowGate,Blocks.underflowGate};
            for(int layer=1;layer<=5;layer++)for(int i=0;i<types.length;i++){
                var b=types[i];int x=20+(i%6)*4,y=28+(i/6)*5,low=-(b.size-1)/2;
                for(int dy=0;dy<b.size;dy++)for(int dx=0;dx<b.size;dx++)place(x+low+dx,y+low+dy,layer,Blocks.air,true,0);
                long id=place(x,y,layer,b,false,i%4);var nativeB=runtime.building(id);verify(nativeB!=null&&nativeB.block==b&&runtime.grid().get(id).layer()==layer,"live native model target on actual platform Z"+layer);
                if(nativeB instanceof mindustry.world.blocks.distribution.Sorter.SorterBuild s)runtime.nativeBuildings().configureNative(id,Items.lead);
            }
            var selection=VolumeClipboardNativeTest.select(18,26,43,35,1,5);var clip=VolumeClipboard.capture(runtime,selection,Vars.player).clip();verify(clip.floors()==5,"all five upper floors captured together");verify(clip.cells().stream().filter(c->!c.platform()).count()==55,"all eleven logistics types on five floors captured once");
            clip=VolumeClipboard.decode(VolumeClipboard.encode(clip));var rotated=VolumeClipboard.rotate(clip);verify(rotated.cells().stream().anyMatch(c->c.name().equals(Blocks.sorter.name)&&LayerConfiguration.decode(c.config())==Items.lead),"sorter native filter retained by 3D clipboard rotation");
            var action=LayerActions.Action.volume(new VolumeClipboard.Placement(clip,48,40,1),LayerComponent.Mode.FREE);unit.set(57*8,45*8);var preview=runtime.pastes().previewVolume(Vars.player,action,false);verify(preview.accepted(),"multi-floor logistics preview valid: "+preview.reason());
            verify(runtime.apply(Vars.player,action).accepted(),"server accepts existing volume construction queue");unit.set(57*8,45*8);
            for(int t=0;t<16000&&runtime.pastes().pending(Vars.player)>0;t++)tick();verify(runtime.pastes().pending(Vars.player)==0,"existing builder completes actual multi-floor logistics fragment: "+runtime.status());
            verify(runtime.grid().components().stream().filter(c->c.kind()==LayerComponent.Kind.BLOCK).count()==110,"copied logistics completed without losing source blocks");
            for(var e:runtime.nativeBuildings().entries())if(e.building().block==Blocks.sorter||e.building().block==Blocks.invertedSorter)verify(e.building().config()==Items.lead,"actual pasted filter configuration retained");
            var bytes=runtime.replication();var restore=LayerRuntime.class.getDeclaredMethod("restore",byte[].class,boolean.class);restore.setAccessible(true);restore.invoke(runtime,bytes,false);verify(runtime.grid().components().stream().filter(c->c.kind()==LayerComponent.Kind.BLOCK).count()==110,"existing save/network state roundtrip retains all floors");
            System.out.println("LogisticsConstructionNativeTest: "+checks+" authorized platform placement/Z1-Z5/3D clipboard preview+rotation+actual builder/filter/save checks PASS; physical UI separate");
        }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
}
