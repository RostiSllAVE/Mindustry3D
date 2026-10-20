package fpv.render;
import arc.graphics.gl.Shader;import arc.graphics.g3d.Camera3D;import arc.util.Time;import mindustry.Vars;import fpv.environment.LiquidBody;
/** Camera-lens immersion and a decaying exit film; no status/damage is applied here. */
final class LiquidImmersionEffect implements AutoCloseable {
 private final fpv.environment.LiquidExposureSystem exposure=new fpv.environment.LiquidExposureSystem(u->null);
 private final fpv.environment.LiquidExposureSystem.Sample heat=new fpv.environment.LiquidExposureSystem.Sample();
 private Shader shader;private float immersion,film;private int kind;private Object world;
 private static final String VERT="attribute vec4 a_position;attribute vec2 a_texCoord0;varying vec2 v_uv;void main(){v_uv=a_texCoord0;gl_Position=a_position;}";
 private static final String FRAG="uniform sampler2D u_texture;uniform vec3 u_tint;uniform float u_amount;uniform float u_film;uniform float u_kind;uniform float u_time;uniform float u_heat;varying vec2 v_uv;void main(){vec2 uv=v_uv;vec3 c=texture2D(u_texture,uv).rgb;c=mix(c,c*u_tint+u_tint*0.12,u_amount*0.65);if(u_film>0.001){vec2 g=uv*vec2(7.0,5.0)+vec2(0.0,u_time*0.10);vec2 id=floor(g);float h=fract(sin(dot(id,vec2(17.1,53.7)))*4375.5);vec2 q=fract(g)-vec2(0.15+0.7*h,h);float d=length(q*vec2(1.0,0.7));float rim=(1.0-smoothstep(0.04,0.065,abs(d-(0.08+0.09*h))))*step(0.72,h);float smear=u_kind>1.5&&u_kind<2.5?0.15*(1.0-uv.y):0.0;vec3 drop=texture2D(u_texture,uv+q*0.007*u_film).rgb;float fill=(1.0-smoothstep(0.07+0.09*h,0.10+0.09*h,d))*step(0.72,h);c=mix(c,drop,fill*u_film*0.65);c=mix(c,u_tint+vec3(0.2),clamp((rim*0.20+smear)*u_film,0.0,0.4));}c=mix(c,vec3(1.0,0.24,0.03),u_heat*0.12*smoothstep(0.1,0.7,length(uv-0.5)));gl_FragColor=vec4(c,1.0);}";
 Shader update(Camera3D camera){if(world!=Vars.world.tiles){world=Vars.world.tiles;immersion=film=0;kind=0;exposure.clear();}
  var t=Vars.world.tile((int)Math.floor(camera.position.x+.5),(int)Math.floor(-camera.position.z+.5));boolean inside=LiquidBody.open(t)&&camera.position.y<LiquidSurfaceVisual.height(camera.position.x,camera.position.z,Time.time/60f,arc.Core.settings.getBool(LiquidSurfaceVisual.WAVES_SETTING,true))&&camera.position.y>LiquidBody.bed(camera.position.x,camera.position.z)-.3;
  float dt=Math.max(0,Math.min(.1f,arc.Core.graphics.getDeltaTime()));
  if(inside){kind=LiquidBody.kind(t.floor());immersion=Math.min(1,immersion+dt*8);film=1;}else{immersion=Math.max(0,immersion-dt*3);film=Math.max(0,film-dt/(kind==2?7:kind==3?2.5f:4));}
  if(shader==null)shader=new Shader(VERT,FRAG);shader.bind();shader.setUniformi("u_texture",0);shader.setUniformf("u_amount",immersion);shader.setUniformf("u_film",inside?0:film);exposure.sample(camera.position.x,-camera.position.z,camera.position.y,0,0,heat);shader.setUniformf("u_heat",(float)heat.burning);shader.setUniformf("u_kind",kind);shader.setUniformf("u_time",Time.time/60f);
  if(kind==3)shader.setUniformf("u_tint",1f,.29f,.07f);else if(kind==2)shader.setUniformf("u_tint",.18f,.22f,.1f);else if(kind==4)shader.setUniformf("u_tint",.25f,.9f,1f);else shader.setUniformf("u_tint",.22f,.55f,.95f);return shader;
 }
 public void close(){if(shader!=null)shader.dispose();shader=null;}
}
