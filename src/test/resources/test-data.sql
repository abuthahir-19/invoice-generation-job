DELETE FROM customers;

INSERT INTO customers (id, name, email, plan_code, start_date, status, billing_address, discount)
VALUES
    (1, 'Test User 1', 'test1@example.com', 'BASIC', '2025-01-01', 'ACTIVE', '123 Test St', 0.0),
    (2, 'Test User 2', 'test2@example.com', 'PRO', '2025-01-01', 'ACTIVE', '456 Test Ave', 10.0),
    (3, 'Test User 3', 'test3@example.com', 'ENTERPRISE', '2025-01-01', 'ACTIVE', '789 Test Blvd', 0.0),
    (4, 'Inactive User', 'inactive@example.com', 'BASIC', '2025-01-01', 'INACTIVE', '000 Test Rd', 0.0);