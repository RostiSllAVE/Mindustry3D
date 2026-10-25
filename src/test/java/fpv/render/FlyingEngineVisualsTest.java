package fpv.render;

import arc.graphics.g3d.Camera3D;import arc.util.*;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;
public final class FlyingEngineVisualsTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static class Sink implements CombatVisualRenderer.Sink {
        java.util.ArrayList<float[]> values=new java.util.ArrayList<>();public void beam(float a,float b,float c,float d,float e,float f,float w,float r,float g,float blue){throw new AssertionError("Exhaust must use closed volumetric beam path");}
        public void laser(float a,float b,float c,float d,float e,float f,float w,float r,float g,float blue){values.add(new float[]{a,b,c,d,e,f,w,r,g,blue});}public void marker(float x,float y,float z,float r,float a,float b,float c){}
    }
    public static void main(String[] args){
        ArcNativesLoader.load();fpv.units.NavalFactoryNativeTest.main(args);var visual=new FlyingEngineVisuals();var camera=new Camera3D();camera.resize(640,480);camera.near=.04f;camera.far=2000;camera.position.set(0,5,15);camera.lookAt(0,5,0);camera.update();
        for(var type:new mindustry.type.UnitType[]{UnitTypes.flare,UnitTypes.zenith,UnitTypes.antumbra,UnitTypes.mono,UnitTypes.oct,UnitTypes.quell,UnitTypes.disrupt}){
            Unit u=type.create(Team.sharded);u.elevation=1;u.add();float health=u.health,x=u.x,y=u.y;int actors=Groups.all.size();
            var sink=new Sink();visual.begin();Time.time=100;check(visual.render(u,0,5,0,0,0,0,.2f,3,camera,sink),"native aircraft exhaust "+type.name);
            check(visual.stats().engines()==Math.max(1,type.engines.size),"native engine count");
            for(var v:sink.values){for(float f:v)check(Float.isFinite(f),"finite jet");check(v[6]>0,"positive thickness");}
            var first=sink.values.get(0);if(type==UnitTypes.flare)check(first[3]<first[0],"default exhaust points rearward");
            var animated=new Sink();Time.time=103;visual.render(u,0,5,0,0,0,0,.2f,3,camera,animated);check(!java.util.Arrays.equals(first,animated.values.get(0)),"deterministic time animation");
            var rotated=new Sink();visual.render(u,0,5,0,0,75,120,.2f,3,camera,rotated);check(!rotated.values.isEmpty(),"bank/pitch body-local hardpoints");
            check(health==u.health&&x==u.x&&y==u.y&&actors==Groups.all.size(),"no native gameplay mutation");
            u.dead=true;check(!visual.render(u,0,5,0,0,0,0,.2f,3,camera,new Sink()),"dead unit immediately hidden");u.remove();
        }
        Unit ground=UnitTypes.vela.create(Team.sharded);ground.add();check(!visual.render(ground,0,5,0,0,0,0,.2f,3,camera,new Sink()),"boosting mechs keep existing effect");ground.remove();
        System.out.println("FlyingEngineVisuals: "+checks+" native hardpoint/colors/volume/orientation/animation/read-only/lifecycle checks PASS");
    }
}
