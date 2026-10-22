package fpv.render;
import mindustry.content.Blocks;import mindustry.world.Block;
/** Explicit playable support/storage scope; campaign barrier projectors remain in their existing path. */
public enum SupportModelProfile {
    MENDER,MEND,OVERDRIVE,DOME,FORCE,CONTAINER,VAULT,REGEN,BUILD,REINFORCED_CONTAINER,REINFORCED_VAULT;
    public boolean storage(){return this==CONTAINER||this==VAULT||this==REINFORCED_CONTAINER||this==REINFORCED_VAULT;}
    public boolean reinforced(){return this==REINFORCED_CONTAINER||this==REINFORCED_VAULT;}
    public static SupportModelProfile forBlock(Block b){
        if(b==null)return null;
        if(b==Blocks.mender)return MENDER;if(b==Blocks.mendProjector)return MEND;if(b==Blocks.overdriveProjector)return OVERDRIVE;if(b==Blocks.overdriveDome)return DOME;
        if(b==Blocks.forceProjector)return FORCE;if(b==Blocks.container)return CONTAINER;if(b==Blocks.vault)return VAULT;if(b==Blocks.regenProjector)return REGEN;
        if(b==Blocks.buildTower)return BUILD;if(b==Blocks.reinforcedContainer)return REINFORCED_CONTAINER;if(b==Blocks.reinforcedVault)return REINFORCED_VAULT;return null;
    }
}
