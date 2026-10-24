# A2-16l logic — Test matrix

PASS applies only to the named fixture. Both graphics runtimes were tested on Windows.

| Check | Java 17 | Stock Steam Java 25 | Scope |
|---|---|---|---|
| Full automated regression suite | PASS 201 | PASS 201 | No failures |
| Nine models, native bounds/contact/UV/LOD/read-only state | PASS | PASS | 773,413 checks / 216 poses |
| Native instructions, memory, switch, message and drawing queues | PASS | PASS | 99 checks; original coolant and same-floor rules |
| Real display pixels, RGB, resolution, orientation, clear and independence | PASS actual GL | PASS actual GL | Native processor-generated content, actual shader and meshes |
| Screen geometry capacity, unchanged FBO reuse, correct ownership/cleanup | PASS actual GL | PASS actual GL | 35 combined GPU checks per runtime |
| Legacy Canvas/live-display integration | PASS actual GL | PASS actual GL | Seven previous GPU checks per runtime |
| All modeled blocks; low/top/oblique; Z0/Z1/Z3/Z5; stacked/below | PASS scripted | PASS scripted | 31 scenes / 3,968 frames, no GL errors |
| Actual disposable SaveIO reload and program redraw | PASS scripted | PASS scripted | Native framebuffer pixels themselves are not saved by vanilla |
| 3D copy/paste model geometry and normal post-paste configuration | PASS scripted | PASS scripted | Actual authorized paste and native redraw |
| Automatic 3D-clipboard processor code/link retention | NOT SUPPORTED | NOT SUPPORTED | Existing explicit safeConfig omission/warning retained |
| Preview, adjacent models, uneven terrain, valid headroom | PASS scripted | PASS scripted | Original construction/physics rules |
| Multiplayer, campaign, input/control regression suites | PASS automated | PASS automated | Does not certify physical gameplay/networking |
| Independent source/binary correspondence; JAR/folder identity | PASS | PASS same JAR | Canonical payload and all 1,060 freshly compiled classes |
| Matched 72-display renderer cost | — | MEASURED | Before/after raw blocks and limitations in report/JSON |
| Stock-runtime allocation profiling | — | UNAVAILABLE | -1 means unavailable, not zero allocations |
| Physical Windows FPV/F5/Tab/menu and import/restart | NOT TESTED | NOT TESTED | Owner manual acceptance required |
| Physical Linux startup/mouse/gameplay | NOT TESTED | NOT TESTED | Portable Java path and previous platform tests only |
| Physical campaign transitions / real two-computer session | NOT TESTED | NOT TESTED | No acceptance claim |

Intermediate fixtures exposed and corrected a coolant-starved test processor, invalid large-screen headroom, and the pre-existing clipboard omission policy; no gameplay workaround was shipped. Actual GPU testing also identified the new screen's overlapping roof and imprecise exact-float marker, corrected before final validation. Intermediate outputs remain local and are not the delivered artifact.

Rebuild: select `--task a2-16l-logic` in `tools/build-diagnostics.py`, with a complete Java 17 JDK and installed Mindustry 160.7 host supplied through the existing environment options. All helper sources, sanitized results and comparison captures are in the local kit; no external development profile is required to play the JAR.
