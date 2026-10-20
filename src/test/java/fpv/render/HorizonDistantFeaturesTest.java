package fpv.render;

import java.util.*;
import static fpv.render.BorderBiomeProfile.Biome.*;

/** CPU geometry/eligibility tests. Actual daylight/night/underwater appearance is a separate GL check. */
public final class HorizonDistantFeaturesTest {
    static int checks;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static BorderBiomeProfile profile(BorderBiomeProfile.Biome biome){
        return BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){
            public int width(){return 128;}public int height(){return 96;}
            public BorderBiomeProfile.Cell cell(int x,int y){return new BorderBiomeProfile.Cell(biome,.32f,.42f,.68f,
                biome==MOUNTAINS,biome==OCEAN,biome==FOREST?1:0,.15f,.35f,.19f,0);}
        },8);
    }
    static HorizonComposition composition(float water,float green,float sand,float rock,float structural,long seed){
        return new HorizonComposition(4096,water,green,sand,rock,0,0,rock,green,green,structural,seed);
    }
    static HorizonGeometry geometry(BorderBiomeProfile.Biome biome,HorizonComposition composition){return HorizonGeometry.build(profile(biome),s->0,composition);}
    static void validate(HorizonGeometry geometry,HorizonDistantFeatures.Result result){
        int stride=HorizonGeometry.STRIDE;
        check(stride==15&&result.vertices().length%(stride*3)==0,"features share native15-float triangle layout");
        check(result.vertices().length/stride<=HorizonDistantFeatures.MAX_VERTICES,"feature geometry bounded");
        check(result.islands()<=12&&result.outcrops()<=12&&result.structures()<=6,"feature counts bounded");
        for(int i=0;i<result.vertices().length;i+=stride){
            float[] v=result.vertices();for(int j=0;j<stride;j++)check(Float.isFinite(v[i+j]),"finite feature vertex");
            check(v[i]<-.5||v[i]>geometry.profile.width-.5||v[i+2]>.5||v[i+2]<-geometry.profile.height+.5,"all side faces stay outside playable map");
            check(v[i+6]==0&&v[i+7]==0&&v[i+8]==0&&v[i+14]==-1,"landmark never masquerades as blue ocean/wave geometry");
            float norm=v[i+11]*v[i+11]+v[i+12]*v[i+12]+v[i+13]*v[i+13];check(Math.abs(norm-1)<.0001,"lowpoly face has valid unit normal");
        }
    }
    public static void main(String[] args){
        var coast=geometry(OCEAN,composition(.70f,.30f,0,0,0,1234));
        var islands=HorizonDistantFeatures.build(coast);validate(coast,islands);
        check(islands.islands()>0&&islands.outcrops()==0&&islands.structures()==0,"green land evidence allows sparse islands in true open ocean only");
        check(islands.vertices().length/HorizonGeometry.STRIDE==islands.islands()*120,"8-point three-ring islands contain no hidden bottoms");
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(int i=0;i<islands.vertices().length;i+=HorizonGeometry.STRIDE){
            var v=islands.vertices();min=Math.min(min,v[i+1]);max=Math.max(max,v[i+1]);
            check(v[i+5]<v[i+3]&&v[i+4]>v[i+5],"islands use nearest dry green land palette, never blue water color");
        }
        check(min<HorizonLandscape.SEA&&max>HorizonLandscape.SEA+2,"island has submerged foot, dry rim and small peak");
        check(Arrays.equals(islands.vertices(),HorizonDistantFeatures.build(coast).vertices()),"same map seed gives exact cached feature geometry");
        var changed=HorizonDistantFeatures.build(geometry(OCEAN,composition(.70f,.30f,0,0,0,1235)));
        check(!Arrays.equals(islands.vertices(),changed.vertices()),"map context seed changes placement without native random state");
        var emptySea=HorizonDistantFeatures.build(geometry(OCEAN,composition(1,0,0,0,0,1234)));
        check(emptySea.vertices().length==0,"featureless ocean invents no distant land");
        var land=geometry(MOUNTAINS,composition(0,0,0,1,.25f,5678));
        var details=HorizonDistantFeatures.build(land);validate(land,details);
        check(details.islands()==0&&details.outcrops()>0&&details.structures()>0,"rocky industrial source permits outcrops and sparse angular traces on dry land");
        check(details.vertices().length/HorizonGeometry.STRIDE==details.outcrops()*45+details.structures()*30,"open-bottom outcrops/fragments use minimal bounded topology");
        int bases=0;
        for(int i=0;i<details.vertices().length;i+=HorizonGeometry.STRIDE){
            var v=details.vertices();float ground=HorizonDistantFeatures.groundHeight(land,v[i],v[i+2]);
            if(v[i+1]<=ground+.001)bases++;
        }
        check(bases>=details.outcrops()*5+details.structures()*4,"skirts attach to or below the submitted terrain triangles");
        var noIndustry=HorizonDistantFeatures.build(geometry(MOUNTAINS,composition(0,0,0,1,0,5678)));
        check(noIndustry.outcrops()>0&&noIndustry.structures()==0,"natural mountains cannot enable industrial traces");
        var dry=HorizonDistantFeatures.build(geometry(DESERT,composition(.6f,0,.4f,0,0,9012)));
        check(dry.islands()==0,"map water fraction alone cannot put islands on a dry border");
        var space=HorizonDistantFeatures.build(geometry(SPACE,composition(.3f,.4f,0,.3f,.1f,1234)));
        check(space.vertices().length==0,"space rejects all decorative terrain/industrial motifs");
        for(int seed=0;seed<24;seed++){
            var g=geometry(MOUNTAINS,composition(0,.15f,0,.85f,.40f,seed));var r=HorizonDistantFeatures.build(g);validate(g,r);
            check(r.outcrops()>0,"eligible multiple-seed rock scenes remain populated");
        }
        System.out.println("HorizonDistantFeaturesTest: "+checks+" bounded/deterministic/outside-map/coast/attachment/palette checks PASS; physical appearance separate");
    }
}
