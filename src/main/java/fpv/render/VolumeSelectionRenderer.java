package fpv.render;

import arc.Core;
import arc.graphics.Gl;
import arc.graphics.g3d.Camera3D;
import arc.graphics.gl.Shader;
import fpv.construction.*;

/** One reused white-atlas stream: translucent volume plus depth-tested member outlines. */
public final class VolumeSelectionRenderer implements AutoCloseable {
    public static final int MAX_OUTLINES=1024;
    private final NativeAtlasMesh.Builder batch=new NativeAtlasMesh.Builder(true);
    private Shader shader;
    private boolean active;
    private StructureVolume selection;
    private LayerCoordinates coordinates;
    private int outlined;
    public void set(boolean active,StructureVolume selection,LayerCoordinates coordinates){this.active=active;this.selection=selection;this.coordinates=coordinates;}
    public int outlined(){return outlined;}
    public void render(Camera3D camera){
        outlined=0;if(!active||selection==null||selection.area()==null)return;
        if(shader==null)shader=new Shader("attribute vec3 a_position;attribute vec4 a_color;attribute vec2 a_texCoord0;uniform mat4 u_combined;varying vec4 c;void main(){c=a_color;gl_Position=u_combined*vec4(a_position,1.0);}","varying vec4 c;void main(){gl_FragColor=c;}");
        boolean depth=Gl.isEnabled(Gl.depthTest),blend=Gl.isEnabled(Gl.blend),cull=Gl.isEnabled(Gl.cullFace),write=Gl.getInt(Gl.depthWritemask)!=0;
        int src=Gl.getInt(Gl.blendSrcRgb),dst=Gl.getInt(Gl.blendDstRgb),sa=Gl.getInt(Gl.blendSrcAlpha),da=Gl.getInt(Gl.blendDstAlpha);
        try{
            Gl.enable(Gl.depthTest);Gl.enable(Gl.blend);Gl.disable(Gl.cullFace);Gl.depthMask(false);Gl.blendFunc(Gl.srcAlpha,Gl.oneMinusSrcAlpha);
            shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);batch.overflowShader=shader;batch.clear();
            var volume=selection.area().bounds(coordinates);fill(volume);box(camera,volume,0,.025,1,.88f,.28f);
            for(var entry:selection.entries()){
                var b=entry.bounds();if(!camera.frustum.containsBounds((float)b.centerX(),(float)b.centerZ(),(float)-b.centerY(),(float)((b.maxX()-b.minX())*.5),(float)((b.maxZ()-b.minZ())*.5),(float)((b.maxY()-b.minY())*.5)))continue;
                if(outlined==MAX_OUTLINES)break;box(camera,b,.012,.014,.32f,1,1);outlined++;
            }
            batch.drawAndClear(shader);
        }finally{Gl.depthMask(write);enabled(Gl.depthTest,depth);enabled(Gl.blend,blend);enabled(Gl.cullFace,cull);Gl.blendFuncSeparate(src,dst,sa,da);}
    }
    private void fill(LayerCoordinates.Bounds b){
        float x=(float)b.minX(),X=(float)b.maxX(),y=(float)b.minZ(),Y=(float)b.maxZ(),z=(float)-b.maxY(),Z=(float)-b.minY();
        face(x,y,z,X,y,z,X,Y,z,x,Y,z);face(X,y,Z,x,y,Z,x,Y,Z,X,Y,Z);
        face(x,y,Z,x,y,z,x,Y,z,x,Y,Z);face(X,y,z,X,y,Z,X,Y,Z,X,Y,z);
        face(x,Y,z,X,Y,z,X,Y,Z,x,Y,Z);face(x,y,Z,X,y,Z,X,y,z,x,y,z);
    }
    private void face(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz){batch.projectionFace(Core.atlas.white(),ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,.24f,.85f,1,.035f);}
    private void box(Camera3D c,LayerCoordinates.Bounds b,double margin,double width,float r,float g,float blue){
        double minX=b.minX()-margin,maxX=b.maxX()+margin,minY=b.minY()-margin,maxY=b.maxY()+margin,minZ=b.minZ()-margin,maxZ=b.maxZ()+margin;
        for(int z=0;z<2;z++){double h=z==0?minZ:maxZ;line(c,minX,minY,h,maxX,minY,h,width,r,g,blue);line(c,maxX,minY,h,maxX,maxY,h,width,r,g,blue);line(c,maxX,maxY,h,minX,maxY,h,width,r,g,blue);line(c,minX,maxY,h,minX,minY,h,width,r,g,blue);}
        for(int x=0;x<2;x++)for(int y=0;y<2;y++)line(c,x==0?minX:maxX,y==0?minY:maxY,minZ,x==0?minX:maxX,y==0?minY:maxY,maxZ,width,r,g,blue);
    }
    private void line(Camera3D c,double ax,double ay,double az,double bx,double by,double bz,double width,float r,float g,float blue){
        double dx=bx-ax,dy=bz-az,dz=ay-by,nx=dy*c.direction.z-dz*c.direction.y,ny=dz*c.direction.x-dx*c.direction.z,nz=dx*c.direction.y-dy*c.direction.x,length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(length<1e-8){nx=c.up.x;ny=c.up.y;nz=c.up.z;length=1;}nx*=width/length;ny*=width/length;nz*=width/length;
        batch.projectionFace(Core.atlas.white(),(float)(ax-nx),(float)(az-ny),(float)(-ay-nz),(float)(ax+nx),(float)(az+ny),(float)(-ay+nz),(float)(bx+nx),(float)(bz+ny),(float)(-by+nz),(float)(bx-nx),(float)(bz-ny),(float)(-by-nz),r,g,blue,.85f);
    }
    private static void enabled(int flag,boolean enabled){if(enabled)Gl.enable(flag);else Gl.disable(flag);}
    @Override public void close(){batch.dispose();if(shader!=null)shader.dispose();shader=null;}
}
