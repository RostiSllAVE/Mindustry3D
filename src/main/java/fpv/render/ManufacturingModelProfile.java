package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Explicit vanilla coverage; all dimensions are cosmetic fractions of the existing bounds. */
public enum ManufacturingModelProfile {
    GRAPHITE(.22f,.18f,1), MULTI(.24f,.17f,2),
    SMELTER(.22f,0,0), CRUCIBLE(.25f,0,0), PYRATITE(.20f,0,0), BLAST(.23f,0,0);
    public final float deck,stroke;public final int heads;
    ManufacturingModelProfile(float deck,float stroke,int heads){this.deck=deck;this.stroke=stroke;this.heads=heads;}
    public boolean press(){return heads>0;}
    public boolean furnace(){return this==SMELTER||this==CRUCIBLE;}
    public boolean mixer(){return this==PYRATITE||this==BLAST;}
    public float headBottom(){return this==MULTI?.55f:.52f;}
    public float headTop(){return this==MULTI?.66f:.64f;}
    public float rotorHeight(){return this==PYRATITE?.44f:.81f;}
    public static ManufacturingModelProfile forBlock(Block b){
        if(b==Blocks.graphitePress)return GRAPHITE;if(b==Blocks.multiPress)return MULTI;
        if(b==Blocks.siliconSmelter)return SMELTER;if(b==Blocks.siliconCrucible)return CRUCIBLE;
        if(b==Blocks.pyratiteMixer)return PYRATITE;if(b==Blocks.blastMixer)return BLAST;return null;
    }
    /** Heavy mechanical detail near the player; native identifying art survives every LOD. */
    public static int lod(float distanceSquared){return distanceSquared<18*18?0:distanceSquared<50*50?1:2;}
}
