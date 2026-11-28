-- A cancelled registration remains historical. A new intent receives a new ID,
-- so retrying an old cancellation cannot cancel a later booking.
ALTER TABLE registration DROP CONSTRAINT registration_participant_id_shift_id_key;
CREATE UNIQUE INDEX registration_active_participant_shift
    ON registration(participant_id, shift_id) WHERE status = 'CONFIRMED';
