# A2-05 — Local-pilot prediction and responsive FPV

Baseline: accepted A2-04 `012710651f4e48427c352e1b642e4681e22ecb7f`, branch
`main`, Mindustry 160.7, internal 0.18.31 / protocol 4. Local diagnostic work only.
The owner accepted A2-04; that is not physical acceptance of this new build.

## Confirmed baseline and implementation

`RemotePoses.copyFlight` used the same 100 ms presentation buffer as other units.
`FpvNetwork.copyLocalFlight` selected that path for the joining pilot, while the
frontend correctly skipped physical integration for network-owned bodies. Thus
own movement waited for input transport, server integration/snapshots, return
transport and delayed presentation. Stabilized freelook already responded locally;
Acro body/camera orientation did not have a local flight model.

The new **LocalPilotPrediction** owns a separate display controller. It calls the
existing Acro FlightController, StabilizedFlight and BuildNavigator with the
server's profile and speeds. It never calls native move, AI, weapons, crash damage,
inventory, construction or boost mutation APIs. Native owner XY/rotation still
reconcile to the exact server sample for native interaction/range validation.
World3DAdapter/UnitAltitudeSystem physical providers are not replaced with a ghost.

Current-frame mouse and keys are sampled before prediction. Camera/picking runs
once afterwards, and the final normalized aim is submitted normally. Local hull
and aircraft mount positions can draw at the predicted pose; gameplay muzzle
origins, projectiles, collision results and actual firing events stay server-owned.
Other units retain the original interpolation, with no additional buffering.

### Server receipt, protocol and replay

- **Protocol remains 4.** Existing INPUT, compact full POSES, chunk and persistent
  channel schemas/rates are unchanged. A small, server-only **STATE adjunct** uses
  the previously unused STATE kind, paired with the exact completed pose revision.
  It carries applied authenticated input sequence, held simulation duration, unit/
  player/type/team, movement category, server speeds, ground acceleration/drag and
  motor/remainder memory. It adds no client coordinate, hit, tuning or HP claims.
- This is new protocol-4 STATE semantics, not permission to mix old/new binaries.
  Strict canonical code/resource identity still rejects mismatches, even though
  both builds display 0.18.31. Every participant must install the same new JAR.
  The adjunct is optional for A2-02 READY; missing it cannot block initial world
  readiness. Delayed/duplicate/foreign-revision receipts cannot replace newer data.
- The server records the sequence actually consumed by the pilot, not merely an
  incoming packet. A read-only FlightController.Memory checkpoint preserves motor
  spool and fractional integration time. Boost/creator accessors expose their
  actual controller memory/speed without changing tuning or save formats.
- A bounded ring retains **512 frames and 512 sent-input stamps**. Replay starts
  from the authority and uses local send time plus server-held duration as an
  approximate command-time anchor; no synchronized wall clocks are required.
  This is not deterministic rollback of the complete Mindustry simulation.
- Replay lead is capped at **0.6 seconds**, source freshness at **0.75 seconds**;
  absent/evicted history is not treated as complete. Server input-timeout holds
  also stop speculative integration. Very late data never causes unlimited motion.
- Small residual position/quaternion corrections decay with an 80 ms time constant.
  The quaternion correction is applied to the live attitude, so new input is not
  routed through an 80 ms input filter. There is no fixed prediction delay.
  Large corrections (over max(2 tiles, twice radius)), ownership/type/team/mode
  changes and long discontinuities reset directly; physical clipping wins over
  cosmetic smoothing.

### Loss recovery: confirmed candidate defect and correction

The first bounded candidate switched straight back to the latest raw server pose
when replay lead expired. Under the deterministic 400 ms / loss fixture this
produced multi-tile jumps, despite good average response. That candidate was not
published. [Candidate measurements](A2-05-candidate-before-hold.csv) are retained.

The final behavior **holds the last display without continuing integration** and
shows `LOCAL POSE HOLD` in network status. A short expiry is not treated as a new
unit/world; a fresh paired receipt can replay/reconcile the held pose. No extra
buffer was introduced. Long outages, actual teleports and large errors may still
require immediate correction. A2-04 reliable backlog/congestion remains unresolved.

## Movement and authority boundaries

- **Native aircraft:** same flight integrators/profile, immediate local input,
  conservative terrain/structure/unit clipping; moving targets are observed from
  replicated state. Full server mass exchange/knockback is not run locally.
