# A2-16h — Test matrix

Baseline: owner-accepted A2-16g `b373e1a`. Automated fixtures are not physical
gameplay acceptance. Final measurements are in the report and validation JSON.

| Test | Status | Scope |
| --- | --- | --- |
| Thirteen installed profiles | PASS automated | Real native names, classes, footprints and capacities |
| Template/cache/UV/finite geometry | PASS automated | All masks/LODs, bounded shared parts, native atlas mapping |
| Actual XYZ model transforms | PASS automated | Z0/Z1/Z3/Z4/Z5, all rotations, several liquids/empty/full |
| Native corner and floor identity | PASS automated | Real corner, foreign neighbor, same-XY different-floor isolation |
| Read-only quantities/clocks | PASS automated | Serialized state, liquid fractions, unchanged native pump clock |
| Pump/thermal operation | PASS automated | Native water extraction and hotrock generation; dry terrain rejection |
| Upper construction and copying | PASS automated | 96 original +96 queued copies, empty copied storage and internal liquid bridge |
| Existing save/replication | PASS automated | Actual native/upper fixture roundtrip; full regressions separate |
| Java17 / stock Steam25 full regressions | PASS | 185 programs per runtime against final JAR |
| Actual Windows SDL/OpenGL | PASS scripted | 41 scenes /5,248 frames per final runtime +baseline; zero GL errors |
| Real pump/thermal native production | PASS scripted | Three ground pumps +thermal active; dry upper pumps produce nothing |
| Actual disk SaveIO / preview / bridge removal | PASS scripted | Disposable profile, current clipboard/ghosts and existing accepted links |
| Dense rendering / cache | MEASURED | Before/after 100/1,024 mixed blocks, actual CPU/GPU/draw/vertex/cache data; no FPS parity claim |
| Source / payload identity | PASS | 1016 independently recompiled classes/resources; strict JAR/folder identity on both runtimes |
| Previous assets / dependencies | PASS | 2065 Alpha files +105 diagnostic assets preserved; licenses/JNA unchanged |
| Focused Thermal Generator active/disabled/idle | PASS scripted | Same camera, actual hotrock generation with prescribed native flags; 384 extra frames per runtime, orange/dark indicators, no save/reload claim |
| Owner Windows gameplay/input | NOT TESTED | Follow three manual actions |
| Physical Linux / X11 / Wayland | NOT TESTED | Portable code paths, no Linux device acceptance claim |
| Real remote multiplayer | NOT TESTED | Previous authority/protocol retained; physical two-computer test pending |
| Sustained gameplay FPS | NOT TESTED | Scripted renderer timings reported separately |
