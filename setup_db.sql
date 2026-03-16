-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS remoteflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE remoteflow;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    equipe_id BIGINT,
    role VARCHAR(50) NOT NULL,
    FOREIGN KEY (equipe_id) REFERENCES equipe(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create equipe table
CREATE TABLE IF NOT EXISTS equipe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Display confirmation
SELECT 'Database remoteflow created successfully' as status;
SHOW TABLES;


