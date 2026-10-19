package fpv.construction;

import arc.Core;
import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.math.geom.Ray;
import arc.struct.Seq;
import fpv.input.FpvDesktopInput;
import fpv.render.LayerWorldRenderer;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;

/** UI/preview only. Confirmation sends one bounded authenticated action, never individual free placements. */
public final class VolumePasteTool {
    public static final KeyBind copy=KeyBind.add("fpv-volume-copy",KeyCode.c,"FPV construction"),paste=KeyBind.add("fpv-volume-paste",KeyCode.v,"FPV construction"),rotate=KeyBind.add("fpv-volume-rotate",KeyCode.r,"FPV construction");
    private final LayerRuntime runtime;private final FpvDesktopInput input;private final LayerWorldRenderer renderer;
    private VolumeClipboard.Clip clipboard,turned;private VolumePlacement.Preview preview;private final Seq<BuildPlan> ghosts=new Seq<>();private LayerCoordinates.Bounds previewBounds;private boolean lastFree;
    private boolean active,pressed,initialized;private int x,y,z,turns;private double wheel;private long wheelFrame=Long.MIN_VALUE,revision=-1,tiles=-1,checked;private String message="",copyWarning="";
    public VolumePasteTool(LayerRuntime runtime,FpvDesktopInput input,LayerWorldRenderer renderer){this.runtime=runtime;this.input=input;this.renderer=renderer;}
    public boolean active(){return active;}public boolean available(){return clipboard!=null;}public VolumePlacement.Preview preview(){return preview;}
    public String status(){return (active?"3D PASTE / "+turned.cells().size()+" entries / "+turns*90+" deg\nDestination Z"+z+" - Z"+(z+turned.floors()-1)+"\n":"")+message+(copyWarning.isBlank()?"":"\nCopy warning: "+copyWarning);}
    public void copy(StructureVolume model){try{var captured=VolumeClipboard.capture(runtime,model,Vars.player);clipboard=captured.clip();copyWarning=captured.warning();message="";start();}catch(IllegalArgumentException bad){cancel();message=bad.getMessage();runtime.acceptResult(LayerActions.Result.rejected(message.length()>240?"Cannot copy this 3D volume":message));}}
    public void start(){if(clipboard==null)return;turned=clipboard;turns=0;active=true;initialized=false;pressed=input.volumePointerHeld();preview=null;wheel=0;revision=-1;input.cancelPointerGesture();}
    public void cancel(){active=false;preview=null;renderer.schematic(null,null,0);renderer.copySelection(null);}
    public void reset(){cancel();clipboard=turned=null;message=copyWarning="";}
    public void process(Ray ray,BuildingMousePicker.Hit hit,int manual,boolean locked,boolean free,LayerBuildController.Buttons keys){
        if(!active)return;var raw=input.originalInput();long frame=Core.graphics.getFrameId();
        if(keys.selectReleased()||!keys.selectHeld()&&!keys.selectTap())pressed=false;
        if(!initialized&&!keys.ui()){
            var target=MouseConstructionTarget.resolve(hit,ray,runtime.grid().coordinates(),null,false,locked,manual);
            if(target.resolved()){z=Math.max(0,Math.min(runtime.grid().coordinates().maxLayers()-turned.floors()+1,target.layer()));initialized=true;}
        }
        if(!initialized){message="Aim at a visible construction surface or lock an empty floor";return;}
        if(!keys.ui()){
            if(wheelFrame!=frame){wheelFrame=frame;float amount=raw.axis(KeyCode.scroll);if(Float.isFinite(amount)){wheel+=Math.max(-1024,Math.min(1024,amount));int step=(int)wheel;wheel-=step;if(step!=0){z=Math.max(0,Math.min(runtime.grid().coordinates().maxLayers()-turned.floors()+1,z+step));revision=-1;}}}
            if(raw.keyTap(rotate)){int count=keys.shift()?3:1;for(int i=0;i<count;i++)turned=VolumeClipboard.rotate(turned);turns=(turns+count)%4;revision=-1;}
            var target=MouseConstructionTarget.resolve(hit,ray,runtime.grid().coordinates(),null,false,true,z);
            if(!target.resolved()){message="Point at the destination floor; no paste on a ray miss";renderer.schematic(null,null,0);renderer.copySelection(null);preview=null;return;}
            if(target.resolved()){int nx=target.x()-turned.width()/2,ny=target.y()-turned.depth()/2;if(nx!=x||ny!=y){x=nx;y=ny;revision=-1;}}
        }
        long now=arc.util.Time.millis();if(preview==null||lastFree!=free||revision!=runtime.grid().geometryRevision()||tiles!=Vars.world.tileChanges||now-checked>=200){
            lastFree=free;revision=runtime.grid().geometryRevision();tiles=Vars.world.tileChanges;checked=now;var pos=new VolumeClipboard.Placement(turned,x,y,z);
            try{preview=runtime.pastes().previewVolume(Vars.player,LayerActions.Action.volume(pos,free?LayerComponent.Mode.FREE:LayerComponent.Mode.STRUCTURAL),false);}
            catch(IllegalArgumentException bad){preview=new VolumePlacement.Preview(pos,pos.plans(),new boolean[turned.cells().size()],java.util.List.of(),bad.getMessage());}
            message=(preview.accepted()?"Ready — LMB queues normal construction":preview.reason());ghosts.clear();for(var p:preview.plans())ghosts.add(p);previewBounds=bounds();
        }
        renderer.schematic(ghosts,preview.valid(),z);renderer.copySelection(previewBounds);
        if(keys.selectTap()&&!pressed&&!keys.ui()){pressed=true;if(!preview.accepted()){runtime.acceptResult(preview.result());return;}
            var result=runtime.submit(LayerActions.Action.volume(preview.placement(),free?LayerComponent.Mode.FREE:LayerComponent.Mode.STRUCTURAL));runtime.acceptResult(result);message=result.reason();
            if(result.accepted()||result.pending()){cancel();input.fenceVolumePointer();}
        }
    }
    private LayerCoordinates.Bounds bounds(){
        double bottom=Double.POSITIVE_INFINITY,top=0;for(var p:preview.plans()){double base=runtime.planeHeight(p.floor);var e=runtime.catalog().entry(p.block.name,p.platform,runtime.grid().coordinates().spacing());double height=p.platform?LayerCoordinates.PLATFORM_THICKNESS:p.floor==0?fpv.net.HeadlessGeometry.heightFor(p.block):e==null?1:e.height();bottom=Math.min(bottom,base-(p.platform?height:0));top=Math.max(top,base+(p.platform?0:height));}
        return new LayerCoordinates.Bounds(x-.5,y-.5,bottom,x+turned.width()-.5,y+turned.depth()-.5,top);
    }
}
