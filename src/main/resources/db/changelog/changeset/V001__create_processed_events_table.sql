CREATE TABLE IF NOT EXISTS processed_events
(
    id BIGINT PRIMARY KEY,
    messageId VARCHAR(64) NOT NULL UNIQUE,
    productId VARCHAR(64) NOT NULL
);