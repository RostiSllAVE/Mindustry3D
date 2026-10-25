package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Explicit utility scope. Native block definitions and physical envelopes stay unchanged. */
public enum SandboxModelProfile {
    ITEM_SOURCE,ITEM_VOID,LIQUID_SOURCE,LIQUID_VOID,POWER_SOURCE,POWER_VOID,PAYLOAD_SOURCE,PAYLOAD_VOID,
    REPAIR_POINT,REPAIR_TURRET,CONVEYOR,ROUTER,TILED_DISPLAY,DUMMY;
    private static final SandboxModelProfile[] ALL=values();
    public Block block(){return switch(this){
        case ITEM_SOURCE->Blocks.itemSource;case ITEM_VOID->Blocks.itemVoid;case LIQUID_SOURCE->Blocks.liquidSource;case LIQUID_VOID->Blocks.liquidVoid;
        case POWER_SOURCE->Blocks.powerSource;case POWER_VOID->Blocks.powerVoid;case PAYLOAD_SOURCE->Blocks.payloadSource;case PAYLOAD_VOID->Blocks.payloadVoid;
        case REPAIR_POINT->Blocks.repairPoint;case REPAIR_TURRET->Blocks.repairTurret;case CONVEYOR->Blocks.payloadConveyor;case ROUTER->Blocks.payloadRouter;
        case TILED_DISPLAY->Blocks.tileLogicDisplay;case DUMMY->Blocks.targetDummy;};}
    public static SandboxModelProfile forBlock(Block b){if(b==null)return null;for(var p:ALL)if(p.block()==b)return p;return null;}
    public boolean belt(){return this==CONVEYOR||this==ROUTER;}
    public boolean repair(){return this==REPAIR_POINT||this==REPAIR_TURRET;}
    public boolean sink(){return this==ITEM_VOID||this==LIQUID_VOID||this==POWER_VOID||this==PAYLOAD_VOID;}
}
