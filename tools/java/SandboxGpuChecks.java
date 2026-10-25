package fpv.render;

import arc.*;import arc.files.Fi;import arc.graphics.*;import arc.graphics.g2d.*;import arc.graphics.g3d.Camera3D;import arc.graphics.gl.*;import arc.math.geom.Vec3;import arc.util.*;
import fpv.construction.*;import fpv.units.UnitAltitudeSystem;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.blocks.logic.*;import mindustry.world.blocks.units.RepairTurret;

/** Real native healing and actual live tiled-screen GPU pixels. Readbacks exist only in tests. */
public final class SandboxGpuChecks {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    public static String run(FpvWorldRenderer renderer,UnitAltitudeSystem altitudes,Fi output)throws Exception{
        if(!Boolean.getBoolean("fpv.smoke"))throw new IllegalStateException("Disposable graphics fixture required");
        var registry=new LayerBuildingAdapter(new LayerBuildingAdapter.Events(){
            @Override public double unitAltitude(Unit u){return altitudes.unitState(u).z();}
            @Override public double buildingAltitude(Building b){return b.tile instanceof LayerTile t?LayerCoordinates.DEFAULT.plane(t.layer):0;}
        });
        var framebuffer=new FrameBuffer(256,256,true);var stream=new NativeAtlasMesh.Builder(true);var models=new SandboxModelRenderer();var displayView=new LayerDisplayRenderer();
        var camera=new Camera3D();camera.resize(256,256);camera.near=.02f;camera.far=100;camera.up.set(0,0,-1);camera.position.set(0,3,0);camera.lookAt(0,0,0);camera.update();
        var shader=new Shader(source("ATLAS_VERTEX"),source("ATLAS_FRAGMENT"));long id=900000;float oldDelta=Time.delta;Time.delta=1;
        try{
            for(int floor:new int[]{0,1,5})for(var block:new mindustry.world.Block[]{Blocks.repairPoint,Blocks.repairTurret}){
                Building b;if(floor==0){Vars.world.tile(140,30).setBlock(block,Team.sharded,0);b=Vars.world.build(140,30);}else b=registry.create(++id,140,30,floor,block,Team.sharded,0);
                var point=(RepairTurret.RepairPointBuild)b;Unit target=UnitTypes.zenith.create(b.team);target.set(b.x+16,b.y+16);target.add();target.health=20;altitudes.initializePayloadRelease(target,LayerCoordinates.DEFAULT.plane(floor)+.8);
                point.target=target;point.rotation=point.angleTo(target);point.enabled=true;point.efficiency=point.potentialEfficiency=1;point.timer.reset(((RepairTurret)block).timerTarget,0);
                float hp=target.health;for(int i=0;i<12;i++)point.updateTile();check(target.health>hp&&point.strength>.1,"native repair changes real HP at Z"+floor+" "+block.name);
                point.target=null;point.efficiency=0;float strength=point.strength;for(int i=0;i<6;i++)point.updateTile();check(point.strength<strength,"disabled repair stops activity");target.remove();if(floor==0)b.tile.remove();else registry.clear();
            }
            gl("after native repair");
            var opaque=(RepairTurret.RepairPointBuild)registry.create(++id,138,38,1,Blocks.repairTurret,Team.sharded,0);
            float hull=fpv.net.HeadlessGeometry.heightFor(opaque.block);camera.position.set(0,hull*.4f,4);camera.up.set(0,1,0);camera.lookAt(0,hull*.4f,0);camera.update();
            Draw.flush();framebuffer.begin();try{
                Gl.disable(Gl.cullFace);Gl.disable(Gl.blend);Gl.enable(Gl.depthTest);Gl.depthMask(true);Gl.clearColor(0,0,0,1);Gl.clear(Gl.colorBufferBit|Gl.depthBufferBit);
                shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_offset",0,0,0);shader.setUniformf("u_ambient",.7f);shader.setUniformf("u_time",0);shader.setUniformf("u_water",0,0);shader.setUniformf("u_waves",0);shader.setUniformf("u_cutoff",.01f);shader.setUniformi("u_texture",0);
                stream.clear();stream.overflowShader=shader;models.render(opaque,stream,0,0,0,hull,0,null);stream.drawAndClear(shader);
                byte[] pixels=ScreenUtils.getFrameBufferPixels(128,128,1,1,false);check((pixels[0]&255)+(pixels[1]&255)+(pixels[2]&255)>50,"real side faces remain opaque below the repair head");
                var image=ScreenUtils.getFrameBufferPixmap(0,0,256,256,true);try{PixmapIO.writePng(output.child("repair-side-gpu.png"),image);}finally{image.dispose();}
            }finally{framebuffer.end();}Core.atlas.white().texture.bind(0);registry.clear();camera.position.set(0,3,0);camera.up.set(0,0,-1);camera.lookAt(0,0,0);camera.update();shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);
            for(int floor:new int[]{1,5}){
                var d=(LogicDisplay.LogicDisplayBuild)registry.create(++id,138,38,floor,Blocks.tileLogicDisplay,Team.sharded,0);
                d.updateTile();
                int n=32-2*((mindustry.world.blocks.logic.TileableLogicDisplay)d.block).frameSize,h=n/2;
                for(int iy=0;iy<2;iy++)for(int ix=0;ix<2;ix++){
                    d.commands.addLast(DisplayCmd.get(LogicDisplay.commandColor,iy==0&&ix==0?240:20,ix==1?230:20,iy==1?240:20,255,0,0));
                    d.commands.addLast(DisplayCmd.get(LogicDisplay.commandRect,ix*h,iy*h,h,h,0,0));
                }
                displayView.prepareRegistry(registry);gl("native tiled prepare");check(d.commands.isEmpty()&&displayView.updated()==1,"native tiled draw commands flushed");
                var old=d.buffer;displayView.prepareRegistry(registry);check(displayView.updated()==0&&d.buffer==old,"unchanged tiled surface has no upload/update");
                Draw.flush();framebuffer.begin();try{
                    Gl.disable(Gl.cullFace);Gl.disable(Gl.blend);Gl.enable(Gl.depthTest);Gl.depthMask(true);Gl.clearColor(0,0,0,1);Gl.clear(Gl.colorBufferBit|Gl.depthBufferBit);
                    stream.clear();float height=fpv.net.HeadlessGeometry.heightFor(d.block);models.render(d,stream,0,0,0,height,0,displayView.texture(d));
                    shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_offset",0,0,0);shader.setUniformf("u_ambient",.2f);shader.setUniformf("u_time",0);shader.setUniformf("u_water",0,0);shader.setUniformf("u_waves",0);shader.setUniformf("u_cutoff",.01f);shader.setUniformi("u_texture",0);stream.overflowShader=shader;stream.drawAndClear(shader);
                    gl("tiled model draw");byte[] pixels=ScreenUtils.getFrameBufferPixels(0,0,256,256,false);float span=d.block.size;
                    for(int iy=0;iy<2;iy++)for(int ix=0;ix<2;ix++){
                        Vec3 pt=camera.project(new Vec3((ix==0?-.25f:.25f)*span,height*.68f+.012f,(iy==0?.25f:-.25f)*span),0,0,256,256);int offset=((int)pt.y*256+(int)pt.x)*4;
                        int r=pixels[offset]&255,g=pixels[offset+1]&255,b=pixels[offset+2]&255;
                        check((iy==0&&ix==0?r>210:r<60)&&(ix==1?g>210:g<60)&&(iy==1?b>210:b<60),"tiled 3D screen color/orientation Z"+floor+" quadrant "+ix+","+iy+" got "+r+","+g+","+b);
                    }
                    var image=ScreenUtils.getFrameBufferPixmap(0,0,256,256,true);try{PixmapIO.writePng(output.child("tiled-gpu-Z"+floor+".png"),image);}finally{image.dispose();}
                }finally{framebuffer.end();}
                gl("after tiled model readback");Core.atlas.white().texture.bind(0);registry.clear();
                var left=(mindustry.world.blocks.logic.TileableLogicDisplay.TileableLogicDisplayBuild)registry.create(++id,138,38,floor,Blocks.tileLogicDisplay,Team.sharded,0);
                var right=(mindustry.world.blocks.logic.TileableLogicDisplay.TileableLogicDisplayBuild)registry.create(++id,139,38,floor,Blocks.tileLogicDisplay,Team.sharded,0);left.updateTile();right.updateTile();
                check(left.rootDisplay==right.rootDisplay&&left.tilesWidth==2,"native two-tile shared canvas");
                int width=64-2*((mindustry.world.blocks.logic.TileableLogicDisplay)left.block).frameSize;
                var root=left.rootDisplay;root.commands.addLast(DisplayCmd.get(LogicDisplay.commandClear,240,20,20,0,0,0));root.commands.addLast(DisplayCmd.get(LogicDisplay.commandColor,20,20,240,255,0,0));root.commands.addLast(DisplayCmd.get(LogicDisplay.commandRect,width/2,0,width/2,32,0,0));displayView.prepareRegistry(registry);
                output.child("joined-debug.txt").writeString("size="+left.block.size+" buffer="+root.buffer.getWidth()+","+root.buffer.getHeight()+" origin="+left.originX+","+left.originY+" UV left="+displayView.texture(left).u+","+displayView.texture(left).u2+" right="+displayView.texture(right).u+","+displayView.texture(right).u2);
                root.buffer.begin();try{var nativeImage=ScreenUtils.getFrameBufferPixmap(0,0,root.buffer.getWidth(),root.buffer.getHeight(),true);try{PixmapIO.writePng(output.child("joined-native.png"),nativeImage);}finally{nativeImage.dispose();}}finally{root.buffer.end();}
                check(displayView.texture(left).u!=displayView.texture(right).u,"two screen tiles sample different native UV slices");
                Draw.flush();framebuffer.begin();try{
                    Gl.depthMask(true);Gl.clear(Gl.colorBufferBit|Gl.depthBufferBit);stream.clear();float height=fpv.net.HeadlessGeometry.heightFor(left.block);
                    models.render(left,stream,-.5f,0,0,height,0,displayView.texture(left));models.render(right,stream,.5f,0,0,height,0,displayView.texture(right));
                    shader.bind();stream.drawAndClear(shader);byte[] pixels=ScreenUtils.getFrameBufferPixels(0,0,256,256,false);
                    var image=ScreenUtils.getFrameBufferPixmap(0,0,256,256,true);try{PixmapIO.writePng(output.child("joined-Z"+floor+".png"),image);}finally{image.dispose();}
                    for(int side=0;side<2;side++){Vec3 pt=camera.project(new Vec3(side==0?-.5f:.5f,height*.68f+.012f,0),0,0,256,256);int i=((int)pt.y*256+(int)pt.x)*4;
                        check(side==0?(pixels[i]&255)>210&&(pixels[i+2]&255)<60:(pixels[i+2]&255)>210&&(pixels[i]&255)<60,"actual joined panel pixels retain distinct left/right content Z"+floor+" side "+side+" got "+(pixels[i]&255)+","+(pixels[i+1]&255)+","+(pixels[i+2]&255));}
                }finally{framebuffer.end();}Core.atlas.white().texture.bind(0);registry.clear();
            }
            return "PASS "+checks+" actual repair HP/start-stop and live tiled-screen GPU/cache/orientation checks";
        }finally{Time.delta=oldDelta;displayView.close();registry.clear();models.clear();stream.dispose();framebuffer.dispose();shader.dispose();}
    }
    static void gl(String stage){int code=Gl.getError();check(code==0,stage+" GL error "+code);}
    static String source(String name)throws Exception{var f=FpvWorldRenderer.class.getDeclaredField(name);f.setAccessible(true);return (String)f.get(null);}
}
