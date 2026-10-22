# A2-16g — Power infrastructure and animated XYZ connections

Baseline: completed A2-16f `3636d2d0d3fe0a1413848df2e7e1fe93fd502a7f` on `main`.
Local diagnostic only. A2-16f and this pass still need owner physical acceptance.

## Models and native state

All fourteen prioritized blocks receive explicit cached models; details are in
`A2-16g-MODEL-COVERAGE.md`. The implementation extends the accepted mechanical
primitives, native-atlas material stream, decorative resource foundations and
18/50-tile LOD convention. Static templates are shared by profile/size/LOD;
atlas-dependent material copies are cached per type and owning renderer. No new
textures, external files or native libraries are required. Ground and upper
renderers dispatch the same models and omit duplicate generic bodies.

Batteries use the actual buffered `power.status` fraction for side/top indicators.
No charge or stored-energy value is estimated. Generators use their native
`productionEfficiency` and enabled state. ConsumeGenerator `totalTime` and Impact
Reactor `totalProgress` drive moving mechanisms; the default Building clock is
intentionally excluded because it can return global render time. Solar, RTG,
reactor and node indicators do not fabricate mechanical production cycles.
Diode uses its original separately loaded arrow texture with native rotation;
its transfer logic is not called by rendering. Missing state yields a dark
indicator. Native warmup can coast; drawing cannot advance the underlying clocks.

The code never invokes generator updates, fuel/coolant consumers, charge transfer,
heat calculations or reactor explosions. Controlled native update methods are
used only by disposable tests outside drawing, with prescribed stock/power.
Their production observations are not a throughput or autonomous startup test.

## One connection presentation

The audit found two separate old paths: streamed Z0 node lines and cached upper
connection ribbons. They now share one power-only presentation pass. Unrelated
weapon lasers, bridges and liquid connections retain their existing paths.

The collector reads native PowerNode `power.links` and the existing authorized
`LayerBuildingAdapter.powerLinks()` collection. It does not propose, configure,
connect or prune gameplay links. Reciprocal edges are deduplicated by canonical
endpoint IDs. Ground IDs retain unsigned packed XY identity; positive IDs resolve
actual layer components/native buildings. Dead, replaced, foreign, payload and
hidden endpoints are filtered. Current world identity and edge signatures
invalidate meshes after configuration, removal, destruction, restore or world
changes. A small O(buildings + edges) read-only scan detects native packed-link
changes, including changes not accompanied by a tile replacement. Unchanged
topology does not regenerate geometry or allocate per-link objects each frame.

Each endpoint uses actual native X/Y, the component's real construction plane and
a model-profile terminal height inside the original physical envelope. Other
legitimate power consumers use their existing roof height. Endpoint geometry is
camera-independent, including exactly vertical runs. Existing range, insulation,
XYZ line of sight, team permissions and server validation remain authoritative.
Opaque geometry hides occluded beam segments; no depth clearing or x-ray pass is used.

Cached four-sided tubes form a narrow core and restrained transparent envelope.
Two opposing traveling highlights are combined symmetrically. A deterministic
coordinate phase offset avoids all links blinking together. Shader time changes
brightness without rebuilding vertices. These pulses visualize connectivity,
**not measured watts or a claimed flow direction**. Native node laser colors and
the host laser-opacity preference are respected. A valid unpowered link may
still be visible, as with vanilla connection visualization.

Up to 64 links share each mesh batch. Coarse batch bounds are frustum/far-plane
culled; partially visible batches are conservatively retained and clipped by the
GPU. Both tubes are depth-tested with depth writes disabled. The core uses normal
alpha blending; the envelope is intentionally faint. Depth, culling, blend state,
blend factors/equations and the prior shader program are restored. There are no
per-link particles, synchronous per-frame GPU readbacks or topology rebuilds
caused merely by camera movement, battery charge or the graphics toggle.

## Setting and controls

**Settings → FPV flight → Animated power connections**, default **ON**, saved as
`fpv-animated-power-connections`. OFF keeps the narrow static core and omits the
animated envelope. The setting is local presentation only. Machine animations
still follow native state. No keys, mouse controls, network packets or host rules
are added or repurposed. Cosmetic connection pulses may continue while paused;
native mechanical clocks and simulation remain paused.

