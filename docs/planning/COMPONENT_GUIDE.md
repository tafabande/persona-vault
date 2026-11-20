# Component guide

## Choose the shared component

| Task | Component |
| --- | --- |
| Save or continue | `PersonaAction` with the primary style |
| Attach, add another, or cancel | `PersonaAction` with the quiet style |
| Compact icon control | `PersonaIconAction` with an accessibility label |
| Scan records, links, or profile fields | `PersonaInformationRow` |
| Label a group and align its Add action | `PersonaSectionHeading` |
| Edit a field, picker, or toggle | `PersonaTextInput`, `PersonaDropdownSelector`, `PersonaSearchableCombobox`, `PersonaToggleRow`, or `PersonaSwitch` |
| Filter results | `PersonaFilterChip` |
| Choose among optional fields | `PersonaAddInformationSheet` with `PersonaSheetOption` |
| Open a sheet or form | `PersonaSheet`, `PersonaSheetHeader`, or `PersonaDialog` |
| Show overflow choices | `PersonaMenu` with `PersonaMenuItem` |
| Confirm deletion or an irreversible action | `PersonaConfirmationDialog` with the destructive style |
| Display empty, loading, or result feedback | `PersonaEmptyState`, existing skeleton/progress components, `PersonaSavingButton`, and `PersonaToastHost` |

## Implement a screen

1. Place the title, safe insets, and responsive gutter at the route boundary. Use the shared theme, spacing, and icon registry.
2. Keep a clear commit action and preserve form drafts through recoverable errors. Disable duplicate submissions and announce a local save after its transaction commits.
3. Keep unloaded, empty, disabled, selected, failed, and protected states legible. Show a visible selection indicator as well as the selected color. A destructive menu item stays neutral until its confirmation.
4. Use the source workflow for gated content and check the appropriate session state before exposing values. Use stable record IDs for navigation and selection.
5. Run the relevant checks and update the screen inventory. A specialized component belongs in the shared kit when it can serve another screen with the same interaction.

Compatibility entry points such as `PimsButton` and `VaultPillActionButton` delegate to the same action component. Extend shared parameters around the interaction semantics. Add a token only when the value has a distinct role across screens.
