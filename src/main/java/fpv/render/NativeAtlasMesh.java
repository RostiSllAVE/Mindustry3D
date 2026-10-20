package fpv.render;

import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.Shader;
import arc.math.Mathf;
import fpv.generated.RenderConfig;
import java.util.*;

/** Meshes reference the game's live atlas textures; they never own or alter those textures. */
final class NativeAtlasMesh {
    private static final VertexAttribute color = new VertexAttribute(4, "a_color");
    private final List<Part> parts = new ArrayList<>();
    long bytes;

    void draw(Shader shader) {
        shader.bind();
        for (Part part : parts) {
            part.texture.bind(0);
            part.mesh.render(shader, Gl.triangles);
        }
    }
    void dispose() { for (Part part : parts) part.mesh.dispose(); parts.clear(); }
    private record Part(Texture texture, Mesh mesh) {}

    static boolean valid(TextureRegion region) {
        return region != null && region.texture != null && region.found();
    }

    static final class Builder {
        private final IdentityHashMap<Texture, Data> data = new IdentityHashMap<>();
        private final IdentityHashMap<Texture, Mesh> streamMeshes = new IdentityHashMap<>();
        private final boolean streaming;
        private final int capacity = (int)RenderConfig.ATLAS_STREAM_VERTICES;
        Shader overflowShader;
        boolean profileWalls;
        private float liquidR,liquidG,liquidB;
        int streamMeshCount(){return streamMeshes.size();}
        Builder(boolean streaming) { this.streaming = streaming; }
        void clear() { for (Data d : data.values()) d.count = 0; }
        void forget(Texture texture){data.remove(texture);Mesh mesh=streamMeshes.remove(texture);if(mesh!=null)mesh.dispose();}
        void dispose() { for (Mesh m : streamMeshes.values()) m.dispose(); streamMeshes.clear(); data.clear(); }

        NativeAtlasMesh upload() {
            NativeAtlasMesh result = new NativeAtlasMesh();
            for (Map.Entry<Texture, Data> e : data.entrySet()) {
                Data d = e.getValue();
                if (d.count == 0) continue;
                Mesh mesh = new Mesh(true, d.count / 9, 0, VertexAttribute.position3, color, VertexAttribute.texCoords);
                mesh.setVertices(d.values, 0, d.count);
                result.parts.add(new Part(e.getKey(), mesh));
                result.bytes += (long)d.count * Float.BYTES;
            }
            return result;
        }

        void drawAndClear(Shader shader) {
            for (Map.Entry<Texture, Data> e : data.entrySet()) flush(e.getKey(), e.getValue(), shader);
        }
        private void flush(Texture texture, Data d, Shader shader) {
            if (d.count == 0) return;
            Mesh mesh = streamMeshes.computeIfAbsent(texture, t -> new Mesh(false, capacity, 0,
                VertexAttribute.position3, color, VertexAttribute.texCoords));
            shader.bind(); texture.bind(0);
            mesh.setVertices(d.values, 0, d.count);
            mesh.render(shader, Gl.triangles);
            if(profileWalls)fpv.profile.WallProfile.draw(d.count/9,d.count);
            d.count = 0;
        }

        /** The sprite's north/top edge follows native +Y, represented by render -Z. */
        void top(TextureRegion region, float x, float y, float z, float width, float depth, float angle,
                 float r, float g, float b, boolean water) {
            if (!valid(region)) return;
            float hw=width/2, hd=depth/2, c=Mathf.cosDeg(angle), s=Mathf.sinDeg(angle);
            quad(region, x-hw*c+hd*s,y,z-hw*s-hd*c, x+hw*c+hd*s,y,z+hw*s-hd*c,
                x+hw*c-hd*s,y,z+hw*s+hd*c, x-hw*c-hd*s,y,z-hw*s+hd*c, r,g,b,water);
        }

        void face(TextureRegion region, float x1,float y1,float z1,float x2,float y2,float z2,
                  float x3,float y3,float z3,float x4,float y4,float z4,float brightness) {
            quad(region,x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,brightness,brightness,brightness,false);
        }

