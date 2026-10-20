# A2-15a — Owner visual acceptance

Use `D:\Linux\Mindustry3-Public\local\a2-15a\test-kit\mindustry-fpv-a2-15a-diagnostics.jar` through normal Mods → Import Mod → restart. Back up your profile first or use a disposable profile. Fullscreen is recommended because of the previously reproduced Windows windowed presentation problem. This is an unreleased diagnostic, not a replacement of public Alpha assets. Use the exact same JAR on all peers; the source/metadata version alone is not the strict multiplayer identity.

The same JAR is intended for current Windows/Linux installations. Linux physical acceptance and independent multiplayer remain pending. No developer JDK or commands are required to play.

## Controls

- F5: existing2D/3D switch. Tab: existing Build Mode. F3: existing construction pause/resume. F7 and Alt+wheel: unchanged camera perspective/distance controls.
- Settings → FPV flight → **Automatic biome horizon (OFF: legacy mountains)**. Default ON; OFF is the old mountain-ring comparison. Return ON to test the new background.
- No new keys or manual biome presets. The background does not accept building, selection, mining or collision outside the native map.

## Three useful tests

1. **Coast and corners.** Open an ocean-edge map and a mixed map. In FPV look along each edge and rotate360°. Inspect corners at low altitude, then from high above (up to720 tiles if convenient). Compare Auto OFF/ON. Water should continue, grey mountains should disappear at an ocean edge, and there should be no white animated gaps or floating strips. Note any abrupt colour change or visible map rectangle, especially high overhead: distant texture/LOD differences are a known remaining limitation. Also check night if convenient.
2. **Biome identity and normal play.** Compare forest, sand, snow and geological mountain edges. A border built from player walls should not turn into a mountain belt. Sparse desert plants should stay desert; forest should have simple distant silhouettes. Turn/move/fire, use F7/zoom, F5, Tab, ordinary placement, Alt-click blueprint priority, F3 pause/resume, and3D selection/copy/paste. Background scenery must not steal picking or change physical boundaries. Compare OFF/ON if frame cost or artwork is objectionable.
3. **Lifecycle.** Save/reload a mixed map, switch to a different map/campaign sector, and repeat OFF/ON. Surroundings should update and remain consistent after reloading the same map. Watch for persistent old scenery, a rebuild every frame, OpenGL errors or growing memory. On Linux repeat the first two tests and report GPU/session details; if a second computer is available, join with the identical JAR and compare the same edges. Those are physical tests, not yet accepted by automation.

Please report map/planet, edge, approximate altitude/time, ON/OFF difference and a screenshot for any issue. Primary saves were not used by the automated lab. The background is inferred decorative terrain, not a playable neighbouring sector; no new terrain tiles or save format were introduced.
