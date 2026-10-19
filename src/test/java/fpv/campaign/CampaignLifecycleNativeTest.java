package fpv.campaign;

import arc.*;
import arc.files.Fi;
import arc.mock.*;
import arc.util.*;
import fpv.construction.*;
import fpv.control.*;
import fpv.net.HeadlessFpvRuntime;
import fpv.units.ground.GroundStanceSmokeChecks;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.type.Sector;
import mindustry.world.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;

/** Actual 160.7 planet generators + full disk SaveIO, in a new profile only.
 * Mock application/audio/graphics, offline native Net(null): not desktop input or multiplayer acceptance. */
public final class CampaignLifecycleNativeTest {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static void boot()throws Exception{
        Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new Settings();
        Core.settings.setDataDirectory(new Fi(java.nio.file.Files.createTempDirectory("fpv-a203-campaign-").toFile()));
        Core.settings.setAutosave(false);Core.settings.setJson(JsonIO.json);Core.settings.load();
        Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();
        Vars.headless=true;Vars.loadLocales=false;Vars.net=new Net(null);Vars.platform=new Platform(){};
        Vars.init();Vars.content.createBaseContent();Vars.content.init();Vars.logic=new Logic();
        Vars.netServer=new NetServer();Vars.netClient=new NetClient();Vars.schematics=new mindustry.game.Schematics();Vars.schematics.load();
        Time.setDeltaProvider(()->1f);
    }
    private static void sector(Sector sector){
        new WorldReloader().begin();Vars.world.loadSector(sector);Vars.logic.play();
        check(Vars.state.isCampaign()&&Vars.state.getSector()==sector,"native requested campaign sector is active");
        check(Vars.state.rules.defaultTeam.core()!=null&&!Vars.world.isGenerating(),"generated sector has a live native core and completed loading");
    }
    private static void player(){
        Vars.player=Player.create();Vars.player.team(Vars.state.rules.defaultTeam);Vars.player.add();
        var core=Vars.player.team().core();var type=((mindustry.world.blocks.storage.CoreBlock)core.block).unitType;
        Unit unit=type.create(Vars.player.team());unit.set(core.x,core.y);unit.add();Vars.player.unit(unit);
    }
    private static long tiles(){
        long h=1;for(var t:Vars.world.tiles){h=h*31+t.floorID();h=h*31+t.overlayID();h=h*31+t.blockID();}return h;
    }
    private static byte[] hash(Fi file)throws Exception{return MessageDigest.getInstance("SHA-256").digest(file.readBytes());}
    private static Tile site(){
        var core=Vars.player.team().core();
        for(var t:Vars.world.tiles)if(t.block()==Blocks.air&&!t.floor().isLiquid&&t.dst(core.x,core.y)>40&&t.dst(core.x,core.y)<100)return t;
        throw new AssertionError("Open construction site near the core");
    }
    public static void main(String[] args)throws Exception{
        boot();check(Version.build==160&&Version.revision==7,"test uses pinned Mindustry 160.7");
        check(!Vars.net.active(),"offline first; no socket provider");
        if(args.length==2&&args[0].equals("--restore-fixture")){
            Path root=Path.of(System.getProperty("fpv.campaign.fixtureRoot")).toRealPath(),file=Path.of(args[1]).toRealPath();
            check(file.startsWith(root)&&!file.equals(root),"explicit disposable fixture root required");
            Fi fixture=new Fi(file.toFile());byte[] before=hash(fixture);var runtime=new HeadlessFpvRuntime();runtime.install();
            try{
                SaveIO.load(fixture);Vars.state.set(GameState.State.playing);
                check(Vars.state.isCampaign()&&Vars.state.rules.defaultTeam.core()!=null,"old checkpoint fixture restores campaign and core in a fresh JVM");
                check(!runtime.construction.grid().components().isEmpty(),"old checkpoint elevated structures restore in a fresh JVM");
                check(!runtime.network.nativeBoost().snapshot().isEmpty()&&!runtime.network.creatorFlight().snapshot().isEmpty(),"old checkpoint boost/creator state restores in a fresh JVM");
                check(!runtime.altitudes.groundFamily().snapshotStances().isEmpty()||Groups.unit.contains(u->u.type==UnitTypes.atrax),"saved walker entity is available for lazy stance restore");
                check(Arrays.equals(before,hash(fixture)),"old fixture is read only");
            }finally{runtime.close();}
            System.out.println("CampaignLifecycleNativeTest: "+checks+" cold-JVM existing checkpoint campaign/upper/boost/creator checks PASS; disposable fixture only");return;
        }
        sector(SectorPresets.groundZero.sector);
        var vanilla=Vars.saveDirectory.child("before-mod.msav");SaveIO.save(vanilla);byte[] vanillaHash=hash(vanilla);
        check(SaveIO.isSaveValid(vanilla),"fresh native campaign save without FPV chunks is valid");
        int initialCopper=Vars.state.rules.defaultTeam.core().items.get(Items.copper);
        var runtime=new HeadlessFpvRuntime();runtime.install();
        try{
            SaveIO.load(vanilla,Vars.world.makeSectorContext(SectorPresets.groundZero.sector));Vars.state.set(GameState.State.playing);
            check(runtime.construction.grid().components().isEmpty(),"existing vanilla save invents no upper blocks");
            check(Vars.state.rules.defaultTeam.core().items.get(Items.copper)==initialCopper,"existing campaign core inventory survives mod initialization/load");
            player();Blocks.copperWall.unlock();var core=Vars.player.team().core();core.items.add(Items.copper,500);
            Tile site=site();site.setBlock(Blocks.copperWall,Vars.player.team());int before=core.items.get(Items.copper);
            var placed=runtime.construction.apply(Vars.player,LayerActions.Action.place(site.x,site.y,1,"copper-wall",false,1,LayerComponent.Mode.STRUCTURAL));
            check(placed.accepted(),"campaign structural wall accepted: "+placed.reason());
            check(core.items.get(Items.copper)<before,"campaign construction charges real native resources");
            Blocks.conveyor.unlock();var belt=runtime.construction.apply(Vars.player,LayerActions.Action.place(site.x,site.y,2,"conveyor",false,2,LayerComponent.Mode.STRUCTURAL));
            check(belt.accepted(),"functional Z2 conveyor supported by saved wall: "+belt.reason());
            runtime.construction.building(belt.componentId()).damage(3);
            float beltHealth=runtime.construction.building(belt.componentId()).health;
            var natural=java.util.stream.StreamSupport.stream(Vars.world.tiles.spliterator(),false).filter(runtime.terrain::naturalWall).findFirst().orElseThrow();
            String classification="{\"version\":1,\"tiles\":{\""+natural.x+","+natural.y+"\":\"artificial\"}}";
            int naturalCount=runtime.terrain.naturalWallCount();Vars.state.rules.tags.put(fpv.terrain.TerrainClassification.TAG,classification);
            check(!runtime.terrain.naturalWall(natural)&&runtime.terrain.naturalWallCount()==naturalCount-1,"native campaign rules override only the chosen rock cell");
            int copper=core.items.get(Items.copper);long nativeTiles=tiles();long terrain=runtime.terrain.mapFingerprint();
            check(tiles()==nativeTiles,"terrain height generation leaves source tiles unchanged");
            System.setProperty("fpv.smoke","true");
            var stance=GroundStanceSmokeChecks.prepareSave(runtime.altitudes);
            var boost=NativeBoostSaveSmokeChecks.prepare(runtime.network.nativeBoost(),runtime.altitudes);
            var creator=CreatorSaveSmokeChecks.prepare(runtime.network.creatorFlight(),runtime.altitudes);
            Unit aircraft=UnitTypes.flare.create(Vars.player.team());aircraft.set(site.worldx(),site.worldy());aircraft.add();aircraft.damage(7);
            int aircraftId=aircraft.id;float aircraftHealth=aircraft.health;
            var saved=Vars.saveDirectory.child("serpulo-with-upper.msav");SaveIO.save(saved);byte[] first= saved.readBytes();
            check(SaveIO.isSaveValid(saved),"full native save includes entities and all FPV custom chunks");
            SaveIO.load(saved,Vars.world.makeSectorContext(SectorPresets.groundZero.sector));Vars.state.set(GameState.State.playing);
            check(classification.equals(Vars.state.rules.tags.get(fpv.terrain.TerrainClassification.TAG)),"native full SaveIO preserves map classification without new chunks/formats");
            var wall=runtime.construction.grid().get(placed.componentId());
            check(wall!=null&&wall.layer()==1&&wall.rotation()==1,"native SaveIO restores elevated wall identity/layer/rotation");
            var restoredBelt=runtime.construction.building(belt.componentId());
            check(restoredBelt!=null&&restoredBelt.rotation==2&&restoredBelt.health==beltHealth,"functional upper building rotation and exact damaged HP restored");
            check(Vars.state.rules.defaultTeam.core().items.get(Items.copper)==copper,"load does not pay for construction twice or duplicate resources");
            check(tiles()==nativeTiles&&runtime.terrain.mapFingerprint()==terrain,"tiles and derived terrain reproduce exactly after full reload");
            Unit restoredAircraft=Groups.unit.getByID(aircraftId);
            check(restoredAircraft!=null&&restoredAircraft.type==UnitTypes.flare&&restoredAircraft.health==aircraftHealth&&restoredAircraft.x==site.worldx()&&restoredAircraft.y==site.worldy(),"native aircraft identity/XY/HP survive sector save reload");
            var aircraftBody=runtime.altitudes.unitState(restoredAircraft);
            check(aircraftBody!=null&&aircraftBody.flying()&&Double.isFinite(aircraftBody.z()),"ordinary aircraft receives a valid regenerated 3D altitude after load");
            check(GroundStanceSmokeChecks.verifySaved(runtime.altitudes,stance).startsWith("PASS"),"native full save restores walker stance");
            check(NativeBoostSaveSmokeChecks.verify(runtime.network.nativeBoost(),runtime.altitudes,boost).startsWith("PASS"),"native full save restores boost and safe grounded handoff");
            check(CreatorSaveSmokeChecks.verify(runtime.network.creatorFlight(),runtime.altitudes,creator).startsWith("PASS"),"native full save restores creator XYZ and safe landing");
            SaveIO.save(saved);check(Arrays.equals(first,SaveIO.backupFileFor(saved).readBytes()),"native overwrite preserves exact previous save as a backup");
            Fi copied=Vars.saveDirectory.child("existing-copy.msav");SaveIO.backupFileFor(saved).copyTo(copied);byte[] copyHash=hash(copied);
            SaveIO.load(copied,Vars.world.makeSectorContext(SectorPresets.groundZero.sector));Vars.state.set(GameState.State.playing);
            check(!runtime.network.creatorFlight().snapshot().isEmpty()&&!runtime.network.nativeBoost().snapshot().isEmpty(),"transition fixture contains active saved flight authority before leaving");
            sector(Planets.serpulo.sectors.get(2));
            check(runtime.construction.grid().components().isEmpty(),"old sector upper structures do not leak into generated Serpulo sector");
            check(runtime.network.creatorFlight().snapshot().isEmpty()&&runtime.network.nativeBoost().snapshot().isEmpty(),"world transition clears previous flight authority");
            check(!Vars.state.rules.tags.containsKey(fpv.terrain.TerrainClassification.TAG),"generated next campaign sector does not inherit authored overrides");
            long other=runtime.terrain.mapFingerprint();check(other!=terrain,"terrain follows newly generated sector");
            sector(SectorPresets.onset.sector);
            check(Vars.state.getPlanet()==Planets.erekir&&runtime.construction.grid().components().isEmpty(),"Erekir preset and environment remain native without previous layers");
            var erekir=Vars.saveDirectory.child("erekir.msav");SaveIO.save(erekir);
            SaveIO.load(erekir,Vars.world.makeSectorContext(SectorPresets.onset.sector));Vars.state.set(GameState.State.playing);
            check(Vars.state.getPlanet()==Planets.erekir&&Vars.state.rules.defaultTeam.core()!=null,"Erekir full disk save reload retains planet/core");
            SaveIO.load(copied,Vars.world.makeSectorContext(SectorPresets.groundZero.sector));Vars.state.set(GameState.State.playing);
            check(Vars.state.getSector()==SectorPresets.groundZero.sector&&runtime.construction.grid().get(placed.componentId())!=null,"return to copied existing campaign restores correct upper structures");
            check(Arrays.equals(hash(copied),copyHash)&&Arrays.equals(hash(vanilla),vanillaHash),"source fixtures remain byte-identical after loading copies/transitions");
            for(int i=0;i<2;i++){
                new WorldReloader().begin();Vars.world.loadGenerator(48,48,t->{t.fill();t.get(12,12).setBlock(Blocks.coreShard,Vars.state.rules.defaultTeam);});Vars.logic.play();
                check(!Vars.state.isCampaign()&&runtime.construction.grid().components().isEmpty()&&runtime.network.creatorFlight().snapshot().isEmpty()&&runtime.network.nativeBoost().snapshot().isEmpty(),"campaign to custom world clears sector-specific layers and active flight authority");
                SaveIO.load(copied,Vars.world.makeSectorContext(SectorPresets.groundZero.sector));Vars.state.set(GameState.State.playing);
                check(runtime.construction.grid().components().size()==2&&runtime.terrain.mapFingerprint()==terrain,"repeated custom-to-campaign restores once without terrain/state leaks");
            }
            Core.settings.forceSave();var settings=new Settings();settings.setDataDirectory(Vars.dataDirectory);settings.setJson(JsonIO.json);settings.load();
            var start=SectorPresets.groundZero.sector;
            check(settings.has(start.planet.name+"-s-"+start.id+"-info"),"sector progression metadata is persisted inside disposable profile");
        }finally{runtime.close();System.clearProperty("fpv.smoke");}
        System.out.println("CampaignLifecycleNativeTest: "+checks+" real sector generation/full disk SaveIO/backup/transition checks PASS; mock graphics, no physical UI or multiplayer proof");
    }
}
