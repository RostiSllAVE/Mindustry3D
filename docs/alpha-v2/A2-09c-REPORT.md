# A2-09c — Upper-floor construction reliability and build time

Baseline: accepted A2-09b, `2901659fc41b4e18242c65837e94ba9427ae873c`, branch `main`.
Owner accepted A2-09b volumetric copying before this pass. This checkpoint is an unreleased diagnostic; published Alpha assets remain unchanged.

## Phase 1 findings and gate

Two queue defects were reproduced against the exact A2-09b JAR before changing production code:

1. With global `infiniteResources=true` and `instantBuild=false`, an upper pasted copper wall waited **36 simulation ticks**. Native `ConstructBuild.construct` finishes immediately in that sandbox case. The old upper queue tested only `instantBuild`, making large sandbox pastes unnecessarily serial/timed.
2. A resource-blocked first entry prevented a later independent, valid entry in the same volume from building. With no copper but adequate titanium, both entries were still pending after **300 ticks**. This was execution starvation, not a renderer acknowledgement problem.

Additional inspection found that timed jobs each received a full builder-speed budget, and invalid heads could accumulate work during resource/range waits. Those inconsistent scheduling semantics are removed. Native and upper work also share builder ownership rather than progressing concurrently.

**The owner's intermittent manual-placement delay is not independently reproduced.** In the baseline fixture, a manual sandbox command mutated the authoritative grid synchronously. No confirmed post-completion renderer timer, invalidation defect, reliable-channel deadlock, or corrupted state was found. This pass does not claim that every instance of the owner's manual symptom has been explained. No serious unresolved authoritative/replication defect was observed, so Phase 2 proceeded on the repaired queue, with bounded timing available for a remaining real-play reproduction.

## Implementation

- Player PLACE commands now enter `LayerRuntime.acceptConstruction`, including the authenticated network handler and host submission. Survival manual placements use the existing `LayerPasteQueue`; trusted completion/rebuild internals retain the atomic validator/payment/mutation operation. There is no second construction engine.
- Sandbox global infinite resources and explicit instant-build rules retain immediate/next-update completion. The existing 32-completions-per-job/update guard remains: instantaneous mode does not submit unbounded work in one frame.
- A bounded scan rotates up to 64 blocked entries per job/update, retaining their individual work and original blueprint metadata. Only physically supported, in-range, authorized and affordable work advances. Missing support is never fabricated; later passes revisit parked dependencies.
- One upper-work budget per builder is shared across jobs. Native-builder progress is observed using reusable plan/progress buffers; upper work yields when native work actually progressed. An unavailable native plan does not indefinitely block independent upper work. Explicit A2-08b focus retains control of scheduling.
- Work uses simulation delta, native `block.buildTime`, build-cost multiplier, unit type build speed, unit build-speed multiplier and team/world build-speed rules. Platforms retain the existing custom 60-tick base, now also cost-multiplier scaled. No wall-clock waiting is used for progress.
- Work is preserved on F3 interruption, range loss, unit switching and priority changes; these pauses do not bank future work. Explicit queue cancellation still removes plans; stopping construction with F3 does not.
- **Existing upper-layer resource semantics are deliberately retained:** the complete modified recipe must be available before upper work advances; items are charged atomically once on completion. Upper work does not incrementally consume/refund items like native Z0 `ConstructBuild`. Costs are not copied from source inventories, and paused/unfinished work creates no operational building or usable support.
- Manual pending footprints cannot overlap existing queued work. A new sequence number is not permission to queue the same footprint twice. Manual strokes on the same owner/unit/floor/mode reuse one job.
- Existing translucent ghosts gain a green progress strip. Server-authoritative owner-private plan metadata v2 adds one quantized progress byte per plan; decoding remains bounded/atomic and accepts the previous v1 representation. Progress is presentation-only and never accepted as a completion command. Existing protocol 4, reliable state transport, replay protection and matching-payload checks remain intact.
- Completed components increment the existing geometry revision and are excluded from pending ghosts immediately. The real visibility/chunk-cache preparation is checked on its next invocation. Config/link finishing still uses A2-09b's separate retry logic; it does not postpone visible completed geometry.

World save format stays **5**. Pending upper jobs/work remain session-only as before; this pass does not add saved partial-construction state. Flight, combat, physics, camera, mouse backends, collapse rules and clipboard format are unchanged.

## Measurements and evidence

See `A2-09c-VALIDATION.json`, the test matrix and ignored `local/a2-09c/` logs. CPU command timings include validation/JIT and are not physical mouse-to-display measurements.

