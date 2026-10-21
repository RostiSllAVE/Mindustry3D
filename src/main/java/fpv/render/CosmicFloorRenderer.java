package fpv.render;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g3d.Camera3D;
import arc.graphics.gl.Shader;
import arc.util.Log;
import mindustry.Vars;
import mindustry.world.Tile;
import java.util.*;
import java.util.function.Predicate;

/** Depth-writing tile portals. Does not clear depth or change any world/simulation state. */
final class CosmicFloorRenderer {
    static final String SETTING="fpv-cosmic-floor";
    private static final String VERTEX="attribute vec3 a_position;uniform mat4 u_combined;varying vec3 v_world;void main(){v_world=a_position;gl_Position=u_combined*vec4(a_position,1.0);}";
    private static final String FRAGMENT="varying vec3 v_world;uniform vec3 u_camera;"+CosmicField.GLSL+"void main(){vec3 ray=normalize(v_world-u_camera);vec3 distant=normalize(ray+u_camera/100000.0);gl_FragColor=vec4(cosmicField(distant),1.0);}";
    private final LinkedHashMap<Integer,Part> parts=new LinkedHashMap<>(32,.75f,true);
    private Shader shader;
    private boolean failed;
    private Object tiles;
    private long bytes;
    private static final long BUDGET=32L*1024*1024;
    private static final class Part {final Mesh mesh;final long bytes,fogHash;long checked=arc.util.Time.millis();Part(Mesh m,long b,long h){mesh=m;bytes=b;fogHash=h;}}
    void begin(Camera3D camera){
        if(tiles!=Vars.world.tiles){clear();tiles=Vars.world.tiles;}
        if(!Core.settings.getBool(SETTING,true)||failed)return;
        if(shader==null)try{shader=new Shader(VERTEX,FRAGMENT);}catch(Throwable e){failed=true;Log.warn("Cosmic floor material unavailable; using original floor. @",e.toString());return;}
        shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_camera",camera.position);
    }
    void draw(int cx,int cy,int size,boolean fog,Predicate<Tile> visible){
        if(shader==null||failed||!Core.settings.getBool(SETTING,true))return;
        int key=cx|cy<<16;Part p=parts.get(key);long fogHash=p==null?0:p.fogHash;
        if(fog&&(p==null||arc.util.Time.timeSinceMillis(p.checked)>500)){
            fogHash=17;
            for(int y=cy*size;y<Math.min(Vars.world.height(),(cy+1)*size);y++)for(int x=cx*size;x<Math.min(Vars.world.width(),(cx+1)*size);x++)
                fogHash=fogHash*31+(visible.test(Vars.world.tile(x,y))?1:0);
            if(p!=null){p.checked=arc.util.Time.millis();if(p.fogHash!=fogHash){remove(key);p=null;}}
        }
        if(p==null){
            float[] data=CosmicFloorGeometry.build(cx*size,cy*size,Math.min(size,Vars.world.width()-cx*size),Math.min(size,Vars.world.height()-cy*size),(x,y)->{Tile t=Vars.world.tile(x,y);return CosmicFloorGeometry.designated(t)&&visible.test(t)?1:0;});
            Mesh m=null;if(data.length>0){m=new Mesh(true,data.length/3,0,VertexAttribute.position3);m.setVertices(data);}
            p=new Part(m,data.length*4L,fogHash);parts.put(key,p);bytes+=p.bytes;
        }
        if(p.mesh!=null){shader.bind();p.mesh.render(shader,Gl.triangles);}
        // Empty chunks have a small bounded metadata cost too. Never allocate one mesh per star/tile.
        while(bytes>BUDGET||parts.size()>4096)remove(parts.keySet().iterator().next());
    }
    private void remove(int key){Part p=parts.remove(key);if(p!=null){if(p.mesh!=null)p.mesh.dispose();bytes-=p.bytes;}}
    void invalidate(int x,int y,int size){remove(x/size|y/size<<16);}
    void clear(){for(Part p:parts.values())if(p.mesh!=null)p.mesh.dispose();parts.clear();bytes=0;}
    void dispose(){clear();if(shader!=null){shader.dispose();shader=null;}}
}
