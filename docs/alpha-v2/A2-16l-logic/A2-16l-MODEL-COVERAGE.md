# A2-16l logic — Complete native block-model inventory

Installed Mindustry 160.7: **447 entries**, with 186 environment/internal and 261 others. The CSV enumerates every native name, footprint, class, group and render path. Geometry coverage does not grant new build permissions; hidden/editor/world logic is not advertised as normal player content.

Logic coverage is **0/9 dedicated bodies → 9/9 requested profiles**, with real native screen contents on both Z0 and upper floors. The accepted six factories, eight upgrade facilities and all earlier dedicated families remain. Existing generic/native overlays, conditional turret models and environmental paths are not counted as newly purpose-built logic bodies.

| Group | Entries | Explicit dedicated profile | Other existing paths |
|---|---:|---:|---:|
| Assemblers | 4 | 0 | 4 |
| Defense | 58 | 0 | 58 |
| Environment/internal | 186 | 0 | 186 |
| Logic | 17 | 9 | 8 |
| Miscellaneous / crafting | 36 | 17 | 19 |
| Miscellaneous / distribution | 28 | 16 | 12 |
| Miscellaneous / effect | 27 | 23 | 4 |
| Miscellaneous / liquid | 21 | 12 | 9 |
| Miscellaneous / power | 25 | 15 | 10 |
| Miscellaneous / production | 14 | 7 | 7 |
| Miscellaneous / units | 17 | 0 | 17 |
| Reconstructors | 8 | 8 | 0 |
| Unit production | 6 | 6 | 0 |

Remaining logic bodies: `tile-logic-display`, `canvas`, `large-canvas`, `reinforced-message`, `world-processor`, `world-cell`, `world-message`, `world-switch`. These retain their previous native/generic behavior. Assemblers/basic module remain deferred. Existing Message and Canvas text/graphics mechanisms are not replaced. The earlier A2-16l inventory is preserved as a historical checkpoint. Reproduction helper: `CoverageAudit.java` in the kit.
