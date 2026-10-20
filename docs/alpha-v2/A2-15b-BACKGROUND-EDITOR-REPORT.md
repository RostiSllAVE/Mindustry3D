# A2-15b — Background customization

Baseline: A2-15a.2 `e07c8d0`, existing `main` branch. Local work only.

## Phase 1 checkpoint — depth and presets

Implemented Auto/Manual and nine whole-world presets with independent N/S/E/W overrides. Both FPV settings and the Build panel open the standard modal Background settings dialog. Preset changes rebuild only background meshes; Auto restores the border-derived profile. Space uses the night sky; Void clears the background sky and scenery. Existing gameplay terrain is unchanged.

The native deep-water **visual** bed now descends smoothly from normal water toward six tiles over an eight-tile band. Shared vertices continue the actual water-edge bed into the generated background, instead of raising it to Z0. The background ocean then deepens toward 64 tiles; small basins use eight. This is not a change to collision, immersion limits, naval support or authoritative water depth. Gradient regions retain matching tile fans; level regions can still use coarse water geometry. Bathymetry is cached and bounded to 1024² samples; exceptionally large maps use a coarser visual field.

Validation: 15 relevant programs PASS on Java17 and stock Steam25. Actual stock SDL fixture: 336 frames, seven views, zero GL errors, unchanged native map/entities, and the settings dialog opens modally. This is scripted validation, not physical mouse/gameplay or Linux acceptance. An initial broad run exposed a seam fixture still using the old synthetic bed anchor; it was updated to use the real shared water-bed callback and passes.

## Phase 2 checkpoint — painter

Added a modal top-down editor with an immutable central native map, 17 biome/shape/restore brushes, world-space zoom/pan, radius/hardness/height controls, 32-stroke undo/redo and explicit Preview/Apply. An 89×89 nonuniform mask puts more samples near the map; material and height channels are independent. Painted islands/coasts are part of the terrain field, with flat separate water, bounded forest proxies and no gameplay entities. Repainting an island with forest retains its land coverage (the first scripted render exposed gaps at overlapping brush rims; fixed and regression-tested). Tree proxies use deterministic jitter.

18 programs PASS on Java17 and stock Steam25. Corrected actual SDL fixture: 240 frames, five views including the real canvas drag event path and Preview button, zero GL errors and unchanged native map/entities. The 960×600 UI and island overview were visually inspected. No physical input or Linux gameplay acceptance is claimed.

## Phase 3 — persistence and final validation

Per-map local profiles now persist modes, directional presets and all paint channels. The location is `<Mindustry data directory>/mindustry3-backgrounds/<SHA256 map key>.m3bg`, beside local settings, never inside a native save or multiplayer packet. No absolute developer path is used at runtime. Campaign identity includes planet/sector and dimensions. Custom-map identity includes native map name, dimensions and a bounded, deterministic floor/overlay sample (maximum 256² reads on world entry). Buildings, inventories, units and destroyed walls are excluded. Native SaveIO can omit a map's author; a real round-trip test caught that instability, so author/path/file existence are deliberately excluded. Identical named terrain clones share a profile; renaming or editing sampled native floors can create a new profile.

Format v1 validates map identity, resolution, enums, finite bounded numbers, protected cells, payload length and CRC32. Maximum file read is 128KiB; a populated profile is 103,037 bytes. Writes use a same-directory temporary file and atomic replacement where supported, retaining the previous valid `.bak`. Invalid data is preserved, recovered from a valid backup, or replaced visually by Auto with a UI status. Explicit Apply retains an invalid primary as `.invalid` before replacement. A limit of 128 map profiles bounds ordinary retention; reaching it leaves edits session-only and reports that fact. No automatic history cleanup or owner-save modification occurs. Disk failures do not abort rendering.

World identity changes discard staged previews. A closing editor can only release its own context's preview. Settings in another sector cannot overwrite the previous sector's art. Reset to Auto preserves the custom mask for later use; Clear paint is staged and undoable. There is no export/import UI or structural-motif brush in this pass. Profiles are local binary art data, not a new map/schematic format.

### Controls

Open **Background settings** in **Settings → FPV flight**, or the **Tab Build panel**, then **Open Background Editor**. Auto restores native border generation; Manual chooses whole-world and N/S/E/W presets; Custom overlays the base mode from which it was entered. Choose Manual Ocean, then enter Custom to paint islands into a sea.

Inside the modal editor: LMB drag paints, RMB/MMB drag pans, wheel zooms at the cursor. Radius, hardness and height sliders adjust brushes. Ctrl+Z / Ctrl+Y (also Ctrl+Shift+Z) undo/redo. Preview temporarily rebuilds scenery; Apply saves it; Close/Escape discards staged edits/preview. Only explicit Preview/Apply/preset actions rebuild 3D geometry. The white central rectangle is read-only, and the editor never calls construction or resource operations. Wheel and shortcuts outside the dialog are unchanged.

### Performance measurements

Sequential developer CPU probes, no concurrent gameplay/test suites. Same native preparation helper against A2-15a.2 and the final JAR; three warmups, seven cold cache-invalidated rebuilds and 10,000 cache-hit calls. Real native Mesh buffers via MockGL, **not GPU/FPS**. Full min/max/allocation data is in `evidence/performance-comparison.json` in the local kit.

