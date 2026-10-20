package fpv.net;
import fpv.world.Unit3DState;import fpv.flight.FlightController;import fpv.generated.FlightProfiles;import java.util.List;
/** Existing protocol4 accepts finite submerged heights; no protocol change or prediction action. */
public final class LiquidWireProbe{
 public static void main(String[] args){var flight=new FlightController(FlightProfiles.create());flight.reset(12,-.45,-12,0);var pose=new Unit3DState(1,2,1,12,12,-.45,0,0,0,0,0,0,-.45,.2,.1,true);var a=new NetProtocol.Snapshot(List.of(pose),new NetProtocol.Flight(1,1,flight.profile(),flight.state()));var b=NetProtocol.snapshot(NetProtocol.snapshot(a));if(!b.units().equals(a.units())||b.local().state().y!=-.45)throw new AssertionError("Submerged protocol state");System.out.println("PASS protocol4 canonical and pilot submerged XYZ round-trip; not a physical multiplayer test");}
}
