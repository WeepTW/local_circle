-- One-time repair for DEMO names imported by a latin1 client before SET NAMES was added.
-- Exact corrupt-name predicates preserve any user-edited names, prices and versions.
SET NAMES utf8mb4;
UPDATE product SET product_name='元大台灣50 ETF（DEMO）'
WHERE product_code='0050' AND product_name=CONVERT(CAST('元大台灣50 ETF（DEMO）' AS BINARY) USING latin1);
UPDATE product SET product_name='富邦科技 ETF（DEMO）'
WHERE product_code='0052' AND product_name=CONVERT(CAST('富邦科技 ETF（DEMO）' AS BINARY) USING latin1);
UPDATE product SET product_name='全球前十大股票示範組合（DEMO）'
WHERE product_code='GLOBAL_TOP10' AND product_name=CONVERT(CAST('全球前十大股票示範組合（DEMO）' AS BINARY) USING latin1);
