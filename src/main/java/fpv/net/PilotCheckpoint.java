package fpv.net;

import fpv.flight.FlightController;
import java.io.IOException;

/** Optional server-only STATE adjunct to a protocol-4 pose revision. No new client claims. */
public record PilotCheckpoint(long revision,int playerId,int unitId,int typeId,int teamId,long inputSequence,
                              double heldSeconds,int kind,double speedMps,double buildSpeedMps,
                              double groundAcceleration,double groundDrag,FlightController.Memory memory) {
    public static final int AIR=0,GROUND=1,FIXED=2,LANDING=3,CREATOR=4;
    private static final int MAGIC=0x50494c31;
    public PilotCheckpoint {
        if(revision<0||inputSequence< -1||teamId<0||teamId>255||kind<AIR||kind>CREATOR||memory==null)
            throw new IllegalArgumentException("Pilot checkpoint identity");
        bounded(heldSeconds,60);bounded(speedMps,400);bounded(buildSpeedMps,60);
        bounded(groundAcceleration,100);bounded(groundDrag,100);
    }
    private static void bounded(double x,double max){if(!Double.isFinite(x)||x<0||x>max)throw new IllegalArgumentException("Pilot checkpoint bounds");}
    public byte[] encode(){return NetProtocol.bytes(out->{
        out.writeInt(MAGIC);out.writeByte(1);out.writeLong(revision);out.writeInt(playerId);out.writeInt(unitId);out.writeInt(typeId);out.writeByte(teamId);
        out.writeLong(inputSequence);out.writeDouble(heldSeconds);out.writeByte(kind);out.writeDouble(speedMps);out.writeDouble(buildSpeedMps);
        out.writeDouble(groundAcceleration);out.writeDouble(groundDrag);out.writeDouble(memory.remainder());out.writeDouble(memory.motorThrottle());
    });}
    public static PilotCheckpoint decode(byte[] bytes){
        if(bytes==null||bytes.length>128)throw new IllegalArgumentException("Pilot checkpoint size");
        return NetProtocol.read(bytes,in->{if(in.readInt()!=MAGIC||in.readUnsignedByte()!=1)throw new IOException("Pilot checkpoint schema");
            return new PilotCheckpoint(in.readLong(),in.readInt(),in.readInt(),in.readInt(),in.readUnsignedByte(),in.readLong(),in.readDouble(),in.readUnsignedByte(),
                NetProtocol.number(in),NetProtocol.number(in),NetProtocol.number(in),NetProtocol.number(in),new FlightController.Memory(NetProtocol.number(in),NetProtocol.number(in)));});
    }
}
