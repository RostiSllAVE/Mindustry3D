# A2-08b — FPV blueprint priority

Baseline: A2-08a `8b43af115e70b963b797b6c4d9442f1e40d3fb66`, branch `main`.
Unreleased diagnostic build: internal mod version 0.18.31, protocol 4. All peers
must install the exact same JAR; strict code/resource identity remains required.

## Controls

In **Tab / Build Mode**:

- With no block, platform or schematic tool selected, **left-click a visible
  unfinished blueprint** to work on it first.
- With a tool selected, use **Alt + left-click** (either Alt key). This retains
  the selected tool. An Alt-click miss is consumed and reports an error instead
  of placing a block at an unrelated location.
- **F3 remains pause/resume**, with its existing remapping and panel button.
  Selecting a blueprint never resumes an explicitly paused builder.
- RMB, Ctrl possession, Shift linking, ordinary placement and explicit queue
  clearing retain their existing roles. The priority press is fenced until release
  so it cannot also place, drag, configure or dismantle a block.

Automatic target selection uses the actual ray-hit floor. Manual floor locking
still controls placement but does not redirect a blueprint click to another floor.
The existing Build panel documents the controls and hover feedback identifies Z.

## Native behavior and implementation

Mindustry 160.7's `ConstructBuild.tapped()` inserts a `BuildPlan` at the front using
`unit.addBuild(plan, false)`. `BuilderComp` owns validation, progress, range/resource
availability and queue skipping. Native player snapshots carry the controlled
unit's queue and building flag to the authenticated server handler.

The FPV adapter previously picked physical buildings for inspection/configuration,
not pending plans. Upper timed schematic requests also had no remotely visible
blueprint representation. No independent scheduling queue is introduced:

1. **Z0:** Move the existing native plan object to the queue head, preserving
   progress, configuration, rotation and the relative order of the other plans.
   A friendly physical `ConstructBuild` absent from the queue can be reselected
   using the same native add-build path. Explicit dismantling requests are never
   silently replaced. Native scheduling can still skip unavailable work; later
   vanilla queue changes take precedence instead of being forcibly overridden.
2. **Upper floors:** Promote the selected entry in the existing `LayerPasteQueue`.
   Work already accumulated on displaced entries is parked per plan and restored
   when they resume. Work is not donated to a newly selected plan. Costs remain
   charged by the existing validated placement operation, once on completion.
3. **Cross-floor focus:** While a chosen upper entry is pending, temporarily
   suppress native work during the native entity-update phase, restoring the
   user's building flag before upper work and outgoing snapshots. Conversely,
   upper jobs wait while the chosen native entry is at the native queue head.
   Other players are unaffected. Completed/cleared/invalid/switched-unit focus
   expires; the remaining existing queues continue normally. No builder movement,
   teleportation, cost or build-speed parameter is changed.
4. **Picking:** Extend the existing `BuildingMousePicker` with pending-plan bounds
   and the same swept-box contact utility. Its scene result now retains opaque
   depth even when fog suppresses object identity. Choose the nearest visible
   blueprint face, including multi-tile footprints; higher opaque floors block
   lower plans. This is not nearest-to-player-altitude floor selection.
5. **Presentation:** Reuse the existing translucent construction ghost and outline
   batches for pending upper entries, with depth testing and frustum checks. The
   existing 256-ghost budget remains; outlined footprints remain available for
   additional visible entries. No animated blueprint paper or schematic format.

## Multiplayer and save compatibility

An appended **PRIORITIZE** action uses the existing bounded, sequenced, reliable
construction envelope. The server resolves it against the authenticated player's
current unit and existing pending work, checking XYZ range, team, resources and
upper placement/support rules. Ground local queue reordering follows the native
client-snapshot workflow; the server still validates and executes construction.
No client request contains progress, costs or claimed resource balances.

The new **construction-plans** state channel is required because upper pending
plans were previously server-only. It sends each accepted peer **only their own**
read-only pending entries through the existing reliable chunk/epoch/replay path.
It uses the normal 5 Hz persistent-state cadence, compares cached owner payloads,
and sends only changed views or requested initial-state replay. Empty payload is
3 logical bytes; each entry is 13 bytes, bounded to 4,096 entries / 53,251 bytes.
These sizes exclude existing packet framing. Pose transport, prediction, input
packets and protocol version 4 are unchanged. A2-02 readiness includes this channel.

There is no change to the layered save format (still format 5). Pending timed jobs,
focus and their presentation remain transient as before; restarting/reloading is
not a new promise to persist unfinished upper jobs. Completed structures still use
the existing save path. The exact fingerprint prevents mixing old/new code despite
the unchanged internal version. Network delay still applies to server execution
and owner views; this task makes no claim of improved real-world latency.

## Limits and verification

Already completed instantaneous upper single placements cannot be selected as
unfinished work. Elevated pending selection applies to existing timed schematic
jobs, including their queued block/platform entries. Invalid support or insufficient
resources produce the existing reason and leave the queue intact. The builder
must be within allowed range; selecting work does not invent an automatic travel AI.

Physical Windows/Linux visuals, OS mouse interaction and two-computer multiplayer
are **NOT TESTED** here. Automated native-content fixtures are not visual acceptance.
See [test matrix](A2-08b-TEST-MATRIX.md), [validation](A2-08b-VALIDATION.json), and
[owner procedure](A2-08b-MANUAL-TEST.md).

## Final automated results

- **131/131** regression programs passed on Java 17 and stock Steam Java 25.0.1
  without java.logging. A2-08a retains **45** passing checks. The final extended
  priority fixture passed **79** checks on both runtimes against this exact JAR;
  the full run contained its earlier 77-check version, followed by two additional
  ordinary-placement/cost checks. The owner-private network fixture passed **21**
  checks. No production code changed between those runs.
- **951** existing compiled classes remain byte-identical to A2-08a, including
  the native input adapter, F3 binding, flight/camera primitives, prediction,
  protocol codec, combat and platform input classes. The scoped construction,
  picking, private-channel integration, upper preview and generated manifest changed.
- GPL/third-party notices and Windows/Linux JNA resources are retained. All **2,065**
  files in the prior Alpha preservation snapshot and **21** preceding diagnostic
  assets retain their hashes. No release was overwritten or published.
- JAR SHA-256: `fca9ea19564b6aa30d1b03604f2429ffe32e329dbc0b6c886db8fa693f2b2b9e`.
- Canonical JAR/folder identity passes actual Java verification. Real socket setup
  is unavailable here (IOException before selector initialization); physical
  Windows/Linux and two-computer gameplay remain **NOT TESTED**.
