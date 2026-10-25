# Mindustry³ / FPV — Alpha 2.0.0 RC1

Experimental, unofficial fan-made 3D/first-person conversion for **Mindustry 160.7**. This is not an official Mindustry release. Internal mod version: **0.19.0**. This checkpoint is a local release candidate, not a published update.

## Install

Install Mindustry separately. Download `mindustry-fpv-0.19.0.jar`, then use **Mods → Import Mod → Import File**, accept replacement and restart the game normally. Enable only one copy of Mindustry³. For the convenience ZIP, extract it and import the JAR inside. Do not import the corresponding-source ZIP or Workshop upload ZIP.

No separate Java installation, JDK, Python, command line or developer resources are required to play. Use the game's bundled runtime. Back up your saves before testing an Alpha. **Fullscreen is strongly recommended**: severe windowed-mode presentation stalls were reproduced on the owner's Windows hardware, while fullscreen removed them in that test.

Workshop subscribers should use only the subscribed copy after the owner publishes an approved update. GitHub, Workshop and Mod Browser delivery of this RC remain untested until publication and indexing. This repository update does not publish anything.

## Compatibility

- Primary validation host: Windows x64 standalone Mindustry 160.7, stock reduced Java 25.0.1, and full Java 17.
- Steam Java 25 startup/input fixes are retained; the stock standalone runtime provides the same reduced Java modules. This RC still requires a physical Steam import/control check.
- Linux desktop paths and SDL relative capture are implemented. The owner previously reported one Linux laptop launch; RC X11/Wayland capture and both Windows/Linux host directions require physical acceptance. Broad Linux certification is not claimed.
- Android, macOS, other Mindustry revisions and arbitrary third-party mod combinations are not accepted targets.
- Multiplayer is experimental. Every peer must use matching code/resources/version (protocol 4). JAR and compiled Workshop folder from this release share one canonical identity. A common protocol number alone does not make different builds compatible. No independent internet multiplayer acceptance is claimed.

## Features

3D terrain and FPV flight; first/third-person views; unit possession and command selection; procedural unit and block models; actual logic display screens; construction across supported XYZ floors with normal costs and support rules; volumetric selection and session-only multi-floor copy/paste; layered power/item/liquid connections; campaign save integration; natural relief, ores, water, vegetation and configurable horizon/cosmic scenery; Smooth, Blocky and Hybrid mountain presentation.

Normal combat, construction and resources remain authoritative. Client prediction changes local presentation, not ownership of damage, spending or construction. Cosmetic mountain styles do not introduce voxel climbing or digging.

## Default controls

Bindings are configurable in FPV/game settings; the Build panel documents contextual tools.

| Control | Action |
| --- | --- |
| F5 | Original 2D / FPV 3D |
| Tab | Build Mode / return to flight; releases cursor in Build |
| F7 | Cycle first/third-person views |
| Alt+F7 | Camera smoothing toggle (default OFF) |
| Alt+wheel | Third-person camera distance |
| F9 | Acro / Stabilized flight |
| WASD, mouse | Flight/ground movement and look, according to mode |
| Q/E | Descend/climb in Stabilized flight; stance height on walkers |
| W/S, A/D, mouse in Acro | Persistent throttle, yaw, pitch/roll respectively |
| Hold RMB in FPV | Mine aimed valid ore with a capable unit |
| F2 / F8 | Native eligible-unit boost / host-permitted Creator Flight |
| F10 / native command binding | Enter unit command selection |
| PageUp / PageDown in Build | Choose/lock construction floor |
| F3 | Pause/resume your construction without deleting plans |
| Click unfinished blueprint | Prioritize it when no placement tool is active |
| Alt+click blueprint | Prioritize while keeping a selected placement tool |
| Ctrl+F in Build | Existing single-floor schematic copy workflow |
| F4 in Build | 3D Selection: drag XY, wheel adjusts Z boundary |
| Ctrl+Shift+C / Ctrl+Shift+V in 3D mode | Copy selection / preview session clipboard |
| R / Shift+R in 3D paste | Quarter-turn rotation |
| LMB in paste / RMB / Esc | Queue valid construction / cancel preview / leave tool |
| Shift + drag in command mode | Native friendly-unit group selection |
| Ctrl+click in Build | Possess eligible friendly unit/turret |

Mouse capture releases for Build, command selection, menus, focus loss and leaving FPV. If native capture cannot be acquired, HOST MOUSE remains a bounded fallback; keyboard look is available. Report the backend and relevant log excerpt after removing personal paths, IP addresses and account details.

## Known limitations and release acceptance

Visual clipping, material approximation, distant LOD/background seams and incomplete custom-block support can remain. 3D clipboard contents are session-only; unsupported external configurations may be omitted with a warning. Fullscreen is the current workaround for the measured windowed presentation issue. Linux compositor support is still an acceptance gate. Long campaign sessions, physical mouse/focus behavior and actual two-laptop networking cannot be certified by automated fixtures.

See [release readiness](docs/alpha-v2/A2-18-RELEASE-READINESS.md), [test matrix](docs/alpha-v2/A2-18-TEST-MATRIX.md), [owner acceptance](docs/alpha-v2/A2-18-OWNER-ACCEPTANCE.md) and [changelog](docs/alpha-v2/A2-18-CHANGELOG.md). Earlier task reports retain historical results and pending labels; they do not certify this candidate.

## Build and test (developers only)

Use a separately installed official standalone 160.7 host and a complete JDK 17. `FPV_MINDUSTRY` selects its installation; `FPV_JDK` selects the compiler. Alternatively use ignored `local/dev-runtime.json`. The build verifies JNA 5.19.1 by SHA-256. Supply `lib/jna-5.19.1.jar` separately; it is bundled into player artifacts with its notices. The game remains compile-only and is never redistributed.

Run `python tools/build.py` from the source root. It refuses to overwrite an existing versioned JAR. `python tools/test-diagnostic-runtime.py --build build --jar dist/mindustry-fpv-0.19.0.jar --java bundled25 --run rc-stock25` runs the same compiled regressions against the stock runtime. `python tools/launch-standalone.py --run rc-owner --jar dist/mindustry-fpv-0.19.0.jar --interactive` creates a new disposable profile. See [distribution checklist](docs/alpha-v2/A2-18-DISTRIBUTION-CHECKLIST.md) for release-source verification and packaging.

The original standalone installation is treated as a read-only reference. Test data, recordings, profiles, binaries and SDKs are ignored. Neither Steam nor a Steam game process is required by the normal development workflow.

## License

GPL-3.0-only; see [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Mindustry/Arc textures and runtime are supplied by the separately installed host. JNA/libffi notices are included. Corresponding source accompanies each binary; use the manifest matching its exact SHA-256. The source snapshot is not an installable mod.
