package fpv.render;

import arc.graphics.g3d.Camera3D;
import mindustry.gen.Building;
import mindustry.world.blocks.defense.turrets.Turret;

/** Conservative presentation envelope, separate from authoritative placement/picking bounds. */
final class BlockVisualBounds {
    static boolean applies(mindustry.world.Block b){
        return b==mindustry.content.Blocks.illuminator||b==mindustry.content.Blocks.impactReactor||
            b==mindustry.content.Blocks.overdriveProjector||b==mindustry.content.Blocks.phaseWeaver||
            b==mindustry.content.Blocks.wave||b==mindustry.content.Blocks.tsunami||b==mindustry.content.Blocks.cyclone;
    }
    static boolean visible(Camera3D camera,Building b,float x,float base,float z,float h,float visualHeight){
        float radius=Math.max(.5f,b.block.size*.75f),low=base,high=base+Math.max(h,visualHeight)+.04f;
        if(b instanceof Turret.TurretBuild){
            // A long native gun rotates in XYZ around the top pivot, not around the box centre.
            var art=b.block.region;
            float span=art==null?b.block.size:Math.max(art.width,art.height)*art.scl()/mindustry.Vars.tilesize;
            radius=Math.max(radius,span*.75f+b.block.size*.25f);
            low=Math.min(low,base+h-radius);high=base+h+radius;
        }
        return camera.frustum.containsBounds(x,(low+high)*.5f,z,radius,(high-low)*.5f,radius);
    }
}
