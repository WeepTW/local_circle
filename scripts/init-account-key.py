"""Create a new local key only when explicitly requested; never overwrite a key."""
import os
from pathlib import Path
import secrets
import sys

if len(sys.argv) != 3 or sys.argv[1] != '--new':
    raise SystemExit('Usage: python3 scripts/init-account-key.py --new DIRECTORY (new installation only)')
directory = Path(sys.argv[2]).resolve()
directory.mkdir(mode=0o700, parents=True, exist_ok=True)
os.chmod(directory, 0o700)
key = directory / 'account.key'
with key.open('x') as stream:
    stream.write(f'id={secrets.token_hex(12)}\nkey={secrets.token_hex(32)}\n')
# The host directory is private. Only this file is mounted read-only into the app.
os.chmod(key, 0o444)
print('Created account key in private directory; back up separately from the database.')
