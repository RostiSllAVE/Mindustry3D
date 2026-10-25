package fpv.construction;
import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.world.blocks.logic.TileableLogicDisplay.TileableLogicDisplayBuild;
/** Real native canvas regrouping through layer-local proximity, never a cross-floor XY join. */
public final class TiledDisplayGroupingNativeTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{LayerCampaignNativeTest.main(args);var owner=new LayerBuildingAdapter(null);
        var a=(TileableLogicDisplayBuild)owner.create(700,20,20,1,Blocks.tileLogicDisplay,Team.sharded,0);
        var b=(TileableLogicDisplayBuild)owner.create(701,21,20,1,Blocks.tileLogicDisplay,Team.sharded,0);
        var upper=(TileableLogicDisplayBuild)owner.create(702,20,20,5,Blocks.tileLogicDisplay,Team.sharded,0);
        a.updateTile();b.updateTile();upper.updateTile();
        check(a.rootDisplay==b.rootDisplay&&a.tilesWidth==2&&b.tilesWidth==2,"native neighboring panels share two-tile canvas");
        check(a.originX==20&&a.originY==20&&upper.originX==20&&upper.tilesWidth==1,"native pixel origins initialized");
        check(upper.rootDisplay!=a.rootDisplay,"no cross-floor display connection");
        owner.remove(701);a.updateTile();check(a.tilesWidth==1&&a.rootDisplay==a,"native canvas splits after removal");
        owner.clear();System.out.println("TiledDisplayGroupingNative: PASS native grouping/origins/removal and separate floors");
    }
}
