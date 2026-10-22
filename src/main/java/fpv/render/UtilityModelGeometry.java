package fpv.render;

/** Small immutable utility mechanisms, rendered through the existing native atlas batch. */
public final class UtilityModelGeometry {
    public record Model(float[] body,float[] lining,float[] lights){}
    private static final Model[][][] CACHE=new Model[4][17][3];
    private UtilityModelGeometry(){}
    public static Model model(UtilityModelProfile p,int size,int lod){
        if(p==null||size<1||size>16||lod<0||lod>2)throw new IllegalArgumentException("Bounded utility model required");
        var old=CACHE[p.ordinal()][size][lod];if(old!=null)return old;
        var body=new MechanicalBlockGeometry.Builder();var light=new MechanicalBlockGeometry.Builder();
        float[] shell=new float[0],lining=new float[0];
        if(lod==2){
            // Distant pads must not retain dozens of subpixel loading-arch faces.
            if(p==UtilityModelProfile.MINE){body.column(0,0,0,.42f,.70f,.28f,4);light.column(0,.704f,0,.14f,.75f,.14f,4);}
            else if(p==UtilityModelProfile.LIGHT){body.box(-.30f,0,-.30f,.30f,.16f,.30f,true);body.box(-.06f,.16f,-.06f,.06f,.50f,.06f,true);body.box(-.27f,.82f,-.27f,.27f,.94f,.27f,true);light.box(-.205f,.50f,-.205f,.205f,.82f,.205f,true);}
            else {body.box(-.46f,0,-.46f,.46f,.43f,.46f,true);light.column(0,.437f,0,.16f,.46f,.16f,4);}
        }else if(p==UtilityModelProfile.MINE){
            body.column(0,0,0,.46f,.45f,.39f,lod==0?8:4);
            body.column(0,.45f,0,.26f,.70f,.23f,lod==0?8:4);
            if(lod<2)for(int i=0;i<4;i++){double a=i*Math.PI/2;float x=(float)Math.cos(a)*.33f,z=(float)Math.sin(a)*.33f;body.column(x,.37f,z,.07f,.90f,.035f,4);}
            light.column(0,.704f,0,.14f,.75f,.14f,4);
        }else if(p==UtilityModelProfile.LIGHT){
            body.column(0,0,0,.38f,.16f,.28f,lod==0?8:4);
            body.column(0,.16f,0,.07f,.48f,.07f,4);
            body.box(-.24f,.48f,-.24f,.24f,.56f,.24f,true);
            body.column(0,.83f,0,.30f,.94f,.20f,4);
            for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)body.box(x*.21f-.025f,.56f,z*.21f-.025f,x*.21f+.025f,.84f,z*.21f+.025f,true);
            light.box(-.205f,.57f,-.205f,.205f,.82f,.205f,true);
        }else{
            var ports=ResourcePortGeometry.generate(size,.23f,lod==0);shell=ports.shell();lining=ports.lining();
            body.box(-.46f,.23f,-.46f,.46f,.43f,.46f,true);
            if(lod<2){
                for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2){
                    float w=p==UtilityModelProfile.LAUNCH?.065f:.045f;
                    body.box(x*.36f-w,.43f,z*.36f-w,x*.36f+w,p==UtilityModelProfile.LAUNCH?.80f:.66f,z*.36f+w,true);
                    light.box(x*.36f-w-.002f,.64f,z*.36f-w-.002f,x*.36f+w+.002f,p==UtilityModelProfile.LAUNCH?.70f:.69f,z*.36f+w+.002f,true);
                }
                if(p==UtilityModelProfile.LAUNCH){for(int side=-1;side<=1;side+=2)body.box(side*.36f-.045f,.72f,-.32f,side*.36f+.045f,.78f,.32f,true);}
                else {for(int side=-1;side<=1;side+=2)body.box(-.32f,.44f,side*.34f-.035f,.32f,.50f,side*.34f+.035f,true);}
            }
            light.column(0,.437f,0,.16f,.46f,.16f,lod==0?8:4);
        }
        body.face(-.46f,0,-.46f,.46f,0,-.46f,.46f,0,.46f,-.46f,0,.46f,0,-1,0);
        float[] parts=body.finish(),all=new float[shell.length+parts.length];System.arraycopy(shell,0,all,0,shell.length);System.arraycopy(parts,0,all,shell.length,parts.length);
        return CACHE[p.ordinal()][size][lod]=new Model(all,lining,light.finish());
    }
}
