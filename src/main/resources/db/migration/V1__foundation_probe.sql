-- Synthetic operational verification only; no volunteer or event data.
CREATE TABLE foundation_probe (
    id varchar(80) PRIMARY KEY,
    description varchar(200) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
