# A2-16l logic — Owner acceptance (three steps)

Import `mindustry-fpv-a2-16l-logic-diagnostics.jar` through Mindustry 160.7 Mods → Import Mod, replace the previous diagnostic copy, restart and enable one copy. Use a disposable/backup save. The host game is installed separately. Fullscreen remains recommended on Windows. The same JAR targets the currently supported Windows/Linux desktop hosts; Linux physical acceptance is pending. No new keys are required.

1. **Models and controls.** Place the three processors, two memory blocks, both displays, Switch and Message. Inspect top/side/FPV, toggle the real switch using its normal interaction, read/edit a message, and check the accepted factory/reconstructor models nearby. Link memory normally and try native read/write instructions. Hyper Processor still needs its original coolant.
2. **Live graphics.** Link a processor to a display as `display1`, paste the example below, and inspect it in F5/FPV and Tab/Build Mode. Run separate processors on two displays; change the clear/color/text commands and verify independent updates, correct text orientation and no stripes/clipping. Both 80×80 and 176×176 displays retain their own native drawing coordinates.
3. **Floors and persistence.** Repeat on valid Z1/Z3/Z5 platforms, stack small screens with enough headroom, and save/reload while the program runs. Large displays require their existing 2.78-tile clearance (use more vertical spacing). Try the existing 3D clipboard: it warns that processor code/links are omitted; reconnect/configure the copied pair normally and verify live graphics. Check F3 construction pause, priority Alt-click, F5/Tab and menus as usual. For multiplayer, use the exact same JAR on both peers and report remote displays separately.

Example tested native graphics syntax (link name `display1`):

```text
draw clear 15 20 30 0 0 0
draw color 60 210 240 255 0 0
draw rect 5 5 25 20 0 0
draw stroke 2 0 0 0 0 0
draw line 2 2 75 75 0 0
draw image 40 40 @copper 16 0 0
print "LIVE"
draw print 4 66 @left 0 0 0
drawflush display1
end
```

Message text remains in the native inspection/configuration UI. Vanilla does not save framebuffer pixels; saved active programs redraw them after loading. Physical owner acceptance and real two-computer networking are not inferred from the automated tests. The source ZIP is GPL corresponding source, not the file to import as the mod.
