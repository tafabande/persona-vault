# Persona Vault design system

Use the existing Material3 theme and mood palettes. Give records visually quiet rows and enough room to scan them. Reserve raised surfaces for containment, selection, overlays and meaningful state changes. Group related information with headings and space. Show optional sections once they contain a record or the user chooses to add one.

## Tokens

| Purpose | Source | Value or role |
| --- | --- | --- |
| Page gutter | `pimsPageGutter()` | 12 dp compact, 20 dp medium, 28 dp expanded |
| Spacing | `PimsSpacing` | 2, 4, 8, 12, 16, 24, 32, 48 dp |
| Field and section gaps | `PimsSpacing` | 8 dp field, 10 dp related field, 20 dp section, 12 dp action |
| Control dimensions | `PimsControlSize` | 48 dp minimum action/touch target, 48 dp compact input, 56 dp standard input and information row, 52 dp selector |
| Shapes | `PimsRadius` | 14 dp input and action, 16 dp surface, 24 dp modal, square section, pill chip |
| Icons | `PersonaIconSize` | 16 dp micro, 20 dp small action, 24 dp regular, 26 dp large, 36 dp hero |
| Color | `MaterialTheme.colorScheme` and `LocalPimsTonalSurfaces` | Surface and on-surface for reading, primary for the main action or selection, error for validation and destructive confirmation |
| Typography | `MaterialTheme.typography` | Headline for page title, title for sections, body for records and details, label for controls |
| Motion | `PersonaMotion`, `PimsAnimation`, `LocalReducedMotion` | Standard transitions and reduced-motion support |

## Spacing and alignment

- Apply safe-area and keyboard insets at the route or sheet boundary. Add the page gutter once. Use a consistent trailing edge for section actions and allow the title to wrap.
- Use a primary action when the user can commit. Use quiet actions for optional adds, attachment selection and cancelling. Give controls a minimum height and allow them to expand with font scale.
- Use the active theme's `onSurface` for the default content color and pair raised quiet controls with `surfaceContainerHigh`. Use the shared Material typography styles for input labels, values, suggestions and messages.
- Use `onSurfaceVariant` for secondary information and keep validation messages readable without relying on color. Reserve destructive color for the confirmation action. Use neutral disabled colors.
- A half-height picker scrolls independently, keeps its title and close action reachable, and uses the active surface color. If the content is empty, provide one concise, actionable message.
- Verify compact width, landscape, a wide window, dark and light themes, and 1.6× font scaling. For protected content, check the secure window and its recents behavior.

See the [component guide](COMPONENT_GUIDE.md), [icon guide](ICON_GUIDE.md), and [screen inventory](APP_WIDE_DESIGN_REMEDIATION.md).
