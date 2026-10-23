# A2-16k — Complete vanilla block-model coverage inventory

Installed Mindustry 160.7 content audit: **447 registered vanilla blocks**, including 186 environment/internal entries and 261 other entries. The accompanying CSV enumerates every native name, footprint, class, group and current model path. Hidden/editor/world-logic content is not advertised as ordinary player-buildable content.

This inventory describes geometry coverage, not new placement permissions. “Generic/native fallback” means no dedicated whole-body profile in the inspected families; existing dynamic screens, configuration colours, payloads and other specialized overlays may still be present. Turret support remains conditional on its native DrawTurret/RegionPart atlas path. Shared textured walls and environmental geometry are not counted as newly purpose-built factory models.

## Before / after

Unit Factory coverage: **0/6 dedicated bodies → 6/6**. Ground has an armored mechanical bay; Air a high gantry; Naval a shipbuilding cradle; Tank armored engineering banks; Ship an aerial assembly frame; Mech a tall guided mechanical assembly. All original top art, output directions, recipes and native progress/payload behavior remain in use. Every native footprint is 3×3. Existing dedicated cores, drills, logistics, manufacturing, power, liquid, repair/shield/storage and utility families are preserved.

## Grouped inventory and remaining work

| Group | Native entries | Dedicated new/previous profile | Other existing paths |
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
| Reconstructors | 8 | 0 | 8 |
| Unit production | 6 | 6 | 0 |

### Unit production

All six explicit vanilla factories now have dedicated models. No modded factories are overridden.

### Reconstructors

Remaining without a dedicated whole-body profile: `additive-reconstructor`, `multiplicative-reconstructor`, `exponential-reconstructor`, `tetrative-reconstructor`, `tank-refabricator`, `ship-refabricator`, `mech-refabricator`, `prime-refabricator`.

### Assemblers

Remaining without a dedicated whole-body profile: `tank-assembler`, `ship-assembler`, `mech-assembler`, `basic-assembler-module`.

### Logic

Remaining without a dedicated whole-body profile: `message`, `switch`, `micro-processor`, `logic-processor`, `hyper-processor`, `memory-cell`, `memory-bank`, `logic-display`, `large-logic-display`, `tile-logic-display`, `canvas`, `large-canvas`, `reinforced-message`, `world-processor`, `world-cell`, `world-message`, `world-switch`.

### Defense

Remaining without a dedicated whole-body profile: `parallax`, `segment`, `target-dummy`.

### Miscellaneous / crafting

Remaining without a dedicated whole-body profile: `incinerator`, `silicon-arc-furnace`, `electrolyzer`, `atmospheric-concentrator`, `oxidation-chamber`, `electric-heater`, `slag-heater`, `phase-heater`, `heat-redirector`, `small-heat-redirector`, `heat-router`, `slag-incinerator`, `carbide-crucible`, `slag-centrifuge`, `surge-crucible`, `cyanogen-synthesizer`, `phase-synthesizer`, `heat-reactor`, `heat-source`.

### Miscellaneous / distribution

Remaining without a dedicated whole-body profile: `duct`, `armored-duct`, `duct-router`, `overflow-duct`, `underflow-duct`, `duct-bridge`, `surge-conveyor`, `surge-router`, `unit-cargo-loader`, `unit-cargo-unload-point`, `item-source`, `item-void`.

### Miscellaneous / effect

Remaining without a dedicated whole-body profile: `shockwave-tower`, `shield-projector`, `large-shield-projector`, `interplanetary-accelerator`.

### Miscellaneous / liquid

Remaining without a dedicated whole-body profile: `reinforced-pump`, `reinforced-conduit`, `reinforced-liquid-junction`, `reinforced-bridge-conduit`, `reinforced-liquid-router`, `reinforced-liquid-container`, `reinforced-liquid-tank`, `liquid-source`, `liquid-void`.

### Miscellaneous / power

Remaining without a dedicated whole-body profile: `beam-node`, `beam-tower`, `beam-link`, `turbine-condenser`, `chemical-combustion-chamber`, `pyrolysis-generator`, `flux-reactor`, `neoplasia-reactor`, `power-source`, `power-void`.

### Miscellaneous / production

Remaining without a dedicated whole-body profile: `vent-condenser`, `cliff-crusher`, `large-cliff-crusher`, `plasma-bore`, `large-plasma-bore`, `impact-drill`, `eruption-drill`.

### Miscellaneous / units

Remaining without a dedicated whole-body profile: `repair-point`, `repair-turret`, `unit-repair-tower`, `payload-conveyor`, `payload-router`, `reinforced-payload-conveyor`, `reinforced-payload-router`, `payload-mass-driver`, `large-payload-mass-driver`, `small-deconstructor`, `deconstructor`, `constructor`, `large-constructor`, `payload-loader`, `payload-unloader`, `payload-source`, `payload-void`.


Reconstructors/refabricators and the three assemblers plus their basic module are deferred to A2-16l. Logic/display entries retain their existing native overlays and general bodies. Environment/internal entries retain terrain, props, void and other existing world paths; they are not447 factory-model targets. This is a pinned160.7 inventory, not a promise about future or third-party content.

Reproduction: the test-kit evidence includes `CoverageAudit.java`, using the installed content loader and explicit existing profile selectors. No definitions were changed to generate the inventory.
