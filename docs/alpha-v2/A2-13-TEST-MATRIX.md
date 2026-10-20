# A2-13 — Test matrix

Baseline: accepted A2-12 `0fa3d5d`. Results below distinguish automated/native fixtures, scripted actual graphics and physical owner acceptance.

| Verification | Result | Actual boundary |
| --- | --- | --- |
| Complete Java17 build/regressions | PASS,142 programs | Windows release17 compilation, assertions enabled |
| Final JAR on stock Steam Java25.0.1 | PASS,142 programs | No java.logging; runner native-access permission |
| Liquid levels, shared edges, phases and native identities | PASS,13,746 checks | Actual native floors and CPU mesh vertices; all11 explicit liquid identities, custom static fallback |
| Shallow/deep/tainted/sand water | PASS native + actual graphics | Same surface elevation, original material colours/art; physical acceptance separate |
| Tar/oil, slag, cryofluid, arkycite | PASS native + actual graphics | Native palette/texture, material-specific motion; no new blue texture |
| Ocean/river/mixed shores | MEASURED + automatic PASS | Identical CPU/native mesh probe,128² maps; not FPS |
| Isolated pools/land/structures/chunk joins | PASS native tests | Anchored corners, bounded waves, quiet chunk edges, deterministic height sampling |
| Mixed liquid coarse cell with artificial wall | PASS regression + actual game repeat | Correct linear tile-array index; first prototype failed and is excluded from final kit |
| Natural cliff and artificial wall shorelines | PASS scripted graphics | Production renderer/native textures, no OpenGL error |
| Z2 platforms above water | PASS scripted actual game | Five authorized free-mode platforms in disposable lab only; normal construction code unchanged |
| Ten native naval profiles | PASS automatic | Afloat offset, lifted/airborne exclusions; original profile/model/stats untouched |
| Risso/Minke/Omura waterlines | PASS scripted graphics | Low/overhead still views; no physical sailing/control acceptance |
| Wave phase and waves-OFF presentation | PASS native + actual captures | Fixed-time native samples, shader uniforms, unedited images; source geometry is not rebuilt for animation |
| Campaign generation/save/reload/transitions | PASS,44 native checks | Actual disposable SaveIO, fixed-time visual liquid hash, native tile/overlay/resource/upper-layer state |
| A2-11 relief/A2-12 ore | PASS,6,064/301 native checks | Production relief/ore modules byte-identical; water overlays follow lowered fan |
| Construction/F3/priority/3D selection/copy-paste | PASS existing regressions | Native/in-memory authority fixtures; no physical clicks asserted |
| Combat, physical movement, camera/mouse, protocol4 | PASS existing regressions + unchanged classes | No gameplay changes; not new two-computer acceptance |
| Actual stock Steam25 SDL/OpenGL | PASS before/after,GL error0 | Two140-frame runs, identical lab entry, original renderer/native atlas; screenshots and asynchronous timing samples |
| Final JAR/folder payload identity | PASS Java17/Steam25 | Strict compatibility still applies; no network/protocol changes |
| Source/binary, resources, licenses and old artifacts | PASS | Fresh runtime recompile, hashes, preserved checkpoints/releases |
| Physical Windows controls and owner-map appearance/FPS | NOT TESTED | Owner follows manual procedure |
| Physical Linux graphics/input/weak laptop performance | NOT TESTED | Same intended cross-platform JAR, no new native dependency |
| Independent multiplayer visual comparison | NOT TESTED | Static geometry deterministic; local cosmetic clock phases are not synchronized |

Actual graphics automation uses an isolated profile and a lab-only smoke entry replacement; it is not physical gameplay acceptance. The packaged JAR retains the normal smoke entry. Existing saves and profiles were not used or changed.
