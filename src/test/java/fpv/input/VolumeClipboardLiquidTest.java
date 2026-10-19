package fpv.input;
import fpv.construction.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
/** Supplemental exact-JAR test: liquid connector remap and atomic blocked-shaft rejection. */
public final class VolumeClipboardLiquidTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static long place(int x,int y,int z,mindustry.world.Block b){var r=runtime.apply(Vars.player,LayerActions.Action.place(x,y,z,b.name,false,0,LayerComponent.Mode.FREE));check(r.accepted(),"fixture "+r.reason());return r.componentId();}
 public static void main(String[] args){init();unit.set(32*8,32*8);Vars.state.rules.instantBuild=true;
  try{
   long a=place(52,27,1,Blocks.liquidRouter),b=place(52,27,3,Blocks.liquidRouter);check(runtime.apply(Vars.player,LayerActions.Action.link(a,b,LayerVerticalTransfers.Kind.LIQUIDS,false)).accepted(),"source liquid shaft valid");
   var m=new StructureVolume(runtime);m.refresh(0);m.begin(52,27,1);m.height(3);m.finish();m.refresh(1);var clip=VolumeClipboard.capture(runtime,m,Vars.player).clip();check(clip.links().size()==1&&clip.links().get(0).kind()==VolumeClipboard.LinkKind.LIQUIDS,"liquid link uses internal indices");
   long obstacle=place(55,31,3,Blocks.copperWall);var before=runtime.grid().snapshot();var action=LayerActions.Action.volume(new VolumeClipboard.Placement(clip,55,31,2),LayerComponent.Mode.FREE);var result=runtime.apply(Vars.player,action);
   check(!result.accepted()&&result.reason().contains("shaft")&&runtime.pastes().pending(Vars.player)==0&&runtime.grid().snapshot().equals(before),"blocked destination shaft rejects whole batch without world/queue mutation");
   runtime.apply(Vars.player,LayerActions.Action.remove(obstacle));check(runtime.apply(Vars.player,action).accepted(),"cleared liquid layout admitted");runtime.update(.1);
   var x=runtime.grid().at(55,31,2,LayerComponent.Kind.BLOCK);var y=runtime.grid().at(55,31,4,LayerComponent.Kind.BLOCK);check(x!=null&&y!=null&&runtime.transfers().links().contains(new LayerVerticalTransfers.Link(x.id(),y.id(),LayerVerticalTransfers.Kind.LIQUIDS)),"liquid link remaps to shifted XYZ endpoints");
   check(runtime.transfers().links().contains(new LayerVerticalTransfers.Link(a,b,LayerVerticalTransfers.Kind.LIQUIDS)),"original liquid connection unchanged");
   System.out.println("VolumeClipboardLiquidTest: "+checks+" checks PASS against production JAR; physical liquids gameplay NOT TESTED.");
  }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
 }
}
