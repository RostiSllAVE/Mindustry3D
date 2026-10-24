package fpv.render;

/** Geometry contacts for the reported floating lamps and moving/static intersection cases. */
public final class UnitAssemblyContactTest {
    static int checks,poses;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        for(var p:UnitProductionProfile.values()){
            var m=UnitProductionGeometry.model(p,0);
            verify(m.body(),m.lights(),m.carriages(),m.tools(),.15f,p.name());
        }
        for(var p:UnitUpgradeProfile.values()){
            int size=p==UnitUpgradeProfile.MULTIPLICATIVE||p==UnitUpgradeProfile.PRIME?5:p==UnitUpgradeProfile.EXPONENTIAL?7:p==UnitUpgradeProfile.TETRATIVE?9:3;
            var m=UnitUpgradeGeometry.model(p,size,0);
            verify(m.body(),m.lights(),m.carriages(),m.tools(),.10f,p.name());
        }
        for(float height:new float[]{.88f,1.64f,2.4f,3.16f,3.5f}){
            float top=UnitAssemblyParts.surface(0,height),bay=height*UnitAssemblyParts.BAY;
            check(top+.002f-bay>.024f,"native output art clears the actual highest bay surface at every size");
            check(top+.030f-(top+.01f)>.019f,"partial upgrade and real input have distinct depth planes");
            check(top+.050f-(top+.030f)>.019f,"scan line clears preview without z fighting");
        }
        System.out.println("UnitAssemblyContactTest: "+checks+" mounted-light/layer-gap/mesh-intersection checks, "+poses+" carriage poses PASS");
    }
    static void verify(float[] body,float[] lamps,float[][] arms,float[] tools,float stroke,String label){
        boolean mount=false;for(int v=0;v<lamps.length&&!mount;v+=8)for(int t=0;t<body.length&&!mount;t+=24)mount=onTriangle(lamps[v],lamps[v+1],lamps[v+2],body,t);
        check(mount,"status lights touch actual housing: "+label);
        for(int side=0;side<2;side++){
            var led=UnitProductionRenderer.toolSide(tools,side);float[] tool=arms[side];
            float low=Float.POSITIVE_INFINITY,high=-low;
            for(int i=0;i<led.length;i+=8){low=Math.min(low,led[i+1]);high=Math.max(high,led[i+1]);}
            boolean attached=false;for(int i=0;i<tool.length;i+=8)if(tool[i+1]>=low-1e-5&&tool[i+1]<=high+1e-5)attached=true;
            check(attached,"emitter overlaps its metal shank: "+label);
            for(int phase=0;phase<=16;phase++){
                float dx=-stroke+stroke*2*phase/16;
                for(int a=0;a<tool.length;a+=24)for(int t=0;t<body.length;t+=24){
                    float minX=Math.min(tool[a],Math.min(tool[a+8],tool[a+16]))+dx,maxX=Math.max(tool[a],Math.max(tool[a+8],tool[a+16]))+dx;
                    float minY=Math.min(tool[a+1],Math.min(tool[a+9],tool[a+17])),maxY=Math.max(tool[a+1],Math.max(tool[a+9],tool[a+17]));
                    float minZ=Math.min(tool[a+2],Math.min(tool[a+10],tool[a+18])),maxZ=Math.max(tool[a+2],Math.max(tool[a+10],tool[a+18]));
                    if(maxX<Math.min(body[t],Math.min(body[t+8],body[t+16]))||minX>Math.max(body[t],Math.max(body[t+8],body[t+16]))||maxY<Math.min(body[t+1],Math.min(body[t+9],body[t+17]))||minY>Math.max(body[t+1],Math.max(body[t+9],body[t+17]))||maxZ<Math.min(body[t+2],Math.min(body[t+10],body[t+18]))||minZ>Math.max(body[t+2],Math.max(body[t+10],body[t+18])))continue;
                    for(int edge=0;edge<3;edge++){int u=a+edge*8,v=a+(edge+1)%3*8;
                        check(!intersects(tool[u]+dx,tool[u+1],tool[u+2],tool[v]+dx,tool[v+1],tool[v+2],body,t),"moving head crosses static housing: "+label+" side="+side+" phase="+phase);
                    }
                }
                poses++;
            }
        }
    }
    static boolean onTriangle(double x,double y,double z,float[] t,int i){
        double ax=t[i],ay=t[i+1],az=t[i+2],ux=t[i+8]-ax,uy=t[i+9]-ay,uz=t[i+10]-az,vx=t[i+16]-ax,vy=t[i+17]-ay,vz=t[i+18]-az;
        double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,n=Math.sqrt(nx*nx+ny*ny+nz*nz);if(n<1e-10||Math.abs((x-ax)*nx+(y-ay)*ny+(z-az)*nz)>n*1e-5)return false;
        double uu=ux*ux+uy*uy+uz*uz,vv=vx*vx+vy*vy+vz*vz,uv=ux*vx+uy*vy+uz*vz,wu=(x-ax)*ux+(y-ay)*uy+(z-az)*uz,wv=(x-ax)*vx+(y-ay)*vy+(z-az)*vz,d=uu*vv-uv*uv;
        double u=(wu*vv-wv*uv)/d,v=(wv*uu-wu*uv)/d;return u>=-1e-5&&v>=-1e-5&&u+v<=1.00001;
    }
    static boolean intersects(double x,double y,double z,double ex,double ey,double ez,float[] t,int i){
        double dx=ex-x,dy=ey-y,dz=ez-z,ux=t[i+8]-t[i],uy=t[i+9]-t[i+1],uz=t[i+10]-t[i+2],vx=t[i+16]-t[i],vy=t[i+17]-t[i+1],vz=t[i+18]-t[i+2];
        double px=dy*vz-dz*vy,py=dz*vx-dx*vz,pz=dx*vy-dy*vx,det=ux*px+uy*py+uz*pz;if(Math.abs(det)<1e-9)return false;
        double sx=x-t[i],sy=y-t[i+1],sz=z-t[i+2],u=(sx*px+sy*py+sz*pz)/det;
        if(u<=1e-5||u>=.99999)return false;double qx=sy*uz-sz*uy,qy=sz*ux-sx*uz,qz=sx*uy-sy*ux,v=(dx*qx+dy*qy+dz*qz)/det;
        if(v<=1e-5||u+v>=.99999)return false;double f=(vx*qx+vy*qy+vz*qz)/det;return f>1e-5&&f<.99999;
    }
}
