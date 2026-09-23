"""Refuse to operate on a Compose project owned by a different checkout."""
import json
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parent.parent

def run(*args):
    return subprocess.run(args, cwd=root, text=True, capture_output=True, check=True).stdout

try:
    project = json.loads(run("docker", "compose", "config", "--format", "json"))["name"]
    containers = run("docker", "ps", "--all", "--quiet", "--filter",
                     f"label=com.docker.compose.project={project}").split()
    if containers:
        owners = run("docker", "inspect", "--format",
                     '{{index .Config.Labels "com.docker.compose.project.working_dir"}}',
                     *containers).splitlines()
        if any(not owner or Path(owner).resolve() != root for owner in owners):
            raise ValueError(f"Compose project '{project}' belongs to another checkout. "
                             "Choose a unique COMPOSE_PROJECT_NAME and unused DB_PORT/WEB_PORT in .env. "
                             "No containers were changed.")
except ValueError as error:
    sys.exit(str(error))
except (OSError, subprocess.CalledProcessError, KeyError):
    sys.exit("Cannot validate Compose project. Check Docker availability and the .env configuration.")
print(f"PASS: Compose project '{project}' is unused or belongs to this checkout")
