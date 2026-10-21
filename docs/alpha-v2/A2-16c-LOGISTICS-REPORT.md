# A2-16c — Advanced 3D item logistics

Baseline: owner-accepted `46ec64c7fd69c374c339a52fe66801e009d51d84` on `main` (A2-16b.1). Local diagnostic only; no published artifacts replaced.

## Coverage audit and native definitions

| Actual vanilla block | Prior presentation | New presentation |
| --- | --- | --- |
| `bridge-conveyor` / Bridge Conveyor (`Blocks.itemBridge`, BufferedItemBridge) | Generic distribution slab; existing obstacle-clearing span | Recessed item bay, four mechanical supports, elevated directional end housing; native span retained |
| `phase-conveyor` (ItemBridge) | Generic slab and native phase bridge art | Distinct taller transfer cage/coils, native endpoint/direction art, inexpensive activity-driven phase-fabric-colored energy detail |
| `unloader` (Unloader) | Generic slab with accepted live filter overlay | Compact recessed-port chamber and side supports; same native `sortItem` center texture/color |
| `duct-unloader` / Duct Unloader (DirectionalUnloader) | Generic Erekir slab | Low directional housing/rails, native top/arrow art and `unloadItem` color |
| `mass-driver` (MassDriver) | Generic base/static roof on Z0, rotating plane upstairs; existing real native bolt/layer pod | Three-mouth-per-side industrial foundation, polygonal bearing, rotating breech/launch rails, alpha-shaped textured gun volume, native recoil and state indicators |

The installed Mindustry 160.7 classes and content were inspected, with native metadata assertions for actual names, sizes and bridge ranges (4 and 12 tiles). Mass Driver is the native 3×3 block, with its own range, capacity, reload, rotation and knockback parameters read rather than replaced. Duct Unloader is the existing directional block, not a newly invented unit or a general Erekir duct pass.

Accepted drills, eleven A2-16b logistics models, Item Source, turrets and unit models retain their implementations. Existing bridge-span rendering and layer cargo-flight presentation were reused rather than discarded. No high-quality unrelated model is replaced.

## Rendering and animation

`AdvancedLogisticsGeometry` generates five bounded templates at three LODs using existing `MechanicalBlockGeometry` and `ResourcePortGeometry`. `AdvancedLogisticsModelRenderer` materializes native atlas UVs once per type/atlas and submits into the existing opaque atlas batch on Z0 and upper floors. Immutable primitive templates are shared; instance motion is an affine transform, not mesh regeneration. Mass Driver uses cached alpha-silhouette extrusion of its actual native gun sprite at near/medium LOD, with original base art, sides and an underside. Far models keep a simple body and native roof/gun art.

The old shared distribution height is a very shallow physical slab. Rendering the new machinery at that same thickness initially still looked flat. Explicit **visual-only** envelopes now give Bridge/Phase/Unloader/Duct Unloader/Mass Driver minimum visible heights of 0.58/0.65/0.32/0.30/0.88 tiles. Existing larger physical heights are respected. These are not new collision, support or building heights. Models remain seated on their actual terrain/platform plane; ordinary stacked-floor clearance is retained. Nearby ceilings and other opaque geometry still occlude them.

Bridge headings resolve native packed links through the owning Tile view. Native range, team/type, reverse-link and diagonal checks remain authoritative. Incoming native links orient receiving endpoints. Explicit cross-floor bridge edges are read only from the existing validated transfer registry; they are never inferred from matching XY, and Mass Drivers are not given cross-floor links. Ground bridge clearance and upper/cross-floor span endpoint heights follow the visual housings. Existing depth testing remains active, so solid geometry can hide a span rather than revealing it through walls.

Bridge activity reads `warmup`, `timeSpeed` and actual native `time`, gated by enabled/efficiency. Idle phase detail remains subdued; motion follows real transport state. Unloaders retain native filter configuration and colors; no fake resource tokens or transfer counts are created. Their low housings leave room beside storage and belts.

Mass Driver yaw comes from native `rotation`, including receiving alignment. Recoil uses `reloadCounter × knockback`; charge uses loaded/linked shooting state and power, and the brief transfer indicator follows real reload state (both firing and receiving). A reload indicator alone is not claimed to distinguish firing from receipt. Native Z0 bolt timing, XY trajectory and cargo are untouched. Its presentation launch height and the existing upper-floor pod drawing are aligned with the new gun; upper pod visuals use the existing flight's progress and a small muzzle inset. No second projectile, inventory, transfer, damage or receipt is created.

## Scope and limitations

