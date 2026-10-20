package fpv.render;

import java.util.*;

/** Sparse cached visual landmarks. No entities, native blocks, resources, materials or extra passes. */
public final class HorizonDistantFeatures {
    public static final int MAX_ISLANDS=12,MAX_OUTCROPS=12,MAX_STRUCTURES=6,MAX_VERTICES=2500;
    public record Result(float[] vertices,int islands,int outcrops,int structures) {}
    private record Site(float x,float z,float radius) {}
    record Position(double along,double distance) {}
    private HorizonDistantFeatures(){}

    public static Result build(HorizonGeometry geometry){
        var landscape=geometry.landscape;var composition=landscape.composition;var mesh=new Builder();
        var occupied=new ArrayList<Site>();var surface=new HorizonLandscape.Surface();
        int islands=0,outcrops=0,structures=0;long seed=composition.seed();
        if(composition.islands()&&!landscape.custom()){
            int target=Math.min(MAX_ISLANDS,Math.max(1,Math.round(composition.waterFraction()*10)));
            for(int i=0;i<96&&islands<target;i++){
                double along=random(seed,i,101)*geometry.profile.perimeter,distance=100+350*random(seed,i,103);
                var point=HorizonGeometry.point(geometry.profile,along,distance);
                float radius=(float)(12+15*random(seed,i,107));
                landscape.sample(along,distance,point.x(),point.z(),surface);
                if(surface.space>.05f||surface.coast<=.7f||!surface.ocean||
                    !available(geometry,occupied,point.x(),point.z(),radius*1.15f))continue;
                if(!island(mesh,geometry,point.x(),point.z(),radius,(float)(2.5+6*random(seed,i,109)),
                    (float)(random(seed,i,113)*Math.PI*2),surface,seed,i))continue;
                occupied.add(new Site(point.x(),point.z(),radius*1.15f));islands++;
            }
        }
        if(composition.rockyOutcrops()){
            int target=Math.min(MAX_OUTCROPS,3+Math.round(composition.rockFraction()*8));
            for(int i=0;i<96&&outcrops<target;i++){
                double along=random(seed,i,211)*geometry.profile.perimeter,distance=65+290*random(seed,i,223);
                var point=HorizonGeometry.point(geometry.profile,along,distance);
                float radius=(float)(3+6*random(seed,i,227));
                landscape.sample(along,distance,point.x(),point.z(),surface);
                if(surface.space>.05f||surface.coast>=-.7f||
                    !available(geometry,occupied,point.x(),point.z(),radius*1.15f))continue;
                if(!outcrop(mesh,geometry,point.x(),point.z(),radius,(float)(4+10*random(seed,i,229)),
                    (float)(random(seed,i,233)*Math.PI*2),surface,seed,i))continue;
                occupied.add(new Site(point.x(),point.z(),radius*1.15f));outcrops++;
            }
        }
        if(composition.structuralMotifs()){
            int target=Math.min(MAX_STRUCTURES,Math.max(1,(int)Math.ceil(composition.structuralFraction()*10)));
            for(int i=0;i<64&&structures<target;i++){
                double along=random(seed,i,307)*geometry.profile.perimeter,distance=95+285*random(seed,i,311);
                var point=HorizonGeometry.point(geometry.profile,along,distance);
                float length=(float)(18+20*random(seed,i,313)),width=(float)(3+4*random(seed,i,317));
                float radius=(float)Math.hypot(length,width);
                landscape.sample(along,distance,point.x(),point.z(),surface);
                if(surface.space>.05f||surface.coast>=-.7f||
                    !available(geometry,occupied,point.x(),point.z(),radius))continue;
                if(!structure(mesh,geometry,point.x(),point.z(),length,width,
                    (float)(1.5+3*random(seed,i,331)),random(seed,i,337)>.5))continue;
                occupied.add(new Site(point.x(),point.z(),radius));structures++;
            }
        }
        if(landscape.custom()){
            for(var material:new fpv.background.BackgroundPaint.Material[]{fpv.background.BackgroundPaint.Material.ROCK,fpv.background.BackgroundPaint.Material.SAND,fpv.background.BackgroundPaint.Material.WASTELAND,fpv.background.BackgroundPaint.Material.VOLCANIC})for(var site:landscape.paint().detailSites(material,12)){
                if(outcrops>=MAX_OUTCROPS)break;float x=site.x(),z=-site.y(),radius=(float)(3+3*site.rank());var pos=position(geometry,x,z);landscape.sample(pos.along,pos.distance,x,z,surface);
                if(surface.space>.05f||surface.coast>=-.7f||!available(geometry,occupied,x,z,radius*1.15f))continue;
                if(outcrop(mesh,geometry,x,z,radius,(float)(4+6*site.rank()),(float)(site.rank()*Math.PI*2),surface,seed,outcrops)){occupied.add(new Site(x,z,radius*1.15f));outcrops++;}
            }
            for(var site:landscape.paint().detailSites(fpv.background.BackgroundPaint.Material.INDUSTRIAL,24)){
                if(structures>=MAX_STRUCTURES)break;float x=site.x(),z=-site.y(),length=(float)(12+12*site.rank()),width=4,radius=(float)Math.hypot(length,width);var pos=position(geometry,x,z);landscape.sample(pos.along,pos.distance,x,z,surface);
                if(surface.space>.05f||surface.coast>=-.7f||!available(geometry,occupied,x,z,radius*1.15f))continue;
                if(structure(mesh,geometry,x,z,length,width,2.5f,site.rank()>.5)){occupied.add(new Site(x,z,radius*1.15f));structures++;}
            }
        }
        return new Result(Arrays.copyOf(mesh.data,mesh.count),islands,outcrops,structures);
    }

