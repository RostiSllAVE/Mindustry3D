package fpv.render;
/** Shared immutable mechanical templates; no per-instance/per-frame tessellation. */
public final class PowerModelGeometry {
 private PowerModelGeometry(){}
 public record Model(float[] body,float[] lining,float[] rotor,float[] lights){}
 private static final Model[][][] CACHE=new Model[14][17][3];
 public static Model model(PowerModelProfile p,int size,int lod){
  if(p==null||size<1||size>16||lod<0||lod>2)throw new IllegalArgumentException("Bounded power model required");var old=CACHE[p.ordinal()][size][lod];if(old!=null)return old;
  var b=new MechanicalBlockGeometry.Builder();var r=new MechanicalBlockGeometry.Builder();var l=new MechanicalBlockGeometry.Builder();float[] shell=new float[0],lining=new float[0];int n=lod==0?8:4;
  if(lod==2){float top=p.solar()?p.terminal-.03f:.80f;b.box(-.48f,0,-.48f,.48f,top,.48f,true);}
  else{
   if(p.ordinal()>=8){var ports=ResourcePortGeometry.generate(size,.18f,lod==0);shell=ports.shell();lining=ports.lining();}else b.box(-.47f,0,-.47f,.47f,.16f,.47f,true);
   switch(p){
    case NODE->{b.column(0,.16f,0,.10f,.83f,.07f,n);b.column(0,.75f,0,.23f,.84f,.23f,n);}
    case LARGE_NODE->{for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.column(x*.23f,.16f,z*.23f,.04f,.72f,.03f,4);b.column(0,.16f,0,.15f,.84f,.12f,n);b.box(-.34f,.69f,-.07f,.34f,.77f,.07f,true);b.box(-.07f,.78f,-.34f,.07f,.86f,.34f,true);}
    case SURGE->{for(int x=-1;x<=1;x+=2){b.box(x*.18f-.035f,.16f,-.09f,x*.18f+.035f,.88f,.09f,true);}for(float y:new float[]{.35f,.56f,.78f})b.box(-.30f,y,-.12f,.30f,y+.045f,.12f,true);b.column(0,.78f,0,.09f,.90f,.09f,n);}
    case DIODE->{b.box(-.34f,.16f,-.27f,.34f,.52f,.27f,true);for(int x=-1;x<=1;x+=2)b.column(x*.28f,.40f,0,.065f,.71f,.065f,4);b.box(-.34f,.52f,-.27f,.34f,.75f,.27f,true);}
    case BATTERY,LARGE_BATTERY->{int cells=p==PowerModelProfile.BATTERY?2:3;float step=.72f/cells,rad=step*.38f;for(int x=0;x<cells;x++)for(int z=0;z<cells;z++){float cx=(x-(cells-1)*.5f)*step,cz=(z-(cells-1)*.5f)*step;b.box(cx-rad,.16f,cz-rad,cx+rad,.70f,cz+rad,true);l.face(cx-rad*.7f,.34f,cz-rad-.003f,cx+rad*.7f,.34f,cz-rad-.003f,cx+rad*.7f,.60f,cz-rad-.003f,cx-rad*.7f,.60f,cz-rad-.003f,0,0,-1);
      l.face(cx+rad*.7f,.34f,cz+rad+.003f,cx-rad*.7f,.34f,cz+rad+.003f,cx-rad*.7f,.60f,cz+rad+.003f,cx+rad*.7f,.60f,cz+rad+.003f,0,0,1);
      l.face(cx-rad-.003f,.34f,cz+rad*.7f,cx-rad-.003f,.34f,cz-rad*.7f,cx-rad-.003f,.60f,cz-rad*.7f,cx-rad-.003f,.60f,cz+rad*.7f,-1,0,0);
      l.face(cx+rad+.003f,.34f,cz-rad*.7f,cx+rad+.003f,.34f,cz+rad*.7f,cx+rad+.003f,.60f,cz+rad*.7f,cx+rad+.003f,.60f,cz-rad*.7f,1,0,0);
      l.face(cx-rad*.7f,.762f,cz-rad*.7f,cx-rad*.7f,.762f,cz+rad*.7f,cx+rad*.7f,.762f,cz+rad*.7f,cx+rad*.7f,.762f,cz-rad*.7f,0,1,0);}b.box(-.40f,.70f,-.40f,.40f,.76f,.40f,true);}
    case SOLAR,LARGE_SOLAR->{float top=p.terminal-.05f;for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.box(x*.32f-.025f,.16f,z*.32f-.025f,x*.32f+.025f,top,z*.32f+.025f,true);b.box(-.47f,top-.045f,-.47f,.47f,top,.47f,true);for(int s=-1;s<=1;s+=2){b.box(s*.455f-.015f,top,-.47f,s*.455f+.015f,top+.025f,.47f,true);}}
    case COMBUSTION->{b.box(-.29f,.18f,-.26f,.29f,.68f,.28f,true);b.column(0,.67f,0,.045f,.79f,.045f,4);for(int x=-1;x<=1;x+=2)b.column(x*.31f,.18f,-.25f,.065f,.93f,.05f,4);l.box(-.19f,.39f,.281f,.19f,.58f,.285f,true);rotor(r,.20f,.72f,n);}
    case STEAM->{for(int x=-1;x<=1;x+=2){b.column(x*.23f,.18f,0,.17f,.68f,.15f,n);b.column(x*.30f,.56f,-.27f,.06f,.93f,.05f,4);}b.box(-.30f,.68f,-.21f,.30f,.74f,.21f,true);b.column(0,.73f,0,.045f,.83f,.045f,4);rotor(r,.25f,.76f,n);}
    case DIFFERENTIAL->{for(int x=-1;x<=1;x+=2){b.column(x*.24f,.18f,0,.18f,.80f,.16f,n);if(lod==0)for(int z=-2;z<=2;z++)b.box(x*.39f-.025f,.30f,z*.12f-.025f,x*.39f+.025f,.70f,z*.12f+.025f,true);}b.box(-.30f,.72f,-.08f,.30f,.81f,.08f,true);l.box(-.06f,.36f,-.12f,.06f,.70f,.12f,true);}
    case RTG->{b.column(0,.18f,0,.33f,.76f,.28f,n);for(int x=-1;x<=1;x+=2)for(int z=-2;z<=2;z++)b.box(x*.36f-.025f,.26f,z*.13f-.025f,x*.36f+.025f,.74f,z*.13f+.025f,true);b.box(-.25f,.75f,-.25f,.25f,.83f,.25f,true);}
    case THORIUM->{b.column(0,.18f,0,.26f,.80f,.22f,n);for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2){b.column(x*.31f,.18f,z*.31f,.07f,.88f,.055f,n);b.box(x*.31f-.04f,.25f,z*.31f-.04f,x*.31f+.04f,.35f,z*.31f+.04f,true);}l.column(0,.75f,0,.17f,.83f,.15f,n);}
    case IMPACT->{for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)b.column(x*.32f,.18f,z*.32f,.065f,.95f,.045f,n);for(int z=-1;z<=1;z+=2)b.box(-.36f,.80f,z*.32f-.04f,.36f,.87f,z*.32f+.04f,true);b.column(0,.18f,0,.29f,.35f,.25f,n);rotor(r,.26f,.54f,n);l.column(0,.39f,0,.10f,.85f,.08f,n);}
   }
  }
  // A bounded central terminal makes all supported link endpoints explicit, including far LOD.
  b.column(0,p==PowerModelProfile.LARGE_BATTERY?.74f:Math.max(.17f,p.terminal-.08f),0,.045f,p.terminal,.045f,4);l.column(0,p.terminal-.025f,0,.047f,p.terminal+.004f,.047f,4);
  b.face(-.47f,0,-.47f,.47f,0,-.47f,.47f,0,.47f,-.47f,0,.47f,0,-1,0);
  var a=b.finish();var body=new float[shell.length+a.length];System.arraycopy(shell,0,body,0,shell.length);System.arraycopy(a,0,body,shell.length,a.length);
  return CACHE[p.ordinal()][size][lod]=new Model(body,lining,r.finish(),l.finish());
 }
 private static void rotor(MechanicalBlockGeometry.Builder r,float radius,float y,int n){r.column(0,y,0,.045f,y+.04f,.045f,4);for(int i=0;i<n;i++){double a=i*Math.PI*2/n;float x=(float)Math.cos(a)*radius,z=(float)Math.sin(a)*radius;r.box(x-.035f,y,z-.035f,x+.035f,y+.045f,z+.035f,true);}}
}
