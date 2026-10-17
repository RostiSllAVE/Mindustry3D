# Mindustry³ Alpha 1.0.4 [v160.7]

Local release candidate; internal mod version **0.18.31**. Prepared 2026-10-17.
Repository: **RostiSllAVE/Mindustry3D**. Suggested tag: `v1.0.4-alpha.1`.
No push, public release, Topic change or Steam Workshop update is performed.

## Scope

Root `mod.hjson`, packaged source metadata and Workshop metadata are synchronized:
`hideBrowser: false`, `repo: RostiSllAVE/Mindustry3D`, `version: 0.18.31`,
`minGameVersion: 160.7`, `java: true`, `main: fpv.FpvMod`, `hidden: false`.
The internal mod name remains `mindustry-fpv` to preserve installation/save
identity. Gameplay, mouse input, physics, combat, rendering, multiplayer protocol
and payload verification code are not changed. Embedded notices/licenses remain.

The owner reports working GitHub/Workshop installation for the preceding release
and one successful launch on a separate Linux laptop. This is owner evidence,
not broad Linux certification. The validated release target remains Windows
64-bit / Mindustry 160.7. Android is unsupported and outside this update.

## Official listing checks

Checked the current [official criteria](https://github.com/Anuken/MindustryMods#criteria)
and [indexer implementation](https://github.com/Anuken/MindustryMods/blob/master/src/modupdater/ModUpdater.java).
The indexer requires discoverable root/assets metadata, the `mindustry-mod` Topic,
no browser opt-out and a valid Java release asset. It also filters archived or
template repositories, blocked names/descriptions and obsolete host versions.
Its current Java threshold is build 154 unless legacy-compatible; our 160.7
metadata exceeds both that threshold and the documented general build 136 floor.

The public repository is already public, non-archived, non-template, primarily
Java and has the required `mindustry-mod` Topic. The name/description are not
excluded by the checked filters. The display name is within the indexer's limit.
No star minimum is required for listing; its star threshold concerns optional
icon generation. No new optional icon or broad platform claim is introduced.

The local root/source metadata are ready. The published default branch and the
future release tag must receive that exact metadata before discovery can work.
Do not ask for a PR adding the mod to `Anuken/MindustryMods`; it is generated.

## Pre-release selection

The title must be exactly **`Mindustry³ Alpha 1.0.4 [v160.7]`**: the revision
marker makes the indexer produce a 160.7-specific release ID. The current indexer
does not exclude published pre-releases from this map; it excludes drafts and
prefers the newest published matching release. Mindustry 160.7 then requests that
explicit release ID and selects the uploaded JAR. Keep one player JAR asset and
do not attach an unrelated/developer or `dexed*.jar` asset.

An unqualified GitHub import otherwise uses `/releases/latest`, which
[excludes drafts and pre-releases](https://docs.github.com/en/rest/releases/releases#get-the-latest-release).
The repository currently returns HTTP 404 there because its releases are all
pre-releases. Do not rely on `/latest` to discover this Alpha; use the indexed
revision-specific release or explicit release selection. A draft release is not
publicly discoverable. A normal published pre-release with the exact title is
compatible with the tested version-specific path.

Validation distinguishes the host parser, an offline fixture executing current
upstream selection methods, and a live listing/download. The fixture omits the
indexer's scanning/file-writing instance initializer and stubs tag-metadata HTTP;
it does not run the real public indexer or publish anything. Exact test outcomes,
payload comparison and artifact hashes are recorded in `BROWSER-VALIDATION.json`
and the candidate `release-manifest.json`/`SHA256SUMS.txt`.

## Candidate verification

- JDK 17 clean build: all 113 Java test programs passed.
- Stock Steam Java 25.0.1: 14 metadata checks, 23 fingerprint checks and the
  real Win32/java.base binding test passed.
- Native import API followed by a new JVM, and compiled-folder startup: both
  passed the scripted 209-check SDL fixture plus camera, construction, mining,
  laser and save/load sub-suites. Both loaded only candidate FPV classes.
- Native Workshop pre-publish prerequisites and preview decode passed; this
  is not publication or a Subscribe/Download test.
- The real host metadata/release parser and 9 offline current-upstream selection
  checks passed, including non-draft pre-release selection and draft rejection.
- Comparing JARs with 0.18.30 found only `mod.hjson` changed. All 830 FPV classes,
  other resources and licenses are byte-identical. All 1,042 prior artifact
  files in the recorded snapshot retain their hashes.

The first ordinary fresh-profile attempts were contaminated by the owner's
existing global Workshop subscription: the duplicate import and class-origin
checks correctly rejected them. Successful reruns used a **test-only launcher
adapter** that suppresses Workshop content enumeration while retaining the stock
Steam host/runtime, native import API and an actual JVM restart. It ships in no
release/source artifact. No subscription or primary save was modified. This
is an instrumented same-PC test, not a clean-machine or physical menu test.
Physical mouse/menu/Alt+Tab acceptance comes from the preceding owner-tested
build; it was not repeated for this metadata-only candidate.

## Final owner actions after approval

1. Push the prepared local checkpoint to **main** of `RostiSllAVE/Mindustry3D`.
   Check that raw root `mod.hjson` on the public default branch reports 0.18.31,
   the corrected repo and `hideBrowser: false`.
2. Create a new tag **`v1.0.4-alpha.1` at that exact checkpoint**, then publish a
   GitHub release named **`Mindustry³ Alpha 1.0.4 [v160.7]`**. It may be marked
   pre-release; it must not remain a draft. Do not reuse an older tag/checkpoint.
3. Upload the prepared `mindustry-fpv-0.18.31.jar`, matching corresponding-source
   ZIP and `SHA256SUMS.txt`; convenience Windows/Workshop ZIPs are optional.
   Do not upload this source ZIP as a JAR or change the tested JAR's contents.
4. **Keep the existing `mindustry-mod` Topic**; no new Topic action is needed.
   Allow the indexer a refresh cycle (normally a few hours), then verify the
   `RostiSllAVE/Mindustry3D` entry and its `160.7` release ID in `mods.json`.
5. On Mindustry **160.7**, refresh/open the Mod Browser, locate the mod, install,
   restart, start a map and test F5/Tab/mouse capture. Confirm version 0.18.31 and
   compare the installed archive with the published checksum. If missing, check
   public metadata, exact release title, release visibility/JAR asset and the
   indexer's next run before changing anything else.

Workshop synchronization is a separate owner action after approval. Every
multiplayer peer must use the new version: metadata participates in the strict
payload identity, so 0.18.30 and 0.18.31 intentionally do not match. JAR and
Workshop folder from the same 0.18.31 candidate must continue to match.

Live Mod Browser installation of this candidate is **NOT TESTED** while it is
unpublished. No promise of immediate indexing is made. Historical releases and
their source/checksum archives remain untouched.
