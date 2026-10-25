package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.Gl;import arc.graphics.g3d.Camera3D;import arc.util.*;
import fpv.render.*;import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;

/** Same cameras/maps in all three styles. Isolated standalone GPU checks and warm CPU timings. */
final class SmokeHarness {
    final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();
    FpvWorldRenderer draw;boolean started,done;int frame,map=-1,checks;long generation,sourceRevision,sourceHash,warmBuilds;
    final java.util.ArrayList<Double> samples=new java.util.ArrayList<>();final StringBuilder csv=new StringBuilder("map,view,style,meanCpuMs,medianCpuMs,p95CpuMs,chunkGenerationMs,cachedVertices,cachedBytes,chunks,detailed,coarse\n");
    String input,motion;long startNanos;final String[] names={"sparse","mountain-heavy","mixed-biome","artificial-heavy"};
    SmokeHarness(FpvMod mod){this.mod=mod;}void update(){}
    void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    void start(){try{
        check(Vars.dataDirectory.absolutePath().startsWith(out.absolutePath())&&!Vars.steam,"Disposable standalone only");
        Core.settings.put("fpscap",120);Core.settings.put("sfxvol",0);Core.settings.put("musicvol",0);Core.settings.put("fpv-auto-horizon",false);Core.settings.put(RenderDistanceSettings.DETAIL,80);
        var auto=FpvMod.class.getDeclaredField("autoEnable");auto.setAccessible(true);auto.set(mod,false);started=true;
    }catch(Throwable t){fail(t);}}
    void world(int kind){
        if(draw!=null)draw.dispose();map=kind;Vars.logic.reset();int n=kind==2?256:kind==1?160:128;
        Vars.world.loadGenerator(n,n,tiles->{
            for(int y=0;y<n;y++)for(int x=0;x<n;x++){
                var floor=kind==2?(x<n/3?Blocks.snow:x<n*2/3?Blocks.sand:Blocks.basalt):Blocks.stone;
                if(kind==2&&y<25)floor=Blocks.water;
                boolean mountain=kind==0?Math.hypot(x-65,y-66)<19||Math.hypot(x-32,y-50)<4:
                    kind==1?x>12&&x<n-10&&y>25&&y<n-8:
                    kind==2?Math.hypot(x-56,y-128)<30||Math.hypot(x-128,y-124)<35||Math.hypot(x-198,y-128)<32||Math.hypot(x-40,y-30)<9:x>26&&x<105&&y>35&&y<110;
                if(kind==1&&x>65&&x<72&&y>65&&y<72)mountain=false;
                if(kind==3&&x%10>=3&&x%10<=6&&y%10>=3&&y%10<=6)mountain=false;
                var rock=kind==2?(x<n/3?Blocks.snowWall:x<n*2/3?Blocks.sandWall:Blocks.daciteWall):Blocks.stoneWall;
                tiles.set(x,y,new Tile(x,y,floor,Blocks.air,mountain?rock:Blocks.air));
            }
            tiles.get(8,8).setBlock(Blocks.coreShard,Team.sharded,0);
            for(int x=22;x<37;x++)tiles.get(x,32).setBlock(Blocks.copperWall,Team.sharded,0);
            for(int x=15;x<28;x++)for(int y=36;y<42;y++)if(tiles.get(x,y).block()==Blocks.air)tiles.get(x,y).setOverlay(Blocks.oreCopper);
            for(int y=43;y<80;y+=5)if(tiles.get(20,y).block()==Blocks.air)tiles.get(20,y).setBlock(Blocks.pine);
            if(kind==3)for(int y=43;y<100;y+=10)for(int x=33;x<100;x+=10)tiles.get(x,y).setBlock(Blocks.graphitePress,Team.sharded,0);
        });
        Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","A217 "+names[kind]));Vars.state.rules.fog=false;Vars.state.rules.staticFog=false;Vars.state.rules.waves=false;Vars.state.rules.canGameOver=false;Vars.state.rules.unitCap=1000;
        Vars.player.team(Team.sharded);var u=UnitTypes.gamma.create(Team.sharded);u.set(8*8,8*8);u.add();Vars.player.unit(u);Vars.logic.play();Vars.state.set(State.paused);
        if(kind==0){mod.setEnabled(true);input=fpv.input.InputSmokeChecks.run(mod.input);motion=fpv.flight.UnitMotionSmokeChecks.run();mod.setBuildMode(true);check(mod.buildMode,"Tab API");mod.setBuildMode(false);}
        mod.setEnabled(false);draw=new FpvWorldRenderer();draw.setUnitAltitudes(mod.unitAltitudes);draw.setConstruction(mod.construction);draw.setCombat(mod.combat);
        Vars.state.rules.editor=true;Vars.state.rules.infiniteResources=true;Core.settings.put(fpv.construction.LayerRuntime.FREE_PERMISSION,true);
        for(int floor:new int[]{1,3}){
            var slab=mod.construction.apply(Vars.player,fpv.construction.LayerActions.Action.place(24,20,floor,"air",true,0,fpv.construction.LayerComponent.Mode.FREE));check(slab.accepted(),"fixture elevated platform: "+slab.reason());
            var wall=mod.construction.apply(Vars.player,fpv.construction.LayerActions.Action.place(24,20,floor,"copper-wall",false,0,fpv.construction.LayerComponent.Mode.FREE));check(wall.accepted(),"fixture elevated wall: "+wall.reason());
        }
        sourceRevision=draw.terrain().revision();sourceHash=hash();
    }
    long hash(){long h=1;for(var t:Vars.world.tiles)h=h*31+t.blockID()*17+t.floorID()*3+t.overlayID();return h;}
    void afterDraw(){if(!started||done)return;try{
        int stages=24,stage=frame/180,tick=frame%180,kind=stage/6,view=stage/3%2,style=stage%3;
        if(kind!=map)world(kind);
        if(tick==0){Core.settings.put(MountainStyle.SETTING,style);samples.clear();generation=draw.terrainMeshStats().buildNanos();startNanos=System.nanoTime();}
        float cx=map==2?128:65,cy=map==2?124:70;
        camera.resize(Core.graphics.getWidth(),Core.graphics.getHeight());camera.near=.04f;camera.far=1000;camera.fov=65;camera.up.set(0,1,0);
        if(view==0){camera.position.set(cx+40,6,-cy+48);camera.lookAt(cx,10,-cy);}else{camera.position.set(cx+42,map==2?160:105,-cy+(map==2?95:65));camera.lookAt(cx,5,-cy);}
        camera.update();long start=System.nanoTime();draw.render(camera);double ms=(System.nanoTime()-start)/1e6;
        check(Gl.getError()==0,"GL error");check(draw.terrain().revision()==sourceRevision,"style must not regenerate canonical terrain");
        if(tick==59)warmBuilds=draw.terrainMeshStats().builds();
        if(tick>=60){samples.add(ms);check(draw.terrainMeshStats().builds()==warmBuilds,"stationary warm view rebuilds no terrain meshes");}
        String name=names[kind]+"-"+(view==0?"low":"high")+"-"+MountainStyle.of(style).label().toLowerCase(java.util.Locale.ROOT);
        if(tick==50)ScreenUtils.saveScreenshot(out.child(name+".png"));
        if(tick==179){
            check(hash()==sourceHash,"style must not change native world");
            var stats=draw.terrainMeshStats();var sorted=new java.util.ArrayList<>(samples);java.util.Collections.sort(sorted);double mean=samples.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            csv.append(String.format(java.util.Locale.ROOT,"%s,%s,%s,%.6f,%.6f,%.6f,%.3f,%d,%d,%d,%d,%d%n",names[kind],view==0?"low":"high",stats.style(),mean,sorted.get(sorted.size()/2),sorted.get((int)(sorted.size()*.95)),(stats.buildNanos()-generation)/1e6,stats.vertices(),stats.bytes(),stats.chunks(),draw.stats().detailedChunks(),draw.stats().coarseChunks()));
            out.child("measurements.csv").writeString(csv.toString());
        }
        if(++frame==stages*180){
            // Real SaveIO roundtrip, preserving style as local settings and canonical geometry.
            var save=out.child("disposable-terrain.msav");long before=hash();float height=draw.terrain().maxHeight();int upper=mod.construction.grid().components().size();mindustry.io.SaveIO.save(save);mindustry.io.SaveIO.load(save);Vars.state.set(State.paused);
            check(before==hash()&&Math.abs(height-draw.terrain().maxHeight())<.00001,"native save/load geometry stable");
            check(mod.construction.grid().components().size()==upper,"actual upper structures survive native SaveIO");
            Core.settings.forceSave();var reread=new arc.Settings();reread.setDataDirectory(Core.settings.getDataDirectory());reread.loadValues();check(reread.getInt(MountainStyle.SETTING,-1)==2,"local style persisted to real settings file");
            done=true;out.child("result.json").writeString("{\"status\":\"PASS\",\"java\":\""+System.getProperty("java.version")+"\",\"scenes\":24,\"frames\":"+frame+",\"checks\":"+checks+",\"glErrors\":0,\"physicalAcceptance\":false,\"saveLoad\":true,\"settingsDiskReadback\":true}");
            out.child("input-api.txt").writeString(input+"\n"+motion);draw.dispose();Core.app.post(Core.app::exit);
        }
    }catch(Throwable t){fail(t);}}
    void fail(Throwable t){done=true;out.child("failed.txt").writeString(t.toString());Log.err(t);Core.app.post(Core.app::exit);}
}
