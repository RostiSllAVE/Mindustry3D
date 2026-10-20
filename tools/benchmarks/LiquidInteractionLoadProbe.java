package fpv.input;
import fpv.environment.*;import fpv.world.*;import mindustry.*;import mindustry.content.*;import mindustry.world.*;import mindustry.game.Team;import java.util.*;
/** CPU-only new-mechanic cost, not FPS, network or native status-tick time. */
public final class LiquidInteractionLoadProbe{
 public static void main(String[] args){ConstructionPriorityNativeTest.init();try{
  Vars.world.tiles=new Tiles(128,128);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(int y=40;y<88;y++)for(int x=40;x<88;x++)Vars.world.tile(x,y).setFloor(Blocks.slag.asFloor());
  for(int count:new int[]{100,1000,4096}){mindustry.gen.Groups.unit.clear();for(int i=0;i<count;i++){var u=UnitTypes.flare.create(Team.sharded);u.set((i*37%128)*8,(i*53%128)*8);u.add();}
   var env=new LiquidExposureSystem(u->new Unit3DState(u.id,u.type.id,u.team.id,u.x/8d,u.y/8d,12,0,0,0,0,0,0,12,.3,.15,true));long begin=System.nanoTime();env.ensure();double index=(System.nanoTime()-begin)/1e6;double[] ms=new double[7];for(int i=-3;i<7;i++){begin=System.nanoTime();env.update(.11);if(i>=0)ms[i]=(System.nanoTime()-begin)/1e6;}Arrays.sort(ms);
   System.out.printf(Locale.ROOT,"EXPOSURE,units=%d,indexMs=%.3f,updateMedianMs=%.3f,minMs=%.3f,maxMs=%.3f,frequencyHz=10%n",count,index,ms[3],ms[0],ms[6]);}
 }finally{ConstructionPriorityNativeTest.input.uninstallProjection();ConstructionPriorityNativeTest.renderer.close();ConstructionPriorityNativeTest.runtime.close();ConstructionPriorityNativeTest.altitude.close();}}
}
