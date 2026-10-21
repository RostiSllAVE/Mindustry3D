package fpv.render;

/** Small immutable templates in existing XYZ/normal/UV format, shared across all instances. */
public final class LogisticsModelGeometry {
    private LogisticsModelGeometry(){}
    public record Model(float[] body,float[] trim,float[] lining){}
    private static final Model[][][] CACHE=new Model[LogisticsModelProfile.values().length][16][3];
    public static final float BELT_SURFACE=.60f;
    public static Model model(LogisticsModelProfile p,int openings,int lod){
        if(p==null||openings<0||openings>15||lod<0||lod>2)throw new IllegalArgumentException("Bounded logistics profile/mask/LOD required");
        if(!p.belt)openings=15;
        var cached=CACHE[p.ordinal()][openings][lod];if(cached!=null)return cached;
        var body=new MechanicalBlockGeometry.Builder();var trim=new MechanicalBlockGeometry.Builder();float[] lining=new float[0];
        if(p.belt){
            body.box(-.5f,0,-.5f,.5f,BELT_SURFACE,.5f,false);
            if(lod<2)for(int side=0;side<4;side++)if((openings&(1<<side))==0){
                float w=p.heavy?.105f:.055f,top=p.heavy?1:.85f;
                if(side==0)trim.box(.5f-w,BELT_SURFACE,-.5f,.5f,top,.5f,true);
                else if(side==2)trim.box(-.5f,BELT_SURFACE,-.5f,-.5f+w,top,.5f,true);
                else if(side==1)trim.box(-.5f,BELT_SURFACE,-.5f,.5f,top,-.5f+w,true);
                else trim.box(-.5f,BELT_SURFACE,.5f-w,.5f,top,.5f,true);
            }
        }else if(lod<2){
            int cells=p==LogisticsModelProfile.DISTRIBUTOR?2:1;
            var ports=ResourcePortGeometry.generate(cells,1,lod==0);lining=ports.lining();
            // The native roof carries the recognizable routing symbols. Chassis geometry owns the side bays.
            body.face(-.49f,0,-.49f,.49f,0,-.49f,.49f,0,.49f,-.49f,0,.49f,0,-1,0);
            var shell=ports.shell();float[] sides=java.util.Arrays.copyOfRange(shell,6*8,shell.length);
            var parts=body.finish();float[] all=new float[parts.length+sides.length];System.arraycopy(parts,0,all,0,parts.length);System.arraycopy(sides,0,all,parts.length,sides.length);
            // The arch piers already form the corners; do not add coplanar duplicate corner posts.
            return CACHE[p.ordinal()][openings][lod]=new Model(all,trim.finish(),lining);
        }else body.box(-.49f,0,-.49f,.49f,1,.49f,false);
        // Upper floors can expose the underside at platform edges.
        body.face(-.5f,0,-.5f,.5f,0,-.5f,.5f,0,.5f,-.5f,0,.5f,0,-1,0);
        return CACHE[p.ordinal()][openings][lod]=new Model(body.finish(),trim.finish(),lining);
    }
}
