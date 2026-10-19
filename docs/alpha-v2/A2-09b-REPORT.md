# A2-09b — Volumetric structure copy and paste

Unreleased diagnostic for Mindustry 160.7, following owner-accepted A2-09a
`acc71fe514564a2ebab06b3eaab8038014bc12d2` on `main`. This is the renamed
A2-19b task. Internal mod version 0.18.31 / protocol 4 remain diagnostic-only;
no published Alpha assets, Workshop entry or remote repository are changed.

## Player workflow

F5 → Tab → F4. Select an X/Y rectangle and use the wheel for its Z range, then
click **Copy 3D selection**. Copy enters a dedicated paste preview. **Paste 3D
clipboard** starts another placement from the same session clipboard.

| While explicit 3D mode is active | Action |
|---|---|
| Ctrl+Shift+C | Copy completed selection and enter paste preview |
| Ctrl+Shift+V | Reuse current 3D clipboard |
| Mouse over world in paste mode | Move the whole structure horizontally |
| Wheel over world | Raise/lower the destination base floor, preserving all relative floors |
| R / Shift+R | Rotate 90° counterclockwise / clockwise |
| LMB | Confirm one validated construction batch; exit preview back to selection |
| RMB during paste | Cancel preview, keep selection and clipboard |
| Esc / F4 | Leave 3D mode, keep session clipboard |
| Tab / F5 / menu / focus loss / command mode | Cancel unconfirmed preview and release mode ownership |

The initial destination uses existing surface placement picking, or the explicitly
locked floor. Afterwards wheel controls the base floor; X/Y comes from the shared
locked-plane resolver. A ray miss cannot confirm the last remembered destination.
Wheel over the Build panel remains UI scrolling. Copy/paste/rotate keys are
remappable in host Controls; the Ctrl+Shift modifier applies to copy/paste only
inside 3D mode. Outside that mode existing keys and native Ctrl+F are unchanged.

F3 pauses construction, retaining queued work and native partial blueprints.
RMB cancelling a preview does not cancel already committed jobs. Explicit native
Clear Building / existing cancel-paste clears unpaid jobs; completed paid blocks
and native partial world blueprints remain. Existing click/Alt-click priorities
work after leaving selection mode. Priority never bypasses missing support or
range requirements. The previous native schematic clipboard/tool is retained.

## Clipboard and transforms

`VolumeClipboard` introduces a session-only, version-1 bounded binary clipboard;
it is not a `.msch` or save format. It contains block names, footprint sizes,
relative X/Y/floor, rotation, an explicit supported configuration value and
internal links identified by clipboard indices. No source entity IDs, team IDs,
health, inventories, ammunition, production progress or live simulation modules
are encoded. Source capture requires completed friendly structures and rechecks
current membership. Whole multi-tile objects are normalized using their actual
footprints, even when their anchors lie outside the selection rectangle. The
lowest included anchor floor is the clipboard base.

Bounds: **1,024 entries**, **128 × 128 tiles**, existing configured Z range,
**2,048 internal links**, and **16 KiB total encoded payload**; whichever limit is
reached first applies. Oversized or empty selections fail without replacing the
world. Four integer quarter-turns restore the exact layout; doubled center
coordinates preserve even-size anchors. Directional blocks rotate; nonrotating
vanilla blocks keep their native orientation. Link indices remain stable as both
endpoint positions transform. Changing base Z shifts every floor together.

`VolumePasteTool` reuses the existing mouse resolver and cached native-texture
schematic ghosts. `LayerSchematicRenderer` reads the per-entry floor for volumetric
plans; ordinary single-floor stamps keep their previous path. The wire enclosure
uses the complete transformed footprint and physical height. Invalid entries are
red, valid entries green; any invalid entry prevents confirmation. Preview lists
and bounds are cached between validation updates; streaming batches are reused.

## Normal construction and authority

`VolumePlacement` checks the complete proposed arrangement in temporary grids.
It includes proposed Z0 wall foundations in structural evaluation, verifies
collision/terrain/discovery/range/host Free permission, support span/load budgets,
existing supported loads, link schemas and blocked inventory shafts. It also
finds an actually constructible support-first prefix order. This never mutates
live tiles, resources, IDs, queues or native building registries.

Confirmation is all-or-nothing **admission**, not an instant world transaction.
The existing `LayerPasteQueue` holds the multi-floor job. Ground cells use the
actual native unit BuildPlan queue and ConstructBuild resource/progress path;
upper cells use the existing timed PLACE validators and normal recipes. No
independent build engine or resource account was added. Native and upper work
must respect XYZ reach. Only ground plans belonging to volume jobs are temporarily
parked outside XYZ range and restored after native updating; unrelated plans are
retained. Cancel cannot resurrect a parked plan. F3 retains work, and earlier
priority scheduling/parked upper work remain in the same queue.

