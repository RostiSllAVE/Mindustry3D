# A2-16o — model coverage update

Read-only audit of the actual standalone Mindustry 160.7 definitions: **447 vanilla entries**. The accompanying CSV records every name, footprint, native class, group and current rendering path. Historical A2-16l inventories remain unchanged.

**14/14 requested utility bodies now have dedicated cached profiles**, replacing their generic/native body path:

| Native name | Presentation / native behavior |
|---|---|
| `item-source`, `item-void` | Resource-port pedestal, configured item color / actual inventory output or disposal |
| `liquid-source`, `liquid-void` | Tank/collar silhouette, configured liquid color / actual native liquid output or disposal |
| `power-source`, `power-void` | Electrical mast and collar / unchanged native graph generation or consumption |
| `payload-source`, `payload-void` | Open gantry/bay / native configured payload production or disposal |
| `repair-point`, `repair-turret` | Pedestal and tracking repair head / actual target, strength and native healing |
| `payload-conveyor`, `payload-router` | Low roller deck, guide rails and router hub / actual carried payload and native transport |
| `tile-logic-display` | Volumetric console edge and original connected bezel / live native shared canvas, correct per-tile UV |
| `target-dummy` | Target stand and readable target plate / existing actual target unit and native hit/DPS handling |

Previous **9 logic**, **6 unit-factory**, **8 reconstructor**, core, turret, drill, logistics, manufacturing, power, support and liquid models keep their accepted paths. Native repair beams retain their existing effect renderer. Payload icons remain the existing lightweight carried-unit/building representation; this task does not redesign carried unit meshes.

There are 127 explicit dedicated profiles overall, plus 26 native turret-part paths and 29 shared wall paths. The remaining 79 generic/native entries and 186 environment/internal entries are enumerated separately; this is not a claim that all remaining entries are missing visible models or eligible for construction. Assemblers remain a future dedicated-model pass.

`A2-16o-GEOMETRY.csv` reports all 42 utility templates (14 × three LODs): **407,808 raw vertex bytes**, at most **534 vertices / 178 triangles** per model template. These immutable CPU arrays are shared across instances. Each active world/layer renderer caches its material-mapped copies and streams transformed instances through existing atlas batches. There are no new per-instance textures, per-frame topology generation or new model rendering pipeline. Dynamic live canvases retain native framebuffer ownership and only process queued changes.

Reproduction helper: `tools/java/ModelCoverageAudit.java`, compiled against the diagnostic classes plus its existing native test bootstrap. Counts describe templates rather than GPU time or complete Java heap usage.
