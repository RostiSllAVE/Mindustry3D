package fpv.render;

/** Shared, direction-based cosmic material: no textures, time or camera-facing star sprites. */
final class CosmicField {
    static final String GLSL = """
        float cosmicHash(vec3 p){return fract(sin(dot(p,vec3(127.1,311.7,74.7)))*43758.5453);}
        vec3 cosmicField(vec3 d){
            // Cube-direction mapping avoids the singularity of longitude/latitude at vertical views.
            vec3 a=abs(d);vec2 q;float face;
            if(a.x>=a.y&&a.x>=a.z){q=d.yz/a.x;face=sign(d.x);}
            else if(a.y>=a.z){q=d.xz/a.y;face=2.0*sign(d.y);}
            else{q=d.xy/a.z;face=3.0*sign(d.z);}
            vec2 st=q*95.0,cell=floor(st),p=fract(st);
            float seed=cosmicHash(vec3(cell,face));
            vec2 center=vec2(.2)+.6*vec2(cosmicHash(vec3(cell+7.0,face)),cosmicHash(vec3(cell+19.0,face)));
            float radius=mix(.055,.19,cosmicHash(vec3(cell+31.0,face)));
            float star=step(.987,seed)*(1.0-smoothstep(radius*.15,radius,length(p-center)));
            vec3 tint=mix(vec3(.60,.76,1.0),vec3(1.0,.82,.60),cosmicHash(vec3(cell+9.0,face)));
            // Continuous large-scale directional clouds, including across cube-face boundaries.
            float band=pow(max(0.0,1.0-abs(dot(d,normalize(vec3(.25,.8,-.47))))),12.0);
            float cloud=.5+.25*sin(dot(d,vec3(17.0,11.0,23.0)))+.25*sin(dot(d,vec3(41.0,-27.0,13.0)));
            return vec3(.003,.005,.014)+band*cloud*vec3(.040,.021,.075)+tint*star*mix(.3,1.0,seed);
        }
        """;
    static final String VERTEX="attribute vec3 a_position;uniform mat4 u_combined;varying vec3 v_world;void main(){v_world=a_position;gl_Position=u_combined*vec4(a_position,1.0);}";
    static final String FRAGMENT="varying vec3 v_world;uniform vec3 u_camera;"+GLSL+"void main(){vec3 ray=normalize(v_world-u_camera);vec3 distant=normalize(ray+u_camera/100000.0);gl_FragColor=vec4(cosmicField(distant),1.0);}";
    private CosmicField(){}
}
