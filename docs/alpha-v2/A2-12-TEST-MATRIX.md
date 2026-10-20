# A2-12 — Test matrix

Baseline: latest accepted A2-11 visibility checkpoint `8825029`. Final JAR SHA-256 `d81b0ce2f16e7a145c1da5d06efc19e266a115589d1e0949bbfc45203a73b3fb`. PASS below identifies the actual test boundary; it does not imply physical owner acceptance.

| Check | Result | Evidence / limits |
| --- | --- | --- |
| Full release17 compilation and regressions | PASS,141 programs | Windows complete Java17; assertions enabled |
| Final JAR on stock Steam Java25.0.1 | PASS,141 programs | No java.logging; native-access flag supplied by test runner |
| Floor relief bounds, determinism, continuity, materials | PASS,6,064 native checks | Actual CPU meshes/UVs, corner/centre samples, seams, foundations, old-contrast normalization |
| Ore contours, roofs, skirts, UVs and cache | PASS,301 native checks | Real Arc pixmap/native content with bounded synthetic alpha fixture; planar containment, size limits, repeat byte-identical geometry |
| All nine native floor ores | PASS automatic | Explicit native identities; all27 real variants extracted in actual GL gallery |
| Unknown/modded ore and non-ore overlays | PASS automatic | Unknown OreBlock retains original overlay; wall ores/vents/props not guessed as deposits |
| Dense/isolated fields and chunk-edge deposits | PASS automatic | Real native meshes; full128² CPU probe plus actual gallery |
| Solid/liquid/material/building/cliff transitions | PASS scripted graphics + native tests | Native atlas, factory/drill/conveyors, water, natural/artificial walls; owner seam judgement pending |
| Original terrain/map/mining state unaffected by rendering | PASS automatic | Physical height/fingerprint, mining state/speed/timer/inventory/drop/drill-time assertions |
| Native drill resource production | PASS automatic | Mechanical drill produces actual copper after native update ticks; no resource grant |
| Player and AI mining progress | PASS scripted host game | Gamma and Mono copper timers advance, correct ore target; not physical mouse interaction |
| Campaign generation/save/load/transitions | PASS,43 native checks | Disposable actual SaveIO, original tile/overlay fingerprints, relief reproduction, Serpulo/Erekir and custom transitions |
| Z0/upper construction, F3 and priority | PASS existing regression fixtures | Native plans/costs and in-memory authority; no physical key acceptance |
| 3D selection/copy/paste | PASS existing regression fixtures | World and authority behavior unchanged; no new schematic format |
| Camera/input/FPV switching/multiplayer safety | PASS existing regression fixtures + unchanged classes | Runtime byte comparison; not a new two-computer or physical-input acceptance |
| Actual Windows Steam25 graphics | PASS before/after,GL error0 | Two95-frame production-renderer runs, identical lab entry/cameras/native atlas; unedited low/top captures |
| CPU mesh generation/memory/batches | MEASURED | Same compiled probe before/after,128² none/sparse/dense maps; MockGL/native buffers, not FPS |
| Actual GPU/CPU submission sample | MEASURED | Asynchronous queries and short capped-window fixture; not broad performance certification |
| Java17 and Steam25 final JAR/folder fingerprint | PASS | Strict payload identity unchanged by packaging |
| Final source/binary correspondence and old artifacts | PASS | Matching source manifest/checksums;993 unrelated class files identical; prior releases preserved |
| Physical Windows gameplay/input/visual acceptance | NOT TESTED | Owner follows A2-12-MANUAL-TEST.md |
| Physical Linux, weaker laptop performance | NOT TESTED | Same intended cross-platform JAR; no Linux-specific changes/dependencies |
| Independent two-computer visual comparison | NOT TESTED | No new packets; deterministic source and network regressions are not physical acceptance |

Cold template extraction and large-map cache behavior are bounded but not a broad hardware benchmark. Dense fields measurably increase mesh memory/rebuild time. This is reported rather than described as a performance improvement.
