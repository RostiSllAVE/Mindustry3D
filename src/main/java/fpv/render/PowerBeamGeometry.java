package fpv.render;
import java.util.Arrays;
/** Camera-independent four-sided tubes. UV carries world-distance phase, not atlas coordinates. */
public final class PowerBeamGeometry {
 private PowerBeamGeometry(){}
 public static final class Builder {
  private float[] values=new float[4096];private int count;private float phase;
  public int vertices(){return count/9;}public float[] finish(){return Arrays.copyOf(values,count);}
  private void v(double x,double y,double z,float r,float g,float b,float u,float length){if(count+9>values.length)values=Arrays.copyOf(values,values.length*2);values[count++]=(float)x;values[count++]=(float)y;values[count++]=(float)z;values[count++]=r;values[count++]=g;values[count++]=b;values[count++]=phase;values[count++]=u;values[count++]=length;}
  public void span(float x,float y,float z,float X,float Y,float Z,float radius,float r,float g,float b){
   double dx=X-x,dy=Y-y,dz=Z-z,len=Math.sqrt(dx*dx+dy*dy+dz*dz);if(!Double.isFinite(len)||len<.0001||!Float.isFinite(radius)||radius<=0)return;phase=(float)(Math.sin((x+X)*.131+(z+Z)*.277+(y+Y)*.19)*3);dx/=len;dy/=len;dz/=len;
   // Choose the least degenerate reference near vertical links, independently of the camera.
   double ax=Math.abs(dy)<.9?0:1,ay=Math.abs(dy)<.9?1:0,rx=-dz*ay,ry=dz*ax,rz=dx*ay-dy*ax,n=Math.sqrt(rx*rx+ry*ry+rz*rz);rx/=n;ry/=n;rz/=n;double ux=dy*rz-dz*ry,uy=dz*rx-dx*rz,uz=dx*ry-dy*rx;
   for(int side=0;side<4;side++){double a=(side+.5)*Math.PI/2,c=(side+1.5)*Math.PI/2;double ox=(rx*Math.cos(a)+ux*Math.sin(a))*radius,oy=(ry*Math.cos(a)+uy*Math.sin(a))*radius,oz=(rz*Math.cos(a)+uz*Math.sin(a))*radius,px=(rx*Math.cos(c)+ux*Math.sin(c))*radius,py=(ry*Math.cos(c)+uy*Math.sin(c))*radius,pz=(rz*Math.cos(c)+uz*Math.sin(c))*radius;
    v(x+ox,y+oy,z+oz,r,g,b,0,(float)len);v(X+ox,Y+oy,Z+oz,r,g,b,1,(float)len);v(X+px,Y+py,Z+pz,r,g,b,1,(float)len);v(x+ox,y+oy,z+oz,r,g,b,0,(float)len);v(X+px,Y+py,Z+pz,r,g,b,1,(float)len);v(x+px,y+py,z+pz,r,g,b,0,(float)len);
   }
  }
 }
}
