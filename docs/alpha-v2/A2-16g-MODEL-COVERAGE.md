# A2-16g — Power model coverage inventory

Baseline: completed A2-16f `3636d2d0d3fe0a1413848df2e7e1fe93fd502a7f`, `main`.
The owner plans to test A2-16f together with this pass; physical acceptance is pending.

The installed 160.7 native definitions, native drawer layouts and both world
renderers were inspected. No dedicated cached power-family model existed for
these fourteen blocks. Nodes had the existing generic mast/crossarm presentation;
the other blocks retained generic bodies or roof art. No separate high-quality
power model was discarded. Familiar mast/base silhouettes and original materials
are retained in the new explicit profiles.

| Block | Size | New geometry / state |
| --- | --- | --- |
| Power Node | 1×1 | Narrow mast, collar, emitter and native graph indicator |
| Large Power Node | 2×2 | Four-post transmitter, wider mast and crossed terminals |
| Surge Tower | 2×2 | Open ladder tower, elevated crossbars and emitter |
| Power Diode | 1×1 | Directional terminal block; original separate arrow texture and native rotation |
| Battery | 1×1 | Four storage cells, side/top charge panels |
| Large Battery | 3×3 | Nine storage cells, enclosing cap, side/top charge panels |
| Solar Panel | 1×1 | Thin native panel, frame, four supports and junction |
| Large Solar Panel | 3×3 | Larger supported panel assembly and junction |
| Combustion Generator | 1×1 | Compact engine, exhaust stacks, shaft, rotor and heat window |
| Steam Generator | 2×2 | Twin turbine housings, shafts, rotating vanes and stacks |
| Differential Generator | 3×3 | Paired heat exchangers, cooling fins and connecting machinery |
| RTG Generator | 2×2 | Enclosed faceted generator, cooling ribs, restrained native thorium palette |
| Thorium Reactor | 3×3 | Reactor vessel, four cooling columns and active chamber cap |
| Impact Reactor | 4×4 | Four industrial pylons, central energy chamber and rotating ring components |

Requested-family dedicated coverage: **0/14 → 14/14**, shared by ground and
eligible elevated placements. The previous twenty A2-16d/e/f factory/extractor
profiles, drills, logistics, turrets and unit families keep their implementations.
See their existing coverage reports for individual entries. Thermal Generator,
other unlisted power blocks and Erekir models remain unchanged.

Physical heights stay exactly as provided by the existing native/XYZ catalog;
new meshes fit inside those envelopes. Solar panels use a deliberately lower
visual structure without reducing their existing collision envelope. This is a
visual pass, not a power, collision, placement or balance change.