| Scenario | A2-09b | A2-09c |
| --- | --- | --- |
| One sandbox upper pasted copper wall | 36 updates / 0.600 simulated s | 1 update / 0.0167 simulated s at 60 Hz |
| Titanium entry after copper-blocked head | Absent at 300 ticks; 2 plans waiting | Completes at tick 72; copper blueprint retained |
| Survival manual upper copper wall, Alpha | Immediate world mutation; no duration | Exactly 36 ticks / 0.600 simulated s |
| Two upper copper-wall jobs, same Alpha | Separate full-speed budgets in code | Exactly 72 ticks total |
| Survival cost multiplier 2, unit multiplier 2, team multiplier 2 | Not measured | Exactly 18 ticks, twice the recipe |
| 256-entry upper sandbox volume | Not measured | First 32 in update 1; all 256 in update 8 |
| Completed block to CPU render-cache preparation | No physical measurement | Next sync; CPU timing recorded separately, no pixel/GPU claim |
| Remote wire transit / real display latency | NOT TESTED | NOT TESTED |

Input-to-queue measurements start at the synthetic server command entry, not a physical click. Queue-to-commit timings include intentionally required work or legitimate waiting. Commit-to-render-ready timings exclude GPU/OS presentation. Existing reliable construction/world channels tick at 5 Hz, so remote display can additionally wait up to roughly one polling interval plus actual transport/chunk/application time; no real-network bound is claimed or transport workaround introduced.

## Optional bounded diagnostics

With JVM property `-Dfpv.construction.timing=true`, each runtime retains at most 256 timing samples, 256 waiting queue markers, 256 recent commits, and logs at most 256 samples per world reset. Default is OFF. Events distinguish request-to-admission, queue-to-commit (including build/wait), commit-to-render-ready and replica application CPU time. No synchronous GPU readbacks or per-frame log spam are added. This developer-only option is not required to install or play.

## Remaining acceptance work

Physical Windows/Linux gameplay, pixel visibility/progress-strip readability and independent two-computer construction remain NOT TESTED in this pass. In particular, the intermittent manual delay should be retested using the owner procedure. The common JAR is Java 17 bytecode and is tested on Java 17 and the stock Windows Steam Java 25; this is not a physical Linux certification.

## Final validation and candidate

Both complete suites passed: **137/137 Java17** and **137/137 stock Steam Java25.0.1**. New construction fixtures cover 59 native and 15 authenticated-handler checks. Previous pause45, priority80/native+21/network, selection54, clipboard117 and liquid9 checks pass. Some exploratory runs failed to compile a test import/atlas stub or used stale v1 malformed-payload assumptions; those fixtures were corrected, their logs retained, and both final full suites pass.

Final native fixture CPU samples (single run, not a performance benchmark):

| Runtime | Synthetic queue admission Z1 cold / Z2 / Z5 | Commit → CPU render preparation Z1 cold / Z2 / Z5 | Client replica application CPU samples |
| --- | --- | --- | --- |
| Java17 | 80.630 / 0.355 / 0.221 ms | 111.5293 / 0.2259 / 0.1455 ms | 1.7532 / 0.3800 ms |
| Steam25 | 23.903 / 0.280 / 0.259 ms | 5.8533 / 0.2005 / 0.1601 ms | 0.2965 / 0.3660 ms |

The CPU render test deliberately uses an empty mock atlas; it validates next-sync visibility/chunk invalidation and ghost state, not actual textured mesh rendering or GPU presentation. The substantial first-call setup overhead is reported rather than discarded. The unchanged standalone baseline probe on Steam25 also reproduced 36→1 sandbox updates and blocked→ready behavior against the exact old/new JARs. The manual sandbox command remained synchronous in both.

Candidate: `local/a2-09c/test-kit/mindustry-fpv-a2-09c-diagnostics.jar` (common Windows/Linux artifact, no physical Linux acceptance).

SHA-256: `6304e75203a291cf79af7455e696c9dda32a14df56f0ae3054a93ea6c618b92f`.

Payload identity: `payload1-fcb04fd9c5e04a67205b9539b4936e594e283bd789d096610a59911a795c118b`.

Artifact audit: 983 existing classes remain byte-identical to A2-09b; changes are restricted to the construction queue/runtime/priority/private-view codec, two construction handler call sites, progress rendering and bounded timings. World save format5, protocol4 and all existing notice/license bytes are preserved. Both Windows/Linux JNA resources remain packaged. All 2065 previous Alpha snapshot files and 30 previous diagnostic release assets were hash-verified unchanged. Corresponding source and checksums are packaged from the local Git checkpoint without developer binaries, saves or profiles.
