package fpv.render;

/** Readable upgrade bays, open at both native input (-X) and output (+X).
 * Body, tools and port geometry are immutable/shared; drawing only transforms cached parts. */
public final class UnitUpgradeGeometry {
    public record Model(float[] body,float[] lining,float[] lights,float[][] carriages,float[] tools){}
    private static final Model[][][] CACHE=new Model[UnitUpgradeProfile.values().length][17][3];
    private UnitUpgradeGeometry(){}
    public static Model model(UnitUpgradeProfile p,int size,int lod){
        if(p==null||size<1||size>16||lod<0||lod>2)throw new IllegalArgumentException("Bounded upgrade model required");
        var old=CACHE[p.ordinal()][size][lod];if(old!=null)return old;
        var b=new MechanicalBlockGeometry.Builder();var dark=new MechanicalBlockGeometry.Builder();
        var lights=new MechanicalBlockGeometry.Builder();var left=new MechanicalBlockGeometry.Builder();
        var right=new MechanicalBlockGeometry.Builder();var tools=new MechanicalBlockGeometry.Builder();float deck=UnitAssemblyParts.DECK;
        float[] shell=new float[0],lining=new float[0];
        if(lod<2){
            var ports=ResourcePortGeometry.generate(size,deck,lod==0);shell=ports.shell();lining=ports.lining();
            dark.box(-.48f,deck+.001f,-.285f,.48f,UnitAssemblyParts.BAY,.285f,true);
        }else b.box(-.49f,0,-.49f,.49f,deck,.49f,true);
        b.face(-.49f,0,-.49f,.49f,0,-.49f,.49f,0,.49f,-.49f,0,.49f,0,-1,0);
        float inner=inner(p),bank=bank(p);
        for(int s:new int[]{-1,1})b.box(-.47f,deck,s<0?-.48f:inner,.47f,bank,s<0?-inner:.48f,true);
        if(lod<2){
            switch(p){
                case ADDITIVE->{
                    posts(b,.36f,.38f,.065f,.86f,4);
                    beam(b,0,.84f,.94f,.045f,.41f);
                }
                case MULTIPLICATIVE->{
                    posts(b,.36f,.40f,.075f,.88f,4);
                    for(int s:new int[]{-1,1})beam(b,s*.22f,.88f,.97f,.035f,.41f);
                    if(lod==0)for(int s:new int[]{-1,1})b.column(0,bank,s*.39f,.08f,.77f,.06f,8);
                }
                case EXPONENTIAL->{
                    cornerBanks(b,.25f,.38f,.10f,.83f);
                    sideTracks(b,.40f,.84f,.97f);
                    beam(b,0,.86f,.97f,.055f,.42f);
                    if(lod==0)for(int s:new int[]{-1,1})b.column(0,bank,s*.405f,.065f,.78f,.05f,6);
                }
                case TETRATIVE->{
                    cornerBanks(b,.25f,.38f,.10f,.88f);
                    sideTracks(b,.41f,.88f,.99f);
                    for(int s:new int[]{-1,1})beam(b,s*.20f,.88f,.98f,.045f,.42f);
                    if(lod==0)for(int s:new int[]{-1,1}){
                        b.box(-.20f,bank,s*.405f-.065f,.20f,.73f,s*.405f+.065f,true);
                        b.box(-.14f,.73f,s*.405f-.04f,.14f,.82f,s*.405f+.04f,true);
                    }
                }
                case TANK->{
                    cornerBanks(b,.27f,.39f,.085f,.85f);
                    for(int s:new int[]{-1,1})b.box(-.40f,.85f,s*.39f-.065f,.40f,.94f,s*.39f+.065f,true);
                    if(lod==0)for(int s:new int[]{-1,1})b.column(0,bank,s*.39f,.065f,.75f,.05f,4);
                }
                case SHIP->{
                    posts(b,.35f,.40f,.075f,.88f,4);sideTracks(b,.40f,.88f,.99f);
                    beam(b,0,.87f,.97f,.035f,.41f);
                }
                case MECH->{
                    posts(b,.35f,.36f,.115f,.91f,4);
                    for(int s:new int[]{-1,1})b.box(-.36f,.81f,s*.37f-.045f,.36f,.91f,s*.37f+.045f,true);
                    if(lod==0)for(int s:new int[]{-1,1})b.box(-.08f,bank,s*.39f-.06f,.08f,.68f,s*.39f+.06f,true);
                }
                case PRIME->{
                    cornerBanks(b,.27f,.39f,.09f,.94f);sideTracks(b,.40f,.94f,.99f);
                    for(int s:new int[]{-1,1})beam(b,s*.21f,.88f,.98f,.04f,.42f);
                    if(lod==0)for(int s:new int[]{-1,1})b.column(0,bank,s*.40f,.07f,.84f,.05f,4);
                }
            }
            UnitAssemblyParts.railLight(lights,inner-.002f,bank);
            UnitAssemblyParts.arm(left,-1,toolTop(p),.27f,0);UnitAssemblyParts.arm(right,1,toolTop(p),.27f,0);
            if(lod==0)for(int s:new int[]{-1,1})UnitAssemblyParts.nozzle(tools,s,toolTop(p),0);
        }else{
            // Different upper silhouettes survive at distance; the two-way corridor stays open.
            float height=p==UnitUpgradeProfile.ADDITIVE?.86f:p==UnitUpgradeProfile.MECH?.91f:.98f;
            for(int s:new int[]{-1,1})b.box(-.40f,bank,s*.39f-.045f,.40f,height,s*.39f+.045f,true);
            if(p==UnitUpgradeProfile.MULTIPLICATIVE||p==UnitUpgradeProfile.TETRATIVE||p==UnitUpgradeProfile.PRIME)
                beam(b,0,height-.09f,height,.04f,.40f);
        }
        return CACHE[p.ordinal()][size][lod]=new Model(join(shell,b.finish()),join(lining,dark.finish()),lights.finish(),new float[][]{left.finish(),right.finish()},tools.finish());
    }
    static float toolTop(UnitUpgradeProfile p){return switch(p){case ADDITIVE->.69f;case MULTIPLICATIVE->.76f;case EXPONENTIAL->.78f;case TETRATIVE->.80f;case TANK->.72f;case SHIP->.80f;case MECH->.69f;case PRIME->.79f;};}
    private static float inner(UnitUpgradeProfile p){return p==UnitUpgradeProfile.SHIP?.33f:p==UnitUpgradeProfile.MECH||p==UnitUpgradeProfile.TETRATIVE?.31f:.30f;}
    private static float bank(UnitUpgradeProfile p){return switch(p){case ADDITIVE->.52f;case MULTIPLICATIVE->.55f;case EXPONENTIAL->.56f;case TETRATIVE->.54f;case TANK->.56f;case SHIP->.37f;case MECH->.49f;case PRIME->.62f;};}
    private static void posts(MechanicalBlockGeometry.Builder b,float x,float z,float radius,float high,int n){
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1})b.column(sx*x,UnitAssemblyParts.DECK,sz*z,radius,high,radius*.82f,n);
    }
    private static void cornerBanks(MechanicalBlockGeometry.Builder b,float inset,float z,float radius,float high){
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1})b.box(sx<0?-.47f:inset,UnitAssemblyParts.DECK,sz*z-radius,sx<0?-inset:.47f,high,sz*z+radius,true);
    }
    private static void sideTracks(MechanicalBlockGeometry.Builder b,float z,float low,float high){for(int s:new int[]{-1,1})b.box(-.44f,low,s*z-.045f,.44f,high,s*z+.045f,true);}
    private static void beam(MechanicalBlockGeometry.Builder b,float x,float low,float high,float half,float span){b.box(x-half,low,-span,x+half,high,span,true);}
    private static float[] join(float[] a,float[] b){var out=new float[a.length+b.length];System.arraycopy(a,0,out,0,a.length);System.arraycopy(b,0,out,a.length,b.length);return out;}
}
