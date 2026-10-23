# A2-16j — 3D core models and base infrastructure

Baseline: `c86f5d2` on `main`, preserving A2-16i-fix1 and the later turret correction. This is an unreleased diagnostic build; the owner's physical acceptance remains separate.

## Completed scope

All six vanilla cores now have separate, purpose-built mechanical silhouettes. Shard is a compact command module; Foundation adds reinforced tiers and square armor; Nucleus has a broad headquarters skirt and higher central structure. Erekir's Bastion, Citadel and Acropolis use crossed armor, progressively larger corner structures and angular command assemblies. They are not uniformly scaled copies. The coverage inventory records each model and the installed native dimensions.

The original body texture is mapped across roofs in footprint coordinates, so its scale and top identity survive changes in height. Original native team masks/precoloured variants follow those same surfaces with a small separate height, rather than a single floating full-sprite plane. Side materials come from an opaque host-atlas swatch. Existing solid-roof underpaint prevents alpha holes; per-view inward contact culling prevents floor overlap while retaining visible undersides below unsupported structures. Existing physical heights and picking/collision bounds are unchanged. Presentation culling includes the new models' complete envelopes.

Each core has restrained team-coloured status details. The small rotating energy indications are ambient decoration, not inventory, production or construction meters. Landing emphasis reads the real native `CoreBuild.thrusterTime`; no spawn, cost or launch is inferred from resources or performed by drawing. Original native unit/respawn effects and behavior are retained rather than replaced with a new synchronized effect system.

Radar receives a pedestal, bearing, rotating yoke and cached extrusion of its original dish sprite. Its angle comes from native `totalProgress × rotateSpeed`; readiness comes from native `smoothEfficiency` and enabled/live state. Rendering never advances these values or updates fog. The native additive glow is not painted as an opaque mask, which would obscure the dish's original details; a mild powered highlight and pedestal lamps provide the status cue in the existing opaque atlas pass. Illuminator's accepted model, configured colour and previous fixes are retained unchanged.

All models use the existing atlas stream, shared mechanical primitives, three distance LODs and immutable cached templates. There is no new rendering framework, shader, GPU effect pass, texture dependency or per-instance mesh generation. Native fields are read without gameplay callbacks.

## Rules and compatibility

No core storage, HP, costs, unit caps/types, item handling, spawning, campaign rules, physics, saves or networking protocol changed. The six cores are explicit render profiles; other/modded CoreBlocks retain the previous fallback. Ground rendering keeps the original fog gate. The layered path uses the same models only for existing supported instances and retains the original visibility gate.

Native tests exercise all six original core unit spawns, stack input/removal and renderer state invariance, plus real Shard → Foundation upgrade APIs with resources consumed once. The native core-zone gate remains in use. Existing upper-core inventory sharing, respawn, campaign, construction, selection/clipboard, combat and multiplayer regressions are retained.

Real elevated placement/copy fixtures in this pass target Illuminator and Radar on supported floors. Candidate upper-core rendering checks are inert adapter fixtures; they neither grant construction permissions nor certify that a core can be placed at that location. No native core restriction is bypassed or modified for testing.

## Geometry and performance

The ground renderer materializes seven cached types once, using 1,017,024 bytes of CPU templates, including original-art/team roof surfaces and Radar sprite volume. The 21 shared raw templates use another 906,432 bytes. Layer rendering has a separate material cache; these figures do not include it, temporary cold-generation allocations or existing shared GPU stream buffers. Geometry is reused across instances and frames.

| Model | Near template vertices | Mid | Far |
|---|---:|---:|---:|
| core-acropolis | 2,970 | 2,202 | 594 |
| core-bastion | 2,082 | 1,698 | 594 |
| core-citadel | 2,682 | 2,202 | 882 |
| core-foundation | 2,226 | 1,842 | 666 |
| core-nucleus | 2,538 | 2,058 | 666 |
| core-shard | 2,010 | 1,530 | 570 |
| radar | 870 | 534 | 366 |

Counts include the relevant model parts and original-art/team roofs; above-ground submission can omit the six inward bottom vertices. These templates contain triangles, not an independent mesh/draw call per instance. Resource bays use one opening per footprint cell per side and remain decorative.

Serial baseline/final measurements used the same stock Steam Java25 runtime, 960×600 test window and scripted cameras on this Windows/RTX4050 machine. No other test/compilation ran during the paired measurements. Each scenario had two 64-frame timing blocks after warmup; screenshots/readbacks were outside those blocks. CPU measures renderer work, not the entire simulation or OS presentation path. GPU samples use the existing asynchronous timer.

| Scene | CPU before → after (ms) | GPU before → after (ms) | Draw calls | Submitted vertices |
|---|---:|---:|---:|---:|
| Six nearby cores | 1.001 → 0.758 | 0.426 → 0.425 | 29 → 28 | 81,953 → 94,661 |
| Six target cores + 256-factory fixture | 1.395 → 1.496 | 0.551 → 0.580 | 44 → 43 | 124,559 → 132,695 |

The denser fixture costs approximately **+0.101 ms CPU / +0.029 ms GPU** in these short samples while submitting more detailed geometry. Nearby-core CPU block means vary substantially (baseline0.559–1.444 ms; final0.597–0.920 ms), so the lower overall average is not a claimed speedup. GPU cost there is effectively unchanged in this run. Native-atlas batching removes one old coloured-geometry draw call in both scenes. Visibility/LOD still applies; the dense fixture's entire population is not necessarily on screen. These are local rendering observations, not sustained FPS, remote multiplayer or Linux performance certification. Stock Java25 does not expose the optional per-thread allocation counter used by the fixture; no allocation-improvement claim is made.

## Validation and remaining limits

194 regression programs pass on both Java17 and stock Steam Java25. Each runtime additionally passes 27 real production-renderer OpenGL scenes / 3,456 frames, including all six cores, team colours, low/side/top views, native upgrade/damage/destruction/respawn calls, native landing timer, adjacency, openings, elevated/copy utilities, dense factories and actual save/reload. The focused native model test covers 2,016 poses and checks finite geometry/UVs, unchanged physical envelopes, state/inventory/registration invariance, culling, inward-contact handling and cache reuse. See the test matrix for evidence boundaries.

The fresh source compilation matches all 1,041 runtime classes. Actual Java fingerprint checks agree between JAR and folder payloads on Java17/25. Previous model families, input/network/physics classes, metadata, notices and dependencies remain intact; the artifact audit permits only render integration/new model classes plus the generated descriptive design manifest. All previous release/diagnostic files are preserved.

**Not certified:** physical Windows/Linux controls/gameplay, current campaign transitions with these visuals, or two-computer multiplayer. Only host/local Radar scanning is verified. Existing upper snapshots do not carry every native animation clock; remote upper Radar rotation/readiness and optional native core landing cues may be static/dim after replica creation. This pass deliberately adds no synchronization protocol or fictitious client gameplay state. A separate presentation follow-up may be needed after physical multiplayer testing.

Other upgrade pairs retain native code but were not separately charged through complete graphical upgrade sequences. Detailed native landing clouds/launch choreography are not rebuilt. The low-poly terrace treatment preserves original top art but is not a hand-sculpted replica. **Interplanetary Accelerator (7×7)** and remaining logic/base families are deferred; existing launch pads, shields, storage and other completed families remain unchanged.

No push, publication, Workshop update or previous-release overwrite was performed. The manual contains three owner checks; representative screenshots and sanitized automated evidence accompany the local test kit.
