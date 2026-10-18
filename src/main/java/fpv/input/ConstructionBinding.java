package fpv.input;

import arc.input.KeyBind;
import arc.input.KeyCode;

/** E/Q remain flight altitude keys; F10 remains command-mode exit. Uses the host Controls UI. */
public final class ConstructionBinding {
    public static final KeyBind toggle=KeyBind.add("fpv-construction-toggle",KeyCode.f3,"FPV construction");
    private ConstructionBinding(){}
    public static String key(){return toggle.value.key==null?toggle.name:toggle.value.key.toString();}
}
