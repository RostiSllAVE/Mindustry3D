# A2-07 — Flying AI attacks on ground structures

Local diagnostic; physical gameplay acceptance pending. Baseline: A2-06
`8108e4994dd04adc767e0d534c421be74e7d35de`, branch `main`.
Internal version 0.18.31 / protocol 4; no public release or Workshop update.

## Confirmed cause

Flare's native FlyingAI acquired valid ground targets and requested shooting. Its
non-omni steering already rotates the real aircraft toward an attack run. The 3D
altitude adapter instead followed velocity heading and smoothed yaw a second time.
During turns the physical 3D frame could lag the native hull heading by roughly
60–75 degrees in the diagnostic trace. Fixed guns then correctly rejected the target
as outside their existing narrow sector. The unit circled with shoot intent but
usually emitted only its initial three-round burst.

A separate aiming mismatch remained after isolating heading: native `keepVelocity`
adds the aircraft's actual XYZ velocity to its projectiles. AI barrels aimed directly
at a stationary structure without compensating that inherited motion. Horizontal
closure could nearly double while descent was not increased correspondingly, causing
shots to pass above low turrets/factories. An isolated fixture with inheritance removed
confirmed this mechanism; **inheritance was not removed in the implementation**.

Evidence comes from installed Mindustry160.7 content, native FlyingAI/Weapon code and
headless attack runs. The same new regression fails against accepted A2-06 after the
initial burst, then passes with the fix. No player save was used.

## Minimal fix

1. Native non-omni, armed, AI-controlled aircraft retain their actual native hull yaw
   in the altitude state. There is no second velocity-heading rotation lag. Native
   movement, turn speed, climb/descent, terrain clearance and bank bounds stay intact.
   Omni aircraft and externally supplied/player poses keep their previous path.
2. AI non-bomber aircraft projectile mounts solve relative target motion minus the
   owner's inherited XYZ velocity, from the physical hinge. Moving-target velocity
   still respects predictTarget; actual emitted projectile speed is used at launch.
   Bombs, artillery, lasers, walkers and manual FPV directions keep their old path.

Damage, projectile speed, keepVelocity, spread, range/lifetime, reload, native
shooting-speed conditions and existing yaw/pitch stops are unchanged. Projectiles
still resolve real terrain/building/upper-volume contacts; no artificial damage,
through-wall shortcut or wider firing sector was added. Native 160.7 no longer exposes
Unit.ammo/Rules.unitAmmo: this pass does not invent an ammunition system, and its
existing host weapon conditions remain responsible for firing permissions.

Only the aircraft aim and altitude-heading modules changed at runtime. Network,
mouse capture, renderer, saves and combat-contact/damage modules were not edited.
The orientation correction is necessary because the same physical body frame drives
both mounted-gun sectors and visible hulls; a weapon-only frame override would detach
guns from their body.

## Reproduction and measurements

The fixture runs native FlyingAI steering/targeting, actual weapon updates and actual
native/proxy projectile updates against real building HP. XY position/drag and native
previous-frame displacement are integrated explicitly; this is not a full graphical
engine/physics/async simulation. Seeded objectives isolate firing from acquisition;
a separate unseeded native-AI/indexer scene verifies acquisition and target switching.
Each row runs up to 2,400 simulation ticks (40s), stopping on target death. No DPS or
real-game frame-rate improvement is inferred. Early isolated probes and failed fixture
iterations are retained locally; final counts are in the measured CSV and matrix.


| Ground target | Before: shots / actual HP damage | After: shots / actual HP damage |
|---|---:|---:|
| copper-wall | 3 / 24.773 | 36 / 324.000 |
| duo | 3 / 0.000 | 27 / 144.000 |
| graphite-press | 3 / 22.843 | 18 / 162.000 |
| core-shard | 3 / 23.697 | 75 / 675.000 |

The four vanilla 2D rows are exactly unchanged in the same fixture. Counts are
projectiles counted by native mounts, not manufactured hits. Overkill can make
HP damage exceed the initial HP; stopping on death prevents DPS comparisons.

See [measurements](A2-07-ATTACK-MEASUREMENTS.csv), [test matrix](A2-07-TEST-MATRIX.md)
and [machine results](A2-07-VALIDATION.json). Frozen-origin velocity/altitude cases
are isolated ballistic fixtures, with explicit movement deltas for the host's native
firing-speed gate; they do not claim actual AI traversal.

## Boundaries / remaining work

Physical Windows/Linux import/restart, camera comfort and live AI battle acceptance
are NOT TESTED for this build. Socket-selector setup is unavailable here; no real
multiplayer or two-computer acceptance is claimed. Native client/server authority
fixtures and the existing network regression suite are separate evidence.

An aircraft can still lack a legal shot when directly overhead, too high, outside
range or behind a solid barrier. These restrictions remain intentional. Existing
altitude descent can recover a firing opportunity; no speculative new altitude or
attack-position planner was necessary for the reproduced ordinary attack runs.
The elevated-building test supplies a real native AI objective on an upper floor
and verifies physical damage there, without a Z0 alias. General native FlyingAI
**discovery of upper-only objectives is not expanded/certified** by this task; that
is a distinct acquisition limitation rather than the reproduced valid-target failure.

## Test and install

Final regression results: **128 programs PASS on complete Java17 and the same
128 PASS on stock Steam Java25**, zero failures. The new attack fixture has 54
checks; altitude math has 3,999 checks including 722 new heading/invariant checks.
The existing six-aircraft native 920-check regression also passes. 966 pre-existing
classes, including all input/network/render/contact-damage/save implementations,
remain byte-identical. Final JAR SHA-256:
`4a3d26424623d96207fc2f383c1a5651a6433500fc08af1dff58bdc4e3a2c9fd`. Actual JAR/folder identity is verified.

Import `local/a2-07/test-kit/mindustry-fpv-a2-07-diagnostics.jar` normally and restart.
Follow the [three owner checks](A2-07-MANUAL-TEST.md): autonomous attacks and HP;
obstacles/elevation; FPV and other aircraft. All peers need this same diagnostic
payload for multiplayer; strict identity checks remain unchanged.

The kit includes corresponding source, checksums and existing GPL/third-party
notices. Previous Alpha and A2-06 assets are preserved. Nothing was pushed/published.
