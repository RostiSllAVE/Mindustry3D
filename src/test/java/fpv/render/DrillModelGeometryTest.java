package fpv.render;

import java.util.*;

public final class DrillModelGeometryTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        var signatures=new HashSet<Integer>();
        for(var profile:DrillModelProfile.values()){
            int previous=Integer.MAX_VALUE;
            for(int lod=0;lod<3;lod++){
                var shape=MechanicalBlockGeometry.drill(profile,lod);
                check(shape==MechanicalBlockGeometry.drill(profile,lod),"immutable template shared between instances");
                check(shape.vertices()>0&&shape.vertices()<650&&shape.vertices()<=previous,"bounded decreasing LOD geometry");previous=shape.vertices();
                check(shape.bytes()==shape.vertices()*32L,"cached vertex memory measurable");
                for(var vertices:new float[][]{shape.body(),shape.pistons()})for(int i=0;i<vertices.length;i+=8){
                    for(int j=0;j<8;j++)check(Float.isFinite(vertices[i+j]),"finite geometry/material");
                    check(Math.abs(vertices[i])<=.50001&&Math.abs(vertices[i+2])<=.50001,"native footprint preserved");
                    check(vertices[i+1]>=0&&vertices[i+1]+profile.stroke<1,"fits existing physical building height including stroke");
                    check(vertices[i+4]>=0,"no hidden foundation bottoms");
                    check(vertices[i+6]==-1||vertices[i+6]==-2||vertices[i+6]>=0&&vertices[i+6]<=1,"UV in native region or cached edge/accent sample");
                }
                if(lod==0)signatures.add(Arrays.hashCode(shape.body()));
                if(lod>0)check(shape.pistons().length==0,"distant piston detail omitted");
            }
            check(profile.cap+profile.capDepth*.5f<1&&profile.rotor+.075f*.5f<profile.cap-profile.capDepth*.5f,"cap and rotor separated inside height");
            System.out.println("A216a_TEMPLATE "+profile+" near="+MechanicalBlockGeometry.drill(profile,0).vertices()+" mid="+MechanicalBlockGeometry.drill(profile,1).vertices()+" far="+MechanicalBlockGeometry.drill(profile,2).vertices());
        }
        check(signatures.size()==4,"four genuinely different mechanical bodies");
        check(DrillModelProfile.lod(0)==0&&DrillModelProfile.lod(28*28)==1&&DrillModelProfile.lod(80*80)==2,"distance LOD boundaries");
        System.out.println("DrillModelGeometryTest: "+checks+" cached templates/finite geometry/footprints/heights/UV/LOD checks PASS");
    }
}
