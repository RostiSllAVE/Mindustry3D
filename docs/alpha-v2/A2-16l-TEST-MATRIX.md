# A2-16l — Test matrix

All results refer to the final-source diagnostic JAR recorded in `A2-16l-VALIDATION.json`. PASS describes the named fixture only.

| Check | Java 17 | Stock Steam Java 25 | Evidence / scope |
|---|---|---|---|
| Full automated regression suite | PASS 199 | PASS 199 | All test programs, zero failures |
| Six factory models, state, XYZ, UV, caches | PASS | PASS | 17,095,463 checks; 1,440 poses |
| Eight upgrade models, all rotations, Z0/1/3/5, three LODs | PASS | PASS | 31,931,505 checks; 1,920 poses |
| Mounted cyan tools and arm/static clearance throughout stroke | PASS | PASS | 57 checks, 476 swept poses; actual triangle intersections |
| Active vs disabled/starved/empty/in-transit animation | PASS | PASS | Native state gates; no render-time world/inventory mutations |
| All 34 native upgrade pairs, costs, output/canonical XYZ | PASS | PASS | 448 real releases; 96 expected dry upper naval holds; 4,864 assertions |
| New models in actual SDL/OpenGL host | PASS | PASS | 44 scenes / 5,632 frames each; zero GL errors |
| Top/side/oblique/low-angle views and raised/stacked/below views | PASS scripted | PASS scripted | Actual renderer captures; physical visual acceptance pending |
| Old/new factory clipping and lamp comparison | — | PASS scripted | Same three cameras/native phases; 384 frames each build |
| Native completion in graphics host | PASS | PASS | One actual ground completion for each of eight new types |
| Save/reload, upper placement, clipboard, preview | PASS scripted | PASS scripted | Disposable SaveIO; 1,096 authorized upper placement calls per gallery |
| Construction pause/priority and volume clipboard regressions | PASS | PASS | Existing A2-08/09/production suites |
| Campaign lifecycle and layer persistence regressions | PASS automated | PASS automated | Does not exercise physical sector UI transitions |
| Input, capture, F5/Tab, networking, combat regression suites | PASS automated | PASS automated | No physical control or real remote-network claim |
| JAR/folder payload identity | PASS | PASS | Identical strict fingerprint, actual Java implementation |
| Fresh compile corresponds to binary/resources | PASS | PASS same JAR | 1,054 final source runtime classes byte-identical |
| Previous artifacts/notices/metadata preserved | PASS | PASS same JAR | 2,188 protected files; 1,155 prior runtime classes unchanged |
| Matched rendering cost before/after | — | MEASURED | Two warmed 64-frame blocks/scene; CPU/GPU and geometry in report |
| Stock-runtime per-frame allocation comparison | — | UNAVAILABLE | Counter reports -1; not zero allocation |
| Physical Windows gameplay, F5/Tab and import/restart | NOT TESTED | NOT TESTED | Owner procedure follows |
| Physical Linux/X11/Wayland controls and startup | NOT TESTED | NOT TESTED | Windows-run portability/native-input regressions only |
| Physical campaign UI transitions | NOT TESTED | NOT TESTED | Primary saves untouched |
| Two-computer multiplayer / remote upper animation acceptance | NOT TESTED | NOT TESTED | No protocol change; transient-clock limitation documented |

An early culling fixture moved only the model while leaving its real payload in view. Correcting the fixture to move the camera resolved that false failure; no gameplay culling workaround was shipped. Final fixtures and suites all pass. Intermediate prototypes and their outputs remain local and are not the delivered binary.

Reproduction: set `FPV_JDK` to a complete Java 17 JDK and `FPV_MINDUSTRY` to the installed Mindustry 160.7 folder; run `tools/build-diagnostics.py --help` and select task `a2-16l`. The test kit contains the graphics/performance/native-definition helper sources, results and sanitized captures. Raw Steam logs, local game profiles and test saves are deliberately omitted.
