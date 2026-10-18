package fpv.net;

import fpv.world.Unit3DState;
import fpv.flight.FlightController;
import fpv.generated.FlightProfiles;
import java.util.*;
import java.nio.ByteBuffer;

public final class PoseSnapshotCodecTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static void reject(Runnable r){try{r.run();throw new AssertionError("Invalid poses accepted");}catch(IllegalArgumentException expected){checks++;}}
    static Unit3DState pose(int id,double x){return new Unit3DState(id,2,1,x,-0.0,7,0,0,0,90,0,0,7,.5,.2,true);}
    static NetProtocol.Snapshot snapshot(int n,double x){var l=new ArrayList<Unit3DState>();for(int i=0;i<n;i++)l.add(pose(i,x+i));return new NetProtocol.Snapshot(l,null);}
    static List<NetProtocol.Chunk> chunks(NetProtocol.Snapshot s,long rev){
        byte[] data=PoseSnapshotCodec.encode(s);int n=(data.length+11999)/12000;var result=new ArrayList<NetProtocol.Chunk>();
        for(int i=0;i<n;i++)result.add(new NetProtocol.Chunk("poses",rev,i,n,data.length,Arrays.copyOfRange(data,i*12000,Math.min(data.length,(i+1)*12000))));return result;
    }
    public static void main(String[] args){
        double[] special={0,-0.0,Double.MIN_VALUE,-Double.MIN_VALUE,Float.MIN_VALUE,Math.nextUp((double)1.5f),1e12,-1e12,Math.PI,Float.MIN_NORMAL};
        for(double x:special){var s=new NetProtocol.Snapshot(List.of(pose(1,x)),null);var restored=PoseSnapshotCodec.decode(PoseSnapshotCodec.encode(s));check(s.units().equals(restored.units()),"exact double bits including signed zero/subnormal");}
        var rand=new Random(2404);
        for(int trial=0;trial<40;trial++){
            var values=new ArrayList<Unit3DState>();
            for(int i=0;i<200;i++){
                double[] v=new double[12];for(int f=0;f<12;f++)v[f]=switch(rand.nextInt(4)){case 0->0;case 1->-0.0;case 2->(float)(rand.nextFloat()*100);default->rand.nextDouble()*100;};
                values.add(new Unit3DState(i,i%40,i%256,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],Math.abs(v[10]),Math.abs(v[11]),rand.nextBoolean()));
            }
            var s=new NetProtocol.Snapshot(values,null);var out=PoseSnapshotCodec.decode(PoseSnapshotCodec.encode(s));
            for(int i=0;i<values.size();i++)check(values.get(i).equals(out.units().get(i)),"all fields losslessly reconstructed");
        }
        var f=new FlightController(FlightProfiles.create());f.reset(15,10,-20,45);f.state().vy=.123456789;
        var own=new NetProtocol.Flight(4,1,f.profile(),f.state());var restored=PoseSnapshotCodec.decode(PoseSnapshotCodec.encode(new NetProtocol.Snapshot(List.of(pose(1,2)),own)));
        check(Arrays.equals(NetProtocol.bytes(o->{NetProtocol.profile(o,own.profile());NetProtocol.state(o,own.state());}),NetProtocol.bytes(o->{NetProtocol.profile(o,restored.local().profile());NetProtocol.state(o,restored.local().state());})),"own flight profile/momentum/quaternion retain exact doubles");
        check(restored.local().unitId()==1&&restored.local().playerId()==4,"owner identity retained");
        byte[] valid=PoseSnapshotCodec.encode(snapshot(1,3));
        for(int i=0;i<valid.length;i++){byte[] truncated=Arrays.copyOf(valid,i);reject(()->PoseSnapshotCodec.decode(truncated));}
        byte[] bad=valid.clone();bad[0]=0;reject(()->PoseSnapshotCodec.decode(bad));
        byte[] flags=valid.clone();flags[18]=(byte)128;reject(()->PoseSnapshotCodec.decode(flags));
        byte[] count=valid.clone();ByteBuffer.wrap(count).putInt(5,Integer.MAX_VALUE);reject(()->PoseSnapshotCodec.decode(count));
        reject(()->PoseSnapshotCodec.decode(Arrays.copyOf(valid,valid.length+1)));
        reject(()->PoseSnapshotCodec.decode(PoseSnapshotCodec.encode(new NetProtocol.Snapshot(List.of(pose(1,0),pose(1,0)),null))));
        byte[] nan=NetProtocol.bytes(o->{o.writeInt(PoseSnapshotCodec.MAGIC);o.writeByte(1);o.writeInt(1);o.writeInt(1);o.writeInt(1);o.writeByte(1);o.writeByte(0);o.writeShort(1);o.writeShort(1);o.writeDouble(Double.NaN);o.writeBoolean(false);});reject(()->PoseSnapshotCodec.decode(nan));
        for(int n:new int[]{100,1000,4096}){
            var s=snapshot(n,10);byte[] compact=PoseSnapshotCodec.encode(s);
            check(compact.length<NetProtocol.snapshot(s).length,"smaller lossless representative frame");
            check(PoseSnapshotCodec.decode(compact).units().equals(s.units()),"exact requested population "+n);
        }
        var dense=new Unit3DState(1,1,1,Math.PI,Math.E,1d/3,Math.PI,Math.E,1d/3,Math.PI,Math.E,1d/3,Math.E,Math.PI,Math.E,true);
        byte[] denseBytes=PoseSnapshotCodec.encode(new NetProtocol.Snapshot(List.of(dense),null));
        check(denseBytes.length==NetProtocol.snapshot(new NetProtocol.Snapshot(List.of(dense),null)).length+5,"raw record avoids per-unit mask expansion; only fixed schema header grows");
        check(PoseSnapshotCodec.decode(denseBytes).units().get(0).equals(dense),"raw record remains bit exact");
        var assembly=new NetProtocol.Assembly();var remote=new RemotePoses();var old=chunks(snapshot(1000,0),1);
        check(assembly.accept(old.get(0),0)==null,"missing chunks never produce partial replacement");
        var newer=chunks(snapshot(1000,9),2);Collections.reverse(newer);byte[] complete=null;
        for(var c:newer){byte[] result=assembly.accept(c,.5);if(result!=null)complete=result;assembly.accept(c,.51);}
        check(complete!=null,"out-of-order chunks plus duplicates reconstruct one complete frame");remote.accept(PoseSnapshotCodec.decode(complete),.6);
        for(var c:old)check(assembly.accept(c,.7)==null,"delayed older snapshot cannot rewind current pose");
        check(remote.latest(0).x()==9,"new snapshot wins without previous-frame dependency");
        var missing=chunks(snapshot(1000,20),3);for(int i=0;i<missing.size()-1;i++)assembly.accept(missing.get(i),1);
        check(remote.latest(0).x()==9,"dropped frame leaves last complete authority intact");
        for(var c:chunks(snapshot(1000,30),4)){byte[] b=assembly.accept(c,1.2);if(b!=null)remote.accept(PoseSnapshotCodec.decode(b),1.2);}
        check(remote.latest(0).x()==30,"next full snapshot recovers loss without a keyframe request");remote.clear();
        check(remote.latest(0)==null,"reconnect clears old display state");
        System.out.println("PoseSnapshotCodecTest: "+checks+" exact precision/bounds/atomic loss/reorder/duplicate/recovery checks PASS; no socket proof");
    }
}
