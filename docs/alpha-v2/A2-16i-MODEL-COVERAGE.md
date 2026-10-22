# A2-16i — Support and storage model inventory

Baseline: owner-accepted A2-16h `77480dd30aebfd09b75890d3315448da74f55122`, `main`.
The supplied task, installed 160.7 registry, native state APIs and both world
renderers were inspected before implementation.

| Native block | Cells | Dedicated model / native presentation state |
| --- | --- | --- |
| mender | 1 | Compact optical emitter, brackets and supported charge ring; heat/charge/phaseHeat |
| mend-projector | 2 | Four supports and larger central repair assembly; native readiness/charge |
| overdrive-projector | 2 | Processing vessel, shaft and rotating emitter ring; native charge/heat |
| overdrive-dome | 3 | Four substantial pylons, fixed crown and inner emitter ring |
| force-projector | 3 | Three emitter supports, central core and working/depleted collar |
| container | 2 | Compact framed storage body, original roof and two loading arches per side |
| vault | 3 | Heavy housing, extra reinforcement and three loading arches per side |
| regen-projector | 3 | Three optical spires, central emitter and actual-regeneration indication |
| build-tower | 3 | Bearing, gantry and original-textured rotating construction head |
| reinforced-container | 2 | Erekir corner armor, raised roof details and native inventory indicator |
| reinforced-vault | 3 | Larger armored housing, reinforcement bands and angular roof assembly |

Dedicated mechanical coverage: **0/11 → 11/11**. Existing generic bodies/roof
art were replaced only for this exact set. The already working Force Projector
field is preserved; it already uses authoritative finite XYZ bounds and hit data.
Earlier power, liquid, factory, logistics, drill and unit implementations remain
unchanged.

Storage item capacities in the installed definitions are Container300,
Vault1000, Reinforced Container160 and Reinforced Vault900 **per item**. Linked
cores can change the native accepted limit. No capacity is changed by this pass.
Indicators report the most-filled actual native item slot, not an invented
aggregate warehouse capacity.

Optional audit: `shield-projector` and `large-shield-projector` are BaseShield
objects with **editorOnly** visibility in installed160.7. Existing upper adapters
support them, but they have campaign/editor barrier semantics and a different
ground rendering path. Their new models are **deferred**. `Blocks.shieldBreaker`
is declared in the installed API but is null/unregistered in this content load;
no nonexistent buildable model is invented.

All new meshes fit the existing physical heights and footprints. Taller native
storage/support blocks require ordinary headroom; Z1/Z3/Z5 stacking is tested.
No collision, construction-plane, influence-range or gameplay classification
change is introduced.
