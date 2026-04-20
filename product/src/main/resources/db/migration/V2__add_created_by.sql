-- Add created_by column for ownership tracking
ALTER TABLE product ADD COLUMN created_by VARCHAR(100) DEFAULT NULL;
