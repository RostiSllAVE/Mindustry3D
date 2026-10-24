# A2-16l — 3D logic processors, memory and functional displays

Continued on `main` from the accepted unit-upgrade checkpoint `b3bb11b`. The task reuses the label A2-16l already used by that prior pass, so this pass uses the **A2-16l-logic** directory/artifact namespace. Earlier A2-16l reports and releases are preserved. No checkout, reset, merge, push or publication was performed.

## Native audit and model coverage

The installed Mindustry 160.7 definitions were inspected directly. Nine explicit native blocks now have dedicated bodies; third-party, privileged/world logic, Canvas, tileable displays and reinforced-message bodies retain their existing paths. No prior dedicated factory, reconstructor, unit or other model family was replaced.

| Native block | Footprint / existing height in tiles | Native function | New presentation |
|---|---|---|---|
| Micro Processor | 1×1 / 0.88 | 2 instructions/tick | Compact octagonal compute core and circuit housing |
| Logic Processor | 2×2 / 1.26 | 8 instructions/tick | Broader compute module with paired side electronics |
| Hyper Processor | 3×3 / 1.64 | 25 instructions/tick; original coolant rules | Dense multi-module housing, raised rear equipment |
| Memory Cell | 1×1 / 1.40 | 64 memory slots | Two storage cartridges with front module details |
| Memory Bank | 2×2 / 1.65 | 512 memory slots | Three substantial storage racks and more module detail |
| Logic Display | 3×3 / 1.64 | Native 80×80 framebuffer | Inclined console with a real bezel opening and live face |
| Large Logic Display | 6×6 / 2.78 | Native 176×176 framebuffer | Larger console/frame, with pitch fitted to existing height |
| Switch | 1×1 / 0.88 | Native enabled/config state | Mounted control handle and distinct ON/OFF pose/color |
| Message | 1×1 / 1.40 | Native message/printflush | Volumetric terminal housing, original identifying art |

Original palette and atlas textures are reused. Processor status reads native initialized/enabled state; memory lights indicate enabled state, not fabricated memory traffic. The switch reads the real native `enabled` flag and retains the normal click/configure interaction. Message text remains available through the original inspection/configuration UI; no fake programmable terminal text was substituted for it.

## Real screen contents and corrections

Z0 previously had no dedicated live-framebuffer face in its FPV block path. Upper floors already used native display buffers on generic roofs. Both paths now share the native display preparation adapter and dedicated console models. `ensureBuffer`, `processCommands` and `getBufferRegion` are vanilla APIs: the mod samples the **actual native framebuffer**, rather than copying pixels or implementing another display interpreter.

Native resolution, drawing coordinates, clear/update behavior, images, text, colors, shapes, lines and transforms remain native. The host-provided flipped framebuffer V region maps native +Y to the north/top edge (-render Z). The live face is square in its own surface space; its ground projection is intentionally foreshortened by the console pitch. Its shader marker preserves RGB independently of ambient terrain lighting, while ordinary model materials retain their existing lighting. The marker uses a bounded range because exact equality of interpolated floating-point values proved unreliable in the actual GPU test.

Actual low-precision-depth testing exposed interference between a full metal roof and live pixels, most visibly on the large screen. The final console has **an actual opening beneath its pixels and a four-sided bezel**, with no duplicate full roof or second top-art skin beneath the face. This removes the competing surface instead of relying on tiny offsets. A disabled/unprepared display has a plain dark face; it does not show decorative simulated content.

Original fog/team visibility gates remain in use; preparation also respects the native display-visibility option and conservative camera culling. Offscreen screens retain their existing buffers rather than reallocating as the camera moves. Removing an upper display releases the mod-owned GPU resources; closing the Z0 presentation cache leaves **vanilla-owned** buffers intact for ordinary 2D viewing. The prior Canvas/live-display GPU regression checks pass.

## Performance and caching

Nine profiles × three LODs share **27 immutable raw templates / 267,072 bytes of float data**. The ground material cache also contains nine builds / 267,072 bytes; the upper renderer owns a separate material cache. These figures exclude object overhead, native textures, transient batches and GPU/driver storage. Cache-build counts remain stable after warmup.

Each native display has a separate texture. The shared stream previously reserved 49,152 vertices (1,769,472 bytes of vertex float storage) for every texture, including a six-vertex display face. New live-screen streams reserve **6 vertices / 216 bytes** in both their CPU staging and GPU vertex capacity. Actual GL tests assert those capacities. Ordinary atlas streams keep their original capacity. This is a measured capacity reduction, not an FPS claim.

There is no per-frame model regeneration or CPU pixel readback/upload. Existing textures are sampled directly; native command drawing occurs only when a queue is nonempty. An unchanged screen does not trigger display-preparation GL state queries/flushes. Many changed screens share one state guard per renderer. Independent framebuffer textures still require separate draw submissions; no texture-array or rendering-engine rewrite was introduced.

