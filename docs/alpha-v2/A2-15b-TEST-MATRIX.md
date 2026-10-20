# A2-15b — Test matrix

Baseline A2-15a.2 `e07c8d0`; existing `main`. Local phase checkpoints: `5c03e87` (depth/presets), `c256a7a` (painter); final persistence checkpoint follows this report. Final JAR SHA-256: `c91bb1319bcc25187d2f8e6c35bf6f807e241826cf581329fb8263fc1b1ad34e`.

| Check | Result | Evidence / scope |
| --- | --- | --- |
| Full Java17 regression suite | PASS 159/159 | All existing/new test programs; separate JVMs |
| Full stock Steam25.0.1 suite | PASS 159/159 | Host runtime without java.logging; no separate player Java needed |
| Presets/Auto/directions | PASS | 304 pure +30 native checks per runtime; live mesh revision, native floors unchanged |
| Visual bathymetry/native edge | PASS 36 native checks | Progressive bed, actual edge collar, ocean depth; authoritative LiquidBody unchanged |
| Painter mask/history | PASS 915 checks | Continuous XY, protected native cells, island/material/height preservation, water clearing, erase, 32-stroke bound |
| Actual Custom geometry | PASS 2,261,496 checks | Deterministic islands/forest/mountains/void, clipped coast, flat water, outside-only vertices, bounded trees |
| Native staging/Preview/Apply | PASS 6 checks | Stable snapshots; no per-stroke rebuild; native entities/tiles unchanged |
| Persistence safety | PASS 20 checks | Exact v1 roundtrip, identity/CRC/version/enums/NaN/size/protected cells, backup recovery, invalid preservation, file-count cap |
| Campaign/custom native save reload | PASS 9 checks | Real planet generators + disk SaveIO; sector/planet separation, missing author, map isolation, stale preview ownership |
| Actual Steam SDL gallery | PASS 528 frames /11 views | Real canvas event path, enabled Undo/Redo, Preview/Apply, settings modal, coastal/submerged/island/forest/ridge/space/void views; GL0 |
| Fresh second host JVM | PASS 48 frames | Copied disposable profile/save restores exact applied Custom art after restart; GL0 |
| CPU preparation/editor/codec | MEASURED | Before/after native64/512 Auto,96/512 presets+Custom,300 strokes,100 roundtrips; see report and performance-comparison.json |
| Background-only asynchronous GPU | PASS + MEASURED | 720 baseline/1,440 final frames; four shared cases plus Ocean/Custom; GL0, atlas/native state unchanged; Auto GPU +0.03–0.31ms in this pair |
| Source/binary correspondence | PASS 949 classes | Fresh Java17 compilation byte-identical; resources and design inputs checked |
| Distribution/old artifacts | PASS | 1,029 existing runtime classes unchanged; all authoritative systems unchanged;2,065 Alpha files +63 diagnostic assets preserved |
| JAR vs expanded-folder identity | PASS Java17 +Steam25 | Same existing ModFingerprint code, strict payload identity; no networking changes |
| F5/Tab/F3/priority/construction/selection/copy | PASS automated regressions | Physical UI/gameplay acceptance remains owner work |
| Linux physical graphics/input | NOT TESTED | Shared Arc/Java code paths and regression tests do not substitute for X11/Wayland gameplay |
| Remote multiplayer with different local art | NOT TESTED physically | No new packets/save chunks; network/authority regression programs pass; do not claim two-computer acceptance |

Intermediate failed compile/fixture runs are retained locally. Final results above refer only to the final JAR and its recorded lab overlays, never to untested physical gameplay.
