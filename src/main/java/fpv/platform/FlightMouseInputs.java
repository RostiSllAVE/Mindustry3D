package fpv.platform;

import arc.Core;
import arc.Input;
import arc.backend.sdl.SdlApplication;
import arc.backend.sdl.jni.SDL;
import arc.util.Log;

/** SDL is already part of the running host; its focus query does not load JNA. */
public final class FlightMouseInputs {
    private FlightMouseInputs(){}
    public static FlightMouseInput create(Input input){
        boolean logging=OptionalFlightInput.loggingAvailable();
        boolean windows=System.getProperty("os.name","").startsWith("Windows");
        boolean linux=System.getProperty("os.name","").equals("Linux");
        boolean forced=Boolean.getBoolean("fpv.input.forceArc");
        String reason=forced?"host input explicitly selected":"native mouse capture is unavailable on this platform";
        return new OptionalFlightInput((windows||linux)&&!forced,reason,()->{
                if(linux){
                    if(!(Core.app instanceof SdlApplication app)||Thread.currentThread()!=app.getMainThread())
                        throw new IllegalStateException("Linux capture requires the host SDL input thread");
                    String path=LoadedSdlLibrary.hostLibrary();
                    LinuxSdlMouseCapture.Api api=logging?new JnaSdlMouseApi(path):new ForeignSdlMouseApi(path);
                    FlightMouseInput nativeInput=new LinuxSdlMouseCapture(api,app.getWindow(),FlightMouseInputs::hostFocused);
                    Log.info("[FPV] Mouse capture: host SDL relative / @ / @",api.driver(),logging?"JNA":"java.base FFM");
                    return nativeInput;
                }
                if(logging){Log.info("[FPV] Mouse capture: Win32 / JNA");return new WindowsFlightInput();}
                FlightMouseInput nativeInput=new ForeignWindowsInput();
                Log.info("[FPV] Mouse capture: Win32 / java.base FFM");return nativeInput;
            },
            ()->new ArcFlightInput(input,FlightMouseInputs::hostFocused,()->Core.graphics.getWidth(),()->Core.graphics.getHeight()),
            message->Log.warn("[FPV] Optional native mouse input unavailable (@). Host input remains active: visible cursor, bounded mouse movement; arrow keys continue aiming.",message));
    }
    private static boolean hostFocused(){
        if(!(Core.app instanceof SdlApplication app))return true; // lifecycle pause/resume still fences other backends
        int flags=SDL.SDL_GetWindowFlags(app.getWindow());
        return (flags&SDL.SDL_WINDOW_INPUT_FOCUS)!=0&&(flags&SDL.SDL_WINDOW_MINIMIZED)==0;
    }
}
