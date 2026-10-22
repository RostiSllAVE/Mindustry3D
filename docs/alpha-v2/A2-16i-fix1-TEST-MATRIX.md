# A2-16i-fix1 test matrix

PASS means the stated automated check ran, not owner acceptance.

| Check | Java 17 / Windows | Steam Java 25 / Windows | Physical Linux / owner |
| --- | --- | --- | --- |
| All existing + new test programs | 192 PASS, 0 FAIL | 192 PASS, 0 FAIL | NOT TESTED |
| Opaque mechanical roofs / seated vs airborne undersides | PASS, streamed vertex/material checks | PASS | NOT TESTED |
| Thin muzzle/edge contours; 72 native drawer yaw/pitch poses | PASS, 66 old culling failures reproduced | PASS | NOT TESTED |
| Seven native models; numeric atlas coverage for three turrets | PASS, final host atlas | PASS, zero missed opaque centres | NOT TESTED |
| Z0, Z1, Z3, Z4, Z5 and stacked geometry | PASS, scripted SDL | PASS, scripted SDL | NOT TESTED |
| Structure-on-structure and actual 3D clipboard queue | PASS, scripted SDL + native regressions; unsupported Free Mode undersides | PASS | NOT TESTED |
| Dense fields of 100/1,024 machines; mixed neighbors | PASS, scripted SDL | PASS, timed separate scenes | NOT TESTED |
| Native SaveIO save/reload in disposable profile | PASS | PASS | NOT TESTED |
| Real native cargo/arrival timing and stop/idle conditions | PASS, native tests + SDL | PASS | NOT TESTED |
| Force real projectile contact / depletion / recovery / floor fade | PASS, native authority + SDL | PASS | NOT TESTED |
| Gradient projections and overhead suppression | PASS, scripted SDL + defense tests | PASS | NOT TESTED |
| Selection, preview, priority, F3, F5/Tab/input regressions | PASS, automated fixtures | PASS, automated fixtures | Physical controls NOT TESTED |
| OpenGL errors and GL-state restoration | 0 errors, 6528 main frames | 0 errors, 6528 main frames | NOT TESTED |
| JAR/folder strict identity and matching fresh source | PASS | PASS | Physical multiplayer NOT TESTED |
| Previous Alpha/diagnostic assets | 2176 files unchanged | Same file hashes | No published updates |

Separate performance comparison and exact hashes: [REPORT](A2-16i-fix1-REPORT.md), [machine-readable validation](A2-16i-fix1-VALIDATION.json). GPL source, notices and evidence accompany the local kit. Earlier intermediate fixture failures were setup errors (missing mock liquid module/abstract draw override; an invalid pasted-unit position), corrected before final validation; they are not claimed as passes.
