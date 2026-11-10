INSERT INTO users (id, email, first_name, last_name, password, role) VALUES
    (101, 'customer@example.com', 'John', 'Doe', 'encoded-password', 'CUSTOMER'),
    (102, 'another@example.com', 'Jane', 'Smith', 'encoded-password', 'CUSTOMER');

INSERT INTO cars (id, model, brand, type, inventory, daily_fee, is_deleted) VALUES
    (101, 'Corolla', 'Toyota', 'SEDAN', 2, 40.00, false),
    (102, 'Model 3', 'Tesla', 'SEDAN', 1, 120.00, false),
    (103, 'Focus', 'Ford', 'HATCHBACK', 3, 30.00, true);

INSERT INTO rentals (id, rental_date, return_date, actual_return_date, car_id, user_id) VALUES
    (101, '2026-01-01', '2026-01-05', NULL, 101, 101),
    (102, '2026-01-01', '2026-01-03', '2026-01-04', 102, 101),
    (103, '2026-01-10', '2026-03-01', NULL, 101, 102);

INSERT INTO payments (id, status, type, rental_id, session_url, session_id, amount_to_pay) VALUES
    (101, 'PENDING', 'PAYMENT', 101,
        'https://checkout.stripe.com/c/pay/cs_test_101', 'cs_test_101', 160.00),
    (102, 'PAID', 'FINE', 102,
        'https://checkout.stripe.com/c/pay/cs_test_102', 'cs_test_102', 180.00),
    (103, 'PENDING', 'PAYMENT', 103,
        'https://checkout.stripe.com/c/pay/cs_test_103', 'cs_test_103', 500.00);