## Construction, saves and limits

All fourteen profiles are tested through actual authorized upper placements on
Z1–Z5 and valid Z1/Z3/Z5 stacks. The native fixture queues 112 copied power blocks,
retains 112 originals, preserves internal XYZ power edges and rejects intersecting
ceilings. It checks that copied batteries do not inherit stored energy. Existing
save/replication and clipboard formats are used unchanged. F3 interruption,
priority selection, costs and permissions remain covered by existing regressions.
Fixtures explicitly authorize sandbox Free Mode, without relaxing geometry or
connection checks. Owners must still provide legitimate supports and headroom.

All fourteen requested models are implemented. Unlisted Thermal Generator and
Erekir models remain deferred. The beam envelope is a lightweight tube, not bloom
or physically simulated plasma. Solar visual bounds remain smaller than their
unchanged existing collision envelope. Very large visible batches still submit
some offscreen links; individual visibility is conservatively handled by GPU
clipping. Native adjacency without an explicit node/XYZ link is not invented as
an extra visible cable. Physical Linux, remote multiplayer and owner input/visual
acceptance are separate from Windows scripted GL and native tests.

To rebuild: provide `FPV_JDK` (Java 17 JDK) and `FPV_MINDUSTRY` (separately
installed Mindustry 160.7), then run
`python tools/build-diagnostics.py --task a2-16g --run <new-local-run-name>`.
The normal builder runs the complete regression set and retains older artifacts.

## Final measured results

The final packaged JAR passed **183 regression programs on Java 17 and the same 183 on stock Steam Java 25**. Independent compilation verifies **1010 project classes** and all packaged resources byte-for-byte. The new native fixtures cover all fourteen models, charge/clocks, a real intermediate-layer insulating wall, accepted/rejected XYZ edges, native reconfiguration, destruction, copying and uncharged copied batteries. These are automated tests, not owner physical acceptance.

The identical actual Windows SDL/OpenGL fixture completed **46 scenes / 5,888 frames per runtime**, plus 5,888 baseline frames, with zero GL errors. All 46 direct power-pass state-restoration probes passed on each final runtime. Stable scenes do not regenerate model/link meshes; drawing preserves native state. A disposable disk SaveIO reload, native/upper generator activity, authorized Z1–Z5 placements and blueprint/clipboard scenes are included. Native stock/power is controlled outside timed rendering; this is not a startup, fuel-throughput or long-term reactor safety benchmark.

| Scene | CPU before → after, ms | GPU before → after, ms | Draws before → after | Vertices before → after |
| --- | --- | --- | --- | --- |
| node | 0.418 → 0.439 | 0.063 → 0.062 | 27 → 28 | 59189 → 61877 |
| surge | 0.384 → 0.481 | 0.065 → 0.065 | 33 → 34 | 68273 → 73115 |
| battery | 0.371 → 0.542 | 0.068 → 0.069 | 35 → 36 | 75425 → 83843 |
| large-solar | 0.338 → 0.639 | 0.072 → 0.090 | 44 → 45 | 88469 → 105179 |
| steam | 0.445 → 0.748 | 0.075 → 0.199 | 48 → 49 | 97031 → 124019 |
| thorium | 0.451 → 1.120 | 0.081 → 0.269 | 55 → 56 | 108077 → 152063 |
| impact | 0.500 → 1.058 | 0.081 → 0.167 | 55 → 56 | 109805 → 149489 |
| stacked | 0.382 → 0.923 | 0.186 → 0.332 | 52 → 53 | 107303 → 136595 |
| field100 | 1.109 → 1.575 | 0.260 → 4.285 | 33 → 33 | 121625 → 147947 |
| field1024 | 1.072 → 1.790 | 0.132 → 3.228 | 66 → 66 | 206039 → 228587 |
| links-stacked | 0.661 → 0.707 | 0.080 → 0.081 | 47 → 53 | 109889 → 120539 |

