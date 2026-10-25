package fpv.render;

public final class TurretBodyPolishTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    public static void main(String[] args){
        byte[] mask=new byte[32*24];java.util.Arrays.fill(mask,(byte)255);
        var shape=SpriteExtrusionGeometry.generate(mask,32,24,24,6,32,.2,16380);var original=shape.vertices();var old=original.clone();var next=TurretBodyPolish.shell(original);
        check(java.util.Arrays.equals(old,original),"immutable source template");check(next.length>original.length,"actual additional exterior side faces");
        for(int i=0;i<original.length;i+=8){
            if(original[i+4]>.5f)for(int j=0;j<8;j++)check(next[i+j]==original[i+j],"exact roof UV/position/normal preserved");
            if(original[i+4]<-.5f){check(Math.abs(next[i+1]+1.15)<1e-6,"underside moved below old shell");check(Math.abs(next[i])<=Math.abs(original[i])+1e-6,"lower contour narrows");}
        }
        for(float f:next)check(Float.isFinite(f),"finite template");
        System.out.println("TurretBodyPolish: "+checks+" roof/pivot-preserving shell/exterior/cache-input checks PASS");
    }
}
