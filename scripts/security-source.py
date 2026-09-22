"""Stage public source for local secret scanning, excluding private task inputs."""
from pathlib import Path
import shutil
import tempfile

root = Path(__file__).resolve().parent.parent
dest = Path(tempfile.mkdtemp(prefix='security-source-', dir=root/'tmp'))
for name in ('backend', 'frontend', 'scripts', 'DB', 'nginx', '.github'):
    shutil.copytree(root/name, dest/name, ignore=shutil.ignore_patterns('node_modules', 'target', 'dist',
        'test-results', 'playwright-report', '__pycache__'))
for name in ('README.md', 'docker-compose.yml', '.env.example', '.gitignore', '.node-version'):
    shutil.copy2(root/name, dest/name)
if (root/'frontend/dist').exists():
    shutil.copytree(root/'frontend/dist', dest/'published-assets')
print(dest)
