package fpv.background;

import fpv.render.HorizonGeometry;
import java.util.Arrays;

/** Bounded world-XY art mask. All nodes inside or on the playable rectangle are immutable. */
public final class BackgroundPaint {
    public static final int EDGE_CELLS=32,MAP_CELLS=24,NODES=EDGE_CELLS*2+MAP_CELLS+1,COUNT=NODES*NODES;
    public static final float MAX_HEIGHT=240;
    public enum Material {
        AUTO("Auto",null,0),FOREST("Forest",BackgroundPreset.FOREST,1),GRASS("Grass",BackgroundPreset.FOREST,.18f),
        SAND("Sand",BackgroundPreset.DESERT,0),ROCK("Rock",BackgroundPreset.WASTELAND,0),SNOW("Snow / Ice",BackgroundPreset.ICE,0),
        WASTELAND("Wasteland",BackgroundPreset.WASTELAND,0),VOLCANIC("Volcanic",BackgroundPreset.VOLCANIC,0),
        OCEAN("Ocean",BackgroundPreset.OCEAN,0),LAKE("Lake / shallow water",BackgroundPreset.OCEAN,0),BEACH("Coast / beach",BackgroundPreset.DESERT,0),
        MOUNTAINS("Mountains",BackgroundPreset.MOUNTAINS,0),SPACE("Space",BackgroundPreset.SPACE,0),VOID("Void",BackgroundPreset.VOID,0);
        public final String label;public final BackgroundPreset preset;public final float forest;
        Material(String label,BackgroundPreset preset,float forest){this.label=label;this.preset=preset;this.forest=forest;}
        public boolean water(){return this==OCEAN||this==LAKE;}
    }
    public enum Brush {
        FOREST("Forest",Material.FOREST,0),GRASS("Grass / vegetation",Material.GRASS,0),SAND("Desert / sand",Material.SAND,0),ROCK("Rock",Material.ROCK,0),
        SNOW("Snow / ice",Material.SNOW,0),WASTELAND("Wasteland",Material.WASTELAND,0),VOLCANIC("Volcanic",Material.VOLCANIC,0),
        OCEAN("Ocean",Material.OCEAN,0),LAKE("Lake / shallow water",Material.LAKE,0),BEACH("Coast / beach",Material.BEACH,.35f),
        HILLS("Hills",null,12),MOUNTAINS("Mountain ridge",Material.MOUNTAINS,80),VALLEY("Lower terrain / valley",null,0),ISLAND("Island / landmass",Material.SAND,10),
        SPACE("Space",Material.SPACE,0),VOID("Void / empty",Material.VOID,0),ERASE("Restore Auto / base",Material.AUTO,0);
        public final String label;public final Material material;public final float defaultHeight;
        Brush(String label,Material material,float height){this.label=label;this.material=material;defaultHeight=height;}
        public boolean shape(){return this==HILLS||this==MOUNTAINS||this==VALLEY||this==ISLAND||this==BEACH;}
    }
    public static final Material[] MATERIALS=Material.values();
    public static final class Sample {
        public final float[] weights=new float[MATERIALS.length];
        public float coverage,height,heightCoverage;
    }
    public final int width,height;
    public final double outer;
    public final double[] xs,ys;
    private final byte[] material=new byte[COUNT];
    private final float[] coverage=new float[COUNT],heights=new float[COUNT],heightCoverage=new float[COUNT];
    private long revision;
    public BackgroundPaint(int width,int height){
        if(width<1||height<1||width>65535||height>65535)throw new IllegalArgumentException("Supported visual map dimensions required");
        this.width=width;this.height=height;outer=HorizonGeometry.outerDistance(width,height);xs=axis(width,outer);ys=axis(height,outer);
    }
    private static double[] axis(int length,double outer){double[] out=new double[NODES];
        for(int i=0;i<=EDGE_CELLS;i++){double t=(EDGE_CELLS-i)/(double)EDGE_CELLS;out[i]=-.5-outer*t*t;}
        for(int i=1;i<=MAP_CELLS;i++)out[EDGE_CELLS+i]=-.5+length*i/(double)MAP_CELLS;
        for(int i=1;i<=EDGE_CELLS;i++){double t=i/(double)EDGE_CELLS;out[EDGE_CELLS+MAP_CELLS+i]=length-.5+outer*t*t;}
        return out;
    }
    public long revision(){return revision;}
    public boolean outside(double x,double y){return x<-.5||y<-.5||x>width-.5||y>height-.5;}
    public boolean editable(int x,int y){return outside(xs[x],ys[y]);}
    public boolean empty(){for(int i=0;i<COUNT;i++)if(coverage[i]>.0001f||heightCoverage[i]>.0001f)return false;return true;}
    public BackgroundPaint copy(){var p=new BackgroundPaint(width,height);p.copyFrom(this);return p;}
    public void copyFrom(BackgroundPaint p){
        if(width!=p.width||height!=p.height)throw new IllegalArgumentException("Paint dimensions differ");
        System.arraycopy(p.material,0,material,0,COUNT);System.arraycopy(p.coverage,0,coverage,0,COUNT);System.arraycopy(p.heights,0,heights,0,COUNT);System.arraycopy(p.heightCoverage,0,heightCoverage,0,COUNT);revision++;
    }
    public void clear(){Arrays.fill(material,(byte)0);Arrays.fill(coverage,0);Arrays.fill(heights,0);Arrays.fill(heightCoverage,0);revision++;}
    public boolean same(BackgroundPaint p){return width==p.width&&height==p.height&&Arrays.equals(material,p.material)&&Arrays.equals(coverage,p.coverage)&&Arrays.equals(heights,p.heights)&&Arrays.equals(heightCoverage,p.heightCoverage);}
    public void line(double ax,double ay,double bx,double by,double radius,double hardness,Brush brush,double height){
        if(brush==null||!Double.isFinite(ax+ay+bx+by+radius+hardness+height)||Math.max(Math.max(Math.abs(ax),Math.abs(ay)),Math.max(Math.abs(bx),Math.abs(by)))>1e7||radius<2||radius>1024)throw new IllegalArgumentException("Finite bounded brush required");
        int steps=Math.max(1,Math.min(512,(int)Math.ceil(Math.hypot(bx-ax,by-ay)/Math.max(1,radius*.22))));
        for(int i=0;i<=steps;i++){double t=i/(double)steps;stamp(ax+(bx-ax)*t,ay+(by-ay)*t,radius,hardness,brush,(float)Math.max(0,Math.min(MAX_HEIGHT,height)));}
        revision++;
    }
    private void stamp(double x,double y,double radius,double hardness,Brush brush,float target){
        hardness=Math.max(0,Math.min(.98,hardness));int minX=lower(xs,x-radius),maxX=Math.min(NODES-1,lower(xs,x+radius)+1),minY=lower(ys,y-radius),maxY=Math.min(NODES-1,lower(ys,y+radius)+1);
        for(int iy=minY;iy<=maxY;iy++)for(int ix=minX;ix<=maxX;ix++){
            if(!editable(ix,iy))continue;double d=Math.hypot(xs[ix]-x,ys[iy]-y)/radius;if(d>=1)continue;
            float w=(float)Math.max(0,Math.min(1,(1-d)/(1-hardness)));w=w*w*(3-2*w);int at=iy*NODES+ix;
            if(brush==Brush.ERASE){coverage[at]*=1-w;heightCoverage[at]*=1-w;continue;}
            Material selected=brush.material;
            if(selected!=null){byte id=(byte)selected.ordinal();
                // Repainting a biome must not cut holes in an existing landmass at the brush rim.
                // One material per node is intentionally bounded; weak edges retain the old material.
                if(material[at]==id||coverage[at]<.5f||w>=.5f)material[at]=id;
                coverage[at]=Math.max(coverage[at],w);
                if(selected.water()&&material[at]==id){heights[at]=heightCoverage[at]=0;}
            }
            if(brush.shape()){heights[at]=target;heightCoverage[at]=Math.max(heightCoverage[at],w);}
        }
    }
    private static int lower(double[] axis,double value){int at=Arrays.binarySearch(axis,value);return Math.max(0,Math.min(NODES-2,at>=0?at:-at-2));}
    public void sample(double x,double y,Sample out){
        Arrays.fill(out.weights,0);out.coverage=out.height=out.heightCoverage=0;
        if(!Double.isFinite(x+y)||!outside(x,y)||x<xs[0]||x>xs[NODES-1]||y<ys[0]||y>ys[NODES-1])return;
        int ix=lower(xs,x),iy=lower(ys,y);float u=(float)((x-xs[ix])/(xs[ix+1]-xs[ix])),v=(float)((y-ys[iy])/(ys[iy+1]-ys[iy]));
        add(out,iy*NODES+ix,(1-u)*(1-v));add(out,iy*NODES+ix+1,u*(1-v));add(out,(iy+1)*NODES+ix,(1-u)*v);add(out,(iy+1)*NODES+ix+1,u*v);
        if(out.coverage>0)for(int i=1;i<out.weights.length;i++)out.weights[i]/=out.coverage;
        if(out.heightCoverage>0)out.height/=out.heightCoverage;
    }
    private void add(Sample out,int at,float w){float c=coverage[at]*w;if(material[at]>0){out.coverage+=c;out.weights[material[at]]+=c;}float h=heightCoverage[at]*w;out.heightCoverage+=h;out.height+=heights[at]*h;}
    public Material material(int x,int y){return MATERIALS[material[y*NODES+x]];}
    public float coverage(int x,int y){return coverage[y*NODES+x];}
    public float height(int x,int y){return heights[y*NODES+x];}
    public float heightCoverage(int x,int y){return heightCoverage[y*NODES+x];}
    /** Versioned local codec entry point, validates the protected region and all numeric bounds. */
    public void node(int x,int y,int id,float alpha,float h,float heightAlpha){
        if(x<0||x>=NODES||y<0||y>=NODES||id<0||id>=MATERIALS.length||!Float.isFinite(alpha+h+heightAlpha)||alpha<0||alpha>1||heightAlpha<0||heightAlpha>1||h<0||h>MAX_HEIGHT||(!editable(x,y)&&(alpha!=0||heightAlpha!=0)))throw new IllegalArgumentException("Invalid paint node");
        int at=y*NODES+x;material[at]=(byte)id;coverage[at]=alpha;heights[at]=h;heightCoverage[at]=heightAlpha;
    }
    public long fingerprint(){long h=width*31L+height;for(int i=0;i<COUNT;i++){h=h*31+material[i];h=h*31+Float.floatToIntBits(coverage[i]);h=h*31+Float.floatToIntBits(heights[i]);h=h*31+Float.floatToIntBits(heightCoverage[i]);}return h;}
    public int bytes(){return COUNT*13;}
    public record TreeSite(float x,float y,double rank){}
    /** Reservoir of painted forest nodes; small patches cannot disappear between random candidates. */
    public java.util.List<TreeSite> treeSites(int limit){
        limit=Math.max(0,Math.min(128,limit));var sites=new java.util.PriorityQueue<TreeSite>(java.util.Comparator.comparingDouble(TreeSite::rank).reversed());
        if(limit==0)return java.util.List.of();
        for(int y=0;y<NODES;y++)for(int x=0;x<NODES;x++){int at=y*NODES+x;if(coverage[at]<.7f||MATERIALS[material[at]].forest<.25f)continue;
            double rank=HorizonGeometry.hash(x+width,y+height);if(sites.size()==limit&&rank>=sites.peek().rank)continue;
            if(sites.size()==limit)sites.poll();
            double dx=(xs[Math.min(NODES-1,x+1)]-xs[Math.max(0,x-1)])*.3,dy=(ys[Math.min(NODES-1,y+1)]-ys[Math.max(0,y-1)])*.3;
            sites.add(new TreeSite((float)(xs[x]+(HorizonGeometry.hash(x+17,y+91)-.5)*dx),(float)(ys[y]+(HorizonGeometry.hash(x+83,y+13)-.5)*dy),rank));
        }var result=new java.util.ArrayList<>(sites);result.sort(java.util.Comparator.comparingDouble(TreeSite::rank));return result;
    }
}
