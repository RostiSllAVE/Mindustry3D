package fpv.input;
import fpv.render.CosmicFloorGeometry;
import mindustry.*;import mindustry.content.*;import mindustry.graphics.CacheLayer;
import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

public final class CosmicFloorNativeTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{init();try{
  check(Blocks.space.asFloor().cacheLayer==CacheLayer.space,"native designated space cache");
  check(Blocks.space.solid&&!Blocks.space.placeableOn&&Blocks.empty.solid&&!Blocks.empty.placeableOn,"original physical restrictions");
  int floors=0;for(var f:Vars.content.blocks())if(f instanceof mindustry.world.blocks.environment.Floor){
   var t=Vars.world.tile(40,40);t.setFloor(f.asFloor());
   check(CosmicFloorGeometry.designated(t)==(f==Blocks.space||f==Blocks.empty),"exact whitelist "+f.name);floors++;
  }
  var mask=(java.util.function.IntBinaryOperator)(x,y)->(x==3&&y==2||x>=10&&y>=4)?1:0;
  float[] a=CosmicFloorGeometry.build(0,0,16,16,mask),b=CosmicFloorGeometry.build(0,0,16,16,mask);
  check(Arrays.equals(a,b),"determinism");check(a.length==13*18,"one isolated tile plus row runs");
  for(int n=0;n<a.length;n+=18){float l=a[n],r=a[n+3],z=a[n+2];int y=Math.round(-z-.5f);
   for(int x=Math.round(l+.5f);x<r+.5f;x++)check(mask.applyAsInt(x,y)==1,"no solid bleeding");
   check(a[n+1]==.002f,"visual overlay only, no simulation changes");
  }
  check(CosmicFloorGeometry.build(0,0,32,32,(x,y)->0).length==0,"no mesh for empty mask");
  check(CosmicFloorGeometry.build(0,0,32,32,(x,y)->1).length==32*18,"large region batched by row");
  var left=CosmicFloorGeometry.build(0,0,32,32,(x,y)->1);var right=CosmicFloorGeometry.build(32,0,32,32,(x,y)->1);
  check(left[3]==right[0],"chunk edge exact");
  check(!CosmicFloorGeometry.designated(null),"missing tiles safe");
  System.out.println("CosmicFloorNativeTest PASS: "+floors+" native floors, exact boundaries, row batching, determinism, chunk seams; no physical acceptance");
 }finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}}
}
