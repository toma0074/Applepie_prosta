-- 権限値の変更: 一般 1 -> 0、管理者 2 -> 1（1文では両方を同時に置換するため、順序を気にせず実行できる）
UPDATE customer
   SET permission = CASE permission WHEN 1 THEN 0 WHEN 2 THEN 1 ELSE permission END
 WHERE permission IN (1, 2);
COMMIT;
