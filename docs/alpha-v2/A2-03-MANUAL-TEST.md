# A2-03 — Three owner checks

No developer tools or separate Java needed. Use Mindustry 160.7 and the enclosed
`mindustry-fpv-a2-03-diagnostics.jar`, imported through **Mods -> Import Mod ->
Import File**, then restart. It has the same compiled payload as A2-02; it is not
a new public campaign hotfix. Disable any subscribed/imported duplicate for the
test. Use Windows Fullscreen to avoid the known windowed performance problem.

**Protect your data first:** export a backup and use a disposable game profile.
Verify Mods -> Open Folder points inside that separate test area before importing
any campaign backup. A copied Linux executable does not by itself establish a
separate profile. If isolation is uncertain, skip the existing-save check and
report NOT TESTED. Never overwrite the only primary campaign for this test.

1. **Fresh offline campaign:** start Ground Zero in the disposable profile. Check
   F5, continuous mouse look, Tab, a normal available construction operation and
   return to flight. Once the necessary blocks are researched/resources available,
   add a supported upper structure. Save, leave to menu and reload. Check core
   resources, unit, upper blocks and controls. Do not use Free Mode to hide a
   support/permission error.
2. **Sector transition:** use the normal planet screen to enter an unlocked
   neighboring sector, then return to the first saved sector. Check the right
   terrain/core/units/upper structures are present in each, without duplicates or
   structures leaking into the other sector. Try Onset/Erekir separately if
   available; lack of unlocked progress is NOT TESTED, not a failure.
3. **Previously affected campaign COPY:** in a verified separate profile, import
   your exported campaign backup. Test entry -> F5/Tab -> save/reload -> another
   available sector -> return. Repeat the available checks on your Linux laptop
   and record it separately from Windows. If it fails, give the exact visible
   error and the first failing step, game/mod version, planet/sector and whether
   the same copied campaign works with the mod disabled after restart.

For each check report PASS / FAIL / NOT TESTED. Include a cropped screenshot if
useful; hide names/private notifications. If no visible error exists, say whether
world loading, player spawn, F5, building or the sector transition is what fails.
Do not send a whole profile or unsanitized log. Share only a short relevant error
excerpt with paths, usernames, IPs and Steam identifiers removed. This task does
not require sending your save file.

Hosted campaign is a separate future two-computer acceptance test. The local
native checks do not certify it. If later testing hosting, use the exact same
payload on both machines and the A2-02 diagnostic procedure; do not disable
compatibility checks. Do not publish the test JAR or update Workshop. Restore
the public mod after testing and keep experimental profiles separate.
