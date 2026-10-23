# A2-16k — Test matrix

Automated checks run on Windows with installed Mindustry 160.7 classes. Scripted real rendering is distinct from physical owner gameplay acceptance.

| Check | Java17 | Stock Steam Java25 | Evidence / boundary |
|---|---|---|---|
| Full regression suite | PASS 196 | PASS 196 | Existing Alpha v2 gameplay/network/campaign/input/model tests retained |
| Native factory geometry and states | PASS | PASS | 17,136,473 checks / 1,440 poses each; all 6, rotations, floors, LODs, five operation states |
| Real production/costs/clear exits | PASS | PASS | All 10 plans;136 successful releases,24 expected dry upper naval holds; existing blocked/shore fixtures |
| Z0/Z1/Z3/Z5 models | PASS | PASS | Actual production renderer; eligible upper placement calls via server-authorized fixture APIs |
| Native SDL/OpenGL | PASS 35 scenes, 4480 frames | PASS 35 scenes, 4480 frames | 0 GL errors each; all 6 top/side/oblique, low FPV-like camera and below-plane views |
| Native ground factory output in graphics | PASS6 | PASS6 | Dagger, Flare, Risso, Stell, Elude, Merui created through native updateTile/payload path |
| Operating/inactive/partial progress | PASS | PASS | Native fields control animation; no rendering-time world/resource/entity mutation |
| Previews, multi-floor paste, stacked floors and adjacency | PASS | PASS | Actual existing preview/clipboard/render paths; no new construction permissions |
| Save/reload | PASS | PASS | Actual disposable SaveIO,6 ground and15 upper factories; old serialization regressions also pass |
| Campaign lifecycle | PASS automated | PASS automated | Current physical sector transition NOT TESTED |
| Cache reuse and culling | PASS | PASS | Six stable material entries; no regeneration after warmup; finite transformed envelopes |
| Matched rendering performance | NOT measured as before/after | MEASURED | Serial parent/final samples; CPU/GPU/calls/vertices and limits in report |
| Per-frame allocation counter | Available in gallery only | UNAVAILABLE | No before/after allocation or FPS improvement claim |
| Fresh source → JAR | PASS | Shared artifact | 1047 runtime classes byte-identical; all resource/design inputs match |
| Strict JAR/folder fingerprint | PASS | PASS | Actual Java ModFingerprint; protocol unchanged |
| Physical Windows F5/Tab, FPV, control/camera acceptance | NOT TESTED | NOT TESTED | Owner procedure; no input code changed |
| Physical Linux/X11/Wayland | NOT TESTED | NOT TESTED | Shared platform-neutral build; native Linux execution not available here |
| Two-computer multiplayer / remote upper animation | NOT TESTED | NOT TESTED | Existing snapshot clock limitation documented; no protocol change |

The early save-fixture count failure was corrected by enumerating sleeping native buildings through world tiles, rather than changing game code. Upper naval holds are expected retained restrictions, not failed visual models. No primary saves or published artifacts were changed.
