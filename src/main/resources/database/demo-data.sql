INSERT OR IGNORE INTO sailors(id, name, rating, age) VALUES
    (1, 'Amina', 8, 24.5),
    (2, 'Rafi', 6, 31.0),
    (3, 'Nila', 9, 27.5),
    (4, 'Sami', 5, 22.0),
    (5, 'Tania', 7, 29.0);

INSERT OR IGNORE INTO boats(id, name, color) VALUES
    (101, 'Aurora', 'red'),
    (102, 'Voyager', 'green'),
    (103, 'Nimbus', 'blue');

INSERT OR IGNORE INTO reserves(sailor_id, boat_id, reserved_on) VALUES
    (1, 101, '2026-09-01'),
    (1, 103, '2026-09-03'),
    (2, 102, '2026-09-02'),
    (3, 101, '2026-09-04'),
    (3, 102, '2026-09-05'),
    (5, 103, '2026-09-06');
