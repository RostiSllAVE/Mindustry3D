package fpv.render;

/** Cached normalized machinery. Pipe arms terminate exactly at cell edges; no per-frame generation. */
public final class LiquidModelGeometry {
    private LiquidModelGeometry(){}
    public record Model(float[] body,float[] lining,float[] rotor,float[] fluid,float[] lights){}
    private static final Model[][][] CACHE=new Model[13][16][3];
    public static Model model(LiquidModelProfile p,int mask,int lod){
        if(p==null||mask<0||mask>15||lod<0||lod>2)throw new IllegalArgumentException("Bounded liquid profile/mask/LOD required");
        if(!p.pipe())mask=15;var old=CACHE[p.ordinal()][mask][lod];if(old!=null)return old;
        var b=new MechanicalBlockGeometry.Builder();var dark=new MechanicalBlockGeometry.Builder();var r=new MechanicalBlockGeometry.Builder();var f=new MechanicalBlockGeometry.Builder();var light=new MechanicalBlockGeometry.Builder();
        if(p.pipe()){
            // An open inspection channel, not a closed slab covering a fictitious full pipe.
            float floor=.035f/p.height; b.box(-.23f,0,-.23f,.23f,floor,.23f,true);
            for(int side=0;side<4;side++){
                if((mask&(1<<side))!=0){
                    arm(b,side,.20f,.50f,.20f,0,floor);arm(b,side,.20f,.50f,.05f,floor,.72f,-.23f);arm(b,side,.20f,.50f,.05f,floor,.72f,.23f);
                    if(lod<2){arm(b,side,.40f,.47f,.045f,floor,.85f,-.24f);arm(b,side,.40f,.47f,.045f,floor,.85f,.24f);}
                    if(p==LiquidModelProfile.PLATED&&lod<2)arm(b,side,.28f,.34f,.29f,.76f,.95f);
                }else arm(b,side,.19f,.27f,.25f,floor,.72f);
            }
            if(p==LiquidModelProfile.PULSE){for(int side=0;side<4;side++)arm(light,side,.19f,.23f,.13f,.73f,.80f);}
            else light.box(-.06f,.73f,-.22f,.06f,.80f,-.18f,true);
        }else if(lod==2){
            b.box(-.45f,0,-.45f,.45f,.83f,.45f,true);
        }else{
            b.box(-.48f,0,-.48f,.48f,.11f,.48f,true);int n=lod==0?8:4;
            // Low liquid connectors match conduit inspection channels, independently of machine size.
            if(p!=LiquidModelProfile.THERMAL)for(int side=0;side<4;side++)arm(b,side,.35f,.50f,.13f,.10f,.23f);
            switch(p){
                case ROUTER->{b.column(0,.11f,0,.34f,.48f,.29f,n);for(int side=0;side<4;side++)arm(b,side,.12f,.43f,.07f,.50f,.86f);}
                case JUNCTION->{b.box(-.38f,.11f,-.13f,.38f,.46f,.13f,true);b.box(-.13f,.47f,-.38f,.13f,.83f,.38f,true);}
                case BRIDGE,PHASE->{for(int x=-1;x<=1;x+=2)b.column(x*.31f,.11f,0,.07f,.92f,.07f,4);b.box(-.40f,.76f,-.20f,.40f,.92f,.20f,true);b.column(0,.12f,0,.20f,.99f,.12f,n);if(p==LiquidModelProfile.PHASE)for(int z=-1;z<=1;z+=2)b.box(-.30f,.35f,z*.24f-.035f,.30f,.65f,z*.24f+.035f,true);}
                case CONTAINER,TANK->{
                    float radius=p==LiquidModelProfile.TANK?.37f:.33f;
                    for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.column(x*radius,.11f,z*radius,.035f,.92f,.035f,4);
                    ring(b,.43f,.32f,.80f,.89f,n);b.column(0,.89f,0,.17f,.97f,.15f,n);
                    if(p==LiquidModelProfile.TANK)for(int side=0;side<4;side++)arm(b,side,.35f,.43f,.035f,.27f,.66f);
                }
                case MECHANICAL->{b.column(0,.11f,0,.25f,.42f,.21f,n);for(int x=-1;x<=1;x+=2)b.column(x*.34f,.11f,0,.06f,.83f,.06f,4);b.box(-.39f,.80f,-.11f,.39f,.91f,.11f,true);b.column(0,.41f,0,.045f,.83f,.045f,4);blades(r,.25f,.57f,.64f);}
                case ROTARY->{b.column(0,.11f,0,.34f,.66f,.28f,n);b.column(0,.65f,0,.045f,.87f,.045f,4);blades(r,.30f,.84f,.91f);for(int x=-1;x<=1;x+=2)b.box(x*.39f-.045f,.11f,-.30f,x*.39f+.045f,.73f,.30f,true);}
                case IMPULSE->{for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.column(x*.30f,.11f,z*.30f,.085f,.80f,.065f,n);b.column(0,.11f,0,.22f,.62f,.19f,n);b.column(0,.61f,0,.06f,.89f,.06f,4);blades(r,.25f,.85f,.94f);ring(b,.40f,.32f,.78f,.84f,n);}
                case THERMAL->{
                    b.column(0,.11f,0,.29f,.69f,.24f,n);b.column(0,.68f,0,.18f,.82f,.17f,n);
                    for(int x=-1;x<=1;x+=2){b.column(x*.36f,.11f,-.12f,.065f,.90f,.065f,4);b.box(x*.36f-.05f,.82f,-.15f,x*.36f+.05f,.90f,.27f,true);}
                    for(int i=0;i<(lod==0?5:3);i++){float z=-.27f+i*(lod==0?.135f:.27f);b.box(-.34f,.30f,z-.025f,.34f,.43f,z+.025f,true);}
                    for(int x=-1;x<=1;x+=2){b.column(x*.32f,.11f,.25f,.065f,.84f,.065f,4);light.box(x*.32f-.04f,.48f,.295f,x*.32f+.04f,.66f,.31f,true);}
                }
                default->{}
            }
        }
        if(p.pipe())closed(f,-1,0,-1,1,1,1);else if(p!=LiquidModelProfile.THERMAL)closed(f,-1,0,-1,1,1,1);
        if(!p.pipe()&&p!=LiquidModelProfile.THERMAL)light.box(-.10f,.93f,-.10f,.10f,.98f,.10f,true);
        return CACHE[p.ordinal()][mask][lod]=new Model(b.finish(),dark.finish(),r.finish(),f.finish(),light.finish());
    }
    private static void blades(MechanicalBlockGeometry.Builder b,float half,float low,float high){b.box(-half,low,-.045f,half,high,.045f,true);b.box(-.045f,low,-half,.045f,high,half,true);}
    private static void closed(MechanicalBlockGeometry.Builder b,float x0,float y0,float z0,float x1,float y1,float z1){b.box(x0,y0,z0,x1,y1,z1,true);b.face(x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,0,-1,0);}
    private static void ring(MechanicalBlockGeometry.Builder b,float outer,float inner,float low,float high,int n){for(int i=0;i<n;i++){double a=(i+.5)*Math.PI*2/n,c=(i+1.5)*Math.PI*2/n;float ax=(float)Math.cos(a),az=(float)Math.sin(a),cx=(float)Math.cos(c),cz=(float)Math.sin(c);b.face(ax*outer,low,az*outer,cx*outer,low,cz*outer,cx*outer,high,cz*outer,ax*outer,high,az*outer,ax,0,az);b.face(ax*outer,high,az*outer,cx*outer,high,cz*outer,cx*inner,high,cz*inner,ax*inner,high,az*inner,0,1,0);}}
    private static void arm(MechanicalBlockGeometry.Builder b,int side,float start,float end,float half,float low,float high){arm(b,side,start,end,half,low,high,0);}
    private static void arm(MechanicalBlockGeometry.Builder b,int side,float start,float end,float half,float low,float high,float offset){switch(side){case 0->b.box(start,low,offset-half,end,high,offset+half,true);case 1->b.box(offset-half,low,-end,offset+half,high,-start,true);case 2->b.box(-end,low,offset-half,-start,high,offset+half,true);default->b.box(offset-half,low,start,offset+half,high,end,true);}}
}
