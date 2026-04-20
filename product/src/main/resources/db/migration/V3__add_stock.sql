-- V3: Add stock column to product table
ALTER TABLE product ADD COLUMN stock INT NOT NULL DEFAULT 10;
