package fpv.render;

import fpv.terrain.TerrainClassification;
import java.util.*;
import java.util.function.BiFunction;

/** No GPU/OS input or world writes. Pure bounded-grid tests plus native content classification. */
public final class HorizonCompositionTest {
    static int checks;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static HorizonComposition.Cell cell(String floor,boolean water,boolean green,boolean sand,boolean rock,
        boolean hot,boolean ice,boolean wall,boolean plant,boolean decoration,boolean structural){
        return new HorizonComposition.Cell(floor,wall?"stone-wall":structural?"metal-wall":"air",
            water,green,sand,rock,hot,ice,wall,plant,decoration,structural);
    }
    static HorizonComposition.Source source(int w,int h,BiFunction<Integer,Integer,HorizonComposition.Cell> cells){
        return new HorizonComposition.Source(){
            public int width(){return w;}public int height(){return h;}
            public HorizonComposition.Cell cell(int x,int y){return cells.apply(x,y);}
        };
    }
    static HorizonComposition.Cell ocean=cell("water",true,false,false,false,false,false,false,false,false,false);
    static HorizonComposition.Cell sand=cell("sand",false,false,true,false,false,false,false,false,false,false);
    static HorizonComposition.Cell forest=cell("grass",false,true,false,false,false,false,false,true,true,false);
    static HorizonComposition.Cell stone=cell("stone",false,false,false,true,false,false,true,false,false,false);
    static HorizonComposition.Cell metal=cell("metal-floor",false,false,false,false,false,false,false,false,false,true);
    public static void main(String[] args)throws Exception{
        var water=HorizonComposition.analyze(source(31,47,(x,y)->ocean));
        check(water.sampledCells()==31*47&&water.waterFraction()==1,"small maps sampled in full");
        check(!water.islands()&&!water.vegetationPatches()&&!water.rockyOutcrops()&&!water.volcanicScars()&&!water.structuralMotifs(),"empty ocean invents no land/industrial motifs");
        var coast=HorizonComposition.analyze(source(64,64,(x,y)->x<32?ocean:sand));
        check(coast.waterFraction()==.5f&&coast.sandFraction()==.5f&&coast.islands(),"sampled coastal land enables sparse sandbars/islands");
        var trees=HorizonComposition.analyze(source(64,64,(x,y)->forest));
        check(trees.vegetationPatches()&&trees.greenFraction()==1&&trees.vegetationFraction()==1,"native-style greenery supports vegetation patches");
        var rocks=HorizonComposition.analyze(source(64,64,(x,y)->stone));
        check(rocks.rockyOutcrops()&&rocks.structuralFraction()==0&&!rocks.structuralMotifs(),"natural walls cannot enable industrial traces");
        var building=HorizonComposition.analyze(source(64,64,(x,y)->x==0&&y==0?metal:sand));
        check(!building.structuralMotifs(),"isolated industrial tile does not enable structural horizon");
        var industry=HorizonComposition.analyze(source(64,64,(x,y)->x<3?metal:sand));
        check(industry.structuralMotifs()&&industry.structuralFraction()==3/64f&&industry.naturalWallFraction()==0,"meaningful industrial area counted separately");
        var tinyIndustry=HorizonComposition.analyze(source(1,2,(x,y)->metal));
        check(!tinyIndustry.structuralMotifs(),"less than three structural samples remains conservative");
        var decorations=HorizonComposition.analyze(source(32,32,(x,y)->cell("sand",false,false,true,false,false,false,false,false,true,false)));
        check(decorations.decorationFraction()==1&&!decorations.vegetationPatches(),"rock decorations are not mistaken for plants");
        var lava=HorizonComposition.analyze(source(64,64,(x,y)->cell("slag",false,false,false,false,true,false,false,false,false,false)));
        check(lava.hotFraction()==1&&lava.waterFraction()==0&&lava.volcanicScars(),"lava does not become a blue ocean");
        var frozen=HorizonComposition.analyze(source(64,64,(x,y)->cell("snow",false,false,false,false,false,true,false,false,false,false)));
        check(frozen.iceFraction()==1&&!frozen.volcanicScars(),"icy identity remains separate");
        check(HorizonComposition.analyze(source(0,40,(x,y)->{throw new AssertionError("empty map read");})).equals(HorizonComposition.EMPTY),"empty startup world is harmless");
        var unknown=HorizonComposition.analyze(source(4,4,(x,y)->null));
        check(unknown.sampledCells()==16&&!unknown.islands()&&!unknown.structuralMotifs(),"null/unknown samples have conservative features");
        for(int[] size:new int[][]{{1,1},{1,999999},{999999,1},{63,63},{64,64},{65,65},{8192,8192},{Integer.MAX_VALUE,Integer.MAX_VALUE}}){
            Set<Long> coordinates=new HashSet<>();boolean[] sides=new boolean[4];
            var p=HorizonComposition.analyze(source(size[0],size[1],(x,y)->{
                check(x>=0&&x<size[0]&&y>=0&&y<size[1],"sample coordinate in range");
                check(coordinates.add(((long)x<<32)|(y&0xffffffffL)),"no duplicate sample reads");
                sides[0]|=x==0;sides[1]|=y==0;sides[2]|=x==size[0]-1;sides[3]|=y==size[1]-1;return ocean;
            }));
            check(p.sampledCells()==coordinates.size()&&p.sampledCells()<=HorizonComposition.MAX_SAMPLES,"at most4096 bounded samples");
            check(sides[0]&&sides[1]&&sides[2]&&sides[3],"endpoint grid includes all edges");
            check(coordinates.contains(0L)&&coordinates.contains(((long)(size[0]-1)<<32)|((size[1]-1)&0xffffffffL)),"opposite corners included even at maximum logical dimensions");
        }
        var repeat=HorizonComposition.analyze(source(64,64,(x,y)->x<32?ocean:sand));
        check(coast.equals(repeat),"repeat/save-equivalent content is deterministic");
        var mirror=HorizonComposition.analyze(source(64,64,(x,y)->x>=32?ocean:sand));
        check(coast.waterFraction()==mirror.waterFraction()&&coast.seed()!=mirror.seed(),"seed includes spatial layout, not only fractions");
        var renamed=HorizonComposition.analyze(source(64,64,(x,y)->new HorizonComposition.Cell("custom-water","air",true,false,false,false,false,false,false,false,false,false)));
        check(renamed.waterFraction()==1&&renamed.seed()!=HorizonComposition.analyze(source(64,64,(x,y)->ocean)).seed(),"seed uses stable content names rather than numeric IDs");
        nativeClassification();
        System.out.println("HorizonCompositionTest: "+checks+" bounded/deterministic/native-content/feature-eligibility checks PASS; physical landscape acceptance separate");
    }
    static void nativeClassification(){
        arc.Core.app=new arc.mock.MockApplication();arc.Core.settings=new arc.mock.MockSettings();
        mindustry.Vars.headless=true;mindustry.Vars.content=new mindustry.core.ContentLoader();mindustry.Vars.content.createBaseContent();
        mindustry.Vars.state=new mindustry.core.GameState();mindustry.Vars.world=new mindustry.core.World();
        mindustry.Vars.world.tiles=new mindustry.world.Tiles(4,4);mindustry.Vars.world.tiles.fill();mindustry.Vars.world.setGenerating(true);
        var tile=mindustry.Vars.world.tile(0,0);var policy=new TerrainClassification();
        tile.setFloor(mindustry.content.Blocks.water.asFloor());var c=HorizonComposition.nativeCell(tile,policy);
        check(c.water()&&!c.hot()&&!c.structural(),"native water has no artificial/hot influence");
        tile.setFloor(mindustry.content.Blocks.slag.asFloor());c=HorizonComposition.nativeCell(tile,policy);check(c.hot()&&!c.water(),"native slag is volcanic, never ocean");
        tile.setFloor(mindustry.content.Blocks.grass.asFloor());tile.setBlock(mindustry.content.Blocks.pine);c=HorizonComposition.nativeCell(tile,policy);
        check(c.green()&&c.vegetation()&&c.decoration()&&!c.naturalWall()&&!c.structural(),"native trees are plants/decorations, not geology/industry");
        tile.setFloor(mindustry.content.Blocks.stone.asFloor());tile.setBlock(mindustry.content.Blocks.stoneWall);c=HorizonComposition.nativeCell(tile,policy);
        check(c.rock()&&c.naturalWall()&&!c.structural()&&!c.vegetation(),"native natural wall uses existing geology policy");
        tile.setBlock(mindustry.content.Blocks.metalWall1);c=HorizonComposition.nativeCell(tile,policy);
        check(!c.naturalWall()&&c.structural(),"native industrial editor wall remains artificial");
        tile.setBlock(mindustry.content.Blocks.air);tile.setFloor(mindustry.content.Blocks.metalTiles13.asFloor());c=HorizonComposition.nativeCell(tile,policy);
        check(c.structural()&&!c.naturalWall(),"native industrial floor may enable subtle structural traces");
        tile.setFloor(mindustry.content.Blocks.space.asFloor());tile.setBlock(mindustry.content.Blocks.pine);c=HorizonComposition.nativeCell(tile,policy);
        check(!c.vegetation()&&!c.naturalWall()&&!c.water(),"space does not gain land from decorations");
        long mapHash=0;for(var t:mindustry.Vars.world.tiles)mapHash=mapHash*31+t.blockID()*71L+t.floorID();
        var result=HorizonComposition.analyze(new fpv.terrain.TerrainElevation());long after=0;for(var t:mindustry.Vars.world.tiles)after=after*31+t.blockID()*71L+t.floorID();
        check(result.sampledCells()==16&&mapHash==after,"native analysis is read-only");
    }
}
