package fpv.render;

/** Immutable per-profile/size/LOD mechanisms. No fields, influence volumes or functional entities. */
public final class SupportModelGeometry {
    private SupportModelGeometry(){}
    public record Model(float[] body,float[] lining,float[] moving,float[] lights,float[] booster){}
    private static final Model[][][] CACHE=new Model[11][17][3];
    public static Model model(SupportModelProfile p,int size,int lod){
        if(p==null||size<1||size>16||lod<0||lod>2)throw new IllegalArgumentException("Bounded support model required");var old=CACHE[p.ordinal()][size][lod];if(old!=null)return old;
        var b=new MechanicalBlockGeometry.Builder();var moving=new MechanicalBlockGeometry.Builder();var light=new MechanicalBlockGeometry.Builder();var boost=new MechanicalBlockGeometry.Builder();float[] shell=new float[0],lining=new float[0];int n=lod==0?8:4;
        if(lod==2)b.box(-.46f,0,-.46f,.46f,.81f,.46f,true);
        else if(p.storage()){
            var ports=ResourcePortGeometry.generate(size,.22f,lod==0);shell=ports.shell();lining=ports.lining();
            b.box(-.37f,.22f,-.37f,.37f,.74f,.37f,true);
            float corner=p.reinforced()?.34f:.39f;for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.box(x*corner-.055f,.22f,z*corner-.055f,x*corner+.055f,.90f,z*corner+.055f,true);
            b.box(-.42f,.74f,-.42f,.42f,.80f,.42f,true);
            if(p==SupportModelProfile.VAULT||p==SupportModelProfile.REINFORCED_VAULT){for(int side=-1;side<=1;side+=2)b.box(-.44f,.42f,side*.39f-.035f,.44f,.50f,side*.39f+.035f,true);}
            if(p.reinforced()){b.column(0,.80f,0,.20f,.92f,.14f,4);for(int side=-1;side<=1;side+=2)b.box(side*.29f-.035f,.80f,-.32f,side*.29f+.035f,.87f,.32f,true);}
            panel(light,.375f);
        }else{
            b.box(-.46f,0,-.46f,.46f,.15f,.46f,true);
            switch(p){
                case MENDER->{b.column(0,.15f,0,.27f,.48f,.19f,4);b.column(0,.47f,0,.11f,.82f,.16f,4);for(int x=-1;x<=1;x+=2)b.box(x*.31f-.045f,.15f,-.19f,x*.31f+.045f,.56f,.19f,true);ring(moving,.25f,.19f,.62f,.67f,4);}
                case MEND->{for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.column(x*.29f,.15f,z*.29f,.07f,.66f,.05f,4);b.column(0,.15f,0,.19f,.83f,.13f,n);ring(moving,.32f,.25f,.64f,.71f,n);}
                case OVERDRIVE->{b.column(0,.15f,0,.25f,.48f,.19f,n);b.column(0,.47f,0,.07f,.85f,.07f,4);ring(moving,.33f,.25f,.64f,.72f,n);for(int z=-1;z<=1;z+=2)b.box(-.31f,.23f,z*.33f-.045f,.31f,.38f,z*.33f+.045f,true);}
                case DOME->{for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.column(x*.30f,.15f,z*.30f,.065f,.81f,.045f,n);b.column(0,.15f,0,.18f,.86f,.11f,n);ring(b,.39f,.33f,.75f,.82f,n);ring(moving,.28f,.21f,.55f,.63f,n);}
                case FORCE->{for(int i=0;i<3;i++){double angle=i*Math.PI*2/3;float x=(float)Math.cos(angle)*.30f,z=(float)Math.sin(angle)*.30f;b.column(x,.15f,z,.07f,.79f,.045f,4);}b.column(0,.15f,0,.20f,.85f,.13f,n);ring(b,.34f,.27f,.57f,.65f,n);ring(moving,.25f,.20f,.70f,.76f,n);}
                case REGEN->{b.column(0,.15f,0,.20f,.84f,.14f,4);for(int i=0;i<3;i++){double angle=i*Math.PI*2/3;float x=(float)Math.cos(angle)*.30f,z=(float)Math.sin(angle)*.30f;b.column(x,.15f,z,.065f,.90f,.035f,4);b.box(x-.055f,.83f,z-.055f,x+.055f,.94f,z+.055f,true);}ring(moving,.27f,.22f,.55f,.62f,n);}
                case BUILD->{b.column(0,.15f,0,.24f,.42f,.20f,n);for(int x=-1;x<=1;x+=2)b.box(x*.29f-.055f,.15f,-.19f,x*.29f+.055f,.65f,.19f,true);b.column(0,.42f,0,.09f,.65f,.09f,4);moving.box(-.29f,0,-.19f,.29f,.18f,.19f,true);moving.box(-.10f,.06f,-.32f,.10f,.13f,-.16f,true);}
                default->{}
            }
            // Core windows remain attached to each profile's central stem, never free particles/drones.
            if(p!=SupportModelProfile.BUILD)light.column(0,.78f,0,.17f,.84f,.17f,4);
            else light.box(-.07f,.06f,-.325f,.07f,.13f,-.315f,true);
            for(int side=-1;side<=1;side+=2)boost.box(side*.31f-.035f,.151f,-.12f,side*.31f+.035f,.18f,.12f,true);
        }
        if(lod==2){if(p.storage())panel(light,.466f);else light.column(0,.815f,0,.13f,.85f,.13f,4);}
        b.face(-.46f,0,-.46f,.46f,0,-.46f,.46f,0,.46f,-.46f,0,.46f,0,-1,0);
        var raw=b.finish();var body=new float[shell.length+raw.length];System.arraycopy(shell,0,body,0,shell.length);System.arraycopy(raw,0,body,shell.length,raw.length);
        return CACHE[p.ordinal()][size][lod]=new Model(body,lining,moving.finish(),light.finish(),boost.finish());
    }
    private static void panel(MechanicalBlockGeometry.Builder b,float edge){for(int side=0;side<4;side++){float s=side<2?1:-1;if(side%2==0)b.face(s*edge,.28f,-.22f,s*edge,.28f,.22f,s*edge,.69f,.22f,s*edge,.69f,-.22f,s,0,0);else b.face(-.22f,.28f,s*edge,.22f,.28f,s*edge,.22f,.69f,s*edge,-.22f,.69f,s*edge,0,0,s);}}
    private static void ring(MechanicalBlockGeometry.Builder b,float outer,float inner,float low,float high,int n){b.box(-inner,low,-.025f,inner,low+.025f,.025f,true);b.box(-.025f,low,-inner,.025f,low+.025f,inner,true);for(int i=0;i<n;i++){double a=(i+.5)*Math.PI*2/n,c=(i+1.5)*Math.PI*2/n;float ax=(float)Math.cos(a),az=(float)Math.sin(a),cx=(float)Math.cos(c),cz=(float)Math.sin(c);b.face(ax*outer,low,az*outer,cx*outer,low,cz*outer,cx*outer,high,cz*outer,ax*outer,high,az*outer,ax,0,az);b.face(cx*inner,low,cz*inner,ax*inner,low,az*inner,ax*inner,high,az*inner,cx*inner,high,cz*inner,-ax,0,-az);b.face(ax*outer,high,az*outer,cx*outer,high,cz*outer,cx*inner,high,cz*inner,ax*inner,high,az*inner,0,1,0);}}
}