        /** Native gun art rotates as a pitched plane; the fixed base remains in its cached mesh. */
        void pitchedTop(TextureRegion region,float x,float y,float z,float size,float yaw,float pitch){
            float half=size*.5f,c=Mathf.cosDeg(yaw),s=Mathf.sinDeg(yaw),cp=Mathf.cosDeg(pitch),sp=Mathf.sinDeg(pitch);
            float rx=-s*half,rz=-c*half,fx=c*half*cp,fz=-s*half*cp,fy=half*sp;
            quad(region,x+rx+fx,y+fy,z+rz+fz,x-rx+fx,y+fy,z-rz+fz,
                x-rx-fx,y-fy,z-rz-fz,x+rx-fx,y-fy,z+rz-fz,1,1,1,false);
        }

        /** Cached alpha geometry, native rectangular proportions and affine pose; same world depth batch. */
        void extrusion(TextureRegion region,float[] vertices,float x,float y,float z,float width,float thickness,float length,
                       double[] basis,float r,float g,float b){
            if(!valid(region))return;
            Data d=data.computeIfAbsent(region.texture,t->new Data(streaming?capacity*9:4096));
            float du=Math.signum(region.u2-region.u)*.5f/region.texture.width,dv=Math.signum(region.v2-region.v)*.5f/region.texture.height;
            float u=region.u+du,v=region.v+dv,us=region.u2-du-u,vs=region.v2-dv-v;
            for(int i=0;i<vertices.length;i+=SpriteExtrusionGeometry.STRIDE){
                if(streaming&&d.count+9>d.values.length)flush(region.texture,d,overflowShader);
                if(d.count+9>d.values.length)d.values=Arrays.copyOf(d.values,Math.max(d.values.length*2,d.count+9));
                double px=vertices[i]*width,py=vertices[i+1]*thickness,pz=vertices[i+2]*length;
                float shade=vertices[i+4]>.5f?1:vertices[i+4]<-.5f?.58f:.78f;
                d.vertex(x+(float)(basis[0]*px+basis[3]*py+basis[6]*pz),y+(float)(basis[1]*px+basis[4]*py+basis[7]*pz),
                    z+(float)(basis[2]*px+basis[5]*py+basis[8]*pz),r*shade,g*shade,b*shade,0,
                    u+vertices[i+6]*us,v+vertices[i+7]*vs);
            }
        }

        /** Separate transparent-pass shader interprets a_color.a as opacity, not opaque material. */
        void projectionFace(TextureRegion region,float ax,float ay,float az,float bx,float by,float bz,
                            float cx,float cy,float cz,float dx,float dy,float dz,float r,float g,float b,float alpha){
            quad(region,ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,r,g,b,alpha,false);
        }

        void terrainTop(TextureRegion region,float x,float z,float width,float depth,float sw,float se,float ne,float nw,float shade){
            quad(region,x-width/2,sw,z+depth/2,x+width/2,se,z+depth/2,
                x+width/2,ne,z-depth/2,x-width/2,nw,z-depth/2,shade,shade,shade,false,true);
        }

        /** Four triangles retain a natural cell's interior relief even when its edges are anchored.
         * Per-vertex slope lighting and UVs are shared by the floor and its ore overlay. */
        void floorReliefTop(TextureRegion region,float x,float z,float sw,float se,float ne,float nw,float center,
                            float lsw,float lse,float lne,float lnw,float lc){
            if(!valid(region))return;
            Data d=data.computeIfAbsent(region.texture,t->new Data(streaming?capacity*9:4096));
            if(streaming&&d.count+108>d.values.length)flush(region.texture,d,overflowShader);
            if(d.count+108>d.values.length)d.values=Arrays.copyOf(d.values,Math.max(d.values.length*2,d.count+108));
            float du=Math.signum(region.u2-region.u)*.5f/region.texture.width,dv=Math.signum(region.v2-region.v)*.5f/region.texture.height;
            float u=region.u+du,u2=region.u2-du,v=region.v+dv,v2=region.v2-dv,uc=(u+u2)*.5f,vc=(v+v2)*.5f;
            d.vertex(x-.5f,sw,z+.5f,lsw,lsw,lsw,0,u,v2);d.vertex(x+.5f,se,z+.5f,lse,lse,lse,0,u2,v2);d.vertex(x,center,z,lc,lc,lc,0,uc,vc);
            d.vertex(x+.5f,se,z+.5f,lse,lse,lse,0,u2,v2);d.vertex(x+.5f,ne,z-.5f,lne,lne,lne,0,u2,v);d.vertex(x,center,z,lc,lc,lc,0,uc,vc);
            d.vertex(x+.5f,ne,z-.5f,lne,lne,lne,0,u2,v);d.vertex(x-.5f,nw,z-.5f,lnw,lnw,lnw,0,u,v);d.vertex(x,center,z,lc,lc,lc,0,uc,vc);
            d.vertex(x-.5f,nw,z-.5f,lnw,lnw,lnw,0,u,v);d.vertex(x-.5f,sw,z+.5f,lsw,lsw,lsw,0,u,v2);d.vertex(x,center,z,lc,lc,lc,0,uc,vc);
        }

