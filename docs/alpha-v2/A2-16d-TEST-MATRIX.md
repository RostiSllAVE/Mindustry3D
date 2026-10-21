# A2-16d — Test matrix

PASS means the stated automated or scripted check passed, not physical owner
acceptance. Primary profiles/saves are not used.

| Check | Result | Evidence / boundary |
| --- | --- | --- |
| Installed native definitions | PASS automated | All six requested GenericCrafter blocks; Silicon Crucible retains AttributeCrafter subtype and original size |
| Geometry/material/LOD/cache | PASS automated | Finite bounded triangle streams; atlas-safe crop/sampled-side mapping; three cached LODs, six type entries |
| Active and inactive animation | PASS automated | Native progress/warmup/totalProgress, disabled/no efficiency/output full, fixed press attachment and bed clearance, no global-time mixer motion |
| Read-only rendering and production | PASS automated | Serialized state unchanged by drawing; installed native crafting produces each output; 25 render calls cannot consume or generate items |
| Z0 and upper floor rendering | PASS automated | Z0/Z1/Z3/Z4/Z5; four stored rotations and all LODs; exact original footprint/height |
| Valid layered placement | PASS automated | Server-authorized Free Mode, real platform footprints and live native factories on Z1–Z5; no direct grid injection |
| Stacked manufacturing/headroom | PASS automated | Valid Z1/Z3/Z5 same-X/Y stack; adjacent ceiling intersection rejected |
| 3D clipboard and actual construction | PASS automated | 48 source factories captured once; encode/decode/rotation, valid preview, existing scheduler completes 48 copies without deleting sources |
| Runtime state not copied | PASS automated | Pasted production progress/inventories remain empty; no duplicated resources |
| Save/replication | PASS automated | Existing layered state restoration retains all 96 source/copied factories |
| Construction/logistics/authority/input | PASS automated | Existing complete regressions retained; physical controls acceptance separate |
| Native recipes/costs/physics/protocols | Unchanged | Runtime diff limited to rendering integration and new visual classes |
| Full Java17 regression | PASS | 176 programs, final source |
| Stock Steam Java25 regression | PASS | Same 176 against final packaged JAR, no java.logging requirement |
| Actual Windows SDL/OpenGL | PASS scripted | 3,072 final frames per runtime +3,072 baseline, 24 scenes, zero GL errors |
| Native output and belts | PASS scripted | Ground/upper crafter output and actual items on neighboring belts in all three fixtures |
| Disk SaveIO reload | PASS scripted | Disposable profile/map; models and native state remain valid |
| Source/JAR/payload identity | PASS | 984 independently compiled classes/resources; strict JAR/folder fingerprint equal on both runtimes |
| Dependencies and previous artifacts | PASS | GPL/notices/JNA unchanged; class target ≤61; 2,065 Alpha files +93 previous diagnostic assets retain hashes |
| Dense renderer comparison | MEASURED | 100/1,024 mixed factory fields, CPU/GPU/draw/vertex/cache data in report/JSON; no gameplay FPS claim |
| Physical owner gameplay/visuals | NOT TESTED | Three checks in manual procedure |
| Physical Linux or independent machine | NOT TESTED | Portable shared code and Java compatibility are not physical GPU/input certification |
| Actual two-computer multiplayer | NOT TESTED | Existing authority/identity checks do not replace a remote session |

Final full-suite, source/payload, actual OpenGL and performance results are appended
by the validation summary and recorded in `A2-16d-VALIDATION.json`.
