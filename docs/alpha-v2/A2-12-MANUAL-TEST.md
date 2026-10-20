# A2-12 — Owner visual checks

This is an unreleased diagnostic. Keep the previous A2-11 visibility JAR and a backup/disposable save. Use Mindustry160.7 and **Fullscreen**; the known windowed presentation problem can distort comparisons. No separate Java installation or developer tools are needed to import the JAR.

1. **Import and compare appearance.** Mods → Import Mod → select `local/a2-12/test-kit/mindustry-fpv-a2-12-diagnostics.jar`, confirm replacement and restart. Open a disposable sandbox/campaign. Press F5, look along open stone/sand/ice ground from a low angle and then from above. Expect deeper geometry but softer slope shading, not darker shadows. Inspect copper, lead, coal, titanium, thorium and scrap, then beryllium/tungsten/crystal-thorium if available. Near ore should have low textured roofs and visible sides, recognizable from above; distant coarse ore remains flat. The kit's `gpu-evidence/after-low.png` and `after-top.png` show the scripted reference gallery.
2. **Check integration and ordinary play.** Inspect isolated and dense ore at chunk boundaries, mountain edges, shorelines and next to factories/conveyors/artificial walls. Ore must not float or protrude through placed buildings. Mine copper and another eligible ore with the controlled unit, start/stop/change target, and watch an AI miner and an ordinary drill. Confirm real resources accrue normally. Use Tab to place/remove Z0 and elevated blocks, F3 pause/resume, Alt-click blueprint priority, and existing 3D selection/copy/paste. Save, reload and change campaign sector, checking the same field remains consistent.
3. **Compare performance and report exceptions.** Use the same map/settings/camera before/after, particularly dense ore fields and low/overhead views. Report FPS/stutter, visible seams, flicker, excessive fragments, feet/camera intersections or slow chunk appearance. On Linux repeat F5/Tab and these visual checks, naming X11/Wayland and GPU if possible. Physical Linux and independent multiplayer checks have not been performed by the automated runner.

No new keys or mining controls are introduced. Mining eligibility, rates, resources, collision, hitboxes, construction support and save/network formats are unchanged. Rocks and relief are visual only: they do not add a mining target or physical height. Unknown/modded ore keeps its native overlay; wall ores are outside this floor-deposit pass.

The candidate keeps the existing metadata version; identify it by filename and SHA-256:

`d81b0ce2f16e7a145c1da5d06efc19e266a115589d1e0949bbfc45203a73b3fb`

Do not upload this diagnostic or update Workshop automatically. Physical visual acceptance remains the owner's decision.
