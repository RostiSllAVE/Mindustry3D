# A2-16e — Validation matrix

Automated/scripted PASS is distinct from physical gameplay acceptance. Primary
profiles and saves are not used.

| Check | Result | Evidence / boundary |
| --- | --- | --- |
| Actual 160.7 definitions | PASS automated | Six exact names/sizes/craft times; GenericCrafter vs Separator retained; native weighted result tables and spinner speed checked |
| Geometry/materials/LOD | PASS automated | Cached finite templates and actual submitted XYZ/UV bounds, including moving rods, shuttle, pivot-centered drums and native alpha details |
| Active/inactive states | PASS automated | Real progress/warmup/clock, disabled/no efficiency/output full, native-clock motion; global render time alone cannot move machinery |
| Read-only production/RNG | PASS automated | Native serialization, resources, progress and seed unchanged by drawing; repeated render calls cannot produce, consume or roll output |
| Z0 / upper presentation | PASS automated | All six on Z0/Z1/Z3/Z4/Z5, four stored rotations and all three LODs |
| Valid upper construction | PASS automated | Approved Free Mode, actual slabs/native buildings on Z1–Z5; no direct world-grid injection |
| Stacked floors/headroom | PASS automated | Valid same-X/Y Z1/Z3/Z5; immediate ceiling intersection rejected |
| 3D clipboard and builder | PASS automated | 48 originals → 48 actual completed copies; source retained, metadata rotation/encoding and preview valid |
| No runtime state duplication | PASS automated | Empty pasted inventories/progress; source separator RNG stream not copied |
| Save/replication | PASS automated | Existing restoration retains all 96 factory placements |
| Prior manufacturing models | Preserved | Their source implementation is untouched; previous tests retained and binary identity checked below |
| Recipes/resources/physics/input/protocols | Unchanged | Runtime modifications limited to two renderer integrations plus new visual classes |
| Full Java17 regression | PASS | 178 programs, final source |
| Stock Steam Java25 regression | PASS | Same 178 against final packaged JAR, no java.logging requirement |
| Actual Windows SDL/OpenGL | PASS scripted | 3,200 final frames per runtime +3,200 baseline, 25 scenes, zero GL errors |
| Native output and belts | PASS scripted | Ground/upper crafter output and actual items on neighboring belts in all three fixtures |
| Disk SaveIO reload | PASS scripted | Disposable profile/map; models and native state remain valid |
| Source/JAR/payload identity | PASS | 990 independently compiled classes/resources; strict JAR/folder fingerprint equal on both runtimes |
| Dependencies and previous artifacts | PASS | GPL/notices/JNA unchanged; class target ≤61; 2,065 Alpha files +96 previous diagnostic assets retain hashes |
| Dense renderer comparison | MEASURED | 100/1,024 mixed factory fields, CPU/GPU/draw/vertex/cache data in report/JSON; no gameplay FPS claim |
| Owner physical gameplay | NOT TESTED | Three checks in manual procedure |
| Physical Linux/independent machine | NOT TESTED | Portable Java tests do not certify GPU/input behavior on another machine |
| Real two-computer multiplayer | NOT TESTED | Automated authority/identity tests do not substitute for an actual session |
| Dense repeat | MEASURED | Additional 256 frames per build, zero GL errors; near-field GPU cost reproduced, distant LOD bounded; no extra save/load claim |
| Real-game FPS parity | NOT TESTED | Controlled per-renderer measurements are reported separately |

Final regression, artifact, actual OpenGL and performance evidence is appended
after validation and stored in `A2-16e-VALIDATION.json`.
