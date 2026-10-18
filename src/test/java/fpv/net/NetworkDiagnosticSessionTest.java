package fpv.net;

import arc.Core;
import arc.files.Fi;
import arc.mock.MockSettings;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.net.Net;
import java.nio.file.*;

/** Real opt-in export lifecycle in a unique temporary profile, without a socket or player files. */
public final class NetworkDiagnosticSessionTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static void poll(NetworkDiagnosticSession d)throws Exception{var f=NetworkDiagnosticSession.class.getDeclaredField("poll");f.setAccessible(true);f.setLong(d,0);d.update(true);}
    public static void main(String[] args)throws Exception{
        Path profile=Files.createTempDirectory("fpv-a201-telemetry-");
        Vars.dataDirectory=new Fi(profile.toFile());Vars.net=new Net(null);Core.settings=new MockSettings();
        Core.settings.put("name","PRIVATE_PLAYER");Core.settings.put("lastip","192.0.2.23");
        var session=new NetworkDiagnosticSession();session.update(true);session.flush();
        check(!session.data.enabled()&&!Files.exists(profile.resolve("fpv-diagnostics")),"ordinary play does not create a diagnostics directory");
        Core.settings.put(NetworkDiagnosticSession.SETTING,true);poll(session);
        check(session.data.enabled(),"menu setting enables collection without JVM options");
        session.identity("payload1-"+"a".repeat(64));session.identity("PRIVATE_PLAYER C:\\private");
        session.data.packet(true,true,123);session.simulationStart();session.simulationEnd();session.drawStart();session.drawEnd();session.flush();
        Path report;try(var files=Files.list(profile.resolve("fpv-diagnostics"))){var all=files.toList();check(all.size()==1,"one bounded report file");report=all.get(0);}
        String text=Files.readString(report);var json=Jval.read(text);
        check(json.getBool("payloadVerified",false)&&json.getInt("role",-1)==0,"numeric verification and role exported");
        check(json.getString("payloadIdentity").equals("payload1-"+"a".repeat(64)),"public payload digest accepted; arbitrary identity text rejected");
        check(json.get("trend").asArray().size==1,"bounded trend row exported");
        check(json.get("counters").getInt("TX_PAYLOAD_BYTES",0)==123,"actual aggregate exported");
        check(!text.contains("PRIVATE_PLAYER")&&!text.contains("192.0.2.23")&&!text.contains(profile.toString()),"no settings identity, endpoint or data path exported");
        check(json.get("metrics").has("SIMULATION_TRIGGER_MS")&&json.get("metrics").has("DRAW_CPU_SPAN_MS"),"trigger spans available");
        Core.settings.put(NetworkDiagnosticSession.SETTING,false);poll(session);check(!session.data.enabled(),"disabling flushes and stops collection");
        session.data.packet(true,true,5);check(session.data.value(NetworkDiagnostics.Counter.TX_PAYLOAD_BYTES)==123,"no collection after stop");
        Path invalid=profile.resolve("file-not-directory");Files.writeString(invalid,"fixture");Vars.dataDirectory=new Fi(invalid.toFile());
        Core.settings.put(NetworkDiagnosticSession.SETTING,true);var unavailable=new NetworkDiagnosticSession();unavailable.update(true);unavailable.flush();
        check(unavailable.data.value(NetworkDiagnostics.Counter.EXPORT_FAILURES)==1,"export IO failure cannot abort gameplay");
        System.out.println("NetworkDiagnosticSessionTest: "+checks+" opt-in/export/privacy/failure-isolation checks PASS");
    }
}
