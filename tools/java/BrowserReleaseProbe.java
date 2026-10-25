package modupdater;
import arc.util.serialization.Jval;
import mindustry.core.Version;
import mindustry.mod.ModListing;
import arc.util.serialization.Json;
import java.nio.file.*;

public final class BrowserReleaseProbe {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static final class Offline extends ModUpdater {
  @Override Jval fetchModJson(String repo,String ref){
   if(!repo.equals("RostiSllAVE/Mindustry3D"))throw new AssertionError("Wrong repo");
   try{return ref.equals("v2.0.0-rc.1")?Jval.read(Files.readString(Path.of("mod.hjson"))):Jval.read("{version: '0.18.30'}");}
   catch(Exception e){throw new RuntimeException(e);}
  }
  @Override void query(String url,arc.struct.StringMap params,arc.func.Cons<Jval> callback){throw new AssertionError("No network allowed in offline probe");}
 }
 public static void main(String[] args)throws Exception{
  var updater=new Offline();
  Jval releases=Jval.read("["+
   "{id:314, name:'Mindustry³ Alpha 2.0.0 RC1 [v160.7]', tag_name:'v2.0.0-rc.1', prerelease:true, draft:false, published_at:'2026-10-17T12:00:00Z'},"+
   "{id:313, name:'Old Alpha [v160.7]', tag_name:'v1.0.3-alpha.1', prerelease:false, draft:false, published_at:'2026-10-16T12:00:00Z'},"+
   "{id:315, name:'Future draft [v160.7]', tag_name:'future', prerelease:false, draft:true, published_at:'2026-10-18T12:00:00Z'}]");
  var map=updater.buildReleaseMap("RostiSllAVE/Mindustry3D",releases);
  check(map.has("160.7"),"actual upstream indexer records the exact game revision");
  check(map.get("160.7").getInt("id",0)==314,"published prerelease outranks older stable; draft excluded");
  check(map.get("160.7").getString("version").equals("0.19.0"),"version comes from candidate tag metadata");
  check(updater.findLatestRelease(releases).getInt("id",0)==314,"indexer latest includes non-draft prerelease");
  check(ModUpdater.parseVersionKey("Mindustry³ Alpha 2.0.0 RC1 [v160.7]").equals("160.7"),"exact candidate title parsed by upstream");
  check(ModUpdater.parseVersionKey("Mindustry³ Alpha 2.0.0 RC1")==null,"untagged prerelease cannot make a version-specific release map");
  int oldBuild=Version.build,oldRevision=Version.revision;
  try{
   Version.build=160;Version.revision=7;
   var listing=new Json().fromJson(ModListing.class,"{repo:'RostiSllAVE/Mindustry3D', hasJava:true, releases:"+map.toString()+"}");
   var selected=listing.getMatchingRelease();
   check(selected!=null&&selected.id.equals("314")&&selected.version.equals("0.19.0"),"actual 160.7 browser selects the indexed prerelease ID");
   Version.revision=8;check(listing.getMatchingRelease()==null,"untested 160.8 does not select the candidate");
   Version.build=161;Version.revision=7;check(listing.getMatchingRelease()==null,"161 does not select candidate");
  }finally{Version.build=oldBuild;Version.revision=oldRevision;}
  System.out.println("BrowserReleaseProbe: "+checks+" current upstream algorithm + actual Mindustry160.7 selection checks PASS; offline fixture only");
 }
}