        /** Static liquid fan; alpha 2..3 is an opaque material/wave weight, never transparency. */
        void liquidTop(TextureRegion region,int x,int y,boolean waves){liquidTop(region,x,y,waves,0);}
        void liquidTop(TextureRegion region,int x,int y,boolean waves,float offset){liquidLayer(region,x,y,waves,offset,false);}
        void liquidBottom(TextureRegion region,int x,int y){liquidLayer(region,x,y,false,0,true);}
        private void liquidLayer(TextureRegion region,int x,int y,boolean waves,float offset,boolean bottom){
            if(!valid(region))return;
            Data d=data.get(region.texture);if(d==null){d=new Data(streaming?capacity*9:4096);data.put(region.texture,d);}
            if(streaming&&d.count+108>d.values.length)flush(region.texture,d,overflowShader);
            if(d.count+108>d.values.length)d.values=Arrays.copyOf(d.values,Math.max(d.values.length*2,d.count+108));
            var tile=mindustry.Vars.world.tile(x,y);liquidR=tile.floor().mapColor.r;liquidG=tile.floor().mapColor.g;liquidB=tile.floor().mapColor.b;
            float sw=LiquidSurfaceVisual.corner(x,y),se=LiquidSurfaceVisual.corner(x+1,y),ne=LiquidSurfaceVisual.corner(x+1,y+1),nw=LiquidSurfaceVisual.corner(x,y+1),c=LiquidSurfaceVisual.center(tile);
            sw+=offset;se+=offset;ne+=offset;nw+=offset;c+=offset;
            float asw=2+(waves?LiquidSurfaceVisual.cornerMotion(x,y):0),ase=2+(waves?LiquidSurfaceVisual.cornerMotion(x+1,y):0);
            float ane=2+(waves?LiquidSurfaceVisual.cornerMotion(x+1,y+1):0),anw=2+(waves?LiquidSurfaceVisual.cornerMotion(x,y+1):0),ac=2+(waves?LiquidSurfaceVisual.centerMotion(tile):0);
            if(bottom){sw=(float)fpv.environment.LiquidBody.visualBedCorner(x,y);se=(float)fpv.environment.LiquidBody.visualBedCorner(x+1,y);ne=(float)fpv.environment.LiquidBody.visualBedCorner(x+1,y+1);nw=(float)fpv.environment.LiquidBody.visualBedCorner(x,y+1);c=fpv.environment.LiquidBody.open(tile)?-(float)fpv.environment.LiquidBody.depth(tile.floor()):0;asw=ase=ane=anw=ac=0;}
            else{float k=10+2*fpv.environment.LiquidBody.kind(tile.floor());asw=k+asw-2;ase=k+ase-2;ane=k+ane-2;anw=k+anw-2;ac=k+ac-2;}
            float du=.5f/region.texture.width,dv=.5f/region.texture.height,u=region.u+du,u2=region.u2-du,v=region.v+dv,v2=region.v2-dv;
            liquidVertex(d,x-.5f,sw,-y+.5f,asw,u,v2);liquidVertex(d,x+.5f,se,-y+.5f,ase,u2,v2);liquidVertex(d,x,c,-y,ac,(u+u2)*.5f,(v+v2)*.5f);
            liquidVertex(d,x+.5f,se,-y+.5f,ase,u2,v2);liquidVertex(d,x+.5f,ne,-y-.5f,ane,u2,v);liquidVertex(d,x,c,-y,ac,(u+u2)*.5f,(v+v2)*.5f);
            liquidVertex(d,x+.5f,ne,-y-.5f,ane,u2,v);liquidVertex(d,x-.5f,nw,-y-.5f,anw,u,v);liquidVertex(d,x,c,-y,ac,(u+u2)*.5f,(v+v2)*.5f);
            liquidVertex(d,x-.5f,nw,-y-.5f,anw,u,v);liquidVertex(d,x-.5f,sw,-y+.5f,asw,u,v2);liquidVertex(d,x,c,-y,ac,(u+u2)*.5f,(v+v2)*.5f);
        }
        private void liquidVertex(Data d,float x,float y,float z,float material,float u,float v){
            float shore=material==0?Math.max(.65f,1+y*.12f):1;
            d.vertex(x,y,z,material==0?shore:liquidR,material==0?shore:liquidG,material==0?shore:liquidB,material,u,v);
        }

