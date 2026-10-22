# A2-16h — Three owner checks

Import `local/a2-16h/test-kit-final/mindustry-fpv-a2-16h-diagnostics.jar` using
Mindustry 160.7 **Mods → Import Mod**, replace the local test build and restart.
Use a disposable map or backed-up save; Windows fullscreen is recommended. One
portable JAR is supplied for Windows/Linux. Source ZIP is not the importable mod.

1. **Pipes and fill.** Connect Conduit, Pulse Conduit and Plated Conduit in straight
   lines, corners and junctions beside Liquid Router. Supply water, oil, slag and
   cryofluid separately. Compare empty/full Container and Tank from above and low
   FPV. Liquid must stay within the channel/chamber without detached segments.
   If native flow-rate observation is unavailable, static fill is expected.
2. **Machines and bridges.** Operate Mechanical, Rotary and Impulse Pumps on valid
   liquid terrain and Thermal Generator on a valid heat-bearing floor. Compare
   working, full/blocked, disabled and F3-paused states. Configure Bridge Conduit
   and Phase Conduit; disconnect/destroy targets and test valid existing XYZ
   connections. No phantom endpoint or new cross-floor capability should appear.
3. **Floors and regressions.** Place supported liquid logistics on Z1/Z3/Z5 with
   ordinary support/headroom; copy/paste the structure and save/reload/change
   sector. Copied storage must not duplicate its contents. Try ghosts, selection,
   Alt-click priority, F3, F5 and Tab. Compare a dense factory against A2-16g and
   report FPS/platform/runtime. A dry upper platform must not magically supply
   liquid or geothermal heat. Check the accepted power models/XYZ beams too.

Pumps require real liquid-bearing terrain; this build adds no elevated water or
heat source. Native collision/blueprint outlines keep their original dimensions;
new visual housings can be taller. Custom unusually tight floor spacing needs
separate inspection. Use identical JARs for multiplayer. Physical Linux/remote
multiplayer and owner visual/input acceptance remain pending.
