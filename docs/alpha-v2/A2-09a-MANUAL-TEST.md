# A2-09a — Owner test (about 5 minutes)

Install `mindustry-fpv-a2-09a-diagnostics.jar` using Mods → Import Mod → restart.
Use a disposable save/profile or backup. This diagnostic is separate from published
Alpha releases; for multiplayer every participant/server must use this exact JAR.
Keep fullscreen for the previously identified windowed presentation issue.

1. Load a map, F5 → Tab. Leave floor lock off. Press **F4** or click **3D Selection**.
   Drag LMB around one completed Z0 building; release. Check yellow volume,
   translucent faces, cyan included-building contour, dimensions and count.
2. Drag around a structure with Z0/Z1/Z2 or Z1/Z2/Z3 buildings/platforms. Start on
   an upper floor and scroll up/down over the world, including below the anchor.
   Check the anchor stays fixed; scroll after releasing too. Try even-size blocks,
   rotated conveyors, partial footprint intersection and an empty area.
3. Scroll over the left panel: it should scroll the panel only. RMB clears;
   another drag starts a new volume. Esc or F4 leaves without changing buildings.
   Enable a manual floor lock and start over empty space on that floor.
4. Start a build queue, pause with **F3**, then repeat selection. No plan, resource
   or partial building should disappear, and F3 should stay paused. Exit selection
   before testing ordinary click/Alt-click blueprint priority, normal placement,
   configuration, linking, dismantling and Ctrl+F copying. These retain their
   previous controls.
5. While dragging, switch Tab/F5, open a menu or Alt+Tab. The draft should be
   abandoned, cursor behavior restored, and a held click must not become a shot
   or placement. Reenter Build/F4 to reuse the last completed volume. Load another
   world: no previous selection should remain.

Inspect from above/side/oblique angles and behind walls. Contours should use world
depth rather than reveal members through solid geometry. Very large selections
show at most 1,024 visible member outlines but full counts; the enclosing box
always describes the complete selected floor volume.

Please report: starting surface/floor-lock state, chosen range/count, any wrong
member, unwanted command or cursor behavior, and whether the volume is readable.
A Linux run should repeat real wheel, F4, Tab, focus and menu behavior. Automated
Windows JVM fixtures cannot establish physical Linux acceptance.
