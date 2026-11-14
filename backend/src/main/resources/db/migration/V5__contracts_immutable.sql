-- M1: a contract is a frozen snapshot. The cost limit is chosen later (M3), so it may be empty,
-- and a stored contract can never be changed or deleted.
ALTER TABLE workflows.contracts ALTER COLUMN cost_limit_amount DROP NOT NULL;

CREATE FUNCTION workflows.reject_contract_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'a confirmed contract never changes (version %)', OLD.contract_version;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER contracts_immutable
    BEFORE UPDATE OR DELETE ON workflows.contracts
    FOR EACH ROW EXECUTE FUNCTION workflows.reject_contract_change();
