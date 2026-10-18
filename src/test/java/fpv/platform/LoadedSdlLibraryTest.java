package fpv.platform;

import java.util.List;

public final class LoadedSdlLibraryTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static String map(String path){return "7f123000-7f456000 r-xp 00000000 08:01 123 "+path;}
    private static void rejects(List<String> lines){try{LoadedSdlLibrary.fromMaps(lines);throw new AssertionError("unsafe mapping accepted");}catch(IllegalStateException expected){checks++;}}
    public static void main(String[] args){
        String host="/tmp/arc/abc/libsdl-arc64.so";
        check(LoadedSdlLibrary.fromMaps(List.of(map(host),map(host),map("/usr/lib/libSDL2-2.0.so.0"))).equals(host),"select exact host bridge, not another SDL");
        check(LoadedSdlLibrary.fromMaps(List.of(map("/tmp/path\\040with\\040spaces/libsdl-arc64.so"))).equals("/tmp/path with spaces/libsdl-arc64.so"),"kernel escaped spaces decoded");
        check(LoadedSdlLibrary.fromMaps(List.of(map("/tmp/arc/libsdl-arc.so"))).endsWith("libsdl-arc.so"),"alternate native bridge name supported");
        rejects(List.of(map("/usr/lib/libSDL2-2.0.so.0")));
        rejects(List.of(map(host+" (deleted)")));
        rejects(List.of(map("relative/libsdl-arc64.so")));
        rejects(List.of(map(host),map("/other/libsdl-arc64.so")));
        rejects(List.of("malformed",map("/tmp/not-libsdl-arc64.so"),map("/tmp/libsdl-arc64.so.bak")));
        if(!System.getProperty("os.name","").equals("Linux")){
            try{LoadedSdlLibrary.hostLibrary();throw new AssertionError("non-Linux path scan");}catch(IllegalStateException expected){checks++;}
            try{new ForeignSdlMouseApi(host);throw new AssertionError("non-Linux native load");}catch(IllegalStateException expected){checks++;}
        }
        // Check the actual supported host API, rather than assuming libGDX cursor methods exist.
        try{arc.backend.sdl.jni.SDL.class.getMethod("SDL_GetWindowFlags",long.class);checks++;}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
        try{arc.Input.class.getMethod("setCursorCatched",boolean.class);throw new AssertionError("host API changed; reconsider direct API integration");}
        catch(NoSuchMethodException expected){checks++;}
        try{arc.backend.sdl.jni.SDL.class.getMethod("SDL_SetRelativeMouseMode",boolean.class);throw new AssertionError("host API changed; reconsider direct API integration");}
        catch(NoSuchMethodException expected){checks++;}
        System.out.println("LoadedSdlLibraryTest: "+checks+" checks passed.");
    }
}
