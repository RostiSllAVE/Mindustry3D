package fpv.construction;

import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.math.geom.Ray;
import mindustry.Vars;

/** Presentation-only gesture state. No builder queues, clipboard, networking or world writes. */
public final class VolumeSelectionTool {
    public static final KeyBind toggle=KeyBind.add("fpv-volume-selection",KeyCode.f4,"FPV construction");
    private final LayerRuntime runtime;
    private final StructureVolume selection;
    private boolean active,fence,pressed;
    private int ax,ay;
    private double dragPlane,scrollRemainder;
    private long scrollFrame=Long.MIN_VALUE;
    public VolumeSelectionTool(LayerRuntime runtime){this.runtime=runtime;selection=new StructureVolume(runtime);}
    public boolean active(){return active;}public boolean fence(){return fence;}public StructureVolume selection(){return selection;}
    public void enter(boolean held){active=true;fence=held;pressed=held;scrollRemainder=0;}
    public void leave(boolean held){active=false;selection.abandonDrag();fence|=held;pressed=held;scrollRemainder=0;}
    public void clear(){selection.clear();scrollRemainder=0;}
    public void reset(){active=fence=pressed=false;clear();}
    public void releaseFence(boolean held){if(!held){fence=false;pressed=false;}}
    public void scroll(float amount,long frame){
        if(!active||!Float.isFinite(amount)||frame==scrollFrame||selection.area()==null)return;
        scrollFrame=frame;scrollRemainder+=Math.max(-1024,Math.min(1024,amount));int steps=(int)scrollRemainder;scrollRemainder-=steps;
        if(steps!=0)selection.height(selection.area().boundary()+steps);
    }
    public void pointer(Ray ray,BuildingMousePicker.Hit hit,int manualFloor,boolean locked,boolean tap,boolean held,boolean released,boolean cancel,boolean escape,boolean ui){
        if(!active){releaseFence(held);return;}
        if(escape){leave(held);return;}
        if(cancel){clear();fence=true;pressed=held;return;}
        if(fence){releaseFence(held);return;}
        if(ui){if(released||!held)selection.abandonDrag();pressed=held;return;}
        if(tap&&!pressed){
            int anchor;double x,y;
            if(locked){anchor=manualFloor;dragPlane=runtime.planeHeight(anchor);var point=point(ray,dragPlane);if(point==null){pressed=held;return;}x=point[0];y=point[1];}
            else if(hit!=null){anchor=hit.layer();x=hit.x();y=hit.y();dragPlane=hit.z();}
            else {pressed=held;return;}
            ax=cell(x,Vars.world.width());ay=cell(y,Vars.world.height());selection.begin(ax,ay,anchor);scrollRemainder=0;
        }
        if(selection.dragging()){
            var point=point(ray,dragPlane);if(point!=null)selection.drag(ax,ay,cell(point[0],Vars.world.width()),cell(point[1],Vars.world.height()));
            if(released||!held)selection.finish();
        }
        pressed=held||tap;
    }
    private static int cell(double value,int size){return Math.max(0,Math.min(size-1,(int)Math.floor(value+.5)));}
    private static double[] point(Ray ray,double height){double d=Math.abs(ray.direction.y)<1e-8?-1:(height-ray.origin.y)/ray.direction.y;if(!Double.isFinite(d)||d<0)return null;return new double[]{ray.origin.x+ray.direction.x*d,-ray.origin.z-ray.direction.z*d};}
}