Integration changes are limited to `FpvWorldRenderer`, `LayerWorldRenderer` and the Mass-Driver-specific presentation height in `CombatVisualRenderer`. No simulation, transport rate/capacity, block costs, combat results, collision, world/save format, protocol, mouse input, camera or prediction code changes. No new controls, native dependencies or textures. Multiplayer still requires identical payload identity.

Ports are decorative recessed bays, not gameplay sockets. Native cargo remains a sprite/projectile or the existing layer pod; this pass does not model every transported item. The old bridge span can climb above obstructions on Z0; upper explicit spans retain their existing straight XYZ route and depth occlusion, so tall intervening structures may hide portions. Headings for coincident XY endpoints use the existing fallback orientation. Selected-item colors reproduce native configuration, including unconfigured states. LODs intentionally omit tiny facets. Existing picking/collision bounds and native hologram previews remain the logical construction representation; they are not replaced by detailed animated preview meshes. Broader Erekir ducts, liquid logistics and payload drivers are outside scope.

See the test matrix and validation JSON for final measured results. Scripted Windows OpenGL, headless native/authority checks and physical owner/Linux/remote gameplay acceptance are separate. Earlier fixture failures were invalid test layouts (a storage block was stacked without enough headroom) and an overly strict zero-angle assertion, corrected in the disposable fixture/test; construction limits were not relaxed.

## Final renderer observations

The same 38-scene fixture completed **4,864 frames on stock Steam25 and 4,864 on Java17**, with zero GL errors, stable warmed model caches, native state unchanged by drawing, and an actual disposable disk save/reload. The accepted A2-16b.1 baseline completed another 4,864 frames. These are scripted Windows render checks, not owner input or physical Linux acceptance.

| Scene | CPU before → after, ms | GPU before → after, ms | Draw calls before → after | Submitted vertices before → after |
| --- | --- | --- | --- | --- |
| bridge | 0.549 → 0.666 | 0.062 → 0.071 | 23 → 24 | 47201 → 52823 |
| phase | 0.529 → 0.850 | 0.065 → 0.102 | 25 → 25 | 50615 → 65663 |
| unloader | 0.690 → 0.996 | 0.068 → 0.138 | 31 → 31 | 59075 → 77687 |
| directional | 0.636 → 0.997 | 0.074 → 0.149 | 35 → 35 | 64811 → 83933 |
| mass-driver | 0.724 → 1.426 | 0.083 → 0.252 | 40 → 40 | 73121 → 106217 |
| floor3 | 1.453 → 1.999 | 0.412 → 0.444 | 49 → 49 | 109799 → 142133 |
| field100 | 1.132 → 1.271 | 0.110 → 0.199 | 31 → 30 | 100913 → 124595 |
| field1024 | 3.406 → 5.633 | 2.241 → 4.039 | 63 → 65 | 289361 → 428303 |

Whole-renderer submissions include surrounding terrain, platforms, other blocks and native cargo. The 100/1,024 cases are mixed logistics fixtures, not that many identical isolated Mass Drivers. One sequential run cannot establish an FPS improvement or parity. Steam25 allocation counters are unavailable (`-1`), so no allocation reduction is claimed.

The CPU material-array cache per owning renderer contains 5 type entries: 0 → 6,018 materialized vertices, 0 → 192,576 bytes. This is additional cache memory relative to generic baseline rendering, not a memory-reduction claim; shared immutable templates and existing streaming GPU buffers are not included in this number. Cached arrays remain reused across instances and frames; both Java runtimes produced identical final cache statistics. Both the primitive chassis and textured gun extrusion use the existing batched atlas stream, not per-building GPU meshes.

Final diagnostic JAR SHA-256: `af9fce2f92fc55148dff5f12552869bd7b10fae4993fe1c6be2e9a660968c1ca`. Fresh compilation confirms all 979 project classes and packaged resources match this source. Three existing rendering source files change; 14 class entries change (nested debug metadata included), 6 are new, and 1,084 existing entries remain unchanged. Dependency payload and notices are retained. The source package is exported from the final local Git checkpoint after this report is committed.

All three identical benchmark fixtures observed a real native bolt, a real upper pod, actual item receipt at both native and upper Mass Driver receivers, and actual Bridge/Phase receiver inventory. Powered native update methods and predefined source inventories run outside timed rendering; this is a disposable functional fixture, not a throughput/endurance or full-game simulation benchmark. No artificial delivery is executed by the renderer.

The OpenGL fixture covers same-floor links on stacked floors. Existing explicit vertical bridge registry visualization is implemented read-only but was not separately exercised graphically in this pass; include such a link in owner testing if available.
