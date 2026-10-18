package fpv.construction;

import arc.Core;
import arc.Events;
import arc.mock.*;
import arc.util.I18NBundle;
import fpv.terrain.TerrainElevation;
import fpv.units.UnitAltitudeSystem;
import fpv.world.World3DAdapter;
import fpv.net.FpvNetwork;
import fpv.combat.Combat3DSystem;
import fpv.effects.RadialExplosion;
import java.io.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.net.Net;
import mindustry.world.*;
import mindustry.io.SaveIO;

/** Native custom-chunk writer/reader and lifecycle hooks, in memory; not planet UI or network streaming. */
public final class A201CampaignChunkTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static byte[] write(boolean network)throws Exception{
        var bytes=new ByteArrayOutputStream();SaveIO.getSaveWriter().writeCustomChunks(new DataOutputStream(bytes),network);return bytes.toByteArray();
    }
    public static void main(String[] args)throws Exception{
        Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();
        Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new Net(null);Groups.init();
        Vars.netServer=new NetServer();Vars.netClient=new NetClient();
        Vars.world=new World();Vars.world.tiles=new Tiles(40,40);Vars.world.setGenerating(true);Vars.world.tiles.fill();Blocks.copperWall.init();
        Vars.state.rules.infiniteResources=true;Vars.state.rules.editor=true;Vars.state.rules.sector=Planets.serpulo.sectors.first();Vars.state.set(GameState.State.playing);
        Vars.player=Player.create();Vars.player.team(Team.sharded);Unit builder=UnitTypes.alpha.create(Team.sharded);builder.set(32,32);builder.add();Vars.player.unit(builder);
        var terrain=new TerrainElevation();var altitudes=new UnitAltitudeSystem(terrain::heightAt);var world=new World3DAdapter(terrain,(x,y)->0,altitudes);
        var explosion=new RadialExplosion(world);var combat=new Combat3DSystem(world,()->false,u->null);var network=new FpvNetwork(world,altitudes,combat,explosion,(x,y)->0);
        var layer=new LayerRuntime(world);layer.install(network);Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
        var placed=layer.apply(Vars.player,LayerActions.Action.place(10,10,2,"copper-wall",false,0,LayerComponent.Mode.FREE));check(placed.accepted(),"synthetic campaign layer created");
        byte[] campaign=write(false);check(new DataInputStream(new ByteArrayInputStream(campaign)).readInt()==1,"native save writer includes the layer chunk");
        byte[] stream=write(true);check(new DataInputStream(new ByteArrayInputStream(stream)).readInt()==0,"native world stream intentionally excludes layer custom chunk");
        Events.fire(new ResetEvent());check(layer.grid().components().isEmpty(),"leaving sector clears previous layer grid");
        Events.fire(new WorldLoadEvent());SaveIO.getSaveWriter().readCustomChunks(new DataInputStream(new ByteArrayInputStream(campaign)));
        check(layer.grid().get(placed.componentId())!=null,"world-load followed by native custom chunk read retains restored layer");
        Vars.state.rules.sector=null;Events.fire(new ResetEvent());Events.fire(new WorldLoadEvent());
        check(layer.grid().components().isEmpty(),"campaign to empty custom map does not leak upper blocks");
        SaveIO.getSaveWriter().readCustomChunks(new DataInputStream(new ByteArrayInputStream(campaign)));
        check(layer.grid().components().size()==1,"returning to the saved chunk restores exactly one block");
        SaveIO.getSaveWriter().readCustomChunks(new DataInputStream(new ByteArrayInputStream(campaign)));
        check(layer.grid().components().size()==1,"repeated native restore does not duplicate structures");
        layer.close();
        System.out.println("A201CampaignChunkTest: "+checks+" native chunk/event checks PASS; no user saves, physical campaign UI or client reconnect tested");
    }
}
