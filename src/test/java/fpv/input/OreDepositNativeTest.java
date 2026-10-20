package fpv.input;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import fpv.render.*;
import fpv.terrain.TerrainElevation;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.world.*;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.production.Drill;
import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
import static fpv.input.TerrainStructureRenderTest.get;
import static fpv.input.TerrainStructureRenderTest.set;
import static fpv.input.TerrainStructureRenderTest.call;

/** Native content, borrowed atlas pixmap fixture, actual builder vertices and native resource rules. */
public final class OreDepositNativeTest {
    static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static class TextureStub extends Texture{TextureStub(){super();width=height=256;}}
    static Object build(FpvWorldRenderer r)throws Exception{var m=r.getClass().getDeclaredMethod("buildChunk",int.class,int.class);m.setAccessible(true);return m.invoke(r,0,0);}
    static float[] data(FpvWorldRenderer r)throws Exception{var values=(Map<?,?>)get(get(r,"staticAtlas"),"data");var d=values.values().iterator().next();return Arrays.copyOf((float[])get(d,"values"),(int)get(d,"count"));}
    static void dispose(Object m)throws Exception{call(m,"dispose",new Class[]{});}
    static int planes(float u,float v){int mask=0;if(v+1e-5>=u&&v+1e-5>=1-u)mask|=1;if(u+1e-5>=v&&u+1e-5>=1-v)mask|=2;if(v<=u+1e-5&&v<=1-u+1e-5)mask|=4;if(u<=v+1e-5&&u<=1-v+1e-5)mask|=8;return mask;}
    public static void main(String[] args)throws Exception{
        arc.util.ArcNativesLoader.load();mindustry.graphics.CacheLayer.init();init();var pix=new Pixmap(32,32);
        for(int y=0;y<32;y++)for(int x=0;x<32;x++)if((x>=3&&x<11||x>=20&&x<28)&&(y>=3&&y<11||y>=20&&y<28))pix.set(x,y,0xda9766ff);
        var texture=new TextureStub();var region=new TextureAtlas.AtlasRegion(texture,32,32,32,32);region.name="native-ore-test";
        Core.atlas=new TextureAtlas(){@Override public AtlasRegion white(){return new AtlasRegion();}@Override public PixmapRegion getPixmap(AtlasRegion r){return new PixmapRegion(pix);}};
        Vars.world.tiles=new Tiles(32,32);Vars.world.tiles.fill();Vars.world.setGenerating(true);for(var t:Vars.world.tiles)t.setFloor(Blocks.stone.asFloor());
        var draw=new FpvWorldRenderer();set(draw,"worldWidth",32);set(draw,"worldHeight",32);
        try{
            byte[] alpha=new byte[1024];for(int i=0;i<1024;i++)alpha[i]=(byte)pix.getA(i%32,i/32);
            var template=OreDepositGeometry.generate(alpha,32,32);check(!template.patches().isEmpty()&&template.patches().size()<=8,"bounded native alpha-island template");
            var again=OreDepositGeometry.generate(alpha,32,32);check(template.patches().size()==again.patches().size(),"deterministic island selection");
            for(int k=0;k<template.patches().size();k++){
                var p=template.patches().get(k);check(Arrays.equals(p.uv(),again.patches().get(k).uv()),"same alpha gives identical ordered outlines");int common=15;
                check(p.points()>=3&&p.points()<=4,"low-poly outline cap");
                for(int i=0;i<p.points();i++){float u=p.uv()[2*i],v=p.uv()[2*i+1];check(u>=0&&u<=1&&v>=0&&v<=1,"outline stays within its native tile");common&=planes(u,v);}
                check(common!=0,"entire roof stays inside one planar floor fan triangle, preventing buried roofs");
                check((alpha[Math.min(31,(int)(p.sideV()*32))*32+Math.min(31,(int)(p.sideU()*32))]&255)>=160,"sides sample original opaque ore art, not transparent atlas pixels");
            }
            check(OreDepositGeometry.generate(new byte[16],4,4).patches().isEmpty(),"empty sprite safely has no raised geometry");
            int supported=0;
            for(Block block:Vars.content.blocks())if(block instanceof OreBlock ore&&!ore.wallOre&&OreDepositRenderer.profile(ore)!=null){ore.variantRegions=new TextureRegion[]{region};supported++;}
            check(supported==9,"all nine native floor ores supported; wall ores remain separate");
            for(Block block:new Block[]{Blocks.pebbles,Blocks.tendrils,Blocks.rhyoliteVent,Blocks.wallOreThorium,Blocks.wallOreGraphite})check(OreDepositRenderer.profile(block)==null,"non-ore or wall overlay is never guessed as a deposit");
            var tile=Vars.world.tile(7,7);tile.setOverlay(Blocks.oreCopper);var terrain=new TerrainElevation();long mask=terrain.mapFingerprint();double physical=terrain.heightAt(7,-7);
            unit.mineTile=tile;unit.mineTimer=12;float mineSpeed=unit.type.mineSpeed;int items=unit.stack.amount;
            var drill=(Drill)Blocks.mechanicalDrill;boolean mineBefore=drill.canMine(tile);var drop=drill.getDrop(tile);float drillTime=drill.getDrillTime(drop);
            Object mesh=build(draw);float[] first=data(draw);dispose(mesh);var deposits=(OreDepositRenderer)get(draw,"oreDeposits");
            check(deposits.builds()==1&&deposits.raisedTiles()==1,"one lazily cached template, one raised tile");
            check(first.length>108&&first.length<=9*(12+OreDepositGeometry.MAX_ADDED_VERTICES),"bounded raised sides/roofs share the overlay's one atlas batch");
            var relief=(FloorSurfaceRelief)get(draw,"floorRelief");float min=relief.minimum(7,7);int roofs=0,sides=0;
            for(int i=108;i<first.length;i+=9){
                float x=first[i],y=first[i+1],z=first[i+2],u=x-7+.5f,v=z+7+.5f;
                check(u>=-1e-5&&u<=1.00001&&v>=-1e-5&&v<=1.00001,"deposits never leave tile footprint or overlap another foundation");
                float surface=relief.surface(7,7,u,v);
                if(y<=min){sides++;check(y>=min-.0051f,"hidden side skirt only sinks by controlled visual allowance");}
                else{roofs++;check(y>surface+.04f&&y<surface+.23f,"exposed mineral vertices follow true local floor plus low-profile raise");}
                check(first[i+7]>=region.u&&first[i+7]<=region.u2&&first[i+8]>=region.v&&first[i+8]<=region.v2,"all roof/side UVs remain inside host ore region");
            }
            check(roofs>0&&sides>0,"real visible thickness, not only another raised overlay");
            mesh=build(draw);check(Arrays.equals(first,data(draw))&&deposits.builds()==1,"repeat chunk generation reuses template and deterministic XYZ/UVs");dispose(mesh);
            check(unit.mineTile==tile&&unit.mineTimer==12&&unit.stack.amount==items&&unit.type.mineSpeed==mineSpeed,"rendering leaves actual player mining state untouched");
            check(drill.canMine(tile)==mineBefore&&drill.getDrop(tile)==drop&&drill.getDrillTime(drop)==drillTime,"native drill eligibility, resource and production time unchanged");
            check(terrain.heightAt(7,-7)==physical&&physical==0&&terrain.mapFingerprint()==mask,"raised ore and deeper visual ground do not enter physical terrain");
            tile.setBlock(Blocks.conveyor,Team.sharded,0);long raised=deposits.raisedTiles();mesh=build(draw);check(deposits.raisedTiles()==raised,"occupied foundations suppress raised mineral fragments");dispose(mesh);tile.setAir();
            tile.setFloor(Blocks.water.asFloor());raised=deposits.raisedTiles();mesh=build(draw);check(deposits.raisedTiles()==raised&&data(draw).length==0,"liquid ore follows the lowered liquid fan without raised mineral rocks");dispose(mesh);tile.setFloor(Blocks.stone.asFloor());
            var unknown=new OreBlock("unknown-native-ore",Items.copper);unknown.variantRegions=new TextureRegion[]{region};tile.setOverlay(unknown);raised=deposits.raisedTiles();mesh=build(draw);check(deposits.raisedTiles()==raised&&data(draw).length==108,"unsupported modded OreBlock remains visible as conforming native overlay");dispose(mesh);
            tile.setOverlay(Blocks.oreLead);Vars.world.tile(15,7).setOverlay(Blocks.oreCopper);mesh=build(draw);check(data(draw).length>216,"isolated ore and chunk-edge ore both produce geometry");dispose(mesh);

            // Real ordinary mechanical-drill production, not a cosmetic resource grant.
            for(int y=20;y<22;y++)for(int x=20;x<22;x++)Vars.world.tile(x,y).setOverlay(Blocks.oreCopper);
            check(drill.canMine(Vars.world.tile(20,20))&&Vars.world.tile(20,20).drop()==Items.copper,"ordinary drill site: block="+Vars.world.tile(20,20).block()+" static="+Vars.world.tile(20,20).block().isStatic()+" drop="+Vars.world.tile(20,20).drop()+" hardness="+Items.copper.hardness+" tier="+drill.tier+" overlay="+Vars.world.tile(20,20).overlay());
            Vars.world.tile(20,20).setBlock(Blocks.mechanicalDrill,Team.sharded,0);var machine=(Drill.DrillBuild)Vars.world.tile(20,20).build;machine.onProximityUpdate();
            check(drill.canMine(machine.tile),"drill can mine its own native occupied footprint: block="+machine.tile.block()+" static="+machine.tile.block().isStatic()+" drop="+machine.tile.drop()+" tier="+drill.tier);
            for(int i=0;i<1200;i++){machine.efficiency=1;arc.util.Time.delta=1;arc.util.Time.update();machine.updateTile();}
            check(machine.items.get(Items.copper)>0,"native drill actually produces copper: "+machine.dominantItem+" cells="+machine.dominantItems+" progress="+machine.progress+" warmup="+machine.warmup);
            System.out.println("OreDepositNativeTest: "+checks+" checks PASS; nine native floor ores, actual alpha-template/roof/side/UV/cache/fallback/native mining and drill behavior; physical gameplay NOT TESTED.");
        }finally{draw.dispose();pix.dispose();input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
    }
}
