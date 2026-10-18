"""Deterministic byte-service/latency MODEL using measured native-LZ4 frame sizes.
No socket, TCP congestion/retransmission emulation, Steam measurement or gameplay acceptance.
"""
import argparse
import csv
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--before', required=True, type=Path)
parser.add_argument('--after', required=True, type=Path)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
if args.output.exists():
    raise SystemExit('Preserve the existing model output; select a new path')
rows = []
for mode, path in [('before', args.before), ('after', args.after)]:
    with path.open(encoding='utf-8-sig') as stream:
        frames = [r for r in csv.DictReader(stream) if r['scene'] == 'moving']
    for frame in frames:
        size, chunks = int(frame['nativeLz4Bytes']), int(frame['chunks'])
        for rate in [32000, 125000, 500000, 2000000]:
            for rtt, stall in [(20, 0), (250, 0), (250, 250)]:
                available = 0.0
                ages, persistent = [], []
                for k in range(200):
                    generated = k / 10
                    start = max(generated, available)
                    if stall and k % 20 == 10:
                        start += stall / 1000
                    available = start + size / rate
                    ages.append(available + rtt / 2000 - generated)
                    if k % 10 == 0:
                        # Illustrative reliable 256-byte state message, queued behind this frame.
                        available += 256 / rate
                        persistent.append(available + rtt / 2000 - generated)
                # Estimate chunk span with uniform chunk compression; NOT a real packet trace.
                chunk_span = size / rate * (chunks - 1) / chunks
                rows.append(dict(mode=mode, units=int(frame['units']), bytesPerSecond=rate,
                                 rttMs=rtt, assumedStallMsEvery2Seconds=stall,
                                 snapshotMeanAgeMs=sum(ages) / len(ages) * 1000,
                                 snapshotP95AgeMs=sorted(ages)[189] * 1000,
                                 reliableStateMaxDelayMs=max(persistent) * 1000,
                                 finalQueueServiceSeconds=max(0, available - 20),
                                 approximateFrameChunkSpanSeconds=chunk_span,
                                 exceedsExistingFiveSecondAssemblyDeadline=chunk_span > 5))
with args.output.open('x', encoding='utf-8', newline='') as stream:
    writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)
print(f'{len(rows)} deterministic MODEL scenarios; no real latency or throughput measurement')
