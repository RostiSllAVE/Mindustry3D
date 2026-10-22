package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Exact installed Serpulo scope. Heights are visual envelopes, never collision/placement data. */
public enum LiquidModelProfile {
    CONDUIT(.24f),PULSE(.28f),PLATED(.32f),ROUTER(.34f),JUNCTION(.32f),BRIDGE(.50f),PHASE(.60f),
    CONTAINER(.85f),TANK(1.12f),MECHANICAL(.58f),ROTARY(.84f),IMPULSE(1.08f),THERMAL(1.06f);
    public final float height;
    LiquidModelProfile(float height){this.height=height;}
    public boolean pipe(){return ordinal()<3;}
    public boolean storage(){return this==CONTAINER||this==TANK;}
    public boolean pump(){return this==MECHANICAL||this==ROTARY||this==IMPULSE;}
    public static LiquidModelProfile forBlock(Block b){
        if(b==Blocks.conduit)return CONDUIT;if(b==Blocks.pulseConduit)return PULSE;if(b==Blocks.platedConduit)return PLATED;
        if(b==Blocks.liquidRouter)return ROUTER;if(b==Blocks.liquidJunction)return JUNCTION;
        if(b==Blocks.bridgeConduit)return BRIDGE;if(b==Blocks.phaseConduit)return PHASE;
        if(b==Blocks.liquidContainer)return CONTAINER;if(b==Blocks.liquidTank)return TANK;
        if(b==Blocks.mechanicalPump)return MECHANICAL;if(b==Blocks.rotaryPump)return ROTARY;if(b==Blocks.impulsePump)return IMPULSE;
        return b==Blocks.thermalGenerator?THERMAL:null;
    }
}
