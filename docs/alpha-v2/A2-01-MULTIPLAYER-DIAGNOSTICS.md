# A2-01 — Multiplayer diagnostics and root-cause investigation

Date: 2026-10-18. Repository: Mindustry3-Public. Starting checkpoint: `a95b535`
(owner's icon addition after Alpha 1.0.4 checkpoint `6090e7d`). Branch: `main`.
Baseline: Alpha 1.0.4 / internal 0.18.31 / protocol 3 / Mindustry 160.7.

## Outcome and limits

**Multiplayer is not fixed or certified by this task.** The investigation confirms
an avoidable local-pilot display delay and a substantial full-world reliable-pose
traffic cost. They explain mechanisms that can make remote play less responsive;
they do not establish the cause of the friend's connection/campaign failure.

The new Windows dedicated-server/two-client attempt could not start: the sandbox
denied a Java selector's loopback connection (`SocketException: Permission denied:
connect`, `Unable to establish loopback connection`). No new permission was
requested and no restriction was bypassed. Hardware/Steam/manual cases were not
available. Consequently there are **no new measured WAN/LAN RTT, live queue,
two-computer frame-time or long-session results**. See the separate test matrix.

All added runtime behavior is opt-in observation and local numeric report export.
No transport, packet schema, authority, physics, interpolation, combat, campaign
rules, save format or rendering decisions are changed. Alpha v1 artifacts remain
untouched. The diagnostic JAR is not a public release and must not replace the
published Alpha artifacts.

## Evidence-backed findings

| Finding | Evidence | Confidence and implication |
|---|---|---|
| Local pilot uses the remote display buffer | `FpvNetwork.copyLocalFlight()` calls `RemotePoses.copyFlight()`. The actual implementation and 24 deterministic wire/display checks show a new sample reaching its full position 100 ms after arrival | Confirmed. This is added input-to-display latency even on an otherwise fast link; it is not measured internet RTT |
| No client prediction or extrapolation | Client `before()` sets native owner XY from the latest server sample; `RemotePoses` clamps interpolation to [0,1]. An outage fixture holds the last state | Confirmed. Input responsiveness depends on the server round trip; severe outage behavior is not repaired here |
| Poses are full per-recipient reliable messages | `sendSnapshots()` scans native units for each accepted peer; `sendChunks()` always calls `clientBinaryPacketReliable`, including `poses`, every nominal 100 ms | Confirmed. Transient movement shares reliable delivery with persistent state; backlog/head-of-line blocking under congestion is a strong hypothesis, not a measured remote queue |
| Pose traffic grows linearly with unit count and recipient count | Real encoder/decoder measures 106 bytes/unit, plus count, own-flight profile and framing. Table below | Confirmed payload cost. Could be important on a slower host uplink; does not explain every small-map case |
| Rates are frame-bound upper targets | Input/pose/channel work uses `now >= next`, then `next = now + period`, once per applicable update callback | Confirmed. A 5 FPS client cannot submit 30 input updates/s; a slow host cannot produce 10 fresh snapshots/s. No physical FPS/RTT correlation collected here |
| Interpolation/reconciliation differ from native owner movement | Native owner XY takes the latest authority sample, while its FPV camera/body copy is interpolated | Confirmed paths. Could contribute to perceived judder; no new visual reproduction |
| Fingerprints are strict and container-independent | Existing 23-check fingerprint suite passes JAR/folder equality, real mismatches and failure isolation | Confirmed mechanism. Same displayed version alone is insufficient. Whether the friend's packages actually differed is unknown |
| An incomplete assembly is bounded, but its idle timeout is not proactive | `Assembly.accept()` checks the 5-second timeout only on another arriving chunk; world/connect reset clears the registry | Confirmed retention behavior. A quiet incomplete channel can retain its partial byte arrays until another arrival/reset. This does not prove an unbounded native send queue |
| Layer snapshots can be expensive to apply | `LayerRuntime.restore(..., true)` creates/validates a prepared native registry before reconciling and clearing it, even for matching geometry | Confirmed source path, unmeasured cost on a real campaign. Instrumentation measures channel apply time; no speculative optimization |

The fixed 100 ms buffer is only one component. A simplified input-response budget
is transport round trip + input scheduling (up to roughly 33 ms at the target rate)
+ host scheduling + snapshot scheduling (up to roughly 100 ms) + display buffering.
This is a **model**, not an observed latency measurement. At low FPS, schedule
gaps can be larger. Stabilized free-look can respond locally while the physical
unit still waits for authority, so camera motion alone is not a movement test.

### Actual wire-size measurements

Measured by `A201NetworkBehaviorTest` using the real encoder and decoder. These
figures exclude the optional owner's flight profile, native entity/build sync,
other FPV channels and native/TCP/IP/Steam framing. They assume the target 10 Hz
is achieved and use one recipient. FPV envelope/chunk framing is included in the
last column; decimal MB, not MiB.

| Unit states | Logical snapshot bytes | 12,000-byte chunks | FPV bytes/s at 10 Hz |
|---:|---:|---:|---:|
| 0 | 5 | 1 | 590 |
| 100 | 10,605 | 1 | 106,590 |
| 1,000 | 106,005 | 9 | 1,064,910 |
| 4,096 | 434,181 | 37 | 4,361,790 |
| 16,384 | 1,736,709 | 145 | 17,445,390 |

The current input envelope is 124 bytes (native framing excluded). At nominal
30 Hz that is 3,720 FPV bytes/s/client, much smaller than a populated pose stream.
As a capacity illustration only, 1,000 poses at 10 Hz exceed a 1 Mbit/s usable
uplink by about 940 kB/s before other traffic. That is **not** a measured friend
uplink, accumulated queue, retransmission rate or Steam relay characteristic.

### Codec elapsed-time/allocation microbenchmark

Windows development PC, complete Java 17.0.20.1, `-Xms64m -Xmx384m`; 10 warm-up
and 30 measured iterations per size. `tools/java/NetworkCodecBenchmark.java`
uses the real codec; the final sample was taken after the test suites completed
(the earlier exploratory sample is retained locally). This is one active-PC run,
not a thermally controlled hardware benchmark. Thread-allocation accounting is developer-only and is not
included in the mod. No GL, world scanning, socket, native entities or real link.
Times use elapsed `System.nanoTime` spans and can include GC/scheduling; they are
not thread CPU counters. These are microbenchmark results, **not gameplay FPS or
end-to-end frame time**.

| Units | Encode mean / p95 ms | Decode mean / p95 ms | Allocated bytes per encode+decode |
|---:|---:|---:|---:|
| 0 | 0.002693 / 0.004700 | 0.003053 / 0.007900 | 712 |
| 100 | 0.211907 / 0.342800 | 0.165410 / 0.234400 | 74,688 |
| 1,000 | 0.641493 / 0.907500 | 0.612917 / 1.126600 | 683,440 |
| 4,096 | 1.758320 / 3.899400 | 1.470037 / 1.637700 | 2,776,037 |
| 16,384 | 6.212957 / 7.743500 | 5.718090 / 7.080400 | 11,107,350 |

Do not extrapolate these into a measured FPS improvement or blame all latency
on GC. Real snapshots also query the physical world and serialize other channels.

## Architecture audit

| Area | Current behavior / boundary |
|---|---|
| Handshake | HELLO carries canonical payload identity; server ACK carries epoch and matching identity. Reliable native custom binary handlers on the game thread. Missing HELLO timeout 10 s; mismatches rejected |
| Input | Bounded axes/modes/aim and intent, not client coordinates or damage; unreliable target 30 Hz; strictly increasing per-peer sequence and matching world epoch; shared sequence also used for construction |
| Physics | Server-owned session integration, native XY bridge, crash/contact ledger and ground/boost authority; 0.5 s input silence selects stable hold |
| Native snapshots | One wrapper preserves native validation/build plans but suppresses client position claims. Entity snapshot wrapper calls its predecessor/native handler once, then restores upper turret control. No duplicate handler execution found in source |
| Poses | Per-recipient full unit scan, team/fog visibility check, up to 16,384 states, own-flight state; 10 Hz target; scan still visits units after the list cap |
| Display | Two complete samples; local-arrival timestamps, 100 ms interpolation, no extrapolation; no transmitted server timestamp or input ACK sequence |
| State channels | Up to 16 registered channels; 5 Hz persistent and 20 Hz combat/explosion targets. All channels are serialized when due, even if no peer needs a new version; byte comparison suppresses unchanged persistent transmissions |
| Chunking | Reliable 12 kB chunks, atomic assembly, <=16 MiB logical message; one in-flight revision/channel; no application ACK proving successful persistent-state application |
| Construction | Separate reliable commands, sequence/epoch/team/range/support/economy checks; full persistent layered-world replication and turret/power state channels |
| Weapon/entity sync | Native entity/health remains authoritative; mod channels carry XYZ visual and aim state. Client presentation/decoding competes with main-thread updates |
| Reset/reconnect | World/reset clears sessions, pose samples, assemblies, creator/boost and contact state; new host epoch, peer input watermarks/channel-version caches reset; client sequence retained across world load, reset on a fresh connection |
| Queue bounds | Peer/session cleanup and bounded per-channel reassembly found. Native provider/OS/Steam outgoing queue depth and retransmissions are not exposed by this instrumentation |

No background packet worker or explicit unbounded FPV application input FIFO was
found. The latest accepted controls overwrite a peer slot. This does **not** mean
that the native reliable transport cannot queue old state. A sender marks a
persistent channel version as sent when queued; it has no application-level
confirmation that decoding/applying it succeeded. Failure-specific recovery
needs a reproduced case before changing that behavior.

## Host-dependent failure: ranked hypotheses

1. **Different effective payload or failed initialization — medium confidence as
   a hypothesis, not confirmed on the friend.** Imported/Workshop duplicate copies,
   an older compiled folder or an extra file can change identity despite the
   same version label. The new report includes a validated public digest and
   verification rejection counts. Never weaken the check to make a mismatch work.
2. **Host uplink, reliable backlog or host-frame stalls — medium confidence for
   severe responsiveness, low for connection establishment.** Payload scaling and
   main-thread cadence are confirmed; actual remote link/queue data is absent.
3. **World/sector handshake/stream ordering — medium-low confidence.** Upper
   world data deliberately uses FPV channels instead of native world streaming;
   the exact failure stage and both peers' reset/ACK counts are needed.
4. **Steam invitation/relay path — unknown.** No account interaction or relay
   telemetry performed. Invitation success once proves neither direct delivery
   nor that a relay caused lag.
5. **Host settings/rendering differences — plausible confounder.** Flight tuning
   belongs to the host. Fullscreen, equal FPS caps and host FPS must be recorded;
   the previously confirmed windowed slowdown can starve desktop hosting.

## Campaign investigation

`A201CampaignChunkTest` passes 8 checks through the installed host's actual
`SaveVersion.writeCustomChunks/readCustomChunks` and the mod's registered hooks:
synthetic campaign upper wall, save inclusion, network-stream exclusion,
reset/world-load then restore, switching to an empty custom context, restoring
again without duplication. `LayerCampaignNativeTest` passes 21 campaign block,
export/import accounting and lifecycle checks. `LayerRuntimeNativeTest` passes
54 economy/structural/runtime checks. All use mock application/native content
and memory buffers; **none is a complete planetary UI or network campaign test**.

Inspection of the actual 160.7 host bytecode shows `SaveVersion.read` reads map
before custom data; map loading normally ends its world context and emits
`WorldLoadEvent` before custom chunks restore. The tested reset-then-restore
order retains upper state. The suspected blanket explanation that every normal
world-load callback erases restored custom data is not supported by that path.

The layered-world chunk has `writeNet() == false`, confirmed by the native writer.
An initial native world stream alone therefore cannot establish upper floors;
the FPV handshake and reliable layered-world channel are required afterwards.
Large state budgets or an apply failure may make this distinction important.

Entering/leaving real planetary sectors, first sector generation, loading a
copied owner/friend campaign, full native SaveIO disk roundtrip for that campaign,
campaign reconnect and switching physical host authority are **NOT TESTED**.
No owner's primary profile/save was opened or changed. No speculative campaign
fix was implemented.

## Opt-in instrumentation and interpretation

Enable **Settings -> FPV flight -> Record multiplayer diagnostics (local, opt-in)**.
It is OFF by default. Developers can also use `-Dfpv.netDiagnostics=true`.
Each process writes one randomly named JSON in its active profile's
`fpv-diagnostics` folder, at most every 15 seconds, on stop or clean disposal.
Export failure is isolated from gameplay. Mean/max/count are cumulative; p50,
p95 and p99 use the most recent 512 samples **per metric**, not a common duration.
The latest 64 export trend rows are retained, bounded in memory/file size.

| Metric / counter | Meaning and limit |
|---|---|
| `FRAME_INTERVAL_MS` | Wall-clock interval between update hooks, including stalls/waits; not CPU-only or GPU time |
| `SIMULATION_TRIGGER_MS` | Span from beforeGameUpdate to the diagnostic afterGameUpdate hook, including earlier callbacks. Desktop adapters registered later are excluded; this is not complete server CPU attribution |
| `DRAW_CPU_SPAN_MS` | Wall-clock span preDraw -> uiDrawEnd callback; includes CPU submissions and possible driver waits, excludes later callbacks/presentation; not GPU timing |
| `APP_RTT_MS` | Existing `NetClient.getPing()` sampled at 1 Hz. Actual 160.7 `pingResponse` uses `Time.timeSinceMillis`, so it includes scheduling/queues at both ends; not pure wire RTT |
| `APP_RTT_ABS_CHANGE_MS` | Absolute change of those sampled application ping values; not packet-level network jitter. Startup zeros can precede the first native ping response |
| Input gaps / silence | Local send and server receive gaps; last received input age when considered. Arrival-to-step is main-handler-receipt to session preparation, not client send-to-host time or kernel queue delay |
| `INPUT_SEQUENCE_GAP_UNATTRIBUTED` | Missing shared sequence values may represent construction/HELLO, not lost inputs. Do not label this a packet-loss percentage |
| Snapshot gap / local age | Complete pose arrival gap; time since last locally completed pose, not server generation age or one-way latency. No synchronized clock or server timestamp exists in protocol 3 |
| `INTERPOLATION_ALPHA` | Existing clamped display fraction; the setting is still 100 ms |
| Owner correction | Native owner XY distance before replacing it with the latest authoritative sample. It is not a prediction error measurement; there is no prediction |
| TX/RX and channels | FPV custom payload packets/bytes, including FPV envelopes, not total network traffic/native entity sync/transport overhead. Outgoing reliable/unreliable are counted separately |
| Encode/apply costs | Main-thread state-channel encode/apply spans; codec benchmark separately covers poses. No GPU readback or packet contents captured |
| Assembly gauges | Pending partial-channel count and retained byte-array bytes; not native/OS/Steam queue depth |
| Stale/partial | Stale/replayed revisions have a separate counter; incomplete-or-stale also includes entirely normal partial chunks and is not a loss count |
| Trend row | `[elapsedSeconds, frameP95ms, appPingP95ms, localPoseAgeP95ms, cumulativeTxBytes, cumulativeRxBytes, simulationP95ms]`; each p95 retains its metric's 512-sample window |

Only fixed enum keys, numbers, booleans, native build/revision/Java feature and
a regex-validated binary payload digest are exported. No Steam authentication,
player names/IDs, address, endpoint, UUID identity, save name, packet data or local
path is collected. The random report filename is not a Steam/player identifier.
Privacy, bounded collection, default-off behavior and failed-export isolation
have automated tests. A public digest is compatibility information, not auth.

Raw OS RTT, synchronized one-way latency, GPU time, kernel/Steam send queue size,
actual per-frame allocations, UDP loss, wire bandwidth and host-generated snapshot age remain
**unavailable / NOT TESTED**. No extra ping protocol or dependency was introduced.
Timing spans overlap; do not sum them into a claimed total CPU frame time. The
allocation benchmark is per codec operation on Java 17, not per frame on the
stripped Java 25 runtime, which has no management module for that counter.

## Reproduction, evidence and next work

See [test matrix](A2-01-TEST-MATRIX.md), [player procedure](A2-01-MANUAL-TEST.md),
[ranked next fixes](A2-01-NEXT-FIXES.md) and [numeric evidence](A2-01-EVIDENCE.json).
Raw local test logs and temporary profiles remain ignored under `local/a2-01`
and `local/net-lab/a201-baseline`; do not publish them without sanitization.

Build the unreleased diagnostic artifact with `tools/build-diagnostics.py --run`
using a new run name, complete JDK 17 and the normal documented host/JNA inputs.
It refuses existing output directories and never writes `dist/`. Its metadata
stays at baseline 0.18.31, but the compiled payload necessarily differs: every
participant must install the exact same diagnostic artifact. Do not mix it with
the public 0.18.31 JAR/folder. GPL/JNA/libffi notices remain embedded.

No corrective gameplay change, publication, push, Workshop update, Topic change,
history rewrite, permission bypass or primary campaign mutation was performed.
