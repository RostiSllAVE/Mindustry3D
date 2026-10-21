package fpv.render;
import fpv.background.*;import java.util.*;import java.util.zip.CRC32;
public final class SpaceAppearanceTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static byte[] v2(byte[] v3){byte[] a=Arrays.copyOf(v3,v3.length-5);a[7]=2;var crc=new CRC32();crc.update(a,0,a.length-4);int n=(int)crc.getValue(),i=a.length-4;a[i]=(byte)(n>>>24);a[i+1]=(byte)(n>>>16);a[i+2]=(byte)(n>>>8);a[i+3]=(byte)n;return a;}
 public static void main(String[] args)throws Exception{
  var style=new BackgroundStyle();String key="d".repeat(64);long original=style.fingerprint();
  style.showPlanet=false;style.showSun=false;style.cycleTime=false;style.fullSpace=true;style.showMapBoundary=true;style.fixedPhase=.23f;
  var copy=style.copy();check(copy.fingerprint()==style.fingerprint(),"all fields copied");copy.showSun=true;check(copy.fingerprint()!=style.fingerprint(),"staged settings independent");
  var restored=BackgroundProfileStore.decode(BackgroundProfileStore.encode(key,64,64,style),key,64,64);check(restored.fingerprint()==style.fingerprint(),"v3 flags/time roundtrip");
  var defaults=new BackgroundStyle();var old=BackgroundProfileStore.decode(v2(BackgroundProfileStore.encode(key,64,64,defaults)),key,64,64);check(old.fingerprint()==original,"v2 migrates with legacy sky defaults");
  style.fixedPhase=Float.NaN;try{BackgroundProfileStore.encode(key,64,64,style);throw new AssertionError("NaN time accepted");}catch(java.io.IOException expected){checks++;}
  var source=(SpaceIslandGeometry.Source)(x,y)->x<0||y<0||x>=64||y>=64?-1:x>=24&&x<40&&y>=12&&y<28?1:0;
  var faces=new ArrayList<float[]>();new SpaceIslandGeometry(0,0,32,32,source).emit((x,y,v,s)->{check(source.kind(x,y)==1,"no geometry in cosmic cells");for(int i=1;i<v.length;i+=3)check(v[i]<=0&&v[i]>=-8,"visual-only negative range");faces.add(v);});
  check(!faces.isEmpty(),"island has lower geometry");var again=new ArrayList<float[]>();new SpaceIslandGeometry(0,0,32,32,source).emit((x,y,v,s)->again.add(v));check(faces.size()==again.size(),"repeat count");for(int i=0;i<faces.size();i++)check(Arrays.equals(faces.get(i),again.get(i)),"deterministic geometry");
  final int[] empty={0};new SpaceIslandGeometry(0,0,32,32,(x,y)->1).emit((x,y,v,s)->empty[0]++);check(empty[0]==0,"ordinary maps generate no undersides");
  var left=new HashSet<String>();var right=new HashSet<String>();
  new SpaceIslandGeometry(0,0,32,32,source).emit((x,y,v,s)->{if(x==31)for(int i=0;i<v.length;i+=3)if(Math.abs(v[i]-31.5f)<.001)left.add(v[i]+":"+v[i+1]+":"+v[i+2]);});
  new SpaceIslandGeometry(32,0,32,32,source).emit((x,y,v,s)->{if(x==32)for(int i=0;i<v.length;i+=3)if(Math.abs(v[i]-31.5f)<.001)right.add(v[i]+":"+v[i+1]+":"+v[i+2]);});
  check(!left.isEmpty()&&left.equals(right),"chunk seams use identical underside nodes");
  System.out.println("SpaceAppearanceTest: "+checks+" profile-v2/v3/NaN/staging/determinism/negative-geometry/seam/no-ordinary-world checks PASS");
 }
}
