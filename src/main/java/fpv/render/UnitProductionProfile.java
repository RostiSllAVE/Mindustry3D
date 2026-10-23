package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Explicit vanilla coverage; production, payload and placement rules belong to the host. */
public enum UnitProductionProfile {
    GROUND, AIR, NAVAL, TANK, SHIP, MECH;
    public boolean erekir(){return ordinal()>=TANK.ordinal();}
    public static UnitProductionProfile forBlock(Block b){
        if(b==null)return null;
        if(b==Blocks.groundFactory)return GROUND;if(b==Blocks.airFactory)return AIR;
        if(b==Blocks.navalFactory)return NAVAL;if(b==Blocks.tankFabricator)return TANK;
        if(b==Blocks.shipFabricator)return SHIP;return b==Blocks.mechFabricator?MECH:null;
    }
}
