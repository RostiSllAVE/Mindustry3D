# A2-17a — blocky and hybrid mountain presentation

Baseline: owner-accepted `ff9c21f` on `main`, including the A2-16o turret/exhaust polish. Local diagnostic only; no push, publication, Workshop update or old artifact replacement.

## Audit and safety boundary

`TerrainClassification` is the A2-10 natural/artificial/decoration policy. `TerrainElevation` builds the existing immutable `TerrainHeightField` from natural-wall membership. Its EDT rise, smoothing, shared corners and exact two-triangle interpolation feed both the renderer **and authoritative XYZ collision, projectile traces, support and picking**. They are not merely decorative mountain heights. These classes and their parameters remain unchanged.

Artificial/industrial walls retain their ordinary block footprints and heights. A2-11 floor relief, A2-12 ore geometry, liquids/shorelines, vegetation, cosmic islands and background/painter have separate presentation paths. None is redesigned here. Plants and floor ores remain in open native cells; new mountain faces stay strictly inside their own natural cell and cannot extend horizontally into those cells or building footprints. Existing holes in the natural-wall mask remain open; this height-field renderer does not contain overhanging caves.

## Three local graphics modes

**Settings → FPV flight → Mountain geometry style**, alongside the two existing draw/detail-distance controls:

- **Smooth:** compatibility default and fallback. The original `terrainTile` and `NativeAtlasMesh.terrainTop` implementations, roof/slope coordinates, materials and legacy exterior faces are unchanged.
- **Blocky:** genuine half-tile treads with nominal 0.40-tile vertical bands and exposed vertical risers. Quantization uses the original field, not a new mountain generator. Broad height/silhouette and the original maximum remain in control. A horizontal tread is admitted only when its whole patch fits the safety tolerance; unusually steep patches retain the original slope.
- **Hybrid:** deterministic broad XY regions select either those rocky treads or the original slopes. Adjacent surfaces are connected with their actual exposed risers, not two intersecting meshes or an average of unrelated meshes.

The preference is client-side and saved by normal Arc settings, not in vanilla saves or network packets. Unknown/corrupt values fall back to Smooth. A style change is observed on the next 3D render, without restart; only terrain chunk batches are discarded and lazily rebuilt using the existing upload limit. Those shared batches contain floor/ore/prop vertices too, so they are reuploaded together, while their reusable material/templates and block/unit model caches remain intact. No authoritative field invalidation, new world scan or height generation is requested by the setting.

## Geometry, seams and textures

`MountainGeometry` reuses fixed scratch arrays during detailed chunk construction. It emits surface patches, then only the higher exposed side of each adjoining pair. Equal/hidden sides and buried cube bottoms are omitted. Compatible flat patches within one native material tile merge back into a single roof quad. A clipped side polygon is split at world-height texture bands; this also handles smooth-to-stepped transitions without missing triangular wedges.

The complete original atlas sprite still covers one native XY tile. Roof subpatches sample their corresponding subrectangle. Risers use world-space horizontal coordinates and one texture repetition per vertical tile, with half-texel-inset atlas UVs. A tall cliff does not stretch one small sprite over its full height. No global texture repeat/filter changes, new textures, new shader pack or Minecraft code/assets. Face orientation and restrained directional brightness remain consistent with the existing stylized renderer.

At chunk edges, patch vertices return exactly to canonical heights, creating a narrow transition to any adjacent detailed, distant or fog chunk. No skirt hangs into an unrelated building. The unchanged coarse/distant tier remains Smooth; the selected style is baked into the **existing high-detail tier**. This deliberately preserves the current draw-distance/detail-distance settings, bounded chunk cache and far-map cost instead of building a new whole-map voxel renderer. Increase high-detail distance for a wider near-style comparison. Distant automatic/custom Background terrain retains its accepted geometry; painter data is never overwritten.

## Collision discrepancy and limitations

Rendered roof deviation is bounded to **0.35 native tiles**, never above the canonical global maximum. The bound covers the whole linear patch, not just its center. Stronger quantization is rejected when it cannot meet the bound. Open/native/artificial cell boundaries never move horizontally.

**Collision, projectile impacts, landing and terrain picking still follow the original Smooth triangles.** A contact/aim marker can therefore differ from a decorative tread by up to that tolerance; these are not physically climbable or destructible voxels. At extremely close contact the discrepancy can be visible. Smooth restores exact original triangle presentation. Do not interpret the new steps as changed construction support or traversal permission.

Blocky deliberately retains some short slopes at chunk transitions and safety fallbacks. Hybrid retains substantial smooth regions. Far LOD and the Background are not converted to cubic scenery. Smooth also retains its original exterior-wall UV behavior as the exact compatibility fallback. Native sprite patterns remain recognizable and may repeat; there is no new triplanar shader/material smoothing.

## Validation and artifacts

See `A2-17a-TEST-MATRIX.md`, `A2-17a-VALIDATION.json` and `A2-17a-MANUAL-TEST.md`. Tests distinguish numerical/native checks, actual Windows OpenGL screenshots and pending owner acceptance. The same diagnostic JAR targets Windows/Linux Java17-compatible bytecode and is exercised on complete Java17 and the standalone host's stock minimal Java25. A physical Linux graphics test and independent two-computer acceptance remain **NOT TESTED**.

Local kit: `local/a2-17a/test-kit/mindustry-fpv-a2-17a-diagnostics.jar`, corresponding-source ZIP, comparison panels, raw screenshots, GPL/third-party notices and checksums. No host game, save or profile is included. Existing releases/diagnostics and the clean standalone reference retain their hashes.

Developer reproduction (fresh run names):

```powershell
python tools/build-diagnostics.py --task a2-17a --run my-build
python tools/test-diagnostic-runtime.py --build local/a2-17a/my-build --java bundled25 --run my-java25
python tools/launch-standalone.py --run my-mountains25 --fixture tools/java/MountainStylesHarness.java
python tools/launch-standalone.py --run my-mountains17 --java java17 --fixture tools/java/MountainStylesHarness.java
```

The launcher uses standalone Mindustry 160.7 and a new disposable profile, not Steam. Choose explicit `--jar` when comparing a different diagnostic. Python/JDK are development tools only; players import the single JAR normally. Existing multiplayer protocol/authority is untouched; strict identity checks still require matching compiled builds on peers.
