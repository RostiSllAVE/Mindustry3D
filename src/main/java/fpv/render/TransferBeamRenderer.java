package fpv.render;

import arc.graphics.*;
import arc.graphics.g3d.Camera3D;
import arc.graphics.gl.Shader;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.campaign.*;

/** Bounded additive, depth-tested shipment pulses. Existing native accounting owns every transfer. */
public final class TransferBeamRenderer implements VolumetricBeamGeometry.Sink {
    private static final String VERTEX="attribute vec3 a_position;attribute vec4 a_color;uniform mat4 u_combined;varying vec4 v_color;void main(){v_color=a_color;gl_Position=u_combined*vec4(a_position,1.0);}";
    private static final String FRAGMENT="varying vec4 v_color;void main(){gl_FragColor=v_color;}";
    private final float[] data=new float[24576*7];private final Color color=new Color();private final CampaignBeamState state=new CampaignBeamState();
    private int count,outgoing,incoming;private float alpha;private Mesh mesh;private Shader shader;private Camera3D camera;
    public int outgoing(){return outgoing;}public int incoming(){return incoming;}
    public void clear(){state.clear();count=outgoing=incoming=0;}
    public void render(Camera3D camera,Iterable<Building> placed,fpv.construction.LayerRuntime construction){
        outgoing=incoming=count=0;this.camera=camera;state.begin(construction);
        int sr=Gl.getInt(Gl.blendSrcRgb),dr=Gl.getInt(Gl.blendDstRgb),sa=Gl.getInt(Gl.blendSrcAlpha),da=Gl.getInt(Gl.blendDstAlpha),er=Gl.getInt(Gl.blendEquationRgb),ea=Gl.getInt(Gl.blendEquationAlpha),program=Gl.getInt(Gl.currentProgram),df=Gl.getInt(Gl.depthFunc);
        boolean blend=Gl.isEnabled(Gl.blend),cull=Gl.isEnabled(Gl.cullFace),depth=Gl.isEnabled(Gl.depthTest),write=Gl.getInt(Gl.depthWritemask)!=0;
        try{
            Gl.enable(Gl.blend);Gl.blendEquation(Gl.funcAdd);Gl.blendFuncSeparate(Gl.srcAlpha,Gl.one,Gl.zero,Gl.one);Gl.disable(Gl.cullFace);Gl.enable(Gl.depthTest);Gl.depthMask(false);Gl.depthFunc(Gl.lequal);
            for(var b:placed)if(b.tile!=null&&b.tile.build==b&&!b.dead)submit(b,0);
            if(construction!=null)for(var entry:construction.nativeBuildings().entries()){var c=construction.grid().get(entry.id());if(c!=null)submit(entry.building(),(float)construction.planeHeight(c.layer()));}
            flush();
        }finally{state.end();count=0;Gl.depthMask(write);Gl.depthFunc(df);enabled(Gl.depthTest,depth);enabled(Gl.cullFace,cull);Gl.blendEquationSeparate(er,ea);Gl.blendFuncSeparate(sr,dr,sa,da);enabled(Gl.blend,blend);Gl.useProgram(program);}
    }
    private void submit(Building b,float base){
        if(!(b instanceof LaunchPad.LaunchPadBuild||b instanceof LandingPad.LandingPadBuild)||b.dead||Vars.player!=null&&b.inFogTo(Vars.player.team()))return;
        float progress=state.progress(b);if(progress<0)return;boolean send=b instanceof LaunchPad.LaunchPadBuild;
        float x=b.x/Vars.tilesize,z=-b.y/Vars.tilesize,y=base+FpvWorldRenderer.heightFor(b.block)*.46f+.04f;
        float tip=25*(send?progress:1-progress),tail=send?Math.max(0,tip-7):Math.min(25,tip+7),low=Math.min(tip,tail),high=Math.max(tip,tail);
        if(high-low<.01||!camera.frustum.containsBounds(x,y+(low+high)*.5f,z,.25f,(high-low)*.5f,.25f))return;
        if(send)color.set(((LaunchPad)b.block).lightColor);else color.set(((LandingPad.LandingPadBuild)b).arriving.color);
        float fade=Math.min(1,Math.min(progress+.025f,1-progress+.025f)*8);
        alpha=.20f*fade;VolumetricBeamGeometry.emit(x,y+low,z,x,y+high,z,.16f,color.r,color.g,color.b,camera.position.x,camera.position.y,camera.position.z,this);
        alpha=.68f*fade;VolumetricBeamGeometry.emit(x,y+low,z,x,y+high,z,.045f,1,.96f,.87f,camera.position.x,camera.position.y,camera.position.z,this);
        if(send)outgoing++;else incoming++;
    }
    @Override public void vertex(float x,float y,float z,float r,float g,float b){
        if(count+7>data.length)flush();data[count++]=x;data[count++]=y;data[count++]=z;data[count++]=r;data[count++]=g;data[count++]=b;data[count++]=alpha;
    }
    private void flush(){if(count==0)return;if(shader==null){shader=new Shader(VERTEX,FRAGMENT);mesh=new Mesh(false,data.length/7,0,VertexAttribute.position3,new VertexAttribute(4,"a_color"));}shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);mesh.setVertices(data,0,count);mesh.render(shader,Gl.triangles);count=0;}
    private static void enabled(int cap,boolean on){if(on)Gl.enable(cap);else Gl.disable(cap);}
    public void close(){clear();if(mesh!=null)mesh.dispose();if(shader!=null)shader.dispose();mesh=null;shader=null;}
}
