package fpv.render;

import arc.*;import arc.files.Fi;import arc.graphics.*;import arc.graphics.g3d.Camera3D;import arc.graphics.gl.*;import arc.util.*;
import fpv.construction.*;import fpv.units.UnitAltitudeSystem;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.blocks.units.RepairTurret;

/** Shipping atlas/mesh checks, readbacks only in this explicit standalone diagnostic. */
public final class TurretPolishGpuChecks {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static String run(UnitAltitudeSystem altitude,Fi output)throws Exception{
  var registry=new LayerBuildingAdapter(new LayerBuildingAdapter.Events(){@Override public double unitAltitude(Unit u){return altitude.unitState(u).z();}@Override public double buildingAltitude(Building b){return b.tile instanceof LayerTile t?LayerCoordinates.DEFAULT.plane(t.layer):0;}});
  var model=new TurretModelRenderer();var mesh=new NativeAtlasMesh.Builder(true);var fbo=new FrameBuffer(320,240,true);var camera=new Camera3D();camera.resize(320,240);camera.near=.04f;camera.far=200;camera.up.set(0,1,0);var shader=new Shader(source("ATLAS_VERTEX"),source("ATLAS_FRAGMENT"));long id=800000;float delta=Time.delta;Time.delta=1;
  try{
   for(var block:new mindustry.world.Block[]{Blocks.repairPoint,Blocks.repairTurret,Blocks.parallax,Blocks.segment})for(int floor:new int[]{0,1,5}){
    Building b;if(floor==0){Vars.world.tile(82,32).setBlock(block,Team.sharded,0);b=Vars.world.build(82,32);}else b=registry.create(++id,82,32,floor,block,Team.sharded,0);
    float h=FpvWorldRenderer.heightFor(block),base=(float)LayerCoordinates.DEFAULT.plane(floor),distance=Math.max(1,block.size*.8f);camera.position.set(distance*2.6f,base+h+distance*2.2f,distance*3.5f);camera.lookAt(0,base+h*.8f,0);camera.update();
    if(b instanceof RepairTurret.RepairPointBuild repair){
     Unit target=UnitTypes.zenith.create(b.team);target.set(b.x+16,b.y+16);target.add();target.health=20;altitude.initializePayloadRelease(target,base+.8);repair.target=target;repair.rotation=repair.angleTo(target);repair.efficiency=repair.potentialEfficiency=1;repair.timer.reset(((RepairTurret)block).timerTarget,0);float hp=target.health;for(int i=0;i<12;i++)repair.updateTile();check(target.health>hp&&repair.strength>.1,"real HP healed at Z"+floor+" "+block.name);target.remove();repair.target=null;
    }
    yaw(b,0);byte[] old=draw(fbo,shader,camera,mesh,model,b,base,h);check(visible(old)>200,"full native base/head at Z"+floor+" "+block.name);long builds=model.builds();draw(fbo,shader,camera,mesh,model,b,base,h);check(builds==model.builds(),"cached auxiliary geometry reused");
    yaw(b,31);byte[] turned=draw(fbo,shader,camera,mesh,model,b,base,h);check(changed(old,turned)>10,"actual head yaw independent of base");
    fbo.begin();try{var image=ScreenUtils.getFrameBufferPixmap(0,0,320,240,true);try{PixmapIO.writePng(output.child("gpu-"+block.name+"-z"+floor+".png"),image);}finally{image.dispose();}}finally{fbo.end();}
    if(floor==0)b.tile.remove();else registry.clear();Core.atlas.white().texture.bind(0);
   }
   check(Gl.getError()==0,"auxiliary model GL state");
   // Exercise the accepted combat turret native capture, recoil, wings, pitch and depth suite.
   System.setProperty("fpv.elevatedTurrets.smoke","true");System.setProperty("fpv.smoke.output",output.absolutePath());String legacy=TurretModelVisualChecks.run();
   return "PASS "+checks+" auxiliary GPU/healing/cache/rotation/floor checks; "+legacy;
  }finally{Time.delta=delta;registry.clear();model.clear();mesh.dispose();fbo.dispose();shader.dispose();}
 }
 static byte[] draw(FrameBuffer fbo,Shader shader,Camera3D camera,NativeAtlasMesh.Builder mesh,TurretModelRenderer model,Building b,float base,float h){
  fbo.begin();try{Gl.disable(Gl.scissorTest);Gl.disable(Gl.cullFace);Gl.disable(Gl.blend);Gl.enable(Gl.depthTest);Gl.depthMask(true);Gl.clearColor(.02f,.03f,.04f,1);Gl.clear(Gl.colorBufferBit|Gl.depthBufferBit);mesh.clear();shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_offset",0,0,0);shader.setUniformf("u_ambient",.9f);shader.setUniformf("u_time",0);shader.setUniformf("u_water",0,0);shader.setUniformf("u_waves",0);shader.setUniformf("u_cutoff",.01f);shader.setUniformi("u_texture",0);mesh.overflowShader=shader;check(model.renderAuxiliary(b,mesh,0,base,0,h),"native auxiliary renderer used");mesh.drawAndClear(shader);return ScreenUtils.getFrameBufferPixels(0,0,320,240,false);}finally{fbo.end();}
 }
 static void yaw(Building b,float y){if(b instanceof RepairTurret.RepairPointBuild r)r.rotation=y;else if(b instanceof mindustry.world.blocks.defense.turrets.BaseTurret.BaseTurretBuild t)t.rotation=y;}
 static int visible(byte[] data){int n=0;for(int i=0;i<data.length;i+=4)if((data[i]&255)>30||(data[i+1]&255)>35||(data[i+2]&255)>40)n++;return n;}
 static int changed(byte[] a,byte[] b){int n=0;for(int i=0;i<a.length;i+=4)if(Math.abs((a[i]&255)-(b[i]&255))+Math.abs((a[i+1]&255)-(b[i+1]&255))+Math.abs((a[i+2]&255)-(b[i+2]&255))>20)n++;return n;}
 static String source(String n)throws Exception{var f=FpvWorldRenderer.class.getDeclaredField(n);f.setAccessible(true);return (String)f.get(null);}
}
