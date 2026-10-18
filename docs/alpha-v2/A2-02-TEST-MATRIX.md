# A2-02 — Actual test matrix

PASS below applies only to the explicitly named test level. An in-memory handler
PASS never substitutes for a physical host/client result. Final machine counts,
JAR digest and canonical identity: [A2-02-VALIDATION.json](A2-02-VALIDATION.json).

| Scenario | Level / result | Evidence and boundary |
|---|---|---|
| Preserved A2-01 ACK-only / silent apply failure / revision replay / zero first ACK / stale input | **CONFIRMED BUG** | Actual old JAR handlers with mock app and native content, no sockets; baseline excerpt in validation |
| Native world + ACK + all channels + poses + live owner required | **PASS / FIXED** | ConnectionLifecycleTest and ConnectionNativeHandlersTest; early state blocked |
| Delayed native completion and missing initial state replay | **PASS** | Handler tests use existing HELLO, same-epoch ACK preserves progress |
| Ground unit and aircraft initial owner pose | **PASS** | Native-content fixture, Dagger/Flare ownership changes; no physical flight test |
| Initial elevated structure | **PASS** | Actual layer decoder restores Z2 wall/rotation before READY, Z0 remains empty; pre-READY construction refused |
| Same-process disconnect/rejoin | **PASS** | Five handler-fixture reconnects and 20 pure lifecycle cycles; one binary handler remains registered |
| World reload / epoch / input sequence reset | **PASS / FIXED** | WorldLoad events, stale epoch refused, fresh epoch input accepted, previous movement cleared |
| State consumer fails, then reconnects | **PASS / FIXED** | FAILED, sanitized reason, new attempt reapplies; old queued callback cannot disconnect newer attempt |
| Protocol/digest mismatch | **PASS** | Client invalid protocol/mismatching ACK failure, server mismatch kick in fixture; strict canonical artifact tests also retained |
| Timeout/death/respawn/new epoch | **PASS** | Deterministic native-world, ACK, initial-state, progress/total and owner-pose bounds; spectator wait; no sleep-based timing claims |
| Opt-in export, new lifecycle fields and privacy | **PASS** | NetworkDiagnosticSessionTest; no ordinary-play export, sanitized fixed keys, IO failure isolation |
| Full existing suite, Java 17 | **PASS** | Final accepted build log/result; compiled --release 17 against installed 160.7 desktop API |
| Full existing suite, stock Steam Java 25 | **PASS** | Same compiled code/tests, stripped bundled runtime, no external Java modules added |
| Actual final JAR and extracted folder canonical identity | **PASS** | Java ModFingerprint and independent packaging hash; licenses/metadata preserved |
| Native mouse/camera lifecycle contracts | **PASS (automated only)** | Existing full-suite tests unchanged; not a manual import/capture test for A2-02 |
| Native selector/socket capability | **UNAVAILABLE** | IOException before a connection; no permissions requested; not a mod/network diagnosis |
| Actual Windows host / Windows client, same PC | **NOT TESTED** | Socket restriction; in-memory tests listed separately above |
| Actual Windows host / friend Windows client | **NOT TESTED** | No second participant; historical occasional success is not this build's evidence |
| Friend host / developer client failure | **NOT REPRODUCED** | Exact physical failure stage still unknown; new manual kit supplied |
| Windows host / Linux client | **NOT TESTED** | Historical owner-reported success/delay only |
| Linux host / Windows client | **NOT TESTED** | Separate machine/participant required |
| Steam invitation, each host direction | **NOT TESTED** | Account/physical UI interaction required |
| Repeated actual hosting after menu, reconnect without game restart | **NOT TESTED** | Handler/event regression PASS does not establish native UI/transport lifecycle |
| Actual F5, Tab, control, resource costs, save/reload and layered building | **NOT TESTED** | Existing automated native gameplay/save tests pass; physical acceptance pending |
| Current JAR native import/restart / Workshop subscription | **NOT TESTED** | No installation, subscription or primary profile changed |
| Paired vanilla vs mod link comparison | **NOT TESTED** | No usable transport in this environment |
| Primary saves and published artifacts | **PRESERVED** | No primary profile access; existing Alpha artifact snapshot verified unchanged |

The first full revised fixture run rejected an occupied test location, as normal
placement validation should. The fixture was moved to a free cell; no construction
rule was relaxed. A later export-test iteration attempted to assert a metric
before sampling it; the final test explicitly samples that metric. Intermediate
logs remain local. Only the final accepted runs count as the full-suite PASS.

No physical movement latency, throughput, FPS gain or friend-host fix is claimed.
