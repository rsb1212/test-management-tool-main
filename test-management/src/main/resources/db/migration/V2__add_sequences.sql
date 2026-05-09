-- ============================================================
-- V2__add_sequences.sql
-- DB-backed sequences for human-readable entity codes
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS seq_tc   START 200  INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_tp   START 10   INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_tr   START 80   INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_ex   START 440  INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_def  START 1000 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_cyc  START 1    INCREMENT 1;
