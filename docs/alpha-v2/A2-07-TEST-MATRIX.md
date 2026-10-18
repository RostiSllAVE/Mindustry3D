# A2-07 — Test matrix

PASS is bounded by the evidence below. Native headless fixtures are not physical gameplay.

| Check | Result | Evidence / boundary |
|---|---|---|
| Regression reproduces A2-06 defect | CONFIRMED FAIL BEFORE FIX | Same new native test fails after Flare's initial 3-projectile burst |
| Deterministic before/after attack measurements | PASS / MEASURED FIXTURE | 16 rows: four ground blocks, 2D/3D before/after; up to 2,400 ticks, stop on death |
| Vanilla 2D behavior | PASS | All four before/after 2D measurement rows exactly equal |
| Wall, Duo, factory, core damage | PASS | Actual native weapon/projectile contacts reduce HP; repeated runs, native metadata unchanged |
| Original AI acquisition and target switch | PASS | Unseeded FlyingAI with real native building index; target removed after actual damage; next target damaged |
| Solid foreground obstacles | PASS | Real projectile damages/contacts upper barrier; target HP unchanged until barrier removed |
| Different aircraft heights and descent | PASS | Height2/4 velocity fixtures and initial height6 normal altitude descent/attack run |
| Near-vertical unreachable target | PASS | No shots/HP loss when actual mechanical pitch stop excludes target |
| Elevated target | PASS, LIMITED | Real layered Duo/native AI objective receives damage in correct upper volume, no Z0 building; objective supplied, upper-only discovery not expanded |
| Manual FPV aim | PASS, FIXTURE | Explicit manual controller/provider; same barrel pitch and actual HP damage; no physical mouse/camera test |
| Zenith, Antumbra, Eclipse ground attacks | PASS | Actual native AI/weapons cause damage to real core |
| Aircraft / bomb / gimbal / mount regressions | PASS | Existing 920-check native test across six aircraft, plus existing visual/math and weapon-polish tests |
| Native heading / ordinary-aircraft / altitude invariants | PASS | 3,999 altitude-model checks, including 722 new yaw/pause/height/climb/bank checks |
| Native firing permissions | PASS | Hold fire, friendly target rejection and minimum shooting velocity retained |
| Unit ammunition | NOT APPLICABLE, HOST | Stock160.7 has no Unit.ammo/Rules.unitAmmo API; no invented ammo rule or weapon condition bypass |
| Client/server damage authority | PASS, LOCAL FIXTURE | Non-authoritative client applies no damage; server role + native NetServer receives real building damage; no socket transport |
| Full Java17 suite | PASS | 128 programs, final acceptance build |
| Stock Steam Java25 suite | PASS | Same 128 programs, final JAR; no java.logging, existing native-access permission |
| JAR / folder fingerprint | PASS | Actual Java ModFingerprint; same canonical identity; strict mismatch logic untouched |
| Scope / platform packaging | PASS, STATIC | Input/network/render/damage/save classes unchanged; Java17 bytecode and existing Windows/Linux x64 resources/notices |
| Previous release preservation | PASS | 2,065 Alpha snapshot files and 15 A2-02–A2-06 diagnostic artifacts hash-identical |
| Real Windows/Linux gameplay, AI combat, FPV/camera | NOT TESTED | Owner physical test pending; Linux A2-06 capture acceptance remains pending too |
| Real local host/client or two computers | NOT TESTED | Socket-selector preflight IOException, no permission bypass; fixtures are separate evidence |
| New universal altitude/approach planner | NOT IMPLEMENTED | Not needed for confirmed cause; impossible angles/occluded targets still cannot produce guaranteed hits |

See [report](A2-07-FLYING-AI-REPORT.md), [CSV](A2-07-ATTACK-MEASUREMENTS.csv),
[machine evidence](A2-07-VALIDATION.json) and [owner test](A2-07-MANUAL-TEST.md).
Final raw logs remain under `local/a2-07/`, outside the source/release payload.
