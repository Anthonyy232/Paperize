# Paperize Maintenance Sweep

This is the maintainer cheat sheet for recurring repository sweeps. Use it to
triage reports, review contributions, verify wallpaper behavior, and publish a
release without trading a clean inbox for regressions.

## Current baseline

Update this section after each release.

- Latest audited release: `v4.1.0` (`versionCode 55`).
- Release commit: `65a2f66344234b75521b5b5e3d321583db003313`.
- Android baseline: `minSdk 31`, `targetSdk 36`, `compileSdk 37`.
- Production package: `com.anthonyla.paperize`.
- Expected production signing-certificate SHA-256:
  `deda8675d63793cb0cf7bc2decb6a2506b16b33040a1fcb1d30807a511e270ac`.
- The Renovate Dependency Dashboard (`#17`) is intentionally persistent. Do
  not close it merely to reach zero open issues.
- Production signing material belongs in GitHub Actions secrets. Never commit a
  keystore, passwords, decoded keys, or local signing paths.

## What the v4.1.0 sweep taught us

- `#586`: a launcher/adaptive icon is not a suitable notification small icon.
  Notifications need a dedicated 24 dp monochrome status-bar asset.
- `#591`: a successful manual change must reset only the schedule it affects.
  Shared home/lock scheduling resets together; independent schedules do not.
- `#594`: applying an exact image must still use Paperize's scaling, effects,
  current-wallpaper, queue, and scheduling pipeline.
- `#579`: WorkManager keeps the 15-minute minimum for static background work,
  but a live wallpaper may use a 1-14 minute timer inside its visible
  `WallpaperService.Engine` lifecycle. It must stop while hidden or previewing.
- `#588`: static wallpaper color extraction is owned by Android after a
  successful `WallpaperManager` write. AOSP Android 16/17 updated correctly;
  an intermittent GrapheneOS/OxygenOS miss has no public app-side force-refresh
  API. Reopen only for an actionable reproduction, preferably on AOSP.
- `#587`: theme-triggered album switching was rejected because Android has no
  portable background theme-state contract across OEMs, and it introduces
  persistent monitoring and conflicting scheduling precedence.
- `#593`: per-image crop/position settings were rejected because a correct
  implementation requires a full editor, persistent per-target transforms,
  URI lifecycle/migration work, and rotation/fold behavior.
- Isolated dependency PRs can fail for reasons that disappear in a coordinated
  toolchain migration. Review the complete compatibility set before merging or
  dismissing them.

## Weekly sweep

### 1. Establish a safe baseline

1. Fetch the remote and inspect the current branch, worktree, tags, latest
   release, and recent workflow runs.
2. Preserve unrelated user changes. Never reset, discard, or overwrite a dirty
   worktree to make the sweep easier.
3. For code or documentation changes, use a `codex/` branch and a focused pull
   request. Do not work directly on `master`.
4. Read the current implementation before relying on this document; the code is
   authoritative when the baseline has moved.

### 2. Inventory the whole GitHub surface

Check all of the following, not just the open-issue page:

- new and open issues;
- new comments on open **and closed** issues;
- open pull requests, reviews, and inline review comments;
- recently updated Discussions, including old threads revived by new comments;
- repository notifications;
- Renovate's dashboard and dependency PRs;
- failed, cancelled, or pending CI and CodeQL runs;
- Dependabot, code-scanning, and secret-scanning alerts;
- tags, drafts, published releases, and release assets.

Record the last checked timestamp so a later run can distinguish new feedback
from history. Historical open Discussions should still receive a maintainer
disposition when they contain a concrete request, but age alone is not a reason
to implement or close them.

### 3. Triage before changing code

Classify every meaningful item as one of:

- reproducible Paperize defect;
- valid enhancement worth the complexity;
- duplicate or already fixed;
- dependency/toolchain update that needs coordinated migration;
- Android/OEM/upstream limitation;
- unsupported or disproportionate request;
- incomplete report requiring a specific reproduction or logs;
- spam or low-substance contribution.

Judge pull requests by their behavior and evidence, not by whether a human or AI
helped write them. Warning signs include fabricated APIs, unrelated sweeping
rewrites, unexplained dependency churn, tests that never exercise the claimed
path, duplicated architecture, and prose or code volume without a reproducible
problem. Do not merge something merely to clear the queue.

Respond to substantive follow-ups. If a closed issue reveals that the prior
resolution was wrong, reopen it and later close it with the correct state reason
instead of burying the new report in a closed thread.

### 4. Preserve Paperize's functional invariants

#### Static wallpaper and scheduling

- Static periodic scheduling uses WorkManager and cannot promise an interval
  below 15 minutes.
- A manual change resets a countdown only after the wallpaper write succeeds.
- Synchronized home/lock schedules reset together. Independent schedules reset
  only the changed target. Disabled scheduling stays disabled.
- The app, launcher shortcut, and Quick Settings tile must share the same change
  and reset path.
- Render against the stable physical display panel/natural orientation, not the
  foreground app's transient orientation or a launcher's wide desired canvas.
- Preserve home/lock separation, optional horizontal home scrolling, queue
  state, and the recorded current wallpaper.

#### Live wallpaper

- A 1-14 minute timer belongs to the visible, non-preview live wallpaper engine;
  it must not continue as hidden background work.
- Restart or cancel the timer when visibility, configuration, selected album,
  mode, pause state, or manual wallpaper state changes.
- Screen-off and double-tap changes must use the established renderer/reload
  lifecycle rather than creating competing schedulers.
- Keep rendering responsive while avoiding a permanent foreground service for
  short live intervals.

