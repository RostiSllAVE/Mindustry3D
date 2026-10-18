# A2-06 — Linux mouse acceptance test

This is an unreleased diagnostic build. Back up your current mod and use a disposable
Mindustry profile or a new test save. Do not replace published Alpha packages.
The same Java17-compatible JAR is used on Windows and Linux. No JDK, shell script,
SDL replacement or additional native package is part of player installation.

1. On the Linux laptop, import `mindustry-fpv-a2-06-diagnostics.jar` using Mindustry
   160.7 **Mods → Import Mod → Import File**, replace the previous mod, then restart
   the game normally. Start a disposable map and press **F5**. Rotate the camera
   through several complete turns in both directions, including very slow motion,
   fast motion, looking up/down and stationary flight. The cursor must be hidden,
   continuous motion must not stop at screen edges, and it must not leave the game.
2. Exercise **Tab/Build Mode**, command/group-selection mode, **Esc/menu**, pause,
   F5 back to 2D, **Alt+Tab**, minimize and return. The pointer must be usable in the
   UI/other application. Return to focused, unpaused FPV flight each time: capture
   should resume without a sudden camera jump. Build/drag/configure/select units,
   turn while flying, switch units, and check normal controls. Close the game both
   from captured flight and from a menu; the desktop pointer must remain usable.
3. Where your login screen offers both sessions, repeat in **X11** and **Wayland**,
   recording the actual session/compositor, game/runtime version and result. A
   Wayland session running Mindustry through XWayland may report SDL **x11**: this
   validates XWayland, not native Wayland. Inspect the local game log for
   `[FPV] Mouse capture: host SDL relative / x11|wayland / JNA|java.base FFM` or
   the single fallback warning. Send that line and the failing step if necessary;
   do not upload full profiles/saves/private logs.

If `HOST MOUSE · arrows at edges` appears, relative capture was unavailable and
bounded pointer plus keyboard look remains active. A denied compositor protocol,
unavailable exported SDL functions, an unreadable/deleted/ambiguous host-library
mapping, missing native-access permission or a non-SDL backend must not crash the
mod. This fallback cannot provide unlimited physical mouse rotation. No cursor
warping is attempted on Wayland. Report a fallback rather than treating it as PASS.

Repeat steps 1–2 on Windows with the usual Steam runtime; if available repeat with
full Java17. Verify the existing Windows native capture rather than Linux's backend.
Do not install another runtime just for this check. Physical Linux and Windows UI
acceptance for this build remains pending until these checks are actually run.

The payload changed: any multiplayer test participants must use this exact diagnostic
JAR. Existing strict code/version matching is unchanged. Multiplayer testing is not
required to establish local mouse capture and is not certified by these steps.
