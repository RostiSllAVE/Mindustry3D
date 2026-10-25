# Mindustry³ Alpha 2.0.0 RC1 [v160.7]

Internal mod version 0.19.0 · proposed tag v2.0.0-rc.1 · local candidate only.
Experimental unofficial fan-made conversion; requires separately installed Mindustry 160.7. Android unsupported. Windows is the primary validation platform; Linux desktop acceptance and real two-computer multiplayer remain pending for this RC.

## Included Alpha v2 work

- Multiplayer diagnostics, safer hosting/joining/reconnect handling, compact movement snapshots and local presentation prediction with server authority retained.
- Linux SDL relative-input integration and retained Windows Java 17/JNA and reduced Java 25/FFM paths.
- Flying AI attack-solution corrections; construction pause/resume and direct blueprint priority selection.
- Multi-floor XYZ selection, copy/paste queues with normal costs, support and server validation.
- Distinct natural/artificial terrain, visible natural surface relief and ores, layered liquid effects, vegetation and configurable biome/cosmic scenery.
- Dedicated procedural block families: drills, logistics, manufacturing, power/liquids, support/shields/storage, cores, factories/reconstructors, payload/utilities and working logic displays.
- Corrected turret presentation, aircraft exhaust and healing beams; animated placement-direction arrows.
- Smooth/Blocky/Hybrid mountain styles with consistent canonical gameplay geometry.

The latest gameplay checkpoint, including the repair beam/arrow fix, was physically accepted by the owner before this release preparation. This task adds no gameplay feature or optimization: all 1,081 FPV runtime classes match that checkpoint. Version metadata, documentation, local packaging and validation tools were updated. Persistent saves and protocol 4 are unchanged; peers still need the exact matching payload, not merely a common protocol number.

## Release-preparation corrections

Removed Alpha 1.0.4 hardcoding from packaging; explicit release title/tag and current notes now accompany the package. Refreshed player instructions/controls. Added normal-startup/native-import/folder checks with isolated standalone profiles, a seven-scene comparison fixture and read-only artifact/source verifier. The regression runner now accepts a production JAR and creates its temporary directory before campaign tests. Native import's legitimate `.zip` normalization is recognized by the test observer.

## Known limitations

Fullscreen is recommended for the owner's previously reproduced severe Windows windowed-mode stalls. Cosmetic material/LOD/background/model imperfections and incomplete third-party content coverage remain. 3D clipboard data is session-only; unsupported external configuration can be omitted with warnings. No broad Linux/compositor or internet multiplayer certification. Full historical all-system network automation exceeds its diagnostic channel allowance; a narrower real-server/two-client naval/identity test passes. No diagnostic channel limit or gameplay protocol was relaxed.

Alpha v3 UI redesign, blueprint-paper presentation, shader/filter presets, seasonal themes and Alpha v4 FPV extensions remain deferred. See A2-18-RELEASE-READINESS and A2-18-OWNER-ACCEPTANCE before any publication.
