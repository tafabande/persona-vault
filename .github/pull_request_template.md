## Summary of Changes
<!-- Concise explanation of what was changed and why -->

## Security Review Checklist
- [ ] No plaintext secrets added to entities, logs, tests, or diagnostic payloads.
- [ ] Cryptographic domain separation verified (`PIMS/*`).
- [ ] Anti-replay and version monotonicity preserved.
- [ ] No new unpinned or floating dependencies in `gradle/libs.versions.toml`.
- [ ] `FLAG_SECURE` maintained on sensitive UI presentation layers.
- [ ] Best-effort zeroization executed on temporary secret buffers.

## Test Verification
- [ ] All unit and module tests pass locally (`./gradlew testDebugUnitTest`).
- [ ] Adversarial attack tests pass (`VaultAdversarialAttackTests`).
- [ ] Android Lint checks pass without new baseline warnings.
