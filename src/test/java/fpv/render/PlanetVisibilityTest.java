package fpv.render;

import fpv.background.*;
import java.util.zip.CRC32;

/** Both named UI switches, per-map round trips, and pre-two-planet profiles. */
public final class PlanetVisibilityTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static byte[] legacy(byte[] bytes,int version){
        bytes=bytes.clone();bytes[7]=(byte)version;
        var crc=new CRC32();crc.update(bytes,0,bytes.length-4);
        int n=(int)crc.getValue(),i=bytes.length-4;
        bytes[i]=(byte)(n>>>24);bytes[i+1]=(byte)(n>>>16);bytes[i+2]=(byte)(n>>>8);bytes[i+3]=(byte)n;
        return bytes;
    }
    public static void main(String[] args)throws Exception{
        String key="a".repeat(64);
        for(boolean host:new boolean[]{false,true})for(boolean serpulo:new boolean[]{false,true})for(boolean erekir:new boolean[]{false,true}){
            var style=new BackgroundStyle();
            style.planetVisible(true,host,serpulo);style.planetVisible(false,host,erekir);
            check(style.planetVisible(true,host)==serpulo,"Serpulo independent on either host");
            check(style.planetVisible(false,host)==erekir,"Erekir independent on either host");
            var copy=style.copy();check(copy.fingerprint()==style.fingerprint(),"both fields staged");
            copy.showOtherPlanet=!copy.showOtherPlanet;check(copy.fingerprint()!=style.fingerprint(),"second body invalidates preview");
            var restored=BackgroundProfileStore.decode(BackgroundProfileStore.encode(key,64,64,style),key,64,64);
            check(restored.fingerprint()==style.fingerprint(),"v4 roundtrip");
        }
        for(boolean visible:new boolean[]{false,true}){
            var old=new BackgroundStyle();old.showPlanet=visible;old.fullSpace=true;old.fixedPhase=.23f;
            byte[] v3=legacy(BackgroundProfileStore.encode(key,64,64,old),3);
            var restored=BackgroundProfileStore.decode(v3,key,64,64);
            check(restored.fingerprint()==old.fingerprint(),"v3 retains all sky settings");
            for(boolean host:new boolean[]{false,true}){
                check(restored.planetVisible(host,host)==visible,"legacy companion retains identity and visibility");
                check(!restored.planetVisible(!host,host),"new body hidden in migrated profile");
            }
        }
        var invalid=new BackgroundStyle();invalid.showOtherPlanet=true;
        try{BackgroundProfileStore.decode(legacy(BackgroundProfileStore.encode(key,64,64,invalid),3),key,64,64);throw new AssertionError("v3 accepted unknown bit");}
        catch(java.io.IOException expected){checks++;}
        System.out.println("PlanetVisibilityTest: "+checks+" independent visibility/copy/roundtrip/v3 migration checks PASS");
    }
}