Initial invalid placement, support, pending-footprint conflict or server filter
rejection creates no partial job or world changes. After a valid job is admitted,
ordinary gameplay can change terrain/support/occupancy/resources. Such a job
waits and revalidates rather than building illegally or silently dropping its
remaining cells. Already completed paid structures remain, as in normal building.
It is not a rollback of legitimate construction if the world changes later.

An appended **VOLUME_PASTE** operation uses the existing authenticated reliable
construction envelope, epoch/sequence replay protection and payload bound. No new
packet or state channel is added. Admission is limited to two volume requests per
second per player, in addition to existing network limits and queue budgets.
The owner-private pending-plan decoder now accepts legal Z0 entries as well as
upper ones; ground platform metadata is still invalid. Client priority can select
those server-owned ground plans without inventing local buildings or resources.
Missing native queue replicas are restored from the authorized pending job.
Strict full payload identity rejects older/different builds before participation;
protocol 4 layout remains unchanged, but all peers must install this exact JAR.

## Configuration and internal connections

Supported scalar/content configurations use the block's native registered schema:
item/liquid filters, supported recipes, switches/doors and message text, for
example. Anonymous content classes are normalized like native Building.configured.
Same-floor ItemBridge/MassDriver Point2 links become index pairs. Internal power
node connections and existing explicit XYZ POWER/ITEMS/LIQUIDS/BRIDGE_ITEMS/
BRIDGE_LIQUIDS links are remapped to newly built destination handles. Conveyors,
conduits and ordinary power adjacency reconnect through their existing logic.

External links and unsupported configurations are cleared with a persistent copy
warning. Processor programs/byte arrays, memory arrays, canvas payloads, arbitrary
point/rally configurations and unsupported polymorphic values are not preserved.
No stale absolute-world coordinate or live ID is retained as configuration.
Existing native block support/unlock/environment/placement restrictions remain;
this feature does not make an otherwise unsupported upper block legal.

After construction, configuration/link application uses native Z0 configuration
or existing upper configure/link authority. Real connector recipes, team rules,
range, endpoint capacities and shaft restrictions apply. Successful links are
tracked so retries cannot duplicate charges or toggle native power connections
off. Connections/configurations blocked by later world changes or lacking
resources/range retry while the job remains active. Explicit cancellation can
stop this phase; completed buildings are retained. The server does not trust the
clipboard as evidence of resource ownership, completion or permission.

## Results and limits

See [test matrix](A2-09b-TEST-MATRIX.md), [owner procedure](A2-09b-MANUAL-TEST.md)
and [validation record](A2-09b-VALIDATION.json) for final program counts/hashes.
Native fixtures build a 17-entry three-floor factory through real native and
upper paths, check exact recipes, verify no inventory/health duplication, rotate
actual multi-tile pastes and verify copied power/bridge endpoints. Authenticated
network handlers are exercised with an in-memory provider, not physical peers.
A supplemental liquid/blocked-shaft fixture runs against the exact final JAR.

Known limits:

- Clipboard is session-only; world/team reset clears it. Queued upper work remains
  transient as before; completed structures use existing saves. No export/import,
  animated paper, mirroring or permanent clipboard persistence is implemented.
- The volume must fit the entry, footprint and byte limits. Large previews still
  incur synchronous dry-run work; measured fixture CPU costs are not FPS/GPU proof.
- Unsupported configs/external links are cleared; normal automatic adjacency may
  connect newly placed neighbors according to vanilla/mod rules.
- A structure needs a safe incremental build order, not merely final support.
  Dependent priority selection cannot force unsupported construction.
- If the world changes mid-build, fix the obstruction or explicitly cancel; the
  batch is not an instantaneous transaction that rolls completed structures back.
- Physical Windows/Linux preview/input acceptance and real two-computer hosting,
  construction and connection completion remain **NOT TESTED**. The common JAR
  preserves existing native input, Windows/Linux resources, licenses and notices.

Combat, flight physics, camera architecture, networking prediction/transport,
vanilla schematic format and save formats are unchanged.

Final automated results: **135/135 current programs on each runtime** (Java 17: 134 in the main build plus one exact-JAR supplement; Steam Java 25: all 135 against the JAR), zero failures. New feature fixtures: 117 native, 13 authenticated-handler and 9 supplemental liquid assertions. The 256-entry / 3619-byte preview averaged 10.933 ms on Java 17 and 11.956 ms on Steam Java 25 (three samples each); these are CPU fixture measurements, not FPS. The audit verified 961 existing classes, all 2,065 prior Alpha snapshot files and 27 previous diagnostic assets unchanged.
