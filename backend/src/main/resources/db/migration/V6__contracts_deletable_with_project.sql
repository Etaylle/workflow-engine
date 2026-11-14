-- A contract never changes. It may go only when its whole project is deleted: that code sets
-- engine.deleting_project for its transaction; any other delete is still refused.
CREATE OR REPLACE FUNCTION workflows.reject_contract_change() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('engine.deleting_project', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'a confirmed contract never changes (version %)', OLD.contract_version;
END;
$$ LANGUAGE plpgsql;
