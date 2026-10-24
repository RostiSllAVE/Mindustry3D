# A2-16l — Complete vanilla block-model coverage inventory

Mindustry 160.7 native audit: **447 registered vanilla entries**: 186 environment/internal, 261 others. The CSV enumerates native name, footprint, class, group and render coverage; it omits unstable runtime object hashes. This is a geometry inventory, not new build permissions. Hidden/editor/internal blocks are not advertised as ordinary playable factory targets.

Unit-upgrade coverage is **0/8 dedicated bodies → 8/8**: Additive, Multiplicative, Exponential, Tetrative, Tank, Ship, Mech and Prime. The accepted **6/6 dedicated unit factories remain**, with only the requested assembly-layer/lamp/arm corrections. Previous core, drill, item/liquid logistics, manufacturing, power, support/shield/storage and utility families are retained. Conditional turret geometry and native dynamic overlays remain separate existing paths rather than newly modeled profiles.

| Group | Entries | Explicit dedicated profile | Other existing paths |
|---|---:|---:|---:|
| Assemblers | 4 | 0 | 4 |
| Defense | 58 | 0 | 58 |
| Environment/internal | 186 | 0 | 186 |
| Logic | 17 | 0 | 17 |
| Miscellaneous / crafting | 36 | 17 | 19 |
| Miscellaneous / distribution | 28 | 16 | 12 |
| Miscellaneous / effect | 27 | 23 | 4 |
| Miscellaneous / liquid | 21 | 12 | 9 |
| Miscellaneous / power | 25 | 15 | 10 |
| Miscellaneous / production | 14 | 7 | 7 |
| Miscellaneous / units | 17 | 0 | 17 |
| Reconstructors | 8 | 8 | 0 |
| Unit production | 6 | 6 | 0 |

## Remaining work

The three assemblers and their module (`tank-assembler`, `ship-assembler`, `mech-assembler`, `basic-assembler-module`) retain their existing generic/native path. They do not upgrade an existing unit through the native Reconstructor mechanism and are not part of this request. No third-party factory is overridden.

Generic/native fallback does not mean no dynamic feature: screens, configuration colors, payload images and other overlays can already exist. Terrain/props/void and shared textured walls are not missing factory targets. For complete remaining block names, filter the CSV by coverage; the preceding A2-16k inventory remains an immutable historical baseline. Reproduction helper: `CoverageAudit.java` in the local test kit, reading installed content definitions and existing explicit profile selectors without changing them.
