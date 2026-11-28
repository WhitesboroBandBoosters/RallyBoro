CREATE TABLE app_user (id uuid PRIMARY KEY, display_name varchar(80) NOT NULL);
CREATE TABLE organization (id uuid PRIMARY KEY, name varchar(120) NOT NULL);
CREATE TABLE membership (user_id uuid REFERENCES app_user, organization_id uuid REFERENCES organization,
    role varchar(12) NOT NULL CHECK (role IN ('ORGANIZER','VOLUNTEER')), PRIMARY KEY(user_id, organization_id));
CREATE TABLE event (id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization,
    title varchar(120) NOT NULL, description varchar(2000) NOT NULL DEFAULT '', location varchar(200) NOT NULL DEFAULT '',
    event_date date NOT NULL, timezone varchar(80) NOT NULL, visibility varchar(10) NOT NULL DEFAULT 'HIDDEN'
    CHECK(visibility IN ('HIDDEN','PRIVATE','PUBLIC')), signup_open boolean NOT NULL DEFAULT false,
    revision integer NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE job (id uuid PRIMARY KEY, event_id uuid NOT NULL REFERENCES event, name varchar(120) NOT NULL,
    description varchar(1000) NOT NULL DEFAULT '', eligibility varchar(1000) NOT NULL DEFAULT '');
CREATE TABLE shift (id uuid PRIMARY KEY, job_id uuid NOT NULL REFERENCES job, starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL, capacity integer NOT NULL CHECK(capacity BETWEEN 1 AND 1000), CHECK(ends_at>starts_at));
CREATE TABLE participant (id uuid PRIMARY KEY, managing_user_id uuid NOT NULL UNIQUE REFERENCES app_user);
CREATE TABLE registration (id uuid PRIMARY KEY, participant_id uuid NOT NULL REFERENCES participant,
    shift_id uuid NOT NULL REFERENCES shift, status varchar(12) NOT NULL CHECK(status IN ('CONFIRMED','CANCELLED')),
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(), UNIQUE(participant_id,shift_id));
CREATE INDEX registration_shift_status ON registration(shift_id,status);
CREATE TABLE mutation_receipt (user_id uuid REFERENCES app_user, request_id uuid NOT NULL, fingerprint varchar(64) NOT NULL,
    result_id uuid NOT NULL, PRIMARY KEY(user_id,request_id));
CREATE TABLE agent_token (id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES app_user,
    organization_id uuid NOT NULL REFERENCES organization, name varchar(80) NOT NULL, token_hash varchar(64) NOT NULL UNIQUE,
    write_enabled boolean NOT NULL, expires_at timestamptz NOT NULL, revoked boolean NOT NULL DEFAULT false);
