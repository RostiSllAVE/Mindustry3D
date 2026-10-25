# A2-16o — validation matrix

PASS below means the stated automated boundary, not physical gameplay acceptance. Final counts, hashes and timing values are included in the adjacent validation JSON.

| Check | Result / evidence |
|---|---|
| Standalone reference version/launcher/JRE | PASS: official 160.7; Packr loads `jre/desktop.jar`; bundled Java 25.0.1 |
| Steam independence | PASS: direct bundled Java and copied Packr EXE runs report `Vars.steam=false`; no Steam process launched |
| Isolated test data | PASS: each run has a new data/mod/tmp/APPDATA profile; one mod copy |
| Java 17/25 runtime selector and launcher syntax | PASS: six Python checks, PowerShell parser checks |
| Original reference files | PASS: 98-file SHA-256 snapshot, checked after execution |
| Existing artifact preservation | PASS: protected previous artifact hashes checked; no old release replacement |
| Old cross-floor factory-release reproduction | FAIL as expected on accepted baseline: clear XYZ exit vetoed by native XY occupancy |
| Fixed factory release, cap, duplicate dump | PASS: real native dump/add/event boundary, same-floor collision retained |
| All factories and reconstructors | PASS: actual production/costs/native state/rotation/XYZ fixtures, including 448 upgrade releases and 96 expected dry-upper naval holds |
| Factory → payload conveyor → router → world | PASS: six actual ground/flying unit chains on Z0/Z1/Z5, no repeat cost; real movement after release |
| Loaded native conveyor release | PASS: native restored payload reaches XYZ boundary |
| Sources/voids | PASS: 30 actual native configuration/inventory/liquid/payload/power checks on Z0/Z1/Z5; existing power graph fixture retained |
| All 14 models / floor contact / immutable state | PASS: 168 native model poses, 102,387 assertions; one cache build per profile |
| Repair Point / Repair Turret | PASS: real native HP increase and stop/disable checks, Z0/Z1/Z5 |
| Target Dummy | PASS: existing native target spawn/tether, real hit counters, config, save/reference and floor isolation fixture; physical shooting NOT TESTED |
| Tiled display grouping / removal / floor isolation | PASS: actual native shared canvas and local adjacency |
| Tiled display pixels / colors / orientation / unchanged cache | PASS: actual OpenGL framebuffer pixels at Z1/Z5; independent and joined panel UV/content checks |
| Model gallery / top / low / upper structures | PASS: 25 actual standalone scenes per final tested runtime; screenshots and GL errors recorded |
| World and detail distance | PASS: independent bounds/LOD tests; actual changed rendering screenshots; original camera far restored after rendering |
| Ordinary FPV / Build Mode | PASS: production API toggles and existing native input/motion fixtures; physical F5/Tab/mouse acceptance NOT TESTED |
| Existing construction / F3 / priority / selection / clipboard / save/campaign / network regressions | PASS in full compiled regression suites; no new protocol/save format |
| Broad legacy support-effects helper | FAIL in paused fixture on baseline and modified JAR at the same repair-snapshot assertion; focused repair checks PASS; not a new accepted gameplay regression |
| Linux physical rendering, mouse, install | NOT TESTED; same portable diagnostic JAR provided, no Windows-only runtime model code |
| Actual two-computer multiplayer | NOT TESTED; local automated authority/codec/connection fixtures are separate evidence |
| Physical owner acceptance | NOT TESTED; three-step manual supplied |

## Performance interpretation

The geometry CSV measures CPU template data: 42 shared templates, 407,808 vertex bytes total, maximum 534 vertices / 178 triangles per template. It excludes object headers and existing stream buffers. Per-instance geometry transforms still occur; topology/material templates are reused. New utility geometry uses the existing atlas batch, not one new draw call per instance. Live displays require their own existing native framebuffer texture.

The renderer benchmark uses identical standalone host/settings/profile construction and scripted scene cameras for the accepted and modified JAR. CPU figures time `FpvWorldRenderer.render`; GPU figures are asynchronous `GL_TIME_ELAPSED` queries, polled only when available. No readback or screenshot occurs inside measured frames. These small synthetic scenes do not establish real-game FPS, multiplayer latency or presentation smoothness. Draw-call/triangle submission and per-frame allocation totals were not separately instrumented in this pass; no unsupported figures are claimed.

Low world distance intentionally culls distant geometry. High-detail distance changes existing visual LOD, not authoritative state. Compare `world64`/`world8192` and `detail20`/`detail240` screenshots in the evidence folders.

## Measured renderer comparison

| Scene | CPU ms, before → after | GPU ms, before → after |
|---|---:|---:|
| top | 0.952 → 1.074 | 0.564 → 0.113 |
| oblique | 0.940 → 1.800 | 0.485 → 0.185 |
| dense (second warmed pass) | 0.491 → 0.723 | 0.387 → 0.091 |

Single sequential Java25 pair, 160×160 map, the same 14 native types on four floors and a 100-source field. Each scene had 60 warmup frames and 120 measured frames (119 available asynchronous GPU samples). No full regression suite ran concurrently. Screenshots occurred during warmup. The second warmed dense pass adds **0.232 ms CPU**. GPU query values were lower in this fixture; GPU clock/load variation and the changed geometry prevent treating this as a general FPS improvement. Draw submission counts, allocations and complete frame/presentation time were not instrumented. Both preference comparisons changed more than 100 actual screenshot pixels.
