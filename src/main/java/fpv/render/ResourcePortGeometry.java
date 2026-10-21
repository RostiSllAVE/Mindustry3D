package fpv.render;

/** Reusable, visual-only resource bays: one mouth per footprint cell on every side.
 * Normalized foundation coordinates share the existing mechanical-model vertex format.
 * No ports, routes or transfer permissions are added to the simulation.
 */
public final class ResourcePortGeometry {
    private ResourcePortGeometry(){}
    public record Model(float[] shell,float[] lining,int perSide){
        public int ports(){return perSide*4;}
    }
    public static Model generate(int cells,float height,boolean detailed){
        if(cells<1||cells>16||!Float.isFinite(height)||height<=0||height>1)throw new IllegalArgumentException("Bounded footprint and foundation height required");
        var shell=new MechanicalBlockGeometry.Builder();var lining=new MechanicalBlockGeometry.Builder();
        // One native-textured roof. No opaque side wall remains behind the apertures.
        shell.face(-.49f,height,-.49f,-.49f,height,.49f,.49f,height,.49f,.49f,height,-.49f,0,1,0);
        float cell=.98f/cells,radius=cell*.32f,shoulder=height*.40f,crown=height*.74f,depth=Math.min(.07f,cell*.14f);
        int segments=detailed?4:2;
        for(int side=0;side<4;side++)for(int i=0;i<cells;i++){
            float left=-.49f+i*cell,right=left+cell,center=(left+right)*.5f;
            panel(shell,side,left,0,center-radius,0,center-radius,height,left,height,0,0);
            panel(shell,side,center+radius,0,right,0,right,height,center+radius,height,0,0);
            for(int j=0;j<segments;j++){
                double a=Math.PI-j*Math.PI/segments,b=Math.PI-(j+1)*Math.PI/segments;
                float x0=center+radius*(float)Math.cos(a),x1=center+radius*(float)Math.cos(b);
                float y0=shoulder+(crown-shoulder)*(float)Math.sin(a),y1=shoulder+(crown-shoulder)*(float)Math.sin(b);
                panel(shell,side,x0,y0,x1,y1,x1,height,x0,height,0,0);
                // A real shallow tunnel ceiling, not a dark mark on an intact wall.
                tunnel(lining,side,x0,y0,x1,y1,depth);
            }
            tunnel(lining,side,center-radius,0,center-radius,shoulder,depth);
            tunnel(lining,side,center+radius,shoulder,center+radius,0,depth);
            panel(lining,side,center-radius,0,center+radius,0,center+radius,crown,center-radius,crown,depth,depth);
        }
        return new Model(shell.finish(),lining.finish(),cells);
    }
    private static float x(int side,float along,float inset){return switch(side){case 0->along;case 1->.49f-inset;case 2->-along;default->-.49f+inset;};}
    private static float z(int side,float along,float inset){return switch(side){case 0->.49f-inset;case 1->-along;case 2->-.49f+inset;default->along;};}
    private static void panel(MechanicalBlockGeometry.Builder out,int side,float a,float ay,float b,float by,float c,float cy,float d,float dy,float front,float back){
        out.face(x(side,a,front),ay,z(side,a,front),x(side,b,front),by,z(side,b,front),x(side,c,back),cy,z(side,c,back),x(side,d,back),dy,z(side,d,back),0,0,0);
    }
    private static void tunnel(MechanicalBlockGeometry.Builder out,int side,float a,float ay,float b,float by,float depth){
        panel(out,side,a,ay,b,by,b,by,a,ay,0,depth);
    }
}
