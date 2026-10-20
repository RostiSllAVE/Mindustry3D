# A2-15a — Automatic biome-based background and horizon

Baseline: accepted A2-14 `d22d028e2931c7f96ba1a7c04829477fa9811147` on `main`. This is a local diagnostic, not a public Alpha replacement. Existing release metadata remains unchanged; both multiplayer peers must use this exact diagnostic payload. No push, publication or Workshop update.

## Findings and scope

The old background used one planet-coloured skirt/mountain ring regardless of the actual border. That made ocean, desert and forest maps terminate against unrelated grey mountains. The existing sky, campaign/day-night clock, stars and companion planet already worked and remain intact. Existing natural/artificial classification, relief, ores, liquid levels/animations, vegetation, playable tiles and simulation boundaries are reused rather than regenerated.

`BorderBiomeProfile` describes the edge environment; `HorizonGeometry` builds a visual-only annulus; `AutomaticHorizon` owns its cached GPU meshes/materials. `EnvironmentRenderer` selects Auto or the retained legacy background. No new entities, native tiles, pathfinding, construction surfaces, collision, packets or save data are created. Decorative geometry never participates in picking. Display far distance expands to enclose it; pilot sight/aim convergence and the original picking range explicitly retain their previous far distance. Camera movement and controls are unchanged.

## Border detection

Every side is analysed independently, in approximately eight-tile sections (at most128/side). Every cell in a bounded strip is sampled: default8 tiles, internal setting `fpv-horizon-strip` clamped2–16. Work scales with perimeter × strip depth, not map area. Long sides retain regional mixtures rather than receiving one majority biome. Shared cyclic interpolation blends adjacent sections and the four corners.

Classification uses native floor identities/materials and existing natural-wall classification. Water includes native shallow/deep/tainted water; snow/ice/red-ice/cryo become ice; sand/darksand/salt become desert; basalt/hotrock/magmarock/slag/tar/carbon-stone become volcanic; supported remaining natural ground becomes rocky wasteland. Space/empty stays space. Unknown content has a neutral fallback. Geological walls contribute mountain weights; artificial defensive/industrial walls do not. Units are not sampled. Adjacent natural-rock/water regions receive a bounded continuity bonus. Vegetation uses existing native profiles; a forest bonus starts only above30% coverage. Sparse desert plants do not turn the entire border into forest. The policy is deterministic, including content-name-based vegetation profiles.

Base colours come from native material metadata, refined using at most8×8 opaque pixels per shared host-atlas region. Samples are cached; there is no synchronous GPU readback or external texture dependency. Missing pixmaps fall back to content colours. Water uses an approximation of the existing surface/bed composite. This matches the native palette more closely than using map-thumbnail colours alone; it does not reproduce the whole in-map shader in the distance.

## Implemented environments and transitions

- Ocean: low continuous surface and inexpensive world-space waves; distant glint fades with view distance to reduce shimmer. No water mechanics beyond the map.
- Mountains: deterministic geological silhouettes, a larger near range and successively smaller far ranges; height capped120 tiles.
- Forest: ground plus two sparse rows of low-poly conifer/leaf/bare/diamond or shrub-like proxies using native vegetation colours. No simulated trees.
- Desert: sandy ground and low coherent dunes.
- Ice: pale/frozen ground and restrained distant formations.
- Wasteland/volcanic: rocky/dark relief without active lava mechanics.
- Space: omit outside geometry and retain the existing sky. Native space-floor rendering is unchanged; no portal/chromakey.
- Unknown: restrained neutral ground rather than invented biome assets.

A four-tile textured collar starts at the exact native rectangular boundary, sampling existing relief, natural mountain corner heights and liquid edge heights. Half-texel UV insets and per-edge orientation preserve native textures while blending into atlas-derived colours. Intentional geological/open height steps get closing wedges. All directions share the same mitered corner coordinates. There are no unsupported outside-world gameplay surfaces.

The collar meets a tile-resolution first band; farther rings use bounded section topology. Nonlinear wave motion is pinned to zero at the12-tile fine/coarse join to prevent animated T-junction cracks observed in an intermediate test. Farther biome boundaries drift gradually with deterministic coherent variation, avoiding a hard90° partition. Outside-only haze softens the far end; playable terrain is not hidden with extra fog. Distant geometry uses the existing world depth buffer, so it does not overlay nearer buildings. Depth precision is subject to the existing renderer and longer display far plane; no new FBO is introduced.

