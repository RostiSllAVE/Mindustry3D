# A2-16o — owner acceptance (standalone first)

Use the new diagnostic JAR in an isolated profile. No installed Alpha, Workshop folder or normal save needs replacement. From this repository with Python 3 available:

```powershell
python tools/launch-standalone.py --run a216o-owner-01 --interactive
```

Use a new run name each time; an existing profile is never overwritten. The launcher uses `local/latest-diagnostic.json`, the clean standalone 160.7 installation and its bundled Java 25. Steam is not required. A complete JDK is needed only for developer fixtures/compilation, not this ordinary interactive launch.

1. **Production and payloads.** On a disposable sandbox map, produce Dagger and Flare; send each through a payload conveyor and payload router to an open exit. Repeat on Z1 and Z5 platforms. Block an exit with a wall, then remove it. The payload should wait, show a warning, and release once space is valid. Test a naval factory facing water; a boat must remain blocked facing dry land. Try an occupied exit at the same XYZ and a unit on a different floor. Test one reconstructor chain. Do not expect a dry upper platform to become navigable water.
2. **Utility models and behavior.** Inspect all 14 blocks listed in the report from above and low FPV. Change Item Source/Liquid Source configuration; test their matching voids, power source/void and payload source/void. Damage a friendly unit and check both repair blocks heal it; remove the target/disable power and check activity stops. Shoot the actual Target Dummy target and verify its counters. Join two tiled displays, draw changing left/right colors through a processor, then split and rejoin them. Repeat representative cases on platforms; save/reload the disposable map.
3. **Controls and distances.** Use F5, Tab, menus and Alt+Tab, then resume flight and continuous mouse rotation. In FPV settings change **3D world draw distance** (64–16,384 tiles; default 8,192) and **3D high-detail distance** (20–240 tiles; default 80) independently. Look across a long map and at detailed machinery. Reopen the game to confirm preferences persist. World distance is a rendering ceiling; higher values cannot extend beyond the existing map/background reach. Terrain and existing block-family LOD thresholds scale with detail distance; unit families without a distance LOD retain their current models.

Automated API/OpenGL tests are not physical keyboard, mouse or two-computer acceptance. Linux physical capture and a real multiplayer factory/payload session remain owner checks. Existing F3 construction pause and Alt-click priority should still work; neither setting changes placement/picking distance, authoritative costs, simulation or saves.
