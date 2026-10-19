# A2-11 — Test matrix

Baseline: A2-10 `744c52c`, branch `main`. PASS below means the stated automated scope only.

| Check | Java 17 | Stock Steam Java 25 | Evidence / scope |
| --- | --- | --- | --- |
| Full diagnostic build/regressions | PASS, 140 programs | PASS, 140 programs against final JAR | Local verified build + per-program runtime logs |
| Deterministic bounded natural relief | PASS | PASS | Independent samplers, unchanged native tile hash, 5,595-check floor fixture |
| Tile, chunk, detail/coarse and fog seams | PASS | PASS | Shared exact corner values; flat chunk apron/fog boundary; actual coarse CPU vertices |
| Natural stone/sand/ice material amplitudes | PASS | PASS | Native content identities and bounded scaling |
| Artificial, authored, core zone, vents and liquids remain flat | PASS | PASS | Explicit cases plus every native liquid |
| Mountains, artificial walls and decoration bases | PASS | PASS | Anchored footprints; physical terrain/classifier unchanged; A2-10 regressions |
| Native floor UV orientation and ore overlays | PASS | PASS | Real atlas-builder CPU vertices, north/south inset UVs, matching triangles/offset |
| Multi-tile building foundations | PASS | PASS | Actual graphite-press footprint cells flatten, unchanged native placement rules |
| Adjacent chunk edit invalidation | PASS | PASS | Existing renderer invalidation marks neighbour locally; no global mountain rebuild |
| Save/reload and campaign transitions | PASS, 43 checks | PASS, 43 checks | Real native sector generators and full disk SaveIO in disposable profile; exact visual-sample hash after reload |
| F3 construction pause | PASS, 45 checks | PASS, 45 checks | Existing native fixture |
| A2-08b priority / Alt-click | PASS, 80 native +21 network checks | PASS, same | Existing native pointer/authority fixture |
| 3D volume selection | PASS, 54 checks | PASS, 54 checks | Existing XYZ selection fixture |
| 3D copy/paste and liquid supplement | PASS,117 native +13 network +9 liquid checks | PASS, same | Existing server-authoritative/native fixtures |
| Upper construction timing/costs | PASS,59 native +15 network checks | PASS, same | Existing sandbox/survival scheduler fixtures |
| Mesh generation / bytes / material batches | PASS, measured before/after | PASS, measured before/after | Identical 64-chunk probe, MockGL, actual native buffers; no GPU claim |
| Allocation counter | PASS, unchanged per rebuild | NOT AVAILABLE | Bundled Steam runtime lacks optional management API; not replaced with a guess |
| Packaged code scope / notices / earlier artifact preservation | PASS | PASS | Artifact byte comparison, class major ≤61, platform native resources retained |
| Actual candidate JAR/folder compatibility identity | PASS | PASS | Native ModFingerprint over actual candidate containers |
| Physical low-angle visuals, ores, shores, buildings and platforms | NOT TESTED | NOT TESTED | Owner acceptance required |
| Physical F5/Tab/mouse/FPV flight/menus | NOT TESTED | NOT TESTED | Input/camera classes unchanged; automated regressions are not physical acceptance |
| Real GPU frame time, hardware draw calls and gameplay FPS | NOT TESTED | NOT TESTED | CPU fixture results are reported separately |
| Physical Linux X11/Wayland visuals/input | NOT TESTED | NOT TESTED | Same portable JAR; no Linux machine test performed |
| Real two-computer multiplayer visual consistency | NOT TESTED | NOT TESTED | Deterministic CPU generation only; no packets/protocol changes |

Primary saves were not accessed. No Alpha/Workshop files were overwritten. Fullscreen remains recommended because of the previously confirmed windowed-presentation performance issue.
