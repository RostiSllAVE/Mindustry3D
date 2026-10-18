# A2-04 — Multiplayer snapshot and traffic optimization

Baseline: A2-03 `cb54e181109f80ded051a795dfb6af62dd4fc14b`, branch `main`,
Mindustry 160.7, internal 0.18.31. Date: 2026-10-18.
The owner reports that offline campaign F5, Tab, save/load and sector transitions
work after A2-03. That report does not certify hosted campaigns or Linux networking.

## Outcome and scope

Implemented two narrowly scoped changes:

1. **Lossless compact, independent full pose snapshots.** Positive zero fields
   occupy a mask bit. A number uses float32 ONLY if converting it back reproduces
   identical double bits; otherwise it stays float64. Negative zero, subnormal
   numbers and all physical dimensions/angles remain exact. Equal desired altitude
   can reference Z within its own record. An incompressible record uses all 12
   doubles without mask overhead; worst-case logical frame growth is a fixed
   five-byte schema header, not per-unit expansion. The owner's flight profile,
   quaternion and momentum still use the original full doubles.
2. **One physical pose query per batch for multiple recipients.** Immutable
   Unit3DState values are reused across recipients within that game-thread call.
   Native team/fog visibility is still checked separately for every player. A
   single recipient avoids the cache lookup path. Cache references are cleared
   after each batch, including exceptions, and on world reset. The map's backing
   capacity is retained for reuse; no cross-frame pose or network queue is kept.

New outer **protocol 4** explicitly distinguishes the changed pose format.
Strict canonical payload checks remain required; matching version text 0.18.31
alone is insufficient. NetProtocol's old full-double snapshot methods remain as
an unoptimized reference for tests/benchmarks; production uses PoseSnapshotCodec.
No cross-frame delta, dictionary, keyframe dependency, prediction, extrapolation,
interest-distance hiding or quantization was introduced.

Unchanged: reliable delivery for poses and all persistent channels; native
construction/inventory/health authority; 30 Hz input, 10 Hz pose target, persistent
and effect rates; 100 ms interpolation buffer; A2-02 readiness/epochs/reconnect;
campaign/save formats, combat balance, camera controls, notices and metadata.
This does **not** eliminate reliable-stream head-of-line blocking or make a link
with insufficient capacity keep up. No real-world latency improvement is claimed.

## Reproducible baseline and measurement boundaries

The baseline uses the preserved A2-03 JAR, SHA-256
`07342fb7d389ec1d860715c8a4099f2036d8b5152fc07b57f31c6e853cbcca4f`.
An initial baseline was recorded before integration; final before/after runs use
the same seeds and tools. Both are retained locally. Final raw measurements are
committed beside this report, and artifact/test details are in
[A2-04-VALIDATION.json](A2-04-VALIDATION.json).

**Important correction to interpreting A2-01:** native Mindustry 160.7's
ArcNetProvider.PacketSerializer already applies LZ4 to these packets. The older
1.06 MB/s/1000-unit figure was FPV payload before native compression, not measured
NIC/Steam traffic. This pass runs the actual installed native serializer and
reader, as well as the real FPV encoder/chunker/assembler/RemotePoses application.
Native-serialized byte counts exclude TCP length framing, TCP/IP, Steam/relay,
retransmissions, native entity sync and other mod channels. They are measured
serialization sizes, **not observed network throughput**.

Windows development PC; Temurin Java 17.0.20.1, heap 128..512 MiB, 20 warm-up and
40 measured samples per row. Timings are wall-clock nanoTime spans and include
scheduling/GC; they are not isolated thread CPU cycles, GPU time or gameplay FPS.
Allocated bytes are the current thread's supported management counter, developer
only. Benchmarks run after the regression processes finish. This remains one
active-PC sample, not a controlled hardware study. Raw timing regressions are
reported, not discarded.

