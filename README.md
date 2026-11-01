# HACK CLIENT (visual-only demo mod)

Minecraft 26.2 - Fabric - Java 25.

Modules (KillAura, Fly, Speed, ESP, Scaffold, NoFall, AutoSprint) are **cosmetic toggles**: they only store an on/off flag
for the GUI. No mixins, no entity access, no packets, no player/world changes.

## Build
    gradle wrapper --gradle-version 9.5.1     # once, if gradle/wrapper/gradle-wrapper.jar is missing
    ./gradlew build
JAR: build/libs/hack-client-1.0.0.jar  (needs Fabric API 0.150.1+26.2 or newer in mods/)

## Use
Right Shift opens the ClickGUI. Click a module to toggle, click its key chip to bind (Backspace = unbind).
Settings tab rebinds the ClickGUI key. Toggles are saved in config/hack_client.json, keys in options.txt.
