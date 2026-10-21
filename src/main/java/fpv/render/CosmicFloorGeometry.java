package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Tile;
import java.util.function.IntBinaryOperator;

/** Exact native content identity, never map colour/name inference. Visual mask at native Z0 only. */
public final class CosmicFloorGeometry {
    public static boolean designated(Tile tile){return tile!=null&&(tile.floor()==Blocks.space||tile.floor()==Blocks.empty);}
    /** Row-run batching; exact tile edges. Zero means solid/hidden; one means cosmic. */
    public static float[] build(int x,int y,int width,int height,IntBinaryOperator mask){
        float[] v=new float[width*height*18];int n=0;
        for(int yy=y;yy<y+height;yy++)for(int xx=x;xx<x+width;){
            if(mask.applyAsInt(xx,yy)==0){xx++;continue;}
            int start=xx;while(xx<x+width&&mask.applyAsInt(xx,yy)!=0)xx++;
            float l=start-.5f,r=xx-.5f,b=-yy-.5f,t=-yy+.5f;
            float[] quad={l,.002f,b,r,.002f,b,r,.002f,t,l,.002f,b,r,.002f,t,l,.002f,t};
            System.arraycopy(quad,0,v,n,18);n+=18;
        }
        return java.util.Arrays.copyOf(v,n);
    }
    private CosmicFloorGeometry(){}
}
