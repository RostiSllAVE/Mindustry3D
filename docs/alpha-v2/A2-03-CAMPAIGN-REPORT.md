# A2-03 — Campaign compatibility investigation

Baseline: A2-02 `08671a79f88f6b70bae5f7c63a87a70cbf423133`, branch `main`.
Host under test: Mindustry **160.7**. Date: 2026-10-18.

## Outcome

**NOT REPRODUCED:** the player's unspecified campaign failure. No confirmed
campaign runtime defect was found in the exercised offline loading/save paths.
Consequently **no gameplay fix, save migration or initialization rewrite was
made**. Changes are a campaign regression test, a diagnostic-build task selector,
this report/matrix/manual procedure and a README link. All production Java,
resources, metadata and A2-02 connection logic remain unchanged.

This is an automated native campaign compatibility checkpoint, **not** physical
planetary-campaign or multiplayer acceptance. Do not interpret a passing native
save test as a successful F5/Tab interaction or complete campaign playthrough.

## Lifecycle audit

Inspected the installed 160.7 APIs/bytecode (World, SaveVersion, SaveIO,
FileMapGenerator and WorldReloader) and the current mod source. Cached host source
was used to navigate; actual tests execute the installed 160.7 desktop classes.
No game code/assets were copied into the mod/source distribution.

| Native path | Mod handling and result |
|---|---|
| New planetary sector | WorldReloader.begin resets the old game; World.loadSector sets planet/environment/rules and runs the actual preset or procedural generator. Native WorldLoad completes the map; no alternate FPV campaign generator exists |
| Preset sector | FileMapGenerator internally uses SaveIO with a filter context, suppressing the inner map-end callback; final WorldLoad occurs after generation. The native preset path is exercised with Ground Zero and Onset |
| Existing campaign sector | Native Control.playSector saves the current slot, then loads the destination with makeSectorContext to retain its correct sector identity. The test exercises that sector-context SaveIO path, not the planet UI itself |
| Full save | SaveIO writes metadata, map, native entities and custom chunks. Campaign SectorInfo/tech state goes into the isolated native Settings as usual. The mod does not replace the native sector save format |
| Full load | SaveIO resets first. Native map completion emits WorldLoad before entity/custom-chunk restore. FPV runtime/cache resets happen before saved layer/stance/boost state is read, so the restored state is not erased afterwards in the tested path |
| Layered construction | Optional fpv-layered-world chunk preserves components, supported native building state and related runtime data. Empty native saves need no invented upper structures; native Z0 remains the map source of truth |
| Terrain | Generated from each active sector's natural-wall mask. Desktop renderer invalidation and headless terrain invalidation occur on WorldLoad; derivation is read only and is rebuilt for the new map |
| Units | Native entity identity/XY/HP are serialized by the host. Dedicated FPV chunks preserve supported stance and boost/creator states. Saved boost/creator bodies resume a safe landing, deliberately not stale player input |
| Network | A2-02 clears world authority/controls/assemblies and changes epoch; native world streaming excludes these custom save chunks and matching FPV channels supply initial state before READY. Code unchanged; actual hosted campaign remains NOT TESTED |

Desktop FpvMod.init runs once at mod startup, using the same supported-host/input/
renderer/network setup for custom maps and campaign. There is no campaign-only
opt-out. Its WorldLoad hook releases the old pilot, clears effects/unit caches,
invalidates rendering, resets ready/unit ID and sets autoEnable; update initializes
the actual new live player unit when available. Reset releases capture and Build
Mode. This is a **source audit**, not a physical player-input test of these hooks.

Ordinary aircraft altitude is a derived sidecar and reinitializes after load;
the new test checks that the restored native aircraft gets a finite flying pose,
not exact preservation of an arbitrary previous FPV altitude/velocity. Terrain
also regenerates rather than being stored as voxels. No new persistence contract
was introduced. Unsupported/corrupt saves are not silently repaired.

## Actual automated checks

`CampaignLifecycleNativeTest` uses a NEW temporary profile under the explicit
D: test directory, real isolated Settings, native base content, real sector
and preset generators, WorldReloader and full disk-backed SaveIO. Application,
audio and graphics are mocks; Net(null) is offline and opens no game socket.

The final campaign test has **39 checks**, run on Java 17 and stock Steam Java 25:

- Fresh Ground Zero generation, native core and no active network.
- A native save written before installing the FPV runtime, then loaded with it:
  no upper blocks invented and real core resources retained.
