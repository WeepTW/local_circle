"""Architectural/security invariants. Does not replace runtime security tests."""
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent
failures=[]
for p in (root/'backend/src/main/java').rglob('*.java'):
    text=p.read_text()
    if 'Map<String' in text: failures.append(f'Untyped contract: {p.name}')
    if 'presentation' in p.parts and ('JdbcTemplate' in text or 'java.sql' in text): failures.append(f'JDBC in presentation: {p.name}')
    if re.search(r'(?i)\b(SELECT\s+.+\s+FROM|INSERT INTO|UPDATE \w+ SET|DELETE FROM)\b',text): failures.append(f'Inline query outside SP: {p.name}')
    if re.search(r'log\.(?:info|warn|error|debug).*getMessage\(',text): failures.append(f'Unredacted logging: {p.name}')
for p in (root/'frontend/src').rglob('*.vue'):
    if 'v-html' in p.read_text():failures.append(f'Unsafe HTML: {p.name}')
sql=(root/'DB/002_stored_procedures.sql').read_text()
assert 'PREPARE ' not in sql and 'EXECUTE ' not in sql
assert 'COMMIT;' not in sql
assert not failures, '\n'.join(failures)
print('PASS: typed contracts, layer separation, static SP SQL, escaped Vue templates, sanitized logging')
