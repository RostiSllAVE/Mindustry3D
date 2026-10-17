package fpv.net;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Container-independent compatibility identity, computed once at startup.
 * All payload bytes and their paths participate; ZIP dates/compression and Steam
 * publication metadata do not. This is compatibility checking, not authentication. */
public final class ModFingerprint {
    private ModFingerprint(){}
    public record Verification(String token,String problem){
        public boolean verified(){return problem==null&&token!=null&&token.matches("payload1-[0-9a-f]{64}");}
        public boolean matches(String peer){return verified()&&token.equals(peer);}
    }
    public static Verification current(Class<?> anchor){
        try{return inspect(Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI()));}
        catch(Exception unavailable){return unavailable(unavailable);}
    }
    public static Verification inspect(Path root){
        try{return new Verification("payload1-"+hash(root),null);}
        catch(Exception unavailable){return unavailable(unavailable);}
    }
    private static Verification unavailable(Exception failure){return new Verification(null,failure.getClass().getSimpleName()+": "+failure.getMessage());}
    private static boolean included(String name)throws IOException{
        if(name.isEmpty()||name.startsWith("/")||name.contains("\\")||name.contains(":")||Arrays.asList(name.split("/",-1)).stream().anyMatch(p->p.isEmpty()||p.equals(".")||p.equals("..")))
            throw new IOException("Unsafe payload path");
        return !Set.of("steamid.json","preview.png","icon.png").contains(name);
    }
    private interface Content { InputStream open()throws IOException; }
    private record Entry(long length,Content content){}
    private static String hash(Path root)throws IOException,GeneralSecurityException{
        var entries=new TreeMap<String,Entry>();
        if(Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)){
            try(var files=Files.walk(root)){
                for(Path p:files.toList()){
                    if(Files.isSymbolicLink(p))throw new IOException("Symbolic link in mod payload");
                    if(Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS))continue;
                    if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))throw new IOException("Non-file in mod payload");
                    String name=root.relativize(p).toString().replace(File.separatorChar,'/');
                    if(included(name))entries.put(name,new Entry(Files.size(p),()->Files.newInputStream(p)));
                }
            }
            return digest(entries);
        }
        if(!Files.isRegularFile(root,LinkOption.NOFOLLOW_LINKS))throw new IOException("Mod payload is neither a directory nor an archive");
        try(var zip=new ZipFile(root.toFile())){
            var all=zip.entries();var names=new HashSet<String>();
            while(all.hasMoreElements()){
                ZipEntry entry=all.nextElement();if(entry.isDirectory())continue;
                String name=entry.getName();
                if(!names.add(name))throw new IOException("Duplicate archive path");
                if(included(name))entries.put(name,new Entry(entry.getSize(),()->zip.getInputStream(entry)));
            }
            return digest(entries);
        }
    }
    private static String digest(SortedMap<String,Entry> entries)throws IOException,GeneralSecurityException{
        if(!entries.containsKey("fpv/FpvMod.class")||!entries.containsKey("mod.hjson"))throw new IOException("Incomplete compiled mod payload");
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try(var output=new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(),hash))){
            output.write("Mindustry3-payload-v1\0".getBytes(StandardCharsets.UTF_8));
            byte[] buffer=new byte[65536];
            for(var item:entries.entrySet()){
                byte[] name=item.getKey().getBytes(StandardCharsets.UTF_8);Entry entry=item.getValue();
                if(entry.length()<0)throw new IOException("Unknown payload length");
                output.writeInt(name.length);output.write(name);output.writeLong(entry.length());
                long read=0;try(var input=entry.content().open()){for(int n;(n=input.read(buffer))!=-1;){if(n==0)continue;read+=n;output.write(buffer,0,n);}}
                if(read!=entry.length())throw new IOException("Mod payload changed while reading");
            }
        }
        return HexFormat.of().formatHex(hash.digest());
    }
}
