package fpv.platform;

/** Exercises the actual factory without capturing/warping the physical pointer. */
public final class FlightMouseFactoryTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        String os=System.getProperty("os.name"),forced=System.getProperty("fpv.input.forceArc");
        try{
            System.setProperty("fpv.input.forceArc","true");
            try(var input=FlightMouseInputs.create(null)){
                check(!input.nativeCapture()&&!input.captured(),"explicit bounded fallback remains available");
                input.setCaptured(false);input.pause();input.resume();
            }
            System.clearProperty("fpv.input.forceArc");
            System.setProperty("os.name","Linux");
            // There is no SdlApplication in this fixture. Crucially do not initialize JNA/FFM
            // or attempt to load a Linux library in a Windows process to fake physical testing.
            try(var input=FlightMouseInputs.create(null)){
                check(!input.nativeCapture()&&!input.captured(),"non-SDL/headless Linux isolates capture failure");
                check(input.failure().contains("host SDL input thread"),"unsupported Linux host diagnosed");
            }
            System.setProperty("os.name","Unsupported test backend");
            try(var input=FlightMouseInputs.create(null)){check(!input.nativeCapture(),"unsupported platform still starts with fallback");}
            if(os!=null)System.setProperty("os.name",os);
            if(os!=null&&os.startsWith("Windows")){
                try(var input=FlightMouseInputs.create(null)){
                    check(input.nativeCapture()&&input.failure()==null,"actual Windows JNA/FFM factory still initializes");
                    input.focused();input.poll();input.setCaptured(false);
                    check(!input.captured()&&input.deltaX()==0&&input.deltaY()==0,"actual Windows factory is idle/releasable");
                }
            }
            System.out.println("FlightMouseFactoryTest: "+checks+" checks PASS; no physical pointer capture performed.");
        }finally{
            if(os==null)System.clearProperty("os.name");else System.setProperty("os.name",os);
            if(forced==null)System.clearProperty("fpv.input.forceArc");else System.setProperty("fpv.input.forceArc",forced);
        }
    }
}
