package fpv.platform;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.util.Map;

/** Existing JNA dependency; RTLD_NOLOAD forbids loading a second SDL/bridge. */
public final class JnaSdlMouseApi implements LinuxSdlMouseCapture.Api {
    private final NativeLibrary library;
    private final Function wasInit,windowId,fromId,driver,focus,set,get,read,error;
    private final IntByReference x=new IntByReference(),y=new IntByReference();
    private final Object[] noArgs={},enable={1},disable={0},sampleArgs={x,y};
    public JnaSdlMouseApi(String mappedPath){
        library=NativeLibrary.getInstance(mappedPath,Map.of(Library.OPTION_OPEN_FLAGS,2|4)); // Linux RTLD_NOW|RTLD_NOLOAD
        try{
            wasInit=fn("SDL_WasInit");windowId=fn("SDL_GetWindowID");fromId=fn("SDL_GetWindowFromID");
            driver=fn("SDL_GetCurrentVideoDriver");focus=fn("SDL_GetKeyboardFocus");
            set=fn("SDL_SetRelativeMouseMode");get=fn("SDL_GetRelativeMouseMode");read=fn("SDL_GetRelativeMouseState");error=fn("SDL_GetError");
        }catch(RuntimeException|LinkageError failure){library.close();throw failure;}
    }
    private Function fn(String name){return library.getFunction(name);}
    public boolean ownsWindow(long window){
        if(wasInit.invokeInt(new Object[]{0x20})==0)return false;
        int id=windowId.invokeInt(new Object[]{new Pointer(window)});
        return id!=0&&Pointer.nativeValue(fromId.invokePointer(new Object[]{id}))==window;
    }
    public String driver(){return driver.invokeString(noArgs,false);}
    public boolean focused(long window){return Pointer.nativeValue(focus.invokePointer(noArgs))==window;}
    public int setRelative(boolean value){return set.invokeInt(value?enable:disable);}
    public boolean relative(){return get.invokeInt(noArgs)!=0;}
    public void read(LinuxSdlMouseCapture.Motion motion){read.invokeInt(sampleArgs);motion.x=x.getValue();motion.y=y.getValue();}
    public String error(){return error.invokeString(noArgs,false);}
    public void close(){library.close();}
}
