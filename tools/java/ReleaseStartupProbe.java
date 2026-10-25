import arc.*;
import arc.files.Fi;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.desktop.DesktopLauncher;

/** Test observer only: unmodified production mod, normal initialization, no fpv.smoke flags. */
public final class ReleaseStartupProbe {
    public static void main(String[] args){
        Events.on(ClientLoadEvent.class, event -> Core.app.post(() -> {
            Jval result=Jval.newObject();
            try{
                Fi profile=new Fi(System.getProperty("probe.profile"));
                if(!java.nio.file.Files.isSameFile(Vars.dataDirectory.file().toPath(),profile.file().toPath()))throw new AssertionError("Unexpected data directory");
                var mod=Vars.mods.getMod("mindustry-fpv");
                if(mod==null||mod.main==null||!System.getProperty("probe.version").equals(mod.meta.version))throw new AssertionError("Production mod missing/wrong version");
                if(Vars.mods.list().size!=1)throw new AssertionError("Duplicate/unexpected mods");
                Class<?> type=mod.main.getClass();
                String source=type.getProtectionDomain().getCodeSource().getLocation().toString();
                if(!source.replace('\\','/').contains(profile.child("mods").absolutePath().replace('\\','/')))throw new AssertionError("External FPV classes");
                var field=type.getDeclaredField("network");field.setAccessible(true);Object network=field.get(mod.main);
                if(network==null)throw new AssertionError("Unrelated initialization aborted");
                String token=(String)network.getClass().getMethod("fingerprint").invoke(network);
                if(!token.equals(System.getProperty("probe.identity")))throw new AssertionError("Canonical payload identity mismatch");
                var input=type.getDeclaredField("input");input.setAccessible(true);
                var mouse=type.getDeclaredField("mouse");mouse.setAccessible(true);
                if(input.get(mod.main)==null||mouse.get(mod.main)==null)throw new AssertionError("Input unavailable");
                boolean folder=Boolean.getBoolean("probe.folder");
                if(folder){
                    if(!mod.file.isDirectory()||!mod.prePublish())throw new AssertionError("Native Workshop folder prerequisites failed");
                    var preview=new arc.graphics.Pixmap(mod.file.child("preview.png"));
                    try{if(preview.width!=512||preview.height!=512)throw new AssertionError("Invalid preview");}finally{preview.dispose();}
                }
                result.add("status","PASS");result.add("java",System.getProperty("java.version"));
                result.add("version",mod.meta.version);result.add("identity",token);result.add("source",source);
                result.add("inputBackend",mouse.get(mod.main).getClass().getName());
                result.add("normalStartup",Jval.valueOf(true));result.add("nativeWorkshopPrerequisites",Jval.valueOf(folder));
                result.add("physicalInputAcceptance",Jval.valueOf(false));
                Core.settings.forceSave();
            }catch(Throwable t){result.add("status","FAIL");result.add("error",t.toString());t.printStackTrace();}
            new Fi(System.getProperty("probe.output")).writeString(result.toString(Jval.Jformat.formatted));
            Core.app.exit();
        }));
        DesktopLauncher.main(args);
    }
}
