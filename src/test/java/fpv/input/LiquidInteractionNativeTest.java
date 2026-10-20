package fpv.input;
import fpv.environment.*;import fpv.render.*;import fpv.world.*;import fpv.physics.*;import fpv.flight.*;import mindustry.*;import mindustry.content.*;import mindustry.game.Team;import mindustry.world.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
/** Native status/authority and underwater flight fixtures; physical acceptance remains separate. */
public final class LiquidInteractionNativeTest {
 static int n;static void check(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  init();try{
   Vars.world.tiles=new Tiles(64,64);Vars.world.tiles.fill();Vars.world.setGenerating(true);Vars.world.tile(12,12).setFloor(Blocks.water.asFloor());
   for(int y=30;y<38;y++)for(int x=30;x<38;x++)Vars.world.tile(x,y).setFloor(Blocks.slag.asFloor());
   var u=UnitTypes.flare.create(Team.sharded);u.set(96,96);u.add();Vars.player.unit(u);double[] z={.1};
   var environment=new LiquidExposureSystem(v->new Unit3DState(v.id,v.type.id,v.team.id,v.x/8d,v.y/8d,z[0],0,0,0,0,0,0,z[0],.2,.15,true));
   check(environment.area(Vars.world.tile(12,12))==1,"isolated water pool");check(environment.area(Vars.world.tile(33,33))==64,"connected lava lake");
   check(LiquidExposureSystem.burnRadius(64)>LiquidExposureSystem.burnRadius(1),"larger lake emits farther");
   var out=new LiquidExposureSystem.Sample();environment.sample(33,33,10,.2,.15,out);check(out.burning>0&&out.contact==0,"tall vertical hot plume, no distant melting");
   environment.sample(33,33,50,.2,.15,out);check(out.burning==0,"bounded plume");
   environment.update(.11);check(u.hasEffect(StatusEffects.wet),"real native wet status near water");u.clearStatuses();u.statusBits().clear();z[0]=8;environment.update(.11);check(!u.hasEffect(StatusEffects.wet),"no wet status high above water");
   u.set(33*8,33*8);z[0]=8;environment.update(.11);check(u.hasEffect(StatusEffects.burning)&&!u.hasEffect(StatusEffects.melting),"burning above lava before melting range");
   z[0]=.1;environment.update(.11);check(u.hasEffect(StatusEffects.melting),"native melting close to lava");
   check(u.isPlayer(),"real player unit controller");u.set(12*8,12*8);check(LiquidBody.flightFloor(u,12,-12,0)<-.5,"player flyer can dive to actual bed");
   var f=new FlightController(fpv.generated.FlightProfiles.create());f.reset(12,-.3,-12,0);var state=f.state();new SweptCollision((x,y)->LiquidBody.flightFloor(u,x,y,0),.1,.1,.1).resolve(state,12,-.2,-12);check(state.y<-.16,"submerged flight is not clamped to old Z0");
   var ground=UnitTypes.dagger.create(Team.sharded);check(LiquidBody.flightFloor(ground,12,-12,0)==0,"ground support unchanged");
   check(LiquidBody.bed(12.4,-12)>LiquidBody.bed(12,-12),"flight bed follows rising shore, not a flat hole");
   check(LiquidBody.depth(Blocks.deepwater.asFloor())>LiquidBody.depth(Blocks.sandWater.asFloor()),"deep and shallow beds differ");
   var idle=UnitTypes.flare.create(Team.sharded);idle.set(33*8,33*8);idle.add();check(LiquidExposureSystem.idle(idle),"free native FlyingAI recognized");
   check(environment.clearance(idle,12,12,.2,0,0,.2)>0,"idle AI also clears native water/contact-effect volume");
   check(environment.clearance(idle,33,33,2,0,0,.2)>10,"idle AI climbs over hot volume");idle.isShooting=true;check(environment.clearance(idle,33,33,2,0,0,.2)==0,"attacking AI keeps normal approach");idle.isShooting=false;altitude.setLiquidExposure(environment);double initial=altitude.unitState(idle).z();altitude.update(.1);float savedX=idle.x;idle.x+=1.6f;idle.vel.set(.8f,0);altitude.update(.1);check(Math.abs(idle.x-savedX)<.001,"idle aircraft waits instead of moving into hot volume before climbing");for(int step=0;step<60;step++)altitude.update(.1);check(altitude.unitState(idle).z()>initial+1,"native altitude solver actually climbs clear of hot lake");idle.mineTile=Vars.world.tile(12,12);check(!LiquidExposureSystem.idle(idle),"working miner excluded");
   check(LiquidBody.kind(Blocks.tar.asFloor())==2&&LiquidBody.kind(Blocks.slag.asFloor())==3&&LiquidBody.kind(Blocks.cryofluid.asFloor())==4,"separate material effects");
   for(int i=0;i<100;i++){double a=LiquidBody.hash(i,-i);check(a>=0&&a<=1&&a==LiquidBody.hash(i,-i),"stable variation");}
   u.clearStatuses();u.statusBits().clear();var net=Vars.net;Vars.net=new mindustry.net.Net(null){@Override public boolean client(){return true;}};z[0]=.1;environment.update(.11);check(!u.hasEffect(StatusEffects.wet),"client cannot apply authoritative environmental status");Vars.net=net;
   System.out.println("LiquidInteractionNativeTest: "+n+" native status/area/plume/player dive/idle AI checks PASS; physical multiplayer/gameplay NOT TESTED");
  }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
 }
}
