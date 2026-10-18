package fpv.input;

import arc.*;
import arc.input.*;
import arc.mock.*;
import arc.scene.Scene;
import arc.struct.*;
import arc.util.*;
import fpv.construction.*;
import fpv.generated.InputProfiles;
import fpv.terrain.TerrainElevation;
import fpv.units.UnitAltitudeSystem;
import fpv.world.World3DAdapter;
import java.util.HashSet;
import java.util.Set;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.input.Binding;
import mindustry.net.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

/** Native builder/progress/cost and actual native snapshot fixtures; no physical UI/socket test. */
public final class ConstructionPauseNativeTest {
    private static int checks;
    private static class Graphics extends MockGraphics {long frame;@Override public long getFrameId(){return frame;}@Override public int getWidth(){return 800;}@Override public int getHeight(){return 600;}}
    private static class Keys extends MockInput {final Set<KeyBind> taps=new HashSet<>();@Override public boolean keyTap(KeyBind key){return taps.contains(key);}}
    private static class Modal extends Scene {boolean typing,dialog;@Override public boolean hasKeyboard(){return typing;}@Override public boolean hasDialog(){return dialog;}}
    private static class Connection extends NetConnection {Connection(){super("in-memory-construction");}public void send(Object o,boolean reliable){}public void close(){}}
    private static Graphics graphics;
    private static Keys keys;
    private static Modal scene;
    private static FpvDesktopInput input;
    private static Unit unit;
    private static CoreBuild core;
    private static UnitAltitudeSystem altitudes;
    private static LayerRuntime layers;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    public static void main(String[] args){
        init();
        try{
            nativeBlueprint();inputLifecycle();upperQueue();nativeSnapshots();commandAndWorld();clearIsDistinct();
            System.out.println("ConstructionPauseNativeTest: "+checks+" checks PASS; actual native progress/cost/snapshot and upper work fixtures; physical gameplay NOT TESTED.");
        }finally{input.uninstallProjection();layers.close();altitudes.close();Core.scene=null;}
    }
    private static void init(){
        Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=graphics=new Graphics();Core.input=keys=new Keys();Core.gl=new MockGL20();
        Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.fogControl=new mindustry.game.FogControl();Vars.net=new Net(null);Groups.init();Vars.world=new World();Vars.world.tiles=new Tiles(64,64);Vars.world.setGenerating(true);Vars.world.tiles.fill();Vars.indexer=new mindustry.ai.BlockIndexer();Vars.pathfinder=new mindustry.ai.Pathfinder(){@Override public void updateTile(Tile tile){}};Groups.resize(0,0,512,512);Time.setDeltaProvider(()->1);Vars.content.init();Vars.state.set(GameState.State.playing);
        Vars.state.rules.editor=true;Vars.state.rules.instantBuild=false;Vars.state.rules.infiniteResources=false;
        Vars.world.tile(8,8).setBlock(Blocks.coreShard,Team.sharded,0);core=(CoreBuild)Vars.world.tile(8,8).build;
        if(!Team.sharded.data().cores.contains(core))Team.sharded.data().cores.add(core); // generating fixture skips registration
        for(var item:Vars.content.items())core.items.set(item,1000);
        Vars.player=Player.create();Vars.player.team(Team.sharded);unit=builder(Vars.player,12,12);
        Core.scene=scene=new Modal();input=new FpvDesktopInput(keys,InputProfiles.create());input.installProjection();input.setFpvEnabled(true);input.setFlightAim(16*8,12*8);
        altitudes=new UnitAltitudeSystem((x,y)->0);layers=new LayerRuntime(new World3DAdapter(new TerrainElevation(),(x,y)->0,altitudes));Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
        ConstructionBinding.toggle.load();
    }
    private static Unit builder(Player player,int x,int y){Unit u=UnitTypes.alpha.create(player.team());u.set(x*8,y*8);u.add();player.unit(u);return u;}
    private static void buildTicks(Unit u,int ticks){for(int i=0;i<ticks;i++){Time.delta=1;Time.update();Vars.state.updateId++;u.updateBuildLogic();}}
    private static void tap(KeyBind key){graphics.frame++;keys.taps.add(key);input.pollConstructionInput();keys.taps.clear();}
    private static void nativeBlueprint(){
        BuildPlan plan=new BuildPlan(16,12,0,Blocks.duo);unit.plans.addLast(plan);unit.updateBuilding(true);input.isBuilding=true;buildTicks(unit,8);
        check(Vars.world.tile(16,12).build instanceof ConstructBuild,"real Z0 construction begins a native partial blueprint: core="+unit.core()+", canBuild="+unit.canBuild()+", buildSpeed="+unit.type.buildSpeed+", tile="+Vars.world.tile(16,12).block()+", queued="+unit.plans.size+", place="+mindustry.world.Build.validPlace(Blocks.duo,Team.sharded,16,12,0));
        var partial=(ConstructBuild)Vars.world.tile(16,12).build;float progress=partial.progress;int copper=core.items.get(Items.copper);
        check(progress>0&&progress<1&&unit.plans.contains(plan),"native builder has real unfinished work and original queue entry");
        input.block=Blocks.conveyor;unit.mineTile=Vars.world.tile(11,12);Vars.player.shooting=true;
        tap(ConstructionBinding.toggle);check(!unit.updateBuilding()&&!input.isBuilding,"F3 immediately sets actual native updateBuilding false in flight");
        check(input.block==Blocks.conveyor&&unit.mineTile!=null&&Vars.player.shooting,"pause changes no selected tool, mining or firing state");
        buildTicks(unit,500);check(Vars.world.tile(16,12).build==partial&&partial.progress==progress&&core.items.get(Items.copper)==copper,"paused native blueprint retains exact progress/resources/identity");
        check(unit.plans.size==1&&unit.plans.first()==plan,"stop retains the committed queue, including its original plan identity");
        input.cancelPointerGesture();input.block=null;check(unit.plans.first()==plan&&!unit.updateBuilding(),"tool/gesture cancellation neither clears queue nor resumes work");
        var next=new BuildPlan(19,12,0,Blocks.conveyor);unit.plans.addLast(next);buildTicks(unit,60);
        check(partial.progress==progress&&unit.plans.contains(next),"another block can be queued while construction is stopped; no priority change");
        tap(ConstructionBinding.toggle);check(unit.updateBuilding()&&input.isBuilding,"F3 resumes native construction");
        buildTicks(unit,1500);check(Vars.world.tile(16,12).block()==Blocks.duo&&Vars.world.tile(19,12).block()==Blocks.conveyor&&unit.plans.isEmpty(),"same blueprint and additional normal plan complete after resume");
        check(core.items.get(Items.copper)==1000-Blocks.duo.requirements[0].amount-Blocks.conveyor.requirements[0].amount,"native resource recipes are charged once, with no pause/resume refund or duplication");
        unit.mineTile=null;Vars.player.shooting=false;
    }
    private static void inputLifecycle(){
        unit.updateBuilding(true);input.isBuilding=true;tap(ConstructionBinding.toggle);check(!unit.updateBuilding(),"paused before Tab transition");
        input.setBuildMode(true);input.setUpperLayerOwned(true);input.update();check(!unit.updateBuilding(),"Build Mode early-return path preserves stopped work");
        graphics.frame++;keys.taps.add(Binding.pauseBuilding);input.update();input.update();keys.taps.clear();
        check(unit.updateBuilding()&&input.isBuilding,"native Build pause binding toggles once, even if input is polled twice in one frame");
        input.setBuildMode(false);tap(Binding.pauseBuilding);check(unit.updateBuilding(),"unmodified E in flight remains an altitude input, not construction toggle");
        input.setBuildMode(true);tap(ConstructionBinding.toggle);input.setBuildMode(false);check(!unit.updateBuilding(),"Tab back to flight cannot silently resume construction");
        boolean paused=unit.updateBuilding();input.setSuspended(true);tap(ConstructionBinding.toggle);check(unit.updateBuilding()==paused,"focus/input suspension ignores pause shortcut");input.setSuspended(false);
        scene.typing=true;tap(ConstructionBinding.toggle);check(unit.updateBuilding()==paused,"typing cannot toggle construction");scene.typing=false;scene.dialog=true;tap(ConstructionBinding.toggle);check(unit.updateBuilding()==paused,"menus/dialogs cannot toggle construction");scene.dialog=false;
        var old=ConstructionBinding.toggle.value;ConstructionBinding.toggle.value=new KeyBind.Axis(KeyCode.f4);ConstructionBinding.toggle.save();ConstructionBinding.toggle.load();check(ConstructionBinding.toggle.value.key==KeyCode.f4&&input.constructionLabel().contains("F4"),"host Controls remapping survives native settings reload and updates the label");ConstructionBinding.toggle.value=old;ConstructionBinding.toggle.save();
        Unit previous=unit;Player other=Player.create();other.team(Team.sharded);Unit untouched=builder(other,12,14);var sentinel=new BuildPlan(18,14,0,Blocks.conveyor);untouched.plans.addLast(sentinel);
        unit=builder(Vars.player,12,13);input.pollConstructionInput();check(unit.updateBuilding()&&input.isBuilding&&!previous.updateBuilding(),"unit switch reads new unit's state and leaves old stopped unit alone");
        tap(ConstructionBinding.toggle);check(!unit.updateBuilding()&&untouched.updateBuilding()&&untouched.plans.first()==sentinel,"pause after possession affects only the controlled unit, not another player queue");
        Vars.player.unit(previous);unit=previous;input.pollConstructionInput();check(!input.isBuilding,"repossessing previous unit retains its stopped state");tap(ConstructionBinding.toggle);
        Core.settings.put("buildautopause",true);input.setBuildMode(true);input.setUpperLayerOwned(true);input.isBuilding=false;input.buildWasAutoPaused=true;input.update();
        check(input.isBuilding&&unit.updateBuilding()&&!input.buildWasAutoPaused,"explicit resume survives native empty-queue auto-pause state");
        input.setBuildMode(false);Core.settings.put("buildautopause",false);
    }
    private static mindustry.game.Schematic schematic(){return new mindustry.game.Schematic(Seq.with(new mindustry.game.Schematic.Stile(Blocks.copperWall,0,0,null,(byte)0)),new StringMap(),1,1);}
    private static void upperQueue(){
        unit.updateBuilding(true);input.isBuilding=true;
        var action=LayerActions.Action.paste(new LayerSchematic.Stamp(schematic(),18,18),2,LayerComponent.Mode.FREE);
        int copper=core.items.get(Items.copper);check(layers.apply(Vars.player,action).accepted(),"real server-owned timed Z2 job queued");layers.update(.1);
        tap(ConstructionBinding.toggle);for(int i=0;i<100;i++)layers.update(.1);
        check(layers.pastes().pending(Vars.player)==1&&layers.grid().at(18,18,2,LayerComponent.Kind.BLOCK)==null&&core.items.get(Items.copper)==copper,"upper pause retains pending work and charges no resources");
        tap(ConstructionBinding.toggle);layers.update(.001);check(layers.pastes().pending(Vars.player)==1,"paused seconds do not accumulate a completion burst");
        Unit original=unit;unit=builder(Vars.player,12,15);input.pollConstructionInput();for(int i=0;i<100;i++)layers.update(.1);
        check(layers.pastes().pending(Vars.player)==1&&layers.grid().at(18,18,2,LayerComponent.Kind.BLOCK)==null,"possession switch suspends original upper job instead of deleting or completing it");
        Vars.player.unit(original);unit=original;input.pollConstructionInput();for(int i=0;i<1000&&layers.pastes().pending(Vars.player)>0;i++)layers.update(.1);
        check(layers.grid().at(18,18,2,LayerComponent.Kind.BLOCK)!=null&&layers.pastes().pending(Vars.player)==0,"same original upper job completes after repossession/resume");
        check(core.items.get(Items.copper)==copper-Blocks.copperWall.requirements[0].amount,"upper recipe charged exactly once");
        Player other=Player.create();other.team(Team.sharded);Unit otherUnit=builder(other,15,15);var otherAction=LayerActions.Action.paste(new LayerSchematic.Stamp(schematic(),21,18),3,LayerComponent.Mode.FREE);
        check(layers.apply(other,otherAction).accepted(),"second player's independent timed job queued");check(layers.apply(Vars.player,LayerActions.Action.paste(new LayerSchematic.Stamp(schematic(),19,18),3,LayerComponent.Mode.FREE)).accepted(),"own second upper job queued");tap(ConstructionBinding.toggle);
        for(int i=0;i<1000&&layers.pastes().pending(other)>0;i++)layers.update(.1);
        check(layers.pastes().pending(other)==0&&layers.pastes().pending(Vars.player)==1&&otherUnit.updateBuilding(),"one paused player cannot stop or cancel another player's work");
        int before=core.items.get(Items.copper);check(layers.apply(Vars.player,LayerActions.Action.cancelPaste()).accepted()&&layers.pastes().pending(Vars.player)==0&&core.items.get(Items.copper)==before,"explicit clear queue remains distinct and creates no refund");
        tap(ConstructionBinding.toggle);
    }
    private static void nativeSnapshots(){
        int before=core.items.get(Items.copper);unit.plans.clear();var plan=new BuildPlan(22,12,0,Blocks.duo);unit.plans.addLast(plan);unit.updateBuilding(true);buildTicks(unit,8);
        var partial=(ConstructBuild)Vars.world.tile(22,12).build;float progress=partial.progress;int copper=core.items.get(Items.copper);
        Vars.netServer=new NetServer();Vars.player.con=new Connection();
        snapshot(10,false);buildTicks(unit,300);check(!unit.updateBuilding()&&partial.progress==progress&&core.items.get(Items.copper)==copper&&unit.plans.size==1,"actual host clientSnapshot transfers pause while retaining partial blueprint and submitted plans");
        snapshot(9,true);check(!unit.updateBuilding(),"stale native snapshot cannot undo a newer pause");snapshot(11,false);check(!unit.updateBuilding(),"repeated paused snapshot is idempotent");snapshot(12,true);buildTicks(unit,1500);
        check(Vars.world.tile(22,12).block()==Blocks.duo&&core.items.get(Items.copper)==before-Blocks.duo.requirements[0].amount,"actual host snapshot resumes the same native blueprint without duplicated costs");
    }
    private static void snapshot(int sequence,boolean building){
        Queue<BuildPlan> copy=new Queue<>();for(var p:unit.plans)copy.addLast(p.copy());
        NetServer.clientSnapshot(Vars.player,sequence,unit.id,false,unit.x,unit.y,unit.x,unit.y,unit.rotation,0,0,0,null,false,false,false,building,null,0,copy,0,0,512,512);
    }
    private static void commandAndWorld(){
        var commands=new FpvUnitCommands(input,null,null,null,layers);input.setCommands(commands);
        commands.modeInput(true,false,false,false,false,true,false,false,false,false,false);
        check(input.commandActive(),"actual command-mode adapter entered");
        unit.updateBuilding(true);input.isBuilding=true;graphics.frame++;keys.taps.add(ConstructionBinding.toggle);input.update();keys.taps.clear();
        check(input.commandActive()&&!unit.updateBuilding(),"construction shortcut works before command-mode early return without exiting command mode");
        commands.leave(false);input.setCommands(null);commands.close();
        Unit replacement=builder(Vars.player,12,13);input.pollConstructionInput();tap(ConstructionBinding.toggle);check(!replacement.updateBuilding(),"new builder can be independently paused");
        var previousTiles=Vars.world.tiles;Vars.world.tiles=new Tiles(64,64);replacement.updateBuilding(true);input.pollConstructionInput();
        check(input.isBuilding&&replacement.updateBuilding(),"world identity change discards old explicit pause intent");Vars.world.tiles=previousTiles;unit=replacement;input.pollConstructionInput();
    }
    private static void clearIsDistinct(){
        var plan=new BuildPlan(24,12,0,Blocks.duo);unit.plans.addLast(plan);unit.updateBuilding(true);input.isBuilding=true;buildTicks(unit,8);
        var partial=(ConstructBuild)Vars.world.tile(24,12).build;float progress=partial.progress;int copper=core.items.get(Items.copper);
        tap(ConstructionBinding.toggle);unit.clearBuilding();buildTicks(unit,100);
        check(unit.plans.isEmpty()&&Vars.world.tile(24,12).build==partial&&partial.progress==progress&&core.items.get(Items.copper)==copper,"native explicit queue clear is separate from stop and does not demolish the physical blueprint");
        unit.plans.addLast(new BuildPlan(24,12,0,Blocks.duo));tap(ConstructionBinding.toggle);buildTicks(unit,1500);
        check(Vars.world.tile(24,12).block()==Blocks.duo,"native explicitly requeued partial blueprint can resume after clearing plans");
        input.setFpvEnabled(false);unit.updateBuilding(false);input.isBuilding=true;tap(ConstructionBinding.toggle);
        check(!unit.updateBuilding()&&input.isBuilding,"F3 and construction tracking leave original 2D input state untouched");
    }
}
