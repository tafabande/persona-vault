# Changelog

## Unreleased

- Gave the Library category groups more space and added labeled record details. Display formatting covers phone numbers, dates and addresses while search and copying preserve the original values.
- Added a dedicated Vault pin picker that opens from Me or the unlocked Vault home. Both entry points share the same saved pins, and full records open in Vault.
- Applied themed foreground colors to Library and shared controls. Replaced the Library filter strip with one category menu and made the Home edit action easier to find.
- Add information now opens a single entry form with muted title completion and a suggested category. Links and custom titles use the same form. Known record types retain their structured editors.
- Me displays pinned legal names and ordinary profile values directly. Sensitive profile previews can be explicitly enabled and pinned payment cards show their last four digits. Note lock, save and success feedback use shared icons and controls.
- Hardened app PIN enrollment, storage, startup, and protected navigation. Zone 4 keeps its existing hardware-bound authorization.
- Preserve original bytes for newly imported supported profile, person, and wallpaper images. Keep legacy encrypted cloud formats readable.
- Added shared actions, section headings, information rows, choice sheets, and centralized Material icon usage. Moved Settings to Me and added a Library entry to the primary navigation.
- Added the account-scoped Me pin layout, including add, reorder, and unpin. Protected links use existing credentials and source routes.
- Added profile and work fields to the Me picker, labeled multiple contact choices, and made the pin order retryable after a storage error.
- Routed personal note changes through a durable local outbox. Separated local save confirmation from cloud status and conflict handling.
- Grouped Vault identification records by type and kept the secure detail workflow. Improved Information Library classification and included searchable notes and tags.
- Consolidated People note presentation and preserved historical content. Explicit tags can suggest links to People.
- Simplified Education, Portfolio and Medical Aid. Optional groups appear when they have data, and shared add actions line up at the trailing edge.
- Kept qualification, experience, certificate, custom information and identity credential forms open through local persistence. A failed write keeps the draft available.
- Required authentication at the protected note editor, including direct navigation, and stopped showing private note tags and Vault titles as general tag suggestions.
- Moved full-screen routes out of the dashboard shell to reduce generated DEX register pressure. Relationship attachments now preview inside People detail.
