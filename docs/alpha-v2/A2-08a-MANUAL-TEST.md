# A2-08a — Owner gameplay check

Back up the installed mod, then import
`local/a2-08a/test-kit/mindustry-fpv-a2-08a-diagnostics.jar` using **Mods → Import Mod**
and restart. Use a disposable save and this exact JAR on every multiplayer peer.
The strict code fingerprint changes; internal version/protocol remain 0.18.31/4.
Use fullscreen if windowed-mode stutter returns. Existing release assets are intact.

1. **Z0 and transitions:** Start a slow/expensive block in Tab Build Mode; press
   F3 before completion. Check that the beam stops and progress/resources stop
   changing while the blueprint remains. Return to flight and back with Tab;
   work must remain paused. Press F3 to finish the same block. Repeat while
   starting from flight, then queue another block while paused and resume. RMB
   should only cancel the selected tool. Explicit clear removes requests; use
   dismantle deliberately if you intend to remove a physical blueprint.
2. **Upper floors and possession:** Copy/paste several blocks on Z1/Z2/Z3 to use
   the timed upper queue. Pause/resume with F3 or the panel button; verify no
   completion or cost while paused. Switch units during queued work, return to
   the original builder and resume. Verify F3 remapping in Controls, normal
   flight, firing/mining, menus, command selection and Tab cancellation.
3. **Multiplayer if available:** On two computers, repeat as host and client.
   Check that client pause reaches the host, resources are charged once, and a
   second player's work continues. Allow ordinary network delay; record whether
   the issue is Z0/native construction or an upper timed job. Do not mix JARs.

Physical Windows/Linux gameplay and two-computer acceptance are **NOT TESTED**
by the automated fixtures. No claim of manual acceptance is made for this build.
