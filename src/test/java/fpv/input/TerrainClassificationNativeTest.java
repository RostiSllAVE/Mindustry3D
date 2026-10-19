package fpv.input;

import arc.Core;
import fpv.terrain.*;
import fpv.net.HeadlessGeometry;
import fpv.world.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.io.JsonIO;
import java.util.*;
import static fpv.input.ConstructionPriorityNativeTest.*;

/** Real native content and Rules JSON, CPU only. No owner profiles or physical renderer acceptance. */
public final class TerrainClassificationNativeTest {
 static int checks;
 static TerrainElevation terrain=new TerrainElevation();
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static void world(int size){Vars.world.tiles=new Tiles(size,size);Vars.world.tiles.fill();Vars.world.setGenerating(true);Vars.state.rules.tags.remove(TerrainClassification.TAG);terrain=new TerrainElevation();}
 static Tile put(int x,int y,Block b){var t=Vars.world.tile(x,y);t.setBlock(b,Team.sharded,0);terrain.invalidate(t);return t;}
 static void rules(String json){Vars.state.rules.tags.put(TerrainClassification.TAG,json);terrain.ensureWorld();}
 static long hash(){long h=1;for(var t:Vars.world.tiles){h=h*31+t.blockID();h=h*31+t.floorID();h=h*31+t.overlayID();h=h*31+t.team().id;}return h;}
 static TerrainHeightField.Parameters defaults(){return new TerrainHeightField.Parameters(.85,1.15,30,3,.23,18);}
 public static void main(String[] args)throws Exception{
  init();try{catalog();mountains();overrides();serializationAndLifecycle();performance();System.out.println("TerrainClassificationNativeTest: "+checks+" checks PASS; native content/geometry/cache/rules, physical gameplay NOT TESTED.");}
  finally{input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
 }
 static void catalog(){
  world(64);int natural=0,industrial=0,decor=0;
  for(var b:Vars.content.blocks())if(b instanceof StaticWall){var t=put(30,30,b);var k=terrain.classification().kind(t);
   if(b==Blocks.darkMetal||b==Blocks.metalWall1||b==Blocks.metalWall2||b==Blocks.metalWall3||b==Blocks.coloredWall){check(k==TerrainClassification.Kind.ARTIFICIAL,"native industrial wall is not geology: "+b.name);industrial++;}
   else if(b instanceof StaticTree||fpv.world.NaturalProps.supported(b)){check(k==TerrainClassification.Kind.DECORATION,"vegetation remains decoration: "+b.name);decor++;}
   else {check(k==TerrainClassification.Kind.NATURAL,"native geological catalog entry: "+b.name);natural++;}
  }
  System.out.println("A210 catalog: "+natural+" natural / "+industrial+" industrial / "+decor+" vegetation");check(natural==21&&industrial==5&&decor==5,"native160.7 catalog: 21 geological,5 industrial,5 vegetation StaticWalls");
  for(var b:Vars.content.blocks())if(b instanceof mindustry.world.blocks.defense.Wall){var t=put(30,30,b);check(!terrain.naturalWall(t)&&terrain.classification().kind(t)==TerrainClassification.Kind.ARTIFICIAL,"all defensive wall families remain artificial: "+b.name);}
  for(var b:new Block[]{Blocks.coreShard,Blocks.graphitePress,Blocks.duo,Blocks.conveyor,Blocks.conduit})check(!terrain.naturalWall(put(20,20,b)),"native building excluded: "+b.name);
  var fake=new StaticWall("fpv-test-mechanical-wall");fake.destructible=true;fake.init();check(terrain.classification().kind(put(40,40,fake))==TerrainClassification.Kind.ARTIFICIAL,"mod synthetic StaticWall cannot form mountains");
  var unknown=new Block("fpv-test-geology");unknown.solid=true;check(!terrain.naturalWall(put(42,40,unknown)),"unknown solid Block defaults to artificial, not inferred from solidity");
  rules("{\"version\":1,\"blocks\":{\"fpv-test-geology\":\"natural\",\"fpv-test-mechanical-wall\":\"natural\"}}");check(terrain.naturalWall(Vars.world.tile(42,40)),"explicit author override permits eligible unknown geological content");check(!terrain.naturalWall(Vars.world.tile(40,40)),"override cannot turn a synthetic structure into geology");
 }
 static void mountains(){
  world(64);boolean[] mask=new boolean[64*64];for(int y=10;y<=50;y++)for(int x=10;x<=50;x++){put(x,y,Blocks.stoneWall);mask[y*64+x]=true;}
  long source=hash();var expected=TerrainHeightField.generate(64,64,mask,defaults());check(terrain.maxHeight()>12,"native stone forms an organic mountain");
  for(int y=0;y<=64;y++)for(int x=0;x<=64;x++)check(Float.floatToIntBits(terrain.vertexHeight(x,y))==Float.floatToIntBits(expected.vertexHeight(x,y)),"unchanged geological map has exact old vertex bits");
  var wall=put(30,30,Blocks.copperWall);check(terrain.heightAt(30,-30)==0&&terrain.naturalWallCount()==1680,"artificial building cuts a genuine open cell, not a mountain smear");
  for(double dx:new double[]{-.49,0,.49})for(double dy:new double[]{-.49,0,.49})check(terrain.heightAt(30+dx,-(30+dy))==0,"whole artificial footprint stays clear");
  check(HeadlessGeometry.surface(terrain,30,-30)==HeadlessGeometry.heightFor(Blocks.copperWall),"render/server surface is ordinary wall height");
  for(var b:new Block[]{Blocks.darkMetal,Blocks.metalWall1,Blocks.metalWall2,Blocks.metalWall3,Blocks.coloredWall}){put(30,30,b);check(terrain.heightAt(30,-30)==0&&terrain.staticArtificial(Vars.world.tile(30,30)),"industrial material inside cliff keeps block geometry");check(HeadlessGeometry.surface(terrain,30,-30)==HeadlessGeometry.heightFor(b),"industrial visible height and collision share block surface");}
  put(30,30,Blocks.stoneWall);check(terrain.mapFingerprint()==expected.maskFingerprint(),"restored natural membership regenerates exact shape");
  long rev=terrain.revision(),scans=terrain.maskScans();for(int i=0;i<100;i++){var t=put(2,2,i%2==0?Blocks.copperWall:Blocks.titaniumWall);terrain.ensureWorld();check(terrain.revision()==rev&&terrain.maskScans()==scans,"artificial-to-artificial edits do not scan/regenerate global terrain");put(2,2,Blocks.air);terrain.ensureWorld();}
  var listed=new java.util.ArrayList<Integer>();terrain.eachStaticArtificial(0,0,6,6,listed::add);check(listed.isEmpty(),"removed artificial structures leave no stale spatial bits");
  put(2,2,Blocks.darkMetal);put(5,5,Blocks.metalWall1);put(4,3,Blocks.copperWall);listed.clear();terrain.eachStaticArtificial(1,1,4,4,listed::add);check(listed.equals(java.util.List.of(2+2*64)),"bounded static index excludes out-of-rectangle and live buildings");put(2,2,Blocks.air);put(5,5,Blocks.air);put(4,3,Blocks.air);
  check(hash()==source,"read-only terrain restores unchanged source after fixture edits");
  var world=new World3DAdapter(terrain,(x,z)->HeadlessGeometry.surface(terrain,x,z),altitude);put(2,2,Blocks.copperWall);
  check(world.traceSegment(0,2,.7,4,2,.7,0).hit(),"ordinary wall retains physical obstruction");check(!world.traceSegment(0,2,2,4,2,2,0).hit(),"ordinary wall does not become an invisible high mountain");
 }
 static void overrides(){
  world(64);for(int y=20;y<40;y++)for(int x=20;x<40;x++)put(x,y,Blocks.stoneWall);terrain.ensureWorld();
  rules("{\"version\":1,\"blocks\":{\"stone-wall\":\"artificial\"},\"tiles\":{\"30,30\":\"natural\"}}");check(terrain.naturalWallCount()==1&&terrain.naturalWall(30,30)&&!terrain.naturalWall(31,30),"tile override wins over block override");
  check(terrain.heightAt(31,-30)==0,"excluded authored static material remains flat-based block, not smeared mountain");
  int parses=terrain.classification().parses();long scans=terrain.maskScans(),rev=terrain.revision();for(int i=0;i<10000;i++)terrain.heightAt(30,-30);check(terrain.classification().parses()==parses&&terrain.maskScans()==scans&&terrain.revision()==rev,"unchanged queries neither parse policies nor scan/regenerate geometry");
  put(10,10,Blocks.copperWall);rules("{\"version\":1,\"blocks\":{\"stone-wall\":\"artificial\",\"copper-wall\":\"natural\"},\"tiles\":{\"30,30\":\"natural\",\"10,10\":\"natural\"}}");check(!terrain.naturalWall(10,10)&&terrain.naturalWallCount()==1,"neither block nor cell overrides can promote built defenses");check(terrain.revision()==rev,"irrelevant policy change does not rebuild field/chunks");
  for(String bad:new String[]{"{\"version\":2}","{\"version\":1.5}","{\"blocks\":{\"stone-wall\":\"natural\"},\"tiles\":{\"-1,2\":\"artificial\"}}","{\"blocks\":{\"stone-wall\":nonsense}}","{\"unknown\":1}","{\"tiles\":{\"32768,1\":\"natural\"}}","x".repeat(TerrainClassification.MAX_CHARACTERS+1)}){
   rules(bad);check(terrain.naturalWallCount()==400&&terrain.classification().status().startsWith("Invalid"),"invalid author metadata uses atomic content defaults");int n=terrain.classification().parses();for(int i=0;i<20;i++)terrain.ensureWorld();check(n==terrain.classification().parses(),"invalid policy warning/parse is not repeated per query");
  }
  Vars.state.rules.tags.put(TerrainClassification.TAG,null);check(terrain.naturalWallCount()==400,"null optional rule metadata safely uses defaults");
  rules("{\"version\":1,\"blocks\":{\"nonexistent-content\":\"natural\"}}");check(terrain.naturalWallCount()==400&&terrain.classification().status().contains("unknown content"),"missing optional content defaults safely");
 }
 static void serializationAndLifecycle(){
  world(48);put(12,12,Blocks.stoneWall);put(13,12,Blocks.stoneWall);rules("{\"version\":1,\"tiles\":{\"12,12\":\"artificial\"}}");String tag=Vars.state.rules.tags.get(TerrainClassification.TAG);long fingerprint=terrain.mapFingerprint();
  Vars.state.rules=JsonIO.read(mindustry.game.Rules.class,JsonIO.write(Vars.state.rules));check(tag.equals(Vars.state.rules.tags.get(TerrainClassification.TAG))&&terrain.mapFingerprint()==fingerprint,"native Rules JSON preserves exact author policy for saves/network rules");
  long rev=terrain.revision();var previous=Vars.world.tiles;Vars.world.tiles=new Tiles(48,48);Vars.world.tiles.fill();terrain.ensureWorld();check(terrain.revision()>rev&&terrain.naturalWallCount()==0,"same-dimension new world identity invalidates old geometry");
  Vars.world.tiles=previous;terrain.invalidate();check(terrain.mapFingerprint()==fingerprint,"restoring world and native rules restores deterministic geometry");
  Vars.state.rules.tags.remove(TerrainClassification.TAG);check(terrain.naturalWallCount()==2,"next sector without metadata does not retain old overrides");
 }
 static void performance(){
  for(boolean dense:new boolean[]{false,true}){
   world(512);for(int y=0;y<512;y++)for(int x=0;x<512;x++)if(dense?x<450&&y<495:((x*31+y*17)%100)<8)put(x,y,Blocks.stoneWall);put(0,0,Blocks.air);long before=hash();
   long start=System.nanoTime();terrain.ensureWorld();double generation=(System.nanoTime()-start)/1e6;long rev=terrain.revision(),scans=terrain.maskScans();start=System.nanoTime();
   for(int i=0;i<100000;i++)terrain.heightAt(5.1,-6.2);double queries=(System.nanoTime()-start)/1e6;start=System.nanoTime();
   for(int i=0;i<1000;i++){put(0,0,i%2==0?Blocks.copperWall:Blocks.titaniumWall);terrain.ensureWorld();}double edits=(System.nanoTime()-start)/1e6;
   check(terrain.revision()==rev&&terrain.maskScans()==scans,"sparse/dense artificial edits cause zero global scans/rebuilds");
   System.out.printf(Locale.ROOT,"A210_PERF,%s,512,%.3f,%.3f,%.3f,%d,%d%n",dense?"dense85":"sparse8",generation,queries,edits,terrain.naturalWallCount(),terrain.revision());
   check(terrain.maxHeight()<=30&&before!=0,"finite bounded large-map field");
  }
 }
}
