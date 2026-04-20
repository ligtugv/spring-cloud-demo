-- V4: Ensure data integrity for stock column
-- V3 already added the stock column. V4 backfills any NULL stock values
-- (can happen with existing products if Hibernate ddl-auto: update created
-- the column without proper default handling before V3 ran).
UPDATE product SET stock = 10 WHERE stock IS NULL;
