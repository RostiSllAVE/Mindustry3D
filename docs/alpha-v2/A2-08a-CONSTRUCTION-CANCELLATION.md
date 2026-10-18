# A2-08a — FPV construction pause and resume

Baseline: A2-07 `9134008dc75d94d7811d60b4624e12f40686658c`, branch `main`.
This is an unreleased diagnostic checkpoint, internal version 0.18.31, protocol 4.

## Confirmed cause

The FPV input policy suppressed native `pauseBuilding` in flight. Its default E
already controls altitude, so simply exposing E would create an input conflict.
The custom elevated-build input and command-mode early returns also bypassed the
native input handler. Separately, `LayerPasteQueue` never checked the builder's
native `updateBuilding` flag. It continued accruing work and spending resources
even when that flag was false, and deleted pending jobs on possession switches.

Reproduction against the accepted A2-07 class files confirmed both the suppressed
binding and a Z2 wall completing while `updateBuilding=false`, consuming 6 copper.
This was an automated native-content fixture, not a physical FPV playtest.

## Focused fix

- **F3** toggles construction activity in FPV flight, Build Mode and command mode.
  Rebind **Pause / resume construction (retain plans)** in the host Controls menu.
  The Build panel offers the same action; a brief HUD message confirms it.
- The input boundary handles the native Build-mode pause binding too, once per
  frame, preventing native/custom double toggles. E/Q retain their flight role.
- Pause changes only the controlled builder's native `isBuilding/updateBuilding`.
  It does not clear plans, reset progress, change resources, deselect a tool or
  initiate firing/mining. Explicit intent survives Tab and native empty-queue
  auto-pause, including elevated jobs which are absent from native `unit.plans`.
- On possession/world changes, the adapter reads the new unit's own state. It
  does not transfer the previous unit's pause intent to another player's unit.
- Server-owned upper jobs now stop accruing work while the builder is paused or
  no longer possessed. Their pending plans and accumulated work remain available
  when the original unit is controlled again. Invalid/dead builders still discard
  their jobs as before. Other players' jobs are untouched.
- Original 2D input delegates to the host. No priority ordering, save format,
  construction validation, costs, balance, protocol or reliable-action changes.

## Three separate operations

| Intention | Action | Result |
|---|---|---|
| Stop/resume work | F3 or Build-panel pause/resume | Keep committed plans and partially constructed world blocks |
| Deselect/cancel a tool or unfinished drag | Existing RMB/cancel workflow | Cancel the uncommitted gesture; committed construction can continue |
| Remove requests or a physical blueprint | Existing clear-building / upper queue-clear; explicit dismantle for a world block | Clear requests only when explicitly requested; dismantling remains a separate validated world action |

Resuming processes the existing queue order. To build something else first, use
the existing queue controls; construction priority selection is **not** added.
Native queue clearing does not itself demolish a physical `ConstructBuild`.

## Authority and limits

The existing native authenticated `NetServer.clientSnapshot` already carries the
building boolean and submitted native plans. Upper work consumes that server-side
flag; no additional RPC is needed. Local input stops immediately, while a remote
host observes it after the normal snapshot/network delay. Server validation and
resource charging remain authoritative. This is not a promise of zero network delay.

Single upper PLACE actions are already instantaneous: completed blocks cannot be
paused retroactively. Upper pause testing concerns the existing timed schematic
queue. Such pending jobs remain session-only; this task adds no persistence across
save/load or reconnect. Switched-away live-unit jobs remain within existing queue
limits until resumed, explicitly cleared, invalidated or the world is reset.

See [test matrix](A2-08a-TEST-MATRIX.md), [validation](A2-08a-VALIDATION.json), and
[owner procedure](A2-08a-MANUAL-TEST.md). Physical gameplay acceptance is pending.

## Final automated results

- **129/129** regression programs passed on Java 17 and again on stock Steam
  Java 25.0.1 (without java.logging). The new native construction fixture has
  **45** checks. Neither run is a graphical import/restart or OS input playtest.
- Actual Java JAR/folder fingerprint verification passed. **969** existing class
  files are byte-identical to A2-07, including networking, physics, combat,
  platform input and rendering. Only construction/input/UI integration and the
  generated design manifest changed; one new key-binding class was added.
- All **2,065** files in the prior Alpha preservation snapshot and **18** prior
  A2-02–A2-07 diagnostic assets retain their hashes. Windows/Linux JNA resources
  and GPL/third-party notices are retained; compiled bytecode targets Java 17.
- Test JAR SHA-256: `e3f60dbc5122af7cbe652a6da7b4a75c1fc1ff38ba00457dbb88a94a864b1138`.
- Real local socket setup is unavailable in this environment (IOException before
  selector setup). Physical Windows/Linux and two-computer gameplay are **NOT
  TESTED**, and the owner procedure remains necessary.
