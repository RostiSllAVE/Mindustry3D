# A2-16b — Final test matrix

| Check | Result | Evidence / limitation |
| --- | --- | --- |
| Complete Java 17 regression | PASS | 171 programs against final source |
| Stock Steam Java 25 regression | PASS | Same 171 programs against final packaged JAR |
| Profile/geometry cache | PASS | 944,915 bounds/height/finite/mask/LOD/cache assertions; redundant corner faces removed |
| Actual native state / layered cargo | PASS | 459,226 checks across eleven types, Z0–Z5, rotations, three LODs; read-only native save/health/inventory and actual upper transport |
| Server-authorized platform placement | PASS | Eleven types placed on real supported footprints Z1–Z5 using existing permitted Free Mode |
| Volumetric copy/paste | PASS | 208 checks: 55 upper blocks/platforms captured/rotated/previewed, actual queue builds 55 copies, original 55 retained, native filter config kept |
| Native/layered save state | PASS | Native serializers and actual layered restore; unchanged save format |
| Actual SDL/OpenGL Steam25 | PASS | 3,840 final-JAR scripted frames, zero GL errors |
| Actual SDL/OpenGL Java17 | PASS | 3,840 final-JAR scripted frames, zero GL errors; model cache statistics match |
| Connected upper lines / real items | PASS scripted | Actual native update methods and inventories on Z0 and every Z1–Z5 platform; normal native backpressure permitted |
| Corners / crossing / dense routing / large neighbor | PASS scripted | Original blend art, read-only cargo positions, Junction crossing and Router/Distributor/Sorter area |
| Upper side/underside views | PASS scripted | Correct base planes and opaque platform depth; no depth-buffer bypass |
| Hologram / clipboard / unfinished block | PASS scripted | Existing transparent, oriented ghosts, multi-floor preview and native ConstructBuild at partial progress |
| Actual disk save/reload | PASS scripted | Disposable SaveIO file only; renderer/native bodies recreated |
| F3 / priority / F5 / Tab / building/picking contracts | PASS automated | Existing construction, mouse/camera/control regressions; physical input acceptance separate |
| Original mining/combat/physics/campaign/network | PASS automated | Existing full regressions; only render integrations changed |
| Source/JAR identity / notices | PASS | 973 freshly compiled classes and resources byte-identical; notices/JNA unchanged |
| JAR/folder multiplayer fingerprint | PASS | Actual strict Java identity matches on Java17 and Steam25; no protocol/validation edits |
| Old releases / diagnostics | PASS | 2,065 Alpha files and 84 earlier diagnostic assets retain hashes |
| Performance | MEASURED | Whole-renderer before/after comparison; 1,024-case adds 2.53 ms CPU / 1.24 ms GPU; not gameplay FPS parity |
| Owner physical FPV/Tab/mouse/visual acceptance | NOT TESTED | Ten checklist steps supplied |
| Physical Linux / separate clean PC | NOT TESTED | Shared Java path and release-17 compatibility do not certify device graphics/input |
| Actual two-computer multiplayer | NOT TESTED | Automated authority/fingerprint fixtures do not replace physical sessions |

Both full suites were repeated for the final geometry after removing redundant
routing corner posts. Early local fixtures caught test-only API/range/setup errors;
those are not presented as final evidence. The final isolated scene snapshots are
taken after explicit native simulation updates, so their invariance assertion tests
the renderer itself. All final graphic results refer to `final-*` with the `ready/`
JAR, not earlier candidate runs. Only the lab SmokeHarness entry point is replaced
in isolated fixture copies; the owner JAR is unmodified.
