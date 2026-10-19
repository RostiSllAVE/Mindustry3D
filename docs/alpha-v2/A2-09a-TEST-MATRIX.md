# A2-09a — Automated and physical test matrix

PASS below means automated native-fixture validation. It does not certify physical
Windows/Linux input or actual GPU appearance. Final counts/hashes are recorded in
[VALIDATION.json](A2-09a-VALIDATION.json).

| Scenario | Result / evidence |
|---|---|
| One completed Z0 building, native non-updating wall | PASS; real native buildings manually indexed in generation-mode fixture |
| Multi-tile footprint partly/fully inside XY | PASS; native 2x2 and upper multi-tile footprints, exactly one entry each |
| Z1-Z3 blocks plus Z2 platform | PASS; four members, correct rotation, floor and full bounds |
| Shrink / expand / reverse Z | PASS; fixed anchor, lower bound Z0 and configured maximum clamped |
| Tall column anchored below selected floor | PASS; real physical overlap includes it |
| Empty area/floors, unfinished native construction | PASS; empty volume remains valid; ConstructBuild excluded |
| Stable-floor drag and visible surface resolution | PASS; real upper picker integration; later higher hits do not retarget anchor |
| Explicit locked empty floor | PASS; lock supersedes visible floor and allows plane projection |
| Repeated wheel samples / signed steps | PASS for once-per-frame and signed steps; raw host wheel adapter used |
| UI wheel / UI release / held pointer on entry | PASS; no Z change, draft abandoned, accidental gesture fenced |
| RMB clear, Esc leave, new drag, parallel ray | PASS; last completed volume retained on abort, new selection replaces |
| Team/world reset | PASS; private selection cleared |
| Queues, resource costs and F3 state | PASS; native plan identity, upper job count, grid snapshot, copper and pause unchanged |
| Alt-click / Ctrl possession / native schematic/removal/clear | PASS; consumed in selection; no priority request, possession or native edit |
| Flight fire after held-click exit | PASS; fenced until release |
| F4 path, inactive Tab/F5 boundary, focus/menu suspension | PASS; actual controller update boundary; no OS event automation |
| Inactive selection background work | PASS; retained membership does not requery |
| Large volume | PASS; 4,096 indexed structures selected; 10,000 idle calls reuse cache; timings in JSON |
| A2-08a/b, ordinary construction, physics, network regressions | Full existing regression programs; see final counts |
| Java 17 and stock Steam Java 25 | Full final suites; see final counts; Java 25 lacks java.logging |
| Windows/Linux source and package paths | Common host APIs; no OS-specific additions/dependencies; both JNA payloads preserved |
| Actual translucent volume, occlusion, readability, real wheel/F4/capture | NOT TESTED physically; owner procedure required |
| Physical Windows/Linux gameplay and two-computer session | NOT TESTED; no multiplayer acceptance claim |

The fixture uses real Mindustry 160.7 content and spatial indexes, mocked input,
GL, pathfinder tile notifications and test world generation. Native quadtrees are
explicitly populated because generation mode intentionally skips BlockIndexer.
The benchmark measures CPU membership generation/cache, not rendering, GPU time,
OS mouse behavior or FPS. No primary saves or published assets are changed.
