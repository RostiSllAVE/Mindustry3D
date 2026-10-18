# A2-04 — Validation matrix

PASS is limited to the evidence named below. No live multiplayer or Linux
gameplay acceptance is inferred from Java tests on Windows.

| Check | Result | Evidence / boundary |
|---|---|---|
| Full regression programs, Java 17 | PASS | 122 programs, including authority, readiness, reconnect, identity, save/campaign, input and renderer contracts |
| Same programs, stock Steam Java 25.0.1 | PASS | 122 programs; no separate runtime required for these tests |
| Exact codec values and bounded malformed input | PASS | 8088 checks including negative zero, subnormals, float64 fallback, counts, duplicate IDs and truncated/trailing data |
| Missing/reordered/duplicate/stale pose chunks | PASS | Actual assembler + complete replacement; next independent full snapshot recovers without delta dependencies |
| Per-recipient visibility and reliable transport choice | PASS | Eight native sender checks; two different team recipients, hidden native test entity, in-memory connections and injected fixture digest |
| Per-batch query reuse, cache cleanup and single-peer path | PASS | Same native fixture; no retained world/unit references after sending/reset |
| 100/1000/4096 lossless serialization sizes and allocations | PASS | Measured old/new native LZ4 serializers; moving, idle, dense, entropy fixtures; see CSVs and regressions |
| Native sender cost, 1 and 4 recipients | PASS | Actual filtering/world query/encoding/chunk submission, native stationary entities; no socket or live AI battle |
| Low bandwidth / 20–250 ms RTT sensitivity | MODELED | 72 FIFO byte-service cases, not TCP/Steam emulation or measured network latency |
| Slow arrival, 4096 units at 32,000 bytes/s | FAIL, BOTH VERSIONS | Actual assembler cannot finish within existing 5-second window; 9.223 seconds before / 6.590 after |
| Other completed slow-arrival frames | PASS | Bit exact through actual serializer/assembler; completing one frame does not mean 10 Hz can keep up |
| Legacy Alpha 1.0.4 campaign save restore | PASS | Eight cold-JVM native checks with final JAR / stock Java 25; disposable synthetic save |
| JAR / folder canonical payload equivalence | PASS | Actual ModFingerprint under stock Java 25, plus package validation; not Steam subscription lifecycle |
| Windows/Linux required code/native payload and notices | PASS, STATIC | Common Java 17 bytecode; JNA Windows/Linux natives and licenses included; no host game/test benchmark classes bundled |
| Existing release and A2-02/A2-03 artifacts unchanged | PASS | SHA-256 preservation audit; primary player saves not accessed |
| Physical import/restart, F5, Tab, mouse/menu and visual quality | NOT TESTED, THIS BUILD | Existing contract/native tests do not replace manual acceptance |
| Real Windows/Windows or Windows/Linux host/join/Steam/reconnect | NOT TESTED | Local selector capability unavailable before connection; no permission bypass attempted |
| Real low bandwidth/high latency, OS/Steam queue behavior | NOT TESTED | Congestion/HOL remains; no latency improvement claimed |
| Physical Linux startup and long sessions | NOT TESTED | No separate Linux computer operated for this pass |

The owner separately confirmed A2-03 offline campaign F5, Tab, saving and sector
transitions. This is historical physical acceptance of A2-03, not an A2-04
multiplayer test. Use the [owner procedure](A2-04-MANUAL-TEST.md) for next checks.
