package fpv.render;

import fpv.terrain.TerrainElevation;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.world.Tile;
import java.util.*;

/** Bounded-strip, read-only biome analysis. Sections run cyclically S/E/N/W in native XY. */
public final class BorderBiomeProfile {
    public enum Biome { OCEAN, MOUNTAINS, FOREST, DESERT, ICE, WASTELAND, VOLCANIC, SPACE, UNKNOWN }
    public enum Side { SOUTH, EAST, NORTH, WEST }
    public record Cell(Biome material,float r,float g,float b,boolean mountain,boolean water,float vegetation,float tr,float tg,float tb,int form){}
    public interface Source {int width();int height();Cell cell(int x,int y);}
    public static final int MAX_SECTIONS_PER_SIDE=128;
    public static final class Sample {
        public final float[] weights=new float[Biome.values().length];
        public float r,g,b,tr,tg,tb;public int form;
        public Biome dominant(){int best=0;for(int i=1;i<weights.length;i++)if(weights[i]>weights[best])best=i;return Biome.values()[best];}
        public float weight(Biome biome){return weights[biome.ordinal()];}
    }
    public record Section(Side side,double start,double end,Sample value){}
    private final List<Section> sections;
    private final double[] centers;
    public final int width,height,strip;public final long visitedTiles;
    public final double perimeter;
    private BorderBiomeProfile(int width,int height,int strip,List<Section> sections,long visited){
        this.width=width;this.height=height;this.strip=strip;this.sections=List.copyOf(sections);visitedTiles=visited;perimeter=2d*(width+height);
        centers=new double[sections.size()];for(int i=0;i<centers.length;i++)centers[i]=(sections.get(i).start+sections.get(i).end)/2;
    }
    public List<Section> sections(){return sections;}
    public static BorderBiomeProfile analyze(Source source,int requestedStrip){
        int w=source.width(),h=source.height();if(w<1||h<1)throw new IllegalArgumentException("Nonempty native map required");
        int strip=Math.max(2,Math.min(16,requestedStrip));var result=new ArrayList<Section>();long visited=0;double offset=0;
        for(Side side:Side.values()){
            int length=side==Side.SOUTH||side==Side.NORTH?w:h,depth=Math.min(strip,side==Side.SOUTH||side==Side.NORTH?h:w);
            int count=Math.min(MAX_SECTIONS_PER_SIDE,Math.max(1,(length+7)/8));
            for(int section=0;section<count;section++){
                int start=section*length/count,end=(section+1)*length/count;Sample out=new Sample();
                float total=0,mountains=0,water=0,flora=0,linksRock=0,linksWater=0;int[] forms=new int[VegetationInstances.Form.values().length];
                Cell[] previous=new Cell[end-start];
                for(int d=0;d<depth;d++){
                    Cell last=null;float weight=1-d/(float)(depth*2);
                    for(int s=start;s<end;s++){
                        int x=s,y=d;
                        switch(side){case NORTH->{x=w-1-s;y=h-1-d;}case EAST->{x=w-1-d;y=s;}case WEST->{x=d;y=h-1-s;}default->{}}
                        Cell c=source.cell(x,y);visited++;if(c==null)c=new Cell(Biome.UNKNOWN,.36f,.36f,.42f,false,false,0,.15f,.26f,.16f,0);
                        total+=weight;out.weights[c.material.ordinal()]+=weight;mountains+=c.mountain?weight:0;water+=c.water?weight:0;flora+=c.vegetation*weight;
                        out.r+=c.r*weight;out.g+=c.g*weight;out.b+=c.b*weight;
                        if(c.vegetation>0){out.tr+=c.tr*c.vegetation*weight;out.tg+=c.tg*c.vegetation*weight;out.tb+=c.tb*c.vegetation*weight;forms[Math.floorMod(c.form,forms.length)]++;}
                        Cell above=previous[s-start];
                        if(c.mountain){if(last!=null&&last.mountain)linksRock++;if(above!=null&&above.mountain)linksRock++;}
                        if(c.water){if(last!=null&&last.water)linksWater++;if(above!=null&&above.water)linksWater++;}
                        previous[s-start]=c;last=c;
                    }
                }
                for(int i=0;i<out.weights.length;i++)out.weights[i]/=total;
                float rockFraction=mountains/total,floraFraction=flora/total;
                // Connected patches receive a bounded bonus; isolated decorative trees cannot overturn desert/water.
                out.weights[Biome.MOUNTAINS.ordinal()]+=rockFraction*1.4f+Math.min(.18f,linksRock/Math.max(1,total)*.10f);
                if(floraFraction>.30f)out.weights[Biome.FOREST.ordinal()]+=(floraFraction-.30f)*2.6f;
                out.weights[Biome.OCEAN.ordinal()]+=Math.min(.18f,linksWater/Math.max(1,total)*.10f)*(water/total);
                float norm=0;for(float v:out.weights)norm+=v;for(int i=0;i<out.weights.length;i++)out.weights[i]/=norm;
                out.r/=total;out.g/=total;out.b/=total;
                if(flora>0){out.tr/=flora;out.tg/=flora;out.tb/=flora;}else{out.tr=.15f;out.tg=.26f;out.tb=.16f;}
                for(int i=1;i<forms.length;i++)if(forms[i]>forms[out.form])out.form=i;
                result.add(new Section(side,offset+start,offset+end,out));
            }
            offset+=length;
        }
        return new BorderBiomeProfile(w,h,strip,result,visited);
    }
    /** Shared interpolation at corners and along transitions; no side owns an independent corner. */
    public void sample(double position,Sample out){
        double p=position-Math.floor(position/perimeter)*perimeter;int found=Arrays.binarySearch(centers,p);
        int right=found>=0?found:-found-1,left=(right+centers.length-1)%centers.length;right%=centers.length;
        double a=centers[left],b=centers[right];if(b<=a)b+=perimeter;if(p<a)p+=perimeter;
        float t=(float)Math.max(0,Math.min(1,(p-a)/(b-a)));t=t*t*(3-2*t);
        Sample x=sections.get(left).value,y=sections.get(right).value;
        for(int i=0;i<out.weights.length;i++)out.weights[i]=x.weights[i]+(y.weights[i]-x.weights[i])*t;
        out.r=lerp(x.r,y.r,t);out.g=lerp(x.g,y.g,t);out.b=lerp(x.b,y.b,t);out.tr=lerp(x.tr,y.tr,t);out.tg=lerp(x.tg,y.tg,t);out.tb=lerp(x.tb,y.tb,t);out.form=t<.5?x.form:y.form;
    }
    public long fingerprint(){long hash=((width*31L+height)*31+strip);for(var s:sections){for(float f:s.value.weights)hash=hash*31+Float.floatToIntBits(f);for(float f:new float[]{s.value.r,s.value.g,s.value.b,s.value.tr,s.value.tg,s.value.tb})hash=hash*31+Float.floatToIntBits(f);hash=hash*31+s.value.form;}return hash;}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    public static Source nativeSource(TerrainElevation terrain){return new Source(){
        public int width(){return Vars.world.width();}public int height(){return Vars.world.height();}
        public Cell cell(int x,int y){return nativeCell(Vars.world.tile(x,y),terrain);}
    };}
    public static Cell nativeCell(Tile tile,TerrainElevation terrain){
        if(tile==null)return null;
        var floor=tile.floor();var block=tile.block();Biome material;
        if(floor==Blocks.space||floor==Blocks.empty)material=Biome.SPACE;
        else if(floor.isLiquid&&floor.liquidDrop==Liquids.water)material=Biome.OCEAN;
        else if(floor==Blocks.snow||floor==Blocks.ice||floor==Blocks.iceSnow||floor==Blocks.redIce||floor==Blocks.cryofluid)material=Biome.ICE;
        else if(floor==Blocks.sand||floor==Blocks.darksand||floor==Blocks.salt)material=Biome.DESERT;
        else if(floor==Blocks.basalt||floor==Blocks.hotrock||floor==Blocks.magmarock||floor==Blocks.slag||floor==Blocks.tar||floor==Blocks.carbonStone)material=Biome.VOLCANIC;
        else material=FloorSurfaceRelief.amplitude(floor)>0?Biome.WASTELAND:Biome.UNKNOWN;
        boolean mountain=material!=Biome.SPACE&&terrain.naturalWall(tile);var form=VegetationInstances.form(block);
        float vegetation=form==null||material==Biome.SPACE?0:switch(form){case SHRUB,SPORE,FROND,CORAL->.45f;default->1;};
        var c=mountain?block.mapColor:floor.mapColor;var tree=block.mapColor;
        return new Cell(material,c.r,c.g,c.b,mountain,material==Biome.OCEAN,vegetation,tree.r,tree.g,tree.b,form==null?0:form.ordinal());
    }
}
