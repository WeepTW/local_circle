"""Fail closed when database integration suites accidentally skip."""
from pathlib import Path
import xml.etree.ElementTree as ET
files = list(Path('backend/target/surefire-reports').glob('TEST-*.xml'))
assert files, 'Missing Java reports'
reports = [ET.parse(p).getroot() for p in files]
assert all(int(r.get('tests', 0)) > 0 and int(r.get('skipped', 0)) == 0 and
           int(r.get('failures', 0)) == 0 and int(r.get('errors', 0)) == 0 for r in reports), 'Failed or skipped Java tests'
assert any('IntegrationTest' in r.get('name', '') for r in reports), 'Database integration suite did not execute'
print('PASS: all selected Java tests executed without skips')
