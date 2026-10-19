# A2-09a — Volumetric 3D structure selection

Unreleased local diagnostic, Mindustry 160.7; internal mod version 0.18.31 and
protocol 4 retained. This is the owner's renamed A2-19a task, following accepted
A2-08b at `4b9e0b76680aab97ad5f48e83c8d90ac73c23b76` on `main`.

## Controls

Enter FPV with F5, then Tab / Build Mode. Click **3D Selection** in the left
Build panel or press **F4** (remappable in the host Controls menu).

| Input, only while this mode is ON | Result |
|---|---|
| LMB drag | Set inclusive X/Y cell rectangle on the starting surface's horizontal plane |
| Wheel up / down over the world | Raise / lower the OTHER Z boundary, during or after dragging |
| Release LMB | Keep the completed volume |
| New LMB drag | Replace it; abandoning this draft preserves the last completed volume |
| RMB / Clear 3D selection | Clear volume; stay in the mode |
| Esc / F4 / mode button | Leave mode; retain completed volume in this world |
| Tab, F5, command mode, focus loss or modal menu | Leave mode and abort any unfinished drag |

Wheel behavior outside this mode is unchanged. Wheel over the panel scrolls the
UI, not the volume. F3/native construction pause remains available and is never
automatically changed by selection. Existing construction continues unless the
player pauses it. The advanced panel and manual placement-floor controls remain
available. F4 is only a default; the panel button displays the configured key.

## Selection semantics

The shared `BuildingMousePicker.pickScene` resolves the nearest visible surface.
Its actual building/platform floor is the anchor, not its wall-top placement
floor and not the pilot's altitude. A locked construction floor explicitly
supersedes the surface and also permits starting over empty space. X/Y drag stays
on the initial surface height: subsequent floors under the pointer cannot retarget
it. Supported bounds are Z0 through the world's configured maximum (normally 64,
up to the existing 512 limit); there is no negative Z or digging.

The selection is one X/Y rectangle with an inclusive Z floor interval. Its
bounding box spans the horizontal cell faces and the full vertical floor bands.
A completed block is included if its full occupied bounds have positive overlap
with that volume; a tall column may therefore be included even when its anchor
is below the selected interval. Platform slabs belong to their anchor floor:
selecting Z2 includes its supporting slab, which physically extends just below
the Z2 plane; it does not include the Z3 slab. This matches existing layered
construction's floor-cell convention.

Whole multi-tile structures are selected once, including structures only partly
intersected by the rectangle. Their outlines may extend beyond the box; the
selection never clips buildings. Actual size/rotation/team/anchor/physical bounds
are retained. Empty volumes are valid. Unfinished native ConstructBuild blocks,
pending upper plans, units and natural terrain are excluded. Fog-hidden buildings
are filtered using the existing discovery rules; outlines depth-test against the
world. Selection is not permission to build/copy enemy structures.

Yellow edges and a translucent cyan shell show the full volume; cyan individual
outlines identify members. The panel shows the floor range, anchor, dimensions,
structure count and type counts (six types plus a remaining-type count). Up to
1,024 frustum-visible members are outlined per frame; membership and all counts
remain complete beyond this rendering budget. No x-ray outlines or per-building
textures/meshes are generated.

## Implementation and safety

- `StructureVolume`: immutable read-only member descriptors; native team building
  quadtrees plus existing upper `LayerWorld.query` spatial index. No map tile scan.
  Requery on selection/geometry/native tile revisions; idle fog/rotation metadata
  refresh is at most 5 Hz. Inactive retained volumes do not run these queries.
- `VolumeSelectionTool`: fixed anchor/drag plane, fractional wheel accumulation,
  once-per-frame wheel sampling, draft/committed lifecycle and held-pointer fence.
- `VolumeSelectionRenderer`: existing white atlas and streaming mesh builder,
  bounded member outlines, frustum culling and depth-tested translucent volume.
  Restores GL blend/depth/cull state; no per-member expanded-bounds allocation.
- The controller owns the pointer before priority, schematic, placing, linking or
  removal code. Native event and polling gates prevent duplicate interpretation.
  Held input cannot become a flight shot after leaving selection. F3 construction
  intent and all committed native/upper jobs remain untouched.

No construction command, network channel/packet, resource operation, save format,
copy/export operation, combat, camera architecture or physics change is added.
Existing protocol, combat, physics and camera classes are checked byte-for-byte
against A2-08b in the artifact audit. Matching full diagnostic payloads are still
required by the existing multiplayer fingerprint check.

## Validation and limitations

See [test matrix](A2-09a-TEST-MATRIX.md), [machine-readable results](A2-09a-VALIDATION.json)
and [owner procedure](A2-09a-MANUAL-TEST.md). Native fixtures exercise real content,
world data, spatial indexes and the production pointer boundary with mocked host
input/GL. They are not physical gameplay or GPU visual acceptance. The shared
JAR is suitable for Windows/Linux testing; no physical Linux acceptance is claimed.

The 4,096-structure membership/cache microbenchmark is measured separately from
rendering; it is not a frame-rate comparison. The existing outline budget caps
presentation cost, not membership. Full-map membership can still take more work
when dragging a very large selection through densely built worlds. Completed
volumes are local, not saved or shared with peers, and clear on world/team reset.

## Preparation for A2-09b (original A2-19b)

Existing `LayerSchematic.capture` creates a vanilla single-floor `.msch`, preserving
block, relative X/Y, rotation, supported configuration and internal native links;
platform cells use the existing `fpv-platforms` tag. Its current bounds and encoded
size limits remain unchanged. It cannot represent arbitrary multi-floor structures
or cross-floor connection identities. This pass does not call it or change it.

A later copy operation can resolve each selected ID back to its live structure,
normalize X/Y and relative floor/Z around an explicit origin, read only supported
vanilla configuration, and remap internal XYZ links. Revalidate membership, team,
support and server permissions at that time. Never copy health, inventories,
resources, work progress or live modules. Partial-footprint selection selects whole
objects, so their anchors can lie outside the drawn rectangle: future origin and
preview bounds must account for that. No new schematic schema is committed here.

Final runs: **132/132 Java 17**, **132/132 stock Steam Java 25**, zero failures;
54 new selection assertions, 45 pause, 79 priority and 21 priority-network assertions.
For 4,096 members, mean refresh was 2.558 ms on Java 17 and 3.541 ms on Steam Java 25 (30 samples each); cached means were 0.000040 / 0.000045 ms (10,000 calls). These are fixture CPU costs, not FPS or GPU timings.
JAR/folder payload identities match; 974 existing classes and all previous Alpha snapshot files / 24 previous diagnostic assets remain unchanged.
