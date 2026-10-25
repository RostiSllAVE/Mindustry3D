package fpv.render;

/** Animated local preview marker. Native rotations: east, north, west, south. No input/world writes. */
public final class BuildDirectionArrow {
    private BuildDirectionArrow(){}
    public interface Sink {void stroke(double ax,double ay,double az,double bx,double by,double bz,double width,boolean outline);}
    public static void emit(double x,double y,double top,double size,int rotation,double seconds,Sink sink){
        if(!Double.isFinite(x+y+top+size+seconds)||size<=0)return;
        int turn=Math.floorMod(rotation,4);double dx=turn==0?1:turn==2?-1:0,dy=turn==1?1:turn==3?-1:0;
        double length=Math.max(.55,Math.min(1.8,size*.65)),pulse=Math.sin(seconds*Math.PI*2*.8);
        double cx=x+dx*length*.06*pulse,cy=y+dy*length*.06*pulse,h=top+.10+.025*pulse;
        double tipX=cx+dx*length*.5,tipY=cy+dy*length*.5,backX=tipX-dx*length*.36,backY=tipY-dy*length*.36;
        for(int pass=0;pass<2;pass++){
            boolean outline=pass==0;double width=length*(outline?.12:.065),z=h-(outline?.006:0);
            sink.stroke(cx-dx*length*.46,cy-dy*length*.46,z,tipX,tipY,z,width,outline);
            sink.stroke(backX-dy*length*.25,backY+dx*length*.25,z,tipX,tipY,z,width,outline);
            sink.stroke(backX+dy*length*.25,backY-dx*length*.25,z,tipX,tipY,z,width,outline);
        }
    }
}
