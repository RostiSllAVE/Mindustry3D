# A2-16i-fix2 — Turret surface overlap

Baseline: `64d4b5d` on `main`. This is an unreleased visual follow-up to the owner's accepted A2-16i-fix1 build.

## Confirmed causes

- **Wave/Tsunami:** Mindustry 160.7 draws body, liquid mask, top mask and heat in painter order. Our capture turned the masks into thin complete extrusions. Liquid and top roofs had the same height, so the depth buffer produced the striped/jagged patches shown by the owner. A disposable stock Steam Java 25 fixture reproduced the defect with the real host atlas, including water and slag.
- **Cyclone:** its three native `under` barrel parts (`-barrel-3`, `-barrel-2`, `-barrel-1`) used the same full depth and vertical position. Their overlapping top, bottom and side faces fought during recoil. The native parts have separate recoil indices; those indices and offsets were already correct.

## Correction

Native liquid/top/heat art now uses cached, surface-only geometry. The surfaces follow the existing pitched assembly normal in native order, with a gap of `max(0.004 tile, 2.5% of thickness)` between layers. They have no duplicate undersides or thin side walls. Native UVs, alpha silhouettes, liquid colours/fill, heat intensity, yaw, pitch, mirrors and recoil remain in use.

Cyclone's existing combined barrel envelope is partitioned into three separated depth bands in native painter order. Its total envelope, native planar pivots, sliding animation and independent recoil indices are preserved. This special handling applies only to Cyclone's three identified barrel parts. Salvo's previously accepted raised side panels and the other turret profiles retain their depths and mount offsets.

All geometry still enters the existing atlas stream. No new rendering pass, texture, native dependency, shader or per-frame mesh generation was introduced. Runtime changes are restricted to `TurretModelRenderer` and `TurretModelProfile`; combat, projectiles, target arcs, input, cameras, saves and network code are unchanged.

## Validation and limits

See the test matrix and machine-readable validation alongside this report. The focused regression checks native callbacks across 480 yaw/pitch/recoil/floor combinations. Real SDL/OpenGL fixtures exercise seven turrets, native masks/parts, four liquid colours plus empty state, top/side/oblique views, upward/downward pitch and elevated bases. Full-world fixtures additionally exercise authorized elevated placement and actual save/reload.

The seven-type native fixture retains 25 cached regions. Cached CPU geometry grows from 405,504 to 449,856 bytes because roof-only templates are also retained; those templates reduce submitted mask geometry. A full-square decal regression emits 6 vertices instead of a complete 36-vertex prism. Cached data is reused across instances and poses. This is a geometry comparison, not a measured FPS improvement. Whole-world timing samples are retained as diagnostic observations only; overlapping test processes and short runs prevent a controlled frame-time conclusion.

The focused GPU fixture uses the actual production mesh capture and host atlas with a simple alpha-tested test shader; the separate world fixture uses the production rendering passes. Test-only harness classes are injected only into disposable fixture JARs and are absent from the delivered diagnostic JAR. Neither scripted fixture establishes physical mouse/gameplay acceptance. Physical Linux and remote multiplayer tests were not performed.

The renderer retains its existing coarse pixel-style silhouette approximation and distant LOD. This fix removes identified coincident surfaces; it does not redesign the turrets. Earlier releases and diagnostic assets remain preserved. Nothing was pushed, published or uploaded to Workshop.
