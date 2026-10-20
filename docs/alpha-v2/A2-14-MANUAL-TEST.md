# A2-14 — Owner visual test

Keep A2-13b and a backup/disposable save. Import `local/a2-14/test-kit/mindustry-fpv-a2-14-diagnostics.jar` through Mods → Import Mod and restart. Use Fullscreen. No controls were changed or added.

1. **Biomes and density:** Open a tree-heavy map. Compare a dense pine forest with isolated trees, snow/white trees, spore flora, shrubs, redweed, yellow coral and red-diamond flora where available. Use F5/F7 for top, side and low FPV views. Look for stable height/yaw/shape differences, readable trunks/crowns and a forest that remains dense. Fly around/through valid gaps: native collision has not moved with the visual nudge.
2. **Obstacles and persistence:** Inspect trees beside cliffs, water, artificial walls and Z2 platforms. Crowns should intersect less; low ceilings may make plants noticeably short. Build/remove a nearby wall/platform and check the local vegetation updates. Save, exit/reload and revisit the same positions: variants should stay stable. Try a campaign sector transition if convenient.
3. **Regression/performance:** Check normal flight/mouse capture, Tab placement, Alt-click priority, F3 stop/resume, Z0/Z2 construction, 3D selection/copy, mining, water waves and diving. Compare the same dense map/settings FPS with A2-13b. Repeat on Linux if available. For multiplayer, use the identical JAR on every participant/server and compare a few chosen plants; independent multiplayer testing is still pending.

Report floating roots, unexpected thinning, extreme sizes, clipping near roofs/cliffs, biome colours/shapes that feel wrong, or additional stutter. Some canopy intersection, simplified far LOD and mismatch between enlarged foliage and original native collision are expected limitations. No terrain, resource, physics, water or save-format changes are intended.

Automated checks/scripted captures are separate from physical gameplay acceptance. Do not publish this diagnostic or update Workshop automatically.