#### Exact image application

- Apply the selected image ID/URI, not a random replacement from the album.
- Offer Home, Lock, and Home-and-lock targets in static mode.
- Preserve the target's current scaling and effects.
- Apply both targets atomically when their presentations match; otherwise keep
  their independent presentation rules.
- Update current/queue state and reset the affected timer only after success.

#### Platform-owned behavior

- Verify dynamic wallpaper colors with deliberately distinct inputs before
  blaming Paperize or an OEM.
- Do not claim an app fix for behavior owned by Android when the same public API
  passes on AOSP and there is no supported force-refresh hook.
- Keep `targetSdk` changes deliberate: review behavior and permission changes,
  not just whether a newer SDK compiles.

## Verification gates

Scale the test effort to the change, but a release that touches wallpaper or
scheduling behavior should cover this matrix.

### Automated gates

- Debug and release Kotlin compilation without new compiler warnings.
- All unit tests, with explicit test/suite/failure/skip counts.
- `lintDebug` with zero errors; inspect and account for warnings in touched code.
- A minified release build to exercise R8 and resource shrinking.
- Instrumentation on a phone-sized AOSP emulator and a foldable/API-latest AOSP
  emulator.
- CodeQL for Java/Kotlin and GitHub Actions.

### Static matrix

- Home, Lock, and Home-and-lock targets.
- FIT, FILL, STRETCH, and NONE with portrait and landscape sources.
- Horizontal scrolling on and off.
- Natural-orientation output while another activity is landscape.
- Blur, darken, grayscale, vignette, and adaptive-brightness effects.
- Exact-image application and persisted current/queue state.
- Dynamic colors using obviously different solid-color wallpapers.

### Live matrix

- Every shader compiles in a real EGL context.
- Pixel-level checks for blur passes, darken, grayscale, vignette, and adaptive
  brightness; exercise parallax on-device.
- Visible short-interval rotation, no hidden rotation beyond the same interval,
  preview behavior, double-tap reload, screen-off behavior, and configuration
  changes.
- Home/lock live-wallpaper placement and fold/unfold sizing.

### Scheduling matrix

- Shared and independent Home/Lock schedules.
- Enabled, paused, and disabled states.
- Boot restoration without duplicate work.
- Manual changes from the app, launcher shortcut, and Quick Settings tile.
- WorkManager intervals of 15 minutes or more and live-engine intervals below
  15 minutes.
- Confirm reset timing from actual scheduled-job state, not only a mocked call.

## Release gate

Do not publish a release merely because the tracker is tidy.

1. Merge only after the pull-request test and CodeQL checks pass.
2. Let `master` CI build with repository signing secrets.
3. Download that APK and verify:
   - package name, version code/name, min/target/compile SDK, and permissions;
   - one valid signer and certificate continuity with the prior release;
   - the expected signature scheme;
   - successful installation and cold launch on an emulator;
   - no startup crash log.
4. Create an annotated `vX.Y.Z` tag on the audited merge commit.
5. Wait for the immutable-tag workflow and verify its attached APK checksum and
   signing certificate again.
6. Match the established release style:
   - tag: `vX.Y.Z`;
   - title: `vX.Y.Z`;
   - body headed `## vX.Y.Z`, concise user-facing bullets, then a **Full
     Changelog** comparison link.
7. Publish only after the asset is verified, then confirm it is non-draft,
   non-prerelease, and marked Latest.

Never expose signing secrets in logs, comments, artifacts, documentation, or
shell output. The certificate fingerprint and APK checksum are safe to report;
the private key and passwords are not.

## Closing and communication standards

- Close implemented work as **completed** and link the release containing it.
- Close rejected or upstream-owned work as **not planned**, with the reason and
  the evidence that informed it.
- Use `wont-add`, `wontfix`, or `question` labels when they clarify the decision.
- Explain root cause, scope, and verification in plain language. Do not tell a
  reporter something is fixed without testing the path they described.
- Add a concise supersession note to dependency PRs replaced by a coordinated
  update.
- Do not close Renovate's Dependency Dashboard.
- A clean sweep means no untriaged actionable feedback, not artificially zero
  open items.

## Completion checklist

- [ ] New issues and comments, including closed-issue follow-ups, are triaged.
- [ ] Open PRs, reviews, and review comments are resolved or intentionally open.
- [ ] Recently active Discussions have a maintainer disposition where needed.
- [ ] Dependency and security alerts are clear or explicitly documented.
- [ ] Accepted fixes have proportionate automated and device evidence.
- [ ] CI and CodeQL are green for the exact merged/released tree.
- [ ] Any release APK preserves signing identity and passes install/launch checks.
- [ ] Issue/PR responses link evidence and use the correct state reason.
- [ ] The only remaining open items are intentional and explained.
- [ ] Local `HEAD`, `origin/master`, and the release tag agree where expected.
- [ ] The worktree is clean and unrelated user changes were preserved.

## Useful commands

Run commands from the repository root and adjust timestamps/version numbers.

```powershell
gh issue list --state open --limit 100
gh pr list --state open --limit 100
gh api --paginate "repos/Anthonyy232/Paperize/issues/comments?since=<timestamp>&per_page=100"
gh api --paginate "repos/Anthonyy232/Paperize/pulls/comments?since=<timestamp>&per_page=100"
gh run list --limit 20
gh release view <tag>

.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat compileDebugKotlin compileReleaseKotlin
.\gradlew.bat connectedDebugAndroidTest

git status --short --branch
git rev-parse HEAD
git rev-parse origin/master
git rev-parse '<tag>^{}'
```
