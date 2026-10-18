package fpv.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/** Locate only an already mapped host bridge, never search for or load a new SDL installation. */
public final class LoadedSdlLibrary {
    private LoadedSdlLibrary(){}
    public static String hostLibrary(){
        if(!System.getProperty("os.name","").equals("Linux"))throw new IllegalStateException("Linux host required");
        try{
            String path=fromMaps(Files.readAllLines(Path.of("/proc/self/maps")));
            if(!Files.isRegularFile(Path.of(path)))throw new IllegalStateException("Mapped SDL bridge is no longer accessible");
            return path;
        }catch(IOException error){throw new IllegalStateException("Cannot inspect the host SDL mapping",error);}
    }
    static String fromMaps(List<String> lines){
        var paths=new LinkedHashSet<String>();
        for(String line:lines){
            String[] fields=line.trim().split("\\s+",6);
            if(fields.length!=6||!fields[5].startsWith("/")||fields[5].endsWith(" (deleted)"))continue;
            String path=fields[5].replace("\\040"," ").replace("\\011","\t").replace("\\012","\n").replace("\\134","\\");
            String name=path.substring(path.lastIndexOf('/')+1);
            if(name.equals("libsdl-arc64.so")||name.equals("libsdl-arc.so"))paths.add(path);
        }
        if(paths.size()!=1)throw new IllegalStateException("Expected one mapped host SDL bridge; found "+paths.size());
        return paths.iterator().next();
    }
}
