# A2-02 — Connection lifecycle investigation

Local, unreleased development based on `f4d592c7973dd646a47277a7874813ff4e2f4efd`
(A2-01), itself based on Alpha 1.0.4 / internal 0.18.31. Date: 2026-10-18.
This is a connection/state-initialization pass. It is not a multiplayer
certification or a movement-latency fix. No publication or Workshop update.

## Evidence and confirmed defects

The A2-01 diagnostics, matrix, manual test and next-fixes report were read.
The current A2-02 brief supersedes the older next-fixes table's A2-02 latency
label. The friend's reverse-hosting failure still has no controlled reproduction.

An isolated probe loaded the preserved A2-01 JAR with real native content,
Net/NetClient/NetServer and Arc mock application services. It invoked the actual
FPV packet handlers/events, with no socket. Before-change observations:

| CONFIRMED BUG | Before-change result | Correction / verification |
|---|---|---|
| ACK alone unlocked FPV | `allowed=true`, zero snapshots, status claimed matching authoritative server | Explicit readiness state machine; ACK, native world, every initial channel, poses and the live owner's pose are required. Handler and deterministic tests PASS |
| State application failure remained apparently connected | Rejected counter increased, but `allowed=true` and successful status remained | FAILED state, sanitized channel/reason, diagnostic flush, queued disconnect and reconnect instruction. Handler tests PASS |
| Failed application was not retried by replaying the same revision | Two deliveries caused only one consumer attempt; reassembly had already marked completion | Do not continue a partially applied session or blindly replay it. Fresh connection/world clears assemblies and applies a fresh initial snapshot. Failure-to-reconnect test PASS |
| First host ACK could carry epoch zero | HELLO before the first host update produced `epoch=0` | Allocate a nonzero epoch before encoding ACK; actual handler test PASS |
| Previous-world movement intent survived world reset | `forward=1.0` before AND after WorldLoad | Clear submitted and per-peer controls, pending-input flags, watermarks and channel-send caches; old epoch rejected, fresh epoch accepted in handler tests |

These are **FIXED in automated handler/state tests**. They are plausible causes
of unusable sessions, not proof of the friend's historical root cause.
The codec's completed-revision rule itself is preserved; a consumer failure
terminates the partial session rather than pretending to roll its side effects back.

## Connection contract

Host: create native world -> native connection/PlayerJoin -> verify HELLO protocol
and canonical payload -> send nonzero-epoch ACK -> create authoritative unit
sessions -> send poses and registered channels. Existing native entity ownership,
input validation, range/team checks and building authority remain in place.

Client stages:

1. **NATIVE_WORLD**: connection begun, waiting for WorldLoad and native loading to
   finish (`isGame`, not generating, not NetClient connecting).
2. **VERIFYING**: require strict protocol and matching canonical payload ACK.
3. **SYNCHRONIZING**: apply complete pose snapshot and every registered state
   channel. Desktop currently registers 12 channels, including layered-world,
   layer-turrets, defense, combat, profiles and boost/creator state.
4. **WAITING_FOR_UNIT**: initial world is complete, but no live matching controlled
   unit/owner pose is available. Spectating/death does not time out a valid world.
5. **READY**: F5 and custom building may be used. The status is visible in FPV
   settings even before entering FPV, and a small READY confirmation is shown.
6. **FAILED**: show a useful reason, stop partial FPV state, release the native
   connection through its existing disconnect path and request reconnect.

State arriving before native world completion is not applied to an incomplete
world. The client requests initial state again using the existing HELLO; the
host's existing HELLO handling clears per-peer send caches. A duplicate same-epoch
ACK does not reset progress. World/epoch changes invalidate readiness and stale
poses. A new connection resets sequences; a world load in the same connection
preserves the client's monotonic sequence. Delayed failure callbacks are guarded
by connection generation, so they cannot disconnect a newer attempt.

While waiting for a live owner's pose, only neutral input is sent after complete
initial synchronization; active piloting is gated to READY. This allows normal
server unit-session establishment without granting early control. No client
prediction or authority has been introduced.

