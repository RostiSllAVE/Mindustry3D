# A2-16a — Test matrix

| Test | Result | Actual scope |
| --- | --- | --- |
| Java 17 full suite | PASS | 167 test programs; release-17 compilation |
| Stock Steam Java 25 full suite | PASS | Same 167 programs against the packaged JAR |
| Shared model geometry | PASS | 36,338 bounds, normals/UV, cache identity and LOD checks |
| Native drill state/geometry | PASS | 207,596 actual native-content, emitted CPU vertices, Z0/Z2, orientation, inactive states, cache, native serialization and production checks; mock GL |
| Real atlas proportions/materials | PASS | Four native profiles, original region sizes, all parts in their host texture batch |
| Actual SDL / OpenGL on Steam Java 25 | PASS | Exact final JAR with lab-only SmokeHarness overlay; 1,792 frames, zero GL errors |
| Actual SDL / OpenGL on complete Java 17 | PASS | Same fixture, 1,792 frames, zero GL errors; model statistics identical |
| Working / idle animation | PASS | 1,698 changed operating pixels, zero changed idle pixels in each runtime |
| Native simulation state unaffected by rendering | PASS | Headless field/inventory/progress assertions plus actual tile/entity invariants |
| Top / side / oblique / low FPV-like camera | PASS | Scripted actual renderer; not physical FPV input acceptance |
| Upper floors / foundation alignment | PASS | Four live upper native drills plus platforms; unchanged placement/physical heights |
| Disk save/load / map changes | PASS | Actual disposable SaveIO round trip and 96→224 fixture-map transitions; native campaign lifecycle regressions also pass |
| Large fields | PASS with cost noted | 100 and 1,024 native drills; stable cache, measured CPU/GPU/GL calls; dense close-up costs more |
| Building / priority / F3 / schematic / XYZ picking | PASS | Existing native automated regressions; no new input bindings |
| Multiplayer safety | PASS automated | Existing authoritative/network regressions and real JAR/folder fingerprint match on both runtimes; no new protocol |
| Source/JAR identity and licenses | PASS | Freshly recompiled project classes/resources match; notices/JNA unchanged |
| Previous release preservation | PASS | 2,065 Alpha files and 78 previous diagnostic artifacts retain hashes |
| Physical owner visual/flight/Build acceptance | NOT TESTED | Owner checks listed separately |
| Physical Linux, separate clean PC | NOT TESTED | Portable shared Java code; no Linux device available for this pass |
| Real two-computer multiplayer | NOT TESTED | No multiplayer gameplay acceptance inferred from fixtures |
| Same real gameplay FPS as baseline | NOT TESTED | Renderer timing is not full simulation/display FPS; close stress cost is explicitly reported |

Preliminary fixture work caught an uninitialized native CacheLayer in the mock
test and duplicate creation of an already listener-created upper building in the
lab. Both fixtures were corrected. Close-up inspection also corrected the first
candidate's stretched part sizes before final testing. Preliminary diagnostic
outputs remain local and are not the final test JAR. The report and validation
refer to `verified/` and `checked-graphics` / `checked-java17`, not early candidates.
