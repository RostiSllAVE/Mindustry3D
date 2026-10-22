# A2-16i follow-up — Test matrix

| Test | Result | Evidence / limits |
| --- | --- | --- |
| Installed native types, sizes, heights | PASS automated | Four requested kinds /five native entries; unchanged authoritative bounds |
| Geometry/atlas/LOD/cache | PASS automated | All rotations, Z0/Z1/Z3/Z4/Z5, finite XYZ, bounded UVs and cache reuse |
| Sphere topology | PASS automated | Thirty hexagonal/twelve pentagonal cells, bounded cached spherical triangles |
| Force cap separation | PASS source +scripted GL | Explicit separation from formerly coplanar roof; owner motion/visual acceptance pending |
| Actual launch / landing | PASS native +scripted GL | Real native cargo removal and receipt, direction clocks, removed/repositioned payload rejection, no inventory-only false beam |
| Upper construction / copy / restore | PASS automated | Forty sources +forty queued copies, empty copied stores, existing scheduler and save/replication |
| Native resource/state immutability during rendering | PASS scripted | Inventory, native clocks, shield state and payload state hash unchanged by drawing |
| Java17 / stock Steam25 regressions | PASS |190 programs per runtime against final packaged JAR |
| Actual Windows SDL/OpenGL | PASS scripted |40 scenes /5,120 frames per final runtime +baseline; zero GL errors |
| Transfer graphics state | PASS scripted |40 direct probes per runtime; depth/write/blend/program state restored |
| Shield active / hit / depleted / disabled / recovery | PASS scripted | Real authoritative hit, actual finite field retained, activity indicator correctly absent |
| Disk save/reload / preview | PASS scripted | Disposable SaveIO map, existing ghost/clipboard/unfinished build paths |
| Dense field / stacked / effects cost | MEASURED |100/1,024 mixed instances and active effects; no sustained gameplay FPS claim |
| Source / fingerprint / existing artifacts | PASS | Independent recompilation, strict JAR/folder identity; previous releases/diagnostics unchanged |
| Owner Windows visual and input acceptance | NOT TESTED | Three manual steps supplied |
| Physical Linux / X11 / Wayland | NOT TESTED | Portable code +Java compatibility checks, no Linux hardware claim |
| Real two-computer effects / campaign | NOT TESTED | Existing protocols/accounting unchanged; ambiguous ground payloads omitted conservatively |
