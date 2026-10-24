package fpv.render;

/** Shared low-poly computing modules and an inclined, square-in-surface-space screen. */
public final class LogicModelGeometry {
    public record Model(float[] body,float[] lights,float[] controlOn,float[] controlOff){}
    private static final Model[][] CACHE=new Model[LogicModelProfile.values().length][3];
    private LogicModelGeometry(){}
    public static float pitch(LogicModelProfile p){
        float h=fpv.net.HeadlessGeometry.heightFor(p.block()),w=p.block().size*.96f;
        return p.display()?Math.min((float)Math.toRadians(22),(float)Math.asin(Math.min(.9f,h*.38f/w))):0;
    }
    /** Normalized Y on the solid sloping console roof; normalized Z is horizontal. */
    static float roofY(LogicModelProfile p,float z){return .72f-z*p.block().size/fpv.net.HeadlessGeometry.heightFor(p.block())*(float)Math.tan(pitch(p));}
    static float screenWidth(LogicModelProfile p){var d=(mindustry.world.blocks.logic.LogicDisplay)p.block();return Math.min(d.size*.86f,d.displaySize*d.scaleFactor/(4f*mindustry.Vars.tilesize));}
    public static Model model(LogicModelProfile p,int lod){
        if(p==null||lod<0||lod>2)throw new IllegalArgumentException("Native logic profile and LOD required");
        var old=CACHE[p.ordinal()][lod];if(old!=null)return old;
        var b=new MechanicalBlockGeometry.Builder();var lights=new MechanicalBlockGeometry.Builder();
        var on=new MechanicalBlockGeometry.Builder();var off=new MechanicalBlockGeometry.Builder();
        b.box(-.49f,0,-.49f,.49f,.15f,.49f,true);
        b.face(-.49f,0,-.49f,.49f,0,-.49f,.49f,0,.49f,-.49f,0,.49f,0,-1,0);
        if(p.display()){
            float d=.48f*(float)Math.cos(pitch(p)),north=roofY(p,-d),south=roofY(p,d);
            // A real bezel opening, not a complete metal roof behind coplanar live pixels.
            // This also avoids precision-dependent z-fighting on 16-bit depth targets.
            float half=screenWidth(p)/p.block().size*.5f,inner=half*(float)Math.cos(pitch(p));
            roof(b,p,-.48f,-d,.48f,-inner);roof(b,p,-.48f,inner,.48f,d);
            roof(b,p,-.48f,-inner,-half,inner);roof(b,p,half,-inner,.48f,inner);
            b.face(-.48f,.15f,-d,.48f,.15f,-d,.48f,north,-d,-.48f,north,-d,0,0,-1);
            b.face(.48f,.15f,d,-.48f,.15f,d,-.48f,south,d,.48f,south,d,0,0,1);
            b.face(-.48f,.15f,d,-.48f,.15f,-d,-.48f,north,-d,-.48f,south,d,-1,0,0);
            b.face(.48f,.15f,-d,.48f,.15f,d,.48f,south,d,.48f,north,-d,1,0,0);
            if(lod<2){for(int s:new int[]{-1,1})b.box(s*.46f-.015f,.15f,-.46f,s*.46f+.015f,north-.03f,-.36f,true);}
        }else if(p.processor()){
            float radius=p==LogicModelProfile.MICRO?.30f:p==LogicModelProfile.PROCESSOR?.32f:.34f;
            b.column(0,.15f,0,radius,.70f,radius*.96f,lod==2?4:8);
            b.box(-.35f,.70f,-.35f,.35f,.80f,.35f,true);
            if(lod<2){
                int modules=p.ordinal()+1;
                for(int side:new int[]{-1,1})for(int i=0;i<modules;i++){
                    float z=-.27f+(i+.5f)*.54f/modules;
                    b.box(side<0?-.47f:.37f,.16f,z-.065f,side<0?-.37f:.47f,.72f+i*.065f,z+.065f,true);
                    lights.box(side*.475f-.002f,.49f,z-.038f,side*.475f+.002f,.55f,z+.038f,true);
                }
                if(p==LogicModelProfile.HYPER)for(int s:new int[]{-1,1})b.box(-.28f,.81f,s*.425f-.035f,.28f,.98f,s*.425f+.035f,true);
            }
            lights.box(-.12f,.803f,-.12f,.12f,.814f,.12f,true);
        }else if(p==LogicModelProfile.CELL||p==LogicModelProfile.BANK){
            int racks=p==LogicModelProfile.CELL?2:3;
            for(int i=0;i<racks;i++){
                float x=-.32f+i*.64f/(racks-1);
                b.box(x-.115f,.15f,-.36f,x+.115f,.88f,.36f,true);
                if(lod<2)for(int k=0;k<(p==LogicModelProfile.CELL?3:5);k++){
                    float y=.28f+k*.10f;
                    b.box(x-.12f,y,-.39f,x+.12f,y+.035f,-.36f,true);
                    lights.box(x-.075f,y,.361f,x+.075f,y+.024f,.366f,true);
                }
            }
            b.box(-.43f,.89f,-.41f,.43f,.97f,.41f,true);
        }else if(p==LogicModelProfile.SWITCH){
            b.box(-.40f,.15f,-.40f,.40f,.26f,.40f,true);
            b.column(0,.26f,0,.14f,.48f,.12f,8);
            lever(on,-1);lever(off,1);
            lights.box(-.23f,.263f,.27f,.23f,.28f,.33f,true);
        }else{
            b.box(-.42f,.15f,-.32f,.42f,.72f,.32f,true);
            b.box(-.46f,.72f,-.38f,.46f,.94f,.38f,true);
            if(lod<2)for(int s:new int[]{-1,1})b.box(s*.38f-.035f,.18f,-.43f,s*.38f+.035f,.78f,-.32f,true);
        }
        return CACHE[p.ordinal()][lod]=new Model(b.finish(),lights.finish(),on.finish(),off.finish());
    }
    private static void lever(MechanicalBlockGeometry.Builder b,int direction){
        float dx=direction*.18f;
        b.box(Math.min(0,dx)-.055f,.44f,-.06f,Math.max(0,dx)+.055f,.51f,.06f,true);
        b.box(dx-.09f,.51f,-.18f,dx+.09f,.70f,.18f,true);
    }
    private static void roof(MechanicalBlockGeometry.Builder b,LogicModelProfile p,float x0,float z0,float x1,float z1){
        float y0=roofY(p,z0),y1=roofY(p,z1);b.face(x0,y0,z0,x0,y1,z1,x1,y1,z1,x1,y0,z0,0,1,0);
    }
}
