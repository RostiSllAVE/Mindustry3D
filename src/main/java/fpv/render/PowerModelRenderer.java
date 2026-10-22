package fpv.render;
import arc.Core;import arc.graphics.g2d.*;import arc.math.Mathf;import mindustry.gen.Building;import mindustry.world.Block;import mindustry.world.blocks.power.*;import java.util.IdentityHashMap;
/** Native power state is read only; this class never updates a graph, consumes fuel or heats reactors. */
final class PowerModelRenderer {
 private static final class Model{TextureRegion region;final float[][][] parts=new float[3][][];}
 private final IdentityHashMap<Block,Model> cache=new IdentityHashMap<>();private Object atlas;private long builds,bytes;private final double[] basis=new double[9],moving=new double[9];
 static boolean supports(Block b){return PowerModelProfile.forBlock(b)!=null;}long builds(){return builds;}long bytes(){return bytes;}int entries(){return cache.size();}void clear(){cache.clear();atlas=null;builds=bytes=0;}
 private static float clamp(float v){return Float.isFinite(v)?Mathf.clamp(v):0;}
 static float level(Building b){if(b instanceof Battery.BatteryBuild)return b.power==null?0:clamp(b.power.status);if(b instanceof PowerGenerator.GeneratorBuild g)return b.enabled?clamp(g.productionEfficiency):0;return b.power==null||b.power.graph==null?0:clamp(b.power.graph.getSatisfaction());}
 static float clock(Building b){float t=b instanceof ConsumeGenerator.ConsumeGeneratorBuild c?c.totalTime:b instanceof ImpactReactor.ImpactReactorBuild i?i.totalProgress:0;return Float.isFinite(t)?t:0;}
 boolean render(Building b,NativeAtlasMesh.Builder out,float x,float base,float z,float ignored,float h,float distance){
  var p=PowerModelProfile.forBlock(b.block);if(p==null||!NativeAtlasMesh.valid(b.block.region))return false;if(atlas!=Core.atlas){clear();atlas=Core.atlas;}var m=cache.get(b.block);if(m==null||m.region!=b.block.region){m=create(b.block,p);cache.put(b.block,m);builds++;}
  int lod=ManufacturingModelProfile.lod(distance);float size=b.block.size,yaw=b.block.rotate&&b.block.rotateDraw?b.rotation*90:0;SpriteExtrusionGeometry.basis(90+yaw,0,0,basis);var a=m.parts[lod];out.solidExtrusion(m.region,a[0],x,base,z,size,h,size,basis,true);out.extrusion(m.region,a[1],x,base,z,size,h,size,basis,.15f,.15f,.15f);
  if(lod==2||p.solar()){float roof=p.solar()?p.terminal-.049f:.801f;out.top(m.region,x,base+h*roof,z,size*(p.solar()?.88f:.96f),size*(p.solar()?.94f:.96f),-yaw,1,1,1,false);}
  if(lod<2){SpriteExtrusionGeometry.basis(90+yaw+(clock(b)*2)%360,0,0,moving);out.solidExtrusion(m.region,a[2],x,base,z,size,h,size,moving,false);}
  if(b.block instanceof PowerDiode diode&&NativeAtlasMesh.valid(diode.arrow))out.top(diode.arrow,x,base+h*(lod==2?.806f:.754f),z,size*.50f,size*.50f,-b.rotation*90f,1,1,1,false);
  float v=.08f+.82f*level(b);var c=p==PowerModelProfile.THORIUM||p==PowerModelProfile.RTG?mindustry.content.Items.thorium.color:mindustry.graphics.Pal.powerLight;out.extrusion(Core.atlas.white(),a[3],x,base,z,size,h,size,basis,c.r*v,c.g*v,c.b*v);return true;
 }
 private Model create(Block b,PowerModelProfile p){var m=new Model();m.region=b.region;float u=.14f,v=.14f;if(m.region instanceof TextureAtlas.AtlasRegion ar){var pix=Core.atlas.getPixmap(ar);if(pix!=null&&!pix.pixmap.isDisposed()){double best=Double.POSITIVE_INFINITY;for(int y=0;y<pix.height;y++)for(int x=0;x<pix.width;x++)if(pix.getA(x,y)>=200){double d=Math.pow(x/(double)Math.max(1,pix.width-1)-.14,2)+Math.pow(y/(double)Math.max(1,pix.height-1)-.14,2);if(d<best){best=d;u=x/(float)Math.max(1,pix.width-1);v=y/(float)Math.max(1,pix.height-1);}}}}
  for(int lod=0;lod<3;lod++){var g=PowerModelGeometry.model(p,b.size,lod);m.parts[lod]=new float[][]{material(g.body(),u,v,false),material(g.lining(),u,v,false),material(g.rotor(),u,v,false),material(g.lights(),.5f,.5f,true)};for(var a:m.parts[lod])bytes+=(long)a.length*4;}return m;}
 private static float[] material(float[] raw,float u,float v,boolean white){var a=raw.clone();for(int i=0;i<a.length;i+=8)if(white||a[i+6]<0){a[i+6]=u;a[i+7]=v;}return a;}
}
