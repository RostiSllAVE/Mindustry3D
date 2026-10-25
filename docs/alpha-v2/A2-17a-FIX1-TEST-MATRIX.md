# A2-17a fix1 — actual checks

Baseline `b3faf9a`. Windows x64 standalone **Mindustry 160.7**, complete **Java17.0.20.1** and stock minimal **Java25.0.1**. No Steam launch. Same final diagnostic player JAR on both runtimes; graphics fixtures overlay only explicit test harness code in disposable copies.

| Check | Actual result |
|---|---|
| Baseline native healing + full production render | **REPRODUCED:** HP 50 → 62.28749, enabled source/valid target/strength 0.63233, legal native counter -1, empty candidates, **0 beams** |
| Identical focused scenario after fix | **PASS:** same HP/strength/counter, cached repair source present, **1 beam / 18 existing effect primitives**; no extra HP from rendering |
| Complete build/preflight and Java17 regressions | **PASS 214/214 programs**, `local/a2-17a-fix1-build.log`, `final/build-result.json` |
| Same complete suite on stock standalone Java25 | **PASS 214/214**, 0 failures; `local/a2-17a-fix1/final/standalone25-tests.{json,log}` |
| New native beam regression | **PASS 30 checks per runtime**: real HP, initial -1 sentinel, scan once/reuse, explicit clear/rebuild, emitter height, stop, read-only HP, authoritative remote snapshot and empty snapshot retirement |
| New pure direction marker regression | **PASS 4,097 checks per runtime**: native E/N/W/S rotation, animation, multiple footprints, Z0/Z1/Z3/Z5 roof clearance, finite/bounded strokes and invalid inputs |
| Actual OpenGL repair + preview fixture | **PASS 33 scenes / 1,584 frames / 3,654 checks per runtime**, zero GL errors |
| Repair Point/Turret active/stopped | Actual HP first, beam submission and actual green beam pixels; Z0/Z1/Z3 real native upper proxies/platforms; endpoints use real target XYZ; rendering preserves HP and native strength |
| Directional placement preview | Z0/Z2/Z5, all four rotations, cyan valid/red invalid, real marker pixels; multi-tile Payload Conveyor; non-rotatable wall suppressed; queued conveyor retains its arrow |
| FPV, Build Mode and native movement API | **PASS** existing input/motion smoke checks, `repair-arrows-final{17,25}/input-api.txt`; physical keys remain separate |
| Terrain styles, construction/priority/pause/copy, save/campaign, native combat, networking/protocol/identity | Existing programs **PASS** in both complete suites; no changes to those source systems |
| Shader/state preservation | Existing translucent preview shader reused; existing support volume/world-depth pass reused; no new shader/state bypass or per-marker mesh |
| Published/accepted artifacts and clean standalone reference | **PASS:** 2,363 prior files and 98 reference files retain hashes |
| Physical owner visual/control acceptance | **NOT TESTED** in this pass; three checks in report |
| Physical Linux GPU/input and independent two-computer session | **NOT TESTED**; portable code and snapshot replay are not physical acceptance |
| FPS/GPU timing | **NOT MEASURED**; no FPS improvement claim |

Final player JAR SHA-256:

`d356e2226113ed4b032c1dfba99f68fa58a217459cff561cfefb05c7f8651220`

Raw full-game evidence:

- `local/standalone-runs/repair-beam-before4/{diagnostic.txt,repair-beam.png,result.json}` — accepted baseline.
- `local/standalone-runs/repair-beam-after/` — identical focused final-JAR probe.
- `local/standalone-runs/repair-arrows-final25/` and `repair-arrows-final17/` — native HP + GPU pixel assertions, all preview rotations, stop and upper floors.
- `local/a2-17a-fix1/preserved-verification.json` — original artifacts/reference checks.

One early probe compile used `EntityGroup.contains(entity)` rather than the actual native predicate API; corrected in the fixture before runs. Baseline OBSERVED files intentionally record the broken build and are not final PASS claims. No production rules were weakened for testing. Large native tests retain their existing mock-GL limitations; the separate actual game runs establish visible pixels. The old broad paused support helper's unrelated server-snapshot assertion is not silently reclassified as passed.
