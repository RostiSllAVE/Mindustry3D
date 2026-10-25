# A2-18 — Distribution checklist

Local preparation only. Proposed tag **v2.0.0-rc.1**, title **Mindustry³ Alpha 2.0.0 RC1 [v160.7]**, internal version **0.19.0**, prerelease=true. No tag, remote release, topic change, push or upload is created by this task. Promote unchanged bytes only after acceptance; any code/resource change needs a new internal version, build, checksums and matching source.

## Build provenance and local artifacts

- Accepted gameplay baseline: `3fc6f3ea6d8f2dda8931559f2498938dae51d420`, main. All 1,081 FPV class files in the production RC are byte-identical to that accepted diagnostic. Only `mod.hjson` differs in the runtime payload. Protocol 4/save formats remain unchanged.
- Production JAR: `dist/mindustry-fpv-0.19.0.jar`; final immutable package directory: `dist/alpha-2.0.0-rc.1/`. Source checkpoint/hash are recorded in `release-manifest.json` and inside `SOURCE-MANIFEST.json`.
- JAR is ready to import. `mindustry-fpv-0.19.0-windows.zip` contains that exact JAR, instructions and notices; players extract first. Linux owner testing uses the identical desktop JAR, not a different binary.
- `workshop/Mindustry3/` contains compiled classes/resources at the mod root plus the preview; `mindustry-fpv-0.19.0-workshop.zip` packages this upload folder. It is not the GitHub install ZIP. No host game/JRE is included.
- Corresponding-source ZIP is a clean Git archive of the checkpoint, never a whole-folder copy. It contains source/build scripts/design descriptors/notices and per-file SHA-256 manifest; no Git history, SDK, profiles, saves, recording or test binaries. JNA 5.19.1 is a separately acquired build dependency with pinned checksum/notices; its upstream source locations are in THIRD_PARTY_NOTICES.
- `SHA256SUMS.txt` covers loose release files including `release-manifest.json`; the checksum file does not hash itself. Workshop content is verified entry-by-entry and by canonical payload identity. Source is freshly compiled and class bytes compared with the production JAR.

Developer commands from the repository root (Python/JDK are developer dependencies only):

```text
python tools/build.py
python tools/test-diagnostic-runtime.py --build build --jar dist/mindustry-fpv-0.19.0.jar --java bundled25 --run NEW-ID
python tools/release-install-probe.py --jar dist/mindustry-fpv-0.19.0.jar --java bundled25 --run NEW-ID
python tools/release-render-checks.py --jar dist/mindustry-fpv-0.19.0.jar --java java17 --run NEW-ID
python tools/distribution.py --jar dist/mindustry-fpv-0.19.0.jar --out NEW-UNUSED-DIRECTORY --source-commit VERIFIED-CLEAN-HEAD --release-title "Mindustry³ Alpha 2.0.0 RC1 [v160.7]" --tag v2.0.0-rc.1
python tools/verify-release.py dist/alpha-2.0.0-rc.1
```

`FPV_MINDUSTRY`/ignored local runtime configuration select the separately installed official standalone 160.7 host; `FPV_JDK` selects full Java 17. Existing builds/output directories are deliberately not overwritten. Source rebuild details and exact SDK hashes are in the local validation evidence. Original standalone files and prior Alpha artifacts are hashed before/after.

## Mod Browser, checked against current official code

The root and packaged metadata agree: repo `RostiSllAVE/Mindustry3D`, java=true, main `fpv.FpvMod`, minGameVersion 160.7, hideBrowser=false and hidden=false. Current public repository is Java, public, nonarchived, nontemplate and already has topic `mindustry-mod`. The public index currently contains the older 0.18.31 release; it does **not** prove this unpublished RC is available.

The official [listing rules](https://github.com/Anuken/MindustryMods#readme) require root/assets metadata, the discovery topic, a supported minimum game version and a compiled Java release. Current [indexer source](https://github.com/Anuken/MindustryMods/blob/master/src/modupdater/ModUpdater.java) also filters obsolete Java minimum builds and builds a revision-specific release map. This candidate meets the local metadata thresholds.

The release selector was executed offline using current upstream methods and actual installed 160.7 classes: published matching prereleases can be selected, drafts are excluded, and `[v160.7]` targets that revision. Therefore **a published prerelease is not hidden from the Browser**. Keep the RC local until approved. The host [asset selector](https://github.com/Anuken/Mindustry/blob/v160.7/core/src/mindustry/ui/dialogs/ModsDialog.java) must find one player JAR; do not attach extra diagnostics/dev/dexed JARs. GitHub's unqualified latest-release endpoint does not supply prerelease-only releases, so use explicit release identity/title selection. These fixtures do not perform a real download/indexing/install.

## Only after explicit publication approval

1. Confirm owner acceptance, clean checkpoint and verified package hashes. Choose final title/tag; if changing binary metadata/version, rebuild and revalidate instead of renaming files only. Retain earlier Alpha releases.
2. GitHub: publish the approved checkpoint with the correct revision title and exactly one installable JAR, convenience ZIP, corresponding source, release notes and checksums. Download through a browser on a clean profile, compare checksum, import/restart/F5/Tab/save. Also test the source archive's provenance. Do not upload raw local profiles/logs or the game runtime.
3. Workshop: owner uploads the verified compiled folder using the existing approved item, preserving its item ID/settings. Wait for actual subscription download; remove duplicate imported copy, restart, verify installed 0.19.0, F5/capture/Tab/save. Compare canonical identity with the GitHub binary in a real mixed-installation multiplayer session. Local `prePublish()` prerequisites passing is not Subscribe→Download→Restart acceptance.
4. Browser: wait for indexing, inspect the public record/release ID and version. Install from the actual 160.7 Browser into a new profile, restart and confirm exact version, F5/capture/Tab. Check updating from an older public release and verify no wrong/multiple JAR is chosen. Do not claim discovery before this happens. No registration PR is needed; do not change Topics automatically.
5. Record channel, date, actual download hash, version and outcome. Any wrong asset, startup failure or identity mismatch stops rollout and requires a new candidate; do not overwrite an existing release to conceal it.
