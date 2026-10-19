# A2-09b — Owner test (about 10 minutes)

Import `mindustry-fpv-a2-09b-diagnostics.jar` through Mods → Import Mod, then restart.
Use a disposable save/profile or a backup; keep fullscreen for the previously
identified windowed presentation issue. This is a local diagnostic, not a
published Alpha replacement. For multiplayer, both players and host must install
this exact JAR.

1. **Select and copy.** F5 → Tab → F4. Drag an X/Y area containing a small factory
   with native ground walls, supporting columns/platforms and upper buildings.
   Wheel to include all floors, then click **Copy 3D selection** (or Ctrl+Shift+C
   while 3D mode is active). Check the transparent preview contains every floor.
2. **Transform.** Move over empty ground. Wheel raises/lowers the entire structure;
   R / Shift+R rotates by 90° in either direction. Try 2×2 buildings and a
   selection touching only their outer cells. Check full footprint/height and
   conveyor orientations. Wheel over the panel should scroll UI only. Look away
   from the destination plane: clicking must not paste at the last position.
3. **Build normally.** Place close enough for your builder with sufficient recipes.
   LMB queues one batch and leaves preview; holding must not enqueue duplicates.
   Watch Z0 foundations build first, then supports/platforms/equipment. F3 should
   pause all work without deleting plans; resume and use ordinary click/Alt-click
   priority after leaving 3D selection. Resources should be spent normally and
   the new factory should start empty/full health.
4. **Invalid/cancel.** Reuse **Paste 3D clipboard** (Ctrl+Shift+V inside 3D mode).
   Try occupied space, missing support, a blocked inventory shaft and an empty
   selection. Invalid confirmation must leave the world/queue untouched. RMB
   cancels preview only; Esc/F4 leaves mode. Tab/F5/menu/Alt+Tab must cancel an
   unconfirmed preview and retain normal mouse capture. Ordinary placement,
   configuration, removal and Ctrl+F must still work afterwards.
5. **Connections.** Include an internal same-floor bridge, cross-floor item/liquid
   bridge or aligned inventory shaft, and power nodes. Check only copied endpoints
   connect; external links/configurations may show an omission warning. Keep the
   builder in XYZ range and supply connector recipes until connections finish.
   If later obstacles/support changes block work, correct them or explicitly
   clear pending construction. Save/reload the completed factory and inspect it.
6. **Separate multiplayer acceptance.** With a friend on another computer, repeat
   one small three-floor paste as client, including F3/priority, resources and
   internal connections. The other player must see the same completed structure;
   no instant buildings or duplicate costs. Test Linux input/preview separately
   if available. This has not been physically certified by automated fixtures.

Clipboard limits: 1,024 entries, 128×128 tiles, configured floors, 16 KiB encoded
request. Processor programs, memory/canvas arrays and unsupported configs are
cleared with a copy warning. No permanent export/import or clipboard persistence.
World/team reset clears the clipboard; queued upper work is transient as before.

Please report the source/destination floor ranges, rotation, block/link types,
which confirmation failed, and whether preview, costs or final connections differ.