`SnapshotTrafficBenchmark` covers deterministic synthetic idle, mixed moving,
dense and high-entropy poses at 100/1000/4096 units, including an owner profile.
The moving fixture has native-float-style XY/velocities and full-double vertical/
attitude/bounds fields. Entropy deliberately makes all numeric fields unrelated
doubles. Every decoded unit record must compare exactly equal. Each sample
includes encoding, actual native packet compression, native reading/reassembly,
codec decoding and RemotePoses application. No native AI, world queries or socket
is included in this microbenchmark.

### Moving synthetic poses: measured frame sizes

| Units | FPV bytes/frame before -> after | Native LZ4 bytes/frame before -> after | Native reduction | Chunks before -> after |
|---:|---:|---:|---:|---:|
| 100 | 10923 -> 7312 | 7367 -> 5381 | 27.0% | 1 -> 1 |
| 1000 | 106755 -> 70582 | 72225 -> 51622 | 28.5% | 9 -> 6 |
| 4096 | 436443 -> 288274 | 295125 -> 210868 | 28.5% | 37 -> 24 |

At nominal 10 Hz, multiply bytes/frame by ten. For example 1000 mixed moving
poses fall from 722,250 to 516,220 native-serialized bytes/s per recipient. This
is about 28.5% fewer bytes under this fixture, not an internet speed measurement.
No units or visual detail were omitted. All original fields are bit exact.

### Pipeline elapsed time and allocations, mixed moving fixture

| Units | Encode ms before -> after | Chunk/native serialization ms before -> after | Native receive + decode ms before -> after | Apply ms before -> after | Allocated bytes/full cycle before -> after |
|---:|---:|---:|---:|---:|---:|
| 100 | 0.039 -> 0.060 | 0.026 -> 0.027 | 0.069 -> 0.060 | 0.011 -> 0.022 | 225784 -> 141656 |
| 1000 | 0.374 -> 0.368 | 0.242 -> 0.163 | 0.567 -> 0.528 | 0.030 -> 0.038 | 2123336 -> 1455225 |
| 4096 | 1.497 -> 1.399 | 0.995 -> 0.541 | 2.122 -> 1.770 | 0.169 -> 0.141 | 8647694 -> 5930353 |

CSV: [before codec](A2-04-before-codec.csv), [after codec](A2-04-after-codec.csv).
Native receive+decode includes real LZ4 decoding and chunk reassembly. Application
is unchanged RemotePoses map replacement/interpolation setup. These columns are
sequential in this benchmark; runtime aggregate metrics can overlap and must not
be added indiscriminately.

### Actual native server sender: stationary mixed units

`SnapshotServerBenchmark` invokes the actual private server sender with native
Flare/Dagger entities, real world queries/filtering and native LZ4 submission to
in-memory connections. It uses the existing explicitly injected fixture digest;
this is **not a real transport/handshake or live AI battle**. No owner flight
session is present, unlike the synthetic table. Normal measured passes have
telemetry OFF; a separate diagnostic pass checks query counts.

| Units / recipients | Sender mean ms before -> after | p95 ms before -> after | Allocated bytes/send before -> after |
|---|---:|---:|---:|
| 100 / 1 | 0.214 -> 0.303 | 0.299 -> 0.491 | 106166 -> 44025 |
| 100 / 4 | 0.658 -> 0.561 | 1.144 -> 0.827 | 408287 -> 122259 |
| 1000 / 1 | 0.793 -> 0.825 | 1.236 -> 1.009 | 950744 -> 387851 |
| 1000 / 4 | 2.601 -> 2.144 | 3.397 -> 2.528 | 3802485 -> 1166520 |
| 4096 / 1 | 3.568 -> 2.277 | 4.926 -> 2.615 | 3856177 -> 1574336 |
| 4096 / 4 | 16.493 -> 8.589 | 21.330 -> 12.376 | 15424168 -> 4723859 |

CSV: [before native sender](A2-04-before-native-server.csv),
[after native sender](A2-04-after-native-server.csv).
At 4096 units/four recipients, query count falls from 16,384 (derived from the
unchanged all-visible baseline loop) to 4096 (actual new diagnostic counter),
with 12,288 reused values. Filtering and per-recipient encoding still scale with
recipient count. The complete sender's sampled mean falls from 16.493 to 8.589 ms;
allocation drops from 15,424,168 to 4,723,859 bytes/send. These are this fixture's
measurements, not a claim that entire battle CPU/frame time halves.

