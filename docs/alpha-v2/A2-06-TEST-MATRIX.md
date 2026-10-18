# A2-06 — Test matrix

PASS is limited to its evidence; mocked SDL results do not certify a real compositor.

| Check | Result | Evidence / boundary |
|---|---|---|
| Complete Java17 regression suite | PASS | 127 programs, final acceptance build, 17.0.20.1+1 |
| Same suite against final JAR on stock Steam Java25 | PASS | 127 programs, zero failures; Logger absent, existing native-access flag |
| Linux relative-capture lifecycle | PASS, CONTRACT | 671 injected-SDL checks on each runtime; both x11/wayland driver labels |
| Continuous right/down motion, no screen-edge saturation | PASS, CONTRACT | 300 consecutive samples per driver; acquisition backlog discarded; stable capture not toggled |
| Tab/Build, F5/2D, command, pause, menu release | PASS, CONTRACT | Existing shared mode gates unchanged; tests send release signals, not physical key presses |
| Focus loss/Alt+Tab/minimize, resume/reacquisition | PASS, CONTRACT | Native + host focus fences, zero stale movement; explicit fresh flight request |
| Unsupported drivers, wrong window/library, capture in use | PASS, CONTRACT | No capture side effects before validating ownership/driver |
| SDL denial, lost capture, native sampling failure, cleanup | PASS, CONTRACT | Failure isolates once, fallback unarmed; incomplete release retried on close |
| Mapping selection and installed host API contract | PASS | 13 checks, repeated maps/spaces/ambiguous/deleted mapping rejection; actual host reflection |
| Factory selection/fallback | PASS | 6 checks/runtime; forced Arc and unsupported/headless Linux; actual Windows factory initialization |
| Windows complete Java17 input | PASS, LIMITED | Actual existing JNA factory initialization/read-only queries/idle release plus 218 lifecycle-fixture checks |
| Windows stock Java25 input | PASS, LIMITED | Actual existing java.base FFM binding/factory/query/idle release; no JNA dependency initialized |
| Final JAR / folder strict identity | PASS | Actual Java ModFingerprint under stock25; same canonical payload |
| Runtime scope preservation | PASS | 969 old classes unchanged, including network/physics/combat/render/camera/Windows implementations; only existing factory + generated descriptor and 6 new input class entries |
| Packaging/licenses/Java17 bytecode | PASS | 1,032 entries; max class major61; existing Windows/Linux x64 JNA resources; notices unchanged; no new dependency |
| Previous releases and checkpoints | PASS | 2,065 prior Alpha snapshot files and 12 A2-02/03/04/05 assets hash-identical; no history rewriting |
| Native Linux JNA / FFM symbol resolution and startup | NOT TESTED | No accessible Linux runtime/desktop; native bridge functions must resolve on target machine |
| Physical X11 / XWayland capture and UI | NOT TESTED | Owner laptop required; XWayland is distinct from native Wayland |
| Physical native Wayland compositor capture and UI | NOT TESTED | Relative-pointer/locking support or rejection must be checked physically |
| Physical Windows import/restart, mouse, F5/Tab, gameplay | NOT TESTED, THIS BUILD | Existing owner acceptance predates this diagnostic; read-only native tests are not UI acceptance |
| Linux/Windows normal GUI startup here | NOT TESTED | WSL inventory denied; desktop socket-selector setup unavailable; not bypassed |
| Real two-computer multiplayer | NOT TESTED | Out of input scope; networking class bytes/protocol unchanged; exact payload matching still required |

Full logs are kept locally in `local/a2-06/acceptance-build.log` and
`local/a2-06/steam25-tests.log`; final machine evidence is
[A2-06-VALIDATION.json](A2-06-VALIDATION.json). Native query tests deliberately do
not capture, confine or warp the owner's desktop pointer. See the [report](A2-06-LINUX-MOUSE-REPORT.md)
and [manual Linux procedure](A2-06-MANUAL-TEST.md).
