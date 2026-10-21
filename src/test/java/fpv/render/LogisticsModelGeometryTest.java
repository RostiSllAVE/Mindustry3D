package fpv.render;

public final class LogisticsModelGeometryTest {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        for(var p:LogisticsModelProfile.values())for(int mask=0;mask<16;mask++)for(int lod=0;lod<3;lod++){
            var m=LogisticsModelGeometry.model(p,mask,lod);check(m==LogisticsModelGeometry.model(p,mask,lod),"immutable type/mask/LOD templates reused");int vertices=0;
            for(var a:new float[][]{m.body(),m.trim(),m.lining()}){check(a.length%24==0,"whole triangles/stride");vertices+=a.length/8;
                for(int i=0;i<a.length;i+=8){for(int j=0;j<8;j++)check(Float.isFinite(a[i+j]),"finite positions/normals/UV");check(Math.abs(a[i])<=.50001&&Math.abs(a[i+2])<=.50001,"no neighboring-cell overlap");check(a[i+1]>=0&&a[i+1]<=1,"inside unchanged physical height");}
            }check(vertices<=1300,"low bounded geometry");if(lod==2)check(m.trim().length==0&&m.lining().length==0,"cheap far model");
            if(!p.belt)check(m==LogisticsModelGeometry.model(p,15,lod),"routing masks share the same immutable model");
        }
        check(LogisticsModelGeometry.model(LogisticsModelProfile.ARMORED,5,0).trim().length>0,"armored guards have actual sides");
        check(LogisticsModelGeometry.model(LogisticsModelProfile.CONVEYOR,15,0).trim().length==0,"open feeds do not intersect a guard");
        check(LogisticsModelProfile.lod(0)==0&&LogisticsModelProfile.lod(26*26)==1&&LogisticsModelProfile.lod(81*81)==2,"bounded LOD");
        System.out.println("LogisticsModelGeometryTest: "+checks+" cached profile/mask/LOD/finite/footprint/height/budget checks PASS");
    }
}
