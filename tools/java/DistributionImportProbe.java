import arc.*;
import arc.files.Fi;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.desktop.DesktopLauncher;

/** Test instrument only; never included on the restart classpath or in player packages. */
public final class DistributionImportProbe {
    public static void main(String[] args){
        Events.on(ClientLoadEvent.class,event->Core.app.post(()->{
            Jval result=Jval.newObject();
            try{
                Fi expected=new Fi(System.getProperty("probe.profile"));
                if(!java.nio.file.Files.isSameFile(Vars.dataDirectory.file().toPath(),expected.file().toPath()))throw new AssertionError("Unexpected profile: "+Vars.dataDirectory.absolutePath()+" vs "+expected.absolutePath());
                if(Vars.mods.getMod("mindustry-fpv")!=null)throw new AssertionError("Profile already contains FPV");
                var mod=Vars.mods.importMod(new Fi(System.getProperty("probe.jar")));
                if(!"0.18.30".equals(mod.meta.version)||mod.main==null)throw new AssertionError("Native import did not load expected metadata/main class");
                result.add("status","PASS");result.add("method","native Mods.importMod API; not a physical menu test");
                result.add("version",mod.meta.version);result.add("installedFile",mod.file.name());
                result.add("java",System.getProperty("java.version"));result.add("requiresRestart",Jval.valueOf(Vars.mods.requiresReload()));
                Core.settings.forceSave();
            }catch(Throwable failure){result.add("status","FAIL");result.add("error",failure.toString());failure.printStackTrace();}
            new Fi(System.getProperty("probe.output")).writeString(result.toString(Jval.Jformat.formatted));
            Core.app.exit();
        }));
        DesktopLauncher.main(args);
    }
}
