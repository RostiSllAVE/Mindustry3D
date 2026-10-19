package fpv.construction;

import fpv.net.NetProtocol;
import java.io.IOException;
import java.util.*;
import mindustry.Vars;

/** Owner-private read-only pending plans; never saved or accepted as construction authority. */
public final class ConstructionPlanCodec {
    private ConstructionPlanCodec(){}
    public static byte[] encode(List<ConstructionPriority.Target> targets){
        if(targets.size()>LayerSchematic.MAX_PLANS)throw new IllegalArgumentException("Pending blueprint limit");
        return NetProtocol.bytes(out->{out.writeByte(1);out.writeShort(targets.size());for(var t:targets){out.writeInt(t.unitId());out.writeShort(t.x());out.writeShort(t.y());out.writeShort(t.layer());out.writeShort(t.platform()?-1:t.block().id);out.writeByte(t.rotation());}});
    }
    public static List<ConstructionPriority.Target> decode(byte[] bytes){
        if(bytes==null||bytes.length>3+LayerSchematic.MAX_PLANS*13)throw new IllegalArgumentException("Pending blueprint bytes");
        return NetProtocol.read(bytes,in->{if(in.readUnsignedByte()!=1)throw new IOException("Blueprint version");int count=in.readUnsignedShort();if(count>LayerSchematic.MAX_PLANS)throw new IOException("Blueprint count");var list=new ArrayList<ConstructionPriority.Target>();var unique=new HashSet<String>();
            for(int i=0;i<count;i++){int unit=in.readInt(),x=in.readUnsignedShort(),y=in.readUnsignedShort(),layer=in.readUnsignedShort(),block=in.readShort(),rotation=in.readUnsignedByte();
                if(unit<0||x>=Vars.world.width()||y>=Vars.world.height()||layer<1||layer>512||block< -1||block>=Vars.content.blocks().size||rotation>3||!unique.add(unit+":"+x+":"+y+":"+layer+":"+(block==-1)))throw new IOException("Invalid pending blueprint");
                list.add(new ConstructionPriority.Target(unit,x,y,layer,block==-1,block==-1?null:Vars.content.block(block),rotation));}
            if(in.available()!=0)throw new IOException("Trailing blueprint bytes");return List.copyOf(list);});
    }
}
