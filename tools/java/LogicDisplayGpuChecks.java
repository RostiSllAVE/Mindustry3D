package fpv.render;

import arc.*;import arc.files.Fi;import arc.graphics.*;import arc.graphics.g2d.*;import arc.graphics.g3d.Camera3D;import arc.graphics.gl.*;import arc.math.geom.Vec3;import arc.util.*;
import fpv.construction.*;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.blocks.logic.*;

/** Explicit disposable actual-GL checks; synchronous readbacks occur only here, outside benchmarks. */
public final class LogicDisplayGpuChecks {
    private static int checks;private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static String run(Fi output)throws Exception{
        if(!Boolean.getBoolean("fpv.smoke"))throw new IllegalStateException("Isolated graphics fixture only");
        var registry=new LayerBuildingAdapter(null);var view=new LayerDisplayRenderer();var borrowed=new LayerDisplayRenderer(false);
        var framebuffer=new FrameBuffer(360,360,true);var mesh=new NativeAtlasMesh.Builder(true);var models=new LogicModelRenderer();
        var camera=new Camera3D();camera.resize(360,360);camera.near=.04f;camera.far=100;camera.position.set(0,10,0);camera.up.set(0,0,-1);camera.lookAt(0,0,0);camera.update();
        var shader=new Shader(source("ATLAS_VERTEX"),source("ATLAS_FRAGMENT"));
        int memoryIndex=Vars.world.width()-8;
        Vars.world.tile(memoryIndex,8).setBlock(Blocks.logicDisplay,Team.sharded,0);
        var ground=(LogicDisplay.LogicDisplayBuild)Vars.world.build(memoryIndex,8);
        try{
            var small=(LogicDisplay.LogicDisplayBuild)registry.create(70001,18,18,1,Blocks.logicDisplay,Team.sharded,0);
            var large=(LogicDisplay.LogicDisplayBuild)registry.create(70002,28,18,3,Blocks.largeLogicDisplay,Team.sharded,0);
            var processor=(LogicBlock.LogicBuild)registry.create(70003,18,12,1,Blocks.microProcessor,Team.sharded,0);
            pattern(processor,small,80);pattern(processor,large,176);
            check(small.commands.size>0&&large.commands.size>0,"real processor programs enqueue independent display commands");
            Draw.flush();framebuffer.begin();Gl.enable(Gl.depthTest);Gl.enable(Gl.cullFace);
            try{
                int before=Gl.getInt(Gl.framebufferBinding);view.prepareRegistry(registry);
                check(Gl.getInt(Gl.framebufferBinding)==before&&Gl.isEnabled(Gl.depthTest)&&Gl.isEnabled(Gl.cullFace),"nested native drawing restores FBO/depth/cull state");
                check(view.updated()==2&&view.created()==2,"only queued native displays are created/updated");
                check(small.buffer.getWidth()==80&&large.buffer.getWidth()==176,"original 80/176 resolutions retained");
                check(small.commands.isEmpty()&&large.commands.isEmpty(),"native command queues consumed normally");
                var smallBuffer=small.buffer;var largeBuffer=large.buffer;
                view.prepareRegistry(registry);check(view.updated()==0&&view.created()==0&&small.buffer==smallBuffer&&large.buffer==largeBuffer,"unchanged frame reuses native textures without drawing/uploading");
                for(var display:new LogicDisplay.LogicDisplayBuild[]{small,large}){
                    var profile=LogicModelProfile.forBlock(display.block);int size=((LogicDisplay)display.block).displaySize;
                    display.buffer.begin();try{corners(ScreenUtils.getFrameBufferPixels(0,0,size,size,false),size,"native pixels "+size);}finally{display.buffer.end();}
                    Gl.disable(Gl.cullFace);Gl.disable(Gl.blend);Gl.enable(Gl.depthTest);Gl.depthMask(true);Gl.clearColor(.02f,.02f,.02f,1);Gl.clear(Gl.colorBufferBit|Gl.depthBufferBit);
                    mesh.clear();mesh.eyeHeight=10;models.render(display,mesh,0,0,0,fpv.net.HeadlessGeometry.heightFor(display.block),0,view.texture(display));
                    shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_offset",0,0,0);shader.setUniformf("u_ambient",.20f);shader.setUniformf("u_time",0);shader.setUniformf("u_water",0,0);shader.setUniformf("u_waves",0);shader.setUniformf("u_cutoff",.01f);shader.setUniformi("u_texture",0);
                    mesh.overflowShader=shader;mesh.drawAndClear(shader);
                    var meshes=(java.util.Map<?,?>)field(mesh,"streamMeshes");var liveMesh=(Mesh)meshes.get(view.texture(display).texture);
                    check(liveMesh!=null&&liveMesh.getMaxVertices()==6,"one live screen reserves six vertices, not a full atlas stream");
                    var data=(java.util.Map<?,?>)field(mesh,"data");check(((float[])field(data.get(view.texture(display).texture),"values")).length==54,"live screen CPU staging is only 54 floats");
                    byte[] rendered=ScreenUtils.getFrameBufferPixels(0,0,360,360,false);
                    float span=Math.min(display.block.size*.86f,size/(4f*Vars.tilesize)),h=fpv.net.HeadlessGeometry.heightFor(display.block);
                    var capture=ScreenUtils.getFrameBufferPixmap(0,0,360,360,true);try{PixmapIO.writePng(output.child("screen-pixels-"+size+".png"),capture);}finally{capture.dispose();}
                    for(int iy=0;iy<2;iy++)for(int ix=0;ix<2;ix++){
                        float x=(ix==0?-.25f:.25f)*span,z=(iy==0?.25f:-.25f)*span*(float)Math.cos(LogicModelGeometry.pitch(profile));
                        float y=h*LogicModelGeometry.roofY(profile,z/display.block.size)+.013f;
                        Vec3 projected=camera.project(new Vec3(x,y,z),0,0,360,360);
                        color(rendered,360,(int)projected.x,(int)projected.y,ix,iy,"actual emissive 3D face "+size);
                    }
                }
                small.commands.addLast(DisplayCmd.get(LogicDisplay.commandClear,19,101,205,0,0,0));view.prepareRegistry(registry);
                check(view.updated()==1&&small.buffer==smallBuffer&&large.buffer==largeBuffer,"changing one display updates one existing buffer");
                small.buffer.begin();try{byte[] p=ScreenUtils.getFrameBufferPixels(0,0,1,1,false);check(Math.abs((p[0]&255)-19)<3&&Math.abs((p[1]&255)-101)<3&&(p[2]&255)>200,"clear replaces old pixels");}finally{small.buffer.end();}
                large.buffer.begin();try{corners(ScreenUtils.getFrameBufferPixels(0,0,176,176,false),176,"other display unchanged");}finally{large.buffer.end();}
                ground.commands.addLast(DisplayCmd.get(LogicDisplay.commandClear,25,180,80,0,0,0));borrowed.prepareWorld(java.util.List.of(ground),null);var ownedByHost=ground.buffer;borrowed.close();check(ground.buffer==ownedByHost,"Z0 cache cleanup leaves vanilla-owned FBO intact");
                registry.remove(70001);registry.remove(70002);view.prepareRegistry(registry);check(small.buffer==null&&large.buffer==null,"removed upper buffers released");
                check(Gl.getError()==0,"no GL errors in screen update/draw/release");
            }finally{framebuffer.end();}
            return "PASS "+checks+" actual GPU/native program/pixel/orientation/color/cache/ownership checks";
        }finally{view.close();borrowed.close();registry.clear();Vars.world.tile(memoryIndex,8).remove();models.clear();mesh.dispose();shader.dispose();framebuffer.dispose();}
    }
    public static String patternCode(int n){int half=n/2;return "draw clear 0 0 0 0 0 0\ndraw color 240 20 30 255 0 0\ndraw rect 0 0 "+half+" "+half+" 0 0\ndraw color 30 230 50 255 0 0\ndraw rect "+half+" 0 "+half+" "+half+" 0 0\ndraw color 30 50 235 255 0 0\ndraw rect 0 "+half+" "+half+" "+half+" 0 0\ndraw color 230 220 210 255 0 0\ndraw rect "+half+" "+half+" "+half+" "+half+" 0 0\ndraw color 255 200 20 255 0 0\ndraw stroke 2 0 0 0 0 0\ndraw line 2 2 "+(n-2)+" "+(n-2)+" 0 0\ndraw image "+half+" "+half+" @copper "+(n/5)+" 0 0\nprint \"LIVE\"\ndraw print 4 "+(n-14)+" @left 0 0 0\ndrawflush display1\nend";}
    public static void pattern(LogicBlock.LogicBuild p,LogicDisplay.LogicDisplayBuild d,int n){
        p.updateCode(patternCode(n),false,a->a.putConst("display1",d));
        for(int i=0;i<p.executor.instructions.length;i++)p.executor.runOnce();
    }
    private static void corners(byte[] p,int n,String why){for(int y=0;y<2;y++)for(int x=0;x<2;x++)color(p,n,n*(x==0?1:3)/4,n*(y==0?1:3)/4,x,y,why);}
    private static void color(byte[] p,int width,int x,int y,int ix,int iy,String why){int i=(y*width+x)*4,r=p[i]&255,g=p[i+1]&255,b=p[i+2]&255;
        // Avoid the yellow diagonal used to verify real line drawing.
        if(ix==iy){int offset=5;i=(y*width+x+offset)*4;r=p[i]&255;g=p[i+1]&255;b=p[i+2]&255;}
        check(iy==0?(ix==0?r>210&&g<70&&b<70:g>200&&r<80&&b<100):(ix==0?b>200&&r<80&&g<110:r>200&&g>190&&b>180),why+" quadrant "+ix+","+iy+" got "+r+","+g+","+b);
    }
    private static String source(String name)throws Exception{var f=FpvWorldRenderer.class.getDeclaredField(name);f.setAccessible(true);return (String)f.get(null);}
    private static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
}
