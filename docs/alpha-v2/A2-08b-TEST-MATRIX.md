# A2-08b — Test matrix

PASS means automated fixture validation, not physical gameplay acceptance.

| Scenario | Status / evidence |
|---|---|
| Z0 C-first selection, A/B retained | Native builder completes C before partial A advances; original plan identities/order and exact three recipes checked |
| A2-08a pause and resume | Selection while paused does not resume; retained partial progress, subsequent resume and existing A2-08a regression fixture |
| Physical unfinished Z0 blueprint absent from own queue | Actual ConstructBuild requeued through native add-build path; injected native surface-pick input, not GPU picking |
| Z2 priority with a tool and unrelated locked floor | Actual controller Alt-click/repeated tap/hold/release lifecycle; selected tool and remaining upper entries retained |
| Cross-floor scheduling | Upper focus holds native Z0 work only during entity update; user flag restored every frame; both queues resume later |
| Parked work and resources | Previously worked upper A completes sooner by its retained work; mixed native/upper exact recipes charged once |
| Nearest layer, fully/partly occluded, multi-tile | Actual shared ray/bounds and upper opaque geometry fixtures; exposed area selectable, covered part rejected, Z4 selected above Z0 |
| Fog-hidden identity | Anonymous opaque-depth boundary preserved; no lower selection through that depth |
| Invalid identity, nonexistent plan, XYZ range, missing resources, Free permission | Rejected without queue loss or free structures |
| Native queue override and explicit clear | Later native queue ordering wins; empty queues do not resurrect prioritized work |
| Alt-click miss and ordinary tool preservation | Gesture consumed without unrelated placement or tool deletion |
| Owner-private reliable state | Actual mod/native in-memory handlers: A/B receive separate views, changed order delivered, unchanged view not resent, initial replay supported |
| Priority network requests | Authenticated owner accepted; replay rejected; forged other-unit selection rejected; other owner's plans unaffected |
| A2-02 readiness/world transition | New channel completes readiness; native layered-world updates retain the read-only view; world reset clears it |
| Malformed metadata | Version/count/size/trailing bytes rejected atomically without replacing the accepted view |
| Full regression programs, Java 17 and Steam Java 25 | Final counts and hashes in VALIDATION.json |
| Physical FPV, visual outlines/ghosts, menus/F5/Tab/OS mouse | NOT TESTED; owner procedure required |
| Two computers / real sockets / physical Linux | NOT TESTED; in-memory transport does not certify remote gameplay |

Native fixtures use real Mindustry content, build progress, resource deductions and
the existing upper placement validators. Input, GL, the native surface-pick input,
pathfinding tile notification and transport are mocked/injected. No independent
construction queue, new save format, combat, unit physics or camera changes.
