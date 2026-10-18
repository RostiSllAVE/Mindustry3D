# A2-01 — Two-player test without developer tools

This is a test protocol for after the owner returns. No test is waiting for an
answer during A2-01. No separate Java, compiler, terminal or packet recorder is
required. Do not change firewall/router settings or install tools for this pass.

## Before starting

1. Both players back up data with the game's **Export Data** option. Keep the
   public Alpha JAR/download available for restoring afterwards.
2. Start with a disposable custom map, not the only campaign save. Use the same
   Mindustry **160.7** revision on both computers. Write down OS, game revision,
   vanilla/mod, who hosts and whether joining was LAN/direct or Steam invitation.
   If the relay/direct route is unknown, write **unknown**.
3. Compare vanilla first with the FPV mod disabled on both sides, then test the
   mod. Restart after changes. For the diagnostics pass both players must import
   the **same `mindustry-fpv-a2-01-diagnostics.jar`** using Mods -> Import Mod ->
   Import File, accept replacement and restart. It is unreleased; do not mix it
   with public Alpha 1.0.4 even though both display 0.18.31. Do not enable an
   imported JAR and a subscribed copy together. Disabling the duplicate in the
   game's Mods screen is enough for the test; do not upload/update Workshop.
4. Use Fullscreen on Windows, **Max FPS 60**, **Show FPS & Ping ON**, the same
   resolution and comparable visual settings. Keep these fixed while comparing
   hosts. Windowed mode is a known confounder. The mod's flight tuning comes from
   the host; record that host's flight preset/speed settings.
5. On **both** players enable Settings -> **FPV flight -> Record multiplayer
   diagnostics (local, opt-in)**. Give it 30 seconds to warm up. This option
   records local numeric summaries; it sends no new diagnostic network packets.

## Repeat the same scenes

| Time/scene | Actions and observations |
|---|---|
| Fresh map, 2 minutes | Host a small map. Join, wait for the world, then check that F5 and Tab work on the client. Record FPS/ping and any exact error text |
| Input response, 1 minute | With Gamma/Alpha, switch to Stabilized. Tap forward, stop, turn, climb and fire. Compare local camera response, actual unit motion, hit/effect timing and build confirmation. Do not judge only by free-look |
| Small combat, 2 minutes | Use a comparable small enemy encounter. Record whether only hit effects are delayed or all movement/building is delayed |
| Construction, 2 minutes | Place/remove one Z0 block and one supported elevated block, rotate a conveyor, enter/leave Tab. Check both players see the same result and floor |
| Longer session, 15 minutes | Keep a modest scene active; repeat the same short movement at minutes 1, 5, 10 and 15. Record whether lag grows, arrives in bursts or disappears after reconnect |
| Swap Windows host | Repeat the identical map/settings with the friend hosting. Do not change host, map, transport and mod version all at once |
| Steam invitation | Repeat separately only when both players are present and agree. Record invitation path; do not assume it means relay or direct |

Windows/Linux and Linux/Windows runs are separate cases, not inferred from
Windows/Windows. Skip unavailable cases and mark NOT TESTED. A successful Linux
launch does not establish Linux multiplayer or Android support.

## Campaign test: use a verified separate profile

Do this only after the disposable-map steps and a complete backup. Use an
existing isolated test copy/profile. On Windows, the previously used separate
160.7 test copy can be used; if making another local copy of the installed game,
run its own executable and first check **Mods -> Open Folder**. Its data/mods
folder must be inside the separate test area, not the normal installation/profile.
Steam subscriptions may still appear globally: disable the duplicate in that
test instance. Do not distribute any copied game/Java files.

If isolation cannot be verified, **skip the campaign steps**. Copying the game on
Linux does not by itself establish a separate profile; do not assume it is safe.

In the isolated profile only, import a backup copy of the affected campaign:

1. Record whether the save opens offline and whether the core/unit/map appears.
2. Start hosting that same loaded sector; record whether the host alone still works.
3. Join from the client: distinguish connection failure, world-loading failure,
   missing F5 initialization, missing upper blocks, movement delay and combat delay.
4. Save, leave to menu, reload, reconnect. Then test an available second sector
   and a custom map, recording exactly which transition fails.
5. Do not overwrite the original campaign or continue playing its only save.

## Collect only what is useful

- Take cropped screenshots of both FPS/ping displays, the mod/version list,
  exact error message and a missing/desynchronized structure if relevant. Hide
  player/Steam names, invitations and notifications before sharing.
- On both sides switch diagnostics OFF; wait about two seconds so the stop flush
  occurs, then close normally. From Mods -> Open Folder go up to the active data
  folder and open **`fpv-diagnostics`**. Copy the newest `network-....json`.
  Identify host/client and scenario in your message or rename the copy manually.
- The JSON has only fixed numeric/boolean fields and a public payload digest.
  It should contain no private path, address or Steam token. Review it anyway.
  Do not send the whole profile, a raw save or an unsanitized `last_log.txt`.
- If the mod never initializes, there may be no diagnostic file. Send the exact
  visible error and only a short manually sanitized log excerpt: remove local
  usernames/paths, IPs, Steam identifiers, UUID/USID and tokens.
- Say whether the problem reproduces in vanilla, whether swapping only the host
  changes it and whether reconnecting helps. Rough FPS/ping values are better
  than an estimate made without looking at the counter.

## Recognize the types of delay

Low host FPS with increasing client delay suggests server/frame starvation.
High FPS but delayed physical input with modest displayed ping is compatible with
the mod's server round trip plus display buffer. Increasing pose age or traffic
with stable FPS suggests delivery/application backlog, but requires analysis.
Only missing upper structures after loading suggests a state/epoch/apply problem.
An identity rejection is a package mismatch, not a reason to disable validation.

The displayed ping is **application ping**, not pure internet RTT. It can rise
because the game thread is stalled. No screenshot alone proves the route or cause.

After collecting the test, restore the public mod on **both** participants,
restart and leave diagnostic recording OFF. Keep test profiles separate. Do not
publish the diagnostic package or update the Workshop item.
