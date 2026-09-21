SET NAMES utf8mb4;
-- Fresh-schema install only. InnoDB tables, UTC sessions, no real financial data.
SET time_zone = '+00:00';
CREATE TABLE app_user (
 user_id BIGINT PRIMARY KEY, user_name VARCHAR(100) NOT NULL,
 email VARCHAR(254) NOT NULL UNIQUE, role VARCHAR(20) NOT NULL,
 CONSTRAINT chk_role CHECK(role IN ('USER','ADMIN'))
) ENGINE=InnoDB;
CREATE TABLE user_label (
 user_id BIGINT NOT NULL, label VARCHAR(64) NOT NULL,
 PRIMARY KEY(user_id,label), FOREIGN KEY(user_id) REFERENCES app_user(user_id)
) ENGINE=InnoDB;
CREATE TABLE account (
 account_id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL,
 account_ref VARCHAR(64) NOT NULL UNIQUE, last4 CHAR(4) NOT NULL,
 currency CHAR(3) NOT NULL DEFAULT 'TWD', active BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(user_id) REFERENCES app_user(user_id),
 CHECK(last4 REGEXP '^[0-9]{4}$'), CHECK(currency='TWD'),
 UNIQUE(account_id,user_id), INDEX(user_id,active)
) ENGINE=InnoDB;
CREATE TABLE product (
 product_id BIGINT PRIMARY KEY AUTO_INCREMENT, product_code VARCHAR(32) NOT NULL UNIQUE,
 product_name VARCHAR(160) NOT NULL, price DECIMAL(20,8) NOT NULL,
 fee_rate DECIMAL(20,12) NOT NULL, currency CHAR(3) NOT NULL DEFAULT 'TWD',
 owner_label VARCHAR(64) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 0,
 CHECK(price>=0), CHECK(fee_rate BETWEEN 0 AND 1), CHECK(currency='TWD')
) ENGINE=InnoDB;
CREATE TABLE preference (
 preference_id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL, account_id BIGINT NOT NULL, planned_quantity INT NOT NULL,
 price_snapshot DECIMAL(20,8) NOT NULL, fee_rate_snapshot DECIMAL(20,12) NOT NULL,
 base_amount DECIMAL(24,8) NOT NULL, total_fee DECIMAL(24,8) NOT NULL,
 total_amount DECIMAL(24,8) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 saved_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 FOREIGN KEY(user_id) REFERENCES app_user(user_id),
 FOREIGN KEY(product_id) REFERENCES product(product_id),
 FOREIGN KEY(account_id,user_id) REFERENCES account(account_id,user_id),
 CHECK(planned_quantity>0), INDEX(user_id,updated_at DESC)
) ENGINE=InnoDB;
-- Minimal transactional audit; no account numbers/email/request payloads.
CREATE TABLE preference_audit (
 audit_id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL,
 preference_id BIGINT NOT NULL, action VARCHAR(12) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(user_id) REFERENCES app_user(user_id),
 CHECK(action IN ('SAVE','UPDATE','DELETE'))
) ENGINE=InnoDB;
