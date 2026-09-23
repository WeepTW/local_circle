SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS schema_version(version INT PRIMARY KEY, applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS account_key_state (singleton INT PRIMARY KEY, key_id VARCHAR(64) NULL, key_check VARCHAR(256) NULL, seed_accounts BOOLEAN NOT NULL DEFAULT FALSE);
INSERT IGNORE INTO account_key_state(singleton) VALUES(1);
DELIMITER $$
DROP PROCEDURE IF EXISTS sp_migrate_personal$$
CREATE PROCEDURE sp_migrate_personal()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='account' AND column_name='number_ciphertext') THEN
  ALTER TABLE account ADD number_ciphertext VARCHAR(256) NULL, ADD key_id VARCHAR(64) NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='account' AND column_name='account_id' AND extra LIKE '%auto_increment%') THEN
  ALTER TABLE account MODIFY account_id BIGINT NOT NULL AUTO_INCREMENT;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='preference' AND column_name='name_snapshot') THEN
  ALTER TABLE preference ADD name_snapshot VARCHAR(160) NULL;
 END IF;
 UPDATE preference p JOIN product r ON r.product_id=p.product_id SET p.name_snapshot=r.product_name, p.updated_at=p.updated_at WHERE p.name_snapshot IS NULL;
 ALTER TABLE preference MODIFY product_id BIGINT NULL;
END$$
SET FOREIGN_KEY_CHECKS=0$$
CALL sp_migrate_personal()$$
SET FOREIGN_KEY_CHECKS=1$$
DROP PROCEDURE sp_migrate_personal$$
DROP PROCEDURE IF EXISTS sp_personal_list$$
CREATE PROCEDURE sp_personal_list(IN uid BIGINT)
BEGIN
 SELECT p.*,COALESCE(p.name_snapshot,r.product_name) AS resolved_name,r.product_code,a.last4,
 a.number_ciphertext IS NOT NULL AS number_available,u.email
 FROM preference p LEFT JOIN product r ON r.product_id=p.product_id
 JOIN account a ON a.account_id=p.account_id AND a.user_id=p.user_id
 JOIN app_user u ON u.user_id=p.user_id WHERE p.user_id=uid ORDER BY p.updated_at DESC,p.preference_id DESC;
END$$
DROP PROCEDURE IF EXISTS sp_personal_get$$
CREATE PROCEDURE sp_personal_get(IN uid BIGINT,IN id BIGINT)
BEGIN
 SELECT p.*,COALESCE(p.name_snapshot,r.product_name) AS resolved_name,r.product_code,a.last4,
 a.number_ciphertext IS NOT NULL AS number_available,u.email
 FROM preference p LEFT JOIN product r ON r.product_id=p.product_id
 JOIN account a ON a.account_id=p.account_id AND a.user_id=p.user_id
 JOIN app_user u ON u.user_id=p.user_id WHERE p.user_id=uid AND p.preference_id=id;
END$$
DROP PROCEDURE IF EXISTS sp_personal_insert$$
CREATE PROCEDURE sp_personal_insert(IN uid BIGINT,IN pid BIGINT,IN aid BIGINT,IN qty INT,IN pname VARCHAR(160),
 IN price DECIMAL(20,8),IN rate DECIMAL(20,12),IN base DECIMAL(24,8),IN fee DECIMAL(24,8),IN total DECIMAL(24,8))
BEGIN
 INSERT INTO preference(user_id,product_id,account_id,planned_quantity,name_snapshot,price_snapshot,fee_rate_snapshot,base_amount,total_fee,total_amount)
 SELECT uid,pid,aid,qty,pname,price,rate,base,fee,total FROM account a
 WHERE a.account_id=aid AND a.user_id=uid AND a.active=TRUE
 AND (pid IS NULL OR EXISTS(SELECT 1 FROM product WHERE product_id=pid AND active=TRUE));
 IF ROW_COUNT()<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid reference'; END IF;
 SELECT LAST_INSERT_ID() AS preference_id;
