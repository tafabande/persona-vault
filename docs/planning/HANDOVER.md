# Overhaul handover

Review [the implementation plan](IMPLEMENTATION_PLAN.md) and [roadmap](ROADMAP.md) for status. See [testing](TESTING_GUIDE.md) for the requirement-by-requirement checks and [changelog](CHANGELOG.md) for the behavior changes.

## Source map

- Shared Compose controls live under `presentation/ui/components`. Theme rules and approved icons live under `presentation/ui/theme`. See [design](DESIGN_SYSTEM.md), [components](COMPONENT_GUIDE.md), and [icons](ICON_GUIDE.md).
- Dashboard navigation owns Me, the Library, and existing protected routes. Me stores ordered references in an encrypted, account-scoped layout preference. Open records through their source workflow. See [ADR-027](adr/ADR-027-me-overview-references.md).
- The dashboard shell and full-screen routes compile as separate composables to control DEX register pressure. Run `DashboardBytecodeDeviceTest` after changes to dashboard navigation or generated Compose code.
- Room/SQLCipher owns local records. Use the durable outbox and cloud account binding rules for background sync. See [data architecture](DATA_ARCHITECTURE.md) and [offline sync](OFFLINE_SYNC.md).
- The PIN verifier and Android credential session are the authorization boundary. Zone 4 requires the existing hardware-authenticated key operation. See [security audit](../security/SECURITY_AUDIT.md).

## Continue verification

Run the guide from a clean checkout with the Android SDK and existing local build configuration. Use an isolated test account and a second device for recovery and conflict exercises. Review active inventory items before claiming every screen is migrated. Preserve historical readers and keep verified data and shared controls at their current boundaries.