### Regressions and limits

- 100 units/one recipient native sender: mean **0.214 -> 0.303 ms**; at 1000/one,
  **0.793 -> 0.825 ms**. Small-case costs did not uniformly improve. Small timings
  are sensitive to JIT/scheduling, and mask work has a real cost.
- High-entropy 4096-unit fixture: native bytes **435,652 -> 437,845** (about +0.5%);
  encode mean **1.549 -> 1.691 ms**, receive/decode **1.965 -> 2.532 ms**.
  Raw-record fallback bounds logical expansion, but does not guarantee a better
  LZ4 match or CPU time for arbitrary dense doubles. This stress case is retained.
- Ordinary moving/idle/native fixtures improve byte and allocation counts;
  we do not advertise universal compression, FPS or latency gains.
- No per-frame runtime allocation profiler, OS/Steam queue-depth telemetry,
  NIC packet trace or Linux timing comparison was available.

## Congestion, latency and data integrity

[Link model](A2-04-link-model.csv): 72 deterministic timing-model cases, 100/1000/
4096 units, measured moving-frame native sizes, 32,000/125,000/500,000/2,000,000
bytes/s, RTT 20 or 250 ms, and an optional assumed 250 ms service stall every two
seconds. Frames are generated at 10 Hz for 20 seconds. A hypothetical reliable
256-byte state message each second shares the service queue. This is a FIFO
byte-service model, **not TCP/Steam emulation**; it excludes native traffic and
congestion control, so it is optimistic about capacity. RTT is a model input,
not a new measured ping. Reduced modeled queue growth is not real-world latency.

[Actual-code slow-delivery probe](A2-04-slow-assembly.csv) feeds real serialized
packets to the actual assembler at modeled arrival times (no socket/sleep):

- Every completed before/after frame at tested rates is bit exact.
- At 4096 moving units and 32,000 bytes/s (~256 kbit/s), BOTH versions fail to
  complete a frame: the unchanged assembler has a five-second absolute assembly
  window. Modeled frame delivery is 9.223 seconds before, 6.590 after. This is a
  **confirmed deterministic limitation**, not a claimed remote connection failure.
- At 125,000 bytes/s those frames assemble (2.361 -> 1.687 seconds/frame), but
  ten such frames/second greatly exceed capacity. Reliable backlog still grows.
- A model's hypothetical delivery/queue number must not be interpreted as usable
  gameplay when assembly/READY limits are exceeded. Initial world/native traffic
  and persistent state can make the real situation worse.

Protocol tests cover missing, delayed, duplicate and reordered chunks, stale full
revisions, bounded malformed data, exact scalar representations and recovery by
the next complete snapshot. Only a complete validated snapshot replaces display
state; no partial poses, inventories, damage or construction are applied. Native
batch tests confirm per-team visibility, reliable sends and one-frame query-cache
lifetime. Existing authority and A2-02 error/readiness tests remain active.

**Do not solve the slow-link limitation by blindly extending buffering/timeouts.**
The narrower follow-up is bounded progress-aware assembly plus measured flow/
latest-state handling for transient poses. Test native/Steam queue behavior and
both host directions first. Persistent inventories/construction must remain
reliable. A transport change needs its own schema/safety acceptance and is not
silently shipped here. Client prediction stays for A2-05.

## Runtime diagnostics, tests and release boundary

Existing opt-in diagnostics remain OFF by default. New fixed keys:
POSE_COLLECT_MS (native generation + visibility combined, avoiding per-unit timer
cost), POSE_ENCODE_MS, POSE_CHUNK_SUBMIT_MS, POSE_DECODE_MS, POSE_APPLY_MS;
POSE_QUERIES, POSE_QUERY_REUSE, POSE_VISIBLE_STATES. Submission spans native call
queuing/serialization work, not kernel delivery/peer receipt. No packet content,
player ID/name or endpoint is exported.

