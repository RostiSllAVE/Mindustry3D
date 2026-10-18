# A2-06 — Linux FPV mouse capture

Status: local diagnostic implementation; **physical Linux acceptance NOT TESTED**.
Baseline: accepted A2-05 `92eac12040bd85ca45b28e304dbee79368001807`, branch `main`.
The internal version remains 0.18.31/protocol 4. This is not a published Alpha update.

## Confirmed cause and API audit

The factory previously allowed native input only on Windows. Linux always received
`ArcFlightInput`: differences between bounded absolute pointer positions, with no
cursor confinement. Moving to an edge necessarily stopped continuous mouse aiming.
The existing flight/UI mode gates were already shared; the missing part was the Linux
capture backend, not a network or camera calculation.

The installed 160.7 `desktop.jar` was inspected with Java class disassembly.
`arc.Input` has no cursor-catching/recentering API. `SdlInput` derives its deltas from
absolute event coordinates; using `Core.input.deltaX/Y` would retain the edge limit.
The Java SDL JNI wrapper exposes window/focus queries but not relative mode, relative
state or mouse warping. The host pumps events before its application update.
The pinned Arc build source (`8eb00ffff0`) links the Linux JNI bridge against SDL2.
The installed Windows jar is not evidence that Linux symbol resolution was exercised.

SDL2 already provides the required behavior: [relative mode hides/confines the cursor
and reports continuous movement](https://wiki.libsdl.org/SDL2/SDL_SetRelativeMouseMode),
while [relative-state sampling returns accumulated motion since the previous
sample](https://wiki.libsdl.org/SDL2/SDL_GetRelativeMouseState).
The bridge/API investigation also used Arc's [input source](https://github.com/Anuken/Arc/blob/8eb00ffff0/backends/backend-sdl/src/arc/backend/sdl/SdlInput.java)
and [Linux link configuration](https://github.com/Anuken/Arc/blob/8eb00ffff0/backends/backend-sdl/build.gradle).

## Implementation

- `FlightMouseInputs` keeps the existing Windows selection and adds Linux only for
  the actual SDL application/main thread. Other backends retain optional fallback.
- `LoadedSdlLibrary` finds one already mapped `libsdl-arc[64].so` in `/proc/self/maps`.
  Repeated segments are deduplicated; ambiguous/deleted/inaccessible mappings fail
  safely. No guessed library search, external assets or SDL installation by name.
- `JnaSdlMouseApi` uses the already bundled JNA on complete Java17+ and opens the
  exact bridge with Linux `RTLD_NOW | RTLD_NOLOAD`. `ForeignSdlMouseApi` uses reflected
  java.base FFM on reduced Java22+ without java.logging, preserving Java17 bytecode.
  Its exact mapped-path library lookup reuses the loaded bridge/dependency. Both
  validate SDL video initialization and the original host window's ID/pointer pair.
  Neither calls SDL_Init/Quit, creates a window, replaces libraries or pumps events.
- `LinuxSdlMouseCapture` accepts the actual SDL `x11` or `wayland` driver, enables
  relative mode on flight capture and samples positive-right/down accumulated motion.
  Entry backlog is discarded; stable capture is not toggled every frame. Motion
  storage/native buffers and call arguments are reused. No watcher, hook or worker.
- Existing `FpvMod` gates release in Build, command selection, menus, pause, F5/2D,
  lost focus/minimization, death and shutdown. Resume requires a fresh flight request.
  The Linux backend additionally checks SDL keyboard focus and clears deltas on loss.
  It does not take over relative mode already enabled by another component.
- Native acquisition/query/read failures are isolated by existing OptionalFlightInput.
  There is one warning and no native retry loop. Bounded HOST MOUSE plus keyboard
  look remains available; unsupported capture must not abort rendering/F5/settings.
  A release error is reported and close retries incomplete release, best effort.

No networking, physics, combat, renderer, camera controls or save-format source was
changed. Windows JNA/FFM implementations and shared mode gates remain byte-identical.
The input design descriptor and diagnostic task selector were updated.

## X11 / Wayland and dependencies

Use SDL's actual driver, not XDG_SESSION_TYPE: XWayland may run under a Wayland login
but report x11. Native Wayland depends on the compositor's relative-pointer/locking
support. The real SDL enable result decides support; unsupported/denied capture falls
back visibly rather than claiming capture. No desktop pointer warps are attempted.
X11 and native Wayland are separate physical acceptance targets.

Required host: supported Mindustry/Arc SDL runtime, original accessible mapped bridge,
SDL2 relative symbols, focused own window, and allowed native calls on the host thread.
JNA resources/licenses were already included. Reduced Java22+ must permit FFM native
access as the host normally does; denied access still permits fallback startup. A
reduced Java17 runtime without java.logging has neither JNA nor FFM capture; normal
startup/fallback remains available. No new native dependency or separate Java install
is shipped. Physical Linux symbol availability and compositor behavior remain pending.

## Validation and limitations

Final acceptance: **127 regression programs PASS on Java17 and the same 127 PASS
on stock Steam Java25**, with zero failures. New Linux lifecycle fixtures have 671
checks per runtime, mapping/host API tests 13, and factory tests 6. Actual Windows
JNA/FFM initialization and read-only queries pass. 969 pre-existing class entries
remain byte-identical. The final JAR SHA-256 is
`034fd153cf1404c4163c60b52bff00d5e027441529acd407a4905d1338f40906`.
Actual JAR/folder identity matches under stock25. Prior 2,065 Alpha snapshot files
and all 12 A2-02–A2-05 diagnostic archives are hash-identical.

See [test matrix](A2-06-TEST-MATRIX.md) and [machine evidence](A2-06-VALIDATION.json)
for final program counts, checksums and unchanged-payload verification. Contract
fixtures test the real capture state machine with injected SDL results, including
both accepted drivers, repeated turns, mode release, Alt+Tab/focus fencing, clean
reacquisition, unsupported drivers/window identity, capture failure and shutdown.
They do not simulate the physical compositor or certify actual native Linux bindings.

The real Windows factory is instantiated without capture/warping on both Java17/JNA
and Steam Java25/FFM. Existing native read-only Windows tests and all regressions run.
Complete import/restart, physical mouse/camera comfort and live gameplay on this build
remain NOT TESTED. WSL inventory is denied in this environment; it was not bypassed.
The desktop networking-selector preflight also prevents an honest automated normal
game-launch claim here. No Linux desktop was operated and no machine was provisioned.

The fallback is intentionally limited: it cannot produce unlimited physical mouse
rotation. A Linux physical failure or fallback is a remaining blocker for certifying
reliable Linux capture, even if all Java contract tests pass. Broader Linux support
and separate computer acceptance are not claimed. No performance improvement claimed.

## Owner test / artifacts

Import `local/a2-06/test-kit/mindustry-fpv-a2-06-diagnostics.jar` normally and restart.
Follow [three short owner steps](A2-06-MANUAL-TEST.md): continuous turns; UI/F5/Tab,
command/pause/Alt+Tab/reacquire/shutdown; repeat X11 and Wayland where available.
The same JAR is also the Windows regression candidate. Pair it only with matching
payloads for multiplayer; strict fingerprint validation is unchanged.

The local test kit contains this JAR, corresponding source, checksums, notices and
manual instructions, with no host game/runtime/profile. Prior Alpha and A2-05 assets
are preserved. Nothing was pushed, published or uploaded to Workshop.
