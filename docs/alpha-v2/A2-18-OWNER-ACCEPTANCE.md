# A2-18 — Owner acceptance (RC1)

Use **Mindustry 160.7**, mod **0.19.0**, one enabled copy per installation. Candidate: `dist/alpha-2.0.0-rc.1/mindustry-fpv-0.19.0.jar`. Verify its SHA-256 against the adjacent `SHA256SUMS.txt`. Back up real saves; use disposable profiles/maps or copied saves. Fullscreen first. These tests are pending; earlier owner acceptance is historical evidence, not RC certification.

## 1. Windows standalone, then Steam

Import the candidate through Mods → Import Mod → Import File, restart normally and confirm version 0.19.0 without errors. Do not combine an imported copy with Workshop. Repeat on Steam with its bundled runtime; no separate JDK or launcher script.

On a small disposable map, F5 to enter/leave FPV, rotate continuously, Tab to build and back, open/close menus, pause, Alt+Tab away/back. Check capture only during flight. Try Acro/Stabilized, F7 views, flying/ground/naval control, unit switch, death/respawn and one weapon blocked by a wall. Check Z0/Z2 construction, F3 interruption/resume, Alt-click priority, F4 three-floor copy/paste with normal costs, a reconstructor and payload unload. Save/reload. Visit another campaign sector and return using copied progress. Report any geometry that blocks playing, not just cosmetic imperfections.

## 2. Linux laptop

Install the **same JAR**, restart and repeat F5/continuous look/Tab/menu/pause/Alt+Tab/capture restoration/shutdown. Record OS, actual SDL driver from the log (x11 or wayland), Java version, Mindustry version and mouse backend. A Wayland desktop can run the game through XWayland; record the SDL driver rather than assuming. Test both drivers only if already available; no new setup is required for this acceptance. HOST MOUSE means native capture failed, not a successful capture test. Check movement and save/reload, plus mountains/background/display screens. This laptop does not certify all Linux distributions/compositors.

## 3. Two-laptop multiplayer, both host directions

Use the same LAN first. On Windows host a disposable world; Linux joins by direct LAN address, then repeat with Linux hosting and Windows joining. Enable any host-only Creator/Free permissions deliberately if testing them. Both machines must show matching mod version and FPV READY before movement.

For each direction: move/fly/shoot and confirm actual server-owned damage; build Z0/Z2, pause/resume/priority and paste a small supported multi-floor factory; check shared resource costs and inventory; unload a produced/payload unit; disconnect/rejoin; host saves, closes and rehosts the saved world; verify both clients recover current XYZ/build state. Briefly compare horizontal/vertical camera views, a dense base and fullscreen/normal 2D.

If Linux hosting fails, keep the failure stage and sanitized diagnostics: does ordinary native direct-IP joining work with FPV off; is the server listening on the intended interface/port (default 6567 for normal Mindustry, not the lab's 16567); does discovery fail while direct IP works; does native joining succeed but FPV handshake time out or reject the identity? Check existing OS/firewall permissions with the owner; do not globally disable the firewall or forward ports automatically. A network timeout before native join is not evidence of an FPV protocol defect. An explicit mismatch requires comparing both JAR SHA-256 values and removing duplicate versions.

Record host OS/runtime, installation method, test result, first relevant error, game/mod version and whether native join/READY occurred. Remove personal paths, public IPs, account IDs/tokens from shared logs. An internet session/third-party session is a separate NOT TESTED item until physically performed.

## Decision

Return PASS/FAIL for Windows standalone, Windows Steam, Linux capture, Windows→Linux, Linux→Windows, save/reload and campaign transition. Any crash, damaged save, unusable construction/production or authoritative state corruption blocks publication. Cosmetic issues can be listed as known limitations. No publication is authorized by running this checklist.
