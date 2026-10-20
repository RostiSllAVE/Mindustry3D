package fpv.background;

import mindustry.Vars;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/** Stable local identity without file paths, account identifiers or changes to native save data. */
public final class BackgroundMapKey {
    private BackgroundMapKey(){}
    public static String current(){
        try{var hash=MessageDigest.getInstance("SHA-256");
            text(hash,"Mindustry3-background-v1");text(hash,Vars.world.width()+"x"+Vars.world.height());
            if(Vars.state.hasSector()){var sector=Vars.state.getSector();text(hash,"sector");text(hash,sector.planet.name);text(hash,Integer.toString(sector.id));}
            else{
                // Native SaveIO preserves map name but may discard author when the map file is absent.
                text(hash,"map");var map=Vars.state.map;text(hash,map==null?"":map.name());
                // Floors/overlays survive saves; buildings, units and wall destruction do not enter the key.
                // Bound work on huge maps. Identical named terrain clones intentionally share art.
                int width=Vars.world.width(),height=Vars.world.height(),nx=Math.min(256,width),ny=Math.min(256,height);
                String[] floors=new String[Vars.content.blocks().size],overlays=new String[floors.length];
                for(int y=0;y<ny;y++)for(int x=0;x<nx;x++){var tile=Vars.world.tile(x*width/nx,y*height/ny);if(tile==null){text(hash,"empty");continue;}int f=tile.floorID()&65535,o=tile.overlayID()&65535;
                    if(floors[f]==null)floors[f]=tile.floor().name;if(overlays[o]==null)overlays[o]=tile.overlay().name;text(hash,floors[f]);text(hash,overlays[o]);
                }
            }return HexFormat.of().formatHex(hash.digest());
        }catch(NoSuchAlgorithmException unavailable){throw new IllegalStateException("SHA-256 required by Java",unavailable);}
    }
    private static void text(MessageDigest hash,String value){hash.update(value.getBytes(StandardCharsets.UTF_8));hash.update((byte)0);}
}
