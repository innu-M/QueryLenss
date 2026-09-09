INSERT OR IGNORE INTO Sailors (sid, sname, rating, age) VALUES
(22, 'Dustin', 7, 45.0),
(29, 'Brutus', 1, 33.0),
(31, 'Lubber', 8, 55.5),
(32, 'Andy', 8, 25.5),
(58, 'Rusty', 10, 35.0);

INSERT OR IGNORE INTO Boats (bid, bname, color) VALUES
(101, 'Interlake', 'blue'),
(102, 'Interlake', 'red'),
(104, 'Marine', 'red');

INSERT OR IGNORE INTO Reserves (sid, bid, day) VALUES
(22, 101, '2026-09-01'),
(31, 102, '2026-09-02'),
(32, 104, '2026-09-03');
