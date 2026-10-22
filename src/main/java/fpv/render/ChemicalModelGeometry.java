package fpv.render;

/** Immutable normalized parts, shared across instances and three distance LODs. */
public final class ChemicalModelGeometry {
    private ChemicalModelGeometry(){}
    public record Model(float[] body,float[] lining,float[] rotor,float[] piston,float[] arm,float[] fluid,float[] accent){}
    private static final Model[][][] CACHE=new Model[8][17][3];
    public static Model model(ChemicalModelProfile p,int cells,int lod){
        if(p==null||cells<1||cells>16||lod<0||lod>2)throw new IllegalArgumentException("Bounded chemical profile/size/LOD required");
        var old=CACHE[p.ordinal()][cells][lod];if(old!=null)return old;
        var b=new MechanicalBlockGeometry.Builder();var r=new MechanicalBlockGeometry.Builder();var s=new MechanicalBlockGeometry.Builder();var a=new MechanicalBlockGeometry.Builder();var f=new MechanicalBlockGeometry.Builder();var light=new MechanicalBlockGeometry.Builder();
        float[] shell=new float[0],lining=new float[0];
        if(lod==2)b.box(-.48f,0,-.48f,.48f,.91f,.48f,false);
        else{
            var ports=ResourcePortGeometry.generate(cells,.20f,lod==0);shell=ports.shell();lining=ports.lining();int n=lod==0?8:4;
            switch(p){
                case CRYO->{
                    for(int x=-1;x<=1;x+=2){tank(b,x*.22f,0,.185f,.14f,.24f,.69f,n);b.column(x*.22f,.67f,-.19f,.055f,.92f,.055f,4);b.box(x*.22f-.04f,.84f,-.19f,x*.22f+.04f,.90f,.025f,true);}
                    b.box(-.29f,.84f,-.24f,.29f,.93f,-.14f,true);b.column(0,.20f,-.34f,.08f,.87f,.07f,4);
                    r.box(-.10f,.38f,-.025f,.10f,.43f,.025f,true);r.box(-.025f,.38f,-.10f,.025f,.43f,.10f,true);r.column(0,.36f,0,.02f,.87f,.02f,4);
                }
                case MELTER->{
                    tank(b,0,0,.37f,.28f,.22f,.76f,n);b.box(-.43f,.20f,-.28f,-.32f,.91f,.20f,true);b.box(-.35f,.82f,-.25f,-.08f,.94f,-.10f,true);
                    if(lod==0)b.column(.30f,.67f,-.25f,.07f,.96f,.055f,4);
                    light.box(-.31f,.34f,.285f,.31f,.46f,.31f,true);
                }
                case PRESS->{
                    for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.column(x*.32f,.20f,z*.29f,.045f,.94f,.045f,4);
                    b.box(-.37f,.85f,-.34f,.37f,.95f,.34f,true);b.box(-.25f,.20f,-.21f,.25f,.34f,.21f,true);
                    closed(s,-.24f,.60f,-.20f,.24f,.69f,.20f);s.column(0,.69f,0,.065f,.88f,.065f,4);
                    tank(b,0,.34f,.13f,.095f,.24f,.48f,n);b.column(0,.69f,0,.035f,.88f,.035f,4);
                }
                case CENTRIFUGE->{
                    tank(b,0,0,.36f,.28f,.20f,.66f,n);
                    for(int x=-1;x<=1;x+=2)b.box(x*.40f-.035f,.20f,-.29f,x*.40f+.035f,.85f,.29f,true);
                    b.box(-.42f,.82f,-.32f,.42f,.91f,-.24f,true);
                    r.column(0,.46f,0,.21f,.78f,.17f,n);for(int i=0;i<4;i++){double angle=i*Math.PI/2;float x=(float)Math.cos(angle)*.235f,z=(float)Math.sin(angle)*.235f;r.column(x,.51f,z,.035f,.70f,.035f,4);}
                }
                case CULTIVATOR->{
                    tank(b,0,0,.40f,.34f,.21f,.43f,n);
                    for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2){b.column(x*.30f,.40f,z*.30f,.027f,.91f,.027f,4);b.column(x*.15f,.22f,z*.15f,.025f,.48f,.025f,4);light.column(x*.15f,.35f,z*.15f,.065f,.68f,.10f,n);}
                    b.box(-.34f,.89f,-.34f,.34f,.96f,-.27f,true);b.box(-.34f,.89f,.27f,.34f,.96f,.34f,true);
                }
                case WATER->{
                    tank(b,.23f,.12f,.18f,.13f,.22f,.59f,n);
                    b.column(-.19f,.20f,-.10f,.13f,.48f,.11f,n);b.column(-.19f,.76f,-.10f,.11f,.88f,.11f,n);b.box(-.32f,.82f,-.24f,.30f,.93f,-.13f,true);
                    b.column(.24f,.55f,-.17f,.055f,.88f,.055f,4);
                    closed(s,-.25f,.46f,-.16f,-.13f,.74f,-.04f);
                    for(int i=0;i<4;i++){double angle=i*Math.PI/2;float x=(float)Math.cos(angle)*.13f,z=(float)Math.sin(angle)*.13f;r.box(x-.045f,.39f,z-.045f,x+.045f,.45f,z+.045f,true);}
                }
                case OIL->{
                    tank(b,.25f,.25f,.18f,.13f,.22f,.58f,n);
                    for(int x=-1;x<=1;x+=2)b.box(x*.16f-.04f,.20f,-.12f,x*.16f+.04f,.65f,.09f,true);
                    b.box(-.22f,.61f,-.15f,.22f,.70f,.12f,true);
                    // Beam pivots about local origin; renderer places it on the fixed bearing.
                    closed(a,-.37f,-.035f,-.055f,.37f,.035f,.055f);
                    closed(s,-.35f,0,-.045f,-.30f,1,.045f);
                    b.column(-.325f,.20f,0,.065f,.31f,.065f,4);
                }
                case PULVERIZER->{
                    b.box(-.34f,.20f,-.32f,.34f,.35f,.32f,true);
                    for(int x=-1;x<=1;x+=2)b.box(x*.39f-.045f,.20f,-.36f,x*.39f+.045f,.92f,.36f,true);
                    for(int z=-1;z<=1;z+=2)b.box(-.34f,.79f,z*.33f-.04f,.34f,.88f,z*.33f+.04f,true);
                    for(int i=0;i<4;i++){double angle=i*Math.PI/2;float x=(float)Math.cos(angle)*.18f,z=(float)Math.sin(angle)*.18f;r.box(x-.055f,.44f,z-.055f,x+.055f,.62f,z+.055f,true);}
                    closed(s,-.17f,.63f,-.16f,.17f,.72f,.16f);
                }
            }
            // A single cached unit-height liquid volume. Per-instance transforms set its real fill level.
            closed(f,-1,0,-1,1,1,1);
        }
        b.face(-.49f,0,-.49f,.49f,0,-.49f,.49f,0,.49f,-.49f,0,.49f,0,-1,0);
        var body=b.finish();var all=new float[shell.length+body.length];System.arraycopy(shell,0,all,0,shell.length);System.arraycopy(body,0,all,shell.length,body.length);
        return CACHE[p.ordinal()][cells][lod]=new Model(all,lining,r.finish(),s.finish(),a.finish(),f.finish(),light.finish());
    }
    private static void closed(MechanicalBlockGeometry.Builder b,float x0,float y0,float z0,float x1,float y1,float z1){b.box(x0,y0,z0,x1,y1,z1,true);b.face(x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,0,-1,0);}
    private static void tank(MechanicalBlockGeometry.Builder b,float x,float z,float outer,float inner,float low,float high,int n){
        for(int i=0;i<n;i++){double a=(i+.5)*Math.PI*2/n,c=(i+1.5)*Math.PI*2/n;float ax=(float)Math.cos(a),az=(float)Math.sin(a),cx=(float)Math.cos(c),cz=(float)Math.sin(c);
            b.face(x+ax*outer,low,z+az*outer,x+cx*outer,low,z+cz*outer,x+cx*outer,high,z+cz*outer,x+ax*outer,high,z+az*outer,ax,0,az);
            b.face(x+cx*inner,low,z+cz*inner,x+ax*inner,low,z+az*inner,x+ax*inner,high,z+az*inner,x+cx*inner,high,z+cz*inner,-ax,0,-az);
            b.face(x+ax*outer,high,z+az*outer,x+cx*outer,high,z+cz*outer,x+cx*inner,high,z+cz*inner,x+ax*inner,high,z+az*inner,0,1,0);
        }
    }
}
