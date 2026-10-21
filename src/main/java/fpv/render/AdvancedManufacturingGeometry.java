package fpv.render;

/** Cached local machinery, using the accepted mechanical/port primitives and atlas stream. */
public final class AdvancedManufacturingGeometry {
    private AdvancedManufacturingGeometry(){}
    public record Model(float[] body,float[] lining,float[] rotor,float[] slider,float[] rods,float[] energy){}
    private static final Model[][][] CACHE=new Model[AdvancedManufacturingProfile.values().length][17][3];
    public static Model model(AdvancedManufacturingProfile p,int cells,int lod){
        if(p==null||cells<1||cells>16||lod<0||lod>2)throw new IllegalArgumentException("Bounded manufacturing profile/size/LOD required");
        if(CACHE[p.ordinal()][cells][lod]!=null)return CACHE[p.ordinal()][cells][lod];
        var body=new MechanicalBlockGeometry.Builder();var rotor=new MechanicalBlockGeometry.Builder();var slider=new MechanicalBlockGeometry.Builder();var rods=new MechanicalBlockGeometry.Builder();var energy=new MechanicalBlockGeometry.Builder();
        float[] shell=new float[0],lining=new float[0];
        if(lod==2)body.box(-.48f,0,-.48f,.48f,.91f,.48f,false);
        else{
            var ports=ResourcePortGeometry.generate(cells,p.deck,lod==0);shell=ports.shell();lining=ports.lining();
            switch(p){
                case KILN->{
                    // A chamfered tunnel furnace with framed mouths, rather than a vertical drum.
                    body.box(-.36f,p.deck,-.31f,-.23f,.65f,.31f,true);body.box(.23f,p.deck,-.31f,.36f,.65f,.31f,true);
                    body.box(-.24f,.64f,-.31f,.24f,.83f,.31f,true);
                    body.face(-.36f,.65f,-.31f,-.36f,.65f,.31f,-.24f,.83f,.31f,-.24f,.83f,-.31f,-1,1,0);
                    body.face(.24f,.83f,-.31f,.24f,.83f,.31f,.36f,.65f,.31f,.36f,.65f,-.31f,1,1,0);
                    for(int s=-1;s<=1;s+=2){body.box(-.24f,p.deck,s*.31f-.025f,.24f,.35f,s*.31f+.025f,true);panel(energy,-.22f,.36f,s*.312f,.22f,.62f,s);}
                    if(lod==0)body.column(.27f,.65f,-.19f,.07f,.97f,.055f,4);
                }
                case COMPRESSOR->{
                    body.box(-.19f,p.deck,-.22f,.19f,.39f,.22f,true);
                    for(int s=-1;s<=1;s+=2){body.box(s*.36f-.08f,p.deck,-.22f,s*.36f+.08f,.76f,.22f,true);for(int z=-1;z<=1;z+=2)body.box(s*.34f-.045f,p.deck,z*.31f-.04f,s*.34f+.045f,.94f,z*.31f+.04f,true);}
                    for(int z=-1;z<=1;z+=2)body.box(-.39f,.86f,z*.31f-.045f,.39f,.95f,z*.31f+.045f,true);
                    closed(slider,-.29f,.45f,-.18f,-.17f,.68f,.18f);
                    for(int z=-1;z<=1;z+=2)rods.box(0,.54f-.02f,z*.12f-.02f,1,.54f+.02f,z*.12f+.02f,false);
                    energy.box(-.045f,.40f,-.16f,.045f,.65f,.16f,true);
                }
                case WEAVER->{
                    for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)body.box(x*.34f-.04f,p.deck,z*.31f-.04f,x*.34f+.04f,.92f,z*.31f+.04f,true);
                    for(int z=-1;z<=1;z+=2)body.box(-.38f,.85f,z*.31f-.045f,.38f,.94f,z*.31f+.045f,true);
                    body.box(-.32f,p.deck,-.27f,.32f,.35f,.27f,true);
                    closed(slider,-.035f,.61f,-.26f,.035f,.68f,.26f);
                    for(int z=-1;z<=1;z+=2)energy.box(-.28f,.53f,z*.17f-.008f,.28f,.543f,z*.17f+.008f,true);
                    rotor.column(0,.38f,0,.22f,.43f,.22f,lod==0?8:4);
                }
                case SURGE->{
                    body.box(-.26f,p.deck,-.29f,.26f,.40f,.29f,true);
                    for(int x=-1;x<=1;x+=2){body.box(x*.32f-.07f,p.deck,-.32f,x*.32f+.07f,.77f,.32f,true);body.column(x*.32f,.72f,-.24f,.075f,.97f,.06f,lod==0?6:4);}
                    body.box(-.29f,.38f,-.36f,.29f,.87f,-.28f,true);
                    for(int x=-1;x<=1;x+=2)body.box(x*.22f-.035f,p.deck,.29f,x*.22f+.035f,.79f,.36f,true);
                    body.box(-.24f,.70f,.28f,.24f,.79f,.36f,true);
                    panel(energy,-.17f,.43f,.337f,.17f,.67f,1);
                    energy.face(-.23f,.405f,-.25f,-.23f,.405f,.25f,.23f,.405f,.25f,.23f,.405f,-.25f,0,1,0);
                    if(lod==0)for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)body.box(x*.415f-.02f,.32f,z*.19f-.055f,x*.415f+.02f,.67f,z*.19f+.055f,true);
                }
                case SEPARATOR->{
                    ring(body,.37f,.30f,p.deck,.59f,lod==0?8:4);
                    body.column(0,p.deck,0,.055f,.83f,.055f,4);
                    for(int x=-1;x<=1;x+=2)body.box(x*.32f-.04f,p.deck,-.29f,x*.32f+.04f,.80f,-.21f,true);
                    body.box(-.36f,.77f,-.29f,.36f,.85f,-.21f,true);
                    for(int i=0;i<3;i++){double a=i*Math.PI*2/3;float x=(float)Math.cos(a)*.16f,z=(float)Math.sin(a)*.16f;closed(rotor,x-.06f,.62f,z-.06f,x+.06f,.70f,z+.06f);}
                    energy.face(-.20f,.31f,-.20f,-.20f,.31f,.20f,.20f,.31f,.20f,.20f,.31f,-.20f,0,1,0);
                }
                case DISASSEMBLER->{
                    body.box(-.34f,p.deck,-.28f,.34f,.38f,.28f,true);
                    for(int x=-1;x<=1;x+=2){
                        body.box(x*.39f-.035f,p.deck,-.34f,x*.39f+.035f,.47f,.34f,true);
                        body.box(x*.39f-.035f,.78f,-.34f,x*.39f+.035f,.85f,.34f,true);
                        for(int z=-1;z<=1;z+=2)body.box(x*.39f-.035f,.47f,z*.30f-.04f,x*.39f+.035f,.78f,z*.30f+.04f,true);
                    }
                    for(int z=-1;z<=1;z+=2){body.box(-.37f,.82f,z*.34f-.045f,.37f,.88f,z*.34f+.045f,true);for(int x=-1;x<=1;x+=2){body.box(x*.18f-.09f,p.deck,z*.34f-.045f,x*.18f+.09f,.83f,z*.34f+.045f,true);body.column(x*.18f,.83f,z*.34f,.065f,.96f,.055f,4);}}
                    // One shared axial drum; end faces are embedded in the bearing guards.
                    rotor.column(0,0,0,.13f,1,.13f,lod==0?8:6);
                    for(int x=-1;x<=1;x+=2)rotor.box(x*.13f-.025f,0,-.035f,x*.13f+.025f,1,.035f,true);
                    energy.face(-.27f,.385f,-.24f,-.27f,.385f,.24f,.27f,.385f,.24f,.27f,.385f,-.24f,0,1,0);
                }
            }
        }
        body.face(-.49f,0,-.49f,.49f,0,-.49f,.49f,0,.49f,-.49f,0,.49f,0,-1,0);
        var r=rotor.finish();if(p==AdvancedManufacturingProfile.DISASSEMBLER&&lod<2)for(int i=0;i<r.length;i+=8){float y=r[i+1],ny=r[i+4];r[i+1]=r[i+2];r[i+2]=(y-.5f)*.54f;r[i+4]=r[i+5];r[i+5]=ny;}
        var rod=rods.finish();for(int i=0;i<rod.length;i+=8){rod[i+6]=-1;rod[i+7]=-1;} // Stretched rods sample a material texel, never UVs outside the atlas region.
        var b=body.finish();var all=new float[shell.length+b.length];System.arraycopy(shell,0,all,0,shell.length);System.arraycopy(b,0,all,shell.length,b.length);
        return CACHE[p.ordinal()][cells][lod]=new Model(all,lining,r,slider.finish(),rod,energy.finish());
    }
    private static void closed(MechanicalBlockGeometry.Builder b,float x0,float y0,float z0,float x1,float y1,float z1){b.box(x0,y0,z0,x1,y1,z1,true);b.face(x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,0,-1,0);}
    private static void panel(MechanicalBlockGeometry.Builder b,float x0,float y0,float z,float x1,float y1,int normal){b.face(x0,y0,z,x1,y0,z,x1,y1,z,x0,y1,z,0,0,normal);}
    private static void ring(MechanicalBlockGeometry.Builder b,float outer,float inner,float low,float high,int n){for(int i=0;i<n;i++){double a=(i+.5)*Math.PI*2/n,c=(i+1.5)*Math.PI*2/n;float ax=(float)Math.cos(a),az=(float)Math.sin(a),cx=(float)Math.cos(c),cz=(float)Math.sin(c);
        b.face(ax*outer,low,az*outer,cx*outer,low,cz*outer,cx*outer,high,cz*outer,ax*outer,high,az*outer,ax,0,az);b.face(cx*inner,low,cz*inner,ax*inner,low,az*inner,ax*inner,high,az*inner,cx*inner,high,cz*inner,-ax,0,-az);b.face(ax*outer,high,az*outer,cx*outer,high,cz*outer,cx*inner,high,cz*inner,ax*inner,high,az*inner,0,1,0);
    }}
}
