# A2-15c — test matrix

PASS means the named automated check, not owner gameplay acceptance.

| Check | Result | Evidence / limit |
|---|---|---|
| Java17 regressions | PASS — 163 programs | Initial run + documented resume after obsolete preset assertion; final runtime correspondence checked |
| Stock Steam Java25 regressions | PASS — 163, zero failures | Complete run against packaged final JAR; java.logging unavailable |
| Native floor whitelist | PASS | All 107 base floor definitions; only Space/Empty; original solidity/placeability |
| Mask boundaries, isolated/mixed/large regions | PASS | Exact row runs, deterministic arrays, zero solid coverage, exact chunk-edge coordinates |
| Real Steam25 SDL/OpenGL render | PASS — 1152 frames /12 views | Mixed top/oblique/low, all Space, Empty, Space/Void sky, Custom mixed, night rotation, OFF, reload, underside; GL errors0 |
| Real Java17 SDL/OpenGL render | PASS — 1152 frames /12 views | Same final JAR, native map/entity counts unchanged; not performance comparison |
| Z3 platform / buildings / unit occlusion | PASS scripted + inspected PNGs | Free-mode fixture only; physical moving-unit/control acceptance NOT TESTED |
| Cached geometry while rotating | PASS scripted | Previously observed mask entries retain identity; new visible chunks may be generated once |
| Real SaveIO reload | PASS scripted | Disposable generated world; native map/entities stable |
| Campaign/world transitions | PASS native regression fixtures | Physical campaign rendering NOT TESTED; no save format changed |
| F5/Tab/priority/F3/3D clipboard/mouse paths | PASS existing native regression fixtures | Physical UI and capture acceptance NOT TESTED in this pass |
| Multiplayer identity and state safety | PASS automated | Actual JAR/folder fingerprint equal in17/25; unchanged protocol/state classes, existing fixtures |
| Two-computer multiplayer | NOT TESTED | No physical remote acceptance claim |
| Linux physical renderer / input | NOT TESTED | Shared Java17 bytecode/shaders, no new natives; same JAR is provided |
| Unsupported-driver fallback | NOT TESTED physically | Shader compilation guarded; OFF tested in actual renderer |
| Primary saves / old artifacts | PASS audit | No primary profile used;2065 Alpha files +69 diagnostic assets matched old hashes |
| GPL/source/JAR correspondence | PASS |956 freshly compiled runtime classes byte-identical, resources/notices match |

## Same-host before / after

Sequential baseline A2-15b.2 versus final JAR, same960×600 viewport,96×64 disposable
map and camera scenarios.32 warmup frames then64 CPU submissions;63 asynchronous GPU
results per view. CPU is render submission time, GPU is the renderer query interval,
neither is complete game frame time. NoFPS improvement claim, no weaker-GPU certification.

| View | CPU before→after, ms | GPU before→after, ms |
|---|---:|---:|
| mixed-top | 0.713 → 0.789 | 0.067 → 0.080 |
| mixed-oblique | 0.636 → 0.640 | 0.057 → 0.075 |
| mixed-low | 0.561 → 0.545 | 0.052 → 0.063 |
| all-space | 0.516 → 0.535 | 0.053 → 0.085 |
| empty-floor | 0.453 → 0.511 | 0.053 → 0.085 |
| space-sky | 0.354 → 0.430 | 0.055 → 0.086 |
| void-sky | 0.422 → 0.410 | 0.054 → 0.085 |
| custom-mixed | 0.342 → 0.404 | 0.055 → 0.086 |
| night | 0.417 → 0.442 | 0.054 → 0.084 |
| fallback | 0.409 → 0.396 | 0.053 → 0.073 |
| reload | 0.450 → 0.460 | 0.053 → 0.086 |
| underside | 0.316 → 0.393 | 0.049 → 0.054 |

## Added geometry / cold allocation measurement

The old build had no cosmic-mask meshes. Native floor geometry is still retained as
fallback; the new cached mask is additive. A full32×32 cosmic chunk uses192 vertices,
64 triangles,2304 GPU vertex bytes and one draw (not one per tile/star). A one-cell
checkerboard uses3072 vertices,1024 triangles,36864 bytes and one draw. Empty mask:
zero GPU vertices/draws. External cosmic regions reuse the existing horizon batch;
previously omitted fully cosmic triangles are now retained. No new texture/material
atlas or star mesh. The mask cache is bounded32MiB plus up to4096 metadata entries.

Cold CPU probe:500 warmups +2000 builds per material on fullJDK17, after other runs.
These allocations occur on cache construction/invalidation, not every frame:

```text
COSMIC,solid,coldChunkMeanUs=13.601600000000001,allocatedBytes=73760,vertices=0,triangles=0,meshBytes=0,draws=0
COSMIC,space,coldChunkMeanUs=13.807649999999999,allocatedBytes=78880,vertices=192,triangles=64,meshBytes=2304,draws=1
COSMIC,mixed,coldChunkMeanUs=9.729,allocatedBytes=78880,vertices=192,triangles=64,meshBytes=2304,draws=1
COSMIC,checker,coldChunkMeanUs=21.16385,allocatedBytes=155680,vertices=3072,triangles=1024,meshBytes=36864,draws=1
```

Measurements are single-host samples, not statistical certification. The CPU probe is
reproducible from `tools/benchmarks/CosmicFloorGeometryBenchmark.java`. Full sanitized
results and matching shader-fixture manifests are in the local test kit. Earlier failed
candidates and the lab-inspector error remain local; none is the distributed test JAR.
