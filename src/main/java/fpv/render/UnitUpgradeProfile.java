package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Only the eight vanilla upgrade facilities; no assemblers or third-party overrides. */
public enum UnitUpgradeProfile {
    ADDITIVE, MULTIPLICATIVE, EXPONENTIAL, TETRATIVE, TANK, SHIP, MECH, PRIME;
    public boolean erekir(){return ordinal()>=TANK.ordinal();}
    public static UnitUpgradeProfile forBlock(Block b){
        if(b==null)return null;
        if(b==Blocks.additiveReconstructor)return ADDITIVE;if(b==Blocks.multiplicativeReconstructor)return MULTIPLICATIVE;
        if(b==Blocks.exponentialReconstructor)return EXPONENTIAL;if(b==Blocks.tetrativeReconstructor)return TETRATIVE;
        if(b==Blocks.tankRefabricator)return TANK;if(b==Blocks.shipRefabricator)return SHIP;
        if(b==Blocks.mechRefabricator)return MECH;return b==Blocks.primeRefabricator?PRIME:null;
    }
}
