# A2-09c — Owner test (disposable map/profile)

Local candidate: `D:\Linux\Mindustry3-Public\local\a2-09c\test-kit\mindustry-fpv-a2-09c-diagnostics.jar`.
Import through Mods → Import Mod, replace the previous local diagnostic and restart. Keep published Alpha JARs and primary saves separate. For multiplayer, host and all clients need this exact same JAR.

1. **Sandbox:** enable global infinite resources; enter F5 → Tab. Place supported walls/platforms/blocks on Z1, Z2 and a higher floor, switching target floors rapidly. Use F4 selection → Copy 3D selection → paste a three-floor structure. Blocks should appear immediately or in bounded batches, without an unexplained wait after completion. Confirm F4/R/wheel/RMB and native Z0 construction still work.
2. **Survival:** supply a core, use an ordinary builder and place upper blocks. Observe translucent blueprints and the green progress strip. F3 pauses without removing blueprints; Alt-click an unfinished blueprint changes priority without resuming F3. Resume with F3, switch units/leave build range/return, and finish the original block. Repeat with missing materials then deliver them. Costs must be charged only once, on upper-block completion. Unsupported placements must be rejected; dependent copied floors must wait for actual supports.
3. **Return/multiplayer:** check Tab/F5 transitions, regular building/configuration/removal, Z0 native progress and save/reload of completed structures. If a second computer is available, repeat upper placement/pause/priority/paste as a client and verify both screens agree, with no duplicate buildings or costs. Pending upper work is session-only and is not promised to survive save/reload. Report any long wait together with mode, unit, floor, F3 state, materials and status message.

Controls are unchanged: F5 toggles 2D/3D, Tab toggles flight/Build Mode, remappable F3 pauses/resumes construction, click without a tool or Alt-click with a tool selects priority. RMB cancels a tool/preview; it does not pause the builder. The existing explicit clear-queue action removes queued work. Do not use it when you only want to pause.

Physical acceptance remains pending; automated fixtures cannot verify real pointer capture, pixels or independent networking.
