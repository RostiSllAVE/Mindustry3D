package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Exact Serpulo scope; recipes and physical bounds remain native. */
public enum ChemicalModelProfile {
    CRYO, MELTER, PRESS, CENTRIFUGE, CULTIVATOR, WATER, OIL, PULVERIZER;
    public static ChemicalModelProfile forBlock(Block b){
        if(b==Blocks.cryofluidMixer)return CRYO;
        if(b==Blocks.melter)return MELTER;
        if(b==Blocks.sporePress)return PRESS;
        if(b==Blocks.coalCentrifuge)return CENTRIFUGE;
        if(b==Blocks.cultivator)return CULTIVATOR;
        if(b==Blocks.waterExtractor)return WATER;
        if(b==Blocks.oilExtractor)return OIL;
        if(b==Blocks.pulverizer)return PULVERIZER;
        return null;
    }
}
