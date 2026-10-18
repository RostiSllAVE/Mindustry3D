# A2-05 — Test matrix

PASS is limited to the evidence described. MODELED is not physical acceptance.

| Check | Result | Boundary |
|---|---|---|
| Full regressions on Java17 | PASS | 124 programs, final `acceptance` build |
| Same programs on stock Steam Java25.0.1 | PASS | 124 programs; final JAR, no modules added |
| Local prediction/replay/precision/limits | PASS | 47 checks: integration memory, receipts, ownership/type, ordering, normalization, modes, stale/input-timeout holds, replay/history eviction, short-outage recovery |
| Native sender/receiver/entity/contacts | PASS | 21 checks: real handler/entity APIs, READY, matching revision and applied-input receipt, epoch/world reset, possession/death, no native XY/HP/inventory/reload/bullet/controller writes, wall/unit/Z contacts, frame-cache cleanup |
| Server-only adjunct rejects client claims | PASS | Actual server handler rejects STATE from client; existing client-position safeguard and authority regression tests retained |
| Delayed/missing/duplicate/reordered pose data | PASS | Existing actual assembly/codec tests plus new receipt revision/duplicate guards and bounded prediction tests; no partial world state applied |
| Acro/Stabilized/Build at0/50/100/200/400ms RTT | MODELED | Shared integrators and actual codecs, 60 deterministic controller cases total |
| Ground/walker/boost at same RTTs | MODELED | Approximate XY reference, supplied walker Z and fixed permission boundary; not native traversal acceptance |
| Jitter ±15ms, 5% input/frame loss and duplicates | MODELED | Dropped full frames stress recovery; not TCP/Steam retransmission/congestion |
| Pose errors, correction/hold frequency, numeric smoothness | MEASURED IN MODEL | CSV before/after against reference server; transform proxies, not monitor/input latency or visual comfort |
| Controller CPU/allocation overhead | MEASURED IN MODEL | Full JDK thread counter; excludes UI/renderer/real network/native contacts |
| Native read-only contacts,100/1000/4096 units | PASS / MEASURED FIXTURE | Native quadtree/world, 1/48 callbacks, cache OFF/ON; no live battle or FPS claim |
| A2-04 pose codec / other-unit interpolation | PASS | PoseSnapshotCodec, RemotePoses and NetProtocol class bytes unchanged |
| A2-02 readiness / strict identity / native owner bridge | PASS | Existing regression programs and unchanged ConnectionLifecycle/ModFingerprint/UnitMotionBridge class bytes |
| Alpha1.0.4 disposable campaign restore | PASS | Eight cold-JVM native checks with final JAR on stock25; original fixture read-only |
| Final JAR/folder canonical identity | PASS | Actual ModFingerprint under stock Java25 plus static package checks |
| Windows/Linux resources, bytecode, notices | PASS, STATIC | Common Java17 payload, native resources and licenses included; new measurement tools excluded |
| Prior Alpha/A2-02/A2-03/A2-04 artifacts | PASS | Hashes preserved; primary saves not accessed |
| Real two-computer host/client, Steam join, host swap | NOT TESTED | Selector capability unavailable (IOException before connection), no permission bypass |
| Physical import/restart, cursor, Acro/F9/Tab/F5/F7 | NOT TESTED, THIS BUILD | Existing input/camera contracts pass; actual desktop behavior needs owner test |
| Real ground/walker/boost landing, combat/build/menu/respawn | NOT TESTED, THIS BUILD | Native/code fixtures do not prove paired physical gameplay |
| Physical Linux startup / cross-OS session / long play | NOT TESTED | No separate machine operated for this pass |
| Real high-latency/low-bandwidth improvement | NOT TESTED | A2-04 reliable congestion and authoritative action latency remain |

The early candidate's large stale-fallback jumps were a confirmed model FAIL,
corrected by held-pose recovery; raw candidate evidence is retained. Small-case
collision-cache timing regressions and residual errors/holds are reported rather
than removed. See the [report](A2-05-PREDICTION-REPORT.md),
[machine evidence](A2-05-VALIDATION.json) and [owner test](A2-05-MANUAL-TEST.md).
