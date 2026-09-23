#!/usr/bin/env bash
source "$(dirname "$0")/lib-test.sh"
init_stack migration
mysql() { "${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -N -uroot "$@"' sh "$@"; }
mysql -e 'CREATE DATABASE lc_upgrade_check; CREATE DATABASE lc_restore_check'
for file in DB/001_ddl.sql DB/002_stored_procedures.sql DB/003_seed_demo.sql; do mysql lc_upgrade_check < "$file"; done
# Remove the fresh-install marker to represent the previous released schema.
mysql lc_upgrade_check -e 'DROP TABLE account_key_state; CALL sp_preference_insert(1,1,10,3,60,0.001,180,0.18,180.18); CALL sp_audit_insert(1,1,"SAVE")' >/dev/null
snapshot='SELECT preference_id,user_id,product_id,account_id,planned_quantity,price_snapshot,fee_rate_snapshot,base_amount,total_fee,total_amount,version,saved_at,updated_at FROM preference'
before=$(mysql lc_upgrade_check -e "$snapshot")
mysql lc_upgrade_check < DB/006_personal_preferences.sql
mysql lc_upgrade_check < DB/006_personal_preferences.sql
[[ $(mysql lc_upgrade_check -e "$snapshot") == "$before" ]]
[[ $(mysql lc_upgrade_check -e 'SELECT COUNT(*) FROM schema_version WHERE version=6') == 1 ]]
[[ $(mysql lc_upgrade_check -e 'SELECT seed_accounts FROM account_key_state WHERE singleton=1') == 0 ]]
[[ $(mysql lc_upgrade_check -e 'SELECT COUNT(*) FROM account WHERE number_ciphertext IS NOT NULL') == 0 ]]
[[ $(mysql lc_upgrade_check -e 'SELECT COUNT(*) FROM preference WHERE name_snapshot IS NULL') == 0 ]]
# Verify opaque encrypted material survives a dump/restore without needing a key in the SQL backup.
mysql lc_upgrade_check -e "UPDATE account SET key_id='restore-test',number_ciphertext=TO_BASE64(RANDOM_BYTES(60)) WHERE account_id=10"
payload=$(mysql lc_upgrade_check -e 'SELECT number_ciphertext FROM account WHERE account_id=10')
umask 077
"${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --set-gtid-purged=OFF --no-tablespaces lc_upgrade_check' > "$report_dir/restore-check.sql"
mysql lc_restore_check < "$report_dir/restore-check.sql"
[[ $(mysql lc_restore_check -e "$snapshot") == "$before" ]]
[[ $(mysql lc_restore_check -e 'SELECT number_ciphertext FROM account WHERE account_id=10') == "$payload" ]]
[[ $(mysql lc_restore_check -e 'SELECT COUNT(*) FROM preference_audit') == 1 ]]
mysql lc_restore_check -e 'CALL sp_personal_list(1)' >/dev/null
echo 'PASS: legacy migration, repeat migration, exact financial/timestamp preservation, no fabricated accounts, encrypted payload and procedure restore'
