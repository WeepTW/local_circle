SET NAMES utf8mb4;
-- ALL DATA IS DEMO. Prices are fictional, not market quotes.
INSERT INTO app_user VALUES
 (1,'Demo User','user@example.test','USER'),
 (2,'Other User','other@example.test','USER'),
 (3,'ETF Admin','etf-admin@example.test','ADMIN'),
 (4,'Global Admin','global-admin@example.test','ADMIN');
INSERT INTO user_label VALUES (1,'TW_ETF_OWNER'),(3,'TW_ETF_OWNER'),(4,'GLOBAL_DEMO_OWNER');
INSERT INTO account(account_id,user_id,account_ref,last4,active) VALUES
 (10,1,'DEMO-USER-1-A','9666',TRUE),(11,1,'DEMO-USER-1-B','1122',TRUE),
 (12,1,'DEMO-INACTIVE','0000',FALSE),(20,2,'DEMO-USER-2','2222',TRUE),
 (30,3,'DEMO-ADMIN-3','3333',TRUE),(40,4,'DEMO-ADMIN-4','4444',TRUE);
INSERT INTO product(product_code,product_name,price,fee_rate,owner_label) VALUES
 ('0050','元大台灣50 ETF（DEMO）',60,0.001,'TW_ETF_OWNER'),
 ('0052','富邦科技 ETF（DEMO）',180,0.001,'TW_ETF_OWNER'),
 ('GLOBAL_TOP10','全球前十大股票示範組合（DEMO）',1000,0.0015,'GLOBAL_DEMO_OWNER');
