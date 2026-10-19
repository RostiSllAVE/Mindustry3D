package fpv.input;

import arc.Core;
import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.math.geom.*;
import fpv.construction.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.input.Binding;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Actual native world/input boundary; physical GPU/OS acceptance is deliberately separate. */
public final class VolumeSelectionNativeTest {
    static int checks;
    static void verify(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static class Pointer extends ConstructionPriorityNativeTest.Keys {
        boolean left,right,f4;float wheel;
        @Override public boolean keyDown(KeyBind k){return k==Binding.select?left:k==Binding.deselect||k==Binding.breakBlock?right:super.keyDown(k);}
        @Override public boolean keyTap(KeyBind k){return k==VolumeSelectionTool.toggle?f4:super.keyTap(k);}
        @Override public float axis(KeyCode c){return c==KeyCode.scroll?wheel:0;}
    }
    static Pointer pointer;
    public static void main(String[] args){
        init();input.uninstallProjection();pointer=new Pointer();Core.input=pointer;input=new FpvDesktopInput(pointer,fpv.generated.InputProfiles.create());input.installProjection();input.setFpvEnabled(true);input.setBuildMode(true);input.setFlightAim(128,96);controls=new LayerBuildController(runtime,input,renderer);
        try{membership();gestures();integration();benchmark();System.out.println("VolumeSelectionNativeTest: "+checks+" checks PASS; native fixtures only, GPU/physical gameplay NOT TESTED.");}
        finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
    static LayerComponent add(int x,int y,int z,int size,int rotation,double h,boolean platform){
        var c=new LayerComponent(runtime.grid().nextId(),platform?LayerComponent.Kind.PLATFORM:LayerComponent.Kind.BLOCK,x,y,z,platform?"platform":"copper-wall",size,rotation,Team.sharded.id,LayerComponent.Mode.FREE,100,100,1,h,LayerComponent.SupportProfile.NONE,null,null);runtime.grid().add(c);return c;
    }
    static void select(StructureVolume m,int ax,int ay,int bx,int by,int anchor,int boundary,long now){m.begin(ax,ay,anchor);m.drag(ax,ay,bx,by);m.height(boundary);m.finish();m.refresh(now);}
    static void membership(){
        var m=new StructureVolume(runtime);m.refresh(0);
        Vars.world.tile(40,40).setBlock(Blocks.graphitePress,Team.sharded,2);var b=Vars.world.tile(40,40).build;
        // Generation-mode fixture bypasses BlockIndexer; production uses its existing quadtree.
        Team.sharded.data().buildingTree=new QuadTree<>(new Rect(0,0,1024,1024));Team.sharded.data().buildingTree.insert(b);
        select(m,41,41,41,41,0,0,1);
        verify(m.entries().size()==1&&m.entries().get(0).size()==2,"edge cell includes whole native multiblock exactly once");
        verify(m.entries().get(0).rotation()==2&&m.entries().get(0).team()==Team.sharded.id,"native rotation and team metadata retained");
        select(m,39,39,42,42,0,0,2);verify(m.entries().size()==1,"multi-cell native building deduplicated");
        long q=m.queries();m.refresh(3);verify(m.queries()==q,"unchanged selection reuses cached membership");
        Vars.world.tile(42,40).setBlock(mindustry.world.blocks.ConstructBlock.get(1),Team.sharded,0);var partial=Vars.world.tile(42,40).build;Team.sharded.data().buildingTree.insert(partial);
        select(m,42,40,42,40,0,0,4);verify(m.entries().isEmpty(),"unfinished native construction is excluded");
        var wall=Vars.world.tile(38,40);wall.setBlock(Blocks.copperWall,Team.sharded,0);Team.sharded.data().buildingTree.insert(wall.build);select(m,38,40,38,40,0,0,5);verify(m.entries().size()==1&&m.entries().get(0).blockName().equals("copper-wall"),"non-updating completed native wall is included");
        var a=add(44,44,1,2,3,.8,false);var p=add(44,44,2,2,0,.16,true);var c=add(44,44,2,2,1,.8,false);var d=add(44,44,3,1,0,.8,false);
        select(m,44,44,45,45,1,3,4);verify(m.entries().size()==4,"one actual XYZ volume includes three floors and platform");
        verify(m.entries().stream().anyMatch(e->e.id()==a.id()&&e.rotation()==3&&e.size()==2),"upper direction and footprint retained");
        verify(m.entries().stream().map(StructureVolume.Entry::id).distinct().count()==4,"upper multiblocks not duplicated");
        m.height(1);m.refresh(5);verify(m.entries().size()==1,"shrinking Z removes higher blocks and next-floor slab");
        select(m,44,44,44,44,2,2,6);verify(m.entries().size()==2&&m.entries().stream().anyMatch(StructureVolume.Entry::platform),"slab belongs to its anchor floor including lower face");
        add(48,48,1,1,0,4.2,false);select(m,48,48,48,48,3,3,7);verify(m.entries().size()==1&&m.entries().get(0).layer()==1,"tall component intersects selected physical band even with lower anchor");
        select(m,70,70,72,72,0,4,8);verify(m.entries().isEmpty()&&m.label().contains("Empty volume"),"empty floors and XY area valid");
        m.height(-100);verify(m.area().boundary()==0,"no negative Z");m.height(9999);verify(m.area().boundary()==runtime.grid().coordinates().maxLayers(),"maximum supported Z clamped");
        var rev=runtime.grid().revision();m.refresh(300);verify(runtime.grid().revision()==rev,"selection has no world writes");
        var old=Vars.player.team();Vars.player.team(Team.crux);m.refresh(301);verify(m.area()==null,"team change clears private selection");Vars.player.team(old);m.refresh(302);
        select(m,44,44,45,45,2,2,303);var oldTiles=Vars.world.tiles;Vars.world.tiles=new mindustry.world.Tiles(128,128);m.refresh(304);verify(m.area()==null,"world transition clears selection");Vars.world.tiles=oldTiles;
        try{m.entries().clear();throw new AssertionError("mutable entries");}catch(UnsupportedOperationException expected){verify(true,"immutable metadata view");}
    }
    static BuildingMousePicker.Hit hit(int x,int y,int layer){double z=runtime.planeHeight(layer);return new BuildingMousePicker.Hit(0,null,layer,x,y,z,new LayerCoordinates.Bounds(x-.5,y-.5,z,x+.5,y+.5,z+.2),true);}
    static Ray ray(int x,int y){return new Ray(new Vec3(x,30,-y),new Vec3(0,-1,0));}
    static void gestures(){
        var t=new VolumeSelectionTool(runtime);t.selection().refresh(0);t.enter(false);
        t.pointer(ray(44,44),hit(44,44,2),0,false,true,true,false,false,false,false);
        t.pointer(ray(46,47),hit(46,47,8),0,false,false,true,false,false,false,false);
        verify(t.selection().area().anchor()==2&&t.selection().area().width()==3&&t.selection().area().depth()==4,"drag pins initial visible floor despite subsequent high surfaces");
        t.scroll(1,1);t.scroll(1,1);verify(t.selection().area().boundary()==3,"one wheel sample per frame");t.scroll(-3,2);verify(t.selection().area().lower()==0&&t.selection().area().upper()==2,"wheel extends below fixed anchor");
        t.pointer(ray(46,47),null,0,false,false,false,true,false,false,false);verify(!t.selection().dragging(),"release commits volume");
        t.scroll(4,3);verify(t.selection().area().boundary()==4,"wheel adjusts completed selection");
        t.pointer(ray(60,60),hit(60,60,1),0,false,true,true,false,false,false,false);t.pointer(ray(61,61),null,0,false,false,false,true,false,false,true);
        verify(t.selection().area().minX()==44&&!t.selection().dragging(),"UI release abandons draft and retains last completed volume");
        t.pointer(ray(62,62),hit(62,62,1),5,true,true,true,false,false,false,false);verify(t.selection().area().anchor()==5,"manual floor lock respected even over another visible layer");
        t.pointer(ray(62,62),null,5,true,false,false,true,false,false,false);verify(t.selection().area().minX()==62,"new completed drag replaces selection");
        t.pointer(ray(0,0),null,0,false,false,false,false,true,false,false);verify(t.active()&&t.selection().area()==null,"RMB clears without exiting mode");
        t.pointer(ray(0,0),null,0,false,false,false,false,false,false,false);
        t.pointer(ray(65,65),null,0,false,true,true,false,false,false,false);verify(t.selection().area()==null,"unlocked ray miss cannot pick unrelated cell");
        t.pointer(ray(65,65),null,0,false,false,false,true,false,false,false);
        t.pointer(ray(65,65),null,3,true,true,true,false,false,false,false);t.pointer(ray(65,65),null,3,true,false,false,true,false,false,false);verify(t.selection().area().anchor()==3,"explicit lock selects empty floor");
        t.pointer(ray(65,65),null,3,true,false,false,false,false,true,false);verify(!t.active()&&t.selection().area()!=null,"Esc leaves mode while retaining completed volume");
        t.enter(true);t.pointer(ray(1,1),hit(1,1,0),0,false,true,true,false,false,false,false);verify(t.selection().area().minX()==65,"UI entry fences already held click");
        t.pointer(ray(1,1),null,0,false,false,false,true,false,false,false);t.pointer(new Ray(new Vec3(1,3,1),new Vec3(1,0,0)),null,2,true,true,true,false,false,false,false);verify(!t.selection().dragging(),"parallel ray does not start invalid selection");
    }
    static void integration(){
        unit.plans.clear();var plan=new BuildPlan(30,30,0,Blocks.duo);unit.addBuild(plan);input.block=Blocks.conveyor;input.toggleConstruction();boolean building=input.isBuilding;
        var gridBefore=runtime.grid().snapshot();int copper=core.items.get(Items.copper);int jobs=runtime.pastes().pending(Vars.player);
        controls.toggleVolume();verify(controls.volumeTool().active()&&input.block==Blocks.conveyor,"mode keeps selected tool");
        verify(input.suppressBinding(Binding.select)&&input.suppressBinding(Binding.breakBlock)&&input.suppressBinding(Binding.schematicSelect)&&input.suppressBinding(Binding.clearBuilding),"mode fences native placement/removal/schematic input");
        verify(!input.suppressBinding(ConstructionBinding.toggle),"F3 remains available");
        input.setControlPick(unit);verify(!input.applyControlSelection(true,true),"selection prevents Ctrl-click possession");
        verify(!input.tap(400,300,1,KeyCode.mouseLeft)&&!input.touchDown(400,300,0,KeyCode.mouseLeft),"native direct event paths cannot place or configure while selecting");
        pointer.left=true;var cam=camera(44,44);controls.processPointer(cam,null,400,300,buttons(true,true));pointer.wheel=1;graphics.frame++;controls.processPointer(cam,null,400,300,buttons(false,true));
        verify(controls.volumeTool().selection().area().anchor()==3&&controls.volumeTool().selection().area().boundary()==4,"shared real picker anchors nearest upper structure and wheel extends Z");
        var ui=new LayerBuildController.Buttons(false,true,false,false,false,false,false,false,0,true);graphics.frame++;controls.processPointer(cam,null,400,300,ui);verify(controls.volumeTool().selection().area().boundary()==4,"wheel over UI never changes volume");pointer.wheel=0;
        pointer.left=false;controls.processPointer(cam,null,400,300,buttons(false,false));
        verify(unit.plans.size==1&&unit.plans.first()==plan&&runtime.pastes().pending(Vars.player)==jobs,"selection preserves native and upper queues");
        verify(runtime.grid().snapshot().equals(gridBefore)&&core.items.get(Items.copper)==copper&&input.isBuilding==building,"no mutation, spend or construction pause change");
        pointer.alt=true;controls.processPointer(cam,null,400,300,buttons(true,true));verify(runtime.priority().target(Vars.player)==null,"Alt-click inside selection mode is not priority command");pointer.alt=false;
        pointer.left=true;controls.toggleVolume();verify(!controls.volumeTool().active()&&input.volumePointerFenced(),"exit fences held pointer");input.setBuildMode(false);input.applyFlightTrigger(true);verify(!Vars.player.shooting,"held selection never becomes flight fire");
        input.setBuildMode(true);controls.toggleVolume();pointer.left=false;input.update();verify(!input.volumePointerFenced(),"release restores pointer");
        controls.update(cam,false,false,null);verify(!controls.volumeTool().active(),"F5/Tab inactive boundary exits selection");
        verify(input.block==Blocks.conveyor&&unit.plans.first()==plan&&input.isBuilding==building,"mode transitions retain tool, queue and F3 pause");
        controls.toggleVolume();controls.processPointer(cam,null,400,300,new LayerBuildController.Buttons(false,false,false,false,false,true,false,false,0,false));verify(!controls.volumeTool().active()&&input.suppressBinding(Binding.menu)&&input.volumePointerFenced()&&!input.applyControlSelection(true,true),"Escape consumes even a same-poll released click without possession");
        pointer.f4=true;graphics.frame++;controls.update(cam,true,false,null);pointer.f4=false;verify(controls.volumeTool().active(),"rebindable F4 toggles via actual update path");controls.endInteraction();
        // A regular mode leave keeps the last committed volume, but aborts an incomplete drag.
        controls.toggleVolume();var saved=controls.volumeTool().selection().area();pointer.left=true;controls.processPointer(cam,null,400,300,buttons(true,true));input.setSuspended(true);controls.update(cam,true,true,null);
        verify(!controls.volumeTool().active()&&!controls.volumeTool().selection().dragging()&&java.util.Objects.equals(controls.volumeTool().selection().area(),saved),"focus/modal suspension aborts draft and retains committed selection");
        input.setSuspended(false);pointer.left=false;controls.toggleVolume();input.update();controls.endInteraction();
        var q=controls.volumeTool().selection().queries();controls.processPointer(cam,null,400,300,buttons(false,false));verify(controls.volumeTool().selection().queries()==q,"inactive selection does not requery retained structures");
        unit.plans.clear();input.block=null;
    }
    static void benchmark(){
        // Valid lightweight descriptors exercise real spatial query/sort/cache without gameplay work.
        int n=0;for(int z=8;z<12;z++)for(int x=0;x<32;x++)for(int y=0;y<32;y++)add(x,y,z,1,0,.8,false);
        var m=new StructureVolume(runtime);m.refresh(0);select(m,0,0,31,31,8,11,1);verify(m.entries().size()==4096,"large volume includes every indexed structure");
        long start=System.nanoTime();for(int i=0;i<30;i++)m.refresh(201+i*201);double ms=(System.nanoTime()-start)/1e6/30;
        long q=m.queries();start=System.nanoTime();for(int i=0;i<10000;i++)m.refresh(6031);double cached=(System.nanoTime()-start)/1e6/10000;
        verify(m.queries()==q,"10000 idle frames reuse membership");System.out.printf(java.util.Locale.ROOT,"Volume benchmark: 4096 structures refresh %.3f ms, cached %.6f ms; no FPS claim.%n",ms,cached);
    }
}
