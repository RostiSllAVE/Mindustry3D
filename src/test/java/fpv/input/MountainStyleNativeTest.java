package fpv.input;

import arc.Core;
import fpv.render.FpvWorldRenderer;
import fpv.render.MountainStyle;
import fpv.terrain.TerrainElevation;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.world.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Actual content/classification/ray/Rules invariance; physical gameplay remains separate. */
public final class MountainStyleNativeTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static long heights(TerrainElevation t){long h=1;for(int y=0;y<64;y++)for(int x=0;x<64;x++)for(int c=0;c<4;c++)h=h*31+Float.floatToIntBits(t.tileCornerHeight(x,y,c));return h;}
    static long tiles(){long h=1;for(var t:Vars.world.tiles){h=h*31+t.blockID();h=h*31+t.floorID();h=h*31+t.overlayID();}return h;}
    public static void main(String[] args)throws Exception{
        init();var draw=new FpvWorldRenderer();
        try{
            Vars.world.tiles=new Tiles(64,64);Vars.world.tiles.fill();Vars.world.setGenerating(true);
            for(int y=10;y<54;y++)for(int x=10;x<54;x++)Vars.world.tile(x,y).setBlock(Blocks.stoneWall);
            Vars.world.tile(32,32).setBlock(Blocks.copperWall,Team.sharded,0);
            var sync=FpvWorldRenderer.class.getDeclaredMethod("syncWorld");sync.setAccessible(true);sync.invoke(draw);
            var terrain=draw.terrain();long revision=terrain.revision(),field=heights(terrain),map=tiles();String rules=mindustry.io.JsonIO.write(Vars.state.rules);
            var picked=draw.pickInteraction(camera(32,32),400,300);
            check(picked!=null&&picked.tile==Vars.world.tile(32,32),"canonical wall picking baseline");
            for(var style:MountainStyle.values()){
                Core.settings.put(MountainStyle.SETTING,style.ordinal());sync.invoke(draw);
                check(draw.terrainMeshStats().style().equals(style.label()),"live renderer observes local style");
                check(terrain.revision()==revision&&heights(terrain)==field,"style switch never regenerates or changes authoritative terrain");
                check(tiles()==map&&rules.equals(mindustry.io.JsonIO.write(Vars.state.rules)),"native tiles and save/network Rules unchanged");
                var next=draw.pickInteraction(camera(32,32),400,300);
                check(next!=null&&next.tile==picked.tile&&next.height==picked.height&&next.distance==picked.distance,"native 3D ray target and surface unchanged");
                check(terrain.heightAt(32,-32)==0&&!terrain.naturalWall(32,32),"artificial footprint remains excluded");
            }
            Core.settings.put(MountainStyle.SETTING,0);System.out.println("MountainStyleNative: "+checks+" live-style/canonical-field/ray/artificial/Rules invariance checks PASS");
        }finally{draw.dispose();input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
}
