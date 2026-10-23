package fpv.render;

import arc.graphics.g3d.Camera3D;
import mindustry.gen.Building;
import mindustry.world.blocks.defense.turrets.Turret;

/** Conservative presentation envelope, separate from authoritative placement/picking bounds. */
final class BlockVisualBounds {
    static boolean applies(mindustry.world.Block b){
        return UnitProductionRenderer.supports(b)||CoreInfrastructureRenderer.supports(b)||b==mindustry.content.Blocks.illuminator||b==mindustry.content.Blocks.impactReactor||
            b==mindustry.content.Blocks.overdriveProjector||b==mindustry.content.Blocks.phaseWeaver||
            b==mindustry.content.Blocks.wave||b==mindustry.content.Blocks.tsunami||b==mindustry.content.Blocks.cyclone;
    }
    static boolean visible(Camera3D camera,Building b,float x,float base,float z,float h,float visualHeight){
        float radius=Math.max(.5f,b.block.size*.75f),low=base,high=base+Math.max(h,visualHeight)+.04f;
        if(UnitProductionRenderer.supports(b.block)&&b instanceof mindustry.world.blocks.units.UnitBlock.UnitBuild factory&&factory.payload!=null){
            // A real outgoing payload can overhang the bay before native release. Keep its
            // capped sprite in the presentation envelope without enlarging collision/picking.
            float offset=Math.max(Math.abs(factory.payload.x()/mindustry.Vars.tilesize-x),Math.abs(-factory.payload.y()/mindustry.Vars.tilesize-z));
            if(Float.isFinite(offset))radius=Math.max(radius,offset+b.block.size*.44f);
        }
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
