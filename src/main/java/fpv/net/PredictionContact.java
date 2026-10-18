package fpv.net;

import fpv.flight.FlightController;
import fpv.physics.*;
import fpv.world.*;
import fpv.generated.*;
import mindustry.Vars;
import mindustry.gen.Unit;

/** Read-only conservative clipping of the local visual ghost. Never CrashSystem or native impulses. */
final class PredictionContact implements LocalPilotPrediction.Contact {
    private final World3DAdapter world;
    private final RammingCollision bodies;
    private SweptCollision terrain;
    private Unit unit;
    private double radius,halfHeight;
    private int kind;
    private final java.util.IdentityHashMap<Unit,Unit3DState> poses=new java.util.IdentityHashMap<>();
    private boolean batch;
    PredictionContact(World3DAdapter world){this.world=world;bodies=new RammingCollision(world,null,this::state);}
    void begin(){poses.clear();batch=true;}
    void end(){batch=false;poses.clear();}
    private Unit3DState state(Unit unit){
        if(!batch)return world.unitState(unit);
        if(poses.containsKey(unit))return poses.get(unit);
        var value=world.unitState(unit);if(poses.size()<NetworkConfig.MAX_UNIT_STATES)poses.put(unit,value);return value;
    }
    void configure(Unit unit,Unit3DState body,int kind){
        this.unit=unit;this.kind=kind;
        double r=Math.max(.22,Math.min(CrashConfig.MAX_COLLIDER_RADIUS_TILES,body.radius()));
        double h=Math.max(.12,Math.min(CrashConfig.MAX_COLLIDER_HALF_HEIGHT,body.halfHeight()));
        if(terrain==null||r!=radius||h!=halfHeight){radius=r;halfHeight=h;terrain=new SweptCollision((x,z)->world.surfaceHeight(x,-z),r,h,CrashConfig.SWEEP_STEP_TILES);}
    }
    public void afterStep(FlightController.State s,double x,double y,double z,double seconds,double elapsed){
        if(unit==null||terrain==null)return;
        s.x=Math.max(.1,Math.min(Vars.world.width()-1.1,s.x));s.z=Math.max(-(Vars.world.height()-1.1),Math.min(-.1,s.z));
        double ceiling=Math.max(RuntimeConfig.MAX_HEIGHT,world.maximumTerrainHeight()+RuntimeConfig.START_HEIGHT);
        if(s.y>ceiling){s.y=ceiling;s.vy=Math.min(0,s.vy);}
        if(kind==PilotCheckpoint.GROUND){
            double dx=s.x-x,dz=s.z-z;int steps=Math.max(1,(int)Math.ceil(Math.hypot(dx,dz)/.2));
            for(int i=1;i<=steps;i++){
                double t=i/(double)steps,px=x+dx*t,py=-z-dz*t;
                if(!unit.canPass((int)Math.round(px),(int)Math.round(py))||Math.abs(world.terrainHeight(px,py)-world.terrainHeight(x,-z))>Math.max(.15,halfHeight)){
                    s.x=x;s.y=y;s.z=z;s.vx=s.vz=0;return;
                }
            }
            var hit=world.traceUnitBarrier(unit,x,-z,y,s.x,-s.z,s.y,radius,halfHeight);
            if(hit!=null){s.x=x+dx*Math.max(0,hit.time()-1e-6);s.z=z+dz*Math.max(0,hit.time()-1e-6);s.y=y;s.vx=s.vz=0;}
            // Stance, ledge traversal, water/legs and support transitions are confirmed by the server.
            return;
        }
        if(kind!=PilotCheckpoint.AIR&&kind!=PilotCheckpoint.LANDING&&kind!=PilotCheckpoint.CREATOR)return;
        // Cosmetic residuals may move a zero-velocity display. Sweep their actual displacement too.
        double probe=Math.max(1e-6,seconds);
        var hit=bodies.first(unit,x,-z,y,s.x,-s.z,s.y,(s.x-x)/probe,-(s.z-z)/probe,(s.y-y)/probe,radius,halfHeight,0);
        if(hit!=null){double t=Math.max(0,hit.time()-1e-6);s.x=x+(s.x-x)*t;s.y=y+(s.y-y)*t;s.z=z+(s.z-z)*t;s.vx=s.vy=s.vz=0;}
        terrain.resolve(s,x,y,z);
    }
}
