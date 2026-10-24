package fpv.render;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.Gl;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g3d.Camera3D;
import fpv.construction.*;
import java.util.*;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicDisplay.LogicDisplayBuild;
import mindustry.world.blocks.logic.CanvasBlock.CanvasBuild;

/** Native command buffers, shared between Z0 and upper model paths. No CPU pixel readbacks/uploads. */
final class LayerDisplayRenderer implements AutoCloseable {
    private final IdentityHashMap<Building,TextureRegion> textures=new IdentityHashMap<>();
    private final Set<Building> present=Collections.newSetFromMap(new IdentityHashMap<>());
    private final boolean ownsBuffers;
    private LayerRuntime runtime;private Object worldTiles;
    private java.util.function.Consumer<Texture> released=texture->{};
    private final Color oldColor=new Color();
    private final java.nio.FloatBuffer oldClear=arc.util.Buffers.newFloatBuffer(4);
    private boolean saved,depth,scissor,cull,blend;private int sr,dr,sa,da,er,ea,program,active,binding;private float mix;
    private int prepared,updated,created;
    LayerDisplayRenderer(){this(true);}
    LayerDisplayRenderer(boolean owns){ownsBuffers=owns;}
    int prepared(){return prepared;}int updated(){return updated;}int created(){return created;}
    void onRelease(java.util.function.Consumer<Texture> callback){released=callback;}
    void prepare(LayerRuntime value){prepare(value,null);}
    void prepare(LayerRuntime value,Camera3D camera){
        if(value!=runtime){close();runtime=value;}if(value==null)return;
        prepareRegistry(value.nativeBuildings(),camera);
    }
    void prepareRegistry(LayerBuildingAdapter registry){prepareRegistry(registry,null);}
    private void prepareRegistry(LayerBuildingAdapter registry,Camera3D camera){
        begin();try{
            for(var entry:registry.entries()){
                float base=(float)(runtime==null?LayerCoordinates.DEFAULT.plane(entry.layer()):runtime.grid().coordinates().plane(entry.layer()));
                visit(entry.building(),camera,base);
            }
            prune();
        }finally{restore();}
    }
    void prepareWorld(Iterable<Building> buildings,Camera3D camera){
        if(worldTiles!=Vars.world.tiles){close();worldTiles=Vars.world.tiles;}
        begin();try{for(Building b:buildings)visit(b,camera,0);prune();}finally{restore();}
    }
    private void begin(){present.clear();prepared=updated=created=0;}
    private void visit(Building building,Camera3D camera,float base){
        if(building.dead||!(building instanceof LogicDisplayBuild||building instanceof CanvasBuild))return;
        present.add(building);
        if(!visible(building,camera,base))return;
        if(building instanceof LogicDisplayBuild&&Vars.renderer!=null&&!Vars.renderer.drawDisplays)return;
        TextureRegion region=textures.computeIfAbsent(building,b->new TextureRegion());Texture previous=region.texture;
        if(building instanceof LogicDisplayBuild display){
            var root=display.rootDisplay==null?display:display.rootDisplay;
            if(root.buffer==null){save();root.ensureBuffer();created++;}
            if(!root.commands.isEmpty()){save();root.processCommands();Draw.flush();updated++;}
            display.getBufferRegion(region);
        }else if(building instanceof CanvasBuild canvas){save();canvas.updateTexture();if(canvas.texture!=null)region.set(canvas.texture);}
        if(previous!=null&&previous!=region.texture)released.accept(previous);
        prepared++;
    }
    private static boolean visible(Building b,Camera3D camera,float base){
        if(camera==null)return true;
        if(Vars.player==null||b.tile==null)return false;
        if(b.tile instanceof LayerTile){
            if(b.team!=Vars.player.team()&&Vars.state.rules.fog&&Vars.fogControl!=null&&!Vars.fogControl.isVisible(Vars.player.team(),b.tile.x,b.tile.y))return false;
        }else if(b.inFogTo(Vars.player.team())||Vars.state.rules.fog&&Vars.state.rules.staticFog&&Vars.fogControl!=null&&!Vars.fogControl.isDiscovered(Vars.player.team(),b.tile.x,b.tile.y))return false;
        float h=fpv.net.HeadlessGeometry.heightFor(b.block),size=b.block.size;
        return camera.frustum.containsBounds(b.x/Vars.tilesize,base+h*.5f,-b.y/Vars.tilesize,size*.6f,h*.5f+.03f,size*.6f);
    }
    private void prune(){var it=textures.entrySet().iterator();while(it.hasNext()){var e=it.next();if(!present.contains(e.getKey())){release(e.getKey(),e.getValue());it.remove();}}}
    // Unchanged visible screens do not even query/alter GL state. The native buffer is sampled directly.
    private void save(){
        if(saved)return;Draw.flush();saved=true;
        depth=Gl.isEnabled(Gl.depthTest);scissor=Gl.isEnabled(Gl.scissorTest);cull=Gl.isEnabled(Gl.cullFace);blend=Gl.isEnabled(Gl.blend);
        sr=Gl.getInt(Gl.blendSrcRgb);dr=Gl.getInt(Gl.blendDstRgb);sa=Gl.getInt(Gl.blendSrcAlpha);da=Gl.getInt(Gl.blendDstAlpha);
        er=Gl.getInt(Gl.blendEquationRgb);ea=Gl.getInt(Gl.blendEquationAlpha);program=Gl.getInt(Gl.currentProgram);active=Gl.getInt(Gl.activeTexture);
        Gl.activeTexture(Gl.texture0);binding=Gl.getInt(Gl.textureBinding2d);
        oldClear.clear();Gl.getFloatv(Gl.colorClearValue,oldClear);oldColor.set(Draw.getColor());mix=Draw.getMixColorPacked();
        Gl.disable(Gl.depthTest);Gl.disable(Gl.scissorTest);Gl.disable(Gl.cullFace);
    }
    private void restore(){
        if(!saved)return;
        Draw.color(oldColor);Draw.mixcol(mix);enabled(Gl.depthTest,depth);enabled(Gl.scissorTest,scissor);enabled(Gl.cullFace,cull);
        Gl.blendEquationSeparate(er,ea);Gl.blendFuncSeparate(sr,dr,sa,da);enabled(Gl.blend,blend);Gl.useProgram(program);
        Gl.clearColor(oldClear.get(0),oldClear.get(1),oldClear.get(2),oldClear.get(3));Gl.activeTexture(Gl.texture0);Gl.bindTexture(Gl.texture2d,binding);Gl.activeTexture(active);saved=false;
    }
    private static void enabled(int cap,boolean value){if(value)Gl.enable(cap);else Gl.disable(cap);}
    TextureRegion texture(Building building){return textures.get(building);}
    private void release(Building b,TextureRegion region){
        if(region.texture!=null)released.accept(region.texture);
        if(!ownsBuffers)return; // Z0 buffers belong to vanilla and survive a view-mode/world-renderer change.
        if(b instanceof LogicDisplayBuild d&&d.buffer!=null){d.buffer.dispose();d.buffer=null;}
        else if(b instanceof CanvasBuild c&&c.texture!=null){c.texture.dispose();c.texture=null;}
    }
    @Override public void close(){for(var e:textures.entrySet())release(e.getKey(),e.getValue());textures.clear();present.clear();runtime=null;worldTiles=null;}
}
