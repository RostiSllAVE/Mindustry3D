# A2-03 — Campaign test matrix

Baseline A2-02; Mindustry 160.7. **PASS applies only to the specified test level.**

| Case | Result | Evidence / boundary |
|---|---|---|
| Historical unspecified player campaign failure | **NOT REPRODUCED** | No controlled original failure/save supplied; no speculative runtime fix |
| Fresh Ground Zero preset | **PASS — native automated** | Real World.loadSector/Logic.play, isolated Settings, native core |
| Existing native campaign without FPV chunks | **PASS — native automated** | Full save before runtime install, full load afterwards, native resources intact |
| Z1 wall + directional damaged Z2 conveyor | **PASS — native automated** | Structural validation, native costs, full disk save/load and correct restoration |
| 3D terrain across sectors/load | **PASS — native automated** | Natural-mask fingerprint regenerates, native tile data unchanged |
| Native aircraft/units | **PASS — native automated** | Flare ID/type/XY/HP plus regenerated finite 3D pose; no exact ordinary-flight altitude persistence claim |
| Walker stance, boost/creator save state | **PASS — native automated** | Existing real-physics helpers around full SaveIO; actual post-load safe handoff |
| Native overwrite backup | **PASS — native automated** | Previous save bytes exactly equal native backup; copies loaded read only |
| Generated Serpulo sector 2 | **PASS — native automated** | Real procedural sector generator, different terrain, prior upper/flight authority cleared |
| Erekir Onset and full reload | **PASS — native automated** | Real preset generator, native environment/core/planet preserved |
| Serpulo -> Erekir -> saved Serpulo | **PASS — native automated** | Correct sector context and restored saved layers, no cross-sector leak |
| Custom world -> campaign twice | **PASS — native automated** | Same process; upper structures restore once, terrain matches saved sector |
| Campaign metadata persistence | **PASS — native automated** | Separate Settings read of new disposable profile; not physical progression |
| A2-02 existing-save cold restore | **PASS — separate JVMs** | 8 checks each on Java 17 and stock Steam 25; generated fixture, not user save |
| Alpha 1.0.4 existing-save cold restore | **PASS — separate JVM** | 8 checks on stock Steam 25; fixture made with exact preserved Alpha JAR |
| Final campaign regression | **PASS** | 39 checks each on Java 17 and Steam 25; final targeted reruns after adding aircraft/active-transition checks |
| Existing full regression suite | **PASS** | 120 programs on each runtime; A2-02 tests/strict identity/mouse contracts retained |
| Actual installable JAR/folder identity | **PASS** | Canonical identity equals A2-02; metadata, notices and runtime source unchanged |
| Actual desktop campaign attempt | **BLOCKED / NOT TESTED** | Native selector setup denied before mod initialization; not a reproduced mod failure |
| Physical planet UI / F5 / Tab / input / views | **NOT TESTED** | Mock graphics/native test does not exercise player actions |
| Copy of owner's actual affected save | **NOT TESTED** | Primary saves not opened/copied; synthetic existing saves only |
| Windows/Linux desktop campaign | **NOT TESTED** | No corresponding physical current-build run; broad Linux acceptance not inferred |
| Hosted/Steam campaign and real reconnect | **NOT TESTED** | No usable native sockets or two-device session; A2-02 handler tests are not network acceptance |
| Capture, research, lost-sector recovery, long exports | **NOT TESTED** | Loading/generation/serialization checks do not certify complete campaign gameplay |
| Primary saves / prior artifacts | **PRESERVED** | Disposable test data only; previous file hashes verified unchanged |

No campaign runtime fix is claimed. There is no observed remaining defect within
the passing native scenarios; the reported physical failure remains unresolved
because it has not been reproduced. Machine details: [validation](A2-03-VALIDATION.json).
