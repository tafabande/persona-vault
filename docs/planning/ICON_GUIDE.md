# Icon guide

Use `PersonaIcons` in `presentation/ui/theme/ThemedIcon.kt` for Material vectors and approved brand and product drawables. Choose `PersonaIcon` to render a standalone application symbol and `PersonaIconAction` for a tap target. Filled and outlined variants communicate navigation selection. Use the registry’s mirrored Back and Forward symbols for direction-aware navigation.

| Purpose | Usage |
| --- | --- |
| Add, edit, remove, close | Use the corresponding shared action symbol with a concise label when the symbol stands alone |
| Disclose a destination | Chevron for routing, downward indicator for a selector, and a checked mark for the current option |
| Warn, validate, confirm | Use the shared status icon and an adjacent message |
| Protect a note | Use the shared outlined lock and open lock in labeled actions. Use one lock badge beside the protected note label. |
| Confirm a save | Use the shared check in the status badge with a message after the local write completes. |
| Brands and product art | Use approved drawables or a text monogram for identifiers without an approved brand symbol |

Give actionable icons accessible names that describe the action. Icons beside an equivalent visible label use a null content description. The default glyph size is 24 dp and icon buttons have a 48 dp touch target. Keep status and disabled tints inside the active theme. Emoji in stored records and legacy parsing stays user content.

Add new symbols to the registry with a semantic name, using supported Material vectors or a documented app drawable. Use [the design system](DESIGN_SYSTEM.md) to choose sizes and colors.
