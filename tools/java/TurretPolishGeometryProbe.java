package fpv.render;

import arc.files.Fi;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.world.blocks.defense.turrets.Turret;

/** Same read-only geometry measurement on the accepted and candidate JARs, in a real atlas context. */
public final class TurretPolishGeometryProbe {
    public static void run(Fi out) {
        var model=new TurretModelRenderer();var stream=new NativeAtlasMesh.Builder(false);
        var csv=new StringBuilder("block,cachedBytes,submittedVertices,submittedTriangles,cacheBuilds\n");
        var tile=Vars.world.tile(82,32);
        try {
            for(var block:new mindustry.world.Block[]{Blocks.arc,Blocks.lancer,Blocks.meltdown,Blocks.wave,Blocks.tsunami,Blocks.hail,Blocks.ripple}) {
                model.clear();stream.clear();tile.setBlock(block,Team.sharded,0);
                var build=(Turret.TurretBuild)tile.build;build.rotation=35;
                if(!model.render(build,stream,0,0,0,FpvWorldRenderer.heightFor(block),0))throw new AssertionError(block.name);
                var mesh=stream.upload();long vertices=mesh.bytes/36;mesh.dispose();
                long cached=model.bytes(),builds=model.builds();stream.clear();
                model.render(build,stream,0,0,0,FpvWorldRenderer.heightFor(block),0);
                if(cached!=model.bytes()||builds!=model.builds())throw new AssertionError("Warm geometry regenerated: "+block.name);
                csv.append(block.name).append(',').append(cached).append(',').append(vertices).append(',').append(vertices/3).append(',').append(builds).append('\n');
            }
            out.child("turret-geometry.csv").writeString(csv.toString());
        } finally {tile.remove();stream.dispose();model.clear();}
    }
}
