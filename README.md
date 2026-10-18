# Mindustry³ / Mindustry 3D FPV — Alpha 1.0.4 Mod Browser candidate

(Hello! before you read that repository i want to let you know that that project 99% vibe-coded but tested by human, i mean by me😁. That is actually my first project and i am new on GitHub as creator. In a few days i will publish an YouTube channel where i show how that mod works. Hope you will have fun with this mod!)

Mod on YouTube: https://youtu.be/lN1eUTf-Mys

An **experimental, unofficial fan-made mod** that presents Mindustry in 3D,
adds FPV flight and spatial combat, and extends construction across elevated
floors. This is not an official Mindustry release or a standalone game.

**Prepared locally: Alpha 1.0.4 / mod 0.18.31**, targeting Windows 64-bit and
Mindustry 160.7. This distribution pass is not published by its preparation tools.
It includes direct JAR installation, a convenience ZIP, and a compiled Workshop
folder with matching code/resource identity. See [Mod Browser readiness and final release steps](docs/releases/ALPHA-1.0.4-BROWSER.md)
and [simple installation instructions](docs/player/INSTALL.txt).

Unreleased development includes [A2-01 diagnostics](docs/alpha-v2/A2-01-MULTIPLAYER-DIAGNOSTICS.md)
and [A2-02 connection lifecycle fixes](docs/alpha-v2/A2-02-CONNECTION-REPORT.md).
Automated readiness/reconnection checks are separate from physical multiplayer
acceptance, which is still pending. Follow the [two-device procedure](docs/alpha-v2/A2-02-MANUAL-TEST.md)
with the exact same diagnostic JAR on every participant. These local test builds
are separate from the preserved Alpha 1.0.4 player artifacts.

[A2-03 campaign audit](docs/alpha-v2/A2-03-CAMPAIGN-REPORT.md) adds full native
sector/save/transition regression tests. No campaign gameplay fix was justified
by the available reproduction. Physical campaign UI/control acceptance remains
pending; see the [three-step owner check](docs/alpha-v2/A2-03-MANUAL-TEST.md).

Liftoff inspired the FPV concept; its code/assets are not included, it is not
required, and this is not a validated Melty two-game mashup. Historical published
Alpha 1.0.0–1.0.3 artifacts are preserved.

Screenshots of units and blocks:
<img width="2559" height="1438" alt="Screenshot 2026-10-12 153603" src="https://github.com/user-attachments/assets/749e0b75-2755-4255-9a84-558c8c6b76b0" />
<img width="2559" height="1438" alt="Screenshot 2026-10-12 153619" src="https://github.com/user-attachments/assets/12860a82-85ac-4670-8a8b-5698bb56bdc1" />
<img width="2096" height="1131" alt="Screenshot 2026-10-12 153630" src="https://github.com/user-attachments/assets/55b1499c-bf72-4fb0-80fc-6e24b27295fc" />
<img width="2559" height="1439" alt="Screenshot 2026-10-12 153708" src="https://github.com/user-attachments/assets/83502b87-9c29-4251-a6c2-3cec2dd74133" />
<img width="2559" height="1438" alt="Screenshot 2026-10-10 221302" src="https://github.com/user-attachments/assets/d6e88f74-8123-4914-a27e-8893474740e4" />
<img width="1296" height="986" alt="Screenshot 2026-10-11 183228" src="https://github.com/user-attachments/assets/584fef0f-7781-4fb7-ad24-8fb68cdaaaed" />
<img width="1623" height="1143" alt="Screenshot 2026-10-12 033826" src="https://github.com/user-attachments/assets/6a46c436-83ea-49cc-86f0-14134d73203c" />
<img width="975" height="912" alt="Screenshot 2026-10-11 214423" src="https://github.com/user-attachments/assets/cfba6d7d-cfa7-4c07-8d6b-fe38f8b10bed" />
<img width="644" height="628" alt="Screenshot 2026-10-10 002310" src="https://github.com/user-attachments/assets/a4a603a8-01b2-4bfc-b422-be05ec4f8ede" />
<img width="657" height="588" alt="Screenshot 2026-10-10 002330" src="https://github.com/user-attachments/assets/9200de68-d082-4612-b17a-21f4228d2c71" />

Gameplay:
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/8eab8167-c90a-42bc-932b-940f30dda5ba" />
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/b69577ca-817f-4288-a162-744b45ede15f" />
<img width="2559" height="1438" alt="image" src="https://github.com/user-attachments/assets/26d28a29-7eb7-4ce0-aac9-ad6fdbcc3833" />
<img width="2559" height="1438" alt="image" src="https://github.com/user-attachments/assets/19c7371c-2ebb-4fde-8727-d598fcab6010" />
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/ae4273b6-ac09-4766-98ec-f8627b8cb2c3" />
<img width="1119" height="637" alt="image" src="https://github.com/user-attachments/assets/38d28bca-9ae0-470b-969c-f242f7dab501" />
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/25938db9-d63b-43d4-8d26-7f26628f09e9" />
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/52a22511-fff0-452e-91a0-4455287e8242" />
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/4aaed243-bbc3-4352-93cd-7684638aca26" />
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/a3d76fb6-e36b-4b01-a1e6-bdce364c25d6" />
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/a1a68326-d01f-4a22-be26-1b364d8d07a5" />