## Cache, lifecycle and future overrides

No per-frame border scan, palette sampling or mesh generation occurs on a cache hit. World/content/atlas/planet/policy/strip changes and relevant border edits rebuild; ordinary interior tile edits do not. Rendering invalidation shares the existing terrain object, avoiding a duplicate mountain field. Replacing a world releases both owned meshes and previous tile/profile references; atlas replacement/dispose also clears palette references. The host owns sampled pixmaps. Dynamic lighting/time changes only uniforms. Save/load recreates identical derived geometry and writes no custom terrain state.

Settings → FPV flight → **Automatic biome horizon (OFF: legacy mountains)** defaults ON. OFF frees Auto world geometry and uses the old ring; ON rebuilds it lazily. There are no new keys. Biome-profile/geometry separation prepares future override policies, but A2-15b manual presets and A2-15c are not implemented.

## CPU, memory and geometry measurements

Identical compiled probe before/after on native64²/512² ocean/mixed maps, Java17 and stock Steam Java25. Shared original terrain generation is prepared and excluded. Real Mesh buffers/uploads through MockGL,3 warm-up +7 measured full rebuilds; medians below. First cold includes JIT. Optional thread allocation is unavailable on stripped Steam25. These are preparation costs, **not frame time or FPS**. All original world meshes are unchanged.

| Runtime / native map | Rebuild median, old → Auto | Auto first cold | Auto allocation / mesh bytes | Auto vertices / triangles |
| --- | ---: | ---: | ---: | ---: |
| java17 / 64/ocean | 0.435 → 1.386 ms | 15.483 ms | 754304 / 138240 B | 3840 / 1280 |
| java17 / 64/mixed | 0.249 → 1.407 ms | 3.980 ms | 761456 / 142992 B | 3972 / 1324 |
| java17 / 512/ocean | 0.101 → 4.420 ms | 6.083 ms | 5696448 / 1105920 B | 30720 / 10240 |
| java17 / 512/mixed | 0.151 → 4.387 ms | 4.745 ms | 5747104 / 1143504 B | 31764 / 10588 |
| steam25 / 64/ocean | 0.409 → 1.260 ms | 14.165 ms | unavailable / 138240 B | 3840 / 1280 |
| steam25 / 64/mixed | 0.140 → 1.371 ms | 4.364 ms | unavailable / 142992 B | 3972 / 1324 |
| steam25 / 512/ocean | 0.141 → 3.844 ms | 4.860 ms | unavailable / 1105920 B | 30720 / 10240 |
| steam25 / 512/mixed | 0.130 → 2.710 ms | 4.376 ms | unavailable / 1143504 B | 31764 / 10588 |

Legacy background:4,608 vertices /1,536 triangles /129,024 mesh bytes /one material batch. Auto: one coloured far mesh plus atlas-textured collar (two batches with the native single atlas; additional texture pages can add collar batches). Empty space produces zero background vertices. At512² Auto uses about1.14MB cached mesh payload and about5.75MB temporary rebuild allocation on Java17. Buffer/driver overhead and exact retained JVM heap are not measured. Preparation arrays are discarded after upload; profile/palette/mesh buffers remain cached. A synthetic8192² border-only fixture validates bounded analysis and linear perimeter geometry without allocating an8192² game world. It is not a physical huge-map benchmark.

Measured reflective cache checks take0.00010–0.00066ms; that includes reflection. Real SDL first atlas access plus analysis took30.291ms and generation6.428ms in the initial scene; subsequent gallery scenes generally took0.6–2.9ms per phase. A brief first-entry/rebuild hitch is therefore possible. Do not interpret cached generation as zero memory cost or claim a measured FPS gain.

### Actual Windows/Steam SDL and GPU measurements

Real stock Steam25/SDL/GL,960×600,30FPS cap, disposable D: profile. Nine96×64 fixtures,1,080 frames for both accepted A2-14 and final A2-15a, GL errors0. Automated camera includes a full yaw turn, low views, and720-tile overhead captures. Timing uses60 low-view CPU samples and59 asynchronous GPU queries/scene; screenshots are outside timing brackets. High views are captured but **not** a high-view performance benchmark.

Separate-process results below fluctuate; some GPU scenes became slower, so they cannot support a blanket performance claim:

