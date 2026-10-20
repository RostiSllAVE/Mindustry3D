package fpv.ui;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.Camera3D;
import arc.graphics.gl.*;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.*;
import arc.util.Buffers;
import fpv.render.EnvironmentRenderer;
import fpv.terrain.TerrainElevation;
import mindustry.Vars;

/** Read-only scenery viewport, sharing the existing renderer, never the gameplay camera/controller. */
public final class BackgroundPreview extends Element implements AutoCloseable {
    private final EnvironmentRenderer environment=new EnvironmentRenderer();
    private final Camera3D camera=new Camera3D();
    private final Texture map;
    private FrameBuffer target;private TextureRegion region;private Mesh plate;private Shader shader;
    private final java.nio.IntBuffer viewport=Buffers.newIntBuffer(4);
    private final java.nio.FloatBuffer clear=Buffers.newFloatBuffer(4);
    private double x,z,distance,yaw=35,pitch=38;private float lastX,lastY;private boolean pan,closed;
    public BackgroundPreview(Texture map){this.map=map;touchable=Touchable.enabled;environment.setTerrain(new TerrainElevation());distance=Math.max(100,Math.hypot(Vars.world.width(),Vars.world.height())*1.4);x=(Vars.world.width()-1)*.5;z=-(Vars.world.height()-1)*.5;
        addListener(new InputListener(){
            @Override public boolean touchDown(InputEvent e,float x,float y,int pointer,KeyCode button){if(pointer!=0)return false;e.stop();lastX=x;lastY=y;pan=button==KeyCode.mouseRight||button==KeyCode.mouseMiddle;Core.scene.setScrollFocus(BackgroundPreview.this);return true;}
            @Override public void touchDragged(InputEvent e,float xx,float yy,int pointer){e.stop();if(pointer!=0)return;float dx=xx-lastX,dy=yy-lastY;lastX=xx;lastY=yy;
                if(pan){double a=Math.toRadians(yaw),s=distance*.002;x-=dx*Math.cos(a)*s+dy*Math.sin(a)*s;z+=dx*Math.sin(a)*s-dy*Math.cos(a)*s;double reach=fpv.render.HorizonGeometry.outerDistance(Vars.world.width(),Vars.world.height());x=Math.max(-reach,Math.min(Vars.world.width()+reach,x));z=Math.max(-Vars.world.height()-reach,Math.min(reach,z));}
                else{yaw=(yaw+dx*.4)%360;pitch=Math.max(5,Math.min(85,pitch-dy*.3));}}
            @Override public boolean scrolled(InputEvent e,float x,float y,float ax,float ay){e.stop();distance=Math.max(12,Math.min(6000,distance*Math.pow(1.15,ay)));return true;}
            @Override public void enter(InputEvent e,float x,float y,int pointer,Element from){if(pointer==-1)Core.scene.setScrollFocus(BackgroundPreview.this);}
            @Override public void exit(InputEvent e,float x,float y,int pointer,Element to){if(pointer==-1&&Core.scene.getScrollFocus()==BackgroundPreview.this)Core.scene.setScrollFocus(null);}
        });
    }
    public void focus(double x,double y){this.x=x;z=-y;}
    public EnvironmentRenderer environment(){return environment;}
    @Override public void draw(){if(closed||getWidth()<2||getHeight()<2)return;Draw.flush();
        float scale=Math.min(arc.scene.ui.layout.Scl.scl(),Math.min(960f/getWidth(),600f/getHeight()));
        int w=Math.max(2,(int)(getWidth()*scale)),h=Math.max(2,(int)(getHeight()*scale));
        boolean depth=Gl.isEnabled(Gl.depthTest),blend=Gl.isEnabled(Gl.blend),cull=Gl.isEnabled(Gl.cullFace),scissor=Gl.isEnabled(Gl.scissorTest),stencil=Gl.isEnabled(Gl.stencilTest);boolean write=Gl.getInt(Gl.depthWritemask)!=0;
        int function=Gl.getInt(Gl.depthFunc),program=Gl.getInt(Gl.currentProgram),fb=Gl.getInt(0x8CA6),active=Gl.getInt(Gl.activeTexture);Gl.activeTexture(Gl.texture1);int texture1=Gl.getInt(Gl.textureBinding2d);Gl.activeTexture(Gl.texture0);int texture=Gl.getInt(Gl.textureBinding2d);
        int src=Gl.getInt(Gl.blendSrcRgb),dst=Gl.getInt(Gl.blendDstRgb),srcA=Gl.getInt(Gl.blendSrcAlpha),dstA=Gl.getInt(Gl.blendDstAlpha),eq=Gl.getInt(Gl.blendEquationRgb),eqA=Gl.getInt(Gl.blendEquationAlpha);
        viewport.clear();Gl.getIntegerv(Gl.viewport,viewport);clear.clear();Gl.getFloatv(Gl.colorClearValue,clear);
        boolean begun=false;try{
            if(target==null||target.getWidth()!=w||target.getHeight()!=h){if(target!=null)target.dispose();target=new FrameBuffer(w,h,true);region=new TextureRegion(target.getTexture());region.flip(false,true);}
            if(plate==null){shader=new Shader("attribute vec3 a_position;attribute vec2 a_texCoord0;uniform mat4 u_combined;varying vec2 v_uv;void main(){v_uv=a_texCoord0;gl_Position=u_combined*vec4(a_position,1.0);}","varying vec2 v_uv;uniform sampler2D u_texture;void main(){gl_FragColor=texture2D(u_texture,v_uv);}");
                float a=-.5f,b=Vars.world.width()-.5f,c=.5f,d=-Vars.world.height()+.5f;plate=new Mesh(true,6,0,VertexAttribute.position3,VertexAttribute.texCoords);plate.setVertices(new float[]{a,0,c,0,1,b,0,c,1,1,b,0,d,1,0,a,0,c,0,1,b,0,d,1,0,a,0,d,0,0});}
            target.begin();begun=true;Gl.disable(Gl.scissorTest);Gl.disable(Gl.stencilTest);Gl.disable(Gl.cullFace);Gl.disable(Gl.blend);Gl.enable(Gl.depthTest);Gl.depthMask(true);Gl.depthFunc(Gl.lequal);Gl.clearColor(0,0,0,1);Gl.clear(Gl.colorBufferBit|Gl.depthBufferBit);
            double angle=Math.toRadians(yaw),rise=Math.toRadians(pitch);camera.resize(w,h);camera.fov=67;camera.near=.05f;camera.up.set(0,1,0);camera.position.set((float)(x+Math.sin(angle)*Math.cos(rise)*distance),(float)(Math.sin(rise)*distance),(float)(z+Math.cos(angle)*Math.cos(rise)*distance));camera.lookAt((float)x,0,(float)z);camera.far=environment.farPlane(camera);camera.update();environment.update(camera);environment.renderBackground(camera);
            Gl.disable(Gl.blend);map.bind(0);shader.bind();shader.setUniformi("u_texture",0);shader.setUniformMatrix4("u_combined",camera.combined.val);plate.render(shader,Gl.triangles);
        }finally{
            if(begun)target.end(); // Restore Arc's framebuffer nesting as well as raw GL bindings.
            Gl.bindFramebuffer(Gl.framebuffer,fb);Gl.viewport(viewport.get(0),viewport.get(1),viewport.get(2),viewport.get(3));Gl.clearColor(clear.get(0),clear.get(1),clear.get(2),clear.get(3));enabled(Gl.depthTest,depth);enabled(Gl.blend,blend);enabled(Gl.cullFace,cull);enabled(Gl.scissorTest,scissor);enabled(Gl.stencilTest,stencil);Gl.depthMask(write);Gl.depthFunc(function);Gl.blendFuncSeparate(src,dst,srcA,dstA);Gl.blendEquationSeparate(eq,eqA);Gl.activeTexture(Gl.texture1);Gl.bindTexture(Gl.texture2d,texture1);Gl.activeTexture(Gl.texture0);Gl.bindTexture(Gl.texture2d,texture);Gl.activeTexture(active);Gl.useProgram(program);
        }
        Draw.color();Draw.rect(region,getX(arc.util.Align.left)+getWidth()*.5f,getY(arc.util.Align.bottom)+getHeight()*.5f,getWidth(),getHeight());Draw.reset();
    }
    private static void enabled(int flag,boolean on){if(on)Gl.enable(flag);else Gl.disable(flag);}
    @Override public void close(){if(closed)return;closed=true;environment.dispose();if(target!=null)target.dispose();if(plate!=null)plate.dispose();if(shader!=null)shader.dispose();if(Core.scene.getScrollFocus()==this)Core.scene.setScrollFocus(null);}
}
