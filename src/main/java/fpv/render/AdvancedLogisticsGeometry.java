package fpv.render;

import mindustry.content.Blocks;
import mindustry.world.Block;

/** Immutable low-poly templates in the existing atlas extrusion format. No transport state. */
public final class AdvancedLogisticsGeometry {
    private AdvancedLogisticsGeometry(){}
    public enum Kind { BRIDGE, PHASE, UNLOADER, DIRECTIONAL, DRIVER }
    public record Model(float[] body,float[] lining,float[] mechanism){}
    private static final Model[][] CACHE=new Model[Kind.values().length][3];
    public static Kind kind(Block b){
        if(b==Blocks.itemBridge)return Kind.BRIDGE;if(b==Blocks.phaseConveyor)return Kind.PHASE;
        if(b==Blocks.unloader)return Kind.UNLOADER;if(b==Blocks.ductUnloader)return Kind.DIRECTIONAL;
        return b==Blocks.massDriver?Kind.DRIVER:null;
    }
    public static Model model(Kind kind,int lod){
        if(kind==null||lod<0||lod>2)throw new IllegalArgumentException("Advanced logistics kind/LOD required");
        if(CACHE[kind.ordinal()][lod]!=null)return CACHE[kind.ordinal()][lod];
        var body=new MechanicalBlockGeometry.Builder();var moving=new MechanicalBlockGeometry.Builder();float[] lining=new float[0];
        float deck=kind==Kind.DRIVER?.28f:kind==Kind.UNLOADER||kind==Kind.DIRECTIONAL?.72f:.38f;
        if(lod==2)body.box(-.48f,0,-.48f,.48f,deck,.48f,false);
        else{
            var ports=ResourcePortGeometry.generate(kind==Kind.DRIVER?3:1,deck,lod==0);lining=ports.lining();
            var shell=ports.shell(); // native roof is emitted once by the state-aware renderer
            for(int i=48;i<shell.length;i+=48)body.face(shell[i],shell[i+1],shell[i+2],shell[i+8],shell[i+9],shell[i+10],shell[i+16],shell[i+17],shell[i+18],shell[i+40],shell[i+41],shell[i+42],shell[i+3],shell[i+4],shell[i+5]);
        }
        body.face(-.48f,0,-.48f,.48f,0,-.48f,.48f,0,.48f,-.48f,0,.48f,0,-1,0);
        if(kind==Kind.DRIVER){
            body.column(0,deck,0,.34f,.56f,.30f,lod==0?8:4);
            if(lod<2){ // paired launch rails and a rear breech; local +X is launch direction
                moving.box(-.35f,.64f,-.26f,-.17f,1.04f,.26f,true);
                for(int s=-1;s<=1;s+=2)moving.box(-.1f,.76f,s*.22f-.045f,.44f,.94f,s*.22f+.045f,true);
            }
        }else if(kind==Kind.BRIDGE||kind==Kind.PHASE){
            if(lod<2)for(int s=-1;s<=1;s+=2){
                for(int side=-1;side<=1;side+=2)body.box(side*.24f-.055f,deck,s*.24f-.055f,side*.24f+.055f,.91f,s*.24f+.055f,true);
                moving.box(-.31f,.86f,s*.30f-.07f,.31f,1.01f,s*.30f+.07f,true);
            }
            if(kind==Kind.PHASE&&lod==0)for(int s=-1;s<=1;s+=2)moving.box(s*.28f-.045f,.62f,-.24f,s*.28f+.045f,1.13f,.24f,true);
        }else if(lod<2){
            // Low filter chamber leaves clearance around containers and neighboring belts.
            if(kind==Kind.DIRECTIONAL)for(int s=-1;s<=1;s+=2)moving.box(-.40f,deck,s*.30f-.07f,.40f,.98f,s*.30f+.07f,true);
            else for(int s=-1;s<=1;s+=2)body.box(s*.34f-.055f,deck,-.30f,s*.34f+.055f,.98f,.30f,true);
        }
        return CACHE[kind.ordinal()][lod]=new Model(body.finish(),lining,moving.finish());
    }
}