| Scene | A2-14 CPU / GPU | A2-15a CPU / GPU |
| --- | ---: | ---: |
| ocean | 2.461 / 1.706 ms | 2.524 / 2.565 ms |
| mountains | 2.487 / 1.263 ms | 2.277 / 1.670 ms |
| forest | 2.025 / 4.849 ms | 2.494 / 4.740 ms |
| desert | 2.368 / 0.666 ms | 2.455 / 1.663 ms |
| ice | 2.416 / 1.161 ms | 2.138 / 1.164 ms |
| mixed | 2.039 / 1.756 ms | 1.905 / 1.294 ms |
| artificial | 2.203 / 0.675 ms | 1.948 / 0.672 ms |
| space | 2.019 / 1.159 ms | 1.994 / 1.129 ms |
| unknown | 2.011 / 1.122 ms | 1.361 / 1.233 ms |

To reduce that noise, a final-JAR same-process comparison toggles the real setting every40 frames, first80 frames discarded and first12/window excluded. Three fixed-camera/time paused scenes,1,200 frames total, two asynchronous timer streams, GL errors0. Generated fingerprints remain stable across toggles:

| Scene | Legacy CPU / GPU | Auto CPU / GPU | Samples |
| --- | ---: | ---: | --- |
| ocean | 1.763 / 1.071 ms | 1.729 / 0.812 ms | CPU112/mode, GPU112/110 |
| desert | 1.799 / 0.994 ms | 1.818 / 0.725 ms | CPU112/mode, GPU112/111 |
| mixed | 1.804 / 0.977 ms | 1.666 / 0.998 ms | CPU112/mode, GPU112/110 |

The paired results show no substantial added frame cost on this Windows GPU in these scenes. They do not certify low-end Linux performance, arbitrary maps, night/high-altitude timing, uncapped FPS or remote multiplayer. Full-world retained mesh bytes are exactly equal before/after in all nine fixtures. Background counters remain stable between world changes/settings changes; there is no per-frame rebuild loop.

## Validation and artifact correspondence

Final JAR SHA-256: `6a6c73ef35f646f84790100b1a4118c5fce28ee915b0f0bd916d3561bea52d87`.

- Java17:147 regression programs PASS. Stock Steam25.0.1 (without java.logging):147 PASS.
- New native horizon tests:260 assertions PASS; geometry/large-border/corner tests:466,349 checks PASS on both runtimes.
- Existing campaign44, relief, ore, liquid/exposure, vegetation, selection/copy/paste, construction pause/priority, input, combat and networking regressions remain PASS.
- Actual native SaveIO/load in the graphical mixed scene preserves map and background fingerprint; nine-world replacement and setting-toggle lifecycle pass.
- Four profile/mesh fingerprints are identical across Java17/Steam25. Deterministic independent calculations are not a real two-client gameplay acceptance test.
- Fresh recompilation:913 runtime classes byte-identical to packaged classes, resources/design sheets match;1,006 existing classes unchanged versus A2-14. Existing native libraries/license notices unchanged. JAR/folder identity matches on both runtimes.
- 2,065 prior Alpha files and54 previous diagnostic assets have unchanged hashes. Primary player data is untouched.

The diagnostic JAR supports the same Windows/Linux code paths without new platform APIs or dependencies. Actual Linux graphics/input, physical import/restart/gameplay, independent multiplayer and owner visual acceptance are NOT TESTED in this pass. The graphical lab replaces only `fpv/SmokeHarness.class` in disposable copies; manifests identify the replacement. The delivered JAR retains its normal harness. Sanitized results/screenshots are packaged; raw Steam logs/profiles/saves/game binaries are excluded.

## Remaining visual limitations

The low-view outside continuation and missing-water/crack defects are addressed. From very high overhead, the in-map texture-detail rectangle and broad mixed-biome material fans can still be recognizable. The collar/LOD/haze reduces termination artifacts; it is **not a guarantee of an invisible boundary at every angle**. Extreme border cliffs may have steep decorative slopes. Far forest proxies are deliberately sparse/simplified and can be hidden by native foliage. This is inferred scenery, not adjacent campaign sectors. Unsupported/modded materials can use neutral colours or a plain fallback collar. Existing outer liquid corners remain pinned to native edge heights; water is not re-levelled. Night/day artistic matching and low-end hardware require owner review.

The three most useful physical tests are ocean/mixed edges and corners; biome identity versus artificial walls; and save/sector transitions plus normal FPV/build controls. Exact steps are in A2-15a-MANUAL-TEST.md. Visual acceptance remains pending; no gameplay expansion is implied.
