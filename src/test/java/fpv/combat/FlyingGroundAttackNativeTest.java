package fpv.combat;

import arc.*;
import arc.math.Mathf;
import arc.mock.*;
import arc.util.*;
import fpv.construction.*;
import fpv.terrain.TerrainElevation;
import fpv.units.UnitAltitudeSystem;
import fpv.world.*;
import java.util.*;
import mindustry.Vars;
import mindustry.ai.types.FlyingAI;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.UnitController;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.Net;
import mindustry.type.UnitType;
import mindustry.world.*;

/** Native AI/weapons/projectiles/damage with explicit XY integration, not a physical gameplay test. */
public final class FlyingGroundAttackNativeTest {
    private static int checks,serial=20000;
    private static UnitAltitudeSystem alt;
    private static World3DAdapter world;
    private static Combat3DSystem combat;
    private static final boolean[] enabled={false};
    private static Unit controlled;
    private static WorldCoordinates.Point sight;
    private static final IdentityHashMap<Unit,Unit3DState> external=new IdentityHashMap<>();
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static final class ObjectiveAI extends FlyingAI {
        boolean fire=true,move=true;
        void seed(Building b){target=b;for(var m:unit.mounts)m.target=b;}
        @Override public boolean retarget(){return false;}
        @Override public boolean shouldFire(){return fire;}
        @Override public void updateMovement(){if(move)super.updateMovement();}
    }
    private record Result(int firstDamage,int shots,float damage,int allowed,int intent){}
    public static void main(String[] args){
        boolean measure=args.length==1&&"--measure".equals(args[0]);
        init();
        try{
            for(boolean xyz:new boolean[]{false,true})for(Block block:new Block[]{Blocks.copperWall,Blocks.duo,Blocks.graphitePress,Blocks.coreShard}){
                clear(xyz);Building b=block(48,48,block,Team.sharded);Unit u=flyer(UnitTypes.flare,30,48);var ai=new ObjectiveAI();u.controller(ai);ai.seed(b);
                var nativeBullet=combat.originalType(u.mounts[0].weapon.bullet);float damage=nativeBullet.damage,speed=nativeBullet.speed,life=nativeBullet.lifetime,reload=u.mounts[0].weapon.reload,cone=u.mounts[0].weapon.shootCone,minimum=u.mounts[0].weapon.minShootVelocity;
                Result r=attack(u,b,2400);
                System.out.printf(Locale.ROOT,"A207_RESULT,%s,%s,%d,%d,%.3f,%d,%d%n",xyz?"3D":"2D",block.name,r.firstDamage,r.shots,r.damage,r.allowed,r.intent);
                if(!measure){
                    check(r.firstDamage>=0&&r.damage>0,"real Flare projectiles damage "+block.name+" xyz="+xyz+": "+r);
                    check(r.shots>=6,"repeated attack runs keep firing instead of sticking after first burst");
                }
                check(u.controller()==ai&&u.type==UnitTypes.flare,"native controller/classification preserved");
                check(nativeBullet.keepVelocity&&nativeBullet.damage==damage&&nativeBullet.speed==speed&&nativeBullet.lifetime==life&&u.mounts[0].weapon.reload==reload&&u.mounts[0].weapon.shootCone==cone&&u.mounts[0].weapon.minShootVelocity==minimum,"all original ballistics/rates/cones/velocity gates unchanged");
            }
            if(measure){System.out.println("A207 measurements only; not an acceptance-test PASS");return;}
            acquisitionAndSwitch();occlusion();verticalAndPlayer();upperTarget();permissionsAndOtherAircraft();authority();
            System.out.println("FlyingGroundAttackNativeTest: "+checks+" checks PASS; native AI/weapon/projectile/HP fixtures; physical gameplay and two-computer NOT TESTED.");
        }finally{combat.close();alt.close();}
    }
    private static void init(){
        Core.app=new MockApplication();Core.files=new MockFiles();Core.settings=new MockSettings();Core.bundle=I18NBundle.createEmptyBundle();Core.audio=new MockAudio();Core.graphics=new MockGraphics();
        Vars.headless=true;Vars.tree=new FileTree();Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.state=new GameState();Vars.net=new Net(null);Groups.init();Vars.world=new World();Vars.world.tiles=new Tiles(96,96);Vars.world.setGenerating(true);Vars.world.tiles.fill();Vars.indexer=new mindustry.ai.BlockIndexer();Groups.resize(0,0,768,768);Time.clear();Time.setDeltaProvider(()->1);Vars.content.init();Vars.state.rules.unitCap=1000;Vars.state.rules.waves=false;Vars.state.set(GameState.State.playing);
        alt=new UnitAltitudeSystem((x,y)->0);alt.setExternalStateProvider(external::get);
        world=new World3DAdapter(new TerrainElevation(),(x,y)->{var t=Vars.world.tile((int)Math.round(x),(int)Math.round(-y));return t!=null&&t.build!=null?fpv.net.HeadlessGeometry.heightFor(t.block()):0;},alt);
        combat=new Combat3DSystem(world,()->enabled[0],u->u==controlled?sight:null);combat.install();
    }
    private static void clear(boolean xyz){
        controlled=null;sight=null;external.clear();combat.clear();alt.clear();world.setConstruction(null);combat.setProjectedDefense(null);combat.setUpperBuildings(List::of);
        for(Unit u:Groups.unit.copy())u.remove();
        for(Bullet b:Groups.bullet.copy()){b.hit=true;b.remove();}
        Vars.world.setGenerating(true);for(Tile t:Vars.world.tiles)if(t.block()!=Blocks.air)t.setBlock(Blocks.air);
        Time.clear();Time.time=0;Mathf.rand.setSeed(2707);enabled[0]=xyz;combat.syncActive();
    }
    private static Building block(int x,int y,Block block,Team team){Vars.world.tile(x,y).setBlock(block,team,0);return Vars.world.tile(x,y).build;}
    private static Unit flyer(UnitType type,double x,double y){Unit u=type.create(Team.crux);u.id=serial++;u.set((float)(x*8),(float)(y*8));u.rotation=0;u.add();return u;}
    private static void tick(Unit u,boolean move){
        // Mirror HitboxComp's previous-frame displacement. Frozen-origin ballistic fixtures
        // provide their explicit native velocity; normal attack runs use real displacement.
        u.deltaX=move?u.x-u.lastX:u.vel.x;u.deltaY=move?u.y-u.lastY:u.vel.y;u.lastX=u.x;u.lastY=u.y;
        Time.delta=1;Time.update();Vars.state.updateId++;u.controller().updateUnit();for(var m:u.mounts)m.weapon.update(u,m);
        if(move){u.set(u.x+u.vel.x,u.y+u.vel.y);u.vel.scl(1-u.type.drag);}
        alt.update(1d/60);combat.update(1d/60);for(Bullet b:Groups.bullet.copy())if(b.isAdded())b.update();
    }
    private static Result attack(Unit u,Building target,int ticks){
        float hp=target.health;int first=-1,intent=0,allowed=0;
        for(int i=0;i<ticks&&!target.dead;i++){
            tick(u,true);if(target.health<hp&&first<0)first=i;
            if(u.mounts[0].shoot)intent++;
            if(enabled[0]){var s=combat.aircraftWeapons().visual(u,u.mounts[0]);if(s!=null&&s.allowed())allowed++;}
        }
        return new Result(first,u.mounts[0].totalShots,hp-target.health,allowed,intent);
    }
    private static void acquisitionAndSwitch(){
        clear(true);Building first=block(45,48,Blocks.solarPanel,Team.sharded),second=block(51,48,Blocks.duo,Team.sharded);
        block(65,48,Blocks.coreShard,Team.sharded); // native long-range objective keeps attack runs active
        arc.Events.fire(new EventType.WorldLoadEvent());
        Vars.world.setGenerating(false);
        Unit u=flyer(UnitTypes.flare,36,48);u.controller(new FlyingAI());
        float hp1=first.health,hp2=second.health;boolean acquired=false,switched=false;
        for(int i=0;i<5000&&!second.dead;i++){
            tick(u,true);var target=u.mounts[0].target;
            acquired|=target==first;switched|=!first.isValid()&&target==second;
            if(acquired&&first.health<hp1&&first.isValid())first.tile.setBlock(Blocks.air); // remove a genuinely hit target
        }
        check(acquired&&first.health<hp1,"unseeded original FlyingAI acquires and damages a native ground building");
        check(!first.isValid()&&switched&&second.health<hp2,"original AI switches to another valid structure after removal: first="+first.health+", second="+second.health+", target="+u.mounts[0].target+", xy="+u.x/8+","+u.y/8);
    }
    private static void fixedPose(Unit u,double z,double yaw,double vx,double vy,double vz){external.put(u,new Unit3DState(u.id,u.type.id,u.team.id,u.x/8,u.y/8,z,vx,vy,vz,yaw,0,0,z,u.hitSize/16,.2,true));}
    private static ObjectiveAI fixedAttacker(Unit u,Building target){var ai=new ObjectiveAI();u.controller(ai);ai.seed(target);u.vel.set(2.2f,0);return ai;}
    private static void occlusion(){
        clear(true);Building target=block(48,48,Blocks.duo,Team.sharded);Unit u=flyer(UnitTypes.flare,40,48);fixedAttacker(u,target);fixedPose(u,3,0,16.5,0,0);
        var grid=new LayerWorld(LayerCoordinates.DEFAULT);
        for(int y=35;y<=60;y++)grid.add(new LayerComponent(y,LayerComponent.Kind.BLOCK,44,y,1,"copper-wall",1,0,Team.sharded.id,LayerComponent.Mode.FREE,100000,100000,1,8,LayerComponent.SupportProfile.NONE,null,null));
        double[] absorbed={0};var physical=new LayerPhysicalQueries(()->grid,(id,damage)->absorbed[0]+=damage);world.setConstruction(physical);combat.setProjectedDefense(physical);
        float hp=target.health;for(int i=0;i<240;i++)tick(u,false);
        check(u.mounts[0].totalShots>0&&absorbed[0]>0&&target.health==hp,"real projectile hits foreground solid upper wall; no through-wall target damage");
        grid.clear();for(int i=0;i<240&&!target.dead;i++)tick(u,false);
        check(target.health<hp,"removing the obstacle restores actual projectile contact");
    }
    private static void verticalAndPlayer(){
        for(double z:new double[]{2,4}){
            clear(true);Building target=block(48,48,Blocks.duo,Team.sharded);Unit u=flyer(UnitTypes.flare,39,48);fixedAttacker(u,target);fixedPose(u,z,0,16.5,0,.5);
            float hp=target.health;for(int i=0;i<300&&!target.dead;i++)tick(u,false);
            check(target.health<hp,"AI aircraft compensates inherited XYZ velocity at height "+z);
            var shot=combat.projectileState(u.mounts[0].bullet);var s=combat.aircraftWeapons().visual(u,u.mounts[0]);
            check(s!=null&&Math.abs(s.pitch())<=combat.aircraftWeapons().profile(u,u.mounts[0]).pitchLimit(),"pitch limits retained");
        }
        clear(true);Building highTarget=block(48,48,Blocks.duo,Team.sharded);Unit descending=flyer(UnitTypes.flare,34,48);var descendingAI=new ObjectiveAI();descending.controller(descendingAI);descendingAI.seed(highTarget);alt.seedAirFromBoost(descending,6,0);
        Result approach=attack(descending,highTarget,2400);check(approach.damage>0,"existing altitude descent obtains a legal attack solution from a high initial pose");
        clear(true);Building target=block(48,48,Blocks.duo,Team.sharded);Unit u=flyer(UnitTypes.flare,43,48);fixedAttacker(u,target);fixedPose(u,30,0,16.5,0,0);
        float hp=target.health;for(int i=0;i<120;i++)tick(u,false);
        check(u.mounts[0].totalShots==0&&target.health==hp&&!combat.aircraftWeapons().visual(u,u.mounts[0]).allowed(),"unreachable near-vertical target cannot bypass mechanical pitch stop");
        clear(true);target=block(48,48,Blocks.copperWall,Team.sharded);u=flyer(UnitTypes.flare,40,48);fixedPose(u,3,0,0,0,0);u.vel.set(2.2f,0);
        // Explicit player-like controller: manual aim must not receive AI motion compensation.
        u.controller(new UnitController(){Unit owner;public void unit(Unit value){owner=value;}public Unit unit(){return owner;}});controlled=u;sight=new WorldCoordinates.Point(1,0,-.2);
        hp=target.health;for(int i=0;i<180&&!target.dead;i++){u.mounts[0].shoot=true;u.mounts[0].aimX=target.x;u.mounts[0].aimY=target.y;tick(u,false);}
        check(target.health<hp,"manually supplied FPV aim still causes actual building damage");
        var s=combat.aircraftWeapons().visual(u,u.mounts[0]);double expected=Math.toDegrees(Math.atan2(sight.z(),Math.hypot(sight.x(),sight.y())));
        check(Math.abs(s.pitch()-expected)<1e-6,"manual FPV direction remains unchanged");
    }
    private static void upperTarget(){
        clear(true);var grid=new LayerWorld(LayerCoordinates.DEFAULT);
        var adapter=new LayerBuildingAdapter(new LayerBuildingAdapter.Events(){@Override public void damaged(long id,float health){var c=grid.get(id);if(c!=null)grid.replace(c.withHealth(health));}});
        var c=new LayerComponent(900,LayerComponent.Kind.BLOCK,48,48,3,"duo",1,0,Team.sharded.id,LayerComponent.Mode.FREE,250,250,1,1,LayerComponent.SupportProfile.NONE,null,null);grid.add(c);adapter.create(900,48,48,3,Blocks.duo,Team.sharded,0);
        var physical=new LayerPhysicalQueries(()->grid,(id,damage)->{var old=grid.get(id);if(old!=null)grid.replace(old.withHealth(Math.max(1,old.health()-damage)));},adapter::building);
        world.setConstruction(physical);combat.setProjectedDefense(physical);combat.setUpperBuildings(adapter::entries);
        Unit u=flyer(UnitTypes.flare,40,48);Building target=adapter.building(900);fixedAttacker(u,target);fixedPose(u,7,0,16.5,0,0);
        try{
            for(int i=0;i<240;i++)tick(u,false);
            check(grid.get(900).health()<250,"native Flare shots hit the real elevated building volume");
            check(Vars.world.tile(48,48).build==null,"upper hit does not substitute a Z0 building");
        }finally{adapter.clear();}
    }
    private static void permissionsAndOtherAircraft(){
        clear(true);Building target=block(48,48,Blocks.duo,Team.sharded);Unit u=flyer(UnitTypes.flare,40,48);var ai=fixedAttacker(u,target);fixedPose(u,3,0,16.5,0,0);float hp=target.health;
        ai.fire=false;for(int i=0;i<120;i++)tick(u,false);check(u.mounts[0].totalShots==0&&target.health==hp,"native hold fire preserved");ai.fire=true;
        // Stock 160.7 no longer exposes Unit.ammo/Rules.unitAmmo; do not invent an ammo system.
        ai.move=false;u.vel.setZero();for(int i=0;i<120;i++)tick(u,false);check(u.mounts[0].totalShots==0&&target.health==hp,"native minimum firing velocity still gates a stationary Flare");ai.move=true;u.vel.set(2.2f,0);
        ai.seed(block(48,48,Blocks.duo,Team.crux));for(int i=0;i<120;i++)tick(u,false);check(u.mounts[0].totalShots==0,"friendly structure cannot become attack target");
        for(UnitType type:new UnitType[]{UnitTypes.zenith,UnitTypes.antumbra,UnitTypes.eclipse}){
            clear(true);target=block(48,48,Blocks.coreShard,Team.sharded);u=flyer(type,38,48);ai=new ObjectiveAI();u.controller(ai);ai.seed(target);Result result=attack(u,target,2400);
            check(result.damage>0&&result.shots>0,"other native aircraft attacks ground core: "+type.name+" "+result);
        }
    }
    private static void authority(){
        clear(true);Building target=block(48,48,Blocks.duo,Team.sharded);Unit u=flyer(UnitTypes.flare,40,48);fixedAttacker(u,target);fixedPose(u,3,0,16.5,0,0);
        Net original=Vars.net;var oldServer=Vars.netServer;boolean[] server={false};combat.setAuthority(()->server[0]);float hp=target.health;
        try{
            Vars.net=new Net(null){@Override public boolean client(){return !server[0];}@Override public boolean server(){return server[0];}};
            Vars.netServer=new NetServer();
            for(int i=0;i<120;i++)tick(u,false);check(target.health==hp,"non-authoritative client cannot apply projectile damage");combat.clear();u.mounts[0].reload=0;server[0]=true;
            for(int i=0;i<240&&!target.dead;i++)tick(u,false);check(target.health<hp,"authoritative local server fixture applies real projectile damage");
        }finally{Vars.net=original;Vars.netServer=oldServer;combat.setAuthority(()->true);}
    }
}
