"""Retain every match. Unresolved entries of ANY severity block release."""
import datetime
import json
from pathlib import Path
import sys

root = Path(sys.argv[1])
now = datetime.date.today()
decisions_file = Path(__file__).resolve().parent.parent / 'docs/security-decisions.json'
decisions = json.loads(decisions_file.read_text()) if decisions_file.exists() else []
rows = []
for report in sorted(root.glob('image-*.json')):
    data = json.loads(report.read_text())
    image = data.get('source', {}).get('target', {}).get('imageID', '')
    if not image.startswith('sha256:') or not isinstance(data.get('matches'), list):
        raise SystemExit(f'BLOCKED: incomplete scan report {report.name}')
    for match in data.get('matches', []):
        artifact, vulnerability = match['artifact'], match['vulnerability']
        package = artifact.get('purl', f"{artifact['name']}@{artifact['version']}")
        accepted = next((d for d in decisions if d.get('image') == image and d.get('package') == package
                         and d.get('id') == vulnerability['id'] and d.get('status') == 'not_affected'
                         and d.get('evidence') and d.get('sources')
                         and datetime.date.fromisoformat(d['reviewBy']) >= now), None)
        rows.append({'image': image, 'report': report.name, 'package': package,
                     'name': artifact['name'], 'version': artifact['version'], 'id': vulnerability['id'],
                     'severity': vulnerability['severity'], 'fix': vulnerability.get('fix', {}),
                     'source': vulnerability.get('dataSource'), 'urls': vulnerability.get('urls', []),
                     'status': 'not_affected' if accepted else 'unresolved', 'decision': accepted})
if {p.name for p in root.glob('image-*.json')} != {'image-app.json', 'image-web.json', 'image-db.json'}:
    raise SystemExit('BLOCKED: expected three complete image reports')
(root / 'disposition.json').write_text(json.dumps(rows, indent=2))
blocked = [r for r in rows if r['status'] == 'unresolved']
lines = ['# Container vulnerability disposition', '', f'Generated {now}. All severities included.', '',
         '| Image | Package | Advisory | Severity | Fixed versions | Status |', '|---|---|---|---|---|---|']
for r in rows:
    link = r['source'] or (r['urls'][0] if r['urls'] else '')
    lines.append(f"| {r['report']} | {r['name']} {r['version']} | [{r['id']}]({link}) | {r['severity']} | {', '.join(r['fix'].get('versions', [])) or 'not listed'} | {r['status']} |")
(root / 'DISPOSITION.md').write_text('\n'.join(lines) + '\n')
print(f'{len(rows)} total matches; {len(blocked)} unresolved. Raw scanner reports retained.')
if blocked:
    raise SystemExit('BLOCKED: unresolved findings; keep changes on a non-deploying branch.')
