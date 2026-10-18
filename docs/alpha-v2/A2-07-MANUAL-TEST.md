# A2-07 — Owner gameplay check

Use a disposable sandbox map/save and back up the current mod. Import
`local/a2-07/test-kit/mindustry-fpv-a2-07-diagnostics.jar` through **Mods → Import Mod**,
replace the old diagnostic and restart. Use this exact JAR on all multiplayer peers;
its canonical code fingerprint differs even though internal version/protocol stay
0.18.31/4. No published Alpha artifacts were overwritten.

1. **Autonomous attack runs:** On a flat open map, place friendly walls, Duo turrets,
   a factory and a core; spawn enemy Flare aircraft at some distance. Keep a core or
   another normal long-range objective so vanilla FlyingAI continues its attack
   route. Observe in F5/3D and 2D. Check actual building HP and several passes, not
   merely muzzle flashes. Remove/destroy a target and observe native retargeting.
2. **3D limits:** Repeat near raised terrain and an elevated structure. Place a solid
   barrier between attacker and target. Bullets must hit the foreground obstacle or
   miss; they must not damage a covered building through the wall. An aircraft
   directly overhead or too high can still lack a legal firing solution. Check
   that normal movement/descent produces later attack opportunities. This pass does
   not grant unlimited pitch or invent a new altitude/approach planner. For an
   upper-only structure, use the existing command/target workflow if necessary:
   native FlyingAI's general discovery of upper-only buildings is not expanded here.
3. **Control/regressions:** Possess a Flare, fly and fire in FPV, then observe in third
   person and release it back to AI. Try Zenith or Eclipse and a Horizon bomber.
   Confirm controls/mouse/Tab remain as before. Flare's native minimum firing speed
   still applies: hovering motionless need not fire. If two computers are available,
   repeat on the host and client and check that only the server applies damage.

Record the failed case, actual target HP and whether the unit is piloted or AI.
Physical Windows/Linux gameplay and two-computer acceptance have NOT been performed
for this build. Native headless fixtures use explicit XY integration and injected
poses for isolated ballistic/upper-floor checks; they are not a physical playtest.
