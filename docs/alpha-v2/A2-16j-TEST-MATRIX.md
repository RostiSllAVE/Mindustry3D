# A2-16j — Test matrix

Automated tests run on Windows with native Mindustry160.7 classes. Full runtime: Java17.0.20.1; stock Steam runtime: Java25.0.1. Physical acceptance is not inferred from scripted screenshots.

| Area | Result | Evidence / limit |
|---|---|---|
| Entire regression suite | PASS, 194 programs on each runtime | Existing connection/prediction, campaign, building/priority/cancellation, selection/copy, combat/input/model regressions retained |
| Native models, materials and state | PASS, 36,808,066 checks / 2,016 poses on each runtime | All six cores + Radar; native footprints/HP/capacity/cost references; team states, rotations, 3 LODs and several base elevations; no save/HP/inventory/team registration mutation |
| Core input/removal and original unit spawn | PASS, native API fixtures | All six original core unit types; exactly one native spawn per invocation; rendering cannot spawn or spend again |
| Paid upgrade and native core-zone gate | PASS, native APIs | Shard → Foundation consumes each requirement once; empty ordinary ground vs complete native core zone checked. Other paid upgrade pairs not separately exercised |
| Real native model rendering | PASS, 27 scenes / 3,456 frames on each runtime | All six individual cores, above/oblique/low views, 3 team colours, Radar on/off, real native spawn/damage/kill/upgrade calls, landing timer, adjacency and openings; 0 OpenGL errors |
| Elevated infrastructure | PASS, server-authorized fixtures | Illuminator/Radar Z1/Z3/Z5; 48 direct upper utility placement calls per gallery, plus actual 3D utility capture/paste; native placement protocol unchanged |
| Upper core geometry | PASS, inert adapter fixtures | Uses existing upper adapter objects, not new placement permission. Existing authoritative upper-core inventory and XYZ respawn regressions also pass |
| Actual save/reload | PASS, disposable native SaveIO | All six core definitions and layered utilities restored; primary saves untouched |
| Campaign lifecycle | PASS, existing automated fixtures | Current physical sector-transition acceptance NOT TESTED |
| Fog/ownership | Existing gates preserved; regression suite PASS | No render-time fog/target/registry updates. New physical hidden-enemy campaign scenario NOT TESTED |
| Performance and LOD | MEASURED | Serial matched Steam25 baseline/final renderer timings, draw calls and vertex counts; additional dense-scene cost disclosed in report; no sustained FPS claim |
| Cache/geometry | PASS | 7 materialized types; 21 shared raw templates; stable-scene generation counts do not increase; native textures/UVs reused |
| Source correspondence | PASS | 1,041 freshly compiled runtime classes byte-identical, packaged resources match |
| Multiplayer identity | PASS, actual JAR/folder fingerprint calls | Same payload digest on Java17/25; protocol4/strict compatibility code unchanged; actual two-computer gameplay NOT TESTED |
| Linux and physical FPV/F5/Tab | NOT TESTED physically | Shared Java17-targeted JAR, no new platform dependency; existing automated input tests pass |
| Remote upper Radar clock | NOT CERTIFIED | Only native host/local phase/readiness verified; existing upper snapshots omit some visual clock fields; no protocol expansion in this pass |
| Existing releases | PASS | 2,182 previous artifact files rehashed; no overwrites |

Screenshots use the actual production renderer and host atlas. The isolated harness replaces only SmokeHarness.class inside fixture copies, never in the delivered JAR. Evidence excludes raw Steam logs, profiles, saves, host executables and app IDs. See the coverage inventory for deferred models and the manual for three owner steps.
