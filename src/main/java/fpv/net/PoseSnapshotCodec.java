package fpv.net;

import fpv.generated.NetworkConfig;
import fpv.world.Unit3DState;
import java.io.*;
import java.util.*;

/** Independent full snapshots. Zero omission and exact float representation only; never quantizes.
 * No previous-frame dictionary, deltas, compression dependency, prediction or gameplay mutation. */
public final class PoseSnapshotCodec {
    private PoseSnapshotCodec(){}
    public static final int MAGIC=0x50534e31;
    private static final int FIELDS=0xfff,DESIRED=1<<9;
    public static byte[] encode(NetProtocol.Snapshot snapshot){
        int count=snapshot.units().size();if(count>NetworkConfig.MAX_UNIT_STATES)throw new IllegalArgumentException("Pose count");
        try{
            var bytes=new ByteArrayOutputStream(Math.min((int)NetworkConfig.MAX_MESSAGE_BYTES,280+count*64));
            var out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeByte(1);out.writeInt(count);
            for(var s:snapshot.units()){
                boolean same=bits(s.z())==bits(s.desiredAltitude());int present=0,wide=0;
                for(int i=0;i<12;i++){
                    if(i==9&&same)continue;double v=field(s,i);valid(v);
                    if(bits(v)!=0){present|=1<<i;if(bits((double)(float)v)!=bits(v))wide|=1<<i;}
                }
                out.writeInt(s.unitId());out.writeInt(s.typeId());out.writeByte(s.teamId());
                // An incompressible record uses the original 12 doubles, with no mask overhead.
                if(4+4*Integer.bitCount(present)+4*Integer.bitCount(wide)>=96){
                    out.writeByte((s.flying()?1:0)|4);for(int i=0;i<12;i++)out.writeDouble(field(s,i));continue;
                }
                out.writeByte((s.flying()?1:0)|(same?2:0));out.writeShort(present);out.writeShort(wide);
                for(int i=0;i<12;i++)if((present&(1<<i))!=0){double v=field(s,i);if((wide&(1<<i))!=0)out.writeDouble(v);else out.writeFloat((float)v);}
            }
            var own=snapshot.local();out.writeBoolean(own!=null);
            if(own!=null){out.writeInt(own.playerId());out.writeInt(own.unitId());NetProtocol.profile(out,own.profile());NetProtocol.state(out,own.state());}
            out.flush();if(bytes.size()>NetworkConfig.MAX_MESSAGE_BYTES)throw new IOException("Pose size");return bytes.toByteArray();
        }catch(IOException e){throw new IllegalArgumentException("Cannot encode poses",e);}
    }
    public static NetProtocol.Snapshot decode(byte[] bytes){return NetProtocol.read(bytes,in->{
        if(in.readInt()!=MAGIC||in.readUnsignedByte()!=1)throw new IOException("Pose schema");
        int n=in.readInt();if(n<0||n>NetworkConfig.MAX_UNIT_STATES||n>(in.available()-1)/10)throw new IOException("Pose count");
        var units=new ArrayList<Unit3DState>(n);var ids=new HashSet<Integer>();
        for(int i=0;i<n;i++){
            int id=in.readInt(),type=in.readInt(),team=in.readUnsignedByte(),flags=in.readUnsignedByte();
            if(!ids.add(id)||(flags&~7)!=0||((flags&4)!=0&&(flags&2)!=0))throw new IOException("Pose flags/identity");
            int p=(flags&4)!=0?FIELDS:in.readUnsignedShort(),w=(flags&4)!=0?FIELDS:in.readUnsignedShort();
            if((p&~FIELDS)!=0||(w&~p)!=0||((flags&2)!=0&&(p&DESIRED)!=0))throw new IOException("Pose masks");
            double x=value(in,p,w,0),y=value(in,p,w,1),z=value(in,p,w,2),vx=value(in,p,w,3),vy=value(in,p,w,4),vz=value(in,p,w,5);
            double yaw=value(in,p,w,6),pitch=value(in,p,w,7),roll=value(in,p,w,8),desired=(flags&2)!=0?z:value(in,p,w,9);
            units.add(new Unit3DState(id,type,team,x,y,z,vx,vy,vz,yaw,pitch,roll,desired,value(in,p,w,10),value(in,p,w,11),(flags&1)!=0));
        }
        NetProtocol.Flight own=in.readBoolean()?new NetProtocol.Flight(in.readInt(),in.readInt(),NetProtocol.profile(in),NetProtocol.state(in)):null;
        return new NetProtocol.Snapshot(List.copyOf(units),own);
    });}
    private static long bits(double v){return Double.doubleToRawLongBits(v);}
    private static void valid(double v){if(!Double.isFinite(v)||Math.abs(v)>1e12)throw new IllegalArgumentException("Bounded finite pose required");}
    private static double value(DataInputStream in,int p,int w,int index)throws IOException{
        if((p&(1<<index))==0)return 0;double v=(w&(1<<index))!=0?in.readDouble():in.readFloat();valid(v);return v;
    }
    private static double field(Unit3DState s,int i){return switch(i){
        case 0->s.x();case 1->s.y();case 2->s.z();case 3->s.vx();case 4->s.vy();case 5->s.vz();
        case 6->s.yaw();case 7->s.pitch();case 8->s.roll();case 9->s.desiredAltitude();case 10->s.radius();case 11->s.halfHeight();default->throw new AssertionError();
    };}
}
