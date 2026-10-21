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

/** Depth-tested colour portals, followed by real-depth island undersides. Does not clear depth or change any world/simulation state. */
final class CosmicFloorRenderer {
    static final String SETTING="fpv-cosmic-floor";
    private final LinkedHashMap<Integer,Part> parts=new LinkedHashMap<>(32,.75f,true);
    private Shader shader;
    private boolean failed;
    private Object tiles;
    private long bytes;
    private static final long BUDGET=32L*1024*1024;
    private static final class Part {final Mesh mesh;final NativeAtlasMesh islands;final long bytes,fogHash;long checked=arc.util.Time.millis();Part(Mesh m,NativeAtlasMesh islands,long b,long h){mesh=m;this.islands=islands;bytes=b+islands.bytes;fogHash=h;}}
    boolean begin(Camera3D camera){
        if(tiles!=Vars.world.tiles){clear();tiles=Vars.world.tiles;}
        if(!Core.settings.getBool(SETTING,true)||failed)return false;
        if(shader==null)try{shader=new Shader(CosmicField.VERTEX,CosmicField.FRAGMENT);}catch(Throwable e){failed=true;Log.warn("Cosmic floor material unavailable; using original floor. @",e.toString());return false;}
        shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_camera",camera.position);return true;
    }
    void draw(int cx,int cy,int size,boolean fog,Predicate<Tile> visible){
        if(shader==null||failed||!Core.settings.getBool(SETTING,true))return;
        int key=cx|cy<<16;Part p=parts.get(key);long fogHash=p==null?0:p.fogHash;
        if(fog&&(p==null||arc.util.Time.timeSinceMillis(p.checked)>500)){
            fogHash=17;
            for(int y=Math.max(0,cy*size-SpaceIslandGeometry.PAD);y<Math.min(Vars.world.height(),(cy+1)*size+SpaceIslandGeometry.PAD);y++)for(int x=Math.max(0,cx*size-SpaceIslandGeometry.PAD);x<Math.min(Vars.world.width(),(cx+1)*size+SpaceIslandGeometry.PAD);x++)
                fogHash=fogHash*31+(visible.test(Vars.world.tile(x,y))?1:0);
            if(p!=null){p.checked=arc.util.Time.millis();if(p.fogHash!=fogHash){remove(key);p=null;}}
        }
        if(p==null){
            float[] data=CosmicFloorGeometry.build(cx*size,cy*size,Math.min(size,Vars.world.width()-cx*size),Math.min(size,Vars.world.height()-cy*size),(x,y)->{Tile t=Vars.world.tile(x,y);return CosmicFloorGeometry.designated(t)&&visible.test(t)?1:0;});
            Mesh m=null;if(data.length>0){m=new Mesh(true,data.length/3,0,VertexAttribute.position3);m.setVertices(data);}
            var builder=new NativeAtlasMesh.Builder(false);
            new SpaceIslandGeometry(cx*size,cy*size,Math.min(size,Vars.world.width()-cx*size),Math.min(size,Vars.world.height()-cy*size),(x,y)->{Tile t=Vars.world.tile(x,y);return t==null||!visible.test(t)||t.floor().isLiquid?-1:CosmicFloorGeometry.designated(t)?0:1;}).emit((x,y,v,shade)->{
                Tile t=Vars.world.tile(x,y);var region=NativeRegions.floor(t.floor(),t);if(!NativeAtlasMesh.valid(region))return;
                builder.face(region,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],v[10],v[11],shade);
            });
            p=new Part(m,builder.upload(),data.length*4L,fogHash);parts.put(key,p);bytes+=p.bytes;
        }
        if(p.mesh!=null){shader.bind();p.mesh.render(shader,Gl.triangles);}
        // Empty chunks have a small bounded metadata cost too. Never allocate one mesh per star/tile.
        while(bytes>BUDGET||parts.size()>4096)remove(parts.keySet().iterator().next());
    }
    private void remove(int key){Part p=parts.remove(key);if(p!=null){if(p.mesh!=null)p.mesh.dispose();p.islands.dispose();bytes-=p.bytes;}}
    void drawIslands(int cx,int cy,Shader atlas){Part p=parts.get(cx|cy<<16);if(p!=null)p.islands.draw(atlas);}
    void invalidate(int x,int y,int size){for(int cy=Math.max(0,y-SpaceIslandGeometry.PAD)/size;cy<=(y+SpaceIslandGeometry.PAD)/size;cy++)for(int cx=Math.max(0,x-SpaceIslandGeometry.PAD)/size;cx<=(x+SpaceIslandGeometry.PAD)/size;cx++)remove(cx|cy<<16);}
    void clear(){for(Part p:parts.values()){if(p.mesh!=null)p.mesh.dispose();p.islands.dispose();}parts.clear();bytes=0;}
    void dispose(){clear();if(shader!=null){shader.dispose();shader=null;}}
}
