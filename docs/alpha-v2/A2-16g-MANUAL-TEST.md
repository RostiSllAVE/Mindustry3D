# A2-16g — Three owner checks

Import `local/a2-16g/test-kit-final/mindustry-fpv-a2-16g-diagnostics.jar` through
Mindustry 160.7 **Mods → Import Mod**, replace the local test build and restart.
Use a disposable sandbox or backed-up save. Windows fullscreen is recommended.
The source ZIP is not the importable mod. One matching JAR is supplied for
Windows/Linux; physical Linux acceptance remains pending.

1. **Models and real state.** Place Node, Large Node, Surge Tower, Diode, both
   batteries, both solar panels, Combustion/Steam/Differential/RTG Generators and
   Thorium/Impact Reactors. Supply normal fuel, coolant and startup power. Inspect
   from above, below and low FPV. Compare idle/working, empty/full battery and
   Diode rotations. Stop input/power and pause with F3. Do not remove reactor
   coolant merely to test cosmetics: native reactor behavior remains intact.
2. **XYZ connections and lifecycle.** Connect valid nodes across offset Z0/Z1/Z3/Z5
   platforms with real range/headroom and a clear path. Check attachment points,
   configure/disconnect, destroy a node and test an insulating obstruction.
   Copy/paste a multilevel power structure, then save/reload or change sector.
   Links must match the accepted network; copied batteries must not clone charge.
3. **Toggle and dense scene.** In Settings → FPV flight toggle **Animated power
   connections**. OFF should retain static cores; ON adds gentle opposing pulses
   and envelopes. Compare 10/100/many links at day/night, near walls and from
   below. Try F5, Tab, normal building, Alt-click priority and F3. Report clipping,
   stale links, excessive brightness or FPS changes, including platform/runtime.

For multiplayer use an identical JAR on peers. Remote multiplayer, physical
Linux input and owner visual acceptance have not been performed by these tests.
