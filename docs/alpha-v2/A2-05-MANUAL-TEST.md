# A2-05 — Owner test (two computers)

Local diagnostic build, Mindustry 160.7, internal 0.18.31 / protocol 4. The same
version label is insufficient: install the EXACT enclosed diagnostic JAR on every
participant. Do not mix it with A2-04 or published Alpha. Back up first and use
disposable profiles/maps. Windows: Fullscreen, equal settings, Show FPS & Ping ON.
No separate Java, command line, administrator tools or network shaping is needed.

1. **Install and compare on the joining client.** Mods -> Import Mod -> Import
   File, select `mindustry-fpv-a2-05-diagnostics.jar`, disable any subscribed or
   imported duplicate, restart both games, then host/join and wait for FPV READY.
   Enable Record multiplayer diagnostics on both. On the CLIENT open Settings ->
   FPV flight -> Local pilot prediction (multiplayer presentation). Compare OFF
   and ON in the same scene: Gamma/Flare, short starts/stops, turns, Acro throttle
   and pitch, Stabilized WASD and looking around, F7 and Tab. A listen host already
   runs authoritative motion locally, so this toggle is not a host responsiveness
   test. Record displayed FPS/ping and whether corrections or clipping are visible.
2. **Check boundaries and real actions.** Fly toward terrain, a wall and an upper
   platform; try ground Dagger and a walker, Q/E stance, Vela boost/landing, unit
   switching, death/respawn and F5. In Tab place/remove one Z0 and one supported
   upper block; check resources and actual construction from the OTHER player's
   view. Shoot/ram once and compare real health and ammunition/reload behavior.
   The preview/ghost must not imply that damage, a boost transition, touchdown or
   construction is completed before the server confirms it. Open a menu/Alt+Tab,
   return, leave/rejoin three times, then save/load or change sector on a COPY.
3. **Swap host and collect.** Repeat the useful cases with the other host; label
   Windows/Linux and Steam invitation/direct cases separately where available.
   Start near 100 units; use 1000 only if the host stays responsive. Turn diagnostics
   OFF, wait two seconds and close normally. From Mods -> Open Folder go up to
   `fpv-diagnostics` and copy the newest JSON from each side, labeled OFF/ON,
   host/client and scene. Send PASS/FAIL/NOT TESTED, ping/FPS, and the exact unit,
   mode and action that snapped or failed. Remove names, endpoints and private
   paths from screenshots/error excerpts; do not send whole profiles or saves.

Stance/support changes and boost permission/touchdown remain server-confirmed.
Lost or very late data can make the display correct or hold; prediction does not
solve the reliable-stream congestion documented in A2-04. Movement may look
responsive while a shot/build request still waits for the server. Existing weapon
effects/animations still use actual replicated events.

Use a naturally slower link only if available; do not change firewall/router
settings for this test. Otherwise mark real impaired-link testing NOT TESTED.
Restore the same public mod on everyone afterwards and restart. Do not publish
the kit, push Git changes or update Workshop from this test.
