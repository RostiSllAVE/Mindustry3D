package fpv.input;
import fpv.background.*;import java.util.*;
/** CPU-only stroke model probe. No graphics, pointer-latency or FPS claim. */
public final class BackgroundBrushGestureProbe {
    public static void main(String[] args)throws Exception{
        var p=new BackgroundPaint(512,512);var history=new BackgroundPaintHistory(p);double[] times=new double[300];
        for(var brush:new BackgroundPaint.Brush[]{BackgroundPaint.Brush.FOREST,BackgroundPaint.Brush.RAISE,BackgroundPaint.Brush.SMOOTH}){
            for(int i=-30;i<300;i++){long start=System.nanoTime();history.begin();var stroke=p.stroke(brush,256,.65,.4,30,(x,y)->12);stroke.line(-300,20,-100,200);for(int n=0;n<8;n++)stroke.line(-300,20,-100,200);history.end();if(i>=0)times[i]=(System.nanoTime()-start)/1e6;}Arrays.sort(times);System.out.printf(Locale.ROOT,"GESTURE,%s,medianMs=%.6f,p95Ms=%.6f,maxMs=%.6f,maskBytes=%d,historyBytes=%d%n",brush,times[150],times[285],times[299],p.bytes(),history.maxBytes());
        }
        var s=new BackgroundStyle();s.mode=BackgroundStyle.Mode.CUSTOM;s.paint=p;byte[] bytes=null;long start=System.nanoTime();for(int i=0;i<100;i++){bytes=BackgroundProfileStore.encode("a".repeat(64),512,512,s);BackgroundProfileStore.decode(bytes,"a".repeat(64),512,512);}System.out.printf(Locale.ROOT,"GESTURE_CODEC,meanMs=%.6f,fileBytes=%d%n",(System.nanoTime()-start)/1e6/100,bytes.length);
    }
}
