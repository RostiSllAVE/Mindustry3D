# A2-16g — Test matrix

Baseline: completed A2-16f `3636d2d0d3fe0a1413848df2e7e1fe93fd502a7f`.
Automated native/render fixtures do not constitute physical gameplay acceptance.

| Test | Status | Evidence / limits |
| --- | --- | --- |
| Fourteen installed native profiles | PASS automated | Actual footprints, original envelopes, all rotations and three LODs |
| Ground and elevated model transforms | PASS automated | Z0/Z1/Z3/Z4/Z5, finite coordinates, atlas bounds and unchanged native state |
| Native charge and generation indicators | PASS automated | Actual buffered status / productionEfficiency; clocks do not follow global time |
| Native and XYZ link topology | PASS automated | Real configuration, reciprocal dedup, cross-floor terminals, range rejection and disconnect |
| Destruction, restore and ownership changes | PASS automated | Stale source removal, existing replication roundtrip and foreign-edge filtering |
| Stable topology cache | PASS automated | Repeated sync and charge changes do not rebuild meshes or mutate native links |
| Vertical and diagonal beam geometry | PASS automated | Finite world-space tube axes, radius and endpoint phase anchoring |
| Authorized upper construction and copying | PASS automated | 112 source blocks +112 queued copies; internal power links, no copied stored energy; invalid ceilings rejected |
| Full Java17 / stock Steam25 regression | PASS | 183 programs per runtime against final JAR |
| Actual Windows SDL/OpenGL | PASS scripted | 5,888 final frames per runtime +5,888 baseline; 46 scenes each; zero GL errors |
| Power-pass GL state restoration | PASS scripted | 46 probes per final runtime; depth/write/blend/cull/program restored |
| Native clocks / generator activity | PASS scripted | Every generator active on ground and upper layer; controlled stock outside timed drawing |
| Disk SaveIO / ghosts / clipboard | PASS scripted | Disposable world/profile; no primary save touched |
| Link density / toggle / lifecycle | MEASURED + PASS scripted | 10/100/512, stacked 139 valid links, removal9/reconfigure8; no toggle-only topology rebuild |
| Independent source / packaging | PASS | 1010 classes/resources; strict JAR/folder fingerprint on both runtimes |
| Prior assets / licenses | PASS | 2065 Alpha files +102 diagnostic assets preserved; unchanged native dependencies |
| Repeated dense-scene comparison | MEASURED | Four repeats ×4 scenarios ×128 frames per baseline/final; 2,048 extra frames each; GPU variance and model cost retained, not an FPS acceptance claim |
| Owner F5/Tab/input and visual acceptance | NOT TESTED | Three checks in manual procedure |
| Physical Linux / Wayland / X11 | NOT TESTED | Same portable JAR; Windows Java validation is not Linux device acceptance |
| Remote two-computer multiplayer | NOT TESTED | Existing strict identity/server rules preserved; no remote gameplay claim |
| Real gameplay FPS parity | NOT TESTED | Whole-renderer benchmark data is reported separately |