        /** Low roof + side skirt, no hidden bottom. Each cached outline lies inside one actual
         * planar floor triangle, so its roof cannot cut through the displaced floor. */
        void orePatch(TextureRegion region,OreDepositGeometry.Patch patch,int x,int y,FloorSurfaceRelief floor,float height,float taper,int seed){
            int n=patch.points(),needed=n*9*9;
            Data d=data.get(region.texture);
            if(d==null){d=new Data(streaming?capacity*9:4096);data.put(region.texture,d);}
            if(streaming&&d.count+needed>d.values.length)flush(region.texture,d,overflowShader);
            if(d.count+needed>d.values.length)d.values=Arrays.copyOf(d.values,Math.max(d.values.length*2,d.count+needed));
            float[] uv=patch.uv();float cu=0,cv=0;for(int i=0;i<n;i++){cu+=uv[i*2];cv+=uv[i*2+1];}cu/=n;cv/=n;
            int ridge=Math.floorMod(seed,n);float lean=.10f+((seed>>>24)&7)*.02f;
            float pu=cu+(uv[ridge*2]-cu)*lean,pv=cv+(uv[ridge*2+1]-cv)*lean;
            float gu=cu+(pu-cu)*taper,gv=cv+(pv-cv)*taper;
            float cx=x+gu-.5f,cz=-y+gv-.5f,cy=floor.surface(x,y,gu,gv)+height+FloorSurfaceRelief.ORE_OFFSET;
            float bottom=floor.minimum(x,y)-.005f;
            float du=Math.signum(region.u2-region.u)*.5f/region.texture.width,dv=Math.signum(region.v2-region.v)*.5f/region.texture.height;
            float u=region.u+du,v=region.v+dv,us=region.u2-du-u,vs=region.v2-dv-v;
            float lc=floor.surfaceShade(x,y,cu,cv),side=lc*.88f;
            for(int i=0;i<n;i++){
                int j=(i+1)%n;float au=uv[2*i],av=uv[2*i+1],bu=uv[2*j],bv=uv[2*j+1];
                float ta=cu+(au-cu)*taper,va=cv+(av-cv)*taper,tb=cu+(bu-cu)*taper,vb=cv+(bv-cv)*taper;
                float ax=x+ta-.5f,az=-y+va-.5f,bx=x+tb-.5f,bz=-y+vb-.5f;
                float ay=floor.surface(x,y,ta,va)+height*.76f+FloorSurfaceRelief.ORE_OFFSET;
                float by=floor.surface(x,y,tb,vb)+height*.76f+FloorSurfaceRelief.ORE_OFFSET;
                float la=floor.surfaceShade(x,y,ta,va),lb=floor.surfaceShade(x,y,tb,vb);
                d.vertex(ax,ay,az,la,la,la,0,u+au*us,v+av*vs);d.vertex(bx,by,bz,lb,lb,lb,0,u+bu*us,v+bv*vs);d.vertex(cx,cy,cz,lc,lc,lc,0,u+pu*us,v+pv*vs);
                float su=u+patch.sideU()*us,sv=v+patch.sideV()*vs;
                d.vertex(x+au-.5f,bottom,-y+av-.5f,side,side,side,0,su,sv);d.vertex(x+bu-.5f,bottom,-y+bv-.5f,side,side,side,0,su,sv);d.vertex(bx,by,bz,side,side,side,0,su,sv);
                d.vertex(x+au-.5f,bottom,-y+av-.5f,side,side,side,0,su,sv);d.vertex(bx,by,bz,side,side,side,0,su,sv);d.vertex(ax,ay,az,side,side,side,0,su,sv);
            }
        }