    private static boolean available(HorizonGeometry geometry,List<Site> occupied,float x,float z,float radius){
        var profile=geometry.profile;
        // Test the whole footprint, including decorative side faces, not only its center.
        if(!(x+radius<-.5f||x-radius>profile.width-.5f||z-radius>.5f||z+radius<-profile.height+.5f))return false;
        for(var site:occupied)if(Math.hypot(x-site.x,z-site.z)<(radius+site.radius)*1.12)return false;
        return true;
    }
    private static boolean island(Builder mesh,HorizonGeometry geometry,float x,float z,float radius,float height,
        float yaw,HorizonLandscape.Surface material,long seed,int id){
        int sides=8;float[][][] rings=new float[3][sides][];var probe=new HorizonLandscape.Surface();
        for(int i=0;i<sides;i++){
            double angle=yaw+i*Math.PI*2/sides;float radial=(float)(.85+.30*random(seed,id*8+i,401));
            float dx=(float)StrictMath.cos(angle)*radius*radial,dz=(float)StrictMath.sin(angle)*radius*radial;
            Position p=position(geometry,x+dx,z+dz);geometry.landscape.sample(p.along,p.distance,x+dx,z+dz,probe);
            if(probe.space>.05f||probe.coast<.65f||!probe.ocean)return false;
            rings[0][i]=mesh.v(x+dx,HorizonLandscape.SEA-.9f,z+dz,material.r,material.g,material.b,.57f,material.sand,material.ice);
            rings[1][i]=mesh.v(x+dx*.76f,HorizonLandscape.SEA+.35f,z+dz*.76f,material.r,material.g,material.b,.94f,material.sand,material.ice);
            rings[2][i]=mesh.v(x+dx*.36f,HorizonLandscape.SEA+height*(float)(.45+.15*random(seed,id*8+i,409)),z+dz*.36f,
                material.r,material.g,material.b,.91f,material.sand,material.ice);
        }
        float[] top=mesh.v(x+radius*.06f,HorizonLandscape.SEA+height,z-radius*.04f,material.r,material.g,material.b,1,material.sand,material.ice);
        for(int i=0;i<sides;i++){
            int next=(i+1)%sides;
            mesh.quad(rings[0][i],rings[1][i],rings[1][next],rings[0][next]);
            mesh.quad(rings[1][i],rings[2][i],rings[2][next],rings[1][next]);
            mesh.triangle(rings[2][i],top,rings[2][next]);
        }
        return true;
    }
    private static boolean outcrop(Builder mesh,HorizonGeometry geometry,float x,float z,float radius,float height,
        float yaw,HorizonLandscape.Surface material,long seed,int id){
        int sides=5;float[][] base=new float[sides][],shoulder=new float[sides][];var probe=new HorizonLandscape.Surface();
        float floor=groundHeight(geometry,x,z),highest=floor;
        for(int i=0;i<sides;i++){
            double angle=yaw+i*Math.PI*2/sides;float radial=(float)(.86+.28*random(seed,id*5+i,503));
            float dx=(float)StrictMath.cos(angle)*radius*radial,dz=(float)StrictMath.sin(angle)*radius*radial;
            Position p=position(geometry,x+dx,z+dz);geometry.landscape.sample(p.along,p.distance,x+dx,z+dz,probe);
            if(probe.space>.05f||probe.coast>-.55f)return false;
            float y=groundHeight(geometry,x+dx,z+dz)-.10f,upper=Math.max(floor+height*.6f,y+height*.30f);
            highest=Math.max(highest,upper);
            base[i]=mesh.v(x+dx,y,z+dz,material.r,material.g,material.b,.63f,0,material.ice);
            shoulder[i]=mesh.v(x+dx*.55f,upper,z+dz*.55f,material.r,material.g,material.b,.90f,0,material.ice);
        }
        float[] top=mesh.v(x+radius*.12f,highest+height*.4f,z-radius*.09f,material.r,material.g,material.b,1,0,material.ice);
        for(int i=0;i<sides;i++){int next=(i+1)%sides;mesh.quad(base[i],shoulder[i],shoulder[next],base[next]);mesh.triangle(shoulder[i],top,shoulder[next]);}
        return true;
    }
    private static boolean structure(Builder mesh,HorizonGeometry geometry,float x,float z,float length,float width,float height,boolean turn){
        float[][] base=new float[4][],top=new float[4][];var probe=new HorizonLandscape.Surface();float highest=-Float.MAX_VALUE;
        float[] dx={-length,length,length,-length},dz={-width,-width,width,width};
        for(int i=0;i<4;i++){
            float xx=x+(turn?dz[i]:dx[i]),zz=z+(turn?dx[i]:dz[i]);
            Position p=position(geometry,xx,zz);geometry.landscape.sample(p.along,p.distance,xx,zz,probe);
            if(probe.space>.05f||probe.coast>-.55f)return false;
            float y=groundHeight(geometry,xx,zz)-.18f;highest=Math.max(highest,y);
            base[i]=mesh.v(xx,y,zz,.43f,.46f,.47f,.70f,0,0);
        }
        for(int i=0;i<4;i++)top[i]=mesh.v(base[i][0],highest+height,base[i][2],.43f,.46f,.47f,1,0,0);
        for(int i=0;i<4;i++){int next=(i+1)%4;
            if(turn)mesh.quad(base[next],top[next],top[i],base[i]);else mesh.quad(base[i],top[i],top[next],base[next]);
        }
        // Open bottom, no hidden internal faces. Axis-aligned traces intentionally read as artificial.
        if(turn)mesh.quad(top[0],top[1],top[2],top[3]);else mesh.quad(top[0],top[3],top[2],top[1]);
        return true;
    }
    /** Inverse of the shared rectangular annulus, so bases use the actual submitted ground triangles. */
    static float groundHeight(HorizonGeometry geometry,float x,float z){
        Position p=position(geometry,x,z);return geometry.surfaceHeight(p.along,p.distance,x,z);
    }
    static Position position(HorizonGeometry geometry,float x,float z){
        var p=HorizonGeometry.coordinates(geometry.profile,x,z);return new Position(p.along(),p.distance());
    }
    private static double random(long seed,int index,int salt){
        long h=seed^((long)index*0x9e3779b97f4a7c15L)^((long)salt*0xc2b2ae3d27d4eb4fL);
        h=(h^(h>>>30))*0xbf58476d1ce4e5b9L;h=(h^(h>>>27))*0x94d049bb133111ebL;h^=h>>>31;
        return(h>>>40)/16777215d;
    }
    private static final class Builder {
        float[] data=new float[4096];int count;
        float[] v(float x,float y,float z,float r,float g,float b,float shade,float sand,float ice){
            return new float[]{x,y,z,r*shade,g*shade,b*shade,0,0,0,sand,ice,0,1,0,-1};
        }
        void vertex(float[] v,float nx,float ny,float nz){
            if(count+HorizonGeometry.STRIDE>MAX_VERTICES*HorizonGeometry.STRIDE)throw new IllegalStateException("Distant feature vertex cap exceeded");
            if(count+HorizonGeometry.STRIDE>data.length)data=Arrays.copyOf(data,Math.max(count+HorizonGeometry.STRIDE,data.length*2));
            System.arraycopy(v,0,data,count,HorizonGeometry.STRIDE);data[count+11]=nx;data[count+12]=ny;data[count+13]=nz;count+=HorizonGeometry.STRIDE;
        }
        void triangle(float[] a,float[] b,float[] c){
            float ax=b[0]-a[0],ay=b[1]-a[1],az=b[2]-a[2],bx=c[0]-a[0],by=c[1]-a[1],bz=c[2]-a[2];
            float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            if(len>1e-8f){nx/=len;ny/=len;nz/=len;}else{nx=0;ny=1;nz=0;}
            vertex(a,nx,ny,nz);vertex(b,nx,ny,nz);vertex(c,nx,ny,nz);
        }
        void quad(float[] a,float[] b,float[] c,float[] d){triangle(a,b,c);triangle(a,c,d);}
    }
}
