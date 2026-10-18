# A2-04 — Short owner A/B test

This is a local diagnostic build, not a public release. Use Mindustry 160.7.
Back up first and use a disposable custom map/profile; do not overwrite the only
campaign save. Windows: use Fullscreen, equal resolution, Max FPS 60, Show FPS &
Ping ON. Record host OS/revision, client OS/revision and join method. Never assume
Steam invitations imply a particular relay/direct path.

1. **Baseline:** install the SAME preserved A2-03 diagnostic JAR on both players
   using Mods -> Import Mod -> Import File and restart. Disable any subscribed/
   imported duplicate. Enable Settings -> FPV flight -> Record multiplayer
   diagnostics on both. Use one disposable scene at 100, 1000, then 4096 units if
   the host can run it safely. Record displayed FPS/ping, moving/combat response,
   delayed build confirmations and whether delay grows over 2 minutes. Stop if
   the machine becomes unresponsive; do not force the largest case.
2. **Optimized:** both import the enclosed `mindustry-fpv-a2-04-diagnostics.jar`
   and restart. It uses protocol 4; the same displayed 0.18.31 is NOT evidence of
   matching binaries. Repeat identical scene/settings/host. Wait for FPV READY,
   test F5, mouse capture, Tab, placing/removing a Z0 and supported upper block,
   resource counts, unit death/possession, team/fog visibility and leave/rejoin
   three times without restarting. Swap host direction afterwards; record Linux
   and Steam invitation cases separately where available. Do not weaken identity
   checks or mix baseline and optimized participants in a valid session.
3. **Collect:** turn diagnostics OFF, wait two seconds and close normally. From
   Mods -> Open Folder go up to `fpv-diagnostics` and copy the newest JSON from
   EACH side, labeled baseline/after, host/client and unit count. Keep a separate
   copy per case; the session report is bounded/rolling. Send PASS/FAIL/NOT TESTED,
   visible FPS/ping, whether response/backlog improved, and any exact error/stage.
   Inspect reports; never send whole profiles, primary saves or raw unsanitized
   logs. Hide names/paths/addresses/Steam IDs in any screenshot/error excerpt.

No separate Java, admin tools or network shaper is needed. If a naturally slower
connection is available, repeat there; do not change firewall/router settings for
this pass. Otherwise mark real constrained-link testing NOT TESTED. Model results
are not a claim about your internet: the 4096-unit/256-kbit/s modeled case still
fails the existing assembly deadline, and overloaded reliable streams still queue.

All participants must restore the same public mod afterwards and restart; keep
experimental profiles separate. Do not publish this package or update Workshop.
