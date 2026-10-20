package fpv.render;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g3d.Camera3D;
import arc.graphics.gl.Shader;
import fpv.terrain.TerrainElevation;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Tile;

/** The automatic visual background owns meshes only; it never creates gameplay tiles/entities. */
public final class AutomaticHorizon {
    public static final String SETTING="fpv-auto-horizon",STRIP_SETTING="fpv-horizon-strip";
    public record Stats(long analyses,long builds,int vertices,long meshBytes,long fingerprint,double analysisMs,double generationMs){}
    private TerrainElevation terrain=new TerrainElevation();
    private final FloorSurfaceRelief relief=new FloorSurfaceRelief((int)fpv.generated.RenderConfig.CHUNK_TILES);
    private BorderBiomeProfile profile;
    private Object tiles,content,atlas;
    private long policy=Long.MIN_VALUE;
    private int strip,planet=-1;
    private boolean dirty=true;
    private Shader farShader,nearShader;
    private Mesh far;
    private NativeAtlasMesh collar;
    private long analyses,builds,meshBytes,fingerprint;
    private int vertices;
    private double analysisMs,generationMs;
    private final java.util.IdentityHashMap<arc.graphics.g2d.TextureRegion,float[]> palette=new java.util.IdentityHashMap<>();
    private Object paletteAtlas;
    private final float[] a=new float[9],b=new float[9],na=new float[9],nb=new float[9];
    private static final String COMMON="uniform vec4 u_bounds;uniform vec3 u_camera;uniform float u_time;float outside(vec3 p){return max(max(u_bounds.x-p.x,p.x-u_bounds.z),max(u_bounds.y-p.z,p.z-u_bounds.w));}float wave(vec3 p,float water){return 0.025*water*smoothstep(0.0,4.0,outside(p))*(0.65*sin(p.x*0.73+p.z*0.41+u_time*0.9)+0.35*sin(p.x*(-0.37)+p.z*0.83-u_time*0.63));}";
    private static final String LIGHT="uniform vec3 u_sky;uniform float u_ambient,u_hazeNear,u_hazeFar;float hash(vec2 p){vec3 q=fract(vec3(p.xyx)*0.1031);q+=dot(q,q.yzx+33.33);return fract((q.x+q.y)*q.z);}float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);}vec3 lit(vec3 c,vec3 pos,float water){float haze=smoothstep(u_hazeNear,u_hazeFar,outside(pos));float detail=1.0-smoothstep(80.0,260.0,length(pos-u_camera));float transition=smoothstep(0.0,4.0,outside(pos));c*=u_ambient*mix(1.0,0.94+0.12*noise(pos.xz*0.035),transition);c+=vec3(0.06,0.07,0.08)*water*(0.5+0.5*sin(pos.x*1.7+pos.z*1.3+u_time))*(1.0-haze)*detail*transition;return mix(c,u_sky,haze);}";
    private static final String FAR_VERTEX="attribute vec3 a_position;attribute vec4 a_color;attribute vec2 a_meta;uniform mat4 u_combined;varying vec4 v_color;varying vec3 v_pos;varying float v_water;"+COMMON+"void main(){v_color=a_color;v_water=a_meta.x;v_pos=a_position;v_pos.y+=wave(v_pos,v_water*a_meta.y);gl_Position=u_combined*vec4(v_pos,1.0);}";
    private static final String FAR_FRAGMENT="varying vec4 v_color;varying vec3 v_pos;varying float v_water;"+COMMON+LIGHT+"void main(){float alpha=1.0-v_color.a;if(alpha<0.01)discard;gl_FragColor=vec4(lit(v_color.rgb,v_pos,v_water),alpha);}";
    private static final String NEAR_VERTEX="attribute vec3 a_position;attribute vec4 a_color;attribute vec2 a_texCoord0;uniform mat4 u_combined;varying vec4 v_color;varying vec3 v_pos;varying vec2 v_uv;"+COMMON+"void main(){v_color=a_color;v_uv=a_texCoord0;v_pos=a_position;v_pos.y+=wave(v_pos,a_color.a);gl_Position=u_combined*vec4(v_pos,1.0);}";
    private static final String NEAR_FRAGMENT="uniform sampler2D u_texture;varying vec4 v_color;varying vec3 v_pos;varying vec2 v_uv;"+COMMON+LIGHT+"void main(){vec4 t=texture2D(u_texture,v_uv);if(t.a<0.02)discard;float blend=smoothstep(0.0,4.0,outside(v_pos));vec3 source=mix(t.rgb,v_color.rgb,v_color.a*0.4*(1.0-blend));vec3 c=mix(source,v_color.rgb,blend);gl_FragColor=vec4(lit(c,v_pos,v_color.a),1.0);}";
    public Stats stats(){return new Stats(analyses,builds,vertices,meshBytes,fingerprint,analysisMs,generationMs);}
    public BorderBiomeProfile profile(){return profile;}
    public void setTerrain(TerrainElevation value){terrain=value;invalidate();}
    public void invalidate(){clear();dirty=true;}
    public void invalidate(Tile tile){if(tile==null){invalidate();return;}int reach=Math.max(2,Math.min(16,Core.settings.getInt(STRIP_SETTING,8)))+3;
        if(tile.x<reach||tile.y<reach||tile.x>=Vars.world.width()-reach||tile.y>=Vars.world.height()-reach)dirty=true;
    }
    /** No scanning, sampling or mesh generation on cache hits. Policy parsing itself is cached. */
    public void ensure(){
        int wanted=Math.max(2,Math.min(16,Core.settings.getInt(STRIP_SETTING,8)));int p=Vars.state.getPlanet()==null?-1:Vars.state.getPlanet().id;
        long current=terrain.classification().revision();
        if(!dirty&&tiles==Vars.world.tiles&&content==Vars.content&&atlas==Core.atlas&&strip==wanted&&policy==current&&planet==p)return;
        clear();terrain.ensureWorld();tiles=Vars.world.tiles;content=Vars.content;atlas=Core.atlas;strip=wanted;policy=current;planet=p;
        if(paletteAtlas!=Core.atlas){palette.clear();paletteAtlas=Core.atlas;}
        long started=System.nanoTime();profile=BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){public int width(){return Vars.world.width();}public int height(){return Vars.world.height();}public BorderBiomeProfile.Cell cell(int x,int y){return coloured(Vars.world.tile(x,y));}},strip);analysisMs=(System.nanoTime()-started)/1e6;analyses++;
        started=System.nanoTime();HorizonGeometry geometry=HorizonGeometry.build(profile,this::boundaryHeight);
        if(geometry.vertices.length>0){far=new Mesh(true,geometry.vertices.length/9,0,VertexAttribute.position3,new VertexAttribute(4,"a_color"),new VertexAttribute(2,"a_meta"));far.setVertices(geometry.vertices);}
        var batch=new NativeAtlasMesh.Builder(false);
        for(int i=0;i<(int)profile.perimeter;i++){
            double s=i,end=i+1;Tile tile=edgeTile(s+.5);if(tile==null||tile.floor()==Blocks.space||tile.floor()==Blocks.empty)continue;
            var region=terrain.naturalWall(tile)?NativeRegions.wall(tile.block(),tile):NativeRegions.floor(tile.floor(),tile);
            if(!NativeAtlasMesh.valid(region))region=Core.atlas.white();
            var pa=HorizonGeometry.point(profile,s,0);var pb=HorizonGeometry.point(profile,end,0);geometry.row(s,0,a);geometry.row(end,0,b);
            float ha=edgeHeight(tile,s),hb=edgeHeight(tile,end);boolean water=tile.floor().isLiquid&&tile.floor().liquidDrop==mindustry.content.Liquids.water;
            // One texture-to-colour transition per actual boundary tile. No rectangle-side skirts below it.
            System.arraycopy(a,0,na,0,9);System.arraycopy(b,0,nb,0,9);na[0]=pa.x();na[1]=ha;na[2]=pa.z();nb[0]=pb.x();nb[1]=hb;nb[2]=pb.z();na[6]=nb[6]=water?1:0;a[6]=a[7];b[6]=b[7];
            if(water){na[3]=nb[3]=tile.floor().mapColor.r;na[4]=nb[4]=tile.floor().mapColor.g;na[5]=nb[5]=tile.floor().mapColor.b;}
            na[7]=Math.max(0,Math.min(1,pa.x()-tile.x+.5f));na[8]=Math.max(0,Math.min(1,pa.z()+tile.y+.5f));nb[7]=Math.max(0,Math.min(1,pb.x()-tile.x+.5f));nb[8]=Math.max(0,Math.min(1,pb.z()+tile.y+.5f));
            a[7]=na[7];a[8]=na[8];b[7]=nb[7];b[8]=nb[8];if(Math.abs(pa.z()-pb.z())<.001f){a[8]=1-na[8];b[8]=1-nb[8];}else{a[7]=1-na[7];b[7]=1-nb[7];}
            batch.horizonFace(region,na,nb,b,a);
            // A geological/open step is an intentional native cliff; close the external wedge only.
            Tile previous=edgeTile(s-.5);float old=edgeHeight(previous,s);
            if(Math.abs(old-ha)>.0001f)batch.projectionFace(region,pa.x(),old,pa.z(),pa.x(),ha,pa.z(),a[0],a[1],a[2],a[0],a[1],a[2],a[3],a[4],a[5],a[6]);
        }
        collar=batch.upload();batch.dispose();vertices=geometry.vertices.length/9+(int)(collar.bytes/36);meshBytes=(long)geometry.vertices.length*4+collar.bytes;fingerprint=geometry.fingerprint();builds++;
        generationMs=(System.nanoTime()-started)/1e6;dirty=false;
    }
    private BorderBiomeProfile.Cell coloured(Tile tile){
        var c=BorderBiomeProfile.nativeCell(tile,terrain);if(c==null)return null;
        var region=c.mountain()?NativeRegions.wall(tile.block(),tile):NativeRegions.floor(tile.floor(),tile);float[] rgb=mean(region);float r=c.r(),g=c.g(),b=c.b();
        if(rgb!=null){if(c.water()){r=.6f*rgb[0]+.4f*r+.02f;g=.6f*rgb[1]+.4f*g+.02f;b=.6f*rgb[2]+.4f*b+.02f;}else{r=rgb[0];g=rgb[1];b=rgb[2];}}
        float[] tree=c.vegetation()>0?mean(NativeRegions.wall(tile.block(),tile)):null;
        return new BorderBiomeProfile.Cell(c.material(),r,g,b,c.mountain(),c.water(),c.vegetation(),tree==null?c.tr():tree[0],tree==null?c.tg():tree[1],tree==null?c.tb():tree[2],c.form());
    }
    /** Bounded host-atlas sampling once per shared region; never reads a GPU buffer or external asset. */
    private float[] mean(arc.graphics.g2d.TextureRegion region){
        if(region==null)return null;if(palette.containsKey(region))return palette.get(region);float[] result=null;
        if(region instanceof arc.graphics.g2d.TextureAtlas.AtlasRegion ar)try{
            var pix=Core.atlas.getPixmap(ar);double r=0,g=0,b=0;int n=0;
            for(int y=0;y<8;y++)for(int x=0;x<8;x++){int value=pix.get(Math.min(pix.width-1,x*pix.width/8),Math.min(pix.height-1,y*pix.height/8));if((value&255)<200)continue;r+=(value>>>24&255)/255d;g+=(value>>>16&255)/255d;b+=(value>>>8&255)/255d;n++;}
            if(n>0)result=new float[]{(float)(r/n),(float)(g/n),(float)(b/n)};
        }catch(RuntimeException unavailable){/* Native content colour is the safe fallback. */}
        palette.put(region,result);return result;
    }
    private Tile edgeTile(double position){double s=position-Math.floor(position/profile.perimeter)*profile.perimeter;
        if(s<profile.width)return Vars.world.tile((int)s,0);s-=profile.width;
        if(s<profile.height)return Vars.world.tile(profile.width-1,(int)s);s-=profile.height;
        if(s<profile.width)return Vars.world.tile(profile.width-1-(int)s,profile.height-1);s-=profile.width;
        return Vars.world.tile(0,profile.height-1-(int)s);
    }
    private float edgeHeight(Tile tile,double s){
        if(tile==null)return 0;
        var point=HorizonGeometry.point(profile,s,0);int cx=Math.round(point.x()+.5f),cy=Math.round(-point.z()+.5f);
        if(terrain.naturalWall(tile))return terrain.vertexHeight(cx,cy);
        if(tile.floor().isLiquid)return LiquidSurfaceVisual.corner(cx,cy);
        return relief.sample(cx,cy,t->true);
    }
    private double boundaryHeight(double s){return Math.max(edgeHeight(edgeTile(s-.5),s),edgeHeight(edgeTile(s+.5),s));}
    public void render(Camera3D camera,float ambient,Color sky,double seconds){
        ensure();if(farShader==null){farShader=new Shader(FAR_VERTEX,FAR_FRAGMENT);nearShader=new Shader(NEAR_VERTEX,NEAR_FRAGMENT);}
        apply(farShader,camera,ambient,sky,seconds);Gl.enable(Gl.blend);Gl.blendFunc(Gl.srcAlpha,Gl.oneMinusSrcAlpha);
        if(far!=null)far.render(farShader,Gl.triangles);Gl.disable(Gl.blend);
        apply(nearShader,camera,ambient,sky,seconds);nearShader.setUniformi("u_texture",0);collar.draw(nearShader);
    }
    private void apply(Shader shader,Camera3D camera,float ambient,Color sky,double seconds){shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_bounds",-.5f,-profile.height+.5f,profile.width-.5f,.5f);shader.setUniformf("u_camera",camera.position);shader.setUniformf("u_time",(float)(seconds%100000));shader.setUniformf("u_ambient",ambient);shader.setUniformf("u_sky",sky.r,sky.g,sky.b);shader.setUniformf("u_hazeNear",(float)Math.max(96,Math.hypot(profile.width,profile.height)*.24));shader.setUniformf("u_hazeFar",(float)(HorizonGeometry.outerDistance(profile.width,profile.height)*.92));}
    public void clear(){if(far!=null)far.dispose();if(collar!=null)collar.dispose();far=null;collar=null;profile=null;tiles=content=atlas=null;vertices=0;meshBytes=0;dirty=true;}
    public void dispose(){clear();palette.clear();paletteAtlas=null;if(farShader!=null)farShader.dispose();if(nearShader!=null)nearShader.dispose();farShader=nearShader=null;}
}
