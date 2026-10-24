package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Explicit native logic family; privileged/world logic and third-party blocks retain their paths. */
public enum LogicModelProfile {
    MICRO, PROCESSOR, HYPER, CELL, BANK, DISPLAY, LARGE_DISPLAY, SWITCH, MESSAGE;
    public boolean processor(){return ordinal()<=HYPER.ordinal();}
    public boolean display(){return this==DISPLAY||this==LARGE_DISPLAY;}
    public static LogicModelProfile forBlock(Block b){
        if(b==null)return null;
        if(b==Blocks.microProcessor)return MICRO;if(b==Blocks.logicProcessor)return PROCESSOR;if(b==Blocks.hyperProcessor)return HYPER;
        if(b==Blocks.memoryCell)return CELL;if(b==Blocks.memoryBank)return BANK;
        if(b==Blocks.logicDisplay)return DISPLAY;if(b==Blocks.largeLogicDisplay)return LARGE_DISPLAY;
        if(b==Blocks.switchBlock)return SWITCH;return b==Blocks.message?MESSAGE:null;
    }
    public Block block(){return switch(this){case MICRO->Blocks.microProcessor;case PROCESSOR->Blocks.logicProcessor;case HYPER->Blocks.hyperProcessor;case CELL->Blocks.memoryCell;case BANK->Blocks.memoryBank;case DISPLAY->Blocks.logicDisplay;case LARGE_DISPLAY->Blocks.largeLogicDisplay;case SWITCH->Blocks.switchBlock;case MESSAGE->Blocks.message;};}
}
