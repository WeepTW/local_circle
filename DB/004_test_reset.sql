SET NAMES utf8mb4;
-- DESTRUCTIVE test fixture reset: run only on a disposable test schema.
DELETE FROM preference_audit;
DELETE FROM preference;
UPDATE product SET version=0,active=TRUE,
 product_name=CASE product_code WHEN '0050' THEN '元大台灣50 ETF（DEMO）' WHEN '0052' THEN '富邦科技 ETF（DEMO）' ELSE '全球前十大股票示範組合（DEMO）' END,
 price=CASE product_code WHEN '0050' THEN 60 WHEN '0052' THEN 180 ELSE 1000 END,
 fee_rate=CASE product_code WHEN 'GLOBAL_TOP10' THEN 0.0015 ELSE 0.001 END;
