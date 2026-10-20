package fpv.render;

import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.Shader;
import java.util.*;

/** Atlas rectangles are constant per face; the shader repeats world-space tile coordinates
 * inside half-texel-inset rectangles instead of stretching interpolated UVs across the collar. */
final class HorizonCollarMesh {
    static final int STRIDE=13;
    private final List<Part> parts=new ArrayList<>();long bytes;
    private record Part(Texture texture,Mesh mesh){}
    void draw(Shader shader){for(var p:parts){p.texture.bind(0);p.mesh.render(shader,Gl.triangles);}}
    void dispose(){for(var p:parts)p.mesh.dispose();parts.clear();}
    static final class Builder {
        final IdentityHashMap<Texture,arc.struct.FloatSeq> data=new IdentityHashMap<>();
        void face(TextureRegion r,float[] a,float[] b,float[] c,float[] d){
            if(!NativeAtlasMesh.valid(r))return;
            var target=data.computeIfAbsent(r.texture,t->new arc.struct.FloatSeq());
            vertex(target,r,a);vertex(target,r,b);vertex(target,r,c);vertex(target,r,a);vertex(target,r,c);vertex(target,r,d);
        }
        void vertex(arc.struct.FloatSeq d,TextureRegion r,float[] v){
            for(int i=0;i<6;i++)d.add(v[i]);d.add(v[7]);
            d.add(r.u+.5f/r.texture.width);d.add(r.v+.5f/r.texture.height);d.add(r.u2-.5f/r.texture.width);d.add(r.v2-.5f/r.texture.height);
            d.add(v[9]);d.add(v[10]);
        }
        HorizonCollarMesh upload(){
            var result=new HorizonCollarMesh();
            for(var e:data.entrySet()){
                var d=e.getValue();var mesh=new Mesh(true,d.size/STRIDE,0,VertexAttribute.position3,new VertexAttribute(4,"a_color"),new VertexAttribute(4,"a_rect"),new VertexAttribute(2,"a_material"));
                mesh.setVertices(d.items,0,d.size);result.parts.add(new Part(e.getKey(),mesh));result.bytes+=(long)d.size*4;
            }
            return result;
        }
    }
}
