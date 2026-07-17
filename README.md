# VisorEssentialsPlus

This is an expanded version of VisorEssentials 0.4.0 for Minecraft 1.21.1 on
NeoForge. It adds custom buttons, a creative inventory menu, and support for
other addons that rely on VisorEssentialsPlus to function.

Requires [Visor 0.4.0](https://github.com/VisorModStudio/Visor), NeoForge
21.1.234 or newer, and Java 21.

## Building

Provide the Visor 0.4.0 NeoForge JAR when building a standalone checkout:

```powershell
.\gradlew.bat build -Plocal_visor_jar="<path-to-Visor-0.4.0-NeoForge-1.21.1.jar>"
```

Based on the original VisorEssentials project by PhoenixRa.
