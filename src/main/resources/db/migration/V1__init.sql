CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(180) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(32) NOT NULL,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP
);

CREATE TABLE accounts (
                          id UUID PRIMARY KEY,
                          account_number VARCHAR(32) UNIQUE NOT NULL,
                          balance NUMERIC(19,2) NOT NULL,
                          status VARCHAR(16) NOT NULL,
                          user_id UUID REFERENCES users(id),
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP
);