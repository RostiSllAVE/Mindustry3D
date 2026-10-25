package fpv.render;

import arc.graphics.Color;
import arc.graphics.g3d.Camera3D;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Unit;

/** Native aircraft hardpoints, body-local exhaust, existing emissive volume batch. Presentation only. */
public final class FlyingEngineVisuals {
    public record Stats(int units,int engines,int segments){}
    private final double[] basis=new double[9];private int units,engines,segments;
    public void begin(){units=engines=segments=0;}
    public Stats stats(){return new Stats(units,engines,segments);}
    public static boolean eligible(Unit unit){return unit!=null&&unit.type!=null&&unit.type.flying&&unit.isFlying()&&unit.isAdded()&&!unit.dead&&unit.health>0;}
    public static float reach(Unit unit){
        if(unit==null||unit.type==null||!unit.type.flying)return 0;
        float reach=(Math.abs(unit.type.engineOffset)+unit.type.engineSize*6)/Vars.tilesize;
        for(var e:unit.type.engines)reach=Math.max(reach,(Math.abs(e.x)+Math.abs(e.y)+e.radius*6)/Vars.tilesize);
        return Float.isFinite(reach)?reach:0;
    }
    public boolean render(Unit unit,float x,float y,float z,float yaw,float pitch,float roll,float halfDepth,
                          double speed,Camera3D camera,CombatVisualRenderer.Sink sink){
        if(!eligible(unit)||camera==null||sink==null)return false;
        float elevation=unit.type.useEngineElevation?Math.max(0,Math.min(1,unit.elevation)):1;if(elevation<.001f)return false;
        double nominal=Math.max(.05,unit.type.speed/Vars.tilesize*60),throttle=Math.max(0,Math.min(1,speed/nominal));
        SpriteExtrusionGeometry.basis(yaw,pitch,roll,basis);int count=0;
        if(unit.type.engines.isEmpty()){
            if(unit.type.engineSize>0&&jet(unit,x,y,z,0,-unit.type.engineOffset,unit.type.engineSize,-90,null,halfDepth,elevation,throttle,0,camera,sink))count++;
        }else for(int i=0;i<unit.type.engines.size;i++){
            var e=unit.type.engines.get(i);
            if(jet(unit,x,y,z,e.x,e.y,e.radius,e.rotation,e.color,halfDepth,elevation,throttle,i,camera,sink))count++;
        }
        if(count>0)units++;return count>0;
    }
    private boolean jet(Unit unit,float x,float y,float z,float right,float forward,float nativeRadius,float rotation,Color tint,
                        float halfDepth,float elevation,double throttle,int index,Camera3D camera,CombatVisualRenderer.Sink sink){
        if(nativeRadius<=0||!Float.isFinite(right+forward+nativeRadius+rotation))return false;
        double r=nativeRadius/Vars.tilesize*elevation,up=-halfDepth*.18;
        double ax=x+basis[0]*right/Vars.tilesize-basis[6]*forward/Vars.tilesize+basis[3]*up;
        double ay=y+basis[1]*right/Vars.tilesize-basis[7]*forward/Vars.tilesize+basis[4]*up;
        double az=z+basis[2]*right/Vars.tilesize-basis[8]*forward/Vars.tilesize+basis[5]*up;
        double a=Math.toRadians(rotation),c=Math.cos(a),s=Math.sin(a),dx=basis[0]*c-basis[6]*s,dy=basis[1]*c-basis[7]*s,dz=basis[2]*c-basis[8]*s;
        double pulse=1+.10*Math.sin(Time.time*.29+unit.id*1.7+index*2.3)+.045*Math.sin(Time.time*.61+index);
        double length=r*(2.2+throttle*2.1)*pulse,bx=ax+dx*length,by=ay+dy*length,bz=az+dz*length;
        if(!camera.frustum.containsBounds((float)((ax+bx)*.5),(float)((ay+by)*.5),(float)((az+bz)*.5),(float)(Math.abs(bx-ax)*.5+r),(float)(Math.abs(by-ay)*.5+r),(float)(Math.abs(bz-az)*.5+r)))return false;
        Color outer=tint!=null?tint:unit.type.engineColor==null?unit.team.color:unit.type.engineColor;
        Color inner=unit.type.engineColorInner==null?Color.white:unit.type.engineColorInner;
        double distance=(ax-camera.position.x)*(ax-camera.position.x)+(ay-camera.position.y)*(ay-camera.position.y)+(az-camera.position.z)*(az-camera.position.z);
        engines++;
        if(distance>RenderDistanceSettings.detail()*(double)RenderDistanceSettings.detail()){
            segment(sink,ax,ay,az,bx,by,bz,r*.85,outer);
        }else{
            segment(sink,ax,ay,az,ax+dx*length*.62,ay+dy*length*.62,az+dz*length*.62,r*1.25,outer);
            segment(sink,ax+dx*length*.57,ay+dy*length*.57,az+dz*length*.57,bx,by,bz,r*.57,outer);
            segment(sink,ax+dx*r*.12,ay+dy*r*.12,az+dz*r*.12,ax+dx*length*.48,ay+dy*length*.48,az+dz*length*.48,r*.58,inner);
        }
        return true;
    }
    private void segment(CombatVisualRenderer.Sink sink,double ax,double ay,double az,double bx,double by,double bz,double width,Color color){
        sink.laser((float)ax,(float)ay,(float)az,(float)bx,(float)by,(float)bz,(float)width,color.r,color.g,color.b);segments++;
    }
}
