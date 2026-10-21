package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Explicit next-family coverage; fractions use the unchanged native physical bounds. */
public enum AdvancedManufacturingProfile {
    KILN(.22f), COMPRESSOR(.23f), WEAVER(.22f), SURGE(.25f), SEPARATOR(.20f), DISASSEMBLER(.24f);
    public final float deck;
    AdvancedManufacturingProfile(float deck){this.deck=deck;}
    public boolean furnace(){return this==KILN||this==SURGE;}
    public boolean separates(){return this==SEPARATOR||this==DISASSEMBLER;}
    public static AdvancedManufacturingProfile forBlock(Block b){
        if(b==Blocks.kiln)return KILN;if(b==Blocks.plastaniumCompressor)return COMPRESSOR;
        if(b==Blocks.phaseWeaver)return WEAVER;if(b==Blocks.surgeSmelter)return SURGE;
        if(b==Blocks.separator)return SEPARATOR;if(b==Blocks.disassembler)return DISASSEMBLER;return null;
    }
}
