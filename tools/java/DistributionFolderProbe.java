import arc.*;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.desktop.DesktopLauncher;

/** Read-only Workshop prerequisite check. Never calls publish or any Steam write API. */
public final class DistributionFolderProbe {
    public static void main(String[] args){
        Events.on(ClientLoadEvent.class,event->Core.app.post(()->{
            var result=Jval.newObject();
            try{
                var mod=Vars.mods.getMod("mindustry-fpv");
                if(mod==null||mod.main==null||!mod.file.isDirectory()||!"0.18.30".equals(mod.meta.version))throw new AssertionError("Expected loaded compiled folder mod");
                if(!mod.prePublish())throw new AssertionError("Native publisher prerequisite validation failed");
                Pixmap preview=new Pixmap(mod.file.child("preview.png"));
                try{if(preview.width!=512||preview.height!=512)throw new AssertionError("Invalid preview size");}finally{preview.dispose();}
                var field=mod.main.getClass().getDeclaredField("network");field.setAccessible(true);Object network=field.get(mod.main);
                String token=(String)network.getClass().getMethod("fingerprint").invoke(network);
                if(!token.matches("payload1-[0-9a-f]{64}"))throw new AssertionError("Folder verification unavailable");
                result.add("status","PASS");result.add("nativePrePublish",Jval.valueOf(true));result.add("preview","512x512 native Pixmap decode passed");
                result.add("payloadIdentity",token);result.add("published",Jval.valueOf(false));
            }catch(Throwable failure){result.add("status","FAIL");result.add("error",failure.toString());failure.printStackTrace();}
            new Fi(System.getProperty("probe.output")).writeString(result.toString(Jval.Jformat.formatted));Core.app.exit();
        }));
        DesktopLauncher.main(args);
    }
}
