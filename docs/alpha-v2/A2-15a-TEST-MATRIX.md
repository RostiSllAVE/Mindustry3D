# A2-15a — Test matrix

PASS means the stated automated check passed; it is not physical gameplay acceptance. Final diagnostic SHA-256: `6a6c73ef35f646f84790100b1a4118c5fce28ee915b0f0bd916d3561bea52d87`. Baseline A2-14 `d22d028`.

| Area | Result | Evidence / limits |
| --- | --- | --- |
| Full Java17 regressions | PASS147/147 | Final `seam-fixed/build-result.json` |
| Full stock Steam Java25 regressions | PASS147/147 | `seam-fixed-steam25-tests.json`; no java.logging |
| Native160.7 classification/cache integration | PASS260 checks/runtime | `HorizonNativeTest` |
| Deterministic geometry/seams/large border | PASS466,349 checks/runtime | `HorizonGeometryTest`; synthetic up to8192², no full huge world |
| Ocean / rock / forest / desert / ice | PASS automated native + GL | Nine-world final gallery, low/high PNGs |
| Mixed / opposite edges / within-edge regions / corners | PASS automated | Native and geometry tests; actual mixed360°/720-high screenshots |
| Wasteland / volcanic | PASS native + geometry | No dedicated actual volcanic screenshot |
| Artificial border walls | PASS native + GL | No mountain classification from industrial/defensive walls |
| Sparse desert trees / dense forest | PASS native | Sparse remains desert;75% forest fixture |
| Space / empty / unknown | PASS native + GL | Space no outside mesh; neutral unknown; no portal |
| Small map / single-tile / huge synthetic border | PASS | Finite/outside-only/bounded topology checks; actual gallery96×64 |
| Relief/mountain/liquid collar | PASS automated + inspected PNG | Shared corner heights, UVs, wave join; high-view LOD mismatch remains |
| Save/load | PASS SDK + actual GL native SaveIO | Disposable mixed gallery map+geometry fingerprint |
| Campaign lifecycle | PASS44 native regression checks | Existing sector generation/save/load/transition fixtures; physical campaign NOT TESTED |
| Settings changes / OFF legacy / return ON | PASS | Actual1,200-frame alternating gallery, stable fingerprints, GL0 |
| World replacement / cache disposal / border edits | PASS native | No tile/profile retained on clear; interior edit no rebuild |
| Cross-runtime determinism | PASS | Four Java17/Steam25 profile/geometry fingerprints equal |
| 3D selection/copy/paste / native construction / F3 priority | PASS regression fixtures | Physical mouse/menu/gameplay NOT TESTED |
| Input / F5 / Tab / prediction / combat / liquids/naval | PASS existing regressions | No physical acceptance or new network gameplay test |
| CPU generation / mesh allocation | MEASURED | `performance-comparison-seam-fixed.json`; MockGL, not FPS |
| Actual SDL render / GL errors | PASS | Baseline1,080 + final1,080 + paired1,200 frames, GL0 |
| GPU overhead | MEASURED, limited | Async queries; paired fixed scenes; separate-process cases include slowdowns |
| High-altitude frame performance | NOT TESTED | Screenshots captured; timings cover low view only |
| Linux physical rendering/input/performance | NOT TESTED | Platform-neutral code/Java17 validation only |
| Independent multiplayer / reconnect / remote map | NOT TESTED physically | Existing networking regressions and deterministic derivation only |
| Physical Windows import/restart, night, menus, FPV/build | NOT TESTED | Owner procedure required |
| Source ↔ JAR / JAR ↔ folder / licenses | PASS | Fresh913-class equivalence, same payload identity17/25 |
| Previous artifacts and primary saves | PASS preservation audit |2,065 prior Alpha files +54 diagnostics; only disposable profiles |

Machine-readable final evidence: `A2-15a-VALIDATION.json`. Local final test-kit includes sanitized benchmark/gallery JSON, manifests, helper source and comparison PNGs. Obsolete `finalized`/`corrected` prototypes are retained locally but are not the delivered artifact.
