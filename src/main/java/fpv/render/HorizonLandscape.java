package fpv.render;

import java.util.*;
import static fpv.render.BorderBiomeProfile.Biome.*;

/** Cached coastal rules used by geometry AND material generation. Never queried by gameplay. */
public final class HorizonLandscape {
    public static final float SEA=-LiquidSurfaceVisual.DEPTH;
    public enum Coast { BEACH, ROCKY, GREEN, ICE }
    public static final class Surface {
        public float coast,height,depth,r,g,b,waterR,waterG,waterB,sand,ice,space,forest;
        public int form;public float tr,tg,tb;public Coast style;public boolean ocean;
    }
    public final BorderBiomeProfile profile;
    public final HorizonComposition composition;
    private final BorderBiomeProfile.Sample[] land;
    private final double[] runLength;
    private final BorderBiomeProfile.Sample sample=new BorderBiomeProfile.Sample();
    private final java.util.function.DoubleUnaryOperator waterBed;
    private final fpv.background.BackgroundPaint paint;
    private final fpv.background.BackgroundPaint.Sample paintSample=new fpv.background.BackgroundPaint.Sample();
    private final BorderBiomeProfile.Sample paintBiome=new BorderBiomeProfile.Sample();
    private final BorderBiomeProfile.Cell[] paintCells=new BorderBiomeProfile.Cell[fpv.background.BackgroundPaint.MATERIALS.length];
    public HorizonLandscape(BorderBiomeProfile profile,HorizonComposition composition){
        this(profile,composition,s->Double.NaN);
    }
    public HorizonLandscape(BorderBiomeProfile profile,HorizonComposition composition,java.util.function.DoubleUnaryOperator waterBed){
        this(profile,composition,waterBed,null);
    }
    public HorizonLandscape(BorderBiomeProfile profile,HorizonComposition composition,java.util.function.DoubleUnaryOperator waterBed,fpv.background.BackgroundStyle style){
        this.profile=profile;this.composition=composition==null?HorizonComposition.EMPTY:composition;
        this.waterBed=waterBed;
        paint=style!=null&&style.mode==fpv.background.BackgroundStyle.Mode.CUSTOM&&style.paint!=null&&style.paint.width==profile.width&&style.paint.height==profile.height&&!style.paint.empty()?style.paint:null;
        if(paint!=null)for(var material:fpv.background.BackgroundPaint.MATERIALS)if(material.preset!=null)paintCells[material.ordinal()]=material.preset.cell();
        int n=profile.sections().size();land=new BorderBiomeProfile.Sample[n];runLength=new double[n];
        for(int i=0;i<n;i++){
            double length=0;int count=0;
            if(profile.readableBiome(i)==OCEAN){
                int left=i;while(count<n&&profile.readableBiome(left)==OCEAN){length+=sectionLength(left);count++;left=(left+n-1)%n;}
                int right=(i+1)%n;while(count<n&&profile.readableBiome(right)==OCEAN){length+=sectionLength(right);count++;right=(right+1)%n;}
            }
            runLength[i]=length;
            int nearest=-1;double best=Double.POSITIVE_INFINITY;double center=center(i);
            for(int j=0;j<n;j++)if(profile.readableBiome(j)!=OCEAN&&profile.readableBiome(j)!=SPACE){double d=Math.abs(center-center(j));d=Math.min(d,profile.perimeter-d);if(d<best){best=d;nearest=j;}}
            land[i]=nearest>=0?profile.readableSection(nearest):fallback();
        }
        // Both halves of a narrow shore transition use the same water-run identity.
        double[] original=runLength.clone();
        for(int i=0;i<n;i++)if(original[i]==0){int nearest=-1;double best=Double.POSITIVE_INFINITY;
            for(int j=0;j<n;j++)if(original[j]>0){double d=Math.abs(center(i)-center(j));d=Math.min(d,profile.perimeter-d);if(d<best){best=d;nearest=j;}}
            if(nearest>=0)runLength[i]=original[nearest];
        }
    }
    private double center(int i){var s=profile.sections().get(i);return(s.start()+s.end())*.5;}
    private double sectionLength(int i){var s=profile.sections().get(i);return s.end()-s.start();}
    private BorderBiomeProfile.Sample fallback(){
        var s=new BorderBiomeProfile.Sample();var c=composition;
        if(c.greenFraction()>.12f){s.weights[FOREST.ordinal()]=1;s.r=.28f;s.g=.43f;s.b=.22f;}
        else if(c.sandFraction()>.08f){s.weights[DESERT.ordinal()]=1;s.r=.76f;s.g=.60f;s.b=.38f;}
        else if(c.iceFraction()>.1f){s.weights[ICE.ordinal()]=1;s.r=.75f;s.g=.81f;s.b=.87f;}
        else{s.weights[WASTELAND.ordinal()]=1;s.r=.38f;s.g=.39f;s.b=.40f;}
        s.tr=.17f;s.tg=.30f;s.tb=.15f;return s;
    }
    public double warpedAlong(double along,double distance){
        var pt=HorizonGeometry.point(profile,along,distance);double seed=(composition.seed()&65535)*.0001;
        return along+Math.min(profile.perimeter*.035,distance*.12)*noise(pt.x()*.008+seed,pt.z()*.008-seed)*smooth(8,40,distance);
    }
    /** A positive coast value means water. Heights are SEABED there; the water mesh is always SEA. */
    public void sample(double along,double distance,float x,float z,Surface out){
        double s=warpedAlong(along,distance);profile.sampleReadable(s,sample);int section=profile.sectionIndex(s);
        var coastLand=land[section];float w=sample.weight(OCEAN);
        boolean ocean=runLength[section]>=Math.max(32,Math.min(profile.width,profile.height)*.60)||runLength[section]>=profile.perimeter*.28;
        double close=48+runLength[section]*2.0;
        if(!ocean)w*=1-smooth(close,close+24,distance);
        out.coast=w*2-1;out.ocean=ocean;out.space=sample.weight(SPACE);out.style=style(coastLand);
        boolean coastalMaterial=sample.weight(OCEAN)>.001f;
        float sand=coastalMaterial?coastLand.weight(DESERT):sample.weight(DESERT),ice=coastalMaterial?coastLand.weight(ICE):sample.weight(ICE);
        float forest=coastalMaterial?coastLand.weight(FOREST):sample.weight(FOREST);
        var material=coastalMaterial?coastLand:sample;
        out.r=material.r;out.g=material.g;out.b=material.b;out.sand=sand;out.ice=ice;out.forest=forest;out.form=material.form;out.tr=material.tr;out.tg=material.tg;out.tb=material.tb;
        // Water has its own identity. Never paint a dry ridge using a separately warped shader mask.
        if(sample.weight(OCEAN)>.5f){out.waterR=sample.r;out.waterG=sample.g;out.waterB=sample.b;}
        else{out.waterR=.27f;out.waterG=.36f;out.waterB=.66f;}
        double depth=waterDepth(along,distance,ocean,close);
        double shore=smooth(0,.30,Math.max(0,out.coast));
        out.depth=(float)(depth*shore);
        if(out.coast>=0)out.height=SEA-out.depth;
        else{
            double h=landHeight(material,x,z,distance,along);
            double shoreEdge=smooth(0,out.style==Coast.ROCKY?.16:.60,-out.coast);
            out.height=(float)(SEA+(Math.max(0,h)-SEA)*shoreEdge);out.depth=0;
        }
        if(composition.volcanicScars()&&material.weight(VOLCANIC)>.5&&distance>32&&noise(x*.045,z*.045)>.25){out.r*=.62f;out.g*=.45f;out.b*=.42f;}
        if(paint!=null)applyPaint(along,distance,x,z,out);
    }
    public boolean custom(){return paint!=null;}
    public fpv.background.BackgroundPaint paint(){return paint;}
    private double waterDepth(double along,double distance,boolean ocean,double close){
        double edge=waterBed.applyAsDouble(along),start=Double.isFinite(edge)?Math.max(.01,SEA-edge):.6;
        double max=ocean?64:8;return start+(Math.max(start,max)-start)*smooth(0,ocean?1200:Math.max(60,close),distance);
    }
    private void applyPaint(double along,double distance,float x,float z,Surface out){
        paint.sample(x,-z,paintSample);float coverage=(float)smooth(.20,.80,paintSample.coverage);
        if(coverage==0&&paintSample.heightCoverage==0)return;
        Arrays.fill(paintBiome.weights,0);float water=0,space=0,forest=0,sand=0,ice=0,r=0,g=0,b=0,land=0;int dominant=0;float best=0;
        for(var material:fpv.background.BackgroundPaint.MATERIALS){int i=material.ordinal();float w=paintSample.weights[i];if(w==0||material.preset==null)continue;var cell=paintCells[i];
            if(material.water()){water+=w;continue;}if(material.preset.biome==SPACE){space+=w;continue;}
            land+=w;r+=cell.r()*w;g+=cell.g()*w;b+=cell.b()*w;forest+=material.forest*w;
            if(material.preset.biome==DESERT)sand+=w;if(material.preset.biome==ICE)ice+=w;paintBiome.weights[material.preset.biome.ordinal()]+=w;
            if(w>best){best=w;dominant=i;}
        }
        float previousHeight=out.height;out.coast+=(water*2-1-out.coast)*coverage;out.space+=(space-out.space)*coverage;
        out.forest+=(forest-out.forest)*coverage;out.sand+=(sand-out.sand)*coverage;out.ice+=(ice-out.ice)*coverage;
        if(land>0){r/=land;g/=land;b/=land;out.r+=(r-out.r)*coverage;out.g+=(g-out.g)*coverage;out.b+=(b-out.b)*coverage;
            for(int i=0;i<paintBiome.weights.length;i++)paintBiome.weights[i]/=land;
            var cell=paintCells[dominant];out.form=dominant==fpv.background.BackgroundPaint.Material.GRASS.ordinal()?VegetationInstances.Form.SHRUB.ordinal():cell.form();out.tr=cell.tr();out.tg=cell.tg();out.tb=cell.tb();out.style=style(paintBiome);
        }
        if(water>0){var blue=paintCells[fpv.background.BackgroundPaint.Material.OCEAN.ordinal()];out.waterR+=(blue.r()-out.waterR)*coverage;out.waterG+=(blue.g()-out.waterG)*coverage;out.waterB+=(blue.b()-out.waterB)*coverage;}
        if(out.coast>=0){boolean lake=paintSample.weights[fpv.background.BackgroundPaint.Material.LAKE.ordinal()]>.5f;out.ocean=!lake;
            out.depth=(float)(waterDepth(along,distance,!lake,80)*smooth(0,.30,out.coast));if(lake)out.depth=Math.min(4,out.depth);if(paintSample.weights[fpv.background.BackgroundPaint.Material.SHALLOW.ordinal()]>.5f)out.depth=Math.min(.8f,out.depth);out.height=SEA-out.depth;
        }else{
            double h=Math.max(0,previousHeight);if(land>0)h+=(landHeight(paintBiome,x,z,distance,along)-h)*coverage;
            if(paintSample.heightCoverage>0)h+=(paintSample.height-h)*paintSample.heightCoverage;
            // Custom height cannot form a giant wall immediately at the native border.
            double safe=Math.max(0,previousHeight)+Math.max(0,distance-4)*.65;
            out.height=(float)(SEA+(Math.max(0,Math.min(safe,Math.min(fpv.background.BackgroundPaint.MAX_HEIGHT,h)))-SEA)*smooth(0,1,-out.coast));out.depth=0;
        }
    }
    private static Coast style(BorderBiomeProfile.Sample s){if(s.weight(DESERT)>.5)return Coast.BEACH;if(s.weight(FOREST)>.5)return Coast.GREEN;if(s.weight(ICE)>.5)return Coast.ICE;return Coast.ROCKY;}
    private double landHeight(BorderBiomeProfile.Sample s,float x,float z,double d,double along){
        double diag=Math.hypot(profile.width,profile.height),n=noise(x*.018,z*.018),fine=noise(x*.065+31,z*.065-17);
        double rise=smooth(4,38,d),ridge=Math.max(0,Math.min(1,(n+.5)*1.3));
        double h=s.weight(MOUNTAINS)*Math.min(120,Math.max(14,diag*.28))*rise*(.12+.65*ridge*ridge+.2*(fine+.5));
        h+=(s.weight(WASTELAND)*3+s.weight(VOLCANIC)*6)*(.55+.30*n+.15*fine)*rise;
        h+=s.weight(DESERT)*Math.min(8,diag*.015)*(1+n*.70+fine*.18);
        h+=s.weight(ICE)*(.2+rise*(.5+.35*n+.15*fine)*Math.min(20,diag*.04));
        h+=s.weight(FOREST)*(n+1)*2;
        // Far massing remains raised; atmosphere closes BEHIND ridges instead of flattening them away.
        double outer=HorizonGeometry.outerDistance(profile.width,profile.height);
        double closure=smooth(outer*.22,outer*.52,d);
        double phase=(composition.seed()&4095)*.01,peaks=.30+.70*Math.pow(.5+.5*StrictMath.sin(along/profile.perimeter*Math.PI*14+phase),2);
        double wall=s.weight(MOUNTAINS)*85+s.weight(WASTELAND)*45+s.weight(VOLCANIC)*70+s.weight(FOREST)*38+s.weight(DESERT)*24+s.weight(ICE)*60;
        return h+closure*wall*peaks;
    }
    public static double smooth(double a,double b,double x){double t=Math.max(0,Math.min(1,(x-a)/(b-a)));return t*t*(3-2*t);}
    public static double noise(double x,double y){int a=(int)Math.floor(x),b=(int)Math.floor(y);double u=x-a,v=y-b;u=u*u*(3-2*u);v=v*v*(3-2*v);return((1-v)*((1-u)*HorizonGeometry.hash(a,b)+u*HorizonGeometry.hash(a+1,b))+v*((1-u)*HorizonGeometry.hash(a,b+1)+u*HorizonGeometry.hash(a+1,b+1)))*2-1;}
}
