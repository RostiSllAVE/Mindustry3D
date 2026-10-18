# A2-01 — Recommended follow-up fixes

No fixes below are implemented in this diagnostic task. Preserve server authority
and strict matching-code validation. These are scoped recommendations for the
named follow-ups, not proof of the friend's root cause.

| Priority / task | Evidence and player impact | Smallest justified next change | Risk / acceptance needed |
|---|---|---|---|
| 1 — A2-02: owner responsiveness | Confirmed 100 ms local-pilot display buffer and no prediction. Affects even low-RTT remote control | Separate local-owner presentation from observer interpolation, with a tested bounded owner delay. Treat removal of 100 ms as a mitigation, not a complete network fix. Then evaluate sequenced input ACK + bounded client prediction/reconciliation as its own protocol change | Latest-sample-only camera can expose 10 Hz stepping; prediction must never award damage/building/authority, must reconcile collisions, boost, possession and map epochs. Measure input-to-motion before/after on an actual link |
| 2 — A2-04: reliable pose traffic | Confirmed full reliable 106-byte/unit pose stream, per-recipient scan and high traffic on populated maps | First measure actual host throughput/queue and apply costs with the new counters. A low-risk initial optimization is suppressing identical pose snapshots while preserving bounded keepalives; broader latest-state delivery/interest filtering needs a separate versioned protocol design | Idle suppression will not solve moving-population traffic. Unreliable 12 kB chunks are not a safe drop-in replacement; retain reliable persistent/construction state, atomic bounds, keyframes and strict compatibility. Do not hide units needed by 3D interaction |
| 3 — A2-05: campaign/host failure | Exact failure stage unknown; native chunk/event tests pass, upper data intentionally excluded from native stream | Collect both sides' matching digests, ACK/reset/rejection counts and a disposable affected campaign reproduction. Implement the first demonstrated lifecycle defect only; consider explicit apply-completion/readiness diagnostics if streaming is implicated | No speculative reset reorder or SaveIO migration. Test new sector, backed-up save, custom-map switch, save/reconnect and physical listen-host swap before declaring fixed |
| 4 — bounded reassembly cleanup | Confirmed timeout only evaluated on a later chunk; quiet incomplete channel retains memory | Add explicit bounded expiry with observable stale/expired counters in a focused follow-up | Must not expire valid slow transfers or replay an old revision; current maximums are bounded. This alone is not a proven latency fix |
| 5 — repeated layer/native reconstruction | Confirmed prepared registry created on remote layer snapshots, cost in real campaign unknown | Profile `CHANNEL_APPLY_MS` plus layer count; if dominant, validate/reuse unchanged geometry/state without rebuilding a full temporary registry | Preserve atomic validation, native resources/configuration, payload references and structural identity. Do not trade desync safety for speed without a measured bottleneck |

For compatibility failures, improve the visible explanation of the existing
strict rejection rather than accepting mismatches. Host flight settings should
also be reported consistently; changing whose settings are authoritative is not
an A2-01 correction.

Do not prioritize internet/router changes or a networking rewrite before the
paired vanilla/mod, equal-settings, actual-link tests. The sandbox socket denial
is an investigation-environment limitation and is not a player-facing defect.
