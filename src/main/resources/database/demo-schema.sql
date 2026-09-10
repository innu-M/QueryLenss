CREATE TABLE IF NOT EXISTS sailors (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    rating INTEGER NOT NULL,
    age REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS boats (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    color TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS reserves (
    sailor_id INTEGER NOT NULL,
    boat_id INTEGER NOT NULL,
    reserved_on TEXT NOT NULL,
    PRIMARY KEY (sailor_id, boat_id, reserved_on),
    FOREIGN KEY (sailor_id) REFERENCES sailors(id),
    FOREIGN KEY (boat_id) REFERENCES boats(id)
);

CREATE INDEX IF NOT EXISTS idx_reserves_sailor ON reserves(sailor_id);
CREATE INDEX IF NOT EXISTS idx_reserves_boat ON reserves(boat_id);
