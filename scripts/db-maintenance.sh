#!/usr/bin/env bash
# Run from the intended checkout; never delete a volume to apply a migration.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/check-compose-project.py
compose=(docker compose)
mysql() { "${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -N -uroot local_circle "$@"' sh "$@"; }
case ${1:-} in
  backup)
    : "${2:?Provide a new backup filename}"
    [[ ! -e $2 ]] || { echo 'Refusing to overwrite a backup.' >&2; exit 1; }
    umask 077
    set -o noclobber
    "${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --set-gtid-purged=OFF --no-tablespaces local_circle' > "$2"
    echo 'Backup complete. Keep the encryption key in a separate secure backup.'
    ;;
  migrate)
    [[ -z $("${compose[@]}" ps --status running -q app web) ]] || { echo 'Stop this checkout app/web before migration.' >&2; exit 1; }
    mkdir -p backups
    bash scripts/db-maintenance.sh backup "backups/pre-v6-$(date -u +%Y%m%dT%H%M%SZ).sql"
    mysql < DB/006_personal_preferences.sql
    if [[ ! -f .secrets/account.key ]]; then
      encrypted=$(mysql -e 'SELECT (SELECT COUNT(*) FROM account WHERE number_ciphertext IS NOT NULL)+(SELECT COUNT(*) FROM account_key_state WHERE key_check IS NOT NULL)')
      [[ $encrypted == 0 ]] || { echo 'Encrypted data exists: restore its original key. Never generate a replacement.' >&2; exit 1; }
      python3 scripts/init-account-key.py --new .secrets
    fi
    echo 'Schema version 6 ready; restart only this checkout app/web.'
    ;;
  restore)
    : "${2:?Provide a backup filename}"
    [[ -z $("${compose[@]}" ps --status running -q app web) ]] || { echo 'Stop app/web before restore.' >&2; exit 1; }
    tables=$(mysql -e 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()')
    [[ $tables == 0 ]] || { echo 'Restore requires an empty, isolated local_circle database.' >&2; exit 1; }
    mysql < "$2"
    echo 'Database restored. Restore the matching account key separately before starting the app.'
    ;;
  *) echo 'Usage: bash scripts/db-maintenance.sh backup FILE | migrate | restore FILE' >&2; exit 2;;
esac
