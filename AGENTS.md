# Working on Paperize

An Android app that changes the home and lock screen wallpaper on a schedule: Kotlin, Jetpack Compose, Material 3, Hilt, Room. Users get it from GitHub releases, F-Droid and IzzyOnDroid.

## Code

- Simple and direct. No speculative abstractions, options or extension points.
- Stay in scope; suggest extras instead of adding them.
- Use what the platform, framework, dependencies and this repo already provide before writing a helper.
- Validate only at real boundaries (user input, storage, network, other processes). No checks, fallbacks or try/catch for cases that can't happen.
- One source of truth for each value or rule.
- Delete dead code you find or create.
- Match the surrounding code.
- Fix root causes, and fix the same pattern everywhere it occurs.

## Comments

- Only where the reason isn't clear from the code: a quirk, a race, a non-obvious decision.
- Short fragments, not sentences. Never restate code, narrate, or label sections.
- Delete comments that no longer match.

## Tests

- Behaviour and the edges that can break. No trivial, duplicate, obvious or low-signal tests.
- A regression test fails without its fix.
- Deterministic: fake time and observable events, no sleeps.
- A failing test is a bug until shown otherwise.

## Paperize

- The UI can only be seen on an emulator or device. Run `./gradlew connectedDebugAndroidTest` there for anything touching rendering, scheduling, storage or imports.
- Dynamic colour swaps the brand palette for the system one, so a design must work with it on and off.
- English is the source language. Translations come through Crowdin: the sync workflow uploads `values/strings.xml` and opens its own pull request with approved translations only, adding a locale once most of it is approved. Add or reword strings in English only, and remove deleted strings from every locale. A locale file fixed by hand must also be sent to Crowdin with the sync workflow's upload option, or a later sync can replace the fix.
- A database schema change needs a migration that keeps user data, never a destructive fallback. Commit the exported schema.
- `metadata/en-US` is the F-Droid and IzzyOnDroid store listing: keep its text and screenshots true to the app.
- F-Droid rebuilds every release from source and compares it with the GitHub APK, so the build must stay reproducible. Its recipe deletes the `foojay` line from `settings.gradle.kts` with `sed`: keep that plugin on one line, and keep `dependenciesInfo` off.
- F-Droid pins the commit of every version: never rewrite a released commit or move a tag. `renovate/*`, `crowdin-translations` and `media` are live branches, not stale ones.
- `app/lint-baseline.xml` holds only deliberate exceptions; fix new warnings instead of adding them to it.

## Done means

- Diff reviewed against this file.
- `./gradlew testDebugUnitTest lintDebug` passes with zero warnings.
- UI changes seen in the running app, light and dark. Docs and metadata changes need no end-to-end run.
- README updated for anything users will notice; behaviour changes called out.

## Docs

- README is for users: what it does, how to get it, how to use it. No internals, no agent instructions.
- This file is for agents: only what can't be found quickly in the code.
- No meta docs in the repo (notes, audits, plans, reports).

## Git

- `type: summary` or `type(scope): summary`: lowercase, imperative, no full stop, about 70 characters at most. Types: `feat` `fix` `refactor` `perf` `test` `docs` `style` `build` `ci` `chore`.
- Body only to say why. `Fixes #123` closes an issue.
- One logical change per commit. Squash fix-ups before merging.
- Branch off `master`. A single commit fast-forwards; several merge with `merge: <summary>`.
- No AI attribution in commits, branches, PRs or releases.
- One maintainer: amending and force-pushing to tidy history is fine. Keep contributors as authors. Delete stale branches.

## Pull requests

- Title in commit format. Body: what changed, why, how it was checked. Short.

## Releases

- Bump commit `chore: release vX.Y.Z`; tag `vX.Y.Z`.
- A pushed `v*` tag is the store submission. F-Droid picks the tag up by itself as soon as it is pushed, builds it, and compares its build with the release APK. CI attaches the signed `paperize-vX.Y.Z.apk` to the draft release for that tag, reusing a draft that already has the notes, but the APK is not public while the release is a draft: publish the draft as soon as CI has attached the APK, or the F-Droid comparison fails and has to be retried. Never rename a tag or asset.
- Title `Paperize X.Y.Z`.
- Notes: banner image first, then `### New`, `### Improved`, `### Fixed` (only those that apply). One line per change, written for users; no internals, dependency lists or test counts. End with the install links (APK, F-Droid, IzzyOnDroid) and the full changelog link.
- Banner: generated and hosted on the orphan `media` branch; use `https://raw.githubusercontent.com/Anthonyy232/Paperize/media/releases/vX.Y.Z.png`.
- The README, site and store listing images are built on the `media` branch too, from emulator captures; its README has the steps for both.
- The site in `site/` hosts the privacy policy the store listing links to at `#privacy`; keep that anchor.

## Agents

- Pushing, amending and force-pushing are allowed. Ask before publishing a release or submitting to a store.
- Leave changes you didn't make alone. Clean up scratch files, processes and emulators.
- Say what was checked and how, and what wasn't.
- Short replies; one recommendation, not a list of options.