        /** Bakes a cached local prop template into the existing texture batch; no per-object GL draw or mesh. */
        void template(TextureRegion region,float[] vertices,float x,float y,float z,float width,float height,float depth,float angle,float sampleU,float sampleV){
            if(!valid(region)||vertices==null)return;
            Data d=data.computeIfAbsent(region.texture,t->new Data(streaming?capacity*9:4096));
            float c=Mathf.cosDeg(angle),s=Mathf.sinDeg(angle);
            float du=Math.signum(region.u2-region.u)*.5f/region.texture.width,dv=Math.signum(region.v2-region.v)*.5f/region.texture.height;
            float u=region.u+du,v=region.v+dv,us=region.u2-du-u,vs=region.v2-dv-v;
            for(int i=0;i<vertices.length;i+=OrganicPropGeometry.STRIDE){
                if(streaming&&d.count+9>d.values.length)flush(region.texture,d,overflowShader);
                if(d.count+9>d.values.length)d.values=Arrays.copyOf(d.values,Math.max(d.values.length*2,d.count+9));
                float lx=vertices[i]*width,lz=vertices[i+2]*depth,shade=vertices[i+5];
                float tu=vertices[i+3]<0?sampleU:vertices[i+3],tv=vertices[i+4]<0?sampleV:vertices[i+4];
                d.vertex(x+lx*c-lz*s,y+vertices[i+1]*height,z+lx*s+lz*c,shade,shade,shade,0,u+tu*us,v+tv*vs);
            }
        }

        private void quad(TextureRegion region, float ax,float ay,float az,float bx,float by,float bz,
                          float cx,float cy,float cz,float dx,float dy,float dz,float r,float g,float b,boolean water) {
            quad(region,ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,r,g,b,water,false);
        }
        private void quad(TextureRegion region, float ax,float ay,float az,float bx,float by,float bz,
                          float cx,float cy,float cz,float dx,float dy,float dz,float r,float g,float b,boolean water,boolean flipV) {
            quad(region,ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,r,g,b,water?1:0,flipV);
        }
        private void quad(TextureRegion region, float ax,float ay,float az,float bx,float by,float bz,
                          float cx,float cy,float cz,float dx,float dy,float dz,float r,float g,float b,float material,boolean flipV) {
            if (!valid(region)) return;
            Data d=data.computeIfAbsent(region.texture, t -> new Data(streaming ? capacity*9 : 4096));
            if (streaming && d.count+54>d.values.length) flush(region.texture,d,overflowShader);
            if (d.count+54>d.values.length) d.values=Arrays.copyOf(d.values,Math.max(d.values.length*2,d.count+54));
            // Half-texel inset prevents neighboring atlas sprites bleeding into 3D surfaces.
            float du=Math.signum(region.u2-region.u)*.5f/region.texture.width;
            float dv=Math.signum(region.v2-region.v)*.5f/region.texture.height;
            float u=region.u+du, u2=region.u2-du, v=region.v+dv, v2=region.v2-dv;
            if(flipV){float swap=v;v=v2;v2=swap;}
            d.vertex(ax,ay,az,r,g,b,material,u,v); d.vertex(bx,by,bz,r,g,b,material,u2,v); d.vertex(cx,cy,cz,r,g,b,material,u2,v2);
            d.vertex(ax,ay,az,r,g,b,material,u,v); d.vertex(cx,cy,cz,r,g,b,material,u2,v2); d.vertex(dx,dy,dz,r,g,b,material,u,v2);
        }
    }
    private static final class Data {
        float[] values; int count;
        Data(int size){values=new float[size];}
        void vertex(float x,float y,float z,float r,float g,float b,float material,float u,float v){
            values[count++]=x;values[count++]=y;values[count++]=z;
            values[count++]=r;values[count++]=g;values[count++]=b;values[count++]=material;
            values[count++]=u;values[count++]=v;
        }
    }
}
