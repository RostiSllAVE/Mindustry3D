package fpv.render;

/** Visual travel only. Native shipment lifetime, resource transfer and authoritative state stay intact. */
final class TransferBeamPose {
    static final float TRAVEL=64,TAIL=12,OUTER=.27f,CORE=.085f;
    static float endpoint(float height){return height*.82f+.10f;}
    static float tip(float progress,boolean send){return TRAVEL*(send?progress:1-progress);}
    static float tail(float tip,boolean send){return send?Math.max(0,tip-TAIL):Math.min(TRAVEL,tip+TAIL);}
}
