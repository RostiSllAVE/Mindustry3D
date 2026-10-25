# A2-17a fix1 — repair beams and construction direction arrows

Baseline: owner-accepted `b3faf9a` on `main`. Small presentation fix, local diagnostic only. No terrain style, gameplay healing, physics, cost, input, save-format or multiplayer-protocol changes.

## Confirmed repair-beam cause

A native standalone reproduction increased a Zenith's HP **50 → 62.28749**, with a valid target, enabled turret, efficiency 1 and strength 0.63233. The full world renderer still submitted **zero healing beams**. Diagnostics showed two attached world buildings, an empty effect candidate list, and both the native and cached `tileChanges` equal to **-1**.

`SupportEffectsRenderer` used -1 as its uninitialized cache sentinel. Mindustry can legitimately retain -1 after world generation/loading, so the equality check skipped the first candidate scan. Re-entering a cleared presentation cache could repeat the problem. This is not a change to mountain geometry or healing rates.

The candidate cache now has an independent validity flag. First use, world replacement or explicit clear always performs one scan, even at counter -1. Unchanged frames reuse the result; ordinary counter changes still invalidate it. There is no new per-frame world scan. Inactive/disabled sources and missing targets retain their existing immediate visual stop guards.

The visual beam origin also now uses the shared auxiliary turret roof depth plus a small clearance. The accepted new 3D head extended above the old fixed emitter offset. Native XY muzzle length/yaw and actual target XYZ remain authoritative. The model geometry itself is unchanged. Remote visuals still consume the existing validated support snapshot; no new packet or local healing authority is introduced.

## Preview direction marker

The shared typed `ConstructionVisualRenderer.ghost(Block,...)` path adds one animated arrow for blocks whose native `rotate` flag is true. This covers Z0 selection, elevated placement previews and queued native building ghosts. Non-directional walls and platform-only previews do not get a misleading direction marker. Existing rotation controls remain unchanged.

The arrow follows actual native rotations **0 east / 1 north / 2 west / 3 south**, scales with the block footprint within a sensible cap and floats above the full preview roof. A small bob/forward pulse uses real time, so it stays animated while F3 pauses simulation. Cyan marks valid placement; the existing removal/red palette marks invalid placement. A dark outline keeps it readable over light materials.

Three strokes plus their outline use the existing translucent atlas batch, original white atlas swatch, depth behavior and shader. The maximum is one arrow per accepted ghost, bounded by the existing 256-ghost limit: at most 36 additional vertices per ghost. No new textures, shader, input events, placement requests, resource operations or per-arrow mesh are created. A hidden/cancelled preview stops submitting its arrow on the next frame.

## Actual validation

See `A2-17a-FIX1-TEST-MATRIX.md` and `A2-17a-FIX1-VALIDATION.json`. Complete Java17 and stock bundled Java25 regression suites, real native HP, start/stop, lower/upper floors, native rotation semantics, active/invalid/queued/multi-tile previews, actual green beam and arrow GPU pixels are tested separately from physical owner acceptance. The exact matching diagnostic is in `local/a2-17a-fix1/test-kit/` with corresponding source, notices and checksums. Existing Alpha artifacts are preserved.

Physical owner controls/import, physical Linux graphics/input and independent two-computer acceptance remain NOT TESTED in this pass. World geometry can normally occlude a repair beam; this is not an always-on-top laser. Directions appear only for genuinely rotatable blocks. Native schematic/rendering paths that do not use the shared typed ghost are not redesigned here. The pre-existing broad paused support helper's separate server-snapshot assertion is not used as proof of these focused fixes.

## Three owner checks

1. Bring a damaged friendly unit near Repair Point and Repair Turret at Z0 and on Z1/Z3 platforms. Check visible green beams and actual HP, then move away or remove power; the beam should stop normally. Recheck after save/load or F5 switching.
2. In Tab Build Mode, preview a conveyor and a larger rotatable payload block. Rotate through all four directions at Z0 and on elevated floors. Check cyan/red validity, roof clearance, animation during F3 pause and disappearance on cancel.
3. Queue a short conveyor line, change tools and switch F5/Tab/menu modes. Confirm arrows stay with previews, ordinary placement/priority/configuration still work, and no extra construction or resource spending occurs.

Reproduce with fresh run names:

```powershell
python tools/build-diagnostics.py --task a2-17a-fix1 --run my-build
python tools/test-diagnostic-runtime.py --build local/a2-17a-fix1/my-build --java bundled25 --run my-java25
python tools/launch-standalone.py --run my-repair-arrows25 --fixture tools/java/RepairArrowVisualHarness.java
python tools/launch-standalone.py --run my-repair-arrows17 --java java17 --fixture tools/java/RepairArrowVisualHarness.java
```

`RepairPreviewHarness.java` is the focused baseline probe that records real HP, native cache counter and actual beam submission; it also works with the accepted baseline JAR. All launches use disposable standalone profiles and never require Steam.
