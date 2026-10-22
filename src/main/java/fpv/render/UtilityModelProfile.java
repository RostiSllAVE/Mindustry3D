package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Exact native utility identities, including both installed Launch Pad variants. */
public enum UtilityModelProfile {
    MINE, LIGHT, LAUNCH, LANDING;
    public static UtilityModelProfile forBlock(Block b){
        if(b==null)return null;
        if(b==Blocks.shockMine)return MINE;
        if(b==Blocks.illuminator)return LIGHT;
        if(b==Blocks.launchPad||b==Blocks.advancedLaunchPad)return LAUNCH;
        if(b==Blocks.landingPad)return LANDING;
        return null;
    }
}