| Auto scenario | Java17 rebuild median before → after | Steam25 before → after | Mesh bytes before → after |
| --- | --- | --- | --- |
| 64² ocean | 5.82 → 6.00 ms | 5.70 → 5.93 ms | 807,936 → 807,936 |
| 64² mixed | 3.97 → 4.26 ms | 5.49 → 5.57 ms | 676,044 → 677,880 |
| 512² ocean | 16.26 → 17.39 ms | 17.84 → 16.88 ms | 4,804,608 → 4,804,608 |
| 512² mixed | 13.11 → 16.12 ms | 12.62 → 13.42 ms | 3,288,444 → 3,290,280 |

Auto classification fingerprints match the baseline. Requested ocean-depth/edge corrections intentionally change the geometry fingerprint. Cached calls measured 0.0001–0.0013ms, without scanning tiles or disk. These small single-PC samples include JIT/GC variation; no general FPS improvement is claimed. First cold builds cost more (up to 71ms here), including local identity/depth initialization.

The separate painter probe used native water maps with Ocean/Forest/Custom designs. Java17 Custom rebuild medians were 25.26ms at 96² and 41.63ms at 512²; Steam25 28.08ms and 41.63ms. Custom meshes were 153,708 /191,538 vertices (51,236 /63,846 triangles), 9.27 /11.69MB; two material batches, as with Auto. Retained geometry is higher than Auto because the painted field has more radial/angular samples. Java17 temporary rebuild allocations were about 59 /84MB; they occur on explicit rebuilds, never each frame/stroke. Stock25 allocation telemetry is unavailable (`-1`), not zero. Larger native perimeters still increase existing collar costs.

300 history-backed brush strokes measured 0.108ms median on both runtimes, p95 0.233ms (Java17) /0.186ms (Steam25). Raw paint arrays are 102,973 bytes; bounded history estimate 3,501,082 bytes. Encode+decode averaged 1.88 /2.30ms without disk I/O. These are model-edit timings, not physical pointer latency or canvas GPU timings. The actual dialog was separately rendered and event-tested at 960×600.

Actual stock-Steam25 background-only GPU comparison used an owned 960×600 depth FBO, 60 warmup and 120 measured frames/case; 118–119 asynchronous GPU samples were available per case. Queries exclude world rendering, FBO setup/clear, screenshot, blit and display presentation. No synchronous GPU waits or per-frame mesh regeneration were added to gameplay.

| Same Auto scene/view | CPU mean before → after | GPU mean before → after |
| --- | --- | --- |
| Forest oblique | 0.161 → 0.127ms | 2.514 → 2.543ms |
| Forest high | 0.107 → 0.113ms | 2.801 → 3.004ms |
| Water/mountain oblique | 0.113 → 0.113ms | 2.478 → 2.785ms |
| Water/mountain high | 0.117 → 0.104ms | 3.765 → 4.064ms |

This single sequential pair measured a small Auto GPU increase (up to 0.31ms), not an optimization win or guaranteed FPS. Additional final-only Manual Ocean cases measured GPU 3.650ms oblique /4.594ms high; the painted-island design measured 4.157 /5.888ms, CPU 0.098 /0.122ms and 144,078 submitted vertices. Custom's added detail has a real cost, especially from above. The full results include maxima and sample counts, and do not certify weak hardware. All 720 baseline and 1,440 final probe frames passed GL, atlas-state and native-state checks. Auto remains available for lower retained geometry cost.

### Compatibility and remaining limits

All 159 programs pass on Java17 and the stock Steam25.0.1 runtime without `java.logging`. The final SDL gallery exercises 528 frames/11 views, actual canvas drag, enabled Undo/Redo, Preview/Apply, native SaveIO and local reload. A second fresh host JVM restores the copied disposable Custom profile and renders another 48 frames successfully. Zero GL errors; native map/entities unchanged. The first extra UI fixture attempted Undo in the same frame before its disabled-state update; repeating on subsequent frames passes. Failed/intermediate fixtures are retained locally, not represented as final passes.

949 freshly recompiled runtime classes match the final JAR exactly. 1,029 existing runtime classes remain byte-identical to A2-15a.2; changes are confined to presentation/UI. Authoritative physics, combat, unit logic, construction, platform input and networking classes are unchanged. GPL/JNA notices and dependency payloads are unchanged. 2,065 earlier Alpha files and 63 earlier diagnostic assets are hash-verified intact. Native save and protocol formats are unchanged; peers still require the same mod JAR, while art preferences stay local.

Physical Windows/Linux gameplay, X11/Wayland interaction and two-computer acceptance remain **NOT TESTED**. Scripted native Windows SDL rendering is not physical acceptance. Profiles and input code use shared Java/Arc APIs without new native dependencies; that does not certify every Linux driver. Very small far-away brush marks are limited by the bounded grid/mesh; use larger distant brushes. Islands are stylized low-poly scenery and can be flown through because there is intentionally no added collision. Whole-world Space/Void changes the sky; directional/painted void removes scenery within a shared sky. No A2-15c portals, physical ocean expansion or deeper authoritative dive limit is introduced.

Final local diagnostic JAR SHA-256: `c91bb1319bcc25187d2f8e6c35bf6f807e241826cf581329fb8263fc1b1ad34e`. Public metadata/releases are not replaced. No push, publish, merge or Workshop update.
