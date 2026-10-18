package fpv.net;

import fpv.flight.FlightController;
import fpv.generated.FlightProfiles;
import fpv.world.Unit3DState;
import java.util.*;

/** Deterministic observations of the current wire/display implementation; not a real network test. */
public final class A201NetworkBehaviorTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static Unit3DState pose(int id,double x){return new Unit3DState(id,1,1,x,20,4,10,0,0,0,0,0,4,.5,.2,true);}
    static NetProtocol.Snapshot snap(double x){
        var flight=new FlightController(FlightProfiles.create());flight.reset(x,4,-20,0);
        return new NetProtocol.Snapshot(List.of(pose(1,x)),new NetProtocol.Flight(1,1,flight.profile(),flight.state()));
    }
    public static void main(String[] args){
        RemotePoses poses=new RemotePoses();poses.accept(snap(0),1);poses.accept(snap(1),1.1);
        check(Math.abs(poses.display(1,1.1).x())<1e-8,"new arrival initially displays previous pose");
        check(Math.abs(poses.display(1,1.15).x()-.5)<1e-8,"100ms interpolation gives half a sample at 50ms");
        check(Math.abs(poses.display(1,1.2).x()-1)<1e-8,"latest pose reached at 100ms after arrival");
        var controlled=new FlightController(FlightProfiles.create());poses.copyFlight(controlled,1.15);
        check(Math.abs(controlled.state().x-.5)<1e-8,"local pilot uses same delayed display buffer");
        poses.copyFlight(controlled,20);check(controlled.state().x==1,"outage holds last authority; no prediction/extrapolation");
        poses.clear();check(!poses.copyFlight(controlled,21),"world reset removes old display data");
        int input=NetProtocol.encode(NetProtocol.INPUT,1,1,out->NetProtocol.controls(out,NetworkControls.hold())).length;
        check(input==124,"current input wire bytes: "+input);
        System.out.println("A201 INPUT payload bytes="+input+" (native packet/transport framing excluded)");
        for(int size:new int[]{0,100,1000,4096,16384}){
            var units=new ArrayList<Unit3DState>(size);for(int i=0;i<size;i++)units.add(pose(i,10));
            byte[] encoded=NetProtocol.snapshot(new NetProtocol.Snapshot(units,null));
            check(encoded.length==5+106*size,"wire state scales by 106 bytes/unit");
            int chunks=Math.max(1,(encoded.length+11999)/12000);
            long wire=encoded.length+chunks*(23+2+5+8+4*4); // envelope + UTF channel + chunk header
            check(wire>=encoded.length,"framing accounted");
            check(NetProtocol.snapshot(encoded).units().size()==size,"real decoder count");
            System.out.println("A201 POSES units="+size+" logicalBytes="+encoded.length+" chunks="+chunks+" fpvBytesPerSecondAt10Hz="+(wire*10)+" perRecipient (no owner profile, native sync or other channels)");
        }
        // A missing TCP segment delays all later bytes on that stream. Feed chunks only
        // after a simulated blocked stream resumes; this is a protocol fixture, not WAN measurement.
        var assembly=new NetProtocol.Assembly();
        assembly.accept(new NetProtocol.Chunk("poses",1,0,2,12003,new byte[12000]),0);
        check(assembly.accept(new NetProtocol.Chunk("poses",1,0,2,12003,new byte[12000]),.2)==null,"incomplete sample invisible while delivery blocked");
        check(assembly.accept(new NetProtocol.Chunk("poses",1,1,2,12003,new byte[3]),.5)!=null,"complete sample only after last chunk arrives");
        System.out.println("A201NetworkBehaviorTest: "+checks+" checks PASS; codec/display fixtures only, not two-computer multiplayer");
    }
}
