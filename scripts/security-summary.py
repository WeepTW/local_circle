"""Summarize all image severities without treating an incomplete scan as clean."""
from collections import Counter
import json
from pathlib import Path

print('## Container vulnerability report\n')
print('Critical findings block CI. Lower severities remain visible for review.\n')
print('| Image | Critical | High | Medium | Low | Other |')
print('|---|---:|---:|---:|---:|---:|')
for service in ('app', 'web', 'db'):
    paths = sorted(Path('tmp/test-results/security').rglob(f'image-{service}.json'), key=lambda p: p.stat().st_mtime)
    try:
        data = json.loads(paths[-1].read_text())
        counts = Counter(m['vulnerability']['severity'] for m in data['matches'])
        values = [counts.pop(s, 0) for s in ('Critical', 'High', 'Medium', 'Low')]
        print('| ' + service + ' | ' + ' | '.join(map(str, values + [sum(counts.values())])) + ' |')
    except (IndexError, ValueError, KeyError):
        print(f'| {service} | INCOMPLETE | — | — | — | — |')
