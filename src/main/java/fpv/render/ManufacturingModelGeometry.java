package fpv.render;

/** Shared immutable mechanical templates; native sprite crops map continuously in local X/Z. */
public final class ManufacturingModelGeometry {
    private ManufacturingModelGeometry(){}
    public record Model(float[] body,float[] lining,float[][] heads,float[][] rods,float[] rotor,float[] heat){}
    private static final Model[][][] CACHE=new Model[ManufacturingModelProfile.values().length][17][3];
    public static Model model(ManufacturingModelProfile p,int cells,int lod){
        if(p==null||cells<1||cells>16||lod<0||lod>2)throw new IllegalArgumentException("Bounded manufacturing profile/footprint/LOD required");
        if(CACHE[p.ordinal()][cells][lod]!=null)return CACHE[p.ordinal()][cells][lod];
        var body=new MechanicalBlockGeometry.Builder();var rotor=new MechanicalBlockGeometry.Builder();var heat=new MechanicalBlockGeometry.Builder();
        float[] shell=new float[0],lining=new float[0];float[][] heads=new float[0][],rods=new float[0][];
        if(lod==2){
            body.box(-.48f,0,-.48f,.48f,.90f,.48f,false);
        }else{
            var ports=ResourcePortGeometry.generate(cells,p.deck,lod==0);shell=ports.shell();lining=ports.lining();
            if(p.press()){
                body.box(-.28f,p.deck,-.32f,.28f,.34f,.32f,true);
                if(p==ManufacturingModelProfile.GRAPHITE){
                    for(int s=-1;s<=1;s+=2)body.box(s*.32f-.06f,p.deck,-.16f,s*.32f+.06f,.92f,.16f,true);
                    body.box(-.40f,.84f,-.17f,.40f,.96f,.17f,true);
                }else{
                    for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)body.box(x*.33f-.055f,p.deck,z*.32f-.055f,x*.33f+.055f,.96f,z*.32f+.055f,true);
                    for(int s=-1;s<=1;s+=2)body.box(-.39f,.84f,s*.22f-.08f,.39f,.97f,s*.22f+.08f,true);
                }
                heads=new float[p.heads][];rods=new float[p.heads][];
                for(int i=0;i<p.heads;i++){
                    float center=p.heads==1?0:(i==0?-.21f:.21f),depth=p.heads==1?.20f:.13f;
                    var head=new MechanicalBlockGeometry.Builder();closedBox(head,-.23f,p.headBottom(),center-depth,.23f,p.headTop(),center+depth);heads[i]=head.finish();
                    var rod=new MechanicalBlockGeometry.Builder();for(int s=-1;s<=1;s+=2)rod.box(s*.14f-.025f,0,center-.025f,s*.14f+.025f,1,center+.025f,false);rods[i]=rod.finish();
                }
            }else if(p==ManufacturingModelProfile.SMELTER){
                body.column(0,p.deck,0,.39f,.85f,.34f,lod==0?8:4);
                vents(body,heat,.42f,.61f);
                if(lod==0)for(int s=-1;s<=1;s+=2)body.box(s*.32f-.05f,.30f,-.26f,s*.32f+.05f,.91f,-.16f,true);
            }else if(p==ManufacturingModelProfile.CRUCIBLE){
                ring(body,.39f,.32f,p.deck,.83f,lod==0?8:4);
                body.box(-.22f,.53f,-.22f,.22f,.57f,.22f,true);heat.face(-.22f,.574f,-.22f,-.22f,.574f,.22f,.22f,.574f,.22f,.22f,.574f,-.22f,0,1,0);
                for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)body.column(x*.33f,p.deck,z*.33f,.065f,.95f,.055f,4);
                vents(body,heat,.39f,.59f);
            }else if(p==ManufacturingModelProfile.PYRATITE){
                ring(body,.39f,.32f,p.deck,.70f,lod==0?8:4);
                body.box(-.22f,.23f,-.22f,.22f,.27f,.22f,true);
                for(int s=-1;s<=1;s+=2)body.box(s*.32f-.045f,p.deck,-.30f,s*.32f+.045f,.86f,-.22f,true);
                body.box(-.36f,.84f,-.31f,.36f,.90f,-.21f,true);
                body.column(0,.27f,0,.035f,.89f,.035f,4);body.box(-.10f,.84f,-.12f,.10f,.94f,.12f,true);
                paddles(rotor,p.rotorHeight());heat.face(-.21f,.274f,-.21f,-.21f,.274f,.21f,.21f,.274f,.21f,.21f,.274f,-.21f,0,1,0);
            }else{
                body.column(0,p.deck,0,.37f,.76f,.37f,lod==0?8:4);
                for(int s=-1;s<=1;s+=2){body.box(s*.40f-.035f,p.deck,-.27f,s*.40f+.035f,.91f,.27f,true);body.column(s*.28f,p.deck,-.28f,.075f,.92f,.065f,4);}
                body.column(0,.76f,0,.04f,.93f,.04f,4);body.box(-.12f,.89f,-.12f,.12f,.97f,.12f,true);
                paddles(rotor,p.rotorHeight());heat.face(-.21f,.775f,-.21f,-.21f,.775f,.21f,.21f,.775f,.21f,.21f,.775f,-.21f,0,1,0);
            }
        }
        body.face(-.49f,0,-.49f,.49f,0,-.49f,.49f,0,.49f,-.49f,0,.49f,0,-1,0);
        float[] extra=body.finish(),all=new float[shell.length+extra.length];System.arraycopy(shell,0,all,0,shell.length);System.arraycopy(extra,0,all,shell.length,extra.length);
        return CACHE[p.ordinal()][cells][lod]=new Model(all,lining,heads,rods,rotor.finish(),heat.finish());
    }
    private static void closedBox(MechanicalBlockGeometry.Builder b,float x0,float y0,float z0,float x1,float y1,float z1){
        b.box(x0,y0,z0,x1,y1,z1,true);b.face(x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,0,-1,0);
    }
    private static void paddles(MechanicalBlockGeometry.Builder b,float y){
        closedBox(b,-.29f,y,-.045f,.29f,y+.055f,.045f);closedBox(b,-.045f,y,-.29f,.045f,y+.055f,.29f);
    }
    private static void ring(MechanicalBlockGeometry.Builder b,float outer,float inner,float low,float high,int sides){
        for(int i=0;i<sides;i++){
            double a=(i+.5)*Math.PI*2/sides,c=(i+1.5)*Math.PI*2/sides;float ax=(float)Math.cos(a),az=(float)Math.sin(a),cx=(float)Math.cos(c),cz=(float)Math.sin(c);
            b.face(ax*outer,low,az*outer,cx*outer,low,cz*outer,cx*outer,high,cz*outer,ax*outer,high,az*outer,ax,0,az);
            b.face(cx*inner,low,cz*inner,ax*inner,low,az*inner,ax*inner,high,az*inner,cx*inner,high,cz*inner,-ax,0,-az);
            b.face(ax*outer,high,az*outer,cx*outer,high,cz*outer,cx*inner,high,cz*inner,ax*inner,high,az*inner,0,1,0);
        }
    }
    private static void vents(MechanicalBlockGeometry.Builder b,MechanicalBlockGeometry.Builder heat,float low,float high){
        for(int s=-1;s<=1;s+=2){
            b.box(-.22f,low-.035f,s*.37f-.025f,.22f,high+.035f,s*.37f+.025f,true);
            b.box(s*.37f-.025f,low-.035f,-.22f,s*.37f+.025f,high+.035f,.22f,true);
            heat.face(-.17f,low,s*.398f,.17f,low,s*.398f,.17f,high,s*.398f,-.17f,high,s*.398f,0,0,s);
            heat.face(s*.398f,low,-.17f,s*.398f,low,.17f,s*.398f,high,.17f,s*.398f,high,-.17f,s,0,0);
        }
    }
}
