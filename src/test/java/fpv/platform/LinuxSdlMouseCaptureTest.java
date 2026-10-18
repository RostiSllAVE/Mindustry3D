package fpv.platform;

import java.util.ArrayList;

/** Deterministic SDL contract, not a physical X11/Wayland test. Never captures the desktop. */
public final class LinuxSdlMouseCaptureTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static final class Sdl implements LinuxSdlMouseCapture.Api {
        String driver="x11";
        boolean owns=true,focus=true,relative,failAcquire,failRelease,throwRead,closed;
        int x,y,changes,reads,closes;
        public boolean ownsWindow(long w){return owns&&w==42;}
        public String driver(){return driver;}
        public boolean focused(long w){return focus&&w==42;}
        public int setRelative(boolean value){changes++;if(value?failAcquire:failRelease)return -1;relative=value;return 0;}
        public boolean relative(){return relative;}
        public void read(LinuxSdlMouseCapture.Motion motion){reads++;if(throwRead)throw new UnsatisfiedLinkError("sample unavailable");motion.x=x;motion.y=y;x=y=0;}
        public String error(){return "compositor denied relative pointer";}
        public void close(){closed=true;closes++;}
    }
    public static void main(String[] args){
        for(String driver:new String[]{"x11","wayland"}){
            Sdl s=new Sdl();s.driver=driver;boolean[] focus={true};
            var m=new LinuxSdlMouseCapture(s,42,()->focus[0]);
            check(!m.captured()&&m.focused()&&s.changes==0,"construction/query does not capture");
            s.x=900;s.y=-800;m.setCaptured(true);
            check(m.captured()&&s.relative&&m.deltaX()==0&&s.x==0,"capture discards entry backlog");
            int changes=s.changes;
            for(int i=0;i<300;i++){
                s.x=80;s.y=-25;m.setCaptured(true);m.poll();
                check(m.deltaX()==80&&m.deltaY()==-25&&m.captured(),"continuous right/up relative motion beyond any screen edge");
            }
            check(s.changes==changes,"steady flight does not toggle or flush relative mode each frame");
            check(m.deltaX()==80&&m.deltaY()==-25,"accessors do not resample");
            m.poll();check(m.deltaX()==0&&m.deltaY()==0,"idle does not repeat motion");
            for(String mode:new String[]{"F5 2D","Tab Build","command selection","pause","menu"}){
                s.x=19;m.setCaptured(false);check(!m.captured()&&!s.relative&&m.deltaX()==0,mode+" releases synchronously");
                int reads=s.reads;m.poll();check(s.reads==reads,"released UI does not drain its pointer motion");
                m.setCaptured(true);check(m.captured()&&m.deltaX()==0,mode+" returns with a clean flight sample");
            }
            s.focus=false;s.x=100;m.poll();check(!m.captured()&&!s.relative&&m.deltaX()==0,"Alt+Tab/focus loss releases");
            m.setCaptured(true);check(!m.captured(),"unfocused window cannot acquire");
            s.focus=true;m.poll();check(!m.captured(),"focus regain needs flight mode request");
            m.setCaptured(true);check(m.captured()&&m.deltaX()==0,"explicit return reacquires without jump");
            focus[0]=false;m.poll();check(!m.captured()&&!s.relative,"host minimized/focus fence also releases");
            focus[0]=true;m.setCaptured(true);m.pause();check(!m.captured()&&!m.focused()&&!s.relative,"lifecycle pause releases");
            m.setCaptured(true);check(!m.captured(),"paused lifecycle cannot arm");
            m.resume();check(m.focused()&&!m.captured(),"resume does not capture UI");
            m.setCaptured(true);m.close();m.close();m.poll();m.setCaptured(true);
            check(s.closed&&s.closes==1&&!s.relative&&!m.captured()&&m.deltaX()==0,"shutdown idempotently releases and closes");
        }
        for(String driver:new String[]{"dummy","offscreen","","X11"}){
            Sdl s=new Sdl();s.driver=driver;
            try{new LinuxSdlMouseCapture(s,42,()->true);throw new AssertionError("unsupported backend accepted");}
            catch(IllegalStateException expected){check(s.closed&&s.changes==0,"unsupported driver untouched and closed");}
        }
        Sdl foreign=new Sdl();foreign.owns=false;
        try{new LinuxSdlMouseCapture(foreign,42,()->true);throw new AssertionError("foreign SDL accepted");}
        catch(IllegalStateException expected){check(foreign.closed&&foreign.changes==0,"different SDL instance rejected without capture");}
        Sdl busy=new Sdl();busy.relative=true;
        try{new LinuxSdlMouseCapture(busy,42,()->true);throw new AssertionError("capture owner replaced");}
        catch(IllegalStateException expected){check(busy.closed&&busy.relative&&busy.changes==0,"another capture owner left untouched");}
        Sdl denied=new Sdl();denied.failAcquire=true;
        var warnings=new ArrayList<String>();var host=new ArcFlightInput(null,()->true,()->800,()->600);
        var optional=new OptionalFlightInput(true,"",()->new LinuxSdlMouseCapture(denied,42,()->true),()->host,warnings::add);
        optional.setCaptured(true);
        check(!optional.nativeCapture()&&!optional.captured()&&denied.closed&&!denied.relative,"unsupported compositor falls back unarmed");
        check(warnings.size()==1&&optional.failure().contains("compositor denied"),"one diagnostic identifies backend rejection");
        optional.setCaptured(false);optional.close();check(warnings.size()==1,"no per-frame native retries");
        Sdl broken=new Sdl();var m=new LinuxSdlMouseCapture(broken,42,()->true);m.setCaptured(true);broken.throwRead=true;m.poll();
        check(m.failure()!=null&&!m.captured()&&!broken.relative&&m.deltaX()==0,"runtime linkage failure releases and clears motion");m.close();
        Sdl lost=new Sdl();m=new LinuxSdlMouseCapture(lost,42,()->true);m.setCaptured(true);lost.relative=false;m.poll();
        check(m.failure()!=null&&!m.captured(),"external capture loss cannot supply stale flight input");m.close();
        Sdl release=new Sdl();m=new LinuxSdlMouseCapture(release,42,()->true);m.setCaptured(true);release.failRelease=true;m.setCaptured(false);
        check(m.failure()!=null&&!m.captured()&&m.deltaX()==0,"release failure is reported with zero pilot input");
        release.failRelease=false;m.close();check(!release.relative,"close retries an incomplete release");
        System.out.println("LinuxSdlMouseCaptureTest: "+checks+" contract checks passed; physical Linux NOT TESTED.");
    }
}
