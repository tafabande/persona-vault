# ADR-026 People notes and vault discovery

**Status** Accepted

## Context

Earlier People forms wrote initial notes into the person, relationship, and structured relationship-note rows. Later edits could rebuild the person field from several notes and update or replace the structured rows. Vault's identity list inferred types from words in titles, and Information Library assigned the Password category to most Vault records.

## Decision

People creation commits the person, contacts, relationship and initial note in one SQLCipher transaction. Edit person changes profile and contact details. The People Notes editor owns additions, updates, attachment changes, privacy, and deletion, using stable relationship-note IDs.

Profile load reads the legacy sources and structured notes in a transaction. When every structured note is public, it preserves each distinct legacy text as a structured note and removes the consumed source copies. The comparison includes exact trimmed content and the exact historical joined-body format. Existing structured notes keep their IDs, attachments, formats and privacy. Photo references and other keys remain in the person's metadata. The migration replaces the consumed body with a metadata marker when synchronizing the person record.

When existing private notes make a historical copy ambiguous, the transaction moves the copy to restricted history in the relationship's local SQLCipher row. This field stays on the device and carries a marker that preserves the restriction even if a private note is later deleted. The Notes section can show the history after Vault unlock. Search and contextual tagging exclude it.

Tags link through an authored hashtag or explicit note tag. A case-insensitive Unicode-normalized tag can match a complete display name or relationship label with spaces replaced by underscores. An ambiguous tag remains unlinked. Navigation uses record and relationship IDs. A name or role change updates derived matches on the next load. Person profiles surface tagged public notes from the journal and other People profiles. The Library links notes back to the related person by relationship ID.

Vault categories and item IDs remain durable. The home screen links to one IDs and memberships screen. The list reads the established type prefix, with a fallback to the historical generated title, and groups records under concrete types. New credential headers store the type alone, while the raw identifier and notes remain in the authenticated record payload. Historical headers remain readable. Opening, revealing or copying an identifier uses the existing Vault detail flow.

Information Library classifies Vault and document records from their enums. Known exact identity field labels can correct a legacy custom category in the Library without rewriting the original record. Search includes available titles, names, record descriptions, permitted document version metadata, public People notes, unlocked personal notes and their tags. Vault content opens through its authenticated flow. Protected values, private People notes, and protected personal note contents stay out of the Library index.

## Verification

- Use `InformationLibraryRegressionTest` for identity grouping and classification, protected content exclusion, note and tag search, ambiguous tags, and preservation of distinct legacy bodies.
- Use `RelationshipNotesRepositoryTest` for relationship ownership, attachment preservation and private-note behavior.
- On an upgraded device, open a person with an initial and an additional note. Edit and delete a note through the profile Notes section, relaunch, and verify historical text appears once and the deleted entry does not reappear.
- On an upgraded device with private notes and unmatched history, delete a private note. Relaunch and verify the history still requires Vault authentication and cannot be found in Library search. Test a precomposed name tag, a decomposed tag, and a similar fragment in a URL.
- With an old Vault record and a newly saved one, verify the correct ID group, masked details, successful authentication, original number reveal and copy, and intact evidence attachments.