## Features

- Existing Mindustry maps, campaign, resources, production, waves, research and
  units, integrated with the mod's spatial extensions.
- Natural-wall mountains, textured terrain, water, distant scenery and day/night sky.
- Procedural models from the installed game's original sprite silhouettes;
  animated weapons, mechs, walkers, aircraft and ships.
- First-person, front observer and rear-follow cameras; adjustable distance,
  FOV, camera tilt and optional smoothing.
- Acro/Stabilized flight, XYZ altitude, collision, projectiles, lasers and shields.
- Build Mode with adjustable world speed, surface-aware placement, elevated
  floors, structural support/collapse, single-layer copy/paste and cross-floor
  logistics/power connections.
- Native mining with 3D feedback, unit commands and eligible unit/turret possession.

Coverage is experimental. Unsupported custom content can use visual fallbacks;
not every native/custom rendering or combat pattern is converted identically.

## Requirements

The verified setup is **64-bit Windows desktop, Mindustry 160.7**, with an
OpenGL-capable GPU. Its standard Steam bundled **Java 25.0.1** can load this hotfix
directly; installing a separate JDK or running PowerShell is not required.
This distribution targets 160.7; other versions/platforms are not certified here.
When a complete Java runtime provides `java.logging`, the existing Win32/JNA
mouse bridge remains available. A full JDK 17 is needed only to build from source.

**Fullscreen is strongly recommended.** Android, Linux/macOS, hardware FPV
controllers and independent dedicated-server packages are unverified.
Bundled JNA native libraries do not imply support for every platform. The owner
reports a successful launch on a separate Linux laptop; broader Linux
compatibility is not certified, and this does not establish Android support.

## Installation — native Mindustry Import Mod

1. Back up saves and download **`mindustry-fpv-0.18.31.jar`**.
2. Launch Mindustry normally through Steam. Open **Mods → Import Mod → Import File**
   and select that JAR. Accept replacement of the previous Mindustry FPV version
   if updating; do not keep duplicate copies enabled.
3. Close/restart the game as requested, then launch it normally through Steam again.
4. **Enable Fullscreen in native graphics settings**. Start a map/save and press
   **F5** to switch 2D ↔ FPV/3D. **Tab** opens the existing Build Mode.

Do not import the source ZIP or whole repository as a mod. The JAR embeds GPL,
JNA/libffi license texts and third-party notices. Matching source is distributed
separately as `mindustry-fpv-0.18.31-corresponding-source.zip`.

The older `Play-*`, `Launch-Alpha.ps1` and profiling scripts retain their historical
paths/version requirements for Alpha 1.0.0. They are not needed for this hotfix's
native installation. Do not run the old `prepare-alpha.py` in the history-free
public repository to recreate historical artifacts.

### Mouse capture

Stock 64-bit Windows Steam Java 25 uses the java.base native binding for hidden,
confined/recentered FPV mouse input. Full Java 17 retains Win32/JNA input. Build
Mode, command mode, menus, pause and focus loss release capture; returning to
active flight restores it. Windows pointer acceleration/DPI can affect sensitivity.

If native access is unavailable, the HUD explicitly says **HOST MOUSE**. That
fallback remains visible and bounded by window edges; arrow keys keep aiming.
Native input failure does not abort the rest of the mod. See the distribution
report for the exact physical tests completed for this candidate.

## Controls (defaults)

| Input | Action |
| --- | --- |
| F5 | Toggle original 2D presentation / FPV 3D |
| Tab | Enter/leave Build Mode; release/capture mouse |
| F7 | First person → front observer → rear follow |
| Alt + F7 | Camera smoothing ON/OFF (default OFF; saved) |
| Alt + wheel | Third-person camera distance |
| F9 | Acro / Stabilized |
| W / S in Acro | Increase/decrease persistent throttle |
| A / D in Acro | Yaw |
| Mouse in Acro | Pitch / roll; arrow keys are alternatives |
| WASD in Stabilized / ground control | Move relative to horizontal viewing direction |
| Q / E in Stabilized flight | Descend / climb |
| Left mouse | Fire through real unit weapon rules |
| Hold right mouse in FPV | Mine aimed valid ore with a capable unit |
| F2 | Native boost / landing for eligible Nova, Pulsar, Quasar, Vela |
| F8 | Creator Flight for ground units, if the host permits it |
| Q / E on walkers | Lower/raise stance; not flight |
| Ctrl + click in Build Mode | Possess eligible friendly unit / native controllable turret |
| F10 / native command binding (usually Shift) | Command/selection mode |
| Shift + drag in command mode | Select friendly units; native movement/attack commands |
| Ctrl + F in Build Mode | Single-floor area copy; paste on valid target floor |
| PageUp / PageDown in Build Mode | Choose/lock higher/lower construction floor |
| WASD, Q / E in flying Build Mode | Precision horizontal / vertical movement |
| Home / End in Build Mode | Turn heading |
| Right mouse / Esc in Build Mode | Cancel placement/linking; native dismantling binding applies when appropriate |
| F6 | Single-player pose reset; server controls multiplayer position |

