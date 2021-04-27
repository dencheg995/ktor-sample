CREATE TABLE user_data
(
    id        SERIAL PRIMARY KEY,
    login     VARCHAR(255),
    full_name VARCHAR(255),
    password  VARCHAR(255),
    age       INT
);