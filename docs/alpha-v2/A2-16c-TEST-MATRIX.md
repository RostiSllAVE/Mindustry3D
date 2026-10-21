# A2-16c — Validation matrix

| Check | Result | Evidence / boundary |
| --- | --- | --- |
| Full Java 17 regression | PASS | 174 programs, final source and diagnostic JAR |
| Stock Steam Java 25 | PASS | Same 174 programs against the final packaged JAR, no java.logging module required |
| New geometry/native presentation | PASS | 919,319 assertions: five actual block types, three LODs, finite bounded/cached templates, native links and read-only serialized state |
| Floors and orientation | PASS automated | Z0/Z1/Z3/Z5, all four rotations; correct floor-local bridge/driver lookup, same-XY ground target rejected |
| Invalid links | PASS automated | Disconnected, enemy, diagonal, over-range and native reverse/double links rejected |
| Unloader filters | PASS automated | Both Unloader/Duct Unloader preserve native selected-item tint and configuration; prior Item Source/Unloader color-clear regression retained |
| Authorized construction/copy/paste | PASS | 143 new assertions: five blocks across Z1–Z5, all platform footprints, 25 captured blocks, native filters, rotated clipboard metadata, 25 queued completed copies, original 25 retained |
| Native driver transport | PASS automated | Existing 4,109-assertion layer fixture verifies real launch, receipt, repeated transfer, removal safety and no wrong-floor delivery |
| Existing native item logistics | PASS automated | Existing complete native-family/bridge/unloader/authority/state regressions; rates and capacity source unchanged |
| Native/layered save and replication | PASS | Actual multi-floor state restoration retains all 50 source/copied blocks and supported filter configuration |
| Construction cancellation/priority/F3 | PASS automated | Prior regressions retained; physical input acceptance separate |
| F5/Tab/mouse/camera/combat | PASS automated | Existing complete regression suite; no input/controller changes |
| Source/JAR correspondence | PASS | 979 independently compiled project classes and resources byte-identical |
| Runtime change audit | PASS | Only three existing rendering source classes change; new advanced renderer/templates. 14 existing class entries change (including nested debug line metadata), 6 new entries, 1,084 old classes unchanged |
| Licenses/dependencies | PASS | GPL, third-party notices, licenses and JNA payload unchanged; class target ≤61 |
| Strict JAR/folder multiplayer identity | PASS | Actual ModFingerprint agrees on Java17 and Steam25; protocol unchanged |
| Prior artifacts | PASS | 2,065 Alpha files and 90 earlier diagnostic assets retain hashes |
| Actual Windows SDL/OpenGL | PASS scripted | 4,864 frames per Java17/Steam25, zero GL errors, stable caches; owner JAR unchanged except isolated harness overlay |
| Actual native cargo observation | PASS scripted | Native bolt/upper pod and real receiver inventory for drivers and both bridges in all three graphics fixtures; powered native methods outside timed drawing |
| Explicit vertical bridge edge visuals | NOT TESTED graphically | Existing registry read-only path retained; same-floor upper/stacked links and floor isolation tested separately |
| Actual disk save/reload | PASS scripted | Disposable native SaveIO file; rendering preserves native world state |
| Renderer cost | MEASURED | Same-fixture before/after timings and draw/vertex/cache counts in report/JSON; no FPS claim |
| Owner physical gameplay/menu/input | NOT TESTED | Three steps supplied separately |
| Physical Linux or independent machine | NOT TESTED | Shared portable code/class target do not certify physical GPU/input behavior |
| Real two-computer multiplayer | NOT TESTED | Automated authority/identity checks do not replace remote sessions |

Actual SDL/OpenGL scripted scenes, real cargo observations, geometry/cache cost and
before/after measurements are recorded in the report and `A2-16c-VALIDATION.json`.
All test profiles/worlds are disposable. The fixture replaces only SmokeHarness in
isolated copies; the importable owner JAR retains its normal entry point. No primary
save/profile is copied or changed. Physical acceptance remains the owner's task.