**122 test programs PASS on Java 17 and stock Steam Java 25.** New codec checks:
8088; native batch checks: 8. A2-02 handshake/readiness/reconnect/epoch tests,
strict identity checks, mouse contracts, combat and full native campaign tests
pass. An Alpha 1.0.4 disposable save also restores through the new JAR on Steam
Java 25. No save format was changed.

A fresh selector capability probe still fails with IOException before a game
connection. No networking escalation/bypass was requested. Actual Windows/Windows,
Windows/Linux, Linux/Windows, Steam invitations, real low-bandwidth/high-latency,
new import-and-restart, physical F5/Tab and long-session acceptance are **NOT TESTED**.
No public-release readiness or real-world latency gain is inferred.

One common Java 17-compatible diagnostic JAR is prepared for Windows/Linux, with
notices and matching source under `local/a2-04/test-kit/`; its convenience ZIP is
beside it. Linux native gameplay/startup remains physically unverified for THIS
build. Every participant must use that exact payload/protocol 4. Prior Alpha,
A2-02 and A2-03 artifacts/checkpoints and primary saves are preserved. No push,
publish, Workshop update, history rewrite or gameplay/visual reduction.

Follow [manual procedure](A2-04-MANUAL-TEST.md). Release only after paired physical
host-swap/visibility/rejoin/building tests; do not call reliable congestion fixed.

## Developer reproduction

Player testing needs only the JAR; the following is for rebuilding measurements.
Use a full Java 17 JDK and Python, with FPV_JDK pointing to that JDK,
FPV_MINDUSTRY to the separately installed 160.7 game directory and FPV_JNA to
JNA 5.19.1 (the build verifies its pinned checksum). No game/runtime is bundled.

1. Run `python tools/build-diagnostics.py --task a2-04 --run NEW_RUN_NAME`.
   It generates configuration from the sheets, compiles with `--release 17`,
   runs every test program, and refuses to overwrite existing runs. The original
   measured final run is named `verified`.
2. Compile the three `tools/java/Snapshot*Benchmark.java` /
   `tools/java/SlowAssemblyProbe.java` sources with `javac --release 17`. The
   classpath is the new diagnostic JAR, that run's `test-classes`, the installed
   `jre/desktop.jar` and JNA; put output in a new ignored local directory. Actual
   files are SnapshotTrafficBenchmark.java, SnapshotServerBenchmark.java and
   SlowAssemblyProbe.java (two benchmarks, one probe).
3. Run `SnapshotTrafficBenchmark legacy` against the preserved A2-03 JAR, then
   `SnapshotTrafficBenchmark compact` against the new JAR. Run
   `fpv.net.SnapshotServerBenchmark` once against each JAR. Use benchmark classes
   first, the chosen JAR second, matching test-classes third, then the same native
   host/JNA. The baseline uses its original ConnectionNativeHandlersTest fixture;
   do not put new production class directories ahead of the baseline JAR.
   All four measured invocations use `-Xms128m -Xmx512m`, `-ea` and a disposable
   `-Djava.io.tmpdir` on D:. Run sequentially, with no other regression processes.
   Capture stdout as UTF-8 CSV without PowerShell formatting/encoding conversion.
4. Run `SlowAssemblyProbe` against the new JAR; it invokes both preserved old
   reference and compact codecs through native serialization. Run
   `python tools/snapshot-link-model.py --before BEFORE.csv --after AFTER.csv
   --output NEW_MODEL.csv`; this refuses to overwrite its output. Inputs are the
   traffic benchmark CSVs, not native-sender CSVs.
5. To repeat stock Java 25 tests, invoke the same 122 main classes with its Java,
   `--enable-native-access=ALL-UNNAMED -Dfpv.test.expectNoLogging=true -ea`, the new
   JAR/test-classes/native host/JNA classpath and disposable tmp directory. Those
   are code/contract tests, not the import/restart or physical mouse workflow.

The original machine evidence hashes identify the retained local logs and raw
CSVs; fresh timing numbers will vary. Do not compare timings across differing
maps, host revisions, JVMs, concurrent runs or injected-fixture versions without
recording that difference. Thread allocation counters need a complete developer
JDK; no management module is required by the distributed mod.
