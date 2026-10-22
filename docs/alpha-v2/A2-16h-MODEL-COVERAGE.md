# A2-16h — Liquid logistics model coverage

Baseline: owner-accepted A2-16g `b373e1a033eb759f8039d1c787bc4b6dd4b7b301`, `main`.
The attachment and installed Mindustry 160.7 definitions were inspected first.
No dedicated cached models existed for these thirteen types. LiquidBlock had
generic bodies/top liquid decals, conduits had generic slabs, and Thermal
Generator had a generic body. Existing functional bridge spans are retained.

| Native block | Cells | Capacity | New presentation |
| --- | --- | --- | --- |
| conduit | 1 | 20 | Open inspection channel, native-color housing and edge connectors |
| pulse-conduit | 1 | 40 | Taller collars and luminous inspection bands |
| plated-conduit | 1 | 50 | Reinforced crossing straps and thicker side protection |
| liquid-router | 1 | 120 | Radial distributor with four connector arms |
| liquid-junction | 1 | 10 | Two visibly separated crossing passages; no invented inventory |
| bridge-conduit | 1 | 100 | Twin supports, elevated crossbar and terminal |
| phase-conduit | 1 | 100 | Reinforced terminal with paired phase housings |
| liquid-container | 2 | 700 | Four-post storage frame, upper ring/cap and visible chamber |
| liquid-tank | 3 | 1800 | Larger octagonal frame, bracing and visible chamber |
| mechanical-pump | 1 | 20 | Compact intake, bearing, shaft and supported rotor |
| rotary-pump | 2 | 80 | Turbine housing, shaft and rotating impeller |
| impulse-pump | 3 | 200 | Four compression columns, intake and central impeller |
| thermal-generator | 2 | 10 | Geothermal vessel, exchangers, fins, pipes and heat indicators |

Requested dedicated coverage: **0/13 → 13/13**. Impulse Pump is present; it is not
an invented alias. Container/Tank are native LiquidRouter subclasses. Plated
Conduit uses ArmoredConduit; the two bridges use LiquidBridge. All original
capacities, placement/transport rules and native classes remain unchanged.

The previous fourteen A2-16g power profiles and all earlier factory, drill,
logistics, unit and terrain model implementations remain unchanged. Erekir
reinforced liquid blocks and other unlisted content are deferred.

Like the accepted item-logistics renderer, this pass gives flat-category native
blocks taller **visual** machinery, without changing collision bounds. Standard
1.4-tile construction planes accommodate the maximum 1.12-tile tank. Native
selection/blueprint outlines retain their existing physical footprint/height.
Unusually tight custom floor spacing can still intersect visual machinery; this
pass does not change structural or collision rules to hide that limitation.