| Model | Near vertices | Medium vertices | Far vertices |
|---|---:|---:|---:|
| hyper-processor | 588 | 588 | 132 |
| large-logic-display | 144 | 144 | 84 |
| logic-display | 144 | 144 | 84 |
| logic-processor | 408 | 408 | 132 |
| memory-bank | 1056 | 1056 | 156 |
| memory-cell | 486 | 486 | 126 |
| message | 156 | 156 | 96 |
| micro-processor | 288 | 288 | 132 |
| switch | 288 | 288 | 288 |

These retained material-template counts include both cached switch poses, even though only one is submitted. Live screen faces add six vertices each; counts are not draw calls. Triangles equal submitted vertices / 3.

### Matched renderer measurements

Sequential stock Steam Java 25 runs used 960×600 and two warmed 64-frame blocks per state, with no concurrent test/compiler/game processes. Each fixture contains **72 independent displays**, paired native processors and the other logic models. Native program simulation runs outside the timed renderer interval; the game is paused. GPU queries are asynchronous and captures/readbacks are outside timed frames. Native 2D display drawing is disabled outside the custom pass to avoid duplicate command processing.

| State | CPU render ms, before → after | GPU ms, before → after | Draw calls, before → after | Vertices, before → after |
|---|---:|---:|---:|---:|
| dense-idle | 1.166 → 1.337 | 0.113 → 0.136 | 62 → 126 | 159,425 → 168,251 |
| dense-active | 0.846 → 1.357 | 0.108 → 0.405 | 62 → 126 | 159,425 → 168,269 |

Baseline is **not feature-equivalent**: it renders live upper screens but lacks Z0 live faces in FPV. New costs include those newly functional screens and the modeled bodies. All raw two-block values, actual buffer populations/color storage and native command-update counters are recorded in the validation JSON. Current idle blocks have **zero native display updates, zero buffer creations and zero texture uploads** after warmup; active drawing also needs no CPU texture upload. Stock Java 25 allocation counters were unavailable (-1, not zero); no total allocation/FPS improvement is claimed. This is a short local renderer comparison, not full gameplay/cross-platform performance acceptance.

## Validation

- **201 full test programs PASS on Java 17 and the same 201 PASS on stock Steam Java 25.** Existing construction pause/priority, volume clipboard, campaigns, networking/prediction/identity, combat, unit models and platform/input regressions are included.
- New native checks: **773,413** geometry/XYZ/UV/state/cache checks in **216 poses**, and **99** actual native processor/read/write/control/draw/message/save/floor checks. Hyper Processor tests supply its original required coolant. Native logic, instruction rates, memory and costs were not altered.
- **31 actual SDL/OpenGL scenes / 3,968 frames per runtime, zero GL errors**: top/low/oblique views, all nine types, independent/changing screens, inactive state, Z0/Z1/Z3/Z5, stacked/below, adjacent/uneven terrain, real authorized placement, SaveIO reload, real 3D paste and preview. Each run recorded 7,347 authorized fixture placement calls.
- **35 actual GPU pixel/command/UV/color/resolution/cache/ownership/capacity checks**, plus **7 previous Canvas/display GPU checks**, PASS on both runtimes. The program visibly and natively draws text, an item icon, lines and colored shapes. Screenshots and readbacks are test-only.
- Independent fresh compilation matches **1,060 runtime classes byte for byte**, with matching resources. Artifact scope audit finds **1,162 previous runtime classes unchanged**, no removed entries, unchanged metadata/licenses/dependencies and class versions compatible with Java 17. Actual strict JAR/folder fingerprint agrees on both runtimes.
- The source archive is exported from the final local checkpoint. All 2,191 protected previous artifacts remain unchanged; primary profiles/saves are untouched. Test profiles, raw Steam logs, saves and host-game binaries are excluded from the delivered kit.

## Existing limitations and owner acceptance

**3D clipboard programs/links:** its existing explicit `safeConfig` policy deliberately omits processor code/configuration and emits a warning. This pass preserves that authority/safety policy. Model copies work; copied display/processor pairs were tested working after ordinary native configuration. Automatic copying of processor programs and logic links is **not implemented or claimed**. No cross-floor link permission or new clipboard/save/protocol format was introduced.

**Saving pixels:** vanilla does not serialize framebuffer pixel contents. Existing saved programs/links/memory survive, and active processors redraw after reload. A paused/static drawing that never redraws may be blank after loading, as in vanilla. The large display requires its existing 2.78-tile physical headroom; fixture stacks respect that rather than weakening physics. Small screens were stacked at Z1/Z3/Z5; large same-XY screens use Z1/Z5, with the Z3 large screen offset horizontally.

Physical Windows controls/import-and-restart, Linux gameplay, current physical campaign transitions and two-computer multiplayer acceptance were **NOT TESTED** here. All actual graphics runs were on Windows, using both runtimes. Existing remote upper logic replay/synchronization behavior is preserved and is not newly certified. Use matching diagnostic JARs on peers. No new controls were added; F5, Tab, construction, selection and normal logic configuration retain their existing bindings.
