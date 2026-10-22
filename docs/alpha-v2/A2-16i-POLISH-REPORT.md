# A2-16i follow-up — Shield cap, activity globe and utility models

Baseline: owner-accepted `121a47055591b4a163694c013841de433f33badf`, `main`.
Local diagnostic only. Prior releases/diagnostics and player profiles are preserved.

## Confirmed correction

The Force Projector column roof and the native top-art quad were both at exactly
`base + .85 * height`, causing depth fighting. Only this cap's art receives an
explicit .008-tile separation. Body dimensions and protective physics are unchanged.

A small rotating geodesic globe above the emitter indicates an active field.
Its cached dual-icosphere has thirty hexagonal and twelve pentagonal panels;
pentagons close the spherical topology. It follows the actual XYZ building base,
team color, authoritative active volume/radius and actual hit flash. It disappears
when the field is broken or the generator is disabled. Fine grid geometry is used
nearby; a small coarse globe is used farther away, fading out at48 tiles.

This globe is a **local emitter hologram**, not a new spherical protective area.
The existing finite polygonal protective prism, projectile contacts, shield health,
cooling, recharge and network snapshots remain unchanged. Opaque geometry occludes
the globe and fields. The existing depth-tested additive field pass renders both,
with no depth writes and full graphics-state restoration.

## Updated model coverage

| Native block | Size / physical height, tiles | Dedicated presentation |
| --- | --- | --- |
| shock-mine | 1 / .22 | Low armored contact disc, four electrodes, native art and lightning palette |
| illuminator | 1 / .88 | Foot, mast, framed lamp and native configurable RGB panels; efficiency/enable driven |
| launch-pad | 3 /1.64 | Loading arches, original deck art, raised launch gantry and charge status |
| advanced-launch-pad | 4 /2.02 | Larger native counterpart using the same cached launch profile and its own atlas art |
| landing-pad | 4 /2.02 | Receiving deck, corner beacons, different low guide-rail silhouette and native item color |

The four requested block types cover five installed registry entries because160.7
contains both Launch Pad variants. Only these exact identities use the new shared
utility profiles. Existing support, storage, power, factory, liquid, logistics,
drill, turret and unit models retain their previous rendering paths. Optional
editor-only Erekir barrier projectors remain deferred from the previous task.

Immutable normalized meshes, original atlas materials, existing resource-port
geometry and the existing18/50-tile model LOD are reused. Near/far mechanisms are
cached by profile, footprint and LOD; atlas copies are cached per native block and
renderer owner. No per-frame mesh generation or external assets are introduced.
Ground and upper-floor geometry fit the existing authoritative envelopes.
The mine has no invented activation pulse; existing real lightning remains native.
The lamp shows actual configured color/efficiency; no world-lighting rewrite is made.

## Shipment animation and evidence

Launch pulses travel upward; landing pulses descend from above toward the deck.
They are visual laser pulses only, never projectiles or a second transport system.
They cannot deliver, duplicate, consume or damage anything. Their visible segment
is depth-tested and culled independently from the pad body, including when the
moving pulse lies above the body frustum.

Ground launch evidence is a real native LaunchPayload with cargo, matching team,
position and lifetime. A native counter-reset transition can associate a new payload with an already
observed pad. The last item can enter and launch between render frames; no full
previous inventory snapshot is required. Merely emptying inventory or resetting
a charge counter produces no effect. Payload removal/repositioning immediately
invalidates the old source effect. Ordinary idle frames do not scan Groups.all;
the payload lookup is lazy on first observation or a native launch-counter reset.

Upper launch beams read the already existing, saved/replicated native adapter's
pod timer, which starts only after the native export removes actual resources.
Landing beams read the actual arriving item and arrivingTimer. Paused native
clocks freeze shipment motion. Enabled/power changes do not retrospectively cancel
an already dispatched shipment. World/source changes clear presentation caches.

Native payloads have no source-floor field. When joining/re-entering3D after a
launch with ground and upper Launch Pads at identical XY, the ground pad does not
adopt an ambiguous existing payload. A subsequently observed real launch can be
shown normally. This conservative omission avoids animating the wrong floor.
Remote ground payload availability/very short missed transitions can likewise
omit a transient effect; no new packet or guessed shipment is introduced. Real
two-computer animation acceptance remains pending. Upper timer replication and
all existing multiplayer authority/protocols remain unchanged.

## Validation scope

Native tests exercise actual resource removal into launch cargo, native upper
launch clocks, actual landing completion and exact item receipt. They also reject
counter-only/inventory-only false launches and moved/removed payloads. Model tests
cover installed sizes, all LODs/rotations, Z0/Z1/Z3/Z4/Z5, valid atlas UVs, native
color/efficiency, read-only rendering and cache reuse. Authorized construction
fixtures build forty source blocks, queue forty copies across Z1–Z5, preserve
source inventory and leave copied inventories empty; save/replication roundtrip
retains all eighty buildings.

The Windows SDL gallery uses a disposable profile/map and real native shipment
update methods outside timed rendering, with fixture resources/efficiency supplied
explicitly. Frozen shipment scenes are real native states after a specified number
of updates; the motion scene advances native clocks between frames. This proves
resource/event presentation in the fixture, not campaign throughput or owner input
acceptance. Separate real shield-contact, depleted/disabled/recovery, low/top/below,
upper-floor, ghost, clipboard and disk SaveIO scenes are included.

