package fpv.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import java.util.IdentityHashMap;

/** Opaque paint beneath decorative sprite alpha. Cached host pixels, never a new texture. */
final class MechanicalMaterial {
    private static final IdentityHashMap<TextureRegion,Color> colors=new IdentityHashMap<>();
    private static Object atlas;
    static Color color(TextureRegion region){
        if(atlas!=Core.atlas){colors.clear();atlas=Core.atlas;}
        var old=colors.get(region);if(old!=null)return old;
        var result=new Color(.46f,.48f,.52f,1);
        if(region instanceof TextureAtlas.AtlasRegion ar){
            var pix=Core.atlas.getPixmap(ar);
            if(pix!=null&&!pix.pixmap.isDisposed()){
                // Match the existing machinery's opaque shell swatch, not transparent black.
                double best=Double.POSITIVE_INFINITY;
                for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>=200){
                    double u=x/(double)Math.max(1,pix.width-1),v=y/(double)Math.max(1,pix.height-1),d=(u-.14)*(u-.14)+(v-.14)*(v-.14);
                    if(d<best){best=d;result.set(pix.get(x,y));}
                }
            }
        }
        colors.put(region,result);return result;
    }
}
