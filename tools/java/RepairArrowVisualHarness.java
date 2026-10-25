package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.Gl;import arc.graphics.g3d.Camera3D;import arc.math.geom.Vec3;import arc.util.*;
import fpv.render.*;import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.game.Team;import mindustry.gen.*;import mindustry.world.Tile;import mindustry.world.blocks.units.RepairTurret;

/** Actual native HP and full world/preview passes. Pixel readbacks are test-only. */
final class SmokeHarness {
    final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();FpvWorldRenderer draw;
    final java.util.ArrayList<RepairTurret.RepairPointBuild> sources=new java.util.ArrayList<>();final java.util.ArrayList<Unit> targets=new java.util.ArrayList<>();
    boolean started,done;int frame,active=-1,checks;float hp,strength;String input,motion;
    SmokeHarness(FpvMod m){mod=m;}void update(){}
    void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    void start(){try{
        check(Vars.dataDirectory.absolutePath().startsWith(out.absolutePath())&&!Vars.steam,"isolated standalone");Core.settings.put("fpscap",120);Core.settings.put("fpv-auto-horizon",false);Vars.logic.reset();
        Vars.world.loadGenerator(96,96,tiles->{for(int y=0;y<96;y++)for(int x=0;x<96;x++)tiles.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));tiles.get(8,8).setBlock(Blocks.coreShard,Team.sharded,0);});
        Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","Repair beam and preview arrow validation"));Vars.player.team(Team.sharded);var pilot=UnitTypes.gamma.create(Team.sharded);pilot.set(64,64);pilot.add();Vars.player.unit(pilot);Vars.logic.play();Vars.state.set(State.paused);Vars.state.rules.fog=false;Vars.state.rules.waves=false;Vars.state.rules.unitCap=1000;Vars.state.rules.editor=true;Vars.state.rules.infiniteResources=true;Vars.state.rules.instantBuild=true;Core.settings.put(LayerRuntime.FREE_PERMISSION,true);
        var auto=FpvMod.class.getDeclaredField("autoEnable");auto.setAccessible(true);auto.set(mod,false);mod.setEnabled(true);input=fpv.input.InputSmokeChecks.run(mod.input);motion=fpv.flight.UnitMotionSmokeChecks.run();mod.setBuildMode(true);check(mod.buildMode,"Build mode API");mod.setBuildMode(false);mod.setEnabled(false);
        for(int floor:new int[]{0,1,3})for(var block:new mindustry.world.Block[]{Blocks.repairPoint,Blocks.repairTurret}){
            int x=block==Blocks.repairPoint?20:40,y=30+floor*12;RepairTurret.RepairPointBuild b;
            if(floor==0){Vars.world.tile(x,y).setBlock(block,Team.sharded,0);b=(RepairTurret.RepairPointBuild)Vars.world.build(x,y);}
            else{int low=-(block.size-1)/2;for(int dy=0;dy<block.size;dy++)for(int dx=0;dx<block.size;dx++)place(x+low+dx,y+low+dy,floor,Blocks.air,true);b=(RepairTurret.RepairPointBuild)mod.construction.building(place(x,y,floor,block,false));}
            var target=UnitTypes.zenith.create(Team.sharded);target.set(b.x+32,b.y+8);target.add();target.health=50;mod.unitAltitudes.initializePayloadRelease(target,floor*1.4+4);b.efficiency=0;b.target=null;sources.add(b);targets.add(target);
        }
        Vars.world.tileChanges=-1;draw=new FpvWorldRenderer();draw.setUnitAltitudes(mod.unitAltitudes);draw.setConstruction(mod.construction);draw.setCombat(mod.combat);started=true;
    }catch(Throwable t){fail(t);}}
    long place(int x,int y,int floor,mindustry.world.Block b,boolean slab){var r=mod.construction.apply(Vars.player,LayerActions.Action.place(x,y,floor,b.name,slab,0,LayerComponent.Mode.FREE));check(r.accepted(),"native fixture placement: "+r.reason());return r.componentId();}
    int arrows(Object owner)throws Exception{var f=owner.getClass().getDeclaredField("constructionVisuals");f.setAccessible(true);return ((ConstructionVisualRenderer)f.get(owner)).directionArrows();}
    void activate(int index){if(active==index)return;for(var b:sources){b.efficiency=0;b.target=null;}active=index;var b=sources.get(index);var t=targets.get(index);b.target=t;b.enabled=true;b.efficiency=b.potentialEfficiency=1;b.rotation=b.angleTo(t);b.timer.reset(((RepairTurret)b.block).timerTarget,0);float old=t.health,delta=Time.delta;Time.delta=1;for(int i=0;i<12;i++)b.updateTile();Time.delta=delta;check(t.health>old&&b.strength>.1,"actual HP healed before render Z"+(index/2));hp=t.health;strength=b.strength;}
    void afterDraw(){if(!started||done)return;try{
        int stage=frame/48,tick=frame%48,repairScenes=6,arrowScenes=24,total=repairScenes+arrowScenes+3;String name;
        draw.clearSelection();draw.constructionRenderer().preview(false,1,60,40,1,true,true);camera.resize(Core.graphics.getWidth(),Core.graphics.getHeight());camera.near=.04f;camera.far=400;camera.fov=55;camera.up.set(0,1,0);
        if(stage<repairScenes){
            activate(stage);var b=sources.get(stage);var target=targets.get(stage);float base=(float)(stage/2==0?0:stage/2==1?1.4:4.2),x=b.x/8,y=b.y/8;
            if(tick==24){b.target=null;b.efficiency=0;}
            camera.position.set(x+5,base+8,-y+9);camera.lookAt(x+2,base+2,-y);name=b.block.name+"-z"+(stage/2==0?0:stage/2==1?1:3)+(tick<24?"-active":"-stopped");
            camera.update();draw.render(camera);check(target.health==hp&&b.strength==strength,"renderer does not mutate healing HP/strength");
            if(tick<24){var beam=draw.supportEffects().healingBeam(b);check(beam!=null&&draw.supportEffects().wasHealingSubmitted(b),"real beam submitted at loaded -1 counter and upper floors");var pose=mod.unitAltitudes.unitState(target);check(Math.abs(beam.bz()-pose.z())<1e-6,"actual target XYZ endpoint");
                if(tick==15){var mid=camera.project(new Vec3((float)(beam.ax()*.6+beam.bx()*.4),(float)(beam.az()*.6+beam.bz()*.4),(float)-(beam.ay()*.6+beam.by()*.4)),0,0,Core.graphics.getWidth(),Core.graphics.getHeight());check(pixels(mid.x,mid.y,0)>0,"actual green beam pixels outside healing head");ScreenUtils.saveScreenshot(out.child(name+".png"));}}
            else{check(draw.supportEffects().healingBeam(b)==null,"beam stops immediately with native target/power stop");if(tick==40)ScreenUtils.saveScreenshot(out.child(name+".png"));}
        }else{
            for(var b:sources){b.efficiency=0;b.target=null;}
            int i=stage-repairScenes,floor=i<arrowScenes?new int[]{0,2,5}[i/8]:0,rotation=i%4;boolean valid=i%8<4;var block=i<arrowScenes?Blocks.conveyor:i==arrowScenes?Blocks.payloadConveyor:i==arrowScenes+1?Blocks.copperWall:Blocks.conveyor;
            double base=floor*1.4;boolean queued=i==arrowScenes+2;name="arrow-z"+floor+"-r"+rotation+"-"+(valid?"valid":"invalid")+(i<arrowScenes?"":"-"+block.name+(queued?"-queued":""));
            camera.up.set(0,0,-1);camera.position.set(60+(block.size-1)*.5f,(float)base+8,-40-(block.size-1)*.5f+.01f);camera.lookAt(60+(block.size-1)*.5f,(float)base,-40-(block.size-1)*.5f);
            Vars.player.unit().plans.clear();
            if(queued)Vars.player.unit().plans.addLast(new mindustry.entities.units.BuildPlan(60,40,rotation,block));
            else if(floor==0)draw.setSelection(60,40,block,rotation,valid);
            else draw.constructionRenderer().preview(true,floor,60,40,block.size,true,valid,block,rotation,false);
            camera.update();draw.render(camera);int count=floor==0?arrows(draw):arrows(draw.constructionRenderer());check(count==(block.rotate?1:0),"one directional arrow per real preview; static walls suppressed");
            if(tick==15){ScreenUtils.saveScreenshot(out.child(name+".png"));if(block.rotate){var p=camera.project(new Vec3(60+block.offset/8,(float)base+FpvWorldRenderer.heightFor(block)+.12f,-40-block.offset/8),0,0,Core.graphics.getWidth(),Core.graphics.getHeight());check(pixels(p.x,p.y,valid?1:2)>0,"visible direction marker pixels");}}
            Vars.player.unit().plans.clear();
        }
        check(Gl.getError()==0,"OpenGL error in "+name);
        if(++frame==total*48){done=true;out.child("result.json").writeString("{\"status\":\"PASS\",\"java\":\""+System.getProperty("java.version")+"\",\"scenes\":"+total+",\"frames\":"+frame+",\"checks\":"+checks+",\"glErrors\":0,\"physicalAcceptance\":false}");out.child("input-api.txt").writeString(input+"\n"+motion);draw.dispose();Core.app.post(Core.app::exit);}
    }catch(Throwable t){fail(t);}}
    int pixels(float x,float y,int color){int w=Core.graphics.getWidth(),h=Core.graphics.getHeight();byte[] p=ScreenUtils.getFrameBufferPixels(0,0,w,h,false);int count=0;for(int yy=Math.max(0,(int)y-18);yy<Math.min(h,(int)y+19);yy++)for(int xx=Math.max(0,(int)x-18);xx<Math.min(w,(int)x+19);xx++){int at=(yy*w+xx)*4,r=p[at]&255,g=p[at+1]&255,b=p[at+2]&255;if(color==0?g>120&&g>r*1.25&&g>b*1.05:color==1?g>160&&b>170&&g>r*1.2:r>160&&r>g*1.4&&r>b*1.2)count++;}return count;}
    void fail(Throwable t){done=true;out.child("failed.txt").writeString(t.toString());Log.err(t);Core.app.post(Core.app::exit);}
}