Timeouts are separately classified: native loading 120 seconds; missing ACK 15
seconds after WorldLoad; incomplete initial state 45 seconds without progress
on a missing channel, at most 180 seconds total after native usability; missing
live owner pose 45 seconds. Fresh worlds/epochs get fresh budgets. Repeated
already-complete poses cannot indefinitely conceal a missing state channel.
These bounds have deterministic tests, not a measured slow-internet acceptance.

## Diagnostics and scope

Existing opt-in recording remains OFF by default and writes bounded summaries
locally. Added fields: stage, compatibility acknowledgement, native-world flag,
required/applied/missing channels, failed channel, native-world/handshake/ready
milliseconds, READY/failure counts, early-state drops, state-apply failures,
first transmitted input, first accepted input and first usable active owner input.
All new exported labels are fixed enums, not player-supplied exception text.

First usable input measures authenticated active input accepted for a valid owner
at the host. It is NOT input-to-photon latency, movement onset, or pure network RTT.
Host exports identify HOST rather than reusing a previous client failure stage.
After native disconnect, the last failure remains readable until a new attempt.
Export tests include a synthetic private exception/settings/endpoint and verify
that none enters the report. Native game logs are not covered by that guarantee.

Unchanged: protocol 3 and packet formats, 30 Hz input, 10 Hz poses, 5 Hz persistent
channels, 20 Hz combat/effect channels, 100 ms presentation buffer, reliable pose
transport, prediction, saves, gameplay, camera/input code, metadata and licenses.
A full initializer/bootstrap refactor was not justified: the friend's failure
has not been reproduced in FpvMod initialization. Only the connection state/error
path and one status label were changed outside diagnostics/tests.

## Actual tests and limitations

See [test matrix](A2-02-TEST-MATRIX.md) and machine-readable
[validation](A2-02-VALIDATION.json). Local raw logs are retained under
`local/a2-02/`; intermediate failed fixture/build attempts were not deleted.
The final artifact is under `local/a2-02/accepted/`, and an installable bundle plus
corresponding source are under `local/a2-02/test-kit/`. Checksums are in the kit.

- The socket capability probe fails in this environment during selector/socket
  setup with IOException. No new permission or firewall change was requested.
  Actual network game/application processes and paired vanilla comparison are
  **NOT TESTED**, not failed player connections.
- Native handler tests use an in-memory provider, mock app/UI and an explicitly
  injected fixture verification token. They do NOT prove a real transport,
  native disconnect dialog, Steam invitation, mouse capture or physical join.
  Actual packaged JAR/folder fingerprint identity is checked separately.
- **NOT REPRODUCED:** the friend's reverse-host failure, severely delayed Steam
  session, or a physical campaign-specific initialization failure. Historical
  Windows/Linux success and delay reports are retained as owner reports only.
- **NOT TESTED:** current two-device Windows/Windows, both Windows/Linux host
  directions, either Steam-invitation host direction, physical F5/Tab/building,
  actual rejoin without restart, menu/Alt+Tab/capture, campaign sector transitions,
  prolonged sessions, Linux startup of this new binary and real Workshop loading.
- Native upper-layer decoder restores a server-authorized test snapshot before READY;
  this is not a physical campaign/Workshop or full gameplay acceptance.

## Owner action and next stage

Use the [manual procedure](A2-02-MANUAL-TEST.md) on both Windows/Linux laptops
and with the friend. All participants need the EXACT SAME A2-02 JAR/payload,
not simply matching displayed version 0.18.31. Send paired sanitized reports
from both host directions, including unsuccessful cases and the stage reached.

Connection acceptance remains pending. A2-03 planning or unrelated investigation
can begin, but do not call this connection milestone accepted or use it as a
verified multiplayer baseline until the physical host-swap/rejoin matrix passes.
A2-01 latency/traffic issues remain for A2-04/A2-05. No FPS, latency, Steam or
Linux multiplayer improvement is claimed by this checkpoint.
