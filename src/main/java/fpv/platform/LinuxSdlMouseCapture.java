package fpv.platform;

import java.util.function.BooleanSupplier;

/** Relative motion from the host SDL instance. All calls belong to the host input thread. */
public final class LinuxSdlMouseCapture implements FlightMouseInput {
    public static final class Motion { public int x,y; }
    public interface Api extends AutoCloseable {
        boolean ownsWindow(long window);
        String driver();
        boolean focused(long window);
        int setRelative(boolean value);
        boolean relative();
        void read(Motion motion);
        String error();
        @Override void close();
    }
    private final Api api;
    private final long window;
    private final BooleanSupplier hostFocus;
    private final Motion motion=new Motion();
    private boolean captured,owned,paused,closed;
    private float dx,dy;
    private String failure;

    public LinuxSdlMouseCapture(Api api,long window,BooleanSupplier hostFocus){
        this.api=api;this.window=window;this.hostFocus=hostFocus;
        try{
            if(window==0||!api.ownsWindow(window))throw new IllegalStateException("SDL does not own the host window");
            String driver=api.driver();
            if(!supportedDriver(driver))throw new IllegalStateException("Unsupported SDL video driver: "+driver);
            // Do not take over capture already owned by another component.
            if(api.relative())throw new IllegalStateException("Host SDL relative mouse mode is already in use");
        }catch(RuntimeException|LinkageError error){api.close();throw error;}
    }
    public static boolean supportedDriver(String driver){return "x11".equals(driver)||"wayland".equals(driver);}
    public boolean focused(){
        if(closed||paused||failure!=null)return false;
        try{return hostFocus.getAsBoolean()&&api.focused(window);}
        catch(RuntimeException|LinkageError error){fail(error.toString());return false;}
    }
    public void setCaptured(boolean value){
        if(closed)return;
        if(!value||!focused()){release();return;}
        if(captured)return;
        try{
            if(api.relative()){fail("SDL relative mode was acquired by another component");return;}
            owned=true; // even a partially successful call must be released on failure
            if(api.setRelative(true)!=0||!api.relative()){fail("SDL capture unavailable: "+api.error());return;}
            api.read(motion); // discard the entry/warp backlog; never feed it into flight
            captured=true;dx=dy=0;
        }catch(RuntimeException|LinkageError error){fail(error.toString());}
    }
    public void poll(){
        dx=dy=0;
        if(!captured)return;
        if(!focused()){release();return;}
        try{
            if(!api.relative()){fail("Host SDL relative capture was lost");return;}
            api.read(motion);dx=motion.x;dy=motion.y; // SDL already uses positive right/down
        }catch(RuntimeException|LinkageError error){fail(error.toString());}
    }
    private void fail(String message){if(failure==null)failure=message;release();}
    private void release(){
        captured=false;dx=dy=0;
        if(!owned)return;
        try{
            if(api.setRelative(false)!=0){if(failure==null)failure="SDL release failed: "+api.error();return;}
            owned=false;
        }catch(RuntimeException|LinkageError error){if(failure==null)failure=error.toString();}
    }
    public float deltaX(){return dx;}
    public float deltaY(){return dy;}
    public boolean captured(){return captured;}
    public String failure(){return failure;}
    public void pause(){paused=true;release();}
    public void resume(){paused=false;release();}
    public void close(){if(!closed){release();closed=true;api.close();}}
}
