package fpv.input;
import arc.Core;
import fpv.render.FpvWorldRenderer;
import fpv.terrain.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.world.*;
import java.lang.reflect.*;
import static fpv.input.ConstructionPriorityNativeTest.*;
/** CPU vertices from the real coarse renderer with missing-texture fallback; no physical screenshots/GPU claim. */
public final class TerrainStructureRenderTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static Object get(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String name,Object v)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,v);}
 static void call(Object o,String name,Class<?>[] args,Object... v)throws Exception{var m=o.getClass().getDeclaredMethod(name,args);m.setAccessible(true);m.invoke(o,v);}
 static void world(boolean stone){Vars.world.tiles=new Tiles(4,4);Vars.world.tiles.fill();Vars.world.setGenerating(true);Vars.state.rules.tags.remove(TerrainClassification.TAG);if(stone)for(var t:Vars.world.tiles)t.setBlock(Blocks.stoneWall);}
 static int coarse(FpvWorldRenderer draw)throws Exception{set(draw,"worldWidth",4);set(draw,"worldHeight",4);Object geometry=get(draw,"dynamic");call(geometry,"clear",new Class[]{});call(draw,"coarseChunk",new Class[]{int.class,int.class,int.class},0,0,4);return (int)get(geometry,"count");}
 static void exactBox(FpvWorldRenderer draw,int offset,int x,int y)throws Exception{
  float[] v=(float[])get(get(draw,"dynamic"),"vertices");float h=fpv.net.HeadlessGeometry.heightFor(Blocks.metalWall1);
  for(int i=offset;i<offset+36*7;i+=7){check(v[i]>=x-.5&&v[i]<=x+.5&&v[i+2]>=-y-.5&&v[i+2]<=-y+.5,"static structure keeps exact XY footprint at coarse stride4");check(v[i+1]>=0&&v[i+1]<=h+1e-6,"static structure uses ordinary collision height");}
 }
 public static void main(String[] args)throws Exception{
  init();Core.atlas=new arc.graphics.g2d.TextureAtlas(){@Override public AtlasRegion white(){return new AtlasRegion();}};
  var draw=new FpvWorldRenderer();
  try{
   world(true);Vars.world.tile(2,2).setBlock(Blocks.metalWall1);check(coarse(draw)==252,"mixed cliff retains one complete36-vertex artificial cube rather than dropping it");exactBox(draw,0,2,2);
   var camera=camera(2,2);var picked=draw.pickInteraction(camera,400,300);check(picked!=null&&picked.tile==Vars.world.tile(2,2)&&Math.abs(picked.height-fpv.net.HeadlessGeometry.heightFor(Blocks.metalWall1))<.01,"interaction ray hits the physical static wall surface");
   var scene=fpv.construction.BuildingMousePicker.pickScene(runtime,camera.getPickRay(400,300),100,picked);check(scene.hit()!=null&&scene.hit().id()==0&&scene.distance()==picked.distance,"static wall occludes without becoming a selectable constructed building");unit.plans.addLast(new mindustry.entities.units.BuildPlan(2,2,0,Blocks.conveyor));check(fpv.construction.BuildingMousePicker.pickBlueprint(runtime,camera.getPickRay(400,300),100,scene)==null,"a hidden lower blueprint is not exposed by terrain reclassification");unit.plans.clear();
   world(false);Vars.world.tile(2,2).setBlock(Blocks.metalWall1);check(coarse(draw)==294,"flat coarse cell emits one exact cube plus floor, never a stride-sized cube");exactBox(draw,0,2,2);
   world(false);Vars.world.tile(0,0).setBlock(Blocks.metalWall1);Vars.world.tile(3,3).setBlock(Blocks.metalWall1);check(coarse(draw)==546,"coarse sampling never loses artificial structures outside sample center");exactBox(draw,0,0,0);exactBox(draw,252,3,3);
   world(false);Vars.world.tile(2,2).setBlock(Blocks.copperWall,Team.sharded,0);check(coarse(draw)==42,"live defensive walls remain owned by native building rendering, not duplicate static geometry");
   world(true);Vars.state.rules.tags.put(TerrainClassification.TAG,"{\"version\":1,\"tiles\":{\"2,2\":\"artificial\"}}");check(coarse(draw)==252,"map-authored artificial stone cell remains visible inside natural cliff");
   var open=draw.getClass().getDeclaredMethod("wallOpen",Tile.class,int.class,int.class,float.class);open.setAccessible(true);
   for(int[] direction:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int dx=direction[0],dy=direction[1];var n=Vars.world.tile(2+dx,2+dy);int a=dx>0?0:dx<0?1:dy>0?0:3,b=dx>0?3:dx<0?2:dy>0?1:2;float edge=Math.min(draw.terrain().tileCornerHeight(n.x,n.y,a),draw.terrain().tileCornerHeight(n.x,n.y,b));check(!(boolean)open.invoke(draw,Vars.world.tile(2,2),dx,dy,edge-.05f),"fully buried static side is culled against actual cliff height");check((boolean)open.invoke(draw,Vars.world.tile(2,2),dx,dy,edge+.05f),"side above lower natural edge remains visible");}
   long revision=draw.terrain().revision(),scans=draw.terrain().maskScans();Object rendererRevision=get(draw,"revision");Vars.world.tile(2,2).setBlock(Blocks.metalWall1);draw.invalidate(Vars.world.tile(2,2));check(draw.terrain().revision()==revision&&draw.terrain().maskScans()==scans&&rendererRevision.equals(get(draw,"revision")),"artificial edit invalidates local mesh only, not global mountains or renderer revision");
   System.out.println("TerrainStructureRenderTest: "+checks+" checks PASS; real coarse CPU vertices with mock atlas, visual gameplay NOT TESTED.");
  }finally{draw.dispose();input.uninstallProjection();renderer.close();runtime.close();altitude.close();}
 }
}
