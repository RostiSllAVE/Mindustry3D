package fpv.render;

/** Open assembly bays. +X is the native payload exit. No tall decoration crosses that corridor.
 * Roof UVs use footprint coordinates; all immutable parts are cached per profile and LOD. */
public final class UnitProductionGeometry {
    public static final float DECK=.18f;
    public record Model(float[] body,float[] lining,float[] lights,float[][] carriages,float[] tools){}
    private static final Model[][] CACHE=new Model[UnitProductionProfile.values().length][3];
    private UnitProductionGeometry(){}
    public static Model model(UnitProductionProfile p,int lod){
        if(p==null||lod<0||lod>2)throw new IllegalArgumentException("Factory profile and LOD required");
        var old=CACHE[p.ordinal()][lod];if(old!=null)return old;
        var b=new MechanicalBlockGeometry.Builder();var dark=new MechanicalBlockGeometry.Builder();
        var lights=new MechanicalBlockGeometry.Builder();var tools=new MechanicalBlockGeometry.Builder();
        var left=new MechanicalBlockGeometry.Builder();var right=new MechanicalBlockGeometry.Builder();
        // A shallow shared service foundation, not the old full-height cube.
        if(lod<2){
            var ports=ResourcePortGeometry.generate(3,DECK,lod==0);
            // Keep native ground art on the deck; port mouths are slightly proud of the plinth.
            // The port shell replaces the foundation's side faces below DECK.
            b.face(-.49f,0,-.49f,.49f,0,-.49f,.49f,0,.49f,-.49f,0,.49f,0,-1,0);
            float[] shell=ports.shell(); // combined once below, not an extra per-instance mesh
            dark.box(-.32f,DECK+.001f,-.29f,.44f,DECK+.009f,.29f,true);
            switch(p){
                case GROUND->{
                    for(int s:new int[]{-1,1}){
                        rail(b,-.43f,.40f,s,.30f,.44f,.36f);
                        b.box(-.45f,DECK,s*.355f-.095f,-.24f,.83f,s*.355f+.095f,true);
                        b.box(-.44f,.83f,s*.355f-.08f,-.20f,.94f,s*.355f+.08f,true);
                    }
                    b.box(-.48f,DECK,-.25f,-.36f,.58f,.25f,true);
                    arm(left,-1,.64f,.30f);arm(right,1,.64f,.30f);
                }
                case AIR->{
                    for(int s:new int[]{-1,1}){
                        rail(b,-.39f,.39f,s,.33f,.46f,.30f);
                        b.box(-.40f,DECK,s*.40f-.055f,-.29f,.90f,s*.40f+.055f,true);
                        b.box(-.39f,.89f,s*.40f-.055f,.29f,.99f,s*.40f+.055f,true);
                    }
                    // Rear gantry leaves the +X launch opening and central vertical airspace clear.
                    b.box(-.40f,.87f,-.40f,-.29f,.98f,.40f,true);
                    arm(left,-1,.80f,.37f);arm(right,1,.80f,.37f);
                }
                case NAVAL->{
                    for(int s:new int[]{-1,1}){
                        rail(b,-.47f,.47f,s,.28f,.46f,.43f);
                        b.box(-.46f,.43f,s*.35f-.08f,-.25f,.71f,s*.35f+.08f,true);
                        if(lod==0)for(int i=0;i<3;i++)b.box(-.26f+i*.23f,DECK,s*.255f-.025f,-.20f+i*.23f,.30f,s*.255f+.025f,true);
                    }
                    b.box(-.48f,.70f,-.43f,-.32f,.83f,.43f,true);
                    arm(left,-1,.55f,.29f);arm(right,1,.55f,.29f);
                }
                case TANK->{
                    for(int s:new int[]{-1,1}){
                        rail(b,-.47f,.46f,s,.28f,.47f,.63f);
                        b.box(-.45f,.63f,s*.36f-.095f,-.15f,.88f,s*.36f+.095f,true);
                        if(lod==0)b.box(-.40f,.88f,s*.36f-.06f,-.22f,.98f,s*.36f+.06f,true);
                    }
                    b.box(-.48f,DECK,-.25f,-.38f,.74f,.25f,true);
                    arm(left,-1,.63f,.28f);arm(right,1,.63f,.28f);
                }
                case SHIP->{
                    for(int s:new int[]{-1,1}){
                        b.column(-.27f,DECK,s*.34f,.14f,.84f,.10f,4);
                        b.box(-.36f,.84f,s*.34f-.075f,.29f,.98f,s*.34f+.075f,true);
                        b.column(.34f,DECK,s*.34f,.10f,.48f,.07f,4);
                    }
                    b.column(-.35f,DECK,0,.14f,.53f,.10f,6);
                    arm(left,-1,.78f,.31f);arm(right,1,.78f,.31f);
                }
                case MECH->{
                    for(int s:new int[]{-1,1}){
                        rail(b,-.45f,.44f,s,.31f,.45f,.40f);
                        b.column(-.32f,DECK,s*.34f,.14f,.95f,.10f,4);
                        b.column(.28f,DECK,s*.37f,.11f,.66f,.085f,4);
                        b.box(-.35f,.69f,s*.35f-.06f,.25f,.80f,s*.35f+.06f,true);
                    }
                    b.box(-.46f,.79f,-.35f,-.30f,.90f,.35f,true);
                    arm(left,-1,.66f,.32f);arm(right,1,.66f,.32f);
                }
            }
            for(int s:new int[]{-1,1})lights.box(-.24f,DECK+.035f,s*.27f-.008f,.40f,DECK+.065f,s*.27f+.008f,true);
            if(lod==0)for(int s:new int[]{-1,1})tools.column(0,.45f,s*.24f,.025f,.55f,.018f,4);
            float[] body=b.finish(),all=new float[shell.length+body.length];System.arraycopy(shell,0,all,0,shell.length);System.arraycopy(body,0,all,shell.length,body.length);
            return CACHE[p.ordinal()][lod]=new Model(all,combine(ports.lining(),dark.finish()),lights.finish(),new float[][]{left.finish(),right.finish()},tools.finish());
        }
        // Silhouette-only LOD retains the open exit and a different upper outline for each family.
        b.box(-.49f,0,-.49f,.49f,DECK,.49f,true);
        b.face(-.49f,0,-.49f,.49f,0,-.49f,.49f,0,.49f,-.49f,0,.49f,0,-1,0);
        float high=p==UnitProductionProfile.NAVAL?.63f:p==UnitProductionProfile.TANK?.82f:.96f;
        for(int s:new int[]{-1,1})rail(b,-.43f,p==UnitProductionProfile.AIR||p==UnitProductionProfile.SHIP?.25f:.43f,s,.32f,.46f,high);
        if(p==UnitProductionProfile.MECH||p==UnitProductionProfile.GROUND)b.box(-.46f,DECK,-.3f,-.33f,high*.86f,.3f,true);
        return CACHE[p.ordinal()][lod]=new Model(b.finish(),dark.finish(),lights.finish(),new float[][]{left.finish(),right.finish()},tools.finish());
    }
    private static void rail(MechanicalBlockGeometry.Builder b,float start,float end,int sign,float inner,float outer,float top){
        b.box(start,DECK,sign<0?-outer:inner,end,top,sign<0?-inner:outer,true);
    }
    private static void arm(MechanicalBlockGeometry.Builder b,int sign,float top,float anchor){
        b.box(-.12f,top-.055f,sign<0?-anchor:anchor-.055f,.13f,top+.035f,sign<0?-anchor+.055f:anchor,true);
        b.box(.05f,top-.13f,sign<0?-anchor+.025f:.23f,.13f,top-.045f,sign<0?-.23f:anchor-.025f,true);
        b.column(.09f,top-.18f,sign*.24f,.035f,top-.10f,.035f,4);
    }
    private static float[] combine(float[] a,float[] b){float[] out=new float[a.length+b.length];System.arraycopy(a,0,out,0,a.length);System.arraycopy(b,0,out,a.length,b.length);return out;}
}
