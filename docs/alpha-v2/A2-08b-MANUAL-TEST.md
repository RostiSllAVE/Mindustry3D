# A2-08b — Owner visual acceptance

Back up the installed mod. Import
`local/a2-08b/test-kit/mindustry-fpv-a2-08b-diagnostics.jar` through **Mods → Import
Mod**, replace the old diagnostic and restart. Use a disposable sandbox/save and
the exact same JAR on all peers. Existing Alpha releases are unchanged.

1. **Native Z0, pause and tools:** Queue three slow blocks A/B/C within builder
   range. Enter Tab and deselect the placement tool with RMB, then click C's visible
   blueprint. Confirm C advances first while A/B remain. Pause with F3, click A,
   switch Tab to flight and back, and confirm it stays paused until F3 resumes it.
   Select a block/schematic tool and Alt-click B; the tool should remain selected
   without an extra placement. Alt-click empty space: nothing should be built.
   Check normal placement, configuration, RMB dismantling and Ctrl possession too.
2. **Actual floors and occlusion:** Queue a timed upper paste on Z2/Z3 with valid
   support (or deliberately host-authorized Free Mode). Click a later upper entry,
   then a native Z0 entry, without manually changing floors. Check that only the
   chosen work advances until complete and that previous progress/costs remain.
   Repeat with the construction floor locked elsewhere. Put a solid higher wall
   over a lower blueprint: it must not select through it. Aim at an exposed edge
   of a partly covered multi-tile blueprint: only the visible area should select.
   Move out of range or remove support: check the rejection message, not free work.
3. **Controls and multiplayer if available:** Fly/fire/mine normally, use F5, Tab,
   menus, command selection and mouse capture; return to construction. Switch units
   during priority work and clear a queue deliberately; no plan should resurrect.
   On two computers repeat as host/client while another player builds independently.
   Confirm costs once, only the owner's queue is reprioritized, upper views update
   after normal network delay, and F3 still stops work. Rejoin/world transition must
   not retain another world's selection. Do not mix diagnostic JAR versions.

Record whether a failure is Z0/native, upper timed work, picking or networking;
include the actual floor, selected tool/modifiers, queue order and resource count.
Physical visual and two-computer acceptance remain **NOT TESTED** until this check.