- **Ground units/walkers:** bounded XY kinematics using native-style acceleration
  and drag with server speed. This is an approximation, not a client native AI/
  locomotion controller. Q/E stance, step/ledge/support changes and exact Z remain
  server-confirmed. Conservative native passability/terrain/upper-volume checks
  can hold a valid server traversal until its next snapshot.
- **Boost/creator:** never started from an unapproved client request. Once the
  server confirms airborne state, the corresponding flight presentation runs.
  Creator retains its existing stabilized/Build-hold semantics. Native boost
  auto-clearance and transitional controls can differ briefly during replication.
- **Landing:** continue only server-confirmed velocity with visual clipping; do
  not decide landing sites, touchdown, health, leg folding or flight permission.
- **Fixed turrets:** cannot translate. Death, respawn, possession, team/type
  changes, sector/world/epoch reset and disconnect drop the old ghost/history.
- **Build/commands/menus/2D:** existing input fences and native command/ownership
  mechanisms remain. Build time does not slow a shared world. Cursor capture is
  unchanged. Leaving FPV clears presentation; idle clears are cheap. The optional
  `Local pilot prediction (multiplayer presentation)` setting defaults ON; OFF
  restores the A2-04 own-pilot interpolation path for comparison.
- No predicted damage, shots, ammunition/reload, resource transfer/spending or
  construction completion is executed. Server-driven effects/animations may lag
  the local ghost. A camera preview ahead of native XY is not proof that a build
  or other action has passed server range/support/permission checks.

### Read-only collision query cost

Repeated replay contacts initially created many duplicate Unit3DState values.
PredictionContact now reuses immutable queried poses within **one visual call**,
bounded to MAX_UNIT_STATES. The cache is cleared in a finally block, including
failure, and retains no unit references between calls. RammingCollision gained
an optional read-only pose source; existing authoritative constructors still use
the exact original World3DAdapter query and unchanged collision mathematics.
The server damage/impulse solver is never invoked by the predictor.

## Measured results

Final measurements and their limits are inserted below. Full raw CSVs, test and
artifact hashes are included beside this report and in A2-05-VALIDATION.json.

### Measurement method and boundaries

Windows development PC, Temurin Java 17.0.20.1, native Mindustry 160.7, heap128..512 MiB. Final runs are sequential after all test programs finish. Earlier runs remain locally. Timings include OS scheduling/GC on an active PC; they are elapsed spans, not isolated CPU cycles or game FPS.

**Controller model:** 120 Hz display, 60 Hz reference controller, 30 Hz inputs, 10 Hz full pose/receipt; 20 seconds per case, six modes × five RTTs × two link profiles =60 cases. Seeds2505 +RTT +Java mode hash are fixed. Impaired cases add independent ±15 ms one-way jitter, 5% input loss, 5% whole completed-frame loss and occasional duplicates. Frame loss here is a robustness stress, NOT how reliable TCP/Steam retransmission behaves. It is not a network congestion emulator. Reordering is covered separately by protocol/receipt tests.

The actual shared Acro/Stabilized/Build primitives and actual pose/receipt codecs are used. Ground reference motion is a simplified native-style XY model; walker Z is a supplied server trajectory, and boost changes category at a fixed simulated permission time. These are not full native battles or proof of physical walker/boost gameplay. The reference is the A2-04 RemotePoses implementation, whose class is byte-identical in both JARs.

Input-to-visible movement means crossing1e-4 tiles of horizontal displacement after a command starts at1.037 seconds (off tick/snapshot boundaries). Resolution is8.333ms. Acro must first create physical tilt/thrust; its position response is not expected in one frame. The camera/mouse hardware, render queue and monitor are absent. Interpolation can smear onset between samples, so baseline onset is not simply RTT+100ms.

Position/orientation errors compare display against the contemporaneous reference server controller after two seconds, NOT against an omniscient zero-latency player. Prediction can intentionally lead current server motion by the outstanding input; error is not expected to be zero. CPU/allocation spans cover display acceptance/replay/step/copy/body creation, excluding input construction, decoder/network/UI/renderer work and native collision queries. Separate contact measurements appear below.

### Fixed-link movement onset (milliseconds, modeled)

