# A2-08a — Validation matrix

PASS below means automated validation only. It does not mean physical gameplay
acceptance. The new fixture uses actual Mindustry 160.7 content, native builder
progress/cost code and the actual `NetServer.clientSnapshot` entry point. Input,
graphics, transport and pathfinding notification are mocked; no GPU, OS keyboard,
real socket or full graphical game loop is exercised.

| Scenario | Result | Evidence / limitation |
|---|---|---|
| A2-07 baseline: native pause blocked in flight | CONFIRMED | Accepted class files; `suppressBinding(pauseBuilding)=true` |
| A2-07 baseline: upper work ignores pause | CONFIRMED | Z2 copper wall completed with `updateBuilding=false`; 6 copper consumed |
| Z0 start → stop before completion | PASS | Actual ConstructBuild identity/progress/resources and committed plan retained through 500 paused native builder ticks |
| Resume same blueprint; another queued block | PASS | Both native builds complete; exact recipe cost, no duplicate charge |
| Stop vs tool cancellation | PASS | Pause retains selected tool/mining/shooting state; gesture cancellation retains committed plans and pause |
| Stop vs explicit request removal | PASS | Native queue clear leaves physical partial blueprint; explicitly requeuing it resumes; upper cancel remains explicit |
| Build/flight transitions and early return | PASS | Tab-like adapter transitions preserve pause, Build native binding toggles once per frame |
| Command mode and world changes | PASS | Actual command-mode early return preserves pause; changing world tile identity resets prior intent |
| E/Q flight input | PASS | Native E pause is ignored in flight; altitude controls unchanged |
| Remapping | PASS | Host KeyBind F3→F4 save/load and panel-label update |
| Dialog, keyboard focus, suspension | PASS | No construction toggle while input is unavailable |
| Unit switching | PASS | New unit reads its own flag; old paused state and other player plans preserved |
| Elevated timed Z2/Z3 jobs | PASS | Pause preserves pending work, no resource cost/accrued-time burst; resume/repossess completes once |
| Other player construction | PASS | Independent upper job continues while owner is paused |
| Native multiplayer building flag | PASS, in-memory | Actual authenticated host snapshot handler transfers stop/resume; stale sequence rejected; duplicate paused state harmless; exact native recipe cost |
| Native 2D | PASS, input boundary | F3 and new tracking do not change vanilla input state |
| Existing regression suites | See VALIDATION.json | Includes layered validation/costs, controls, mining, commands, combat, prediction, connection readiness, saves/campaign and platform APIs |
| JAR/folder code identity and packaging | See VALIDATION.json | Canonical strict fingerprint, scoped binary diff and retained license/platform resources |
| Physical FPV/Build interaction, beam appearance and Controls menu | NOT TESTED | Owner procedure provided; mock input is not OS acceptance |
| Real local/remote multiplayer and packet-delay behavior | NOT TESTED | In-memory host handler only; no claim of two-computer acceptance |
| Linux physical gameplay and mouse capture | NOT TESTED | Same cross-platform bytecode; A2-06 physical Linux check still pending |

The fixture does not implement queue priority or a new upper-blueprint save format.
Already instantaneous upper placements are not made interruptible after completion.
