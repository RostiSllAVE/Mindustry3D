package fpv.render;

import java.util.*;

public final class BuildDirectionArrowTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static class Capture implements BuildDirectionArrow.Sink {
        final ArrayList<double[]> strokes=new ArrayList<>();
        public void stroke(double ax,double ay,double az,double bx,double by,double bz,double width,boolean outline){strokes.add(new double[]{ax,ay,az,bx,by,bz,width,outline?1:0});}
    }
    public static void main(String[] args){
        int[][] expected={{1,0},{0,1},{-1,0},{0,-1}};
        for(int floor:new int[]{0,1,3,5})for(double size:new double[]{1,2,4,10})for(int rotation=0;rotation<4;rotation++){
            var a=new Capture();double top=floor*1.4+1.2;BuildDirectionArrow.emit(40.5,32.5,top,size,rotation,.1,a);
            check(a.strokes.size()==6,"bounded three-stroke glyph and dark outline");
            var shaft=a.strokes.get(3);double dx=shaft[3]-shaft[0],dy=shaft[4]-shaft[1];
            check(dx*expected[rotation][0]+dy*expected[rotation][1]>.5,"matches actual native rotation direction");
            check(Math.abs(dx*expected[rotation][1]-dy*expected[rotation][0])<1e-9,"no handedness/inverted north direction");
            for(var s:a.strokes){for(double v:s)check(Double.isFinite(v),"finite glyph");check(s[2]>top+.06&&s[5]>top+.06,"glyph above full preview roof on every floor");check(s[6]>0&&s[6]<.3,"visible bounded width");}
            var b=new Capture();BuildDirectionArrow.emit(40.5,32.5,top,size,rotation,.4,b);check(!Arrays.equals(shaft,b.strokes.get(3)),"animation moves marker without changing rotation");
        }
        var invalid=new Capture();BuildDirectionArrow.emit(0,0,0,0,0,0,invalid);BuildDirectionArrow.emit(Double.NaN,0,0,1,0,0,invalid);check(invalid.strokes.isEmpty(),"invalid presentation inputs suppressed");
        System.out.println("BuildDirectionArrow: "+checks+" native directions/animation/roof clearance/size/floor/bounds checks PASS");
    }
}
