package fpv.render;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.content.Blocks;
import mindustry.world.Block;

/** Four normalized native material patterns packed into one owned, repeating mipmapped texture.
 * Host atlas sampling/filter/wrap state is never modified. No host pixels are packaged in the mod. */
final class HorizonDetailTexture {
    static Texture create(){
        Block[] materials={Blocks.sand,Blocks.stone,Blocks.snow,Blocks.water};
        float[][] channels=new float[4][32*32];
        for(int c=0;c<4;c++){
            TextureRegion region=materials[c].variantRegions!=null&&materials[c].variantRegions.length>0?materials[c].variantRegions[0]:materials[c].region;
            PixmapRegion source=null;
            if(region instanceof TextureAtlas.AtlasRegion ar)try{source=Core.atlas.getPixmap(ar);}catch(RuntimeException ignored){}
            float mean=0;
            for(int y=0;y<32;y++)for(int x=0;x<32;x++){
                float value=1;
                if(source!=null&&source.width>0&&source.height>0){int pixel=source.get(x*source.width/32,y*source.height/32);value=((pixel>>>24&255)*.30f+(pixel>>>16&255)*.59f+(pixel>>>8&255)*.11f)/255f;}
                else value=.88f+.24f*(float)HorizonGeometry.hash(x,y+c*37);
                channels[c][y*32+x]=value;mean+=value;
            }
            mean=Math.max(.01f,mean/1024);
            for(int i=0;i<1024;i++)channels[c][i]=Math.max(.55f,Math.min(1.45f,channels[c][i]/mean));
        }
        Pixmap pixels=new Pixmap(32,32);
        for(int y=0;y<32;y++)for(int x=0;x<32;x++){
            int rgba=0;for(int c=0;c<4;c++)rgba=rgba<<8|Math.round((channels[c][y*32+x]-.5f)*255);
            pixels.set(x,y,rgba);
        }
        Texture texture=new Texture(pixels,true);pixels.dispose();
        texture.setFilter(Texture.TextureFilter.mipMapLinearLinear,Texture.TextureFilter.nearest);
        texture.setWrap(Texture.TextureWrap.repeat,Texture.TextureWrap.repeat);return texture;
    }
}
