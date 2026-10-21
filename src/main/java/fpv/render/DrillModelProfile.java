package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Cosmetic ratios inside the existing footprint/height; not mining or collision data. */
public enum DrillModelProfile {
    MECHANICAL(.19f,.67f,.83f,.13f,.025f),
    PNEUMATIC(.24f,.69f,.85f,.14f,.050f),
    LASER(.22f,.73f,.87f,.12f,.016f),
    AIRBLAST(.29f,.72f,.87f,.15f,.035f);

    public final float deck,rotor,cap,capDepth,stroke;
    DrillModelProfile(float deck,float rotor,float cap,float capDepth,float stroke){
        this.deck=deck;this.rotor=rotor;this.cap=cap;this.capDepth=capDepth;this.stroke=stroke;
    }
    public static DrillModelProfile forBlock(Block block){
        if(block==null)return null;
        if(block==Blocks.mechanicalDrill)return MECHANICAL;
        if(block==Blocks.pneumaticDrill)return PNEUMATIC;
        if(block==Blocks.laserDrill)return LASER;
        if(block==Blocks.blastDrill)return AIRBLAST;
        return null;
    }
    /** Full machinery nearby, reduced support frame at medium range, compact body beyond 80 tiles. */
    public static int lod(float distanceSquared){return distanceSquared<28*28?0:distanceSquared<80*80?1:2;}
}
