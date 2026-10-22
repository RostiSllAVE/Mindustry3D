package fpv.render;

public final class ShieldActivityGeometryTest {
    public static void main(String[] args){
        var m=ShieldActivityGeometry.model();check(m==ShieldActivityGeometry.model(),"shared immutable globe");check(m.hexagons()==30&&m.pentagons()==12,"closed geodesic globe with recognizable hexagons");
        for(var part:new float[][]{m.panels(),m.edges(),m.coarse()}){check(part.length%9==0,"complete triangles");for(int i=0;i<part.length;i+=3){double r=0;for(int j=0;j<3;j++){check(Float.isFinite(part[i+j]),"finite globe");r+=part[i+j]*part[i+j];}check(r>.98&&r<1.03,"bounded spherical panels and borders");}}
        check(m.panels().length+m.edges().length+m.coarse().length<10000,"bounded shared template");System.out.println("ShieldActivityGeometryTest: cached 30-hexagon/12-pentagon globe and all finite spherical vertices PASS");
    }
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
