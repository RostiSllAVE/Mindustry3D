# A2-16b — Owner visual acceptance

Import `local/a2-16b/test-kit-final/mindustry-fpv-a2-16b-diagnostics.jar` using
Mindustry 160.7 Mods → Import Mod, enable one copy, then restart. No developer
files or separate Java are needed. Use a disposable map/backup and fullscreen on
Windows. The same diagnostic JAR targets the existing Windows/Linux paths;
physical Linux acceptance is still pending. No new key bindings.

1. Build a straight ordinary/titanium conveyor line on Z0. Inspect direction,
   continuous joins, side guards and item height from low FPV and top-down Tab.
2. Build a straight line on a supported upper platform; inspect its exposed edge
   from the side and below. Solid platforms should still hide the belt above.
3. Add left/right turns and side feeds in all four orientations. Check rails do
   not cross a valid input and the native belt texture still points correctly.
4. Build a Junction crossing with items arriving on both axes; verify native
   independent transport and readable crossing/side ports.
5. Inspect a compact Router/Distributor area connected to several conveyor lines.
6. Compare Sorter and Inverted Sorter; change the selected resource, then clear
   it. Roof art and filter color should follow the actual configuration.
7. Run copper, lead and another item through a dense line, including armored and
   plastanium sections. Check actual pallet/item motion and nearby large blocks.
8. Repeat the same arrangement on Z0 and a Z3+ platform, preferably stacked. Also
   glance at Z1/Z2/Z4/Z5; upper items must not use a lower floor's position.
9. Copy/paste a multi-floor logistics fragment; rotate its preview and check
   orientation, transparency, native costs, unfinished states and completion.
10. Save/load the test map and repeat F5/Tab, placement/configuration/removal,
    Alt-click blueprint priority and F3 stop/resume. Nothing should lose its state.

Optional: compare Overflow/Underflow native symbols and routing. Report any gaps,
floating/cropped cargo, hidden deck surfaces, severe flicker or dense-field cost.
Items remain sprites; partial clipping at the boundary of a belt can be visible
in close slow inspection. Very distant models intentionally omit tiny arches.
These steps are physical acceptance, not claimed complete by automated tests.
