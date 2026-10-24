CREATE DATABASE IF NOT EXISTS masjidtv;
USE masjidtv;

CREATE TABLE IF NOT EXISTS devices (
  id INT AUTO_INCREMENT PRIMARY KEY,
  device_id VARCHAR(64) UNIQUE,
  lat DECIMAL(10,7),
  lng DECIMAL(10,7),
  city VARCHAR(100),
  province VARCHAR(100),
  mosque VARCHAR(150),
  payload JSON,
  last_seen DATETIME,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id VARCHAR(64),
  lat DECIMAL(10,7),
  lng DECIMAL(10,7),
  city VARCHAR(100),
  province VARCHAR(100),
  mosque VARCHAR(150),
  kas_balance BIGINT,
  payload JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_device (device_id),
  INDEX idx_created (created_at)
);
