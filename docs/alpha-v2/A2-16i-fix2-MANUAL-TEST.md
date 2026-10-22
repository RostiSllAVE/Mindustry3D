# A2-16i-fix2 — Owner check

Import `mindustry-fpv-a2-16i-fix2-diagnostics.jar` through **Mods → Import Mod**, replace the preceding diagnostic copy, and restart Mindustry 160.7. Enable one copy of the mod. The JAR in `local/a2-16i-fix2/test-kit/` is the playable artifact; the corresponding-source ZIP is source code. Fullscreen remains recommended on Windows.

1. Put **Wave and Tsunami** near each other, fill them with water and then another supported liquid such as slag. Inspect the coloured panels from above, from a low FPV angle and while rotating/firing. Check empty tanks too: there should be no striped or flickering patches, and native liquid colours should remain visible.
2. Fire **Cyclone**, turn it and aim upward/downward. Inspect the three sliding barrel layers at close range. Compare idle and recoil, then briefly check **Salvo**, **Duo**, **Spectre** and **Ripple** for preserved animation and appearance.
3. Repeat a Wave/Tsunami/Cyclone check on **Z2 or Z3**, then save/reload. Switch F5 and Tab and confirm ordinary building and control still work. Report the turret, liquid, floor and view if overlap remains.

No controls changed. Physical Windows/Linux acceptance and actual remote multiplayer remain owner checks, separate from the automated tests. Multiplayer peers must use the same diagnostic payload; strict compatibility checking is retained.
