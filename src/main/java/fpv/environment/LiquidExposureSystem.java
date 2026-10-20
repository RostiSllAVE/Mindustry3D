package fpv.environment;
import mindustry.Vars;import mindustry.content.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.entities.units.AIController;import mindustry.ai.types.*;import fpv.world.*;import java.util.function.Function;
/** Bounded connected-liquid index and server-only native status application. No custom damage packets. */
public final class LiquidExposureSystem {
 private float[] heatTop=new float[0];
 private int[] label=new int[0],sizes=new int[0],queue=new int[0];private Object tiles;private int changes=-1,regions;private double elapsed;
 private final Function<Unit,Unit3DState> poses;private final Sample sample=new Sample();
 public static final class Sample {public double burning,contact,safeHeight;public Tile nearest;public void clear(){burning=contact=safeHeight=0;nearest=null;}}
 public LiquidExposureSystem(Function<Unit,Unit3DState> poses){this.poses=poses;}
 public void clear(){tiles=null;changes=-1;elapsed=0;}
 public void ensure(){if(Vars.world==null)return;int n=Vars.world.width()*Vars.world.height();if(tiles==Vars.world.tiles&&changes==Vars.world.tileChanges&&label.length==n)return;tiles=Vars.world.tiles;changes=Vars.world.tileChanges;
  if(label.length!=n){label=new int[n];sizes=new int[n+1];queue=new int[n];heatTop=new float[n];}java.util.Arrays.fill(label,0);int id=0,w=Vars.world.width();
  for(int p=0;p<n;p++){Tile t=Vars.world.tiles.geti(p);if(label[p]!=0||!LiquidBody.open(t))continue;int kind=LiquidBody.kind(t.floor()),head=0,tail=1;queue[0]=p;label[p]=++id;
   while(head<tail){int at=queue[head++],x=at%w,y=at/w;for(int k=0;k<4;k++){int nx=x+(k==0?-1:k==1?1:0),ny=y+(k==2?-1:k==3?1:0);Tile next=Vars.world.tile(nx,ny);if(!LiquidBody.open(next)||LiquidBody.kind(next.floor())!=kind)continue;int q=nx+ny*w;if(label[q]==0){label[q]=id;queue[tail++]=q;}}}sizes[id]=tail;
  }regions=id;java.util.Arrays.fill(heatTop,0);
  for(int p=0;p<n;p++){Tile t=Vars.world.tiles.geti(p);if(!LiquidBody.open(t)||t.floor().status==StatusEffects.none&&LiquidBody.kind(t.floor())!=3)continue;int material=LiquidBody.kind(t.floor());double r=material==3?burnRadius(sizes[label[p]]):touchRadius(material,sizes[label[p]]);heatTop[p]=Math.max(heatTop[p],(float)(5*r+.35));
   boolean edge=false;for(int d=0;d<4;d++){Tile q=Vars.world.tile(t.x+(d==0?-1:d==1?1:0),t.y+(d==2?-1:d==3?1:0));if(!LiquidBody.open(q)||LiquidBody.kind(q.floor())!=material){edge=true;break;}}if(!edge)continue;
   int reach=(int)Math.ceil(r+1);for(int yy=t.y-reach;yy<=t.y+reach;yy++)for(int xx=t.x-reach;xx<=t.x+reach;xx++){if(Vars.world.tile(xx,yy)==null)continue;double dx=Math.max(0,Math.abs(xx-t.x)-1),dy=Math.max(0,Math.abs(yy-t.y)-1),h=r*r-dx*dx-dy*dy;if(h<=0)continue;int k=xx+yy*w;heatTop[k]=Math.max(heatTop[k],(float)(5*Math.sqrt(h)+.35));}
  }
 }
 public int area(Tile t){ensure();if(t==null)return 0;int p=t.x+t.y*Vars.world.width();return p>=0&&p<label.length?sizes[label[p]]:0;}
 public static double burnRadius(int area){return .6+Math.min(5,.28*Math.sqrt(Math.max(1,area)));}
 public static double touchRadius(int kind,int area){return kind==3?.16+Math.min(1.2,.08*Math.sqrt(Math.max(1,area))):.08+Math.min(.18,.018*Math.sqrt(Math.max(1,area)));}
 public void sample(double x,double y,double z,double radius,double half,Sample out){ensure();out.clear();if(regions==0)return;double best=Double.POSITIVE_INFINITY;int cx=(int)Math.floor(x+.5),cy=(int)Math.floor(y+.5),reach=7+(int)Math.ceil(Math.min(16,radius));
  for(int ty=cy-reach;ty<=cy+reach;ty++)for(int tx=cx-reach;tx<=cx+reach;tx++){Tile t=Vars.world.tile(tx,ty);if(!LiquidBody.open(t))continue;int kind=LiquidBody.kind(t.floor());int n=sizes[label[tx+ty*Vars.world.width()]];
   double dx=Math.max(0,Math.abs(x-tx)-.5-radius),dy=Math.max(0,Math.abs(y-ty)-.5-radius),horizontal=Math.hypot(dx,dy),vertical=Math.max(0,z-half+.16);
   double distance=Math.hypot(horizontal,vertical/5),contact=touchRadius(kind,n);double c=Math.max(0,1-distance/contact);
   if(c>out.contact)out.contact=c;
   if(kind==3){double r=burnRadius(n);out.burning=Math.max(out.burning,Math.max(0,1-distance/r));if(horizontal<r)out.safeHeight=Math.max(out.safeHeight,-.16+5*Math.sqrt(Math.max(0,r*r-horizontal*horizontal))+half+.35);}
   if(distance<best){best=distance;out.nearest=t;}
  }
 }
 public void update(double seconds){if(seconds<=0||Vars.net.client()||!Vars.state.isGame()||Vars.state.isPaused())return;double before=elapsed;elapsed+=seconds;ensure();if(regions==0)return;
  for(Unit u:Groups.unit){if(u.dead||!u.isAdded())continue;double phase=(u.id%100)*.001;if(seconds<.1&&Math.floor((elapsed+phase)*10)==Math.floor((before+phase)*10))continue;var p=poses.apply(u);if(p==null)continue;sample(p.x(),p.y(),p.z(),p.radius(),p.halfHeight(),sample);
   if(sample.burning>0)u.apply(StatusEffects.burning,45);
   // Evaluate each nearby contact material separately: overlapping water/oil/heat must not borrow a different tile's status.
   int cx=(int)Math.floor(p.x()+.5),cy=(int)Math.floor(p.y()+.5),reach=2+(int)Math.ceil(Math.min(16,p.radius()));for(int yy=cy-reach;yy<=cy+reach;yy++)for(int xx=cx-reach;xx<=cx+reach;xx++){Tile t=Vars.world.tile(xx,yy);if(!LiquidBody.open(t))continue;int kind=LiquidBody.kind(t.floor()),n=sizes[label[xx+yy*Vars.world.width()]];double dx=Math.max(0,Math.abs(p.x()-xx)-.5-p.radius()),dy=Math.max(0,Math.abs(p.y()-yy)-.5-p.radius()),v=Math.max(0,p.z()-p.halfHeight()+.16);
    if(Math.hypot(Math.hypot(dx,dy),v/5)<touchRadius(kind,n))u.apply(t.floor().status,Math.max(45,t.floor().statusDuration));}
  }
 }
 private static final java.lang.reflect.Field AI_TARGET=targetField();private static java.lang.reflect.Field targetField(){try{var f=AIController.class.getDeclaredField("target");f.setAccessible(true);return f;}catch(ReflectiveOperationException|RuntimeException e){return null;}}
 public static boolean idle(Unit u){if(u==null||!u.type.flying||!u.isFlying()||u.isPlayer()||u.isShooting||u.mineTile!=null||u.isBuilding())return false;
  if(!(u.controller() instanceof FlyingAI)&&!(u.controller() instanceof CommandAI))return false;
  if(u.controller() instanceof CommandAI c&&(c.hasCommand()&&c.currentCommand()!=mindustry.ai.UnitCommand.moveCommand||c.attackTarget!=null||c.targetBuild!=null))return false;
  if(u.mounts!=null)for(var m:u.mounts)if(m.target!=null||m.shoot)return false;
  try{return AI_TARGET!=null&&AI_TARGET.get(u.controller())==null;}catch(IllegalAccessException e){return false;}
 }
 /** Altitude-only steering through the existing obstacle/clearance solver, ahead of idle flyers. */
 public double clearance(Unit u,double x,double y,double height,double vx,double vy,double half){if(!idle(u)||Vars.net.client())return 0;ensure();double result=0,r=Math.min(16,Math.max(.2,u.hitSize/16d));for(int step=0;step<3;step++)for(int j=-1;j<=1;j++)for(int i=-1;i<=1;i++){int tx=(int)Math.floor(x+vx*step*.4+i*r+.5),ty=(int)Math.floor(y+vy*step*.4+j*r+.5);if(Vars.world.tile(tx,ty)!=null)result=Math.max(result,heatTop[tx+ty*Vars.world.width()]);}return result==0?0:result+half;}
}
