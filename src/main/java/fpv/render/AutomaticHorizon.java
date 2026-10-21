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
    public record Stats(long analyses,long builds,int vertices,long meshBytes,long fingerprint,double analysisMs,double generationMs,int islands,int outcrops,int structures,int compositionSamples){}
    private TerrainElevation terrain=new TerrainElevation();
    private final FloorSurfaceRelief relief=new FloorSurfaceRelief((int)fpv.generated.RenderConfig.CHUNK_TILES);
    private BorderBiomeProfile profile;
    private Object tiles,content,atlas;
    private long policy=Long.MIN_VALUE;
    private long styleRevision=Long.MIN_VALUE;
    private int strip,planet=-1;
    private boolean dirty=true;
    private Shader farShader,nearShader;
    private Mesh far;
    private HorizonCollarMesh collar;
    private Texture detailTexture;
    private HorizonComposition composition=HorizonComposition.EMPTY;
    private int islands,outcrops,structures;
    private long analyses,builds,meshBytes,fingerprint;
    private int vertices;
    private double analysisMs,generationMs;
    private final java.util.IdentityHashMap<arc.graphics.g2d.TextureRegion,float[]> palette=new java.util.IdentityHashMap<>();
    private Object paletteAtlas;
    private final float[] a=new float[HorizonGeometry.STRIDE],b=new float[HorizonGeometry.STRIDE],na=new float[HorizonGeometry.STRIDE],nb=new float[HorizonGeometry.STRIDE];
    private static final String COMMON="""
        uniform vec4 u_bounds;uniform vec3 u_camera;uniform float u_time,u_waves;
        float outside(vec3 p){return max(max(u_bounds.x-p.x,p.x-u_bounds.z),max(u_bounds.y-p.z,p.z-u_bounds.w));}
        float wave(vec3 p,float water,float depth){
            float d=outside(p),join=smoothstep(0.0,3.0,abs(d-12.0));
            float swell=(0.035+0.065*clamp(depth/12.0,0.0,1.0))*(0.65*sin(p.x*0.012+p.z*0.008+u_time*0.12)+0.35*sin(p.x*(-0.009)+p.z*0.016-u_time*0.08));
            float ripple=0.018*sin(p.x*0.73+p.z*0.41+u_time*0.7);
            return u_waves*water*smoothstep(0.0,0.4,depth)*smoothstep(0.0,8.0,d)*join*(swell+ripple);
        }
        """;
    private static final String LIGHT="""
        uniform sampler2D u_detail;uniform vec3 u_sky;uniform float u_ambient,u_hazeNear,u_hazeFar;
        vec2 projected(vec3 p,vec3 normal){vec3 n=abs(normal);if(n.y>=max(n.x,n.z))return p.xz;if(n.x>=n.z)return p.zy;return p.xy;}
        vec3 material(vec3 colour,vec3 pos,vec3 normal,vec2 weights,float water){
            vec4 pattern=texture2D(u_detail,pos.xz+vec2(0.5))+vec4(0.5);
            float steep=1.0-smoothstep(0.45,0.78,abs(normal.y));
            if(steep>0.001)pattern=mix(pattern,texture2D(u_detail,projected(pos,normal)+vec2(0.5))+vec4(0.5),steep);
            float factor=mix(dot(pattern.rgb,vec3(weights.x,max(0.0,1.0-weights.x-weights.y),weights.y)),1.0+0.06*sin(pos.x*0.06+pos.z*0.04-u_time*0.1),water);
            return colour*factor;
        }
        vec3 lit(vec3 c,vec3 pos,vec3 normal,float water,float depth,float coast){
            float deep=smoothstep(0.8,13.0,depth);
            if(water>0.5){
                c*=mix(vec3(1.0),vec3(0.25,0.42,0.65),deep);
                float foam=(1.0-smoothstep(0.02,0.35,coast))*(1.0-smoothstep(0.5,1.4,depth));
                c+=vec3(0.13,0.17,0.18)*foam*pow(max(0.0,sin(outside(pos)*1.6-u_time*0.65)),5.0)*u_waves;
                c+=vec3(0.025,0.035,0.045)*pow(max(0.0,sin(pos.x*0.025+pos.z*0.018+u_time*0.11)),8.0)*(1.0-deep*0.6);
            }else{float shade=0.78+0.22*max(0.0,dot(normal,normalize(vec3(-0.3,1.0,0.4))));c*=shade;}
            c*=u_ambient;
            if(u_camera.y<(-0.17)&&pos.y<(-0.17)&&water<0.5){float attenuation=1.0-exp(-length(pos-u_camera)*(0.025+0.005*depth));c=mix(c,vec3(0.025,0.075,0.14)*u_ambient,attenuation);}
            float haze=smoothstep(u_hazeNear,u_hazeFar,outside(pos));
            return mix(c,u_sky,haze);
        }
        """;
    private static final String FAR_VERTEX="attribute vec3 a_position;attribute vec4 a_color;attribute vec2 a_meta,a_material;attribute vec3 a_normal;attribute float a_coast;uniform mat4 u_combined;varying vec4 v_color;varying vec3 v_pos,v_normal;varying vec2 v_meta,v_material;varying float v_coast;"+COMMON+"void main(){v_color=a_color;v_normal=a_normal;v_meta=a_meta;v_material=a_material;v_coast=a_coast;v_pos=a_position;v_pos.y+=wave(v_pos,a_meta.x,a_meta.y);gl_Position=u_combined*vec4(v_pos,1.0);}";
    private static final String FAR_FRAGMENT="varying vec4 v_color;varying vec3 v_pos,v_normal;varying vec2 v_meta,v_material;varying float v_coast;"+COMMON+LIGHT+CosmicField.GLSL+"void main(){float alpha=1.0-v_color.a;float terrain=step(0.1,length(v_normal));vec3 n=normalize(v_normal+vec3(0.0,0.0001,0.0));vec3 c=mix(v_color.rgb,material(v_color.rgb,v_pos,n,v_material,v_meta.x),terrain);c=lit(c,v_pos,n,v_meta.x,v_meta.y,v_coast);if(v_color.a>0.001){c=mix(c,cosmicField(normalize(v_pos-u_camera+u_camera/100000.0)),clamp(v_color.a,0.0,1.0));alpha=1.0;}if(v_meta.x>0.5)alpha*=u_camera.y<(-0.17)?0.94:mix(0.88,0.98,smoothstep(0.1,3.5,v_meta.y));gl_FragColor=vec4(c,alpha);}";
    private static final String NEAR_VERTEX="attribute vec3 a_position;attribute vec4 a_color,a_rect;attribute vec2 a_material;attribute vec3 a_normal;attribute float a_depth;uniform mat4 u_combined;varying vec4 v_color,v_rect;varying vec3 v_pos,v_normal;varying vec2 v_material;varying float v_depth;"+COMMON+"void main(){v_color=a_color;v_rect=a_rect;v_normal=a_normal;v_material=a_material;v_depth=a_depth;v_pos=a_position;v_pos.y+=wave(v_pos,a_color.a,a_depth);gl_Position=u_combined*vec4(v_pos,1.0);}";
    // The map apron is not a shoreline. Only the clipped far coast supplies foam eligibility.
    private static final String NEAR_FRAGMENT="uniform sampler2D u_texture;varying vec4 v_color,v_rect;varying vec3 v_pos,v_normal;varying vec2 v_material;varying float v_depth;"+COMMON+LIGHT+"void main(){vec3 n=normalize(v_normal+vec3(0.0,0.0001,0.0));vec2 uv=mix(v_rect.xy,v_rect.zw,fract(projected(v_pos,n)+vec2(0.5)));vec4 t=texture2D(u_texture,uv);if(t.a<0.02)discard;float blend=smoothstep(0.0,4.0,outside(v_pos));vec3 c=mix(t.rgb,material(v_color.rgb,v_pos,n,v_material,v_color.a),blend);c=lit(c,v_pos,n,v_color.a,v_depth,1.0);float alpha=v_color.a>0.5?mix(mix(1.0,0.88,blend),0.98,smoothstep(0.1,3.5,v_depth)):1.0;gl_FragColor=vec4(c,alpha);}";
    public Stats stats(){return new Stats(analyses,builds,vertices,meshBytes,fingerprint,analysisMs,generationMs,islands,outcrops,structures,composition.sampledCells());}
    public BorderBiomeProfile profile(){return profile;}
    public HorizonComposition composition(){return composition;}
    public void setTerrain(TerrainElevation value){terrain=value;invalidate();}
    public void invalidate(){clear();dirty=true;}
    public void invalidate(Tile tile){if(tile==null){invalidate();return;}int reach=Math.max(2,Math.min(16,Core.settings.getInt(STRIP_SETTING,8)))+3;
        if(tile.x<reach||tile.y<reach||tile.x>=Vars.world.width()-reach||tile.y>=Vars.world.height()-reach)dirty=true;
    }
    /** No scanning, sampling or mesh generation on cache hits. Policy parsing itself is cached. */
    public void ensure(){
        int wanted=Math.max(2,Math.min(16,Core.settings.getInt(STRIP_SETTING,8)));int p=Vars.state.getPlanet()==null?-1:Vars.state.getPlanet().id;
        long current=terrain.classification().revision();
        long style=fpv.background.BackgroundProfiles.revision();
        if(!dirty&&tiles==Vars.world.tiles&&content==Vars.content&&atlas==Core.atlas&&strip==wanted&&policy==current&&planet==p&&styleRevision==style)return;
        clear();terrain.ensureWorld();tiles=Vars.world.tiles;content=Vars.content;atlas=Core.atlas;strip=wanted;policy=current;planet=p;
        if(paletteAtlas!=Core.atlas){palette.clear();paletteAtlas=Core.atlas;if(detailTexture!=null)detailTexture.dispose();detailTexture=null;}
        styleRevision=style;
        long started=System.nanoTime();profile=fpv.background.BackgroundProfiles.current().profile(BorderBiomeProfile.analyze(new BorderBiomeProfile.Source(){public int width(){return Vars.world.width();}public int height(){return Vars.world.height();}public BorderBiomeProfile.Cell cell(int x,int y){return coloured(Vars.world.tile(x,y));}},strip));composition=HorizonComposition.analyze(terrain);analysisMs=(System.nanoTime()-started)/1e6;analyses++;
        started=System.nanoTime();HorizonGeometry geometry=HorizonGeometry.build(profile,this::boundaryHeight,composition,this::waterBedHeight,fpv.background.BackgroundProfiles.current());
        if(geometry.vertices.length>0){far=new Mesh(true,geometry.vertices.length/HorizonGeometry.STRIDE,0,VertexAttribute.position3,new VertexAttribute(4,"a_color"),new VertexAttribute(2,"a_meta"),new VertexAttribute(2,"a_material"),new VertexAttribute(3,"a_normal"),new VertexAttribute(1,"a_coast"));far.setVertices(geometry.vertices);}
        var batch=new HorizonCollarMesh.Builder();
        for(int i=0;i<geometry.seamPositions.length-1;i++){
            double s=geometry.seamPositions[i],end=geometry.seamPositions[i+1];Tile tile=edgeTile((s+end)*.5);if(tile==null||tile.floor()==Blocks.space||tile.floor()==Blocks.empty)continue;
            var region=terrain.naturalWall(tile)?NativeRegions.wall(tile.block(),tile):NativeRegions.floor(tile.floor(),tile);
            if(!NativeAtlasMesh.valid(region))region=Core.atlas.white();
            var pa=HorizonGeometry.point(profile,s,0);var pb=HorizonGeometry.point(profile,end,0);geometry.row(s,0,a);geometry.row(end,0,b);
            if(a[6]>.995f&&b[6]>.995f)continue;
            float ha=edgeHeight(tile,s),hb=edgeHeight(tile,end);boolean water=LiquidSurfaceVisual.open(tile)&&tile.floor().liquidDrop==mindustry.content.Liquids.water;
            // Split the native tile's collar at every fractional outer-row knot, too.
            // All three meshes share identical d4/d12 edge polylines rather than T-junctions.
            System.arraycopy(a,0,na,0,HorizonGeometry.STRIDE);System.arraycopy(b,0,nb,0,HorizonGeometry.STRIDE);na[0]=pa.x();na[1]=ha;na[2]=pa.z();nb[0]=pb.x();nb[1]=hb;nb[2]=pb.z();na[7]=nb[7]=0;na[8]=nb[8]=0;na[14]=nb[14]=water?1:-1;
            coastalCollar(batch,region,new float[][]{na,nb,b,a},geometry,s,end,tile,water);
            // A geological/open step is an intentional native cliff; close the external wedge only.
            if(s==Math.floor(s)){Tile previous=edgeTile(s-.5);float old=edgeHeight(previous,s);
                if(Math.abs(old-ha)>.0001f){float[] step=na.clone();step[1]=old;batch.face(region,step,na,a,a);}}
        }
        collar=batch.upload();vertices=geometry.vertices.length/HorizonGeometry.STRIDE+(int)(collar.bytes/(HorizonCollarMesh.STRIDE*4));meshBytes=(long)geometry.vertices.length*4+collar.bytes;fingerprint=geometry.fingerprint();builds++;
        islands=geometry.islandCount;outcrops=geometry.outcropCount;structures=geometry.structureCount;generationMs=(System.nanoTime()-started)/1e6;dirty=false;
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
            for(int y=0;y<32;y++)for(int x=0;x<32;x++){int value=pix.get(Math.min(pix.width-1,x*pix.width/32),Math.min(pix.height-1,y*pix.height/32));if((value&255)<200)continue;r+=(value>>>24&255)/255d;g+=(value>>>16&255)/255d;b+=(value>>>8&255)/255d;n++;}
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
        double start=Math.floor(s);float a=edgeVertexHeight(tile,start);float t=(float)(s-start);
        return t==0?a:a+(edgeVertexHeight(tile,start+1)-a)*t;
    }
    private float edgeVertexHeight(Tile tile,double s){
        if(tile==null)return 0;
        var point=HorizonGeometry.point(profile,s,0);int cx=Math.round(point.x()+.5f),cy=Math.round(-point.z()+.5f);
        if(terrain.naturalWall(tile))return terrain.vertexHeight(cx,cy);
        if(tile.floor().isLiquid)return LiquidBedVisual.corner(cx,cy);
        return relief.sample(cx,cy,t->true);
    }
    private double boundaryHeight(double s){double start=Math.floor(s),t=s-start,a=boundaryVertexHeight(start);return t==0?a:a+(boundaryVertexHeight(start+1)-a)*t;}
    private float boundaryVertexHeight(double s){return Math.max(edgeVertexHeight(edgeTile(s-.5),s),edgeVertexHeight(edgeTile(s+.5),s));}
    private double waterBedHeight(double s){
        Tile tile=edgeTile(s+.00001);if(!LiquidSurfaceVisual.open(tile)||tile.floor().liquidDrop!=mindustry.content.Liquids.water)return Double.NaN;
        return edgeHeight(tile,s);
    }
    private void coastalCollar(HorizonCollarMesh.Builder batch,arc.graphics.g2d.TextureRegion region,float[][] points,HorizonGeometry geometry,double start,double end,Tile tile,boolean nativeWater){
        for(int[] index:new int[][]{{0,1,2},{0,2,3}})for(boolean wet:new boolean[]{false,true}){
            float[][] triangle={points[index[0]],points[index[1]],points[index[2]]};var polygon=clipCollar(triangle,wet);
            for(int i=1;i+1<polygon.size();i++)batch.triangle(region,polygon.get(0),polygon.get(i),polygon.get(i+1));
            if(!wet)continue;
            var surface=new HorizonLandscape.Surface();
            for(int j=0;j<polygon.size();j++){float[] vertex=polygon.get(j);
                boolean first=Math.abs(vertex[0]-points[0][0])<.00001&&Math.abs(vertex[2]-points[0][2])<.00001;
                boolean second=Math.abs(vertex[0]-points[1][0])<.00001&&Math.abs(vertex[2]-points[1][2])<.00001;
                vertex=vertex.clone();
                if(nativeWater&&(first||second)){double along=first?start:end;var p=HorizonGeometry.point(profile,along,0);vertex[1]=LiquidSurfaceVisual.corner(Math.round(p.x()+.5f),Math.round(-p.z()+.5f));vertex[3]=tile.floor().mapColor.r;vertex[4]=tile.floor().mapColor.g;vertex[5]=tile.floor().mapColor.b;}
                else{vertex[1]=HorizonLandscape.SEA;geometry.landscape.sample((start+end)*.5,4,vertex[0],vertex[2],surface);vertex[3]=surface.waterR;vertex[4]=surface.waterG;vertex[5]=surface.waterB;}
                vertex[7]=1;polygon.set(j,vertex);
            }
            for(int i=1;i+1<polygon.size();i++)batch.triangle(region,polygon.get(0),polygon.get(i),polygon.get(i+1));
        }
    }
    private java.util.ArrayList<float[]> clipCollar(float[][] triangle,boolean wet){
        var polygon=new java.util.ArrayList<float[]>(4);float[] previous=triangle[triangle.length-1];boolean old=wet?previous[14]>=0:previous[14]<=0;
        for(float[] v:triangle){boolean in=wet?v[14]>=0:v[14]<=0;if(in!=old){float t=previous[14]/(previous[14]-v[14]);float[] join=new float[HorizonGeometry.STRIDE];for(int i=0;i<join.length;i++)join[i]=previous[i]+(v[i]-previous[i])*t;join[1]=HorizonLandscape.SEA;join[8]=join[14]=0;polygon.add(join);}if(in)polygon.add(v);previous=v;old=in;}
        return polygon;
    }
    public void render(Camera3D camera,float ambient,Color sky,double seconds){
        ensure();if(detailTexture==null)detailTexture=HorizonDetailTexture.create();
        if(farShader==null){farShader=new Shader(FAR_VERTEX,FAR_FRAGMENT);nearShader=new Shader(NEAR_VERTEX,NEAR_FRAGMENT);}
        detailTexture.bind(1);
        apply(farShader,camera,ambient,sky,seconds);Gl.enable(Gl.blend);Gl.blendFunc(Gl.srcAlpha,Gl.oneMinusSrcAlpha);
        if(far!=null)far.render(farShader,Gl.triangles);
        apply(nearShader,camera,ambient,sky,seconds);nearShader.setUniformi("u_texture",0);collar.draw(nearShader);Gl.disable(Gl.blend);Gl.activeTexture(Gl.texture0);
    }
    private void apply(Shader shader,Camera3D camera,float ambient,Color sky,double seconds){shader.bind();shader.setUniformMatrix4("u_combined",camera.combined.val);shader.setUniformf("u_bounds",-.5f,-profile.height+.5f,profile.width-.5f,.5f);shader.setUniformf("u_camera",camera.position);shader.setUniformf("u_time",(float)(seconds%100000));shader.setUniformf("u_ambient",ambient);shader.setUniformf("u_sky",sky.r,sky.g,sky.b);shader.setUniformi("u_detail",1);shader.setUniformf("u_waves",Core.settings.getBool(LiquidSurfaceVisual.WAVES_SETTING,true)?1f:0f);shader.setUniformf("u_hazeNear",(float)(HorizonGeometry.outerDistance(profile.width,profile.height)*.72));shader.setUniformf("u_hazeFar",(float)(HorizonGeometry.outerDistance(profile.width,profile.height)*.92));}
    public void clear(){if(far!=null)far.dispose();if(collar!=null)collar.dispose();far=null;collar=null;profile=null;tiles=content=atlas=null;vertices=0;meshBytes=0;islands=outcrops=structures=0;composition=HorizonComposition.EMPTY;dirty=true;}
    public void dispose(){clear();palette.clear();paletteAtlas=null;if(detailTexture!=null)detailTexture.dispose();detailTexture=null;if(farShader!=null)farShader.dispose();if(nearShader!=null)nearShader.dispose();farShader=nearShader=null;}
}
