# A2-15c — Space floor and cosmic background

Baseline: accepted A2-15b.2 `74d0441266a47f0b66be0bd5c241466659cd43a3`, branch `main`.
This is a local diagnostic, not a published Alpha replacement.

## Confirmed content and implementation

Inspected the actual Mindustry160.7 host bytecode: `Blocks.space` is a Floor using
`CacheLayer.space`; `Blocks.empty` is `EmptyFloor`, whose base pass draws edges/overlays.
Both are solid, non-placeable, non-shadowing, non-overlay-supporting native floors.
Only these **exact two identities** receive the new material. No name/color heuristic,
third-party floor opt-in, volcanic/dark-floor reclassification or physical change.

`CosmicFloorGeometry` produces exact tile-edge row-run masks. `CosmicFloorRenderer`
caches one position-only mesh per nonempty visible chunk, with a 32MiB/4096-entry LRU
budget. Frustum selection is shared with the existing world renderer. No star meshes,
external textures, per-tile draws or ordinary-frame mesh regeneration. Static fog
respects discovered tiles and refreshes cached visibility at 500ms; reveal resets,
team/world changes and tile edits invalidate the appropriate cache. Meshes are freed
on world/cache invalidation; the shared shader is released on renderer disposal.

A specialized opaque depth-writing pass runs after native terrain and before buildings,
layers and units. It uses their existing depth attachment, without clearing it or
changing transparency. The native floor remains underneath as the safe fallback;
the portal mask is at visual Z0+0.002 tiles, not a physical elevation. Mask fragments
compute a stable world-direction starfield with restrained 1/100000 translation parallax.
Cube-direction sampling avoids latitude/pole singularities at vertical camera angles.
Sparse warm/cool stars and continuous, subdued directional nebulae share no time/random
state. Existing outer renderer state restoration remains in charge of GL state.

The same procedural field serves whole Manual/Custom-base Space and darker Void skies.
The existing external horizon's space weight now renders a batched cosmic mask rather
than dropping those triangles. Fully cosmic triangles contain no water or landscape;
non-space geometry/materials are preserved, and decorative trees/industrial scenery
remain excluded by the existing space rules. This also supports painted cosmic regions
without making the whole world cosmic. No new background format, asset or packet.

Settings → FPV flight → **Cosmic floor effect**, default ON. OFF or a failed floor shader
uses the original floor pass. The background preset is independently selected.

## Scope and limitations

Native physical restrictions are preserved: Space/Empty do not become buildable or
traversable differently. No negative-Z space, vacuum, falling, support, construction,
unit, combat, camera, save or protocol changes. This is not a terrain cut-out: its visual
mask still writes depth at native Z0. Large stars may show minor pixel shimmer at some
resolutions; there is no temporal antialiasing/bloom or per-star geometry.

Local cosmic floors/paint do not replace the shared sky above the entire map. Select
whole Space/Void when a fully cosmic sky is desired. The inherited 4-tile native apron
and finite horizon extent remain; a local void at a map edge can meet a planetary sky
outside the mask. Mixed painter material transitions are blended, rather than physical
holes. Underside geometry is the original layer geometry. The 32MiB cache may evict and
rebuild on pathological huge checkerboard maps; no claim of universal FPS is made.

Unsupported-driver shader compilation failure is guarded but has not been physically
reproduced. Linux rendering/input, owner visual acceptance and real two-computer play
are NOT TESTED in this pass. No new native libraries or GL baseline requirements.

## Verification and performance

Final measurements and exact evidence are recorded in `A2-15c-VALIDATION.json` and
`A2-15c-TEST-MATRIX.md`. Tests are automated CPU/native fixtures and scripted real SDL
rendering, not physical gameplay acceptance. Compare GPU elapsed time only at identical
fixture settings; no end-to-end FPS improvement is claimed. Licensed host content is
read from the separately installed game and is not redistributed.

The local test kit includes the diagnostic JAR, GPL corresponding source, notices,
checksums, sanitized evidence and the three owner checks. Original Alpha releases and
previous diagnostic artifacts remain intact. No push, publication or Workshop update.
