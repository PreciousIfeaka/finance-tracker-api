CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    role VARCHAR(255) NOT NULL DEFAULT 'USER',
    currency VARCHAR(255),
    otp VARCHAR(255),
    otp_expired_at TIMESTAMP WITHOUT TIME ZONE,
    avatar_url VARCHAR(255),
    limit_exceeded BOOLEAN
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    amount NUMERIC(38, 2) NOT NULL,
    month VARCHAR(255) NOT NULL,
    direction VARCHAR(255),
    description VARCHAR(255),
    transaction_date_time TIMESTAMP WITHOUT TIME ZONE,
    user_id UUID,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS income (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    amount NUMERIC(38, 2) NOT NULL,
    source VARCHAR(255) NOT NULL,
    note VARCHAR(255),
    is_recurring BOOLEAN NOT NULL,
    month VARCHAR(255) NOT NULL,
    transaction_date_time TIMESTAMP WITHOUT TIME ZONE,
    user_id UUID,
    CONSTRAINT fk_income_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS expense (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    amount NUMERIC(38, 2) NOT NULL,
    category VARCHAR(255),
    note VARCHAR(255),
    is_recurring BOOLEAN NOT NULL,
    month VARCHAR(255) NOT NULL,
    transaction_date_time TIMESTAMP WITHOUT TIME ZONE,
    user_id UUID,
    CONSTRAINT fk_expense_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS budget (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    amount NUMERIC(38, 2) NOT NULL,
    category VARCHAR(255),
    month VARCHAR(255) NOT NULL,
    is_exceeded BOOLEAN,
    is_recurring BOOLEAN NOT NULL,
    user_id UUID,
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bank_statement (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    month VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    user_id UUID,
    CONSTRAINT fk_bank_statement_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bank_statement_document_urls (
    bank_statement_id UUID NOT NULL,
    file_key VARCHAR(255),
    mime_type VARCHAR(255),
    CONSTRAINT fk_document_urls_statement FOREIGN KEY (bank_statement_id) REFERENCES bank_statement(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_income_user_id ON income(user_id);
CREATE INDEX IF NOT EXISTS idx_expense_user_id ON expense(user_id);
CREATE INDEX IF NOT EXISTS idx_budget_user_id ON budget(user_id);
CREATE INDEX IF NOT EXISTS idx_bank_statement_user_id ON bank_statement(user_id);