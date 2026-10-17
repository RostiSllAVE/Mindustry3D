package fpv.net;

import java.nio.file.*;
import java.util.Arrays;
import arc.struct.ArrayMap;
import arc.util.serialization.Jval;
import mindustry.core.Version;
import mindustry.mod.ModListing;
import mindustry.ui.dialogs.ModsDialog;

/** Uses the supported host's actual metadata and release-selection parser. */
public final class BrowserMetadataTest {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        byte[] root=Files.readAllBytes(Path.of("mod.hjson")),packaged=Files.readAllBytes(Path.of("src/main/resources/mod.hjson"));
        check(Arrays.equals(root,packaged),"root and packaged metadata byte-identical");
        Jval meta=Jval.read(new String(root,java.nio.charset.StandardCharsets.UTF_8));
        check(meta.getString("version").equals("0.18.31"),"release version synchronized");
        check(meta.getString("minGameVersion").equals("160.7"),"only release target advertised");
        check(meta.getBool("java",false)&&meta.getString("main").equals("fpv.FpvMod"),"Java entrypoint declared");
        check(meta.has("hideBrowser")&&!meta.getBool("hideBrowser",true),"metadata permits browser discovery");
        check(meta.getString("repo").equals("RostiSllAVE/Mindustry3D"),"renamed repository reference is exact");
        check(meta.getString("version").matches("[0-9]+\\.[0-9]+\\.[0-9]+"),"browser-friendly semantic version");
        check(Integer.parseInt(meta.getString("minGameVersion").split("\\.")[0])>=154,"current Java-mod listing build threshold");
        check(!meta.getBool("hidden",false),"multiplayer mod remains visible to native compatibility checks");
        int build=Version.build,revision=Version.revision;
        try{
            Version.build=160;Version.revision=7;
            check(ModsDialog.matchesGameVersion("Mindustry³ Alpha 1.0.4 [v160.7]"),"recommended release title matches actual 160.7 host parser");
            check(!ModsDialog.matchesGameVersion("Mindustry³ [v160.5]"),"wrong revision rejected");
            check(ModsDialog.matchesGameVersion("Mindustry³ [v160]"),"major-only label broadens selection to untested revisions");
            var listing=new ModListing();listing.releases=new ArrayMap<>();var release=new ModListing.ModRelease();release.id="123";release.version="0.18.31";
            listing.releases.put("160.7",release);check(listing.getMatchingRelease()==release,"browser selects exact release ID, including a prerelease indexed upstream");
            Version.revision=5;check(listing.getMatchingRelease()==null,"nonmatching revision falls back to latest endpoint");
        }finally{Version.build=build;Version.revision=revision;}
        System.out.println("BrowserMetadataTest: "+checks+" actual host metadata/version/explicit release selection checks PASS");
    }
}
