package fpv.construction;

import fpv.net.NetProtocol;
import java.io.IOException;
import java.util.*;
import mindustry.Vars;

/** Owner-private read-only pending plans; never saved or accepted as construction authority. */
public final class ConstructionPlanCodec {
    private ConstructionPlanCodec(){}
    public record Snapshot(List<ConstructionPriority.Target> targets,Map<ConstructionPriority.Target,Double> progress){}
    public static byte[] encode(List<ConstructionPriority.Target> targets){return encode(targets,t->0);}
    public static byte[] encode(List<ConstructionPriority.Target> targets,java.util.function.ToDoubleFunction<ConstructionPriority.Target> progress){
        if(targets.size()>LayerSchematic.MAX_PLANS)throw new IllegalArgumentException("Pending blueprint limit");
        return NetProtocol.bytes(out->{out.writeByte(2);out.writeShort(targets.size());for(var t:targets){out.writeInt(t.unitId());out.writeShort(t.x());out.writeShort(t.y());out.writeShort(t.layer());out.writeShort(t.platform()?-1:t.block().id);out.writeByte(t.rotation());double p=progress.applyAsDouble(t);if(!Double.isFinite(p)||p<0||p>=1)throw new IllegalArgumentException("Blueprint progress");out.writeByte((int)(p*255));}});
    }
    public static List<ConstructionPriority.Target> decode(byte[] bytes){return read(bytes).targets();}
    public static Snapshot read(byte[] bytes){
        if(bytes==null||bytes.length>3+LayerSchematic.MAX_PLANS*14)throw new IllegalArgumentException("Pending blueprint bytes");
        return NetProtocol.read(bytes,in->{int version=in.readUnsignedByte();if(version<1||version>2)throw new IOException("Blueprint version");int count=in.readUnsignedShort();if(count>LayerSchematic.MAX_PLANS)throw new IOException("Blueprint count");var list=new ArrayList<ConstructionPriority.Target>();var unique=new HashSet<String>();var progress=new HashMap<ConstructionPriority.Target,Double>();
            for(int i=0;i<count;i++){int unit=in.readInt(),x=in.readUnsignedShort(),y=in.readUnsignedShort(),layer=in.readUnsignedShort(),block=in.readShort(),rotation=in.readUnsignedByte();
                if(unit<0||x>=Vars.world.width()||y>=Vars.world.height()||layer>512||layer==0&&block==-1||block< -1||block>=Vars.content.blocks().size||rotation>3||!unique.add(unit+":"+x+":"+y+":"+layer+":"+(block==-1)))throw new IOException("Invalid pending blueprint");
                var t=new ConstructionPriority.Target(unit,x,y,layer,block==-1,block==-1?null:Vars.content.block(block),rotation);list.add(t);int p=version==2?in.readUnsignedByte():0;if(p==255)throw new IOException("Completed blueprint progress");if(p>0)progress.put(t,p/255.0);}
            if(in.available()!=0)throw new IOException("Trailing blueprint bytes");return new Snapshot(List.copyOf(list),Map.copyOf(progress));});
    }
}
