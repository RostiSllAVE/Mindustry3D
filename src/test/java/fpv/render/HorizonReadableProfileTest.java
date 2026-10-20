package fpv.render;

import java.util.function.BiFunction;
import static fpv.render.BorderBiomeProfile.Biome.*;

/** Bounded synthetic border sources; no renderer, native state or physical gameplay acceptance. */
public final class HorizonReadableProfileTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void close(float actual,float expected,String message){check(Math.abs(actual-expected)<.00002f,message+": "+actual+" != "+expected);}
    private static BorderBiomeProfile.Cell cell(BorderBiomeProfile.Biome biome,float r,float g,float b,boolean rock,float flora){
        return new BorderBiomeProfile.Cell(biome,r,g,b,rock,biome==OCEAN,flora,.8f,.1f,.7f,0);
    }
    private static BorderBiomeProfile profile(int w,int h,BiFunction<Integer,Integer,BorderBiomeProfile.Cell> cells){
        return BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){
            public int width(){return w;}public int height(){return h;}
            public BorderBiomeProfile.Cell cell(int x,int y){check(x<8||y<8||x>=w-8||y>=h-8,"analysis stays inside bounded edge strip");return cells.apply(x,y);}
        },8);
    }
    private static void equal(BorderBiomeProfile.Sample a,BorderBiomeProfile.Sample b,String label){
        for(int i=0;i<a.weights.length;i++)close(a.weights[i],b.weights[i],label+" weight "+i);
        close(a.r,b.r,label+" red");close(a.g,b.g,label+" green");close(a.b,b.b,label+" blue");
        close(a.tr,b.tr,label+" tree red");close(a.tg,b.tg,label+" tree green");close(a.tb,b.tb,label+" tree blue");check(a.form==b.form,label+" form");
    }
    public static void main(String[] args){
        var sand=cell(DESERT,.9f,.7f,.2f,false,0);var sea=cell(OCEAN,.1f,.25f,.8f,false,0);
        var p=profile(64,48,(x,y)->x<32?sand:sea);var q=profile(64,48,(x,y)->x<32?sand:sea);
        var sample=new BorderBiomeProfile.Sample();var again=new BorderBiomeProfile.Sample();
        check(p.fingerprint()==q.fingerprint(),"identical border sources have deterministic fingerprint");
        check(p.sectionIndex(0)==0&&p.sectionIndex(7.999)==0&&p.sectionIndex(8)==1,"actual half-open section footprints");
        check(p.sectionIndex(p.perimeter)==0&&p.sectionIndex(-1)==p.sections().size()-1,"section lookup wraps cyclically");
        check(p.sectionIndex(Double.NaN)==0&&p.sectionIndex(Double.POSITIVE_INFINITY)==0,"invalid diagnostic coordinates fall back safely");
        check(p.readableBiome(3)==DESERT&&p.readableBiome(4)==OCEAN,"adjacent material cores preserve categorical identity");
        check(p.readableBiome(-1)==p.readableBiome(p.sections().size()-1),"readable section indices wrap");
        for(int i=0;i<p.sections().size();i++){
            var section=p.sections().get(i);double middle=(section.start()+section.end())*.5;
            p.sampleReadable(middle,sample);q.sampleReadable(middle,again);equal(sample,again,"repeatable core");
            close(sample.weight(p.readableBiome(i)),1,"each section centre is an unmixed material core");
            var returned=p.readableSection(i);equal(sample,returned,"section accessor");returned.weights[0]=7;returned.r=7;
            p.sampleReadable(middle,again);equal(sample,again,"readableSection defensive copy cannot corrupt cache");
        }
        p.sampleReadable(31.59,sample);close(sample.weight(DESERT),1,"sand remains pure outside narrow boundary band");close(sample.r,.9f,"pure native sand colour");
        p.sampleReadable(32.41,sample);close(sample.weight(OCEAN),1,"water remains pure outside narrow boundary band");close(sample.b,.8f,"pure native water colour");
        p.sampleReadable(32,sample);close(sample.weight(DESERT),.5f,"actual shared boundary has continuous half blend");close(sample.weight(OCEAN),.5f,"boundary water half blend");
        int blended=0;for(int i=0;i<=200;i++){
            double at=31+i*.01;p.sampleReadable(at,sample);q.sampleReadable(at,again);equal(sample,again,"deterministic narrow transition");
            if(sample.weight(OCEAN)>0&&sample.weight(OCEAN)<1)blended++;
            float sum=0;for(float weight:sample.weights){check(weight>=0&&weight<=1,"bounded readable weight");sum+=weight;}close(sum,1,"normalised readable transition");
        }
        check(blended>=78&&blended<=81,"transition spans approximately .8 of an eight-tile section, not its entire width: "+blended);
        // Legacy sampling deliberately remains broad for pre-existing baseline consumers.
        p.sample(30,sample);check(sample.weight(OCEAN)>.1f,"legacy sample retains its original section-centre interpolation");
        p.sampleReadable(30,sample);close(sample.weight(OCEAN),0,"readable sample removes broad wash");
        for(double boundary:new double[]{0,32,64,112,144,176,p.perimeter}){
            p.sampleReadable(boundary-1e-7,sample);p.sampleReadable(boundary+1e-7,again);equal(sample,again,"cyclic boundary/corner continuity");
            p.sampleReadable(boundary+p.perimeter*5,sample);p.sampleReadable(boundary,again);equal(sample,again,"cyclic repeated query");
        }
        var mixed=profile(64,48,(x,y)->x%8<6?sand:sea);
        for(int i=0;i<mixed.sections().size();i++){
            var material=mixed.readableSection(i);check(mixed.readableBiome(i)==DESERT,"sand-majority section stays desert");
            close(material.r,.9f,"sand palette excludes minority blue water");close(material.g,.7f,"unmixed desert green");close(material.b,.2f,"unmixed desert blue");
        }
        var wall=cell(OCEAN,.45f,.42f,.38f,true,0);
        var rock=profile(64,48,(x,y)->x%8<5?wall:sea);
        for(int i=0;i<rock.sections().size();i++){
            check(rock.readableBiome(i)==MOUNTAINS,"majority natural walls override water native floor");var colour=rock.readableSection(i);
            close(colour.r,.45f,"rock palette contains actual wall colour");close(colour.b,.38f,"rock does not inherit blue floor");close(colour.weight(OCEAN),0,"rock core does not shade as water");
        }
        var coherentSea=profile(64,48,(x,y)->x%8<2?wall:sea);
        for(int i=0;i<coherentSea.sections().size();i++){
            check(coherentSea.readableBiome(i)==OCEAN,"coherent open water overrides sparse rock influence");
            var colour=coherentSea.readableSection(i);close(colour.r,.1f,"water palette excludes rocky minority");close(colour.b,.8f,"native water identity retained");
        }
        var green=profile(32,24,(x,y)->cell(WASTELAND,.22f,.44f,.16f,false,1));
        for(int i=0;i<green.sections().size();i++){
            check(green.readableBiome(i)==FOREST,"dense vegetation retains forest identity");var colour=green.readableSection(i);
            close(colour.r,.22f,"forest material uses native supporting ground, not canopy");close(colour.g,.44f,"forest ground green preserved");
            close(colour.tr,.8f,"canopy identity retained separately for proxies");close(colour.tb,.7f,"canopy blue retained separately");
        }
        var tiny=profile(1,1,(x,y)->sea);tiny.sampleReadable(-.001,sample);close(sample.weight(OCEAN),1,"one-cell map has continuous readable surface");
        var unknown=profile(16,16,(x,y)->null);unknown.sampleReadable(0,sample);check(sample.dominant()==UNKNOWN,"unknown/null source remains safe");
        System.out.println("HorizonReadableProfileTest: "+checks+" deterministic bounded/core/boundary/unmixed-palette/rock-water checks PASS; synthetic sources, not physical gameplay acceptance");
    }
}
