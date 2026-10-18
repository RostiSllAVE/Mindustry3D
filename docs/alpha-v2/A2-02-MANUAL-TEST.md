# A2-02 — Two-device connection acceptance (no developer tools)

This is an UNRELEASED diagnostic build, not an Alpha update to publish. No separate
Java, PowerShell, compiler, admin access or packet recorder is needed. Skip cases
that require unavailable participants/permissions and record NOT TESTED.

## Install the exact same package

1. Back up with Mindustry's Export Data. Keep the current public mod download for
   restoration. Use a disposable custom map first, not your only campaign save.
2. Share the local `mindustry-fpv-a2-02-test-kit.zip` privately with the test
   participants. Its JAR is `mindustry-fpv-a2-02-diagnostics.jar`. Do not use the
   older A2-01 diagnostic JAR or the published Alpha on either side. All still
   display 0.18.31; that text does not prove matching code. The included manifest
   gives the exact SHA-256 and canonical payload identity.
3. Use Mindustry **160.7** on both computers. Mods -> Import Mod -> Import File,
   select this JAR, accept replacement and restart. Avoid enabling a Workshop
   duplicate and the imported JAR at once; disable the duplicate in Mods for the
   test. Do not update or upload Workshop.
4. Windows: use Fullscreen to avoid the known windowed-mode performance issue.
   Use equal settings, Max FPS 60 and Show FPS & Ping ON. Record OS/game revision,
   which machine hosts, join method and any exact error. Do not change firewall,
   router, Java or graphics settings to force a result in this pass.
5. Both players enable Settings -> FPV flight -> **Record multiplayer diagnostics
   (local, opt-in)** before hosting/joining. The FPV settings show the current
   connection status even if F5 is not yet usable.

## Run each available pairing separately

First compare the same disposable map with the mod disabled on BOTH computers
and restarted. Record whether native vanilla hosting/joining works. Then enable
this exact diagnostic build on both and restart once for installation.

| Pairing | Required repetitions |
|---|---|
| Your Windows host -> friend's client | Fresh join, leave/rejoin 3 times without restarting either game |
| Friend host -> your Windows client | Repeat same map/settings and rejoin sequence; this is the historical failing direction |
| Your Windows host -> your Linux laptop | Same checks, separate result from Windows/Windows |
| Linux laptop host -> your Windows client | Swap only host direction, repeat |
| Steam invitation from either host | Repeat each available host direction separately, with the friend present; record route as unknown if unknown |

For EACH pairing:

1. Host a small fresh map. Join normally. Record stages seen in FPV settings:
   waiting for native world -> verifying -> receiving initial state -> READY.
   Brief stages may be missed visually; the report records readiness/timings.
2. Wait for **FPV READY / F5 view / Tab build**. An ACK/matching version alone is
   not READY. If it says waiting for a controllable unit, spawn/possess a valid
   friendly unit normally. Do not repeatedly click F5 to bypass waiting.
3. Check ground and flying units, F5 in/out, movement/turning/firing, Tab in/out,
   mouse capture/release, menus and Alt+Tab. On both screens confirm the same
   unit and world. Record delay separately from inability to connect/control.
4. Place/remove a normal Z0 block and a structurally supported elevated block;
   confirm both clients see the same floor, direction, build completion and cost.
   Before a later join, have the host leave one supported Z2 structure in place;
   the rejoining client must receive it before becoming READY.
5. Client leaves to menu, then rejoins WITHOUT restarting. Repeat three times.
   Confirm no stuck input, missing layers, duplicate placements or accumulating
   errors. Swap between a ground unit and aircraft; check normal control returns.
6. Host returns to menu and hosts a fresh map again in the same process. Rejoin.
   Also save/reload a DISPOSABLE custom game and reconnect. No old-world movement
   or construction should execute in the new world.
7. If a failure occurs, record exact message, last status, whether native world
   appeared, whether F5/Tab were disabled and whether reconnect recovers. Let
   the classified timeout finish once instead of judging an ACK as success.
   Native world timeout is 120 seconds; initial FPV transfer has a 45-second
   inactivity / 180-second total bound. Stop if the game/system becomes unwell.
8. At the end turn recording OFF on both, wait two seconds and close normally.
   From Mods -> Open Folder go UP to the active data folder, then open
   `fpv-diagnostics`. Copy the newest `network-....json` from EACH side. Keep
   separate copies per pairing before another test overwrites that session's
   rolling report. Note host/client role and success/failure in your message.

## Safe mismatch check and campaign boundary

After a successful disposable-map connection only: one participant temporarily
imports the previous public Alpha while the other keeps A2-02, then restarts.
Attempt joining once: an explicit incompatible-mod rejection is EXPECTED. Restore
A2-02 on BOTH and restart before continuing. Never disable validation or change
the fingerprint manually. Skip if ordinary import replacement is inconvenient.

Campaign/sector tests require an already verified isolated profile and an exported
BACKUP copy. Verify Mods -> Open Folder is inside the separate test area first.
If isolation is uncertain, skip. Never use or overwrite the primary campaign for
this pass. Test offline load -> host -> join -> save/leave -> reload/rejoin, then
an available sector/custom-map transition. Record failure stage separately from
physical input lag. Native-world and layer snapshot tests cannot certify this.

## What to send back

For every pairing: PASS/FAIL/NOT TESTED, OS/game revision, host direction, join
method, whether vanilla works, stages/errors, F5/Tab/control/build results, 3
rejoin results, map reload result, approximate visible FPS/ping and both JSONs.

Reports use fixed numeric/enum labels and public payload digests. Review before
sharing; do not send your whole profile, raw save or unsanitized logs. If the mod
never initializes and no JSON exists, share only the exact visible error and a
short manually sanitized log excerpt (remove names, paths, IPs, Steam IDs,
UUID/USID and tokens). A native connection error is different from FPV FAILED;
record which one occurred. First usable input timing is not visual latency.

After tests restore the public mod on BOTH, restart and leave recording OFF.
Do not publish the diagnostic build or update Workshop. These physical results
are needed before accepting the A2-02 connection milestone.
