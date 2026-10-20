package fpv.background;

import fpv.render.BorderBiomeProfile;
import fpv.render.VegetationInstances;
import mindustry.content.Blocks;
import mindustry.world.Block;

/** Original host-material colours, not external textures or simulated terrain. */
public enum BackgroundPreset {
    OCEAN("Ocean",BorderBiomeProfile.Biome.OCEAN,.22f,.35f,.65f),
    MOUNTAINS("Mountains",BorderBiomeProfile.Biome.MOUNTAINS,.40f,.41f,.43f),
    FOREST("Forest",BorderBiomeProfile.Biome.FOREST,.33f,.53f,.25f),
    DESERT("Desert",BorderBiomeProfile.Biome.DESERT,.77f,.64f,.43f),
    ICE("Snow / Ice",BorderBiomeProfile.Biome.ICE,.77f,.84f,.90f),
    WASTELAND("Rocky Wasteland",BorderBiomeProfile.Biome.WASTELAND,.36f,.38f,.40f),
    VOLCANIC("Volcanic",BorderBiomeProfile.Biome.VOLCANIC,.40f,.24f,.18f),
    SPACE("Space",BorderBiomeProfile.Biome.SPACE,.02f,.03f,.06f),
    VOID("Void / Empty",BorderBiomeProfile.Biome.SPACE,0,0,0);
    public final String label;public final BorderBiomeProfile.Biome biome;public final float r,g,b;
    BackgroundPreset(String label,BorderBiomeProfile.Biome biome,float r,float g,float b){this.label=label;this.biome=biome;this.r=r;this.g=g;this.b=b;}
    public static BackgroundPreset safe(int value){return value>=0&&value<values().length?values()[value]:OCEAN;}
    public BorderBiomeProfile.Cell cell(){
        Block block=switch(this){case OCEAN->Blocks.water;case MOUNTAINS->Blocks.stoneWall;case FOREST->Blocks.grass;case DESERT->Blocks.sand;case ICE->Blocks.snow;case WASTELAND->Blocks.stone;case VOLCANIC->Blocks.basalt;default->null;};
        var c=block==null?null:block.mapColor;var tree=Blocks.pine==null?null:Blocks.pine.mapColor;
        return new BorderBiomeProfile.Cell(biome,c==null?r:c.r,c==null?g:c.g,c==null?b:c.b,this==MOUNTAINS,this==OCEAN,this==FOREST?1:0,tree==null?.15f:tree.r,tree==null?.30f:tree.g,tree==null?.18f:tree.b,VegetationInstances.Form.PINE.ordinal());
    }
}
