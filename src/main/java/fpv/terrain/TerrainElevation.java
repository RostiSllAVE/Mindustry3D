package fpv.terrain;

import fpv.generated.TerrainConfig;
import mindustry.Vars;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import java.util.BitSet;
import java.util.function.IntConsumer;

/** Read-only native-map adapter. Called on the game thread; never changes tiles or save data. */
public final class TerrainElevation {
    private final TerrainHeightField.Parameters parameters;
    private TerrainHeightField field;
    private Tiles sourceTiles;
    private boolean dirty = true;
    private long revision;
    private double buildMilliseconds;
    private final TerrainClassification classification=new TerrainClassification();
    private long classificationRevision=Long.MIN_VALUE,maskScans;
    /** Sparse per-row bits: coarse rendering visits existing static structures, not every tile's content policy. */
    private BitSet[] staticStructures=new BitSet[0];
    public TerrainClassification classification(){return classification;}
    public long maskScans(){return maskScans;}

    public TerrainElevation() {
        this(new TerrainHeightField.Parameters(TerrainConfig.MINIMUM_HEIGHT_TILES,
            TerrainConfig.RISE_PER_LAYER, TerrainConfig.MAXIMUM_HEIGHT_TILES,
            (int)TerrainConfig.SMOOTHING_PASSES, TerrainConfig.HEIGHT_VARIATION,
            TerrainConfig.VARIATION_SCALE_TILES));
    }

    public TerrainElevation(TerrainHeightField.Parameters parameters) {
        if (parameters == null) throw new IllegalArgumentException("Terrain parameters required");
        this.parameters = parameters;
    }

    /** One content/map policy is shared by generation, rendering, headless surfaces and collision. */
    public boolean naturalWall(Tile tile) {
        return classification.kind(tile)==TerrainClassification.Kind.NATURAL;
    }
    public boolean staticArtificial(Tile tile){
        ensureWorld();return tile!=null&&tile.x>=0&&tile.y>=0&&tile.y<staticStructures.length&&tile.x<field.width()
            &&staticStructures[tile.y]!=null&&staticStructures[tile.y].get(tile.x);
    }
    public void eachStaticArtificial(int x,int y,int width,int height,IntConsumer visitor){
        ensureWorld();int right=(int)Math.min(field.width(),(long)x+Math.max(0,width)),top=(int)Math.min(field.height(),(long)y+Math.max(0,height));
        for(int row=Math.max(0,y);row<top;row++){
            BitSet bits=staticStructures[row];if(bits==null)continue;
            for(int col=bits.nextSetBit(Math.max(0,x));col>=0&&col<right;col=bits.nextSetBit(col+1))visitor.accept(row*field.width()+col);
        }
    }
    private static void staticBit(BitSet[] rows,Tile tile,boolean value){
        BitSet row=rows[tile.y];if(row==null){if(!value)return;rows[tile.y]=row=new BitSet();}row.set(tile.x,value);
    }

    public void invalidate() { dirty = true; }

    /** Ordinary building placement should not regenerate every mountain. */
    public void invalidate(Tile tile) {
        if (tile == null || field == null || sourceTiles == null || Vars.world == null ||
            sourceTiles != Vars.world.tiles || tile.x < 0 || tile.y < 0 ||
            tile.x >= field.width() || tile.y >= field.height()) {
            dirty = true;
            return;
        }
        var kind=classification.kind(tile);
        if (field.isNatural(tile.x, tile.y) != (kind==TerrainClassification.Kind.NATURAL)) dirty = true;
        staticBit(staticStructures,tile,tile.build==null&&kind==TerrainClassification.Kind.ARTIFICIAL);
    }

    /** O(1) in unchanged worlds. A changed mask causes one deterministic full field rebuild. */
    public void ensureWorld() {
        long policy=classification.revision();if(policy!=classificationRevision)dirty=true;
        Tiles tiles = Vars.world == null ? null : Vars.world.tiles;
        int width = tiles == null ? 0 : tiles.width, height = tiles == null ? 0 : tiles.height;
        if (!dirty && sourceTiles == tiles && field != null && field.width() == width && field.height() == height) return;
        long started = System.nanoTime();
        boolean[] natural = new boolean[width * height];
        BitSet[] structures=new BitSet[height];
        boolean changed=sourceTiles!=tiles||field==null||field.width()!=width||field.height()!=height;
        if (tiles != null) {
            for (int i = 0; i < natural.length; i++){
                Tile tile=tiles.geti(i);var kind=classification.current(tile);natural[i]=kind==TerrainClassification.Kind.NATURAL;
                staticBit(structures,tile,tile.build==null&&kind==TerrainClassification.Kind.ARTIFICIAL);
                if(!changed&&natural[i]!=field.isNatural(tile.x,tile.y))changed=true;
            }
        }
        maskScans++;classificationRevision=policy;staticStructures=structures;
        // Rule/presentation invalidation without an effective mask change must not rebuild mountains/chunks.
        if(!changed){dirty=false;return;}
        TerrainHeightField next = TerrainHeightField.generate(width, height, natural, parameters);
        sourceTiles = tiles;
        field = next;
        dirty = false;
        revision++;
        buildMilliseconds = (System.nanoTime() - started) / 1_000_000.0;
    }

    public double heightAt(double renderX, double renderZ) { ensureWorld(); return field.heightAt(renderX, renderZ); }
    public float vertexHeight(int cornerX, int cornerY) { ensureWorld(); return field.vertexHeight(cornerX, cornerY); }
    public float tileCornerHeight(int tileX, int tileY, int corner) { ensureWorld(); return field.tileCornerHeight(tileX, tileY, corner); }
    public boolean naturalWall(int tileX, int tileY) { ensureWorld(); return field.isNatural(tileX, tileY); }
    public float maxHeight() { ensureWorld(); return field.maxHeight(); }
    public int naturalWallCount() { ensureWorld(); return field.naturalCount(); }
    public long revision() { ensureWorld(); return revision; }
    /** Dimensions and natural membership only; this is not a serialization hash of the game world. */
    public long mapFingerprint() { ensureWorld(); return field.maskFingerprint(); }
    public double buildMilliseconds() { return buildMilliseconds; }
}
