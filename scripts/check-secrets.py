"""Check known local secrets without printing secret values; inspect source + build output."""
from pathlib import Path
import subprocess
import os
root=Path(__file__).resolve().parent.parent
secrets=[]
env_files = [root/'.env', root/'tmp/acceptance.env']
if os.environ.get('ENV_FILE'): env_files.append(Path(os.environ['ENV_FILE']))
for env in env_files:
    if env.exists():
        for line in env.read_text().splitlines():
            key,_,value=line.partition('=')
            if 'PASSWORD' in key and len(value)>=16: secrets.append(value.encode())
key_files = [root/'.secrets/account.key']
if os.environ.get('ACCOUNT_KEY_FILE'): key_files.append(Path(os.environ['ACCOUNT_KEY_FILE']))
for key_file in key_files:
    if key_file.is_file():
        for line in key_file.read_text().splitlines():
            if line.startswith('key='): secrets.append(line.partition('=')[2].encode())
tracked=subprocess.check_output(['git','-c',f'safe.directory={root}','ls-files','-z'],cwd=root).decode().split('\0')
paths=[root/p for p in tracked if p]
paths+=list((root/'frontend/dist').rglob('*'))
failures=[]
for p in paths:
    if p.is_file():
        content=p.read_bytes()
        if any(secret in content for secret in secrets): failures.append(str(p.relative_to(root)))
assert not failures, 'Known local secret detected in: '+', '.join(failures)
assert not any(p.startswith('.secrets/') or p.startswith('backups/') for p in tracked)
assert '.env' not in tracked and 'tmp/acceptance.env' not in tracked
assert not any(Path(p).suffix in ('.pem', '.p12', '.pfx', '.ini') or
               (Path(p).name.startswith('.env') and Path(p).name != '.env.example') for p in tracked if p), 'Private configuration file is tracked'
print(f'PASS: no known local DB secrets in {len(paths)} tracked/build files; .env files untracked')
