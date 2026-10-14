# Contributing to Deen

Thank you for helping. A few ground rules keep the app trustworthy:

1. **Privacy.** No analytics, trackers, ads or crash-reporting SDKs. The `foss` flavor must not depend on any
   proprietary library; CI enforces this.
2. **Architecture.** Features depend on core modules only (`feature → domain ← data`), never on each other.
   See [docs/BLUEPRINT.md](docs/BLUEPRINT.md) §4–6.
3. **Formatting.** Run `./gradlew spotlessApply` before committing (ktlint, Android Studio style).
4. **Religious content** (adhkar, hadith, translations, calculation presets) needs a source, a grading where
   relevant, and sign-off from a qualified reviewer. Please open an issue before large content changes.
5. **Alarm bugs.** Include your phone model, ROM name and version (e.g. "Redmi Note 13, HyperOS 2.0 global"), and
   once it exists, the alarm health log from the Reliability screen.

By contributing you agree that your code is licensed under GPL-3.0-or-later.

## Licence of contributions

By contributing, you agree that your contribution is licensed under GPL-3.0-or-later with the
additional terms in [NOTICE](NOTICE), and that the name, icon and releases of Deen stay with its
author (see [TRADEMARKS.md](TRADEMARKS.md)).
