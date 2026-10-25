package fpv.input;

import arc.graphics.g3d.Camera3D;
import arc.util.Time;
import fpv.render.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.blocks.units.RepairTurret;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Native healing, legal -1 post-load counter, cache lifetime and authoritative visual replay. */
public final class RepairBeamPreviewNativeTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static class Sink implements SupportEffectsRenderer.Sink {
        int lasers;public void laser(float a,float b,float c,float d,float e,float f,float w,float r,float g,float blue){lasers++;check(w>0&&Float.isFinite(a+b+c+d+e+f+w),"finite volumetric healing span");}
        public void beam(float a,float b,float c,float d,float e,float f,float w,float r,float g,float blue){}
        public void marker(float a,float b,float c,float d,float r,float g,float blue){}
        public void disk(float a,float b,float c,float d,float e,float r,float g,float blue){}
    }
    public static void main(String[] args){
        init();var effects=new SupportEffectsRenderer();var camera=new Camera3D();camera.resize(800,600);camera.position.set(26,15,-20);camera.lookAt(24,2,-24);camera.near=.03f;camera.far=200;camera.update();
        try{
            for(var block:new mindustry.world.Block[]{Blocks.repairPoint,Blocks.repairTurret}){
                var tile=Vars.world.tile(24,24);tile.setBlock(block,mindustry.game.Team.sharded,0);var build=(RepairTurret.RepairPointBuild)tile.build;
                Unit target=UnitTypes.zenith.create(build.team);target.set(build.x+16,build.y);target.add();target.health=50;altitude.initializePayloadRelease(target,4);build.target=target;build.enabled=true;build.efficiency=build.potentialEfficiency=1;build.rotation=0;build.timer.reset(((RepairTurret)block).timerTarget,0);
                float hp=target.health;Time.delta=1;for(int i=0;i<12;i++)build.updateTile();check(target.health>hp&&build.strength>.1,"real native HP production before rendering");
                Vars.world.tileChanges=-1;effects.clear();int[] scans={0};Iterable<Building> source=()->{scans[0]++;return java.util.List.<Building>of(build).iterator();};
                var sink=new Sink();float healed=target.health;effects.render(camera,source,altitude,(x,z)->0,sink);
                check(sink.lasers>0&&effects.wasHealingSubmitted(build),"active source discovered even with legal -1 counter");check(scans[0]==1,"first source cache built once");
                var beam=effects.healingBeam(build);check(beam.az()>fpv.net.HeadlessGeometry.heightFor(block)+.09,"emitter is above volumetric healing head");check(target.health==healed,"effect pass never heals twice");
                effects.render(camera,source,altitude,(x,z)->0,sink);check(scans[0]==1,"unchanged frame reuses cache");
                effects.clear();effects.render(camera,source,altitude,(x,z)->0,sink);check(scans[0]==2&&effects.wasHealingSubmitted(build),"F5/world presentation reset rebuilds cache at same counter");
                build.efficiency=0;effects.render(camera,source,altitude,(x,z)->0,sink);check(!effects.wasHealingSubmitted(build),"power/operation stop removes beam immediately");
                build.efficiency=1;build.target=null;effects.render(camera,source,altitude,(x,z)->0,sink);check(!effects.wasHealingSubmitted(build),"missing target removes beam");build.target=target;
                var state=new SupportEffectsState();state.applySnapshot(java.util.List.of(new SupportEffectsState.Effect(SupportEffectsState.TURRET,build.pos(),block.id,build.team.id,SupportEffectsState.UNIT_TARGET,target.id,target.type.id,-1,.6f,0,0)));
                var net=Vars.net;Vars.net=new mindustry.net.Net(null){@Override public boolean client(){return true;}};
                try{effects.setState(state);effects.render(camera,source,altitude,(x,z)->0,sink);check(effects.wasHealingSubmitted(build),"client draws existing authoritative support snapshot");state.applySnapshot(java.util.List.of());effects.render(camera,source,altitude,(x,z)->0,sink);check(!effects.wasHealingSubmitted(build),"empty server snapshot retires remote beam");}
                finally{Vars.net=net;effects.setState(null);}
                target.remove();tile.remove();
            }
            System.out.println("RepairBeamPreviewNative: "+checks+" HP/cache sentinel/reset/emitter/stop/remote/read-only checks PASS");
        }finally{effects.clear();input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
}
