# A2-15b — Background customization

Baseline: A2-15a.2 `e07c8d0`, existing `main` branch. Local work only.

## Phase 1 checkpoint — depth and presets

Implemented Auto/Manual and nine whole-world presets with independent N/S/E/W overrides. Both FPV settings and the Build panel open the standard modal Background settings dialog. Preset changes rebuild only background meshes; Auto restores the border-derived profile. Space uses the night sky; Void clears the background sky and scenery. Existing gameplay terrain is unchanged.

The native deep-water **visual** bed now descends smoothly from normal water toward six tiles over an eight-tile band. Shared vertices continue the actual water-edge bed into the generated background, instead of raising it to Z0. The background ocean then deepens toward 64 tiles; small basins use eight. This is not a change to collision, immersion limits, naval support or authoritative water depth. Gradient regions retain matching tile fans; level regions can still use coarse water geometry. Bathymetry is cached and bounded to 1024² samples; exceptionally large maps use a coarser visual field.

Validation: 15 relevant programs PASS on Java17 and stock Steam25. Actual stock SDL fixture: 336 frames, seven views, zero GL errors, unchanged native map/entities, and the settings dialog opens modally. This is scripted validation, not physical mouse/gameplay or Linux acceptance. An initial broad run exposed a seam fixture still using the old synthetic bed anchor; it was updated to use the real shared water-bed callback and passes.

## Phase 2 checkpoint — painter

Added a modal top-down editor with an immutable central native map, 17 biome/shape/restore brushes, world-space zoom/pan, radius/hardness/height controls, 32-stroke undo/redo and explicit Preview/Apply. An 89×89 nonuniform mask puts more samples near the map; material and height channels are independent. Painted islands/coasts are part of the terrain field, with flat separate water, bounded forest proxies and no gameplay entities. Repainting an island with forest retains its land coverage (the first scripted render exposed gaps at overlapping brush rims; fixed and regression-tested). Tree proxies use deterministic jitter.

18 programs PASS on Java17 and stock Steam25. Corrected actual SDL fixture: 240 frames, five views including the real canvas drag event path and Preview button, zero GL errors and unchanged native map/entities. The 960×600 UI and island overview were visually inspected. No physical input or Linux gameplay acceptance is claimed.

Phase 3 per-map persistence remains outstanding at this checkpoint; phase 2's in-memory custom painting is not advertised as restart-safe yet.
