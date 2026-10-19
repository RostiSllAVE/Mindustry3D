package fpv.input;

import arc.Core;
import arc.math.geom.*;
import fpv.construction.*;
import fpv.render.LayerWorldRenderer;
import fpv.terrain.TerrainElevation;
import fpv.world.World3DAdapter;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import java.util.*;
import java.io.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Actual native builder/cost/config/structure tests; mocked host IO, not physical gameplay. */
public final class VolumeClipboardNativeTest {
    static int checks;
    static void verify(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static void reject(Runnable r,String s){try{r.run();throw new AssertionError("Accepted: "+s);}catch(IllegalArgumentException expected){verify(true,s);}}
    public static void main(String[] args)throws Exception{
        init();runtime.close();renderer.close();
        runtime=new LayerRuntime(new World3DAdapter(new TerrainElevation(),(x,z)->{var t=Vars.world.tile((int)Math.floor(x+.5),(int)Math.floor(-z+.5));return t!=null&&t.build!=null?fpv.net.HeadlessGeometry.heightFor(t.block()):0;},altitude));renderer=new LayerWorldRenderer();renderer.setRuntime(runtime);controls=new LayerBuildController(runtime,input,renderer);unit.set(32*8,32*8);
        try{codec();ground();factory();links();pointBridge();power();rotations();boundaries();cancelRange();ui();benchmark();System.out.println("VolumeClipboardNativeTest: "+checks+" checks PASS; actual native/upper construction, physical gameplay NOT TESTED.");}
        finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
    static VolumeClipboard.Cell cell(int x,int y,int z,mindustry.world.Block block,int rotation){return new VolumeClipboard.Cell(x,y,z,block.name,block.size,rotation,false,LayerConfiguration.encode(null));}
    static VolumeClipboard.Clip clip(List<VolumeClipboard.Cell> cells,List<VolumeClipboard.Link> links,int w,int d,int h){return new VolumeClipboard.Clip(1.4,w,d,h,cells,links);}
    static VolumeClipboard.Clip one(mindustry.world.Block b){return clip(List.of(cell(0,0,0,b,0)),List.of(),b.size,b.size,1);}
    static LayerActions.Action action(VolumeClipboard.Clip c,int x,int y,int z,boolean free){return LayerActions.Action.volume(new VolumeClipboard.Placement(c,x,y,z),free?LayerComponent.Mode.FREE:LayerComponent.Mode.STRUCTURAL);}
    static StructureVolume select(int ax,int ay,int bx,int by,int low,int high){
        for(var team:Team.all)team.data().buildingTree=new QuadTree<>(new Rect(0,0,1024,1024));for(var t:Vars.world.tiles)if(t.build!=null&&t.build.tile==t) t.build.team.data().buildingTree.insert(t.build);
        var m=new StructureVolume(runtime);m.refresh(0);m.begin(ax,ay,low);m.drag(ax,ay,bx,by);m.height(high);m.finish();m.refresh(1);return m;
    }
    static long put(int x,int y,int z,mindustry.world.Block b,boolean platform,boolean free){var r=runtime.apply(Vars.player,LayerActions.Action.place(x,y,z,b.name,platform,0,free?LayerComponent.Mode.FREE:LayerComponent.Mode.STRUCTURAL));verify(r.accepted(),"source setup: "+b.name+" Z"+z+" "+r.reason());return r.componentId();}
    static void run(int limit){for(int i=0;i<limit&&runtime.pastes().pending(Vars.player)>0;i++)tick();}
    static void codec()throws Exception{
        var original=clip(List.of(cell(0,0,0,Blocks.container,0),cell(3,0,2,Blocks.conveyor,1)),List.of(),4,2,3);
        var bytes=VolumeClipboard.encode(original);var decoded=VolumeClipboard.decode(bytes);verify(Arrays.equals(bytes,VolumeClipboard.encode(decoded)),"versioned roundtrip preserves all cells and floors");
        var rotated=decoded;for(int angle=1;angle<=4;angle++){rotated=VolumeClipboard.rotate(rotated);verify(rotated.cells().get(0).size()==2&&rotated.cells().get(1).z()==2,"even footprint and XYZ preserved at "+angle*90);verify(rotated.cells().get(1).rotation()==(1+angle)%4,"direction rotated at "+angle*90);}
        verify(Arrays.equals(VolumeClipboard.encode(rotated),bytes),"four rotations restore exact clipboard including even anchor alignment");
        var a=action(decoded,12,15,1,true);var out=new ByteArrayOutputStream();LayerActions.write(new DataOutputStream(out),a);verify(a.equals(LayerActions.read(new DataInputStream(new ByteArrayInputStream(out.toByteArray())))),"existing construction envelope carries bounded 3D clipboard");
        reject(()->VolumeClipboard.decode(Arrays.copyOf(bytes,bytes.length-1)),"truncation rejected");var trailing=Arrays.copyOf(bytes,bytes.length+1);reject(()->VolumeClipboard.decode(trailing),"trailing bytes rejected");reject(()->VolumeClipboard.decode(new byte[VolumeClipboard.MAX_BYTES+1]),"oversize binary rejected");var version=bytes.clone();version[4]=99;reject(()->VolumeClipboard.decode(version),"unknown version rejected");
        reject(()->clip(List.of(cell(0,0,0,Blocks.conveyor,0),cell(0,0,0,Blocks.conveyor,0)),List.of(),1,1,1),"overlap rejected before world changes");
        reject(()->clip(List.of(cell(0,0,0,Blocks.conveyor,0)),List.of(new VolumeClipboard.Link(0,99,VolumeClipboard.LinkKind.POWER)),1,1,1),"forged endpoint index rejected");
    }
    static void ground(){
        Vars.world.tile(20,20).setBlock(Blocks.sorter,Team.sharded,0);var source=Vars.world.build(20,20);mindustry.gen.Call.tileConfig(Vars.player,source,Items.lead);
        var copy=VolumeClipboard.capture(runtime,select(20,20,20,20,0,0),Vars.player);verify(copy.clip().cells().size()==1&&LayerConfiguration.decode(copy.clip().cells().get(0).config())==Items.lead,"Z0 native selection captures supported sorter configuration");
        int copper=core.items.get(Items.copper);var a=action(copy.clip(),22,20,0,false);verify(runtime.apply(Vars.player,a).accepted(),"Z0 paste admitted");verify(Vars.world.tile(22,20).block()==Blocks.air&&core.items.get(Items.copper)==copper,"confirmation queues unpaid native plan; no instant structure");
        for(int i=0;i<5;i++)tick();verify(Vars.world.build(22,20) instanceof ConstructBuild,"actual native partially completed blueprint");input.toggleConstruction();float before=((ConstructBuild)Vars.world.build(22,20)).progress;for(int i=0;i<20;i++)tick();verify(((ConstructBuild)Vars.world.build(22,20)).progress==before,"F3 stops without losing partial Z0 work");input.toggleConstruction();altitude.setExternalStateProvider(u->u==unit?new fpv.world.Unit3DState(u.id,u.type.id,u.team.id,u.x/8.,u.y/8.,100,0,0,0,0,0,0,100,.2,.2,true):null);for(int i=0;i<20;i++)tick();verify(((ConstructBuild)Vars.world.build(22,20)).progress==before,"native volume builder cannot work from excessive Z despite valid XY");altitude.setExternalStateProvider(null);unit.set(120*8,120*8);for(int i=0;i<20;i++)tick();verify(((ConstructBuild)Vars.world.build(22,20)).progress==before,"owned native volume plans respect build range during execution");unit.set(32*8,32*8);run(3000);
        verify(Vars.world.tile(22,20).block()==Blocks.sorter&&Vars.world.build(22,20).config()==Items.lead,"native building completes with original config");verify(core.items.get(Items.copper)==copper-Blocks.sorter.requirements[0].amount,"native recipe charged exactly once");runtime.pastes().clear();
    }
    static void factory(){
        for(int x=24;x<=25;x++)for(int y=24;y<=25;y++)Vars.world.tile(x,y).setBlock(Blocks.titaniumWall,Team.sharded,0);
        for(int x=24;x<=25;x++)for(int y=24;y<=25;y++){put(x,y,1,Blocks.air,true,false);put(x,y,1,Blocks.titaniumWall,false,false);put(x,y,2,Blocks.air,true,false);}
        long press=put(24,24,2,Blocks.graphitePress,false,false);runtime.building(press).items.add(Items.graphite,77);runtime.building(press).health-=20;
        var copied=VolumeClipboard.capture(runtime,select(24,24,25,25,0,2),Vars.player).clip();verify(copied.cells().size()==17&&copied.floors()==3,"whole three-floor factory includes native walls, columns, slabs and 2x2 machine");
        var pos=action(copied,40,40,0,false);var dry=runtime.pastes().previewVolume(Vars.player,pos,false);verify(dry.accepted(),"combined future foundations and structural order valid: "+dry.reason());
        var amounts=new HashMap<mindustry.type.Item,Integer>();for(var c:copied.cells()){var e=runtime.catalog().entry(c.name(),c.platform(),1.4);for(var req:e.requirements())amounts.merge(req.item,req.amount,Integer::sum);}
        var before=new HashMap<mindustry.type.Item,Integer>();for(var item:amounts.keySet())before.put(item,core.items.get(item));
        verify(runtime.apply(Vars.player,pos).accepted(),"atomic three-floor queue accepted");verify(runtime.grid().at(40,40,1,LayerComponent.Kind.BLOCK)==null&&Vars.world.tile(40,40).block()==Blocks.air,"all structures initially remain plans");
        for(int i=0;i<5000&&Vars.world.tile(41,41).block()!=Blocks.titaniumWall;i++)tick();verify(Vars.world.tile(40,40).block()==Blocks.titaniumWall,"native supporting ground built before upper dependencies");
        var target=new ConstructionPriority.Target(unit.id,41,41,1,false,Blocks.titaniumWall,0);verify(runtime.requestPriority(target).accepted(),"ready upper blueprint can take A2-08b priority");int pending=runtime.pastes().pending(Vars.player);input.toggleConstruction();for(int i=0;i<20;i++)tick();verify(runtime.pastes().pending(Vars.player)==pending,"F3 preserves volumetric job and priority");input.toggleConstruction();run(20000);
        verify(runtime.pastes().pending(Vars.player)==0,"complete support-first batch drains existing queue: "+runtime.status());var out=runtime.grid().at(40,40,2,LayerComponent.Kind.BLOCK);verify(out!=null&&out.blockName().equals("graphite-press"),"factory reaches corresponding Z2, not flattened");verify(runtime.building(out.id()).items.total()==0&&runtime.building(out.id()).health==Blocks.graphitePress.health,"no source inventories / damaged health duplicated");
        for(var item:amounts.keySet())verify(core.items.get(item)==before.get(item)-amounts.get(item),"exact once batch cost: "+item.name);verify(runtime.solver().support(out.id()).supported(),"finished structure retains actual live support");
        var partial=VolumeClipboard.capture(runtime,select(41,41,41,41,2,2),Vars.player).clip();verify(partial.cells().stream().anyMatch(c->c.name().equals("graphite-press")&&c.size()==2)&&partial.width()==2,"partial selection preserves whole multiblock outside rectangle");runtime.pastes().clear();
    }
    static void links(){
        long a=put(15,35,1,Blocks.itemBridge,false,true),b=put(15,35,3,Blocks.itemBridge,false,true);verify(runtime.apply(Vars.player,LayerActions.Action.link(a,b,LayerVerticalTransfers.Kind.BRIDGE_ITEMS,false)).accepted(),"source real cross-floor bridge installed");
        var copied=VolumeClipboard.capture(runtime,select(15,35,15,35,1,3),Vars.player).clip();verify(copied.links().size()==1&&copied.floors()==3,"supported internal XYZ bridge captured by indices");var rotated=VolumeClipboard.rotate(copied);verify(rotated.links().equals(copied.links()),"rotation keeps internal index endpoints");
        verify(runtime.apply(Vars.player,action(rotated,45,22,2,true)).accepted(),"shifted Z2/Z4 bridge queued");run(10000);var ca=runtime.grid().at(45,22,2,LayerComponent.Kind.BLOCK);var cb=runtime.grid().at(45,22,4,LayerComponent.Kind.BLOCK);verify(ca!=null&&cb!=null,"destination Z offset preserved on both endpoints");verify(runtime.transfers().links().contains(new LayerVerticalTransfers.Link(ca.id(),cb.id(),LayerVerticalTransfers.Kind.BRIDGE_ITEMS)),"new link references copied IDs only");verify(runtime.transfers().links().contains(new LayerVerticalTransfers.Link(a,b,LayerVerticalTransfers.Kind.BRIDGE_ITEMS)),"source link retained");runtime.pastes().clear();
    }
    static void pointBridge(){
        long a=put(18,30,1,Blocks.itemBridge,false,true),b=put(20,30,1,Blocks.itemBridge,false,true);verify(runtime.apply(Vars.player,LayerActions.Action.configure(a,new Point2(2,0))).accepted(),"same-floor native bridge source configured");
        var copied=VolumeClipboard.rotate(VolumeClipboard.capture(runtime,select(18,30,20,30,1,1),Vars.player).clip());verify(copied.links().stream().anyMatch(l->l.kind()==VolumeClipboard.LinkKind.POINT),"native internal Point2 converted to clipboard indices");
        verify(runtime.apply(Vars.player,action(copied,50,34,3,true)).accepted(),"rotated point bridge queued");run(8000);var node=runtime.grid().at(50,34,3,LayerComponent.Kind.BLOCK);verify(node!=null&&((mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild)runtime.building(node.id())).link==Point2.pack(50,36),"point endpoint rotates and shifts to copied destination, never original coordinates");runtime.pastes().clear();
    }
    static void power(){
        long a=put(18,36,1,Blocks.powerNode,false,true),b=put(20,36,1,Blocks.powerNode,false,true);verify(runtime.apply(Vars.player,LayerActions.Action.link(a,b,LayerVerticalTransfers.Kind.POWER,false)).accepted(),"source power link valid");
        var copied=VolumeClipboard.capture(runtime,select(18,36,20,36,1,1),Vars.player).clip();verify(copied.links().stream().anyMatch(l->l.kind()==VolumeClipboard.LinkKind.POWER),"internal node links captured");copied=VolumeClipboard.rotate(copied);
        verify(runtime.apply(Vars.player,action(copied,47,39,2,true)).accepted(),"rotated power pair queued on different floor");run(8000);var na=runtime.grid().at(47,39,2,LayerComponent.Kind.BLOCK);var nb=runtime.grid().at(47,41,2,LayerComponent.Kind.BLOCK);verify(na!=null&&nb!=null&&runtime.nativeBuildings().linkedPower(na.id(),nb.id()),"rotated power endpoints connect new IDs only");runtime.pastes().clear();
        unit.set(40*8,24*8);long pa=put(55,18,1,Blocks.powerNode,false,true),pb=put(57,18,3,Blocks.powerNode,false,true);verify(runtime.apply(Vars.player,LayerActions.Action.link(pa,pb,LayerVerticalTransfers.Kind.POWER,false)).accepted(),"explicit cross-floor source power linked");
        var cross=VolumeClipboard.capture(runtime,select(55,18,57,18,1,3),Vars.player).clip();verify(runtime.apply(Vars.player,action(cross,55,24,2,true)).accepted(),"cross-floor power copy queued");run(8000);var x=runtime.grid().at(55,24,2,LayerComponent.Kind.BLOCK);var y=runtime.grid().at(57,24,4,LayerComponent.Kind.BLOCK);verify(x!=null&&y!=null&&runtime.transfers().links().contains(new LayerVerticalTransfers.Link(x.id(),y.id(),LayerVerticalTransfers.Kind.POWER)),"cross-floor copied power uses new authoritative endpoints");unit.set(32*8,32*8);runtime.pastes().clear();
    }
    static void rotations(){
        var shape=clip(List.of(cell(0,0,0,Blocks.container,0),cell(3,0,2,Blocks.conveyor,1)),List.of(),4,2,3);
        int[][] container={{0,0},{2,0},{0,2}},belt={{1,3},{0,1},{0,0}};
        for(int i=0;i<3;i++){shape=VolumeClipboard.rotate(shape);int x=10+i*10,y=45,z=2;
            verify(runtime.apply(Vars.player,action(shape,x,y,z,true)).accepted(),"actual "+(i+1)*90+" degree paste accepted");run(8000);
            var c=runtime.grid().at(x+container[i][0],y+container[i][1],z,LayerComponent.Kind.BLOCK);var b=runtime.grid().at(x+belt[i][0],y+belt[i][1],z+2,LayerComponent.Kind.BLOCK);
            verify(c!=null&&c.blockName().equals("container")&&b!=null&&b.blockName().equals("conveyor")&&b.rotation()==(2+i)%4,"actual rotated even-size footprint, direction and floor offset");runtime.pastes().clear();
        }
    }
    static void benchmark(){
        var cells=new ArrayList<VolumeClipboard.Cell>();for(int x=0;x<16;x++)for(int y=0;y<16;y++)cells.add(cell(x,y,0,Blocks.conveyor,0));var c=clip(cells,List.of(),16,16,1);var a=action(c,70,70,3,true);
        Vars.state.rules.infiniteResources=true;long start=System.nanoTime();for(int i=0;i<3;i++)verify(runtime.pastes().previewVolume(Vars.player,a,false).accepted(),"large preview remains valid");double ms=(System.nanoTime()-start)/1e6/3;
        System.out.printf(java.util.Locale.ROOT,"Volume clipboard benchmark: 256 entries, %d encoded bytes, mean preview %.3f ms (3 samples); no FPS claim.%n",VolumeClipboard.encode(c).length,ms);
    }
    static void cancelRange(){
        runtime.pastes().clear();var unrelated=new mindustry.entities.units.BuildPlan(60,60,0,Blocks.conveyor);unit.plans.addLast(unrelated);verify(runtime.apply(Vars.player,action(one(Blocks.copperWall),50,20,0,false)).accepted(),"range-cancel batch queued without changing other work");
        unit.set(120*8,120*8);runtime.priority().beforeNative();runtime.apply(Vars.player,LayerActions.Action.cancelPaste());runtime.priority().afterNative();verify(runtime.pastes().pending(Vars.player)==0&&unit.plans.size==1&&unit.plans.first()==unrelated,"cancel while XYZ range-parked cannot resurrect owned plans or remove unrelated work");unit.set(32*8,32*8);unit.plans.clear();runtime.pastes().clear();
    }
    static class Mouse extends arc.mock.MockInput {
        boolean left,right,rotate,copy,paste,ctrl,shift;float wheel;
        @Override public boolean keyDown(arc.input.KeyBind k){return k==mindustry.input.Binding.select?left:k==mindustry.input.Binding.deselect||k==mindustry.input.Binding.breakBlock?right:false;}
        @Override public boolean keyDown(arc.input.KeyCode k){return k==arc.input.KeyCode.controlLeft?ctrl:k==arc.input.KeyCode.shiftLeft?shift:false;}
        @Override public boolean keyTap(arc.input.KeyBind k){return k==VolumePasteTool.rotate?rotate:k==VolumePasteTool.copy?copy:k==VolumePasteTool.paste?paste:false;}
        @Override public float axis(arc.input.KeyCode k){return k==arc.input.KeyCode.scroll?wheel:0;}
        @Override public int mouseX(){return 400;}@Override public int mouseY(){return 300;}
    }
    static void ui(){
        runtime.pastes().clear();input.uninstallProjection();var mouse=new Mouse();Core.input=mouse;input=new FpvDesktopInput(mouse,fpv.generated.InputProfiles.create());input.installProjection();input.setFpvEnabled(true);input.setBuildMode(true);input.setFlightAim(128,96);controls=new LayerBuildController(runtime,input,renderer);input.block=Blocks.conveyor;controls.setLayer(1);controls.setFree(true);
        var m=controls.volumeTool().selection();m.refresh(0);m.begin(20,20,0);m.finish();m.refresh(1);controls.copyVolume();verify(controls.volumePasteTool().active(),"copy button enters dedicated preview with retained source selection");var camera=camera(50,43);
        controls.processPointer(camera,null,400,300,buttons(false,false));verify(controls.volumePasteTool().preview().accepted()&&controls.volumePasteTool().preview().plans().get(0).floor==1,"locked-floor multi-layer preview resolves actual destination");
        mouse.wheel=1;graphics.frame++;controls.processPointer(camera,null,400,300,buttons(false,false));verify(controls.volumePasteTool().preview().plans().get(0).floor==2,"wheel moves destination Z");
        graphics.frame++;controls.processPointer(camera,null,400,300,new LayerBuildController.Buttons(false,false,false,false,false,false,false,false,0,true));verify(controls.volumePasteTool().preview().plans().get(0).floor==2,"UI wheel does not move paste");mouse.wheel=0;
        var away=camera(50,43);away.direction.set(0,1,0);away.up.set(0,0,1);away.update();int q=runtime.pastes().pending(Vars.player);controls.processPointer(away,null,400,300,buttons(true,true));verify(runtime.pastes().pending(Vars.player)==q&&controls.volumePasteTool().preview()==null,"ray miss cannot confirm a stale last destination");controls.processPointer(camera,null,400,300,buttons(false,false));mouse.rotate=true;graphics.frame++;controls.processPointer(camera,null,400,300,buttons(false,false));mouse.rotate=false;verify(controls.volumePasteTool().status().contains("90 deg"),"R rotates production preview");
        int before=runtime.pastes().pending(Vars.player);mouse.left=true;controls.processPointer(camera,null,400,300,buttons(true,true));controls.processPointer(camera,null,400,300,buttons(true,true));verify(runtime.pastes().pending(Vars.player)==before+1&&!controls.volumePasteTool().active(),"one click confirms exactly one batch; held click cannot duplicate");verify(input.block==Blocks.conveyor,"existing native tool retained");runtime.apply(Vars.player,LayerActions.Action.cancelPaste());mouse.left=false;controls.pasteVolume();verify(controls.volumePasteTool().active(),"session clipboard reusable");
        controls.processPointer(camera,null,400,300,new LayerBuildController.Buttons(false,false,true,true,true,false,false,false,0,false));verify(!controls.volumePasteTool().active()&&controls.volumePasteTool().available()&&controls.volumeTool().active(),"RMB cancels preview only; no queued construction or clipboard loss");
        mouse.ctrl=mouse.shift=mouse.paste=true;graphics.frame++;controls.update(camera,true,false,null);mouse.paste=false;verify(controls.volumePasteTool().active(),"Ctrl+Shift+V enters preview through registered input path");
        controls.update(camera,false,false,null);verify(!controls.volumePasteTool().active()&&controls.volumePasteTool().available(),"Tab/F5 boundary cancels preview but keeps session clipboard");controls.reset();verify(!controls.volumePasteTool().available(),"world reset clears transient clipboard");
    }
    static void boundaries(){
        var single=one(Blocks.copperWall);var before=runtime.grid().snapshot();int copper=core.items.get(Items.copper);
        verify(!runtime.apply(Vars.player,action(single,40,40,0,false)).accepted(),"occupied footprint rejects entire batch");verify(runtime.grid().snapshot().equals(before)&&core.items.get(Items.copper)==copper,"rejection changes neither world nor resources");runtime.pastes().clear();
        verify(!runtime.apply(Vars.player,action(single,50,30,4,false)).accepted(),"unsupported destination rejected atomically");runtime.pastes().clear();Core.settings.put(LayerRuntime.FREE_PERMISSION,false);verify(!runtime.apply(Vars.player,action(single,50,30,4,true)).accepted(),"client Free request cannot bypass host");Core.settings.put(LayerRuntime.FREE_PERMISSION,true);runtime.pastes().clear();
        core.items.clear();var r=runtime.apply(Vars.player,action(single,50,30,4,true));verify(r.accepted(),"missing resources produce ordinary unpaid waiting plan");for(int i=0;i<40;i++)tick();verify(runtime.grid().at(50,30,4,LayerComponent.Kind.BLOCK)==null,"no free completion without recipe");core.items.set(Items.copper,100);run(4000);verify(runtime.grid().at(50,30,4,LayerComponent.Kind.BLOCK)!=null&&core.items.get(Items.copper)==100-Blocks.copperWall.requirements[0].amount,"resupply resumes at normal cost");runtime.pastes().clear();
        var empty=select(90,90,92,92,0,3);reject(()->VolumeClipboard.capture(runtime,empty,Vars.player),"empty selection rejected");
        var large=new ArrayList<VolumeClipboard.Cell>();for(int x=0;x<33;x++)for(int y=0;y<33;y++)large.add(cell(x,y,0,Blocks.conveyor,0));reject(()->clip(large,List.of(),33,33,1),"very large clipboard bounded");
        Vars.world.tile(21,20).setBlock(Blocks.sorter,Team.crux,0);reject(()->VolumeClipboard.capture(runtime,select(21,20,21,20,0,0),Vars.player),"enemy structure cannot be copied");
        for(var item:Vars.content.items())core.items.set(item,10000);runtime.pastes().clear();var queued=action(single,48,32,5,true);verify(runtime.apply(Vars.player,queued).accepted(),"cancel fixture queued");verify(!runtime.apply(Vars.player,queued).accepted()&&runtime.pastes().pending(Vars.player)==1,"duplicate pending paste rejected without duplicated jobs");runtime.apply(Vars.player,LayerActions.Action.cancelPaste());verify(runtime.pastes().pending(Vars.player)==0,"explicit cancellation clears only pending job; no resource operation");
    }
}
