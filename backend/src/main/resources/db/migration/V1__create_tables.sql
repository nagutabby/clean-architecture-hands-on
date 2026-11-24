CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    amount INT NOT NULL CHECK (amount >= 1),
    transaction_date DATE NOT NULL,
    memo TEXT,
    CONSTRAINT fk_categories FOREIGN KEY (category_id) REFERENCES categories(id)
);

INSERT INTO categories (name, type) VALUES ('食費', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('日用品', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('交通費', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('給与', 'INCOME');
INSERT INTO categories (name, type) VALUES ('副業', 'INCOME');
