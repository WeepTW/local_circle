"""Bounded security probes against an isolated loopback deployment only."""
import json
import os
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from urllib.parse import urlparse

base = os.environ['BASE_URL']
assert urlparse(base).hostname in ('127.0.0.1', 'localhost'), 'Loopback targets only'

def request(path, method='GET', data=None, headers=None):
    req = Request(base + path, data=data, method=method, headers=headers or {})
    try:
        response = urlopen(req, timeout=10)
    except HTTPError as e:
        response = e
    with response:
        return response.status, response.headers, response.read(100000).decode('utf-8')

for path in ('/preferences', '/api/v1/products'):
    status, headers, body = request(path, headers={'X-Demo-User-Id': '1'})
    assert status == 200
    assert headers.get('X-Content-Type-Options') == 'nosniff'
    assert headers.get('X-Frame-Options') == 'DENY'
    assert "frame-ancestors 'none'" in headers.get('Content-Security-Policy', '')
    assert 'unsafe-inline' not in headers.get('Content-Security-Policy', '')
    assert '/' not in headers.get('Server', '')  # no server version disclosure
    if path.startswith('/api'):
        assert headers.get('Cache-Control') == 'no-store'

status, headers, _ = request('/api/v1/products')
assert status == 401 and headers.get('Cache-Control') == 'no-store'
for path in ('/.env', '/.git/config', '/backend/pom.xml', '/DB/001_ddl.sql'):
    _, _, body = request(path)
    assert not any(marker in body for marker in ('MYSQL_PASSWORD', '[core]', '<project', 'CREATE TABLE'))

status, _, _ = request('/api/v1/preferences', 'POST', b'x' * 20000,
                      {'X-Demo-User-Id': '1', 'Content-Type': 'application/json'})
assert status == 413, 'Proxy request limit not enforced'
print('PASS: headers, cache controls, source-file privacy and request size limits')

status, headers, body = request('/api/v1/accounts/10/number', headers={'X-Demo-User-Id': '1'})
assert status == 200 and headers.get('Cache-Control') == 'no-store'
assert json.loads(body)['accountNumber'] == '0000009666'
status, _, body = request('/api/v1/accounts/10/number', headers={'X-Demo-User-Id': '2'})
assert status == 404 and '0000009666' not in body
_, _, body = request('/api/v1/accounts', headers={'X-Demo-User-Id': '1'})
assert '0000009666' not in body and 'ciphertext' not in body and 'key_id' not in body
print('PASS: owner-scoped no-store account reveal; list/error responses remain masked')
