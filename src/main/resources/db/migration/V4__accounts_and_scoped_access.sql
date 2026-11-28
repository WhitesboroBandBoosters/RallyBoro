ALTER TABLE app_user ADD COLUMN auth_version integer NOT NULL DEFAULT 0;
CREATE TABLE local_credential (
    user_id uuid PRIMARY KEY REFERENCES app_user,
    username varchar(40) NOT NULL UNIQUE CHECK (username=lower(username)),
    password_hash varchar(100) NOT NULL,
    recovery_hash varchar(64),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE external_identity (
    issuer varchar(1000) NOT NULL, subject varchar(255) NOT NULL,
    user_id uuid NOT NULL REFERENCES app_user, PRIMARY KEY(issuer,subject)
);
ALTER TABLE membership ADD COLUMN can_manage boolean NOT NULL DEFAULT false;
ALTER TABLE membership ADD COLUMN can_organize boolean NOT NULL DEFAULT false;
UPDATE membership SET can_manage=true,can_organize=true WHERE role='ORGANIZER';
CREATE TABLE event_manager (
    event_id uuid NOT NULL REFERENCES event, user_id uuid NOT NULL REFERENCES app_user,
    PRIMARY KEY(event_id,user_id)
);
CREATE TABLE organization_invitation (
    id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization,
    created_by uuid NOT NULL REFERENCES app_user, token_hash varchar(64) NOT NULL UNIQUE,
    can_manage boolean NOT NULL, can_organize boolean NOT NULL, event_id uuid REFERENCES event,
    expires_at timestamptz NOT NULL, revoked boolean NOT NULL DEFAULT false,
    accepted_by uuid REFERENCES app_user, created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (event_id IS NULL OR (NOT can_manage AND NOT can_organize))
);
CREATE INDEX organization_invitation_org ON organization_invitation(organization_id,created_at);
CREATE TABLE access_audit (
    id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization,
    actor_id uuid NOT NULL REFERENCES app_user, action varchar(50) NOT NULL,
    target_id uuid, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX access_audit_org ON access_audit(organization_id,created_at);
CREATE TABLE pilot_import (user_id uuid PRIMARY KEY REFERENCES app_user);