Native Controls settings remain authoritative for remappable bindings,
dismantling and possession. F10 avoids a Shift/boost conflict on boost-capable
units. The front observer view **does not reverse** the firing direction.

The left Build panel exposes automatic surface selection, floor locking,
platforms, structural/host-authorized Free placement, inventory transfers and
connections. **Build movement speed**, **Flight speed** and **World speed** are
separate settings. Camera tilt/FOV and visual toggles remain in FPV settings.

## Multiplayer — experimental

Two local clients have worked with matching mod/server authority.
**Independent multiplayer testing is still pending.** Some broader local
regression runs stalled during creator landing / elevated turret control.
Every client **and server** needs the exact same tested JAR; rebuilt JARs with
the same version label can have different fingerprints. Arbitrary vanilla
servers are unsupported. Stock headless-server packages are not certified here.

## Known issues

- **Windowed performance:** on the development Windows/NVIDIA laptop, 2D and
  near-vertical/edge views could display only 2–7 FPS despite ~240 submitted
  frames/s. Fullscreen eliminated the player's reproduced stalls; recorded
  post-load segments were ~142–240 displayed FPS. VSync alone did not fix it.
  This is a workaround on one system, not a universal FPS guarantee.
  [Measured investigation](docs/performance/camera-profiling.json).
- Procedural model clipping, stepped silhouettes, incomplete special
  effects/animations and generic content fallbacks remain possible.
- A broad smoke fixture still fails a Flare fixed-gun firing assertion at +85°
  pitch. Practical scope is unresolved; narrower camera/combat checks passed.
- Copying supports **one layer at a time**, not full volumetric schematics.
  Support and occupancy validation still apply.
- Saves may depend on this exact mod version. Back up before importing campaign
  progress, removing/updating the mod or testing multiplayer. Elevated buildings
  are not preserved automatically when removing the mod.
- Long campaign playthroughs, every content combination, controller calibration
  and remote multiplayer have not received complete acceptance testing.

Report bugs with game/mod versions, mode, reproduction steps and a short relevant
log excerpt. Remove usernames, IPs, private paths and tokens before sharing.

## Release verification and source

Candidate JAR: **0.18.31**. Exact package/source hashes and the canonical
multiplayer payload identity are in the prepared `release-manifest.json` and
`SHA256SUMS.txt`. The JAR and Workshop folder share one verified payload.

See [browser release report](docs/releases/ALPHA-1.0.4-BROWSER.md),
[validation](docs/releases/BROWSER-VALIDATION.json) and
[current build/package instructions](docs/releases/BUILDING-DISTRIBUTION.md).
Folder loading is tested separately from the real Steam subscription lifecycle.
A separate clean Windows machine and independent multiplayer remain unverified.
Prepared metadata enables Mod Browser discovery and names `RostiSllAVE/Mindustry3D`.
The repository already has the discovery topic. Actual listing/download of this
candidate remains pending publication; no remote change is performed by this task.

Historical [Alpha 1.0.3 distribution audit](docs/releases/ALPHA-1.0.3-DISTRIBUTION.md),
[Alpha 1.0.0 notes](docs/releases/ALPHA-1.0.0.md),
[1.0.1 import hotfix](docs/releases/ALPHA-1.0.1-HOTFIX.md),
[0.18.29 capture validation](docs/releases/MOUSE-CAPTURE-0.18.29.md) and the
[original publication audit](docs/releases/PUBLICATION-READINESS.md) are preserved.

Unreleased Alpha v2 diagnostic work is documented separately:
[A2-04 snapshot measurements and limits](docs/alpha-v2/A2-04-NETWORK-OPTIMIZATION.md),
[test matrix](docs/alpha-v2/A2-04-TEST-MATRIX.md) and
[paired owner test](docs/alpha-v2/A2-04-MANUAL-TEST.md).
Its protocol 4 payload must match on every participant; it is not a replacement
for the published Alpha artifacts. Reliable-stream congestion remains unresolved.

## License

Project code: **GPL-3.0-only**, [LICENSE](LICENSE). No warranty.
Mindustry is by Anuken and contributors; Arc is Apache-2.0. Bundled JNA 5.19.1
uses its Apache-2.0 option and includes libffi under its permission license.
See [third-party notices](THIRD_PARTY_NOTICES.md).

Publish the hotfix JAR **together with** its corresponding-source ZIP; license
texts are embedded in the JAR and included with the source. No game binaries,
Java runtime, profiles, saves, backups, raw recordings
or proprietary Liftoff resources are included in the prepared assets.
