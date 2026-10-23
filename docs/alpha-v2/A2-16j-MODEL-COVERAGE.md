# A2-16j — Core and base-infrastructure coverage

Baseline: `c86f5d2` on `main`, preserving A2-16i-fix1 and the subsequent turret correction. The owner deferred physical acceptance of that turret correction until this pass. The complete supplied A2-16j task, installed 160.7 definitions, original body/team sprites, native state and both rendering paths were inspected before implementation.

| Block | Native footprint | Before | A2-16j |
|---|---:|---|---|
| Core: Shard | 3×3 | Shared generic core chamber/pylons | Compact octagonal command module, four small corner nodes, diagonal braces and input bays |
| Core: Foundation | 4×4 | Same shared core geometry | Reinforced lower octagon, square corner armor, crossing support rails and taller tiered central assembly |
| Core: Nucleus | 5×5 | Same shared core geometry | Broad armored octagonal headquarters, heavier corner structures and raised central energy tower |
| Core: Bastion | 4×4 | Same shared core geometry | Erekir crossed armored housing, substantial corner supports and stepped command cap |
| Core: Citadel | 5×5 | Same shared core geometry | Wider fortress skirt, diagonal armor banks, eight-sided corner bunkers and upper command module |
| Core: Acropolis | 6×6 | Same shared core geometry | Monumental crossed terraces, four heavy perimeter towers and high central angular command structure |
| Illuminator | 1×1 | Accepted dedicated Utility model | Preserved, including configured colour and corrected cap/culling/floor contact |
| Radar | 1×1 | Generic body and flat top | Pedestal/bearing, rotating yoke and cached volumetric original dish; native scanning clock/readiness |

Dedicated tier-specific core coverage: **0/6 → 6/6**. Radar receives a new dedicated mechanism. Illuminator is not replaced.

The original body and team artwork is mapped over model roofs in footprint coordinates; no external texture is introduced. Resource inputs retain the established visual convention of one bay per footprint cell on each side. These bays do not add routes or change native input/output behavior. Models fit existing physical heights: Shard1.64, Foundation2.02, Nucleus2.40, Bastion2.02, Citadel2.40, Acropolis2.78 tiles.

Native core definitions remain unchanged:

| Core | HP | Per-item base capacity | Unit-cap modifier | Original core unit |
|---|---:|---:|---:|---|
| Shard | 1,100 | 4,000 | 8 | Alpha |
| Foundation | 3,500 | 9,000 | 16 | Beta |
| Nucleus | 6,000 | 13,000 | 24 | Gamma |
| Bastion | 4,500 | 2,000 | 15 | Evoke |
| Citadel | 16,000 | 3,000 | 15 | Incite |
| Acropolis | 30,000 | 4,000 | 15 | Emanate |

Linked storage and team core inventories still follow native/previous layer lifecycle rules. Erekir cores retain `requiresCoreZone`; no core placement, upgrade, environment or team permission is relaxed.

## Preserved and deferred infrastructure

- **Interplanetary Accelerator:** installed native 7×7 `AcceleratorBuild`; audited and deferred. Its launch/charge/campaign state warrants a separate model task. Existing general/native launch presentation and upper adapter remain unchanged.
- **Launch Pad, Advanced Launch Pad, Landing Pad, Shock Mine:** accepted Utility geometry and real-shipment effects retained.
- **Storage, repair, shield, boost/build support, power, liquid logistics, factories, drills, conveyors and advanced item logistics:** their dedicated renderers and shared geometry remain unchanged. See the preceding A2-16a–i inventories.
- **Logic processors, displays and remaining utilities:** existing generic/native presentation retained. No broader base, logic or payload family pass is attempted.
- **Other/modded CoreBlock types:** retain the previous generic core fallback; the new profiles cover only the six explicit vanilla definitions.

One shared atlas-stream implementation serves ground cores and existing layered instances. Candidate upper-core fixtures verify rendering only; they are not new construction permissions. Real server-authorized elevated placement/copy tests in this pass target Illuminator and Radar. Existing upper-core inventory/respawn/campaign regressions are also retained.
