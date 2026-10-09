# ADR-011: Bank-account record integrity

**Status:** Accepted

Bank accounts remain distinct encrypted vault records. Their stable vault item IDs identify individual accounts; `personId` identifies the associated Persona person. A bank name is a label only and is never a uniqueness key. Payment cards remain separate vault records and reference a selected account only through an explicit account ID.

An account edit must load and authenticate the selected existing `BANK_ACCOUNT` record before changing it. Failure to locate, authorize, or decrypt that record aborts the edit; it must never become a partial overwrite or a mock UI result. Masked identifiers are derived from the verified decrypted account number and never written back as the account number.

This decision does not introduce a new plaintext financial table or a destructive migration. Existing encrypted payloads remain in their established format.