| RTT | Stabilized before → after | Acro before → after |
|---:|---:|---:|
| 0 | 71.3 → 4.7 | 171.3 → 63.0 |
| 50 | 96.3 → 4.7 | 204.7 → 63.0 |
| 100 | 229.7 → 4.7 | 246.3 → 63.0 |
| 200 | 279.7 → 4.7 | 371.3 → 63.0 |
| 400 | 471.3 → 4.7 | 571.3 → 63.0 |

Build/ground/walker modeled onset is4.7ms in the fixed-link runs; this does not claim equivalent native locomotion accuracy. Boost onset is measured from a separate movement command after the modeled permission boundary, NOT from the boost-toggle request. See [all60 rows](A2-05-prediction-model.csv).

### Errors and numeric smoothness: 400ms RTT + jitter/loss

| Mode | Position p95, tiles | Attitude p95, degrees | Position corrections/s | Hold frames /2401 | Max correction, tiles | Frame-delta change p95 before → after, tiles/frame² |
|---|---:|---:|---:|---:|---:|---:|
| ACRO | 3.253 | 6.968 | 9.25 | 1 | 0.981 | 0.1839 → 0.0592 |
| STABILIZED | 1.890 | 2.628 | 8.45 | 14 | 0.399 | 0.0825 → 0.0012 |
| BUILD | 1.159 | 0.000 | 7.85 | 2 | 0.143 | 0.0535 → 0.0008 |
| GROUND | 1.937 | 2.570 | 8.65 | 5 | 0.362 | 0.0864 → 0.0017 |
| WALKER | 1.937 | 2.576 | 9.30 | 12 | 0.423 | 0.0897 → 0.0023 |
| BOOST | 1.903 | 2.629 | 8.15 | 21 | 1.152 | 0.0830 → 0.0021 |

These are numeric display-transform proxies, not physical camera comfort or GPU frame pacing. Position correction counts use a1e-5-tile threshold and exclude initial/ownership/category resets; attitude error is separate. All60 cases have zero large same-category hard corrections, but this is not a guarantee against native collision/knockback or long-outage snaps. Holds are real limitations, not discarded samples.

In the pre-hold candidate the maximum single-frame change for impaired400ms GROUND/WALKER/BOOST was4.283/5.216/5.943 tiles. Final maxima are respectively 0.099/0.099/0.169 tiles. Comparing maxima does not remove ordinary physical movement or prove an internet latency improvement.

### Added CPU/allocation cost

| Mode, fixed400ms | Baseline mean µs/frame | Prediction mean /p95 µs/frame | Baseline → prediction bytes/frame |
|---|---:|---:|---:|
| ACRO | 0.438 | 2.275 / 14.200 | 154 → 256 |
| STABILIZED | 0.478 | 1.682 / 8.100 | 154 → 256 |
| BUILD | 0.327 | 0.875 / 3.600 | 154 → 256 |
| GROUND | 0.380 | 1.544 / 8.400 | 154 → 256 |
| WALKER | 0.213 | 1.154 / 7.000 | 154 → 256 |
| BOOST | 0.188 | 1.049 / 6.500 | 150 → 256 |

Prediction adds CPU and allocation work; no FPS improvement is claimed. Histories are fixed arrays; retained controls are capped512. The measured controller/body path typically allocates256 bytes/frame versus roughly154 baseline. Actual frontend input/aim records and world rendering add costs outside this microbenchmark.

### Native read-only contact batches: cache OFF versus ON

Actual native Flare entities/quadtree and world queries in a dense synthetic40×40 map. 100 warm-up batches and200 measured batches;1 or48 callbacks per batch. No AI battle, real networking or GPU. A48-contact batch represents repeated replay work, not an assertion about every frame. Counters include native state generation/query allocations; cache backing storage was warmed.

| Units /contacts | Mean µs OFF → ON | p95 µs OFF → ON | Bytes/batch OFF → ON |
|---|---:|---:|---:|
| 100 /1 | 6.4 → 3.0 | 14.4 → 4.0 | 133 → 128 |
| 100 /48 | 61.8 → 40.0 | 103.0 → 59.2 | 6250 → 6144 |
| 1000 /1 | 39.8 → 41.9 | 67.3 → 80.0 | 17152 → 17152 |
| 1000 /48 | 582.3 → 247.3 | 765.8 → 351.1 | 823296 → 23168 |
| 4096 /1 | 54.0 → 51.1 | 94.2 → 71.4 | 46336 → 46336 |
| 4096 /48 | 2068.6 → 914.3 | 3050.1 → 1537.5 | 2224128 → 52352 |

