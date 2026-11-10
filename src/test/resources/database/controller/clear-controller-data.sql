DELETE FROM payments;
DELETE FROM rentals;
DELETE FROM cars;
DELETE FROM users WHERE email <> 'manager@carsharing.com';
