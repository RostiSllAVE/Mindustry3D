# A2-10 — Natural terrain versus artificial structures

Baseline: accepted A2-09c `5eaddd17b1e52ff21fd086bec345ffd805d2f14d`, existing branch `main`. The owner confirmed sandbox/survival progress, F3, priority and no further observed manual delay before this task. This is an unreleased local candidate; no release/Workshop changes are made.

## Audit and confirmed defect

The previous mountain mask admitted `StaticWall` without a Building, excluding `StaticTree` and supported vegetation/props. Normal copper/titanium defensive walls and factories were **already excluded**; this pass does not claim to discover or fix a pre-existing copper-wall mountain bug. The distance transform, noise, smoothing, exact triangle interpolation and open-cell isolation already worked.

However, native Mindustry 160.7 defines `dark-metal`, `metal-wall-1/2/3` and `colored-wall` as StaticWall too. They have no Building, requirements or native geological-material flag. They therefore became mountains despite representing industrial/editor structures. A repeated exact-old/new-JAR probe of a 75×75 dark-metal region confirmed **5625 mountain cells and a 22.926674-tile peak before, zero mountain cells after**. The sampled physical surface changes from 22.687399 to the normal 1.4-tile block height.

The base StaticWall catalog was checked in the actual 160.7 host: **21 geological,5 industrial,5 vegetation** entries. Small rocks/boulders/trees/bushes remain decorations rather than becoming new mountains. Map/editor placement provenance is not available reliably from tile ownership and is not guessed.

## Classification and geometry

`TerrainClassification` provides one CPU-only policy used by the existing TerrainElevation adapter. Renderer, headless physical heights, rays, unit clearance and construction continue consuming that adapter.

| Content | Default |
| --- | --- |
| Live Building, synthetic/hasBuilding content, paid/buildable definitions, defensive Wall | Artificial; metadata cannot promote into terrain |
| Native industrial darkMetal/metalWall1/2/3 identities; ColoredWall capability | Artificial static structure |
| Eligible geological StaticWall | Natural mountain-forming material |
| Trees/bushes/small Prop rocks and other existing decorations | Existing decoration models, no mountain contribution |
| Unknown static non-StaticWall content | Conservative artificial default; author can identify eligible geology explicitly |
| Empty/floor cells | No mountain |

The four native industrial block identities and ColoredWall class are a documented content table, not a block-name substring heuristic. Unknown custom StaticWall still uses the natural default; authors must identify a custom mechanical wall when its content definition provides no distinction. Known industrial static material can intentionally be overridden as geological terrain, but live/synthetic/buildable structures and decorations remain protected.

The mountain generator and numeric parameters are unchanged. Geological-only vertices match the original generated field **bit for bit**, including cliff corners, holes and continuous natural edges. Natural geometry never occupies an artificial footprint.

Coarse rendering previously represented isolated static samples with a stride-sized approximation and omitted non-natural static structures inside mixed cliff cells. It now draws indexed artificial static cells at their actual positions and block dimensions, through the existing textured-wall batch. Side culling compares the actual shared mountain-edge heights instead of a generic flat StaticWall height, preserving exposed strips. The interaction ray treats solid static structures as occluders, without making them selectable/copyable constructed buildings or revealing hidden lower blueprints. No new renderer, shader or raycaster is introduced.

**Physical correction is explicit:** formerly misclassified industrial environmental walls now use the same ordinary block surface in rendering and collision. They no longer retain invisible 22-tile mountains. Collision algorithms, movement, projectiles, support rules and combat parameters are unchanged; this is a correction of their classified shape, not experimental mountain physics.

## Cache and world lifecycle

- Classification is cached by content identity and optional rule text. Unchanged queries do not parse JSON or scan the world.
- The existing mask scan also builds sparse per-row static-structure bits. Native tile events update a single bit for artificial edits; coarse passes query bounded row ranges using a cached visitor rather than reclassifying every visible tile or allocating a callback per cell.
- Artificial add/remove events on open ground do not regenerate the height field or invalidate all terrain chunks. Local renderer chunk invalidation remains active. Replacing actual rock changes mountain membership and legitimately triggers the original full deterministic distance-field rebuild.
- A rule/presentation invalidation whose effective natural mask is unchanged reuses the field and its geometry revision. Static bits are still refreshed.
- World identity/dimensions, actual mask changes and changed rule metadata invalidate the proper caches. Native campaign SaveIO and sector transitions retain/clear authored metadata correctly.

No tile/floor/overlay/team data is rewritten. XYZ conventions, world save format 5, clipboard format and protocol 4 are unchanged. All construction, network, combat, input backend and unit-model class files remain byte-identical to A2-09c; the artifact audit restricts changes to TerrainElevation, FpvWorldRenderer, the new classifier and generated design metadata.

## Optional map-author overrides

