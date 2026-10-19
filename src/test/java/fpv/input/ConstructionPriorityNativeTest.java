package fpv.input;

import arc.*;
import arc.input.*;
import arc.mock.*;
import arc.math.geom.*;
import arc.graphics.g3d.Camera3D;
import arc.scene.Scene;
import arc.struct.*;
import arc.util.*;
import fpv.construction.*;
import fpv.generated.InputProfiles;
import fpv.render.LayerWorldRenderer;
import fpv.terrain.TerrainElevation;
import fpv.units.UnitAltitudeSystem;
import fpv.world.World3DAdapter;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.net.Net;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import java.util.*;

/** Real native builder/costs, existing upper scheduler and actual pointer boundary; no OS/GPU test. */
public final class ConstructionPriorityNativeTest {
    static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static class Keys extends MockInput {boolean alt;boolean pause;@Override public boolean keyDown(KeyCode c){return alt&&c==KeyCode.altLeft;}@Override public boolean keyTap(KeyBind k){return pause&&k==ConstructionBinding.toggle;}}
    static class Graphics extends MockGraphics {long frame;@Override public long getFrameId(){return frame;}@Override public int getWidth(){return 800;}@Override public int getHeight(){return 600;}}
    static Keys keys;static Graphics graphics;static Unit unit;static CoreBuild core;static UnitAltitudeSystem altitude;static LayerRuntime runtime;static LayerWorldRenderer renderer;static FpvDesktopInput input;static LayerBuildController controls;
    public static void main(String[] args){
        init();try{ground();upper();pickingAndAuthority();codec();System.out.println("ConstructionPriorityNativeTest: "+checks+" checks PASS; native progress/costs, XYZ priority and pointer lifecycle; physical gameplay NOT TESTED.");}
        finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
    static void init(){
        Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.gl=new MockGL20();Core.graphics=graphics=new Graphics();Core.input=keys=new Keys();
        Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new Net(null);Groups.init();Vars.world=new World();Vars.world.tiles=new Tiles(128,128);Vars.world.setGenerating(true);Vars.world.tiles.fill();Vars.indexer=new mindustry.ai.BlockIndexer();Vars.pathfinder=new mindustry.ai.Pathfinder(){@Override public void updateTile(Tile t){}};Vars.fogControl=new mindustry.game.FogControl();Groups.resize(0,0,1024,1024);Time.setDeltaProvider(()->1);Vars.content.init();Vars.state.set(GameState.State.playing);Vars.state.rules.editor=true;Vars.state.rules.instantBuild=false;Vars.state.rules.infiniteResources=false;
        Vars.world.tile(8,8).setBlock(Blocks.coreShard,Team.sharded,0);core=(CoreBuild)Vars.world.tile(8,8).build;Team.sharded.data().cores.add(core);for(var item:Vars.content.items())core.items.set(item,10000);
        Vars.player=Player.create();Vars.player.team(Team.sharded);unit=UnitTypes.alpha.create(Team.sharded);unit.set(96,96);unit.add();Vars.player.unit(unit);
        Core.scene=new Scene();input=new FpvDesktopInput(keys,InputProfiles.create());input.installProjection();input.setFpvEnabled(true);input.setBuildMode(true);input.setFlightAim(128,96);
        altitude=new UnitAltitudeSystem((x,y)->0);runtime=new LayerRuntime(new World3DAdapter(new TerrainElevation(),(x,y)->0,altitude));Core.settings.put(LayerRuntime.FREE_PERMISSION,true);renderer=new LayerWorldRenderer();renderer.setRuntime(runtime);controls=new LayerBuildController(runtime,input,renderer);
    }
    static void pause(){graphics.frame++;keys.pause=true;input.pollConstructionInput();keys.pause=false;}
    static void tick(){Time.delta=1;Time.update();Vars.state.updateId++;runtime.priority().beforeNative();try{unit.updateBuildLogic();}finally{runtime.priority().afterNative();}runtime.update(1.0/60);}
    static ConstructionPriority.Target target(int x,int y,int layer,Block block){return new ConstructionPriority.Target(unit.id,x,y,layer,false,block,0);}
    static Camera3D camera(int x,int y){var c=new Camera3D();c.resize(800,600);c.near=.01f;c.far=100;c.position.set(x,20,-y);c.direction.set(0,-1,0);c.up.set(0,0,-1);c.update();return c;}
    static LayerBuildController.Buttons buttons(boolean tap,boolean held){return new LayerBuildController.Buttons(tap,held,false,false,false,false,false,false,0,false);}
    static void click(int x,int y,boolean alt){keys.alt=alt;var c=camera(x,y);fpv.render.FpvWorldRenderer.PickResult ground=null;
        if(Vars.world.tile(x,y).build instanceof ConstructBuild)try{
            var ctor=fpv.render.FpvWorldRenderer.PickResult.class.getDeclaredConstructors()[0];ctor.setAccessible(true);float h=fpv.net.HeadlessGeometry.heightFor(Vars.world.tile(x,y).block());
            ground=(fpv.render.FpvWorldRenderer.PickResult)ctor.newInstance(x*8f,y*8f,h,20-h,Vars.world.tile(x,y));
        }catch(Exception e){throw new AssertionError(e);}
        controls.processPointer(c,ground,400,300,buttons(true,true));controls.processPointer(c,ground,400,300,buttons(true,true));controls.processPointer(c,ground,400,300,buttons(false,true));controls.processPointer(c,ground,400,300,buttons(false,false));keys.alt=false;
    }
    static void ground(){
        var a=new BuildPlan(16,12,0,Blocks.duo);var b=new BuildPlan(18,12,0,Blocks.duo);var c=new BuildPlan(20,12,0,Blocks.duo);unit.plans.addLast(a);unit.plans.addLast(b);unit.plans.addLast(c);int copper=core.items.get(Items.copper);
        for(int i=0;i<8;i++)tick();var partial=(ConstructBuild)Vars.world.tile(16,12).build;float progress=partial.progress;pause();
        controls.setLayer(7);click(20,12,false);
        check(unit.plans.first()==c&&unit.plans.get(1)==a&&unit.plans.get(2)==b,"actual click puts C first while retaining A/B object identity and order despite locked Z7");
        check(unit.plans.size==3&&!unit.updateBuilding()&&!input.isBuilding,"held/repeated click does not duplicate plans or resume F3 pause");
        for(int i=0;i<100;i++)tick();check(partial.progress==progress&&Vars.world.tile(20,12).block()==Blocks.air,"selected plan waits until explicit resume");
        pause();for(int i=0;i<2000&&Vars.world.tile(20,12).block()!=Blocks.duo;i++)tick();
        check(Vars.world.tile(20,12).block()==Blocks.duo&&partial.progress==progress,"C actually completes before unfinished A advances");
        for(int i=0;i<3000&&!unit.plans.isEmpty();i++)tick();check(Vars.world.tile(16,12).block()==Blocks.duo&&Vars.world.tile(18,12).block()==Blocks.duo,"A/B finish later through native builder");
        check(core.items.get(Items.copper)==copper-3*Blocks.duo.requirements[0].amount,"three exact native costs; no priority refund or duplicate spend");
        // A physical friendly partial blueprint need not already be in this player's queue.
        var p=new BuildPlan(22,12,0,Blocks.duo);unit.addBuild(p);for(int i=0;i<8;i++)tick();unit.clearBuilding();click(22,12,false);
        check(unit.plans.size==1&&unit.plans.first().x==22,"friendly partial ConstructBuild can be reselected through its visible pick when not already queued");
        for(int i=0;i<1500&&!unit.plans.isEmpty();i++)tick();
    }
    static void upper(){
        var schematic=new mindustry.game.Schematic(Seq.with(new mindustry.game.Schematic.Stile(Blocks.copperWall,0,0,null,(byte)0),new mindustry.game.Schematic.Stile(Blocks.copperWall,2,0,null,(byte)0),new mindustry.game.Schematic.Stile(Blocks.copperWall,4,0,null,(byte)0)),new StringMap(),5,1);
        check(runtime.apply(Vars.player,LayerActions.Action.paste(new LayerSchematic.Stamp(schematic,18,18),2,LayerComponent.Mode.FREE)).accepted(),"timed upper A/B/C queue accepted");int copper=core.items.get(Items.copper);for(int i=0;i<5;i++)tick();pause();input.block=Blocks.conveyor;
        click(20,18,true);check(runtime.priority().target(Vars.player).equals(target(20,18,2,Blocks.copperWall)),"Alt-click picks real Z2 C automatically while construction floor stays locked elsewhere");
        check(input.block==Blocks.conveyor&&runtime.pastes().pending(Vars.player)==3&&!unit.updateBuilding(),"selected placement tool, upper queue and F3 pause remain intact");
        for(int i=0;i<100;i++)tick();check(core.items.get(Items.copper)==copper&&runtime.grid().components().isEmpty(),"paused priority spends nothing and creates no structure");
        unit.addBuild(new BuildPlan(24,14,0,Blocks.duo));pause();
        for(int i=0;i<1000&&runtime.grid().at(20,18,2,LayerComponent.Kind.BLOCK)==null;i++){tick();check(unit.updateBuilding(),"native frame guard always restores user building flag");}
        check(runtime.grid().at(20,18,2,LayerComponent.Kind.BLOCK)!=null&&runtime.grid().at(16,18,2,LayerComponent.Kind.BLOCK)==null,"selected upper C actually completes first");
        check(Vars.world.tile(24,14).block()==Blocks.air&&unit.plans.size==1,"native Z0 work is retained and does not run in parallel with upper focus");
        check(runtime.apply(Vars.player,LayerActions.Action.prioritize(target(16,18,2,Blocks.copperWall))).accepted(),"switch upper priority back to A");
        int elapsed=0;while(elapsed++<1000&&runtime.grid().at(16,18,2,LayerComponent.Kind.BLOCK)==null)tick();
        int full=(int)Math.ceil(Blocks.copperWall.buildTime/(unit.type.buildSpeed*unit.buildSpeedMultiplier));
        check(elapsed<=full-4,"A retains its earlier five ticks of work instead of restarting or donating it to C: ticks="+elapsed+" full="+full);
        for(int i=0;i<3000&&(runtime.pastes().pending(Vars.player)>0||!unit.plans.isEmpty());i++)tick();
        check(runtime.grid().at(18,18,2,LayerComponent.Kind.BLOCK)!=null&&Vars.world.tile(24,14).block()==Blocks.duo,"remaining upper B and native D resume after chosen focus completes");
        check(core.items.get(Items.copper)==copper-3*Blocks.copperWall.requirements[0].amount-Blocks.duo.requirements[0].amount,"mixed-floor actual costs charged exactly once");input.block=null;
    }
    static void pickingAndAuthority(){
        unit.addBuild(new BuildPlan(28,20,0,Blocks.duo));var c=camera(28,20);var ray=c.getPickRay(400,300);
        var scene=BuildingMousePicker.pickScene(runtime,ray,100,null);check(BuildingMousePicker.pickBlueprint(runtime,ray,100,scene).equals(target(28,20,0,Blocks.duo)),"shared 3D picker sees own visible Z0 plan");
        var placed=runtime.apply(Vars.player,LayerActions.Action.place(28,20,3,"copper-wall",false,0,LayerComponent.Mode.FREE));check(placed.accepted(),"solid covering wall fixture placed");
        scene=BuildingMousePicker.pickScene(runtime,ray,100,null);check(BuildingMousePicker.pickBlueprint(runtime,ray,100,scene)==null,"opaque higher floor blocks a completely hidden lower blueprint");
        runtime.apply(Vars.player,LayerActions.Action.remove(placed.componentId()));
        unit.addBuild(new BuildPlan(30,30,0,Blocks.graphitePress));var cover=runtime.apply(Vars.player,LayerActions.Action.place(30,30,3,"copper-wall",false,0,LayerComponent.Mode.FREE));check(cover.accepted(),"partial-cover fixture accepted");
        var exposed=camera(31,31).getPickRay(400,300);check(BuildingMousePicker.pickBlueprint(runtime,exposed,100,BuildingMousePicker.pickScene(runtime,exposed,100,null)).equals(target(30,30,0,Blocks.graphitePress)),"exposed part of a partly occluded multi-tile blueprint remains selectable");
        var covered=camera(30,30).getPickRay(400,300);check(BuildingMousePicker.pickBlueprint(runtime,covered,100,BuildingMousePicker.pickScene(runtime,covered,100,null))==null,"covered part of the same blueprint is not selectable through the wall");runtime.apply(Vars.player,LayerActions.Action.remove(cover.componentId()));
        var stamp=new mindustry.game.Schematic(Seq.with(new mindustry.game.Schematic.Stile(Blocks.copperWall,0,0,null,(byte)0)),new StringMap(),1,1);
        check(runtime.apply(Vars.player,LayerActions.Action.paste(new LayerSchematic.Stamp(stamp,32,20),4,LayerComponent.Mode.FREE)).accepted(),"Z4 pending plan accepted");unit.addBuild(new BuildPlan(32,20,0,Blocks.duo));
        var overlap=camera(32,20).getPickRay(400,300);check(BuildingMousePicker.pickBlueprint(runtime,overlap,100,BuildingMousePicker.pickScene(runtime,overlap,100,null)).equals(target(32,20,4,Blocks.copperWall)),"overlapping XYZ plans resolve nearest visible Z4 rather than lower Z0 or player altitude");
        int money=core.items.get(Items.copper);core.items.set(Items.copper,0);check(!runtime.apply(Vars.player,LayerActions.Action.prioritize(target(32,20,4,Blocks.copperWall))).accepted()&&runtime.pastes().pending(Vars.player)==1,"priority cannot bypass real upper resource requirements or delete a waiting plan");core.items.set(Items.copper,money);
        Core.settings.put(LayerRuntime.FREE_PERMISSION,false);check(!runtime.apply(Vars.player,LayerActions.Action.prioritize(target(32,20,4,Blocks.copperWall))).accepted(),"priority cannot grant host-disabled Free Mode");Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
        input.block=Blocks.conveyor;int count=runtime.grid().size();click(37,35,true);check(input.block==Blocks.conveyor&&runtime.grid().size()==count&&runtime.priority().target(Vars.player)==null,"Alt-click miss consumes the gesture without placing, erasing a tool or selecting an unrelated blueprint");input.block=null;
        var blocked=new BuildingMousePicker.ScenePick(null,1);check(BuildingMousePicker.pickBlueprint(runtime,ray,100,blocked)==null,"hidden/fog-occluder depth survives even with no public building identity");
        int old=unit.plans.size;var other=Player.create();other.team(Team.sharded);var u=UnitTypes.alpha.create(Team.sharded);u.set(96,96);u.add();other.unit(u);
        check(!runtime.apply(other,LayerActions.Action.prioritize(target(28,20,0,Blocks.duo))).accepted()&&unit.plans.size==old,"forged builder identity cannot edit another player's queue");
        unit.set(120*8,120*8);check(!runtime.apply(Vars.player,LayerActions.Action.prioritize(target(28,20,0,Blocks.duo))).accepted(),"server rejects out-of-XYZ-range priority");unit.set(96,96);
        check(!runtime.apply(Vars.player,LayerActions.Action.prioritize(target(60,60,2,Blocks.copperWall))).accepted(),"nonexistent upper blueprint cannot create free construction");
        check(runtime.apply(Vars.player,LayerActions.Action.prioritize(target(28,20,0,Blocks.duo))).accepted(),"native priority selected before a vanilla queue override");
        var override=new BuildPlan(26,22,0,Blocks.conveyor);unit.addBuild(override,false);runtime.priority().beforeNative();runtime.priority().afterNative();
        check(unit.plans.first()==override&&runtime.priority().target(Vars.player)==null,"later vanilla queue reordering or a native blocked-plan skip takes precedence; no persistent re-pin in 2D");
        unit.clearBuilding();runtime.priority().beforeNative();runtime.priority().afterNative();check(unit.plans.isEmpty(),"explicit native queue clear never resurrects a prioritized plan");
        controls.setLayer(4);controls.setFree(true);input.block=Blocks.conveyor;int before=core.items.get(Items.copper);click(32,20,false);
        var ordinary=runtime.grid().at(32,20,4,LayerComponent.Kind.BLOCK);
        check(ordinary!=null&&ordinary.blockName().equals("conveyor")&&runtime.priority().target(Vars.player)==null,"ordinary placement with a selected tool takes precedence over an unfinished blueprint unless Alt is held");
        check(core.items.get(Items.copper)==before-Blocks.conveyor.requirements[0].amount&&runtime.pastes().pending(Vars.player)==1,"ordinary held placement charges once and does not remove pending requests as a priority side effect");input.block=null;
        runtime.reset();check(runtime.priority().target(Vars.player)==null&&runtime.pastes().targets(Vars.player).isEmpty(),"world transition clears transient focus and pending views");
    }
    static void codec(){
        var list=List.of(target(30,30,2,Blocks.duo),target(32,32,3,Blocks.graphitePress));byte[] bytes=ConstructionPlanCodec.encode(list);check(ConstructionPlanCodec.decode(bytes).equals(list),"owner-private pending view roundtrips exact layer/block/rotation/identity");
        byte[] tail=Arrays.copyOf(bytes,bytes.length+1);reject(()->ConstructionPlanCodec.decode(tail));byte[] bad=bytes.clone();bad[0]=2;reject(()->ConstructionPlanCodec.decode(bad));reject(()->ConstructionPlanCodec.decode(new byte[]{1,16,1}));
        var action=LayerActions.Action.prioritize(list.get(0));check(fpv.net.NetProtocol.read(fpv.net.NetProtocol.bytes(out->LayerActions.write(out,action)),LayerActions::read).equals(action),"priority uses existing bounded reliable construction envelope");
    }
    static void reject(Runnable r){try{r.run();throw new AssertionError("malformed pending view accepted");}catch(IllegalArgumentException expected){checks++;}}
}
