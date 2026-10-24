package fpv.render;

/** Shared, presentation-only assembly surfaces and mounted tools. +X is native output. */
final class UnitAssemblyParts {
    static final float DECK=.18f,BAY=.189f;
    // World-space clearance stays positive for small factories AND large reconstructors.
    static float surface(float base,float height){return base+height*BAY+.025f;}
    static void arm(MechanicalBlockGeometry.Builder b,int sign,float top,float anchor,float x){
        b.box(x-.12f,top-.055f,sign<0?-anchor:anchor-.055f,x+.13f,top+.035f,sign<0?-anchor+.055f:anchor,true);
        b.box(x+.05f,top-.13f,sign<0?-anchor+.025f:.23f,x+.13f,top-.045f,sign<0?-.23f:anchor-.025f,true);
        b.column(x+.09f,top-.18f,sign*.24f,.035f,top-.10f,.035f,4);
    }
    static void nozzle(MechanicalBlockGeometry.Builder b,int sign,float top,float x){
        // Its upper end overlaps the metal shank; it cannot float when the carriage moves.
        b.column(x+.09f,top-.195f,sign*.24f,.025f,top-.177f,.023f,4);
    }
    static void railLight(MechanicalBlockGeometry.Builder b,float inner,float high){
        for(int s:new int[]{-1,1})b.box(-.10f,high-.055f,s*inner-.002f,.35f,high-.035f,s*inner+.002f,true);
    }
}