Cache effects are not uniformly positive for a single query; retain the slower/noisy rows. The largest reuse gain is repeated native state generation during replay. Geometry sweeps and quadtree traversals still run; ceilings, shield volumes and dense layered maps may add more cost. [Raw contact measurements](A2-05-native-contact.csv).

### Wire overhead and unchanged systems

The example server receipt is91 bytes,114 with the existing FPV envelope,102 after the actual native LZ4 serializer. At10Hz this example adds1140 FPV bytes/s or1020 native-serialized bytes/s per owned recipient. Values/compression vary; this excludes TCP/IP/Steam/retransmission and is NOT measured NIC traffic. No owner receipt is sent without a server pilot session.

A2-04 compact POSES, RemotePoses, NetProtocol, ModFingerprint, ConnectionLifecycle and UnitMotionBridge class bytes are unchanged. The receipt does not resend the flight profile/state or change pose/input rates. Construction, inventories, combat and persistent state remain reliable and authoritative.

## Tests, compatibility and limitations

Automated suites exercise authority/readiness/reconnect/identity, campaign saves,
existing flight/collision/mouse/render contracts, memory replay, receipt bounds,
ordering, stale holds, eviction and native read-only contacts. Native fixtures
use in-memory connections and an explicitly injected fixture identity; they are
not a successful real handshake or two-computer game. A separate actual JAR/folder
fingerprint check and old disposable campaign restore are recorded independently.

The common Java 17-compatible diagnostic JAR includes the existing Windows/Linux
native resources and unchanged license notices. New developer measurement classes
are excluded; existing runtime diagnostic harnesses remain unchanged. No game
binary, Java runtime, profile or save is required by the player package.
Java allocation management is used only by developer benchmark tools.

The selector capability probe fails with IOException before any connection in
this environment. No networking permission escalation/bypass was attempted.
Real Windows/Windows, Windows/Linux, Steam invitation, physical import/restart,
mouse/camera comfort, F5/Tab and long-session acceptance for this artifact remain
**NOT TESTED**. Existing code/contract tests do not substitute for those checks.
Linux physical startup of this artifact is also NOT TESTED.

Remaining limitations include approximate ground motion/Z, 30 Hz sampled inputs
(brief or lost input can disagree with local frame history), conservative visual
contacts, server-driven animation/VFX lag, brief holds under loss and unavoidable
large corrections after long outages/warps. This does not accelerate authoritative
shots/builds, solve A2-04 congestion or establish a real internet latency gain.

Follow the [three owner checks](A2-05-MANUAL-TEST.md), especially client OFF/ON,
host swap, terrain/upper walls, walkers/boost/landing, death/possession and actual
actions seen from the other computer. Do not publish this diagnostic candidate.

## Developer reproduction

Use a complete Java 17 JDK/Python, the separately installed 160.7 host and pinned
JNA, with FPV_JDK / FPV_MINDUSTRY / FPV_JNA as documented for A2-04. Run
`python tools/build-diagnostics.py --task a2-05 --run NEW_NAME`; existing outputs
are preserved. The final tested run is `acceptance`.

Compile PilotPredictionBenchmark.java and PilotContactBenchmark.java against that
JAR, its test-classes, the native desktop.jar and JNA. Run the default-package
`PilotPredictionBenchmark` and `fpv.net.PilotContactBenchmark` sequentially under
Java 17 with `-Xms128m -Xmx512m -ea`, disposable tmp on D: and no concurrent test
processes; capture stdout as UTF-8 CSV. Both use deterministic fixtures and no
socket. Stock Steam Java 25 tests use the same JAR/test classes/native host/JNA,
`--enable-native-access=ALL-UNNAMED -Dfpv.test.expectNoLogging=true -ea` and a
disposable tmp directory; no runtime modules are added.

This source checkpoint, matching JAR/source/checksums and procedure are packaged
under `local/a2-05/test-kit/`, with a convenience ZIP beside it. Prior Alpha and
A2-02/A2-03/A2-04 assets, checkpoints and primary saves are preserved. No push,
publish, merge, history rewrite or Workshop update is performed.
