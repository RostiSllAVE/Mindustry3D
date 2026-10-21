# A2-16a — 3D block-model coverage audit

Audited before implementation from owner-accepted `57602ce` on `main`.
Coverage describes rendering, not whether a block functions in the simulation.

| Family | Existing Z0 presentation | Existing elevated presentation | Action in this task |
| --- | --- | --- | --- |
| Mechanical / Pneumatic / Laser / Airblast Drill | Shared colored box, rotating cuboid, native rotator/top planes | Generic textured structural box; no drill-specific animation | Four dedicated profiles; share the new model path across floors |
| Other drill/mining types (Erekir and modded content) | Generic/special shared fallback | Generic structural presentation | Retain; do not guess profiles from names or tier |
| Conventional DrawTurret turrets | Cached alpha-contour base/guns, native RegionPart articulation, pitched assembly | Same turret model system | Preserve |
| Specialized unsupported turret drawers | Generic base/gun fallback | Generic base plus native gun plane | Preserve; future coverage work |
| Cores | Dedicated plinth/chamber/roof/team art | Generic structural body | Preserve |
| Crafters / factories | Generic body, vents/warmup cue; unit factories have native recipe/payload layers | Structural body plus native assembly/payload layers | Preserve |
| Conveyors / liquid blocks / doors | Native moving item/frame/fill/open-state adapters on generic bodies | Shared dynamic adapters on cached structural bodies | Preserve |
| Power / resource and liquid bridges | Generic pole/body plus existing XYZ links and native connections | Cached bodies plus existing XYZ connections | Preserve |
| Walls / terrain / platforms | Native textures, true wall/cliff surfaces, surface relief, ore deposits | Cached structural wall/platform geometry | Preserve |
| Natural vegetation / rocks | Cached procedural textured templates and vegetation variants | Shared scenery, surface-aware placement | Preserve |
| Units | Dedicated mech/naval/green profiles, cached sprite volume, articulated rigs and weapon systems | Independent XYZ unit rendering | Outside this block-family pass |
| Remaining native blocks | Generic height/footprint body and native roof/team regions | Generic structural body and selected dynamic layers | Future family passes |

## Existing infrastructure and constraints

- `FpvWorldRenderer.renderBuilding` already performs fog, distance and frustum
  checks. Distant buildings use a cheaper body/roof path.
- `LayerWorldRenderer` caches stationary structural chunks and streams native
  animated building state separately; doors and supported turrets already own
  their complete visible geometry instead of drawing a duplicate chunk body.
- `NativeAtlasMesh.Builder` batches by the host's texture identity, reuses streaming
  buffers and flushes bounded vertex batches. Half-texel UV insets avoid atlas
  bleeding. Textures remain host-owned; no global filter/wrap changes.
- Existing template/extrusion submission supports transformed immutable vertex
  arrays. `TurretModelRenderer` demonstrates lazy cached alpha silhouettes and
  separation of native simulation from visual transforms.
- Native `DrillBuild.draw` draws region → optional warmup rim → rotator driven by
  `timeDrilled * rotateSpeed` → top → optional dominant-item tint. `updateTile`
  owns warmup, progress, mining eligibility and resources; presentation must never
  advance those fields. Examined directly in the installed 160.7 bytecode.
- Native drill sizes, regions, rotation settings, production statistics and
  consumers are read from loaded content rather than duplicated gameplay values.

The baseline has **0/4 dedicated requested drill profiles**. It has a partial
generic drill animation on Z0, not four purpose-built mechanical models. Existing
turret/unit/scenery work does not need replacement to add this family.

For later families, reuse cached low-poly local templates, explicit native-content
profiles, read-only animation sampling, atlas-safe material sampling and the
existing shared streams. Keep expensive pixel/template work out of frame loops,
provide bounded distant geometry, and measure geometry plus actual CPU/GPU cost
separately from physical gameplay acceptance.
