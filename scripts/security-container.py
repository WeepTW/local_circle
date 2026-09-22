"""Assert actual runtime hardening, not just Dockerfile text."""
import json
import subprocess
import sys

containers = json.loads(subprocess.check_output(['docker', 'inspect', *sys.argv[1:]], text=True))
assert len(containers) == 3
for c in containers:
    service = c['Config']['Labels']['com.docker.compose.service']
    assert c['State']['Health']['Status'] == 'healthy'
    if service == 'app':
        assert c['Config']['User'] not in ('', '0', 'root', '0:0')
        assert c['HostConfig']['ReadonlyRootfs']
        assert any(s.startswith('no-new-privileges') for s in c['HostConfig']['SecurityOpt'])
        assert 'ALL' in c['HostConfig']['CapDrop']
        assert not any(c['HostConfig']['PortBindings'].values())
        subprocess.run(['docker', 'exec', c['Id'], 'test', '-r', '/app/app.jar'], check=True)
    for bindings in (c['HostConfig']['PortBindings'] or {}).values():
        for binding in bindings or []:
            assert binding['HostIp'] == '127.0.0.1'
print('PASS: non-root app, read-only filesystem, dropped capabilities, private bindings, readable JAR')
