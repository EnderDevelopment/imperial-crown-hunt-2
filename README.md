# Minecraft Plugin: Imperial Crown Hunt

Imperial Crown Hunt is a Minecraft plugin for a timed crown race: it builds a protected spawn event structure, locks a crown chest for two days, and sends the first player to finish a short parkour course to claim the crown.

## Install

1. Use a Paper server compatible with Minecraft 1.21.11 and Java 21.
2. In the project, add an empty `src/main/resources/config.yml` before building. The supplied source calls `saveDefaultConfig()`, but the provided files do not include that resource.
3. Build the plugin with `mvn package`.
4. Copy `target/plugin.jar` into the server’s `plugins` folder and restart the server. On first startup, the plugin builds its event structure near the first world’s spawn.

## Use

- The chest becomes openable after two days. Before then, attempts to open it are blocked.
- Opening the unlocked chest starts the event and teleports the player to the parkour course, which is placed about 20,000 blocks from spawn.
- Reach the emerald-block finish to claim the crown, if nobody already has it.
- The crown is an unbreakable golden helmet with custom model data and an armor attribute. While the crown is found in a player’s inventory, the plugin applies Strength II and Speed I.
- Use `/imperial status` to check the chest state and remaining time, `/imperial open` to unlock it immediately, or `/imperial reset` to restart the two-day countdown. These commands require `imperial.admin`, granted to operators by default.
- The plugin protects the area around the event chest from block breaking and explosions. Crown drops on death depend on normal server inventory and gamerule behavior.

The supplied files include a crown SVG texture asset, but not a complete resource pack or automatic resource-pack setup. The declared `setcastle` and `setparkour` commands are not implemented.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/2d3b3711-454a-4d6c-957d-2285e2c59a45?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=2d3b3711-454a-4d6c-957d-2285e2c59a45