Normal players need no settings or extra files. Authors may store this inner JSON document as the string value of **native `Rules.tags["fpv-terrain-classification"]`** (not mod.hjson or a per-player setting):

```json
{
  "version": 1,
  "blocks": {
    "your-mod-industrial-wall": "artificial",
    "your-mod-geological-block": "natural"
  },
  "tiles": {
    "24,18": "artificial"
  }
}
```

Keys in `blocks` are exact registered content names. `tiles` uses native ground-grid X,Y, not render Z or construction floors. Tile overrides take precedence over block overrides; safety guards for live/buildable objects take precedence over both. For example, one hand-authored stone-wall ruin can remain a block while the rest of its stone-wall biome forms mountains. This does not create terrain from air, add negative Z, edit terrain or bypass structural support.

Only version 1, fields version/blocks/tiles, values natural/artificial, nonnegative native coordinates up to 32767, at most 4096 combined overrides and 65536 characters are accepted. Unknown optional content is ignored with a policy status. Invalid documents fall back atomically to content defaults and emit one warning per changed invalid document, not per frame. Native Rules JSON/SaveIO already saves and transports tags; no custom save chunk, new multiplayer packet or editor UI is added. Configure overrides through existing map/rules JSON or a trusted map-generation script before hosting; live editor/native rule-broadcast UI is not newly implemented or physically certified.

## Validation and performance

Both final complete suites passed: **139/139 Java 17 and 139/139 stock Steam Java 25.0.1**. New tests cover 4452 classification/height/cache/rule checks and 305 actual coarse CPU-geometry/edge/picking checks. The campaign fixture performs 42 real native generation/disk SaveIO/backup/transition checks in disposable profiles. The 11705 pure height-field checks and prior construction/selection/copy/combat/input regressions pass.

The owner still needs to review actual textured pixels, distant LOD and normal control in-game. Renderer fixtures use real CPU vertices/picking with mock atlas/GL; they are not screenshots, GPU measurements, Import→Restart proof or physical Windows/Linux/multiplayer acceptance.

Sequential standalone before/after CPU probes use identical 512×512 maps and settings. Generation is the median of five fresh fields; cached-query and edit columns are single batches, include JIT/native tile-edit overhead and must not be interpreted as per-frame FPS. Full min/max and hashes are in A2-10-VALIDATION.json and ignored local comparison logs.

| Runtime / map | Field generation median ms, before → after | 100000 cached queries ms, before → after | 1000 artificial edits ms, before → after | Extra field rebuilds |
| --- | --- | --- | --- | --- |
| Java 17 / sparse8 | 29.302 → 25.298 | 13.014 → 8.695 | 17.197 → 19.918 | 0 → 0 |
| Java 17 / dense85 | 42.276 → 45.091 | 4.177 → 6.744 | 0.659 → 1.675 | 0 → 0 |
| Steam Java 25 / sparse8 | 27.700 → 18.866 | 2.505 → 5.865 | 16.833 → 17.193 | 0 → 0 |
| Steam Java 25 / dense85 | 29.084 → 39.730 | 4.696 → 6.289 | 1.237 → 0.971 | 0 → 0 |

Sparse is an 8% deterministic pattern (20968 natural cells); dense is a 450×495 contiguous rock region (222749 natural cells), with a flat edit site. Before/after fingerprints and sample checksums agree on both geological maps. Dense one-time generation is slower in this sample (+2.815 ms Java 17, +10.646 ms Steam Java 25), and some cached-query batches are slower; sparse medians are lower but noisy. This is **not an FPS improvement claim**. Both versions correctly produce zero extra full rebuilds for 1000 artificial edits; the new classification preserves that behavior. Policy parsing and world-scan counters stay unchanged during repeated ordinary queries. GPU/frame timing and allocation profiling remain unmeasured.

## Candidate and remaining limits

- Final common Windows/Linux JAR: `local/a2-10/test-kit-final/mindustry-fpv-a2-10-diagnostics.jar`.
- SHA-256: `9bf2ddf608de6c70738ee45d3745365d4fe83d534a4274db8857e13db2862ee1`.
- Payload: `payload1-d16ab5141f2334987530365996403b549da25b149d1809103da128809c934af5` (actual Java JAR/folder fingerprint verified).
- 991 previous class files are byte-identical. Existing Windows/Linux native resources and all notice/license bytes are preserved; 2065 prior Alpha snapshot files and 33 prior diagnostic release assets were hash-verified unchanged. Source/checksums are packaged from the local Git checkpoint; earlier A2-10 candidates are also retained.

Unknown custom content still needs correct definitions or author overrides; the mod cannot infer whether someone intentionally used natural rock as a building material. A real natural-mask edit still requires a global distance-field rebuild. Actual texture/color/LOD appearance, physical Linux gameplay and independent two-computer world/rule synchronization remain NOT TESTED in this pass. No Android claim is added. Follow A2-10-MANUAL-TEST.md on a disposable map/save copy.
