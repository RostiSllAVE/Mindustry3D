# A2-16i — Test matrix

Baseline: owner-accepted A2-16h `77480dd`. Native/GL fixtures are not physical
owner acceptance. Final measurements and program evidence accompany the report.

| Test | Status | Scope |
| --- | --- | --- |
| Installed eleven-block registry | PASS automated | Real classes, sizes, capacities and optional editor-only shield audit |
| Shared templates / native atlas | PASS automated | All LODs, bounds, UVs, finite transforms and cached data |
| Z0/Z1/Z3/Z4/Z5 and rotations | PASS automated | Body/head parts within unchanged physical envelopes |
| Readiness / actual-work flags | PASS automated | Native charge/heat, didRegen, Build Tower plans; no render-time fake clock |
| Shield hardware state | PASS automated | Broken/available native state; drawing does not modify shield values |
| Storage indicators | PASS automated | Empty/half/full per-item capacity and linked-core accepted limits |
| Real repair / acceleration | PASS automated | Actual unchanged upper Mend heal, overdrive timeScale and Regen heal |
| Upper copying / save state | PASS automated | 88 sources +88 queued copies, empty copied inventories and roundtrip |
| Shield/build compatibility | PASS automated | Existing real contact, independent floor identities and native tower reconstruction |
| Java17 / stock Steam25 full regressions | PASS | 187 programs per runtime against final JAR |
| Actual Windows SDL/OpenGL | PASS scripted | 44 scenes /5,632 frames per final runtime +baseline; zero GL errors |
| Real shield state / contact | PASS scripted | One actual authoritative hit, native depleted/recovery, 54 active faces vs zero depleted; overlap/depth/night views |
| Actual disk SaveIO / ghosts / clipboard | PASS scripted | Disposable map/profile, native state read-only during drawing |
| Dense rendering / cache | MEASURED | 100/1,024 mixed, 1,024 storage, 512 support blocks, overlapping fields; CPU/GPU/draw/vertex/cache data |
| Source / payload identity | PASS | 1022 independently recompiled classes/resources; strict JAR/folder identity on both runtimes |
| Previous assets / dependencies | PASS | 2065 Alpha files +108 diagnostic assets preserved; licenses/JNA unchanged |
| Owner Windows gameplay/input | NOT TESTED | Three manual checks |
| Physical Linux / X11 / Wayland | NOT TESTED | Portable code paths, no Linux-device acceptance claim |
| Real remote multiplayer | NOT TESTED | Existing authority/protocol unchanged; no physical two-PC claim |
| Sustained gameplay FPS | NOT TESTED | Scripted rendering timings are reported separately |
