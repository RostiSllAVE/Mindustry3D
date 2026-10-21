package fpv.render;

import java.util.Arrays;

/** Shared low-poly block primitives. XYZ/normal/UV matches the existing atlas extrusion stream.
 * Templates are generated once, shared by all instances, and never mutated by animation.
 */
public final class MechanicalBlockGeometry {
    private MechanicalBlockGeometry(){}
    public record Model(float[] body,float[] pistons){
        public int vertices(){return (body.length+pistons.length)/SpriteExtrusionGeometry.STRIDE;}
        public long bytes(){return (long)(body.length+pistons.length)*Float.BYTES;}
    }
    private static final Model[][] CACHE=new Model[DrillModelProfile.values().length][3];
    public static Model drill(DrillModelProfile profile,int lod){
        if(profile==null||lod<0||lod>2)throw new IllegalArgumentException("Drill profile and LOD required");
        var old=CACHE[profile.ordinal()][lod];if(old!=null)return old;
        var body=new Builder();var moving=new Builder();float deck=profile.deck;
        body.box(-.49f,0,-.49f,.49f,lod==2?.65f:deck,.49f,true);
        if(lod==2)return CACHE[profile.ordinal()][lod]=new Model(body.finish(),moving.finish());
        body.column(0,deck,0,.24f,profile.rotor-.06f,.19f,profile==DrillModelProfile.LASER?4:8);
        body.column(0,profile.rotor-.06f,0,.065f,profile.cap-profile.capDepth*.5f,.065f,4);
        if(profile==DrillModelProfile.MECHANICAL){
            for(int i=0;i<4;i++){
                float x=(i%2==0?-1:1)*.31f,z=(i<2?-1:1)*.31f;
                body.box(x-.065f,deck,z-.065f,x+.065f,.53f,z+.065f,true);
                if(lod==0)moving.box(x-.032f,.48f,z-.032f,x+.032f,.66f,z+.032f,true);
            }
        }else if(profile==DrillModelProfile.PNEUMATIC){
            for(int i=0;i<4;i++){
                double a=i*Math.PI/2;float x=(float)Math.cos(a)*.34f,z=(float)Math.sin(a)*.34f;
                body.column(x,deck,z,.095f,.52f,.085f,lod==0?6:4);
                if(lod==0)moving.column(x,.47f,z,.042f,.68f,.042f,4);
            }
        }else if(profile==DrillModelProfile.LASER){
            for(int i=0;i<4;i++){
                float x=(i%2==0?-1:1)*.32f,z=(i<2?-1:1)*.32f;
                body.box(x-.08f,deck,z-.08f,x+.08f,.73f,z+.08f,true);
                if(lod==0)body.accentBox(x-.10f,.39f,z-.10f,x+.10f,.47f,z+.10f);
            }
        }else{
            body.column(0,deck,0,.36f,.43f,.32f,8);
            for(int i=0;i<4;i++){
                double a=i*Math.PI/2;float x=(float)Math.cos(a)*.34f,z=(float)Math.sin(a)*.34f;
                body.box(x-.09f,deck,z-.09f,x+.09f,.61f,z+.09f,true);
                if(lod==0){moving.column(x,.52f,z,.055f,.74f,.055f,4);
                    body.accentBox(x-.11f,.33f,z-.11f,x+.11f,.40f,z+.11f);}
            }
        }
        return CACHE[profile.ordinal()][lod]=new Model(body.finish(),moving.finish());
    }
    /** No bottom faces under foundations or hidden internal horizontal subdivision layers. */
    public static final class Builder {
        private float[] values=new float[1024];private int count;
        private void vertex(float x,float y,float z,float nx,float ny,float nz){
            if(count+8>values.length)values=Arrays.copyOf(values,values.length*2);
            values[count++]=x;values[count++]=y;values[count++]=z;
            values[count++]=nx;values[count++]=ny;values[count++]=nz;
            values[count++]=ny>.5f?x+.5f:-1;values[count++]=ny>.5f?z+.5f:-1;
        }
        private void face(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,float nx,float ny,float nz){
            vertex(ax,ay,az,nx,ny,nz);vertex(bx,by,bz,nx,ny,nz);vertex(cx,cy,cz,nx,ny,nz);
            vertex(ax,ay,az,nx,ny,nz);vertex(cx,cy,cz,nx,ny,nz);vertex(dx,dy,dz,nx,ny,nz);
        }
        public void box(float x0,float y0,float z0,float x1,float y1,float z1,boolean roof){
            if(roof)face(x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,0,1,0);
            face(x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,0,0,-1);
            face(x1,y0,z1,x0,y0,z1,x0,y1,z1,x1,y1,z1,0,0,1);
            face(x0,y0,z1,x0,y0,z0,x0,y1,z0,x0,y1,z1,-1,0,0);
            face(x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,1,0,0);
        }
        public void accentBox(float x0,float y0,float z0,float x1,float y1,float z1){
            int first=count;box(x0,y0,z0,x1,y1,z1,true);
            for(int i=first;i<count;i+=8){values[i+6]=-2;values[i+7]=-2;}
        }
        public void column(float x,float low,float z,float radius,float high,float topRadius,int sides){
            for(int i=0;i<sides;i++){
                double a=(i+.5)*Math.PI*2/sides,b=(i+1.5)*Math.PI*2/sides;
                float ax=(float)Math.cos(a),az=(float)Math.sin(a),bx=(float)Math.cos(b),bz=(float)Math.sin(b);
                face(x+ax*radius,low,z+az*radius,x+bx*radius,low,z+bz*radius,x+bx*topRadius,high,z+bz*topRadius,x+ax*topRadius,high,z+az*topRadius,ax,0,az);
                vertex(x,high,z,0,1,0);vertex(x+ax*topRadius,high,z+az*topRadius,0,1,0);vertex(x+bx*topRadius,high,z+bz*topRadius,0,1,0);
            }
        }
        public float[] finish(){return Arrays.copyOf(values,count);}
    }
}
