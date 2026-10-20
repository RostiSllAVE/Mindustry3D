package fpv.render;
import fpv.background.*;import java.nio.file.*;import java.io.*;import java.util.*;import java.util.zip.CRC32;

public final class BackgroundProfileStoreTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void bad(byte[] bytes,String key){try{BackgroundProfileStore.decode(bytes,key,96,64);throw new AssertionError("Invalid data accepted");}catch(IOException expected){checks++;}}
    static byte[] crc(byte[] bytes){var c=new CRC32();c.update(bytes,0,bytes.length-4);int n=(int)c.getValue(),i=bytes.length-4;bytes[i]=(byte)(n>>>24);bytes[i+1]=(byte)(n>>>16);bytes[i+2]=(byte)(n>>>8);bytes[i+3]=(byte)n;return bytes;}
    public static void main(String[] args)throws Exception{
        String key="a".repeat(64),other="b".repeat(64);var style=new BackgroundStyle();style.mode=BackgroundStyle.Mode.CUSTOM;style.customBaseManual=true;style.whole=BackgroundPreset.OCEAN;style.sides.put(BorderBiomeProfile.Side.WEST,BackgroundPreset.FOREST);style.paint(96,64).line(48,140,48,180,70,.7,BackgroundPaint.Brush.ISLAND,14);
        var bytes=BackgroundProfileStore.encode(key,96,64,style);var decoded=BackgroundProfileStore.decode(bytes,key,96,64);check(decoded.fingerprint()==style.fingerprint(),"all config/material/height channels round trip exactly");check(bytes.length<BackgroundProfileStore.MAX_BYTES,"bounded raw payload");
        bad(Arrays.copyOf(bytes,20),key);bad(new byte[BackgroundProfileStore.MAX_BYTES+1],key);bad(bytes,other);var corrupt=bytes.clone();corrupt[12]^=7;bad(corrupt,key);corrupt=bytes.clone();corrupt[7]=99;bad(crc(corrupt),key);corrupt=bytes.clone();corrupt[48]=99;bad(crc(corrupt),key);
        corrupt=bytes.clone();corrupt[60]=(byte)255;bad(crc(corrupt),key); // first material enum
        corrupt=bytes.clone();int at=60+32*BackgroundPaint.NODES*18+32*18;corrupt[at]=1;corrupt[at+1]=0x3f;corrupt[at+2]=(byte)0x80;bad(crc(corrupt),key); // protected native node
        corrupt=bytes.clone();corrupt[61]=0x7f;corrupt[62]=(byte)0xc0;bad(crc(corrupt),key); // NaN coverage
        Path root=Files.createTempDirectory("m3-background-store-");var store=new BackgroundProfileStore(root);check(store.load(key,96,64).style().mode==BackgroundStyle.Mode.AUTO,"missing profile falls back Auto");store.save(key,96,64,style);check(store.load(key,96,64).style().fingerprint()==style.fingerprint(),"new store loads persisted file");
        var original=Files.readAllBytes(store.file(key));style.whole=BackgroundPreset.ICE;store.save(key,96,64,style);check(Arrays.equals(Files.readAllBytes(root.resolve(key+".m3bg.bak")),original),"previous valid bytes preserved as backup");
        Files.write(store.file(key),new byte[]{1,2,3});var fallback=store.load(key,96,64);check(fallback.warning().contains("Recovered")&&fallback.style().whole==BackgroundPreset.OCEAN,"corrupt primary recovers valid backup without deleting it");store.save(key,96,64,style);check(Arrays.equals(Files.readAllBytes(root.resolve(key+".m3bg.invalid")),new byte[]{1,2,3}),"invalid file retained during explicit Apply");
        Files.write(store.file(other),new byte[]{3});check(store.load(other,96,64).style().mode==BackgroundStyle.Mode.AUTO&&!store.load(other,96,64).warning().isEmpty(),"corruption without backup is safe Auto with visible status");
        try{store.file("../../elsewhere");throw new AssertionError("path traversal accepted");}catch(IllegalArgumentException expected){checks++;}
        for(int i=0;i<BackgroundProfileStore.MAX_PROFILES;i++){String k=String.format("%064x",i);if(!Files.exists(store.file(k)))Files.write(store.file(k),new byte[0]);}
        try{store.save("c".repeat(64),96,64,style);throw new AssertionError("unbounded profile count");}catch(IOException expected){checks++;}
        check(Files.list(root).noneMatch(p->p.toString().endsWith(".tmp")),"no abandoned temporary profile");
        System.out.println("BackgroundProfileStoreTest: "+checks+" exact/CRC/version/bounds/map identity/corrupt/backup/atomic/count-limit checks PASS");
    }
}
