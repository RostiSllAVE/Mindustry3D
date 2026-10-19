# A2-09b — Test matrix

PASS means automated validation with real Mindustry 160.7 content and mocked host
input/GL/transport. It is not physical gameplay acceptance.

| Scenario | Evidence / status |
|---|---|
| Z0 copy and resource-consuming paste | PASS; actual sorter → native BuildPlan → partial ConstructBuild → completion, preserved item filter and exact recipe |
| Three-floor factory | PASS; 17 entries: four native foundations, four upper walls, eight slabs, 2×2 machine; real live solver supports completed destination |
| Pause and priority | PASS; F3 stops native progress/upper job, retains work; priority selects ready upper job without losing others |
| XYZ build range | PASS; no native volume progress at excessive altitude or XY distance, resumes on return |
| Base Z adjustment | PASS; real bridge Z1/Z3 → Z2/Z4, UI wheel changes all destination floors |
| 90 / 180 / 270 degree paste | PASS; actual even-size container/conveyor destinations, directions and floor offsets verified |
| Partial footprint selection | PASS; 2×2 building selected via outer cell retained as whole block outside original rectangle |
| Normal costs, no live state copy | PASS; all factory recipes charged once; source 77 stored items and damaged health absent from copy |
| Platforms / incremental support | PASS; dry-run future roots and actual support-first ordering; unsupported destination rejected atomically |
| Internal links | PASS; rotated same-floor bridge Point2, XYZ item bridge, same/cross-floor power, new destination IDs only; original links unchanged |
| Liquid connector and obstructed shaft | Supplemental PASS against exact final JAR; invalid shaft rejects without world/queue mutation; cleared layout reconnects at shifted floors |
| Missing resources / resupply | PASS; unpaid queue waits; no free completion; ordinary recipe charged on resume |
| Occupancy / duplicate pending batch | PASS; whole admission rejection, no duplicate job/resource spend |
| Empty / oversized / malformed clipboard | PASS; empty, >1,024 entries, oversized bytes, truncated/trailing/version/endpoint/overlap cases rejected |
| Unauthorized source / host Free policy | PASS; enemy capture denied; client Free flag cannot override host |
| Server action filter | PASS; denied second cell rejects entire job; no partial native/upper mutation |
| Authenticated actions / replay / rate limit | PASS; actual handlers, owner-only queue, replay rejection, two volume requests/second, oversized requests and dead builder rejected |
| Ground metadata / client priority | PASS; owner-private Z0 view accepted; server repairs missing native plan; client sends priority without fabricated local native plans |
| Preview controls | PASS; copy/button path, real ray resolver, wheel vs UI, rotation, one click/hold, stale-ray miss, RMB, Ctrl+Shift+V, Tab/F5, reset |
| Cancellation with range-parked plans | PASS; only batch's native plans removed, no resurrection; unrelated plan identity preserved |
| Native Ctrl+F / F3 / F4 / placement / campaign / networking regressions | Full existing regression programs; exact counts in VALIDATION.json |
| Java 17 / Steam Java 25 | Final complete current test set, including supplemental exact-JAR fixture; counts/hashes in VALIDATION.json |
| Artifact and source correspondence | Verified compiled classes, source inputs, JAR/folder identity, licenses/resources, source export and checksums |
| GPU ghost appearance, real keyboard/wheel/focus | NOT TESTED physically; owner procedure required |
| Linux/X11/Wayland and two computers | NOT TESTED physically; shared Java/host APIs and native payload preserved, no acceptance claim |

The main build runs 134 programs. One additional liquid/shaft test source was
added after the production freeze and compiled/run against the unchanged exact
JAR on both runtimes: the current repository has 135 tested programs in total.
Production source hashes remain identical to the verified build. This distinction
is recorded rather than claiming the original single build invocation ran 135.

The 256-entry preview microbenchmark measures complete CPU dry-run work and
encoded bytes in a native fixture, not GPU cost, real pointer responsiveness or
FPS. Source/native world indexing, GL, input, pathfinder notifications and transport
are mocked as described by each fixture. No primary player saves are used.
