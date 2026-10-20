package fpv.render;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import mindustry.content.Blocks;
import mindustry.world.*;
import mindustry.world.blocks.environment.OreBlock;
import java.util.IdentityHashMap;

/** CPU templates from host-owned ore sprites; baked into the existing atlas chunk batch. */
public final class OreDepositRenderer {
    public record Profile(float height,float taper){}
    private final IdentityHashMap<TextureRegion,OreDepositGeometry.Template> templates=new IdentityHashMap<>();
    private Object atlas;
    private long builds,raisedTiles,vertices;
    public long builds(){return builds;}public long raisedTiles(){return raisedTiles;}public long vertices(){return vertices;}
    private static final Profile COPPER=new Profile(.13f,.88f),LEAD=new Profile(.16f,.90f),COAL=new Profile(.14f,.84f),
        TITANIUM=new Profile(.18f,.86f),THORIUM=new Profile(.19f,.88f),SCRAP=new Profile(.095f,.93f),GREEN=new Profile(.17f,.87f);
    public static Profile profile(Block ore){
        if(ore==Blocks.oreCopper)return COPPER;
        if(ore==Blocks.oreLead)return LEAD;
        if(ore==Blocks.oreCoal)return COAL;
        if(ore==Blocks.oreTitanium)return TITANIUM;
        if(ore==Blocks.oreThorium||ore==Blocks.oreCrystalThorium)return THORIUM;
        if(ore==Blocks.oreScrap)return SCRAP;
        if(ore==Blocks.oreBeryllium||ore==Blocks.oreTungsten)return GREEN;
        return null;
    }
    boolean append(Tile tile,TextureRegion region,NativeAtlasMesh.Builder batch,FloorSurfaceRelief relief){
        Profile profile=profile(tile.overlay());
        if(profile==null||!(tile.overlay() instanceof OreBlock ore)||ore.wallOre||tile.block()!=Blocks.air||tile.floor().isLiquid||!NativeAtlasMesh.valid(region))return false;
        if(atlas!=Core.atlas){clear();atlas=Core.atlas;}
        OreDepositGeometry.Template template=templates.get(region);
        if(template==null){
            template=read(region);templates.put(region,template);builds++;
        }
        int count=template.patches().size();if(count==0)return false;
        int seed=tile.x*0x1f123bb5+tile.y*0x5f356495+ore.name.hashCode();seed^=seed>>>16;
        int first=Math.floorMod(seed,count),number=Math.min(OreDepositGeometry.MAX_RAISED_PATCHES,count);
        for(int i=0;i<number;i++){
            var patch=template.patches().get((first+i)%count);
            float height=profile.height*(.82f+((seed>>>(i*7))&31)*(.30f/31));
            float taper=profile.taper*(.96f+((seed>>>16)&15)*(.06f/15));
            batch.orePatch(region,patch,tile.x,tile.y,relief,height,taper,seed+i*0x6c8e9cf5);
            vertices+=patch.points()*9;
        }
        raisedTiles++;return true;
    }
    private static OreDepositGeometry.Template read(TextureRegion region){
        try{
            if(!(region instanceof AtlasRegion ar))return new OreDepositGeometry.Template(java.util.List.of());
            var pix=Core.atlas.getPixmap(ar);if(pix.width>256||pix.height>256)return new OreDepositGeometry.Template(java.util.List.of());
            byte[] alpha=new byte[pix.width*pix.height];for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)alpha[y*pix.width+x]=(byte)pix.getA(x,y);
            return OreDepositGeometry.generate(alpha,pix.width,pix.height);
        }catch(RuntimeException unavailable){return new OreDepositGeometry.Template(java.util.List.of());}
    }
    public void clear(){templates.clear();atlas=null;}
}
