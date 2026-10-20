package fpv.environment;
import mindustry.Vars;import mindustry.content.*;import mindustry.gen.Unit;import mindustry.world.*;import mindustry.world.blocks.environment.Floor;
/** Shared CPU material/depth definitions. Native tile coordinates/buildings never move. */
public final class LiquidBody {
 public static final double MAX_DEPTH=1.8;
 private LiquidBody(){}
 public static int kind(Floor f){if(f==null||!f.isLiquid)return 0;if(f.liquidDrop==Liquids.water)return 1;if(f.liquidDrop==Liquids.oil)return 2;if(f.liquidDrop==Liquids.slag)return 3;if(f.liquidDrop==Liquids.cryofluid)return 4;return 5;}
 public static boolean open(Tile t){return t!=null&&t.floor().isLiquid&&t.block()==Blocks.air;}
 public static double depth(Floor f){return f.isDeep()?MAX_DEPTH:f==Blocks.sandWater||f==Blocks.darksandWater||f==Blocks.darksandTaintedWater?.55:.95;}
 public static double bed(double x,double z){int tx=(int)Math.floor(x+.5),ty=(int)Math.floor(-z+.5);Tile t=Vars.world.tile(tx,ty);if(!open(t))return 0;
  double u=x-tx+.5,v=z+ty+.5,sw=visualBedCorner(tx,ty),se=visualBedCorner(tx+1,ty),ne=visualBedCorner(tx+1,ty+1),nw=visualBedCorner(tx,ty+1),c=-depth(t.floor());
  if(v>=u&&v>=1-u)return sw*(v-u)+se*(u+v-1)+c*2*(1-v);if(v<=u&&v<=1-u)return nw*(1-u-v)+ne*(u-v)+c*2*v;if(u>=v&&u>=1-v)return se*(u+v-1)+ne*(u-v)+c*2*(1-u);return sw*(v-u)+nw*(1-u-v)+c*2*u;
 }
 public static double flightFloor(Unit u,double x,double z,double original){return u!=null&&u.type.flying&&u.isPlayer()&&open(Vars.world.tile((int)Math.floor(x+.5),(int)Math.floor(-z+.5)))?bed(x,z):original;}
 public static double visualBedCorner(int x,int y){double d=0;for(int j=-1;j<=0;j++)for(int i=-1;i<=0;i++){var t=Vars.world.tile(x+i,y+j);if(!open(t))return 0;d+=depth(t.floor());}return -d*.25;}
 public static double hash(int x,int y){int h=x*0x1f123bb5+y*0x5f356495;h^=h>>>16;h*=0x7feb352d;return (h&0x7fffffff)/2147483647d;}
}
