# A2-16e — Three owner checks

Import `mindustry-fpv-a2-16e-diagnostics.jar` through Mindustry 160.7
**Mods → Import Mod**, restart and enable only one copy. No developer directory
or separate Java installation is required. Use a disposable map/save or backup;
Windows fullscreen is recommended because of the previously identified windowed
performance issue. The same JAR uses the existing Windows/Linux desktop code,
but physical Linux GPU/input acceptance remains pending. No controls are added.

1. **Inspect six operating machines.** Place Kiln, Plastanium Compressor, Phase
   Weaver, Surge Smelter, Separator and Disassembler beside normal belts and
   supply their required items, liquids and power. In F5 FPV and Tab Build Mode,
   inspect from above and close to the side. Check furnace heat, opposed rams and
   attached rods, the loom shuttle/weave, separator spinner and paired shredder
   drums. Remove input/power, disable a block and let output fill; motion should
   rest/coast and heat fade, then resume correctly. Confirm real output on belts,
   including the separators' normal mixture of resources.

2. **Inspect elevated and copied machinery.** Use supported platforms or
   explicitly host-approved Free Mode. Test Z1 and Z3–Z5, using Z1/Z3/Z5 for an
   aligned vertical stack: these factories legitimately need headroom. Inspect
   edges from below, side and low FPV. Copy/paste a valid multi-floor group with
   the existing volume tool, allow it to finish, and inspect unfinished ghosts,
   selection outlines, bases, neighbors and animated attachments. No inventory
   or finished products should be duplicated by copying.

3. **Check a mixed factory field and reload.** Mix the new six with the accepted
   Graphite/Multi-Press, Silicon Smelter/Crucible and Pyratite/Blast Mixer models.
   Inspect close and distant views, then save/reload. Confirm prior models,
   construction progress, resources and connections remain correct. Check F3
   interruption/resume, Alt-click blueprint priority, F5/Tab, menus and mouse
   capture/release. Repeat on Linux when convenient; real multiplayer acceptance
   needs two computers with the identical diagnostic JAR.

Report the block, floor, operating state, camera angle and screenshot if anything
looks wrong. Automated native tests and scripted OpenGL screenshots are evidence,
not a replacement for these physical checks.