| Actual links | Static → animated CPU, ms | Static → animated GPU, ms | Added draws | Added vertices |
| --- | --- | --- | --- | --- |
| 10 | 0.423 → 0.416 | 0.075 → 0.074 | 1 | 240 |
| 100 | 0.720 → 0.673 | 0.101 → 0.101 | 2 | 2400 |
| 512 | 1.656 → 1.498 | 3.917 → 3.739 | 8 | 12288 |

The stacked power scene has **139 actual accepted links**, including native adapter auto-connections. The removal/reconfiguration scenes contain 9 and 8 remaining native edges, respectively. Static/animated toggles reuse the same topology meshes. There is one extra envelope draw per visible batch of up to 64 links, rather than one draw per beam. Both tubes together use 48 vertices / 1,728 vertex-buffer bytes per edge, plus chunk and driver overhead; OFF retains the cached envelope but skips its draw.

The measurements cover the whole renderer (terrain, machines, platforms and connections), with the same camera and distance LOD across the dense field. Additional model geometry has a cost; a single sequential run does not prove FPS parity or statistical CPU differences. No whole-frame zero-allocation claim is made. Java17 allocation data is in JSON; stock Java25 exposes no allocation counter here. GPU queries are asynchronous and skipped until available; no synchronous readback was added to runtime rendering.

The 42 shared raw templates (14 profiles × 3 LODs) occupy **680,832 bytes** of vertex data. One cold generation sample per profile/LOD is recorded, not a sustained benchmark. Near meshes range from 240 to 2,100 vertices. The materialized cache in this fixture holds **21,276 vertices / 680,832 bytes per owning renderer**, separate from shared templates and streaming buffers. Both runtimes agree. No per-frame leg/particle/mesh generation is introduced.

Final JAR SHA-256: `005a1d5e85bb820da088f30215257a21bf87415d2ca3452b668a759ee2fe6c7d`. 1,107 pre-existing class entries remain byte-identical, including dependencies and prior unit/factory implementations; 15 existing entries differ (the three scoped integration sources and their nested/debug metadata), and 13 entries are new. Licenses/native dependencies are unchanged. Strict payload fingerprints agree between JAR and folder on both runtimes. Hash audits preserve 2,065 Alpha files and 102 earlier diagnostic assets.

The prior-only screenshot comparison changed 1105 pixels, with bounds (292, 96, 408, 159); this is reported without inferring a gameplay regression from a pixel difference. Prior implementation class identity is checked independently. Owner visual/input acceptance, physical Linux, remote multiplayer and actual gameplay FPS remain NOT TESTED.

## Repeated dense-scene follow-up

The first full comparison showed a GPU increase of about 3–4 ms in dense scenes, on both final runtimes. This warranted a narrower repeated comparison before committing. The exact baseline/final binaries were tested with an identical separate four-scenario fixture, repeated four times on stock Steam25 (2,048 additional frames per binary, no GL errors, stable caches and 16 additional final GL-state probes). This follow-up deliberately does not perform or claim a save/reload.

| Scene | CPU median before → after, ms | GPU median before → after, ms | Final GPU range, ms |
| --- | --- | --- | --- |
| field100 | 0.980 → 1.394 | 0.157 → 0.440 | 0.383–0.685 |
| field1024 | 1.061 → 1.602 | 0.125 → 0.578 | 0.460–0.733 |
| links512-static | 1.071 → 1.514 | 0.149 → 0.474 | 0.417–0.681 |
| links512-animated | 0.959 → 1.666 | 0.150 → 0.562 | 0.445–3.858 |

New models have a measurable CPU/GPU cost. Most repeated dense GPU samples fall around 0.4–0.7 ms; one animated512 sample still reaches 3.86 ms. The earlier 3–4 ms increase is therefore not consistently reproduced, and its cause is **not established**. Both the initial comparison and every follow-up sample are retained. These measurements do not establish a sustained gameplay FPS regression or parity. The beam toggle adds at most eight envelope draws for 512 links in this fixture; its measured timing differences cannot be separated from timing variance with statistical confidence. Owner dense-factory FPS testing remains required. No speculative renderer rewrite was introduced in response to the timing variance.
