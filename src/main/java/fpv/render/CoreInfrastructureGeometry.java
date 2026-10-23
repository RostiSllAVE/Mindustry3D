package fpv.render;

/** Native top art is mapped in footprint coordinates over purpose-built mechanical terraces.
 * All positions are fractions of existing XYZ bounds. Immutable templates are shared by instances. */
public final class CoreInfrastructureGeometry {
    public record Model(float[] body,float[] lining,float[] lights,float[] moving,float[] movingLights){}
    private static final Model[][][] CACHE=new Model[CoreInfrastructureProfile.values().length][17][3];
    private CoreInfrastructureGeometry(){}

    public static Model model(CoreInfrastructureProfile p,int size,int lod){
        if(p==null||size<1||size>16||lod<0||lod>2)throw new IllegalArgumentException("Bounded core/infrastructure profile required");
        var old=CACHE[p.ordinal()][size][lod];if(old!=null)return old;
        var b=new MechanicalBlockGeometry.Builder();var lights=new MechanicalBlockGeometry.Builder();
        var moving=new MechanicalBlockGeometry.Builder();var movingLights=new MechanicalBlockGeometry.Builder();
        float[] shell=new float[0],lining=new float[0];
        if(p.core()){
            if(lod<2){var ports=ResourcePortGeometry.generate(size,.16f,lod==0);shell=ports.shell();lining=ports.lining();}
            else b.box(-.49f,0,-.49f,.49f,.16f,.49f,true);
            switch(p){
                case SHARD->{
                    b.column(0,.16f,0,.30f,.71f,.26f,8);
                    corners(b,.32f,.105f,.16f,.48f,lod==0?8:4);
                    b.column(0,.71f,0,.22f,.90f,.19f,8);
                    if(lod<2)for(int i=0;i<2;i++)bar(b,0,0,.065f,.67f,.20f,.30f,45+i*90);
                    ring(lights,.215f,.194f,.895f,.928f,8);
                }
                case FOUNDATION->{
                    b.column(0,.16f,0,.40f,.38f,.36f,8);
                    corners(b,.355f,.105f,.16f,.59f,4);
                    b.column(0,.38f,0,.32f,.75f,.28f,8);
                    b.column(0,.75f,0,.245f,.95f,.23f,8);
                    if(lod<2){bar(b,0,0,.08f,.82f,.26f,.42f,0);bar(b,0,0,.08f,.82f,.26f,.43f,90);}
                    ring(lights,.265f,.247f,.745f,.784f,8);
                }
                case NUCLEUS->{
                    b.column(0,.16f,0,.47f,.36f,.43f,8);
                    corners(b,.36f,.115f,.16f,.70f,4);
                    b.column(0,.36f,0,.35f,.64f,.32f,8);
                    b.column(0,.64f,0,.25f,.96f,.21f,8);
                    if(lod<2)for(int i=0;i<2;i++)bar(b,0,0,.08f,.89f,.37f,.49f,45+i*90);
                    ring(lights,.285f,.265f,.635f,.685f,8);
                }
                case BASTION->{
                    bar(b,0,0,.29f,.91f,.16f,.43f,45);
                    bar(b,0,0,.29f,.91f,.16f,.49f,-45);
                    corners(b,.32f,.115f,.16f,.68f,4);
                    b.column(0,.49f,0,.29f,.81f,.25f,8);
                    b.column(0,.81f,0,.16f,.95f,.14f,4);
                    ring(lights,.285f,.264f,.805f,.845f,8);
                }
                case CITADEL->{
                    b.column(0,.16f,0,.46f,.33f,.42f,8);
                    for(int i=0;i<2;i++)bar(b,0,0,.15f,.89f,.33f,.56f,45+i*90);
                    corners(b,.35f,.12f,.16f,.76f,8);
                    b.column(0,.56f,0,.31f,.80f,.285f,8);
                    b.column(0,.80f,0,.21f,.97f,.18f,4);
                    ring(lights,.31f,.29f,.795f,.833f,8);
                }
                case ACROPOLIS->{
                    bar(b,0,0,.32f,.95f,.16f,.39f,45);
                    bar(b,0,0,.32f,.95f,.16f,.45f,-45);
                    b.column(0,.45f,0,.40f,.65f,.35f,8);
                    corners(b,.355f,.125f,.16f,.82f,lod==0?8:4);
                    if(lod<2)for(int i=0;i<2;i++)bar(b,0,0,.10f,.86f,.66f,.74f,45+i*90);
                    b.column(0,.65f,0,.265f,.98f,.22f,4);
                    ring(lights,.31f,.286f,.645f,.69f,8);
                }
                default->throw new IllegalStateException();
            }
            // Roof-mounted team nodes, never capacity/resource indicators.
            float corner=p==CoreInfrastructureProfile.SHARD?.48f:p==CoreInfrastructureProfile.FOUNDATION?.59f:
                p==CoreInfrastructureProfile.NUCLEUS?.70f:p==CoreInfrastructureProfile.BASTION?.68f:
                p==CoreInfrastructureProfile.CITADEL?.76f:.82f;
            float offset=p==CoreInfrastructureProfile.SHARD||p==CoreInfrastructureProfile.BASTION?.32f:
                p==CoreInfrastructureProfile.FOUNDATION||p==CoreInfrastructureProfile.ACROPOLIS?.355f:
                p==CoreInfrastructureProfile.NUCLEUS?.36f:.35f;
            if(lod<2)for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)
                lights.column(x*offset,corner-.005f,z*offset,.052f,corner+.018f,.045f,4);
            if(lod<2){
                float y=p==CoreInfrastructureProfile.SHARD?.929f:p==CoreInfrastructureProfile.FOUNDATION?.785f:
                    p==CoreInfrastructureProfile.NUCLEUS?.686f:p==CoreInfrastructureProfile.BASTION?.846f:
                    p==CoreInfrastructureProfile.CITADEL?.834f:.691f;
                float r=p==CoreInfrastructureProfile.SHARD?.20f:p==CoreInfrastructureProfile.FOUNDATION?.257f:
                    p==CoreInfrastructureProfile.NUCLEUS?.277f:p==CoreInfrastructureProfile.BASTION?.276f:.30f;
                for(int i=0;i<4;i++){
                    double a=i*Math.PI/2;float x=(float)Math.cos(a)*r,z=(float)Math.sin(a)*r;
                    movingLights.box(x-.012f,y,z-.012f,x+.012f,y+.014f,z+.012f,true);
                }
            }
        }else{
            // Small Erekir radar: armored pedestal, bearing and two supports for its native dish.
            b.box(-.35f,0,-.35f,.35f,.17f,.35f,true);
            b.column(0,.17f,0,.14f,.52f,.12f,4);
            b.column(0,.52f,0,.24f,.65f,.23f,8);
            for(int s=-1;s<=1;s+=2)moving.box(s*.20f-.035f,.55f,-.07f,s*.20f+.035f,.75f,.07f,true);
            lights.box(-.07f,.18f,.294f,.07f,.24f,.302f,true);
        }
        // Inward contact is culled per view above the support; below-plane views keep the bottom.
        float foot=p.core()?.49f:.35f;
        b.face(-foot,0,-foot,foot,0,-foot,foot,0,foot,-foot,0,foot,0,-1,0);
        float[] body=b.finish(),all=new float[shell.length+body.length];
        System.arraycopy(shell,0,all,0,shell.length);System.arraycopy(body,0,all,shell.length,body.length);
        return CACHE[p.ordinal()][size][lod]=new Model(all,lining,lights.finish(),moving.finish(),movingLights.finish());
    }
    private static void corners(MechanicalBlockGeometry.Builder b,float offset,float radius,float low,float high,int n){
        for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.column(x*offset,low,z*offset,radius,high,radius*.88f,n);
    }
    private static void ring(MechanicalBlockGeometry.Builder b,float outer,float inner,float low,float high,int n){
        for(int i=0;i<n;i++){
            double a=(i+.5)*Math.PI*2/n,c=(i+1.5)*Math.PI*2/n;
            float ax=(float)Math.cos(a),az=(float)Math.sin(a),cx=(float)Math.cos(c),cz=(float)Math.sin(c);
            b.face(ax*outer,low,az*outer,cx*outer,low,cz*outer,cx*outer,high,cz*outer,ax*outer,high,az*outer,ax,0,az);
            b.face(cx*inner,low,cz*inner,ax*inner,low,az*inner,ax*inner,high,az*inner,cx*inner,high,cz*inner,-ax,0,-az);
            b.face(ax*outer,high,az*outer,cx*outer,high,cz*outer,cx*inner,high,cz*inner,ax*inner,high,az*inner,0,1,0);
        }
    }
    private static void bar(MechanicalBlockGeometry.Builder b,float x,float z,float w,float length,float low,float high,float degrees){
        double a=Math.toRadians(degrees);float c=(float)Math.cos(a),s=(float)Math.sin(a);
        float[] X={x-w*c/2+length*s/2,x+w*c/2+length*s/2,x+w*c/2-length*s/2,x-w*c/2-length*s/2};
        float[] Z={z-w*s/2-length*c/2,z+w*s/2-length*c/2,z+w*s/2+length*c/2,z-w*s/2+length*c/2};
        b.face(X[0],high,Z[0],X[3],high,Z[3],X[2],high,Z[2],X[1],high,Z[1],0,1,0);
        for(int i=0;i<4;i++){int j=(i+1)%4;float dx=X[j]-X[i],dz=Z[j]-Z[i],n=(float)Math.hypot(dx,dz);
            b.face(X[i],low,Z[i],X[j],low,Z[j],X[j],high,Z[j],X[i],high,Z[i],dz/n,0,-dx/n);}
    }
}
