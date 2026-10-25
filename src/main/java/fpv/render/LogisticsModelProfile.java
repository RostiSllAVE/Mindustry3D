package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Explicit vanilla coverage. No name/size heuristics opt unrelated logistics into a model. */
public enum LogisticsModelProfile {
    CONVEYOR(true,false), TITANIUM(true,false), PLASTANIUM(true,true), ARMORED(true,true),
    JUNCTION(false,false), ROUTER(false,false), DISTRIBUTOR(false,false), SORTER(false,false), INVERTED_SORTER(false,false),
    OVERFLOW(false,false), UNDERFLOW(false,false);
    public final boolean belt,heavy;
    LogisticsModelProfile(boolean belt,boolean heavy){this.belt=belt;this.heavy=heavy;}
    public static LogisticsModelProfile forBlock(Block b){
        if(b==null)return null;
        if(b==Blocks.conveyor)return CONVEYOR;if(b==Blocks.titaniumConveyor)return TITANIUM;
        if(b==Blocks.plastaniumConveyor)return PLASTANIUM;if(b==Blocks.armoredConveyor)return ARMORED;
        if(b==Blocks.junction)return JUNCTION;if(b==Blocks.router)return ROUTER;if(b==Blocks.distributor)return DISTRIBUTOR;
        if(b==Blocks.sorter)return SORTER;if(b==Blocks.invertedSorter)return INVERTED_SORTER;
        if(b==Blocks.overflowGate)return OVERFLOW;if(b==Blocks.underflowGate)return UNDERFLOW;return null;
    }
    public static int lod(float distanceSquared){distanceSquared=RenderDistanceSettings.normalizedSquared(distanceSquared);return distanceSquared>80*80?2:distanceSquared>25*25?1:0;}
}