- A structurally supported Z1 wall and functional Z2 conveyor with real resource
  payment, directional rotation and damaged conveyor HP; restored on full load
  without paying twice. Test content is explicitly unlocked for this fixture;
  this is not a research-progression or building-UI acceptance test.
- Native map fingerprint unchanged by 3D height generation and full save/load;
  correct regenerated terrain on another generated Serpulo sector and on return.
- Walker stance and actual boost/creator XYZ/momentum through full native save;
  safe landing after load. Native Flare identity/XY/HP and finite derived flight
  pose also restored.
- Exact previous file retained by native SaveIO backup on overwrite; source
  fixtures and copied files remain byte-identical when only loaded.
- Ground Zero -> generated Serpulo sector 2 -> Erekir Onset -> disk reload ->
  existing Ground Zero copy; twice more custom-world -> campaign. No old upper
  blocks or boost authority leak into another sector/custom world; no duplicates
  when returning to the saved one.
- Campaign sector metadata survives a separate Settings instance reading the
  isolated profile. No normal player settings/save directory is loaded.

Additional **8-check cold-JVM restores** use disposable campaign fixtures made
with the exact preserved A2-02 JAR (Java 17 and Steam Java 25), and a fixture made
with preserved Alpha 1.0.4 (Steam Java 25). These restore campaign/core, upper
structures and boost/creator state in a new process and do not alter the fixture.
They are synthetic existing saves, **not** the owner's actual reported save.

All **120 regression programs** pass on Java 17 and stock Steam Java 25, including
A2-02's readiness/reconnect handlers, strict fingerprints, native campaign blocks,
construction, combat and mouse contracts. The final aircraft and active-transition checks were
added after the initial full build and the complete campaign program was rerun
on both runtimes; all 39 pass. No production class changed between these runs.
See [machine evidence](A2-03-VALIDATION.json) and [matrix](A2-03-TEST-MATRIX.md).

## Failed attempts and untested boundaries

- A real isolated SDL desktop attempt reached OpenGL/Java startup, then failed
  constructing the native ArcNetProvider selector: loopback connect permission
  was denied, **before mod initialization or sector entry**. No networking
  permission was requested or bypass attempted. The native early logger/crash
  handler also attempted default-profile log output before the directory override;
  those writes were denied. No primary save was accessed or modified.
- This environment failure is **not a campaign-mod defect**. Graphical campaign
  initialization, planet UI, physical F5/mouse/Tab, menu/Alt+Tab and visual artifacts
  remain **NOT TESTED** for this checkpoint.
- All Windows/Linux two-computer hosting, Steam invitations and hosted campaign
  transitions remain **NOT TESTED**. Prior owner reports are historical evidence,
  not certification of this pass. Linux startup of this artifact was not rerun.
- Long campaign play, wave capture, tech tree completion, landing/launch animation,
  recapture of lost sectors, exports over long offline periods, modded planets,
  corrupt saves and a copy of the actual affected player save remain NOT TESTED.
- Early test-fixture failures were corrected only in tests: native Settings needs
  JsonIO serialization, an updating player's entity must be added to the native
  player group, simple walls use component HP rather than a native adapter, and
  sector metadata keys must derive the actual preset sector ID. None justified a
  production change. Intermediate local logs are retained rather than hidden.

## Local artifact and preservation

`local/a2-03/test-kit/` contains the installable diagnostic JAR, matching source,
checksums, notices and this procedure; a convenience ZIP is beside it. It still
reports internal 0.18.31 and protocol 3. Its canonical compiled payload is exactly
A2-02's, because production code/resources were not changed. Archive SHA-256 can
vary with ZIP timestamps; canonical identity is checked separately. This is not
a new public Alpha release and not a campaign-fix claim.

Existing Alpha artifacts, A2-02 JAR/test kit/checkpoint, primary saves and original
repository were preserved. No push, publish, Workshop update, merge or reset.

## Owner action

Follow the [three short checks](A2-03-MANUAL-TEST.md). The most useful next evidence
is the exact point and visible error from the previously affected campaign copy,
including whether F5 fails on entry or only after changing/reloading a sector.
Physical offline campaign acceptance remains pending; multiplayer acceptance
requires a real two-computer run and is not inferred from this work.

### Owner follow-up received before A2-04

The owner reports successful offline campaign F5, Tab, saving and transitions
between sectors. These specific physical checks are now owner-confirmed. The
automated evidence above is unchanged; long playthrough and real two-computer
hosted campaign acceptance are still pending. No platform-wide certification is
inferred from this report.
