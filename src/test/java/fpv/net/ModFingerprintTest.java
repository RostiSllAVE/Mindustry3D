package fpv.net;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class ModFingerprintTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void zip(Path path,Map<String,byte[]> files,boolean reverse)throws Exception{
        var names=new ArrayList<>(files.keySet());if(reverse)Collections.reverse(names);
        try(var zip=new ZipOutputStream(Files.newOutputStream(path))){
            zip.setLevel(reverse?0:9);
            for(String name:names){var entry=new ZipEntry(name);entry.setTime(reverse?1700000000000L:1800000000000L);zip.putNextEntry(entry);zip.write(files.get(name));zip.closeEntry();}
        }
    }
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("fpv-payload-test-");
        var files=new LinkedHashMap<String,byte[]>();
        files.put("mod.hjson","version: 0.18.30\n".getBytes());files.put("fpv/FpvMod.class",new byte[]{1,2,3});
        files.put("fpv/net/Example.class",new byte[]{4,5,6});files.put("design/flight.sheet.json","{rate:1}".getBytes());
        files.put("com/sun/jna/win32-x86-64/jnidispatch.dll",new byte[]{7,8});files.put("LICENSE","GPL".getBytes());
        Path jar=temp.resolve("mod.jar"),repacked=temp.resolve("renamed.zip"),folder=temp.resolve("workshop");
        zip(jar,files,false);zip(repacked,files,true);Files.createDirectories(folder);
        for(var e:files.entrySet()){Path p=folder.resolve(e.getKey());Files.createDirectories(p.getParent());Files.write(p,e.getValue());}
        var original=ModFingerprint.inspect(jar);check(original.verified(),"complete archive verified");
        check(original.matches(ModFingerprint.inspect(folder).token()),"real unpacked directory matches archive");
        check(original.matches(ModFingerprint.inspect(repacked).token()),"ZIP order/compression/date/filename irrelevant");
        Files.writeString(folder.resolve("steamid.json"),"{id:123}");Files.write(folder.resolve("preview.png"),new byte[]{1});
        check(original.matches(ModFingerprint.inspect(folder).token()),"Steam publication-only extras do not affect identity");
        for(String path:List.of("fpv/FpvMod.class","fpv/net/Example.class","mod.hjson","design/flight.sheet.json","com/sun/jna/win32-x86-64/jnidispatch.dll","LICENSE")){
            Files.write(folder.resolve(path),new byte[]{99});check(!original.matches(ModFingerprint.inspect(folder).token()),"actual payload change rejected: "+path);Files.write(folder.resolve(path),files.get(path));
        }
        Files.writeString(folder.resolve("fpv/Injected.class"),"extra code");check(!original.matches(ModFingerprint.inspect(folder).token()),"additional class rejected");Files.delete(folder.resolve("fpv/Injected.class"));
        Files.delete(folder.resolve("fpv/net/Example.class"));check(!original.matches(ModFingerprint.inspect(folder).token()),"missing class rejected");Files.write(folder.resolve("fpv/net/Example.class"),files.get("fpv/net/Example.class"));
        check(!original.matches("unverified")&&!original.matches(null),"unverified peers never match");
        var absent=ModFingerprint.inspect(temp.resolve("absent"));check(!absent.verified()&&absent.problem()!=null,"verification failure is a result, not an initialization exception");
        check(!absent.matches(absent.token()),"two unavailable identities cannot authenticate each other");
        for(String bad:List.of("../escape.class","/absolute.class","a\\b.class","a/./b.class","a//b.class","C:/b.class")){
            var invalid=new LinkedHashMap<>(files);invalid.put(bad,new byte[]{0});Path p=temp.resolve("invalid-"+checks+".jar");zip(p,invalid,false);
            check(!ModFingerprint.inspect(p).verified(),"unsafe archive entry fails closed: "+bad);
        }
        var missing=new LinkedHashMap<>(files);missing.remove("mod.hjson");Path noMeta=temp.resolve("missing-meta.jar");zip(noMeta,missing,false);
        check(!ModFingerprint.inspect(noMeta).verified(),"incomplete root never authenticated");
        Files.writeString(temp.resolve("corrupt.jar"),"not a zip");check(!ModFingerprint.inspect(temp.resolve("corrupt.jar")).verified(),"corrupt archives fail closed");
        System.out.println("ModFingerprintTest: "+checks+" cross-container/strict-mismatch/failure-isolation checks PASS");
    }
}
