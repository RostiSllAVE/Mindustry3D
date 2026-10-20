package fpv.render;

import fpv.terrain.TerrainClassification;
import fpv.terrain.TerrainElevation;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.world.Block;
import mindustry.world.Tile;

/**
 * Bounded, read-only context for optional background motifs, not a replacement for local edge biomes.
 * Sampled only when the owning horizon cache rebuilds. No retained world references or native RNG.
 */
public record HorizonComposition(int sampledCells,float waterFraction,float greenFraction,float sandFraction,
    float rockFraction,float hotFraction,float iceFraction,float naturalWallFraction,float vegetationFraction,
    float decorationFraction,float structuralFraction,long seed) {
    public static final int MAX_GRID_AXIS=64,MAX_SAMPLES=MAX_GRID_AXIS*MAX_GRID_AXIS;
    public static final float MIN_STRUCTURAL_FRACTION=.04f;
    public static final HorizonComposition EMPTY=new HorizonComposition(0,0,0,0,0,0,0,0,0,0,0,0);

    /** Flags may overlap: e.g. native hotrock is both rocky and hot, a tree is plant and decoration. */
    public record Cell(String floorName,String blockName,boolean water,boolean green,boolean sand,
        boolean rock,boolean hot,boolean ice,boolean naturalWall,boolean vegetation,
        boolean decoration,boolean structural) {}
    public interface Source {int width();int height();Cell cell(int x,int y);}

    /** Land evidence is required: a featureless all-water map does not invent distant land. */
    public boolean islands(){return waterFraction>=.15f&&
        (greenFraction+sandFraction+rockFraction>=.04f||naturalWallFraction>=.02f);}
    public boolean vegetationPatches(){return greenFraction>=.12f||vegetationFraction>=.03f;}
    public boolean rockyOutcrops(){return rockFraction>=.12f||naturalWallFraction>=.06f;}
    public boolean volcanicScars(){return hotFraction>=.04f;}
    /** Three sampled cells plus a meaningful area fraction keep isolated facilities from dominating. */
    public boolean structuralMotifs(){return structuralFraction>=MIN_STRUCTURAL_FRACTION&&
        structuralFraction*sampledCells>=2.999f;}

    public static HorizonComposition analyze(){return analyze(new TerrainElevation());}
    public static HorizonComposition analyze(TerrainElevation terrain){
        if(Vars.world==null||Vars.world.width()<1||Vars.world.height()<1)return EMPTY;
        TerrainClassification classification=terrain==null?new TerrainClassification():terrain.classification();
        return analyze(new Source(){
            public int width(){return Vars.world.width();}
            public int height(){return Vars.world.height();}
            public Cell cell(int x,int y){return nativeCell(Vars.world.tile(x,y),classification);}
        });
    }

    /**
     * Endpoint-inclusive uniform grid: at most 4096 unique reads even on enormous logical maps.
     * All four corners and each sampled border are represented. Small maps are sampled in full.
     * Fractions are approximate map context; border eligibility still comes from BorderBiomeProfile.
     */
    public static HorizonComposition analyze(Source source){
        if(source==null)throw new IllegalArgumentException("Map source required");
        int width=source.width(),height=source.height();
        if(width<0||height<0)throw new IllegalArgumentException("Nonnegative map dimensions required");
        if(width==0||height==0)return EMPTY;
        int nx=Math.min(width,MAX_GRID_AXIS),ny=Math.min(height,MAX_GRID_AXIS),count=nx*ny;
        int water=0,green=0,sand=0,rock=0,hot=0,ice=0,walls=0,plants=0,decor=0,structural=0;
        long seed=hash(hash(0xcbf29ce484222325L,width),height);
        for(int j=0;j<ny;j++)for(int i=0;i<nx;i++){
            int x=coordinate(i,nx,width),y=coordinate(j,ny,height);
            Cell cell=source.cell(x,y);
            seed=hash(hash(seed,x),y);
            if(cell==null){seed=hash(seed,0);continue;}
            water+=cell.water?1:0;green+=cell.green?1:0;sand+=cell.sand?1:0;rock+=cell.rock?1:0;
            hot+=cell.hot?1:0;ice+=cell.ice?1:0;walls+=cell.naturalWall?1:0;plants+=cell.vegetation?1:0;
            decor+=cell.decoration?1:0;structural+=cell.structural?1:0;
            seed=hash(hash(seed,cell.floorName),cell.blockName);
            int flags=(cell.water?1:0)|(cell.green?2:0)|(cell.sand?4:0)|(cell.rock?8:0)|
                (cell.hot?16:0)|(cell.ice?32:0)|(cell.naturalWall?64:0)|(cell.vegetation?128:0)|
                (cell.decoration?256:0)|(cell.structural?512:0);
            seed=hash(seed,flags);
        }
        return new HorizonComposition(count,water/(float)count,green/(float)count,sand/(float)count,
            rock/(float)count,hot/(float)count,ice/(float)count,walls/(float)count,plants/(float)count,
            decor/(float)count,structural/(float)count,seed);
    }
    private static int coordinate(int index,int samples,int length){
        return samples==1?0:(int)((long)index*(length-1)/(samples-1));
    }
    private static long hash(long hash,int value){return (hash^(value&0xffffffffL))*0x100000001b3L;}
    private static long hash(long hash,String value){
        if(value==null)return hash(hash,0);
        hash=hash(hash,value.length());for(int i=0;i<value.length();i++)hash=hash(hash,value.charAt(i));return hash;
    }

    /** Native policy identities, not HP, team, numerical content IDs or map-name heuristics. */
    public static Cell nativeCell(Tile tile,TerrainClassification classification){
        if(tile==null)return null;
        Block floor=tile.floor(),block=tile.block();
        boolean space=floor==Blocks.space||floor==Blocks.empty;
        boolean water=!space&&tile.floor().isLiquid&&tile.floor().liquidDrop==Liquids.water;
        boolean green=!space&&(floor==Blocks.grass||floor==Blocks.moss||floor==Blocks.sporeMoss);
        boolean sand=floor==Blocks.sand||floor==Blocks.darksand||floor==Blocks.salt;
        boolean hot=floor==Blocks.hotrock||floor==Blocks.magmarock||floor==Blocks.basalt||floor==Blocks.slag;
        boolean ice=floor==Blocks.ice||floor==Blocks.snow||floor==Blocks.iceSnow||floor==Blocks.redIce||floor==Blocks.cryofluid;
        boolean rock=rockFloor(floor);
        var kind=classification.kind(tile);
        boolean natural=!space&&kind==TerrainClassification.Kind.NATURAL;
        boolean decoration=!space&&kind==TerrainClassification.Kind.DECORATION;
        boolean plant=decoration&&VegetationInstances.form(block)!=null;
        boolean structural=kind==TerrainClassification.Kind.ARTIFICIAL||artificialFloor(floor);
        return new Cell(floor.name,block.name,water,green,sand,rock,hot,ice,natural,plant,decoration,structural);
    }
    private static boolean rockFloor(Block floor){
        return floor==Blocks.stone||floor==Blocks.craters||floor==Blocks.dacite||floor==Blocks.rhyolite||
            floor==Blocks.rhyoliteCrater||floor==Blocks.roughRhyolite||floor==Blocks.regolith||
            floor==Blocks.yellowStone||floor==Blocks.redStone||floor==Blocks.denseRedStone||
            floor==Blocks.arkyicStone||floor==Blocks.ferricStone||floor==Blocks.ferricCraters||
            floor==Blocks.carbonStone||floor==Blocks.beryllicStone||floor==Blocks.crystallineStone||
            floor==Blocks.crystalFloor||floor==Blocks.yellowStonePlates||floor==Blocks.shale||
            floor==Blocks.basalt||floor==Blocks.hotrock||floor==Blocks.magmarock;
    }
    private static boolean artificialFloor(Block floor){
        return floor==Blocks.metalFloor||floor==Blocks.metalFloorDamaged||floor==Blocks.metalFloor2||
            floor==Blocks.metalFloor3||floor==Blocks.metalFloor4||floor==Blocks.metalFloor5||
            floor==Blocks.darkPanel1||floor==Blocks.darkPanel2||floor==Blocks.darkPanel3||
            floor==Blocks.darkPanel4||floor==Blocks.darkPanel5||floor==Blocks.darkPanel6||
            floor==Blocks.metalTiles1||floor==Blocks.metalTiles2||floor==Blocks.metalTiles3||
            floor==Blocks.metalTiles4||floor==Blocks.metalTiles5||floor==Blocks.metalTiles6||
            floor==Blocks.metalTiles7||floor==Blocks.metalTiles8||floor==Blocks.metalTiles9||
            floor==Blocks.metalTiles10||floor==Blocks.metalTiles11||floor==Blocks.metalTiles12||floor==Blocks.metalTiles13;
    }
}