No gameplay, collision, inventory, campaign ledger, save format, camera/input or
network implementation is modified. No new keybindings. Java17 and stock Steam25
are validated separately. Physical Linux, owner input/visual acceptance, sustained
gameplay FPS and real remote multiplayer remain NOT TESTED in this pass.

Rebuild: set FPV_JDK to a Java17 JDK and FPV_MINDUSTRY to a separately installed160.7;
run `python tools/build-diagnostics.py --task a2-16i-polish --run <new-name>`.

## Final measured results

**190 regression programs pass against the packaged JAR on Java17 and the same 190 on stock Steam Java25.** Independent compilation verifies 1032 runtime classes and packaged resources byte-for-byte. Existing campaign accounting, network/prediction, input/cancellation/priority, construction/copying, saves, combat and previous model regressions pass. No optional dependencies changed.

Initial dense-field measurements identified subpixel loading arches retained in far LOD. Final far templates remove those details (72 vertices per pad) while preserving nearby models; the regression test now caps every far model below 150 vertices. The pre-LOD sample is retained in validation JSON.

The actual Windows SDL/OpenGL fixture completes **40 scenes /5,120 frames per final runtime**, plus 5,120 baseline frames, with zero GL errors. Each run observes 70 real native cargo payload creations across independently reset shipment scenes, 181 cumulative approved upper placements, one actual authoritative shield hit and a disposable disk SaveIO roundtrip. The final runtimes each pass 40 direct transfer-pass GL-state restoration probes. Disabled/depleted spheres are absent, depletion has no field faces, both transfer directions are visible in their close views and no beam remains after completion. Model states/resources are explicit disposable fixtures, not a real campaign throughput benchmark.

| Scene | CPU before → after, ms | GPU before → after, ms | Draws before → after | Vertices before → after |
| --- | --- | --- | --- | --- |
| mine | 0.346 → 0.443 | 0.340 → 0.289 | 29 → 29 | 62033 → 63419 |
| light | 0.327 → 0.499 | 0.364 → 0.371 | 35 → 34 | 73481 → 79403 |
| launch | 0.387 → 0.609 | 0.360 → 0.292 | 41 → 40 | 79325 → 93245 |
| advanced-launch | 0.403 → 0.685 | 0.360 → 0.375 | 45 → 44 | 90149 → 110621 |
| landing | 0.401 → 0.665 | 0.389 → 0.368 | 46 → 45 | 94757 → 114509 |
| send-close | 0.371 → 0.739 | 0.670 → 0.472 | 44 → 44 | 90143 → 110915 |
| receive-close | 0.372 → 0.803 | 0.750 → 0.524 | 47 → 47 | 99557 → 120641 |
| transfer-motion | 0.428 → 0.727 | 0.563 → 0.437 | 48 → 48 | 106085 → 122327 |
| stacked | 0.329 → 0.780 | 0.883 → 0.593 | 48 → 48 | 102449 → 122807 |
| field100 | 0.777 → 1.368 | 0.760 → 3.294 | 33 → 33 | 105683 → 141599 |
| field1024 | 1.336 → 1.846 | 0.845 → 2.371 | 66 → 66 | 208571 → 225203 |
| force-close | 0.462 → 0.537 | 0.097 → 0.895 | 29 → 29 | 65765 → 70085 |
| shield-overlap | 0.468 → 0.411 | 0.419 → 0.482 | 36 → 36 | 91811 → 92531 |

Measurements include the whole renderer/terrain and actual view culling; they are single samples, not an FPS improvement or statistical parity claim. Detailed allocations from Java17 are retained in JSON; Steam25 has no allocation counter. Fifteen shared profile/footprint/LOD templates use **314,112 vertex-data bytes**. The one shared globe uses **28,800 bytes**. Each warmed model owner caches five native types, **9,816 vertices /314,112 bytes** across all LODs; both runtimes agree. Ground/upper owners share immutable templates but have separate atlas material copies. Stream/native/driver buffers are additional. No stable-state model rebuild occurs in the gallery.

JAR SHA-256: `7841b0885a65ea15c784428854d377bf802dd0e3e8ccb6cd10890247ebf0da08`. Strict folder/JAR fingerprints agree on both runtimes. The artifact audit verifies 1,132 pre-existing class entries byte-identical; changes are limited to the four scoped rendering integration/visual sources and new utility/effect classes. ForceProjectorSystem, campaign/transfer accounting, input, networking, collision, saves, dependency and license bytes are unchanged. Hash checks preserve 2,065 Alpha files and 111 earlier diagnostic artifacts, including all accepted A2-16i assets. Corresponding source is exported from the final local checkpoint.

The unchanged-model-only image differs by 421 pixels, bounds (166, 289, 542, 310). Physical owner acceptance, physical Linux, remote multiplayer effect delivery and sustained gameplay FPS remain NOT TESTED.

The retained-model comparison does not pin Mend/Overdrive initial charge. Installed160.7 constructors initialize charge with Mathf.random(reload), so their mechanical ring phases can differ between processes. This explains a plausible source of the small pixel difference; exact pixel parity is not claimed. Prior geometry implementations and native gameplay classes are audited separately.
