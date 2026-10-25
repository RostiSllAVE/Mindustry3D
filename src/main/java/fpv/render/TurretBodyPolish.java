package fpv.render;

import mindustry.world.Block;

/** Selected thin native heads get one tapered lower shell; roof, pivot and recoil stay exact. */
public final class TurretBodyPolish {
    public static boolean selected(Block type){return type!=null&&switch(type.name){
        case "arc","lancer","meltdown","wave","tsunami","hail","ripple"->true;default->false;};}
    public static final float EXTRA=.65f;
    private TurretBodyPolish(){}
    public static float[] shell(float[] original){
        if(original==null||original.length%24!=0)throw new IllegalArgumentException("Triangle-aligned native silhouette required");
        int sides=0;for(int i=0;i<original.length;i+=24)if(Math.abs(original[i+4])<.5f)sides+=24;
        float[] result=new float[original.length+sides];int at=0;
        // Existing roof and walls; the old underside is moved to the lower tapered contour.
        for(int i=0;i<original.length;i+=8){System.arraycopy(original,i,result,at,8);
            if(original[i+4]<-.5f){result[at]*=.80f;result[at+1]-=EXTRA;result[at+2]*=.80f;}
            at+=8;
        }
        for(int i=0;i<original.length;i+=24)if(Math.abs(original[i+4])<.5f){
            for(int j=i;j<i+24;j+=8){System.arraycopy(original,j,result,at,8);
                boolean lower=original[j+1]<0;result[at]*=lower?.80f:1;result[at+2]*=lower?.80f:1;
                result[at+1]=lower?-.5f-EXTRA:-.5f;at+=8;
            }
        }
        return result;
    }
}
