SET NAMES utf8mb4;
-- Static SQL only. Procedures participate in the caller's transaction; never COMMIT here.
DELIMITER $$
CREATE PROCEDURE sp_actor_get(IN uid BIGINT)
BEGIN SELECT user_id,user_name,email,role FROM app_user WHERE user_id=uid; END$$
CREATE PROCEDURE sp_actor_labels(IN uid BIGINT)
BEGIN SELECT label FROM user_label WHERE user_id=uid; END$$
CREATE PROCEDURE sp_product_list_active()
BEGIN SELECT * FROM product WHERE active=TRUE ORDER BY product_code; END$$
CREATE PROCEDURE sp_product_list_all()
BEGIN SELECT * FROM product ORDER BY product_code; END$$
CREATE PROCEDURE sp_product_get(IN pid BIGINT)
BEGIN SELECT * FROM product WHERE product_id=pid; END$$
CREATE PROCEDURE sp_product_lock(IN pid BIGINT)
BEGIN SELECT * FROM product WHERE product_id=pid FOR SHARE; END$$
CREATE PROCEDURE sp_account_list_by_user(IN uid BIGINT)
BEGIN SELECT account_id,last4,currency,version FROM account WHERE user_id=uid AND active=TRUE ORDER BY account_id; END$$
CREATE PROCEDURE sp_account_lock(IN aid BIGINT, IN uid BIGINT)
BEGIN SELECT account_id,last4,currency,version FROM account WHERE account_id=aid AND user_id=uid AND active=TRUE FOR SHARE; END$$
CREATE PROCEDURE sp_preference_list(IN uid BIGINT)
BEGIN
 SELECT p.*,r.product_code,r.product_name,a.last4,u.email FROM preference p
 JOIN product r ON r.product_id=p.product_id JOIN account a ON a.account_id=p.account_id
 JOIN app_user u ON u.user_id=p.user_id WHERE p.user_id=uid ORDER BY p.updated_at DESC,p.preference_id DESC;
END$$
CREATE PROCEDURE sp_preference_get_owned(IN pid BIGINT,IN uid BIGINT)
BEGIN
 SELECT p.*,r.product_code,r.product_name,a.last4,u.email FROM preference p
 JOIN product r ON r.product_id=p.product_id JOIN account a ON a.account_id=p.account_id
 JOIN app_user u ON u.user_id=p.user_id WHERE p.preference_id=pid AND p.user_id=uid;
END$$
CREATE PROCEDURE sp_preference_insert(IN uid BIGINT,IN pid BIGINT,IN aid BIGINT,IN qty INT,
 IN price DECIMAL(20,8),IN rate DECIMAL(20,12),IN base DECIMAL(24,8),IN fee DECIMAL(24,8),IN total DECIMAL(24,8))
BEGIN
 INSERT INTO preference(user_id,product_id,account_id,planned_quantity,price_snapshot,fee_rate_snapshot,base_amount,total_fee,total_amount)
 SELECT uid,pid,aid,qty,price,rate,base,fee,total FROM account a JOIN product r ON r.product_id=pid
 WHERE a.account_id=aid AND a.user_id=uid AND a.active=TRUE AND r.active=TRUE;
 IF ROW_COUNT()<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid reference'; END IF;
 SELECT LAST_INSERT_ID() AS preference_id;
END$$
CREATE PROCEDURE sp_preference_update_owned(IN id BIGINT,IN uid BIGINT,IN ver BIGINT,IN pid BIGINT,IN aid BIGINT,IN qty INT,
 IN price DECIMAL(20,8),IN rate DECIMAL(20,12),IN base DECIMAL(24,8),IN fee DECIMAL(24,8),IN total DECIMAL(24,8))
BEGIN
 UPDATE preference p SET product_id=pid,account_id=aid,planned_quantity=qty,price_snapshot=price,
 fee_rate_snapshot=rate,base_amount=base,total_fee=fee,total_amount=total,version=version+1
 WHERE preference_id=id AND user_id=uid AND version=ver
 AND EXISTS(SELECT 1 FROM account a WHERE a.account_id=aid AND a.user_id=uid AND a.active=TRUE)
 AND EXISTS(SELECT 1 FROM product r WHERE r.product_id=pid AND r.active=TRUE);
 SELECT ROW_COUNT() AS affected_rows;
END$$
CREATE PROCEDURE sp_preference_delete_owned(IN id BIGINT,IN uid BIGINT,IN ver BIGINT)
BEGIN
 DELETE FROM preference WHERE preference_id=id AND user_id=uid AND version=ver;
 SELECT ROW_COUNT() AS affected_rows;
END$$
CREATE PROCEDURE sp_product_update(IN pid BIGINT,IN uid BIGINT,IN ver BIGINT,IN pname VARCHAR(160),
 IN pprice DECIMAL(20,8),IN rate DECIMAL(20,12),IN enabled BOOLEAN)
BEGIN
 UPDATE product p SET product_name=pname,price=pprice,fee_rate=rate,active=enabled,version=version+1
 WHERE product_id=pid AND version=ver AND EXISTS(
 SELECT 1 FROM app_user u JOIN user_label l ON l.user_id=u.user_id
 WHERE u.user_id=uid AND u.role='ADMIN' AND l.label=p.owner_label);
 SELECT ROW_COUNT() AS affected_rows;
END$$
CREATE PROCEDURE sp_audit_insert(IN uid BIGINT,IN pid BIGINT,IN operation VARCHAR(12))
BEGIN
 INSERT INTO preference_audit(user_id,preference_id,action) VALUES(uid,pid,operation);
 SELECT LAST_INSERT_ID() AS audit_id;
END$$
DELIMITER ;