END$$
DROP PROCEDURE IF EXISTS sp_personal_update$$
CREATE PROCEDURE sp_personal_update(IN uid BIGINT,IN id BIGINT,IN ver BIGINT,IN pid BIGINT,IN aid BIGINT,IN qty INT,IN pname VARCHAR(160),
 IN price DECIMAL(20,8),IN rate DECIMAL(20,12),IN base DECIMAL(24,8),IN fee DECIMAL(24,8),IN total DECIMAL(24,8))
BEGIN
 UPDATE preference SET product_id=pid,account_id=aid,planned_quantity=qty,name_snapshot=pname,price_snapshot=price,
 fee_rate_snapshot=rate,base_amount=base,total_fee=fee,total_amount=total,version=version+1
 WHERE preference_id=id AND user_id=uid AND version=ver
 AND EXISTS(SELECT 1 FROM account WHERE account_id=aid AND user_id=uid AND active=TRUE)
 AND (pid IS NULL OR EXISTS(SELECT 1 FROM product WHERE product_id=pid AND active=TRUE));
 SELECT ROW_COUNT() AS affected_rows;
END$$
DROP PROCEDURE IF EXISTS sp_account_create_encrypted$$
CREATE PROCEDURE sp_account_create_encrypted(IN uid BIGINT,IN ref VARCHAR(64),IN digits CHAR(4),IN kid VARCHAR(64),IN ciphertext VARCHAR(256))
BEGIN
 INSERT INTO account(user_id,account_ref,last4,key_id,number_ciphertext) VALUES(uid,ref,digits,kid,ciphertext);
 SELECT LAST_INSERT_ID() AS account_id;
END$$
DROP PROCEDURE IF EXISTS sp_account_encrypted_owned$$
CREATE PROCEDURE sp_account_encrypted_owned(IN uid BIGINT,IN aid BIGINT)
BEGIN SELECT account_id,account_ref,key_id,number_ciphertext FROM account WHERE user_id=uid AND account_id=aid AND active=TRUE; END$$
DROP PROCEDURE IF EXISTS sp_key_state_lock$$
CREATE PROCEDURE sp_key_state_lock()
BEGIN SELECT * FROM account_key_state WHERE singleton=1 FOR UPDATE; END$$
DROP PROCEDURE IF EXISTS sp_key_state_set$$
CREATE PROCEDURE sp_key_state_set(IN kid VARCHAR(64),IN payload VARCHAR(256))
BEGIN UPDATE account_key_state SET key_id=kid,key_check=payload WHERE singleton=1 AND key_check IS NULL; SELECT ROW_COUNT(); END$$
DROP PROCEDURE IF EXISTS sp_account_seed_list$$
CREATE PROCEDURE sp_account_seed_list()
BEGIN SELECT account_id,user_id,account_ref,last4 FROM account WHERE number_ciphertext IS NULL AND EXISTS(SELECT 1 FROM account_key_state WHERE singleton=1 AND seed_accounts=TRUE); END$$
DROP PROCEDURE IF EXISTS sp_account_seed_complete$$
CREATE PROCEDURE sp_account_seed_complete(IN aid BIGINT,IN kid VARCHAR(64),IN payload VARCHAR(256))
BEGIN UPDATE account SET key_id=kid,number_ciphertext=payload WHERE account_id=aid AND number_ciphertext IS NULL AND EXISTS(SELECT 1 FROM account_key_state WHERE singleton=1 AND seed_accounts=TRUE); SELECT ROW_COUNT(); END$$
DROP PROCEDURE IF EXISTS sp_account_seed_finish$$
CREATE PROCEDURE sp_account_seed_finish()
BEGIN UPDATE account_key_state SET seed_accounts=FALSE WHERE singleton=1; SELECT ROW_COUNT(); END$$
DELIMITER ;
INSERT IGNORE INTO schema_version(version) VALUES(6);
