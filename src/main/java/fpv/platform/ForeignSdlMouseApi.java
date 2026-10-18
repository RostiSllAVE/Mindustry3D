package fpv.platform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/** java.base-only binding for reduced Java22+ hosts. Reflection preserves Java17 loading.
 * Opens only the exact mapped host bridge, whose existing SDL dependency is reused by dlopen.
 * Never initializes SDL, creates a window, pumps events, or opens a system SDL by name. */
public final class ForeignSdlMouseApi implements LinuxSdlMouseCapture.Api {
    private Object arena,linker,library,ints,address,x,y;
    private Class<?> segment,layout,descriptor,option;
    private MethodHandle intAt,pointer,addressOf;
    private MethodHandle wasInit,windowId,fromId,driver,focus,set,get,read,error;
    public ForeignSdlMouseApi(String mappedPath){
        try{
            if(!System.getProperty("os.name","").equals("Linux"))throw new IllegalStateException("Linux host required");
            // Refuse arbitrary paths even if this constructor is called outside the factory.
            if(!mappedPath.equals(LoadedSdlLibrary.hostLibrary()))throw new IllegalStateException("SDL bridge is not the mapped host");
            Class<?> arenas=Class.forName("java.lang.foreign.Arena"),values=Class.forName("java.lang.foreign.ValueLayout");
            Class<?> intLayout=Class.forName("java.lang.foreign.ValueLayout$OfInt");
            segment=Class.forName("java.lang.foreign.MemorySegment");layout=Class.forName("java.lang.foreign.MemoryLayout");
            descriptor=Class.forName("java.lang.foreign.FunctionDescriptor");option=Class.forName("java.lang.foreign.Linker$Option");
            arena=arenas.getMethod("ofConfined").invoke(null);
            linker=Class.forName("java.lang.foreign.Linker").getMethod("nativeLinker").invoke(null);
            ints=values.getField("JAVA_INT").get(null);address=values.getField("ADDRESS").get(null);
            library=Class.forName("java.lang.foreign.SymbolLookup").getMethod("libraryLookup",String.class,arenas).invoke(null,mappedPath,arena);
            var handles=java.lang.invoke.MethodHandles.publicLookup();
            intAt=java.lang.invoke.MethodHandles.insertArguments(handles.unreflect(segment.getMethod("get",intLayout,long.class)),1,ints)
                .asType(MethodType.methodType(int.class,Object.class,long.class));
            pointer=handles.unreflect(segment.getMethod("ofAddress",long.class)).asType(MethodType.methodType(Object.class,long.class));
            addressOf=handles.unreflect(segment.getMethod("address")).asType(MethodType.methodType(long.class,Object.class));
            var allocate=Class.forName("java.lang.foreign.SegmentAllocator").getMethod("allocate",long.class,long.class);
            x=allocate.invoke(arena,4L,4L);y=allocate.invoke(arena,4L,4L);
            wasInit=bind("SDL_WasInit",ints,ints).asType(MethodType.methodType(int.class,int.class));
            windowId=bind("SDL_GetWindowID",ints,address).asType(MethodType.methodType(int.class,Object.class));
            fromId=bind("SDL_GetWindowFromID",address,ints).asType(MethodType.methodType(Object.class,int.class));
            driver=bind("SDL_GetCurrentVideoDriver",address).asType(MethodType.methodType(Object.class));
            focus=bind("SDL_GetKeyboardFocus",address).asType(MethodType.methodType(Object.class));
            set=bind("SDL_SetRelativeMouseMode",ints,ints).asType(MethodType.methodType(int.class,int.class));
            get=bind("SDL_GetRelativeMouseMode",ints).asType(MethodType.methodType(int.class));
            read=bind("SDL_GetRelativeMouseState",ints,address,address).asType(MethodType.methodType(int.class,Object.class,Object.class));
            error=bind("SDL_GetError",address).asType(MethodType.methodType(Object.class));
        }catch(Throwable failure){close();throw unavailable(failure);}
    }
    private static RuntimeException unavailable(Throwable failure){
        if(failure instanceof InvocationTargetException wrapped)failure=wrapped.getCause();
        return new IllegalStateException("Host SDL java.base access unavailable: "+failure,failure);
    }
    private MethodHandle bind(String name,Object result,Object... parameters)throws ReflectiveOperationException{
        Object symbol=((Optional<?>)Class.forName("java.lang.foreign.SymbolLookup").getMethod("find",String.class).invoke(library,name))
            .orElseThrow(()->new IllegalStateException("Host SDL symbol missing: "+name));
        Object layouts=Array.newInstance(layout,parameters.length);
        for(int i=0;i<parameters.length;i++)Array.set(layouts,i,parameters[i]);
        Object desc=descriptor.getMethod("of",layout,layouts.getClass()).invoke(null,result,layouts);
        Object options=Array.newInstance(option,0);
        return (MethodHandle)Class.forName("java.lang.foreign.Linker").getMethod("downcallHandle",segment,descriptor,options.getClass()).invoke(linker,symbol,desc,options);
    }
    private long pointerCall(MethodHandle fn){try{return (long)addressOf.invokeExact((Object)fn.invokeExact());}catch(Throwable t){throw unavailable(t);}}
    private String stringCall(MethodHandle fn){
        try{
            Object value=(Object)fn.invokeExact();
            if((long)addressOf.invokeExact(value)==0)return "unknown";
            return (String)segment.getMethod("getString",long.class).invoke(segment.getMethod("reinterpret",long.class).invoke(value,4096L),0L);
        }catch(Throwable t){throw unavailable(t);}
    }
    public boolean ownsWindow(long window){
        try{
            if((int)wasInit.invokeExact(0x20)==0)return false;
            int id=(int)windowId.invokeExact((Object)pointer.invokeExact(window));
            return id!=0&&(long)addressOf.invokeExact((Object)fromId.invokeExact(id))==window;
        }catch(Throwable t){throw unavailable(t);}
    }
    public String driver(){return stringCall(driver);}
    public boolean focused(long window){return pointerCall(focus)==window;}
    public int setRelative(boolean value){try{return (int)set.invokeExact(value?1:0);}catch(Throwable t){throw unavailable(t);}}
    public boolean relative(){try{return (int)get.invokeExact()!=0;}catch(Throwable t){throw unavailable(t);}}
    public void read(LinuxSdlMouseCapture.Motion motion){
        try{int buttons=(int)read.invokeExact(x,y);motion.x=(int)intAt.invokeExact(x,0L);motion.y=(int)intAt.invokeExact(y,0L);}
        catch(Throwable t){throw unavailable(t);}
    }
    public String error(){return stringCall(error);}
    public void close(){if(arena!=null){try{Class.forName("java.lang.foreign.Arena").getMethod("close").invoke(arena);}catch(ReflectiveOperationException ignored){}arena=null;}}
}
