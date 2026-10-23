package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Explicit visual coverage only. Native core rules, height and capacity remain authoritative. */
public enum CoreInfrastructureProfile {
    SHARD, FOUNDATION, NUCLEUS, BASTION, CITADEL, ACROPOLIS, RADAR;

    public boolean core(){return this!=RADAR;}
    public boolean erekir(){return this==BASTION||this==CITADEL||this==ACROPOLIS;}
    public static CoreInfrastructureProfile forBlock(Block b){
        if(b==null)return null;
        if(b==Blocks.coreShard)return SHARD;if(b==Blocks.coreFoundation)return FOUNDATION;
        if(b==Blocks.coreNucleus)return NUCLEUS;if(b==Blocks.coreBastion)return BASTION;
        if(b==Blocks.coreCitadel)return CITADEL;if(b==Blocks.coreAcropolis)return ACROPOLIS;
        return b==Blocks.radar?RADAR:null;
    }
}
