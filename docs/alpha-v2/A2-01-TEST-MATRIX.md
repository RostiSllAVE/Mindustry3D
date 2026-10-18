# A2-01 — Results matrix

2026-10-18. PASS applies only to the described scope. NOT TESTED means unavailable
or blocked, not a successful test inferred from codec or localhost fixtures.

## Requested physical configurations

The owner previously reported Windows-host success, friend-host failure, severe
Steam-invitation delay and a Linux-client delay. Those observations motivate this
task; they are not new controlled A2-01 measurements.

| Scenario | Path | Vanilla | Matching mod | Fresh map | Combat | Construction/FPV | Longer session |
|---|---|---|---|---|---|---|---|
| A: Windows -> Windows, two physical PCs | Direct/LAN | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED |
| B: Windows -> Linux | Direct/LAN or unknown actual route | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED |
| C: Linux -> Windows | Direct/LAN or unknown actual route | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED |
| D: Friend's Windows host -> owner's Windows client | Remote route unknown | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED |
| E: Windows -> remote Windows via Steam invitation | Direct/relay not determined | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED |

No second machine, friend profile or Steam-account operation was used. The new
same-PC dedicated server plus two clients was also **NOT TESTED**: Java failed
before the world/handshake on sandbox-denied loopback selector setup. Its startup
attempt has an environmental FAIL; it is not evidence of a mod connection bug.
No elevated socket run was requested. Vanilla network comparison is unavailable
for the same reason; a protocol microbenchmark is not its substitute.

## Automated checks and what they prove

| Check | Status | Actual scope |
|---|---|---|
| Complete JDK 17 diagnostic build/test suite | PASS | Final build result records 117 programs; no network/GPU/physical gameplay proof |
| Stock Steam Java 25 diagnostic tests | PASS | Final corrected-runtime run documented in evidence; stripped-runtime expectation supplied to existing optional-input test |
| Initial Java 25 suite invocation | FAIL | Harness omitted `fpv.test.expectNoLogging=true`; existing test expected full-runtime Logger. Not an initialization regression; retained in local evidence |
| Protocol/display behavior | PASS | 24 real codec/display assertions; owner 100 ms buffer, hold on outage, wire sizes, atomic chunk delivery |
| Numeric collector | PASS | 16 disabled/bounded/percentile/privacy/reassembly assertions |
| Report session lifecycle | PASS | 12 settings/export/privacy/failure-isolation assertions in a unique temporary profile |
| Fingerprint strictness | PASS | Existing 23 JAR/folder/mismatch/failure-isolation checks; no weakening of compatibility |
| Mouse capture lifecycle | PASS | Existing 218 lifecycle checks; physical import/menu/Alt+Tab of diagnostic JAR NOT TESTED |
| Native campaign custom-chunk/events | PASS | 8 in-memory native writer/reader/reset/restore checks; not planetary UI |
| Native campaign block accounting | PASS | Existing 21 native-content checks; planetary transitions deliberately not invoked |
| Layered runtime/construction | PASS | Existing native simulation/structural/economy/codec suites; not network placement acceptance |
| Codec time/allocation benchmark | PASS | Java 17, 10 warm-ups + 30 samples/size; no socket, GL, world scan or gameplay FPS |
| Real diagnostic Mod Import -> restart -> F5 | NOT TESTED | New stock-runtime application requires forbidden loopback setup in this environment |
| Diagnostic Workshop subscribe/download | NOT TESTED | No publishing or account interaction authorized |
| Actual LAN/WAN RTT / packet-level jitter / loss | NOT TESTED | No live connection or packet trace; app ping observer only |
| Host simulation/client frame/GPU attribution on remote scene | NOT TESTED | Instrumentation prepared; GPU timing intentionally not added |
| Live transport queue accumulation | NOT TESTED | No native send-queue API measured; partial assembly gauges only |
| Matching diagnostic JAR/folder canonical identity | PASS | Same extracted compiled payload; licenses included; packaging cosmetics ignored |
| Published Alpha artifacts retained | PASS | 2,065 pre-existing artifact files verified against saved hashes |

## Campaign acceptance matrix

| Scenario | Status | Reason / evidence |
|---|---|---|
| Synthetic sector layer chunk save/restore | PASS | Native chunk API in memory, 8-check test |
| Reset -> WorldLoad -> custom restore | PASS | Matches inspected ordinary 160.7 map/custom read order |
| Synthetic campaign -> empty custom context | PASS | Upper state cleared; restoring same chunk twice does not duplicate |
| Native world stream excludes upper custom chunk | PASS | `writeCustomChunks(..., true)` omits it; FPV channel required |
| Actual enter/leave planetary sectors | NOT TESTED | Requires full game/planet lifecycle in an isolated profile |
| Start fresh real campaign sector | NOT TESTED | No physical/native application run available |
| Load copied owner's campaign through full disk SaveIO | NOT TESTED | No primary or copied owner campaign used this task |
| Friend's affected campaign | NOT TESTED | Not provided and friend machine unavailable |
| Save/leave/client reconnect across real sector transition | NOT TESTED | Live networking blocked |
| Change physical host / Steam invite / Linux sector host | NOT TESTED | Hardware/account operations unavailable |

The first published Alpha's prior local two-client/owner acceptance remains
historical evidence only. This task does not turn those cases into current
PASS entries or certify broad Linux/Android compatibility.
