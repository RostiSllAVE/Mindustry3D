# A2-10 — Test matrix

Baseline: owner-accepted A2-09c. Final local candidate only; no published artifacts changed. Both runtimes run on the Windows development PC.

| Scenario | Java 17 | Stock Steam Java 25 | Evidence / limits |
| --- | --- | --- | --- |
| Complete final regression suite | PASS 139 programs | PASS 139 programs | verified-final/build-result.json and verified-final-steam25-tests.json |
| Actual160.7 StaticWall catalog; native defense/building families | PASS | PASS | 21 geological,5 industrial,5 vegetation; all defensive Wall families excluded |
| Natural stone mountain, open edges/gaps, untouched shape | PASS | PASS | Bit-identical old vertex comparison; 11705 pure height assertions |
| Industrial75×75 region formerly becoming mountain | PASS | PASS | 5625 →0 mountain cells; physical center22.687399 →1.4 tiles |
| Copper/titanium walls, industrial blocks inside/beside cliff | PASS | PASS | No raised terrain footprint; shared headless/renderer heights |
| Mixed/distant static structures, exact footprints | PASS CPU vertices | PASS CPU vertices | Real coarse fallback geometry; no textures/GPU claim |
| Exposed side strips beside actual cliff edge | PASS | PASS | Both sides of min-edge threshold in four directions |
| Solid static-wall pick/occlusion; hidden blueprint | PASS | PASS | 305 renderer/picking checks total |
| Artificial add/remove; local-only invalidation | PASS | PASS | 1000 edits/map:0 field rebuilds; cache/scans unchanged |
| Rule precedence, safety guards, null/invalid metadata | PASS | PASS | 4452 native checks total; unknown content uses documented defaults |
| Native Rules JSON + real disk campaign SaveIO | PASS | PASS | 42 native campaign checks, copied disposable saves only |
| Serpulo/Erekir transitions and returning to saved sector | PASS | PASS | No old overrides/layers leak; native tiles/backup fixtures preserved |
| Construction/support/platform/XYZ selection/copy | PASS fixtures | PASS fixtures | Prior A2-09a/b/c and structural/native suites preserved |
| FPV/F3/F4/F5/Tab, mouse backends, camera, combat/unit regressions | PASS fixtures | PASS fixtures | Production classes unchanged outside terrain/render scope |
| JAR/folder fingerprint + notices + previous assets | PASS | PASS native helper | 61 max class major; Windows/Linux resources included |
| 512² sparse/dense performance | MEASURED CPU | MEASURED CPU | Five-generation medians,100k queries,1000 edits; no FPS claim |
| Actual Import→Restart→F5 and textured visual review | NOT TESTED | NOT TESTED | Owner procedure; no physical acceptance claim |
| GPU frame time/FPS or per-frame allocation profiler | NOT MEASURED | NOT MEASURED | Counters/source establish no hot policy parse/full scan |
| Physical Linux/X11/Wayland or independent two-PC session | NOT TESTED | NOT TESTED | Portable common JAR is not physical certification |

A2-10-VALIDATION.json lists all final test programs, hashes and measurements. Exploratory candidates/logs remain under local/a2-10; only the final test-kit-final JAR is the completion candidate. No owner save, Steam Workshop listing, Git remote or public Alpha asset was modified.
