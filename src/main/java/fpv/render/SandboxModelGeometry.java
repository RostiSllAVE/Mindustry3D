package fpv.render;

/** Immutable footprint-normalized templates, shared across every building at three LODs. */
public final class SandboxModelGeometry {
    public record Model(float[] body,float[] dark,float[] lights,float[] moving){public int vertices(){return (body.length+dark.length+lights.length+moving.length)/8;}}
    private static final Model[][] CACHE=new Model[SandboxModelProfile.values().length][3];
    public static Model model(SandboxModelProfile p,int lod){
        if(p==null||lod<0||lod>2)throw new IllegalArgumentException("Utility profile and bounded LOD required");
        var old=CACHE[p.ordinal()][lod];if(old!=null)return old;
        var b=new MechanicalBlockGeometry.Builder();var d=new MechanicalBlockGeometry.Builder();var l=new MechanicalBlockGeometry.Builder();var m=new MechanicalBlockGeometry.Builder();
        if(p==SandboxModelProfile.TILED_DISPLAY){
            // Connected native tiles share a coplanar screen, with no raised internal bezel.
            b.box(-.5f,0,-.5f,.5f,.68f,.5f,false);
        }else if(p.belt()||p==SandboxModelProfile.PAYLOAD_SOURCE||p==SandboxModelProfile.PAYLOAD_VOID){
            b.box(-.49f,0,-.49f,.49f,.16f,.49f,true);d.box(-.43f,.161f,-.43f,.43f,.18f,.43f,true);
            if(p.belt()){
                for(int side:new int[]{-1,1}){b.box(-.49f,.16f,side*.46f-.025f,.49f,.29f,side*.46f+.025f,true);
                    l.box(-.38f,.292f,side*.46f-.013f,.38f,.31f,side*.46f+.013f,true);}
                if(lod<2)for(int i=0;i<5;i++){float x=-.36f+i*.18f;b.box(x-.035f,.18f,-.41f,x+.035f,.23f,.41f,true);}
                if(p==SandboxModelProfile.ROUTER){b.column(0,.23f,0,.25f,.28f,.25f,8);m.box(-.29f,.28f,-.025f,.29f,.31f,.025f,true);}
            }else{
                for(int s:new int[]{-1,1}){b.box(-.45f,.16f,s*.41f-.04f,.45f,.64f,s*.41f+.04f,true);l.box(-.32f,.65f,s*.41f-.02f,.32f,.68f,s*.41f+.02f,true);}
            }
        }else if(p.repair()){
            b.column(0,0,0,.48f,.18f,.40f,8);b.column(0,.18f,0,.20f,.56f,.16f,8);
            m.box(-.27f,.56f,-.24f,.29f,.80f,.24f,true);m.box(.27f,.61f,-.09f,.47f,.73f,.09f,true);
            l.box(.47f,.63f,-.075f,.48f,.71f,.075f,true);
            if(p==SandboxModelProfile.REPAIR_TURRET&&lod<2)for(int s:new int[]{-1,1})b.box(-.25f,.18f,s*.34f-.06f,.25f,.51f,s*.34f+.06f,true);
        }else if(p==SandboxModelProfile.DUMMY){
            b.box(-.44f,0,-.44f,.44f,.16f,.44f,true);b.column(0,.16f,0,.13f,.50f,.13f,4);
            b.box(-.31f,.50f,-.10f,.31f,.96f,.10f,true);d.box(-.24f,.56f,-.105f,.24f,.90f,-.10f,true);
            l.box(-.04f,.59f,-.11f,.04f,.87f,-.105f,true);l.box(-.20f,.70f,-.11f,.20f,.76f,-.105f,true);
        }else{
            var ports=ResourcePortGeometry.generate(p.block().size,.25f,lod==0);
            // Familiar resource mouths and one original top, not stacked sprite slabs.
            b.column(0,.25f,0,p==SandboxModelProfile.POWER_SOURCE||p==SandboxModelProfile.POWER_VOID?.23f:.36f,.69f,.31f,lod==2?4:8);
            if(p==SandboxModelProfile.LIQUID_SOURCE||p==SandboxModelProfile.LIQUID_VOID){
                b.column(0,.69f,0,.31f,.90f,.26f,8);l.column(0,.48f,0,.365f,.56f,.365f,8);
            }else if(p==SandboxModelProfile.POWER_SOURCE||p==SandboxModelProfile.POWER_VOID){
                b.column(0,.69f,0,.12f,.98f,.07f,4);l.column(0,.64f,0,.29f,.70f,.29f,8);
            }else{b.box(-.34f,.69f,-.34f,.34f,.83f,.34f,true);l.box(-.17f,.835f,-.17f,.17f,.85f,.17f,true);}
            return CACHE[p.ordinal()][lod]=new Model(join(ports.shell(),b.finish()),ports.lining(),l.finish(),m.finish());
        }
        return CACHE[p.ordinal()][lod]=new Model(b.finish(),d.finish(),l.finish(),m.finish());
    }
    private static float[] join(float[] a,float[] b){var v=java.util.Arrays.copyOf(a,a.length+b.length);System.arraycopy(b,0,v,a.length,b.length);return v;}
}
