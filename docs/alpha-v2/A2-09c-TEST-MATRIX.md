# A2-09c — Test matrix

Final source checkpoint: current local A2-09c commit; baseline `2901659`. All automated tests execute on this Windows development machine. Headless fixtures use real Mindustry 160.7 content/queues/build logic and mocked input, GL and transport. They are not physical gameplay acceptance.

| Coverage | Java 17 | Stock Steam Java 25 | Evidence |
| --- | --- | --- | --- |
| Complete regression suite | PASS: 137 programs | PASS: 137 programs | `local/a2-09c/verified/build-result.json`, `verified-steam25-tests.json` |
| Manual survival Z1/Z2/Z5, native build-time formula, cost/speed modifiers | PASS | PASS | `UpperConstructionNativeTest`, 59 checks total |
| Sandbox manual / pasted blocks; bounded 256-entry insertion | PASS | PASS | 1 update single paste; 8 updates for 256 |
| Resource-blocked head followed by independent ready work | PASS | PASS | exact A2-09b JAR repro; new entry completes tick 72 |
| Native blocked Z0 plan with independent upper work | PASS | PASS | mixed-height batch retains ground blueprint |
| Support-first column/platform/Z2 block; unsupported manual rejection | PASS | PASS | real structural solver / live geometry |
| F3 pause, global pause, priority, range loss, unit switch/resume | PASS | PASS | retained work; no burst/duplicate payment |
| One builder across multiple jobs / native and upper work | PASS | PASS | 36+36 ticks, native progress guard |
| Native Z0, multi-floor factory, rotations/configurations/power/items/liquids | PASS | PASS | prior 117 clipboard checks and 9 liquid checks |
| Prior pause / priority / volume selection fixtures | PASS | PASS | 45 pause, 80 native priority, 21 network priority, 54 selection checks |
| Actual authenticated placement handlers, replay, ownership, partial progress, completion | PASS | PASS | 15 new network checks; in-memory transport |
| Atomic v2 progress decoding; invalid complete-progress/trailing bytes | PASS | PASS | no mutation of accepted state on malformed data |
| Geometry revision, ghost exclusion, next render-cache sync | PASS: CPU only | PASS: CPU only | mock empty atlas; real revision/visibility/cache methods |
| JAR/folder code identity, notices, prior archives | PASS | PASS (native Java helper) | payload fingerprint, 2065 prior Alpha files and 30 diagnostic assets unchanged |
| Actual Import → Restart → F5 / visible progress-strip appearance | NOT TESTED | NOT TESTED | owner procedure |
| Real screen/GPU frame timing / pointer capture / menus | NOT TESTED | NOT TESTED | previous native input code unchanged, no new physical claim |
| Physical Linux game / X11 / Wayland | NOT TESTED | NOT TESTED | one common Java17-bytecode JAR; platform regressions pass on Windows |
| Two physical computers / real network congestion and transit | NOT TESTED | NOT TESTED | no multiplayer acceptance claim |
| Owner's intermittent manual-placement delay | NOT REPRODUCED independently | NOT REPRODUCED independently | bounded diagnostics available; no universal resolution claim |

`A2-09c-VALIDATION.json` lists every final test program and measured samples. The first request and renderer preparation in each process include cold/JIT initialization; warm CPU values must not be interpreted as end-to-end player latency. No primary save was loaded, no published release was replaced, and nothing was pushed/published.
