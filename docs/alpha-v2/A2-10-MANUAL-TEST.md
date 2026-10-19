# A2-10 — Owner manual check

Candidate: `D:\Linux\Mindustry3-Public\local\a2-10\test-kit-final\mindustry-fpv-a2-10-diagnostics.jar`.
Import the final candidate through Mods → Import Mod and restart. Use a disposable map/save copy. Keep the existing published Alpha artifacts. All multiplayer participants must use the identical final JAR.

1. **Compare materials.** On a sandbox/editor test map, put a natural stone/snow rock mass beside copper/titanium defensive walls and native industrial `dark-metal` / `metal-wall-1/2/3` / `colored-wall`. Rock should form organic mountains; industrial and defensive walls should remain ordinary blocks. Check from above, from the side and far away: no enlarged/missing industrial blocks at coarse distance, no terrain surface across a building's footprint. Trees/bushes/boulders should keep their existing models.
2. **Interact near a cliff.** Add/remove a few artificial walls; build a supported upper platform/factory near the rock face. Check Tab placement, F3 progress/pause, priority selection and F4 multi-floor copy/paste. Solid industrial walls must occlude selection of a blueprint behind/below them. Fly/walk/fire near the wall edge: collision should match the visible block/cliff instead of an invisible former metal mountain. This is a verification of geometry, not new traversal or digging.
3. **Reload/transition.** Save the disposable map or campaign sector, reload it and enter another sector. Natural shapes/materials and completed construction should remain correct. If two computers are available, compare the same map with matching JARs. Report visual problems with material, camera distance and map/floor.

Normal players need no new files, settings, commands or Java runtime. Explicit map-author metadata is optional and documented in `A2-10-TERRAIN-REPORT.md`; no new editor interface or negative-Z terrain is added. Physical Windows/Linux/multiplayer acceptance remains pending.
