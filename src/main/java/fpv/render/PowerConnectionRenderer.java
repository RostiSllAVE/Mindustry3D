package fpv.render;
import arc.Core;import arc.graphics.*;import arc.graphics.g3d.Camera3D;import arc.graphics.gl.Shader;import arc.struct.IntMap;import arc.util.Time;import fpv.construction.*;import mindustry.Vars;import mindustry.gen.Building;import mindustry.world.blocks.power.PowerNode;import java.util.*;

/** One read-only view of native Z0 links and the already-authorized XYZ edge set. */
final class PowerConnectionRenderer implements AutoCloseable {
 static final String SETTING="fpv-animated-power-connections";
 private static final VertexAttribute COLOR=new VertexAttribute(4,"a_color");
 private static final String VERTEX="attribute vec3 a_position;attribute vec4 a_color;attribute vec2 a_texCoord0;uniform mat4 u_combined;varying vec4 v_color;varying vec2 v_phase;void main(){v_color=a_color;v_phase=a_texCoord0;gl_Position=u_combined*vec4(a_position,1.0);}";
 private static final String FRAGMENT="varying vec4 v_color;varying vec2 v_phase;uniform float u_time;uniform float u_animated;uniform float u_glow;uniform float u_opacity;void main(){float a=v_phase.x*v_phase.y*2.4,b=(1.0-v_phase.x)*v_phase.y*2.4;float pulse=min(1.0,pow(0.5+0.5*cos(a-u_time*3.0+v_color.a),12.0)+pow(0.5+0.5*cos(b-u_time*3.0+v_color.a),12.0))*u_animated;float light=mix(0.82,0.58+0.36*pulse,u_animated);float alpha=mix(0.86,0.10+0.07*pulse,u_glow)*u_opacity;gl_FragColor=vec4(v_color.rgb*light,alpha);}";
 record Edge(long first,long second,float x,float y,float z,float X,float Y,float Z,float r,float g,float b){}
 private record Pair(long a,long b){Pair{if(a>b){long c=a;a=b;b=c;}}}
 private static final class End{Building b;float x,y,z;}
 private static final class Chunk{Mesh core,glow;float x,y,z,hx,hy,hz;}
 private final ArrayList<Edge> edges=new ArrayList<>();private final ArrayList<Chunk> chunks=new ArrayList<>();private final End a=new End(),b=new End();private Shader shader;private long signature=Long.MIN_VALUE,builds;private Object world;private LayerBuildingAdapter owner;private Collection<LayerBuildingAdapter.PowerLink> layerLinks=List.of();
 long builds(){return builds;}int links(){return edges.size();}List<Edge> edges(){return Collections.unmodifiableList(edges);}
 private boolean endpoint(long id,LayerRuntime runtime,End out){
  Building value;float base=0,h;
  if(id>0){if(runtime==null)return false;var c=runtime.grid().get(id);value=runtime.building(id);if(c==null||value==null||Vars.player==null)return false;if(c.team()!=Vars.player.team().id&&Vars.state.rules.fog&&Vars.fogControl!=null&&!Vars.fogControl.isVisible(Vars.player.team(),c.x(),c.y()))return false;base=(float)runtime.planeHeight(c.layer());h=(float)c.height();}
  else{long packed=-id-1;if(packed<0||packed>0xffffffffL)return false;value=Vars.world.build((int)packed);if(value==null||Vars.player==null||value.inFogTo(Vars.player.team()))return false;h=fpv.net.HeadlessGeometry.heightFor(value.block);}
  if(value.dead||value.health<=0||value.power==null||value.isPayload()||value.team==mindustry.game.Team.derelict)return false;out.b=value;out.x=value.x/Vars.tilesize;out.z=-value.y/Vars.tilesize;out.y=base+PowerModelProfile.attachment(value.block,h)+.004f;return Float.isFinite(out.x+out.y+out.z);
 }
 private boolean valid(long first,long second,LayerRuntime runtime){return first!=second&&endpoint(first,runtime,a)&&endpoint(second,runtime,b)&&a.b.team==b.b.team;}
 private long hash(long h,long first,long second){h=h*31+first;h=h*31+second;h=h*31+a.b.id;h=h*31+b.b.id;h=h*31+a.b.block.id;h=h*31+b.b.block.id;h=h*31+Float.floatToIntBits(a.x);h=h*31+Float.floatToIntBits(a.y);h=h*31+Float.floatToIntBits(a.z);h=h*31+Float.floatToIntBits(b.x);h=h*31+Float.floatToIntBits(b.y);return h*31+Float.floatToIntBits(b.z);}
 private void add(long first,long second,Set<Pair> seen){if(!seen.add(new Pair(first,second)))return;var color=a.b.block instanceof PowerNode node?node.laserColor1:b.b.block instanceof PowerNode node?node.laserColor1:mindustry.graphics.Pal.powerLight;edges.add(new Edge(first,second,a.x,a.y,a.z,b.x,b.y,b.z,color.r,color.g,color.b));}
 private static boolean nativeNode(Building b){return b.block instanceof PowerNode&&b.power!=null&&!b.dead&&Vars.world.build(b.pos())==b;}
 void sync(IntMap<Building> ground,LayerRuntime runtime){
  var current=runtime==null?null:runtime.nativeBuildings();if(current!=owner){owner=current;layerLinks=current==null?List.of():current.powerLinks();signature=Long.MIN_VALUE;}
  long hash=17;for(var source:ground.values())if(nativeNode(source))for(int i=0;i<source.power.links.size;i++){long first=-Integer.toUnsignedLong(source.pos())-1,second=-Integer.toUnsignedLong(source.power.links.get(i))-1;if(valid(first,second,runtime))hash=hash(hash,first,second);}
  for(var link:layerLinks)if(valid(link.first(),link.second(),runtime))hash=hash(hash,link.first(),link.second());
  if(world==Vars.world.tiles&&hash==signature)return;world=Vars.world.tiles;signature=hash;edges.clear();var seen=new HashSet<Pair>();
  for(var source:ground.values())if(nativeNode(source))for(int i=0;i<source.power.links.size;i++){long first=-Integer.toUnsignedLong(source.pos())-1,second=-Integer.toUnsignedLong(source.power.links.get(i))-1;if(valid(first,second,runtime))add(first,second,seen);}
  for(var link:layerLinks)if(valid(link.first(),link.second(),runtime))add(link.first(),link.second(),seen);rebuild();
 }
 private static Mesh mesh(float[] vertices){var m=new Mesh(true,vertices.length/9,0,VertexAttribute.position3,COLOR,VertexAttribute.texCoords);m.setVertices(vertices);return m;}
 private void rebuild(){for(var c:chunks){c.core.dispose();c.glow.dispose();}chunks.clear();builds++;
  for(int from=0;from<edges.size();from+=64){var core=new PowerBeamGeometry.Builder();var glow=new PowerBeamGeometry.Builder();float minX=Float.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
   for(int i=from;i<Math.min(from+64,edges.size());i++){var e=edges.get(i);core.span(e.x,e.y,e.z,e.X,e.Y,e.Z,.014f,e.r,e.g,e.b);glow.span(e.x,e.y,e.z,e.X,e.Y,e.Z,.05f,e.r,e.g,e.b);minX=Math.min(minX,Math.min(e.x,e.X));minY=Math.min(minY,Math.min(e.y,e.Y));minZ=Math.min(minZ,Math.min(e.z,e.Z));maxX=Math.max(maxX,Math.max(e.x,e.X));maxY=Math.max(maxY,Math.max(e.y,e.Y));maxZ=Math.max(maxZ,Math.max(e.z,e.Z));}
   var c=new Chunk();c.core=mesh(core.finish());c.glow=mesh(glow.finish());c.x=(minX+maxX)*.5f;c.y=(minY+maxY)*.5f;c.z=(minZ+maxZ)*.5f;c.hx=(maxX-minX)*.5f+.055f;c.hy=(maxY-minY)*.5f+.055f;c.hz=(maxZ-minZ)*.5f+.055f;chunks.add(c);
  }
 }
 void render(Camera3D camera,IntMap<Building> ground,LayerRuntime runtime){sync(ground,runtime);float opacity=arc.math.Mathf.clamp(mindustry.core.Renderer.laserOpacity);if(chunks.isEmpty()||opacity<=0)return;if(shader==null)shader=new Shader(VERTEX,FRAGMENT);boolean animated=Core.settings.getBool(SETTING,true);
  boolean depth=Gl.isEnabled(Gl.depthTest),blend=Gl.isEnabled(Gl.blend),cull=Gl.isEnabled(Gl.cullFace),write=Gl.getInt(Gl.depthWritemask)!=0;int program=Gl.getInt(Gl.currentProgram),src=Gl.getInt(Gl.blendSrcRgb),dst=Gl.getInt(Gl.blendDstRgb),srcA=Gl.getInt(Gl.blendSrcAlpha),dstA=Gl.getInt(Gl.blendDstAlpha),eq=Gl.getInt(Gl.blendEquationRgb),eqA=Gl.getInt(Gl.blendEquationAlpha);
  try{Gl.enable(Gl.depthTest);Gl.disable(Gl.cullFace);Gl.depthMask(false);Gl.enable(Gl.blend);Gl.blendEquation(Gl.funcAdd);Gl.blendFunc(Gl.srcAlpha,Gl.oneMinusSrcAlpha);shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_time",Float.isFinite(Time.time)?Time.time/60f%3600:0);shader.setUniformf("u_animated",animated?1:0);shader.setUniformf("u_opacity",opacity);
   for(var c:chunks){if(!camera.frustum.containsBounds(c.x,c.y,c.z,c.hx,c.hy,c.hz))continue;shader.setUniformf("u_glow",0);c.core.render(shader,Gl.triangles);if(animated){shader.setUniformf("u_glow",1);c.glow.render(shader,Gl.triangles);}}
  }finally{Gl.depthMask(write);if(depth)Gl.enable(Gl.depthTest);else Gl.disable(Gl.depthTest);if(cull)Gl.enable(Gl.cullFace);else Gl.disable(Gl.cullFace);if(blend)Gl.enable(Gl.blend);else Gl.disable(Gl.blend);Gl.blendFuncSeparate(src,dst,srcA,dstA);Gl.blendEquationSeparate(eq,eqA);Gl.useProgram(program);}
 }
 public void close(){for(var c:chunks){c.core.dispose();c.glow.dispose();}chunks.clear();edges.clear();if(shader!=null)shader.dispose();shader=null;signature=Long.MIN_VALUE;world=null;owner=null;layerLinks=List.of();}
}
