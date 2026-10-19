package fpv.terrain;

import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.util.Log;
import arc.util.serialization.Jval;
import fpv.world.NaturalProps;
import java.util.IdentityHashMap;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.*;

/** Shared CPU-only content policy. Native rules tags supply saved/server-owned map-author overrides. */
public final class TerrainClassification {
    public enum Kind { NATURAL, ARTIFICIAL, DECORATION, OPEN }
    public static final String TAG="fpv-terrain-classification";
    public static final int MAX_CHARACTERS=65536,MAX_OVERRIDES=4096;
    private Object content;
    private String raw;
    private long revision;
    private int parses;
    private String status="Content defaults";
    private final IdentityHashMap<Block,Kind> defaults=new IdentityHashMap<>();
    private IdentityHashMap<Block,Kind> blocks=new IdentityHashMap<>();
    private IntMap<Kind> tiles=new IntMap<>();

    public long revision(){refresh();return revision;}
    public int parses(){return parses;}
    public String status(){refresh();return status;}
    public Kind kind(Tile tile){refresh();return current(tile);}

    /** No provenance is inferred from team, map coordinates, wall density or block-name substrings. */
    Kind current(Tile tile){
        if(tile==null)return Kind.OPEN;
        Block b=tile.block();
        if(tile.build!=null)return Kind.ARTIFICIAL;
        Kind base=defaults.computeIfAbsent(b,TerrainClassification::contentDefault);
        // Metadata can reclassify static environmental material, never turn a live/buildable building into terrain.
        if(!eligible(b))return base;
        Kind override=tiles.get(tile.pos());
        return override!=null?override:blocks.getOrDefault(b,base);
    }
    private static boolean eligible(Block b){
        return b!=null&&b!=Blocks.air&&b.solid&&!b.hasBuilding()&&!b.synthetic()&&b.requirements.length==0
            &&!(b instanceof mindustry.world.blocks.defense.Wall)&&!(b instanceof StaticTree)
            &&!NaturalProps.supported(b)&&(!(b instanceof Prop)||b instanceof StaticWall);
    }
    private static Kind contentDefault(Block b){
        if(b==null||b==Blocks.air||b instanceof Floor)return Kind.OPEN;
        if(b.hasBuilding()||b.synthetic()||b.requirements.length>0||b instanceof mindustry.world.blocks.defense.Wall)return Kind.ARTIFICIAL;
        if(NaturalProps.supported(b)||b instanceof StaticTree||b instanceof Prop&&!(b instanceof StaticWall))return Kind.DECORATION;
        // Native StaticWall also contains industrial/editor walls. Its class alone cannot identify geology.
        // These exact content identities have no native geological/material flag; authors can override them.
        if(b instanceof ColoredWall||b==Blocks.darkMetal||b==Blocks.metalWall1||b==Blocks.metalWall2||b==Blocks.metalWall3)return Kind.ARTIFICIAL;
        return b instanceof StaticWall&&eligible(b)?Kind.NATURAL:Kind.ARTIFICIAL;
    }
    void refresh(){
        String value=Vars.state==null||Vars.state.rules==null||Vars.state.rules.tags==null?"":Vars.state.rules.tags.get(TAG,"");
        if(value==null)value="";
        if(content==Vars.content&&value.equals(raw))return;
        if(content!=Vars.content){content=Vars.content;defaults.clear();}
        raw=value;revision++;blocks=new IdentityHashMap<>();tiles=new IntMap<>();status="Content defaults";
        if(value.isBlank())return;
        parses++;
        try{
            if(value.length()>MAX_CHARACTERS)throw new IllegalArgumentException("Override text exceeds limit");
            Jval root=Jval.read(value);if(!root.isObject()||root.get("version")!=null&&root.get("version").asDouble()!=1)throw new IllegalArgumentException("Expected classification object version 1");
            for(var e:root.asObject())if(!e.key.equals("version")&&!e.key.equals("blocks")&&!e.key.equals("tiles"))throw new IllegalArgumentException("Unknown classification field");
            var nextBlocks=new IdentityHashMap<Block,Kind>();var nextTiles=new IntMap<Kind>();int count=0,unknown=0;
            Jval blockRules=root.get("blocks"),tileRules=root.get("tiles");
            if(blockRules!=null){
                if(!blockRules.isObject())throw new IllegalArgumentException("blocks must be an object");
                for(var e:blockRules.asObject()){
                    if(++count>MAX_OVERRIDES||e.key.length()>128)throw new IllegalArgumentException("Override count/name limit");
                    Kind kind=override(e.value);Block b=Vars.content==null?null:Vars.content.getByName(mindustry.ctype.ContentType.block,e.key);
                    if(b==null)unknown++;else nextBlocks.put(b,kind);
                }
            }
            if(tileRules!=null){
                if(!tileRules.isObject())throw new IllegalArgumentException("tiles must be an object");
                for(var e:tileRules.asObject()){
                    if(++count>MAX_OVERRIDES||!e.key.matches("(0|[1-9][0-9]{0,4}),(0|[1-9][0-9]{0,4})"))throw new IllegalArgumentException("Invalid tile override");
                    int comma=e.key.indexOf(','),x=Integer.parseInt(e.key.substring(0,comma)),y=Integer.parseInt(e.key.substring(comma+1));
                    if(x>32767||y>32767)throw new IllegalArgumentException("Tile coordinate exceeds native range");
                    nextTiles.put(Point2.pack(x,y),override(e.value));
                }
            }
            blocks=nextBlocks;tiles=nextTiles;status="Map classification overrides: "+count+(unknown>0?"; unknown content ignored: "+unknown:"");
        }catch(RuntimeException bad){status="Invalid map classification overrides; using content defaults";Log.warn("[FPV terrain] @",status);}
    }
    private static Kind override(Jval value){
        return switch(value.asString()){case "natural"->Kind.NATURAL;case "artificial"->Kind.ARTIFICIAL;default->throw new IllegalArgumentException("Expected natural or artificial");};
    }
}
