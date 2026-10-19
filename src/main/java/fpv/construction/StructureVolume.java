package fpv.construction;

import arc.math.geom.Rect;
import java.util.*;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;

/** Local read-only XYZ selection. Entries describe whole structures, never live contents/configuration. */
public final class StructureVolume {
    public record Area(int minX,int minY,int maxX,int maxY,int anchor,int boundary){
        public Area{if(minX<0||minY<0||maxX<minX||maxY<minY||anchor<0||boundary<0)throw new IllegalArgumentException("Invalid volume selection");}
        public int lower(){return Math.min(anchor,boundary);}public int upper(){return Math.max(anchor,boundary);}
        public int width(){return maxX-minX+1;}public int depth(){return maxY-minY+1;}public int floors(){return upper()-lower()+1;}
        public LayerCoordinates.Bounds bounds(LayerCoordinates c){return new LayerCoordinates.Bounds(minX-.5,minY-.5,c.plane(lower()),maxX+.5,maxY+.5,c.plane(upper())+c.spacing());}
        public Area height(int other){return new Area(minX,minY,maxX,maxY,anchor,other);}
    }
    public record Entry(long id,int x,int y,int layer,int size,int rotation,int team,boolean platform,String blockName,LayerCoordinates.Bounds bounds){}
    private final LayerRuntime runtime;
    private Area committed,draft,queried;
    private List<Entry> entries=List.of();
    private String types="";
    private long geometry=Long.MIN_VALUE,nativeChanges=Long.MIN_VALUE,checkedAt,queries;
    private LayerWorld grid;
    private Object tiles;
    private int team=-1;
    private boolean dragging;
    public StructureVolume(LayerRuntime runtime){this.runtime=runtime;}
    public Area area(){return dragging?draft:committed;}public boolean dragging(){return dragging;}
    public List<Entry> entries(){return entries;}public long queries(){return queries;}
    public void begin(int x,int y,int floor){draft=new Area(x,y,x,y,floor,floor);dragging=true;}
    public void drag(int ax,int ay,int bx,int by){if(dragging)draft=new Area(Math.min(ax,bx),Math.min(ay,by),Math.max(ax,bx),Math.max(ay,by),draft.anchor(),draft.boundary());}
    public void height(int value){var a=area();if(a==null)return;value=Math.max(0,Math.min(runtime.grid().coordinates().maxLayers(),value));if(dragging)draft=a.height(value);else committed=a.height(value);}
    public void finish(){if(dragging){committed=draft;draft=null;dragging=false;}}
    public void abandonDrag(){draft=null;dragging=false;queried=null;}
    public void clear(){committed=draft=queried=null;dragging=false;entries=List.of();types="";}
    /** Bounds changes are immediate; idle metadata/fog changes are sampled at 5 Hz. No map-cell scan. */
    public void refresh(long now){
        Object current=Vars.world==null?null:Vars.world.tiles;int playerTeam=Vars.player==null?-1:Vars.player.team().id;
        if(tiles!=null&&(tiles!=current||grid!=runtime.grid()||team!=playerTeam))clear();
        tiles=current;grid=runtime.grid();team=playerTeam;var a=area();if(a==null){entries=List.of();types="";return;}
        int max=grid.coordinates().maxLayers();if(a.anchor()>max||a.boundary()>max){a=new Area(a.minX(),a.minY(),a.maxX(),a.maxY(),Math.min(max,a.anchor()),Math.min(max,a.boundary()));if(dragging)draft=a;else committed=a;}
        long changes=Vars.world.tileChanges,revision=grid.geometryRevision();
        if(a.equals(queried)&&changes==nativeChanges&&revision==geometry&&now-checkedAt<200)return;
        queried=a;nativeChanges=changes;geometry=revision;checkedAt=now;queries++;
        var found=new LinkedHashMap<Long,Entry>();var box=a.bounds(grid.coordinates());
        // Native team quadtrees include non-updating buildings and return multiblocks only once.
        var rect=new Rect((float)(box.minX()*Vars.tilesize),(float)(box.minY()*Vars.tilesize),(float)((box.maxX()-box.minX())*Vars.tilesize),(float)((box.maxY()-box.minY())*Vars.tilesize));
        for(var t:Team.all)if(t.data().buildingTree!=null)t.data().buildingTree.intersect(rect,b->ground(found,box,b));
        for(var c:grid.query(box)){
            if(c.health()<=0)continue;var b=runtime.building(c.id());
            if(hidden(c.x(),c.y(),b))continue;var occupied=grid.coordinates().bounds(c);
            if(!xy(box,occupied)||!(c.kind()==LayerComponent.Kind.PLATFORM?a.lower()<=c.layer()&&c.layer()<=a.upper():z(box,occupied)))continue;
            found.put(c.id(),new Entry(c.id(),c.x(),c.y(),c.layer(),c.size(),c.rotation(),c.team(),c.kind()==LayerComponent.Kind.PLATFORM,c.blockName(),occupied));
        }
        var sorted=new ArrayList<>(found.values());sorted.sort(Comparator.comparingInt(Entry::layer).thenComparingInt(Entry::y).thenComparingInt(Entry::x).thenComparingLong(Entry::id));entries=List.copyOf(sorted);
        var counts=new TreeMap<String,Integer>();for(var e:entries)counts.merge(e.platform()?"platform":e.blockName(),1,Integer::sum);
        var summary=new StringBuilder();int n=0;for(var e:counts.entrySet()){if(n++==6){summary.append("\n+ ").append(counts.size()-6).append(" more types");break;}if(summary.length()>0)summary.append("\n");summary.append(e.getKey()).append(" x").append(e.getValue());}types=summary.toString();
    }
    private void ground(Map<Long,Entry> out,LayerCoordinates.Bounds box,Building b){
        if(b==null||b.dead||b instanceof ConstructBuild||b.tile==null||Vars.world.build(b.pos())!=b||hidden(b.tileX(),b.tileY(),b))return;
        double x=b.x/(double)Vars.tilesize,y=b.y/(double)Vars.tilesize,half=b.block.size*.5;
        var occupied=new LayerCoordinates.Bounds(x-half,y-half,0,x+half,y+half,fpv.net.HeadlessGeometry.heightFor(b.block));
        if(!xy(box,occupied)||!z(box,occupied))return;long id=LayerVerticalTransfers.groundId(b.tileX(),b.tileY());
        out.put(id,new Entry(id,b.tileX(),b.tileY(),0,b.block.size,b.rotation,b.team.id,false,b.block.name,occupied));
    }
    private boolean hidden(int x,int y,Building b){return Vars.player==null||Vars.state.rules.fog&&Vars.state.rules.staticFog&&!Vars.fogControl.isDiscovered(Vars.player.team(),x,y)||b!=null&&b.inFogTo(Vars.player.team());}
    private static boolean xy(LayerCoordinates.Bounds a,LayerCoordinates.Bounds b){return Math.min(a.maxX(),b.maxX())-Math.max(a.minX(),b.minX())>1e-7&&Math.min(a.maxY(),b.maxY())-Math.max(a.minY(),b.minY())>1e-7;}
    private static boolean z(LayerCoordinates.Bounds a,LayerCoordinates.Bounds b){return Math.min(a.maxZ(),b.maxZ())-Math.max(a.minZ(),b.minZ())>1e-7;}
    public String label(){var a=area();if(a==null)return "No volume. Drag on a visible surface\nor on an explicitly locked empty floor.";return "Z"+a.lower()+" - Z"+a.upper()+" (anchor Z"+a.anchor()+")\n"+a.width()+" x "+a.depth()+" x "+a.floors()+" floors / "+entries.size()+" structures"+(types.isEmpty()?"\nEmpty volume":"\n"+types);}
}
