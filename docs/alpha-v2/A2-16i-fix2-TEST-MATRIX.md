# A2-16i-fix2 — Test matrix

All automated tests used disposable test data on Windows, with Mindustry 160.7 host classes. Steam Java 25 is the stock bundled 25.0.1 runtime; the complete Java runtime is 17.0.20.1.

| Check | Java 17 | Steam Java 25 | Evidence / limit |
|---|---|---|---|
| Full regression suite | PASS: 193 programs | PASS: 193 programs | Construction, priority/cancellation, selection/clipboard, campaign, combat, input and networking regressions included; automated fixtures |
| New native layer separation test | PASS | PASS | 5,189 checks; 480 yaw/pitch/recoil/floor poses, native liquid/top/heat order and Cyclone barrel non-intersection |
| Prior thin-tip/contour and culling regression | PASS | PASS | Existing TurretOutlineCoverageTest retained |
| Real native atlas / OpenGL fixture | PASS: 1,087 checks | PASS: 1,087 checks | 1,080 main pose frames plus cache-reuse draws per runtime; 7 turrets; empty/water/slag/cryofluid/oil; oblique/side/top, -35/0/+55 pitch, 4 yaws/recoil states; zero GL errors |
| Production world renderer | PASS: 11 scenes | PASS: 11 scenes | 1,408 frames per runtime, authorized upper-floor placements, actual save/reload, 100/1,024 mixed-turret fields; rendering leaves native state unchanged |
| Original defect | Reproduced from baseline JAR | Reproduced with actual SDL/atlas | New regression intentionally fails against the old JAR; baseline PNGs show mask striping/barrel overlap |
| Fresh source/binary correspondence | PASS | Same shared JAR | 1,035 freshly compiled runtime classes byte-identical; resources match |
| Actual multiplayer fingerprint JAR/folder identity | PASS | PASS | Same payload hash; strict validation code unchanged |
| Previous releases/diagnostic assets | PASS | PASS | 2,179 existing artifact files rehashed |
| Physical FPV, mouse, F5/Tab and firing acceptance | NOT TESTED | NOT TESTED | Owner's three-step manual check remains required |
| Physical Linux / remote multiplayer | NOT TESTED | NOT TESTED | No new native APIs or dependencies; no compatibility certification inferred |

## Geometry observations

Seven native turret types retain 25 cached regions. CPU template storage: 405,504 → 449,856 bytes. The extra roof templates are immutable and reused. Full-square mask regression: 36-vertex extrusion → 6-vertex roof.

| Mixed world scene | Submitted vertices before → after | Draw calls before → after |
|---|---:|---:|
| field100 | 260,423 → 226,901 | 35 → 35 |
| field1024 | 743,939 → 640,007 | 77 → 75 |

These are one-frame whole-world geometry counts at matched scripted poses, including terrain and other passes. Short CPU/GPU timing samples were collected during other automated test work and are not controlled performance comparisons. No FPS improvement, allocation improvement or real multiplayer result is claimed. Sanitized fixture results/screenshots and source are included in the local test kit; raw Steam logs, profiles and saves are excluded.
