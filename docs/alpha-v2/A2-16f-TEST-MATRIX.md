# A2-16f — Test matrix

Baseline: `195cb152c66679f5a46c8850ca85abf2c3e85a92` (A2-16e).
The owner accepted the previous pass. New physical acceptance is not implied.

| Check | Result | Evidence / limits |
| --- | --- | --- |
| Eight installed native block definitions and footprints | PASS automated | Generic/AttributeCrafter, SolidPump and Fracker; original physical heights |
| Three LODs, shared geometry, finite transforms and atlas UVs | PASS automated | Native test covers all eight, four orientations, Z0/Z1/Z3/Z4/Z5 and multiple animation phases |
| Actual liquid identity/levels and missing/non-finite fallback | PASS automated | Distinct water/oil amounts, empty state and missing module; bounded transformed meshes |
| Rendering cannot produce resources or advance native clocks | PASS automated | Native serialized state and explicit pump clock compared before/after draws |
| Native production | PASS automated | Actual `updateTile` with controlled native inputs; no reimplementation of recipes; not throughput/endurance testing |
| Authorized upper placement, invalid ceilings and 3D clipboard | PASS automated | 64 source machines, 64 built copies, same-X/Y Z1/Z3/Z5, state roundtrip; approved Free Mode still validates geometry |
| Full Java17 regression | PASS | 180 programs, final source |
| Stock Steam Java25 regression | PASS | Same 180 against final packaged JAR |
| Actual Windows SDL/OpenGL | PASS scripted | 3,456 final frames per runtime +3,456 baseline; 27 scenes, zero GL errors |
| Native output and belts | PASS scripted | Per-type ground/upper production and real items on ground/upper belts |
| Disk SaveIO reload | PASS scripted | Disposable map/profile, no primary saves |
| Source/JAR/payload identity | PASS | 997 independently recompiled classes, resources and strict folder/JAR fingerprint on both runtimes |
| Prior artifacts/licenses/dependencies | PASS | 2065 Alpha files +99 diagnostic assets preserved; unchanged notices/JNA |
| Dense-field performance | MEASURED | Whole-renderer CPU/GPU, draw/vertex/cache data; no physical FPS claim |
| Owner physical gameplay | NOT TESTED | Three actions in manual test file |
| Physical Linux graphics/input | NOT TESTED | Shared Java17 bytecode is not Linux device acceptance |
| Real remote multiplayer | NOT TESTED | No second computer/network gameplay claim; regression and strict payload checks separate |
| Gameplay FPS parity | NOT TESTED | Whole-renderer CPU/GPU measurements reported separately |
