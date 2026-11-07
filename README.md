# PerfCore
Optimization without compromises for Minecraft 26.3 (Forge)

PerfCore is a lightweight mod designed to solve the most annoying performance issues in modern Minecraft. Instead of rewriting the engine, the mod surgically patches the hot paths that cause freezes and FPS drops, without breaking vanilla gameplay.

---

## License

Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)
Copyright (c) 2026 Puging_Pug.

This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.

You are free to:
- Share: Copy and redistribute the material in any medium or format.
- Adapt: Remix, transform, and build upon the material.

Under the following terms:
- Attribution: You must give appropriate credit to Puging_Pug, provide a link to the license, and indicate if changes were made.
- NonCommercial: You may not use the material for commercial purposes.
- ShareAlike: If you remix, transform, or build upon the material, you must distribute your contributions under the same license as the original.

Full legal text: https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode

---

## What's new in 1.3

This release is a bug-fix and cleanup release.

### Fixed
- Entity Freeze (Items/XP): only resting entities are skipped now. Items and XP orbs no longer hang in the air, the time skipped is added to their despawn timer (before, despawn was up to 20 times slower), and XP orbs can merge again.
- Sound Throttling: music and interface sounds are never cut off. The counter moved out of the mixin class into the mod code, because mixin classes must not be referenced directly.
- TNT: deferred TNT waits on the last fuse tick and retries every tick, so up to `maxExplosionsPerTick` explosions happen each tick instead of one batch every two ticks.
- Dynamic render distance: manual changes of the render distance are tracked at all times, so the mod always restores the value you chose.
- Fast World Creation: the patch only applies when vanilla passes its 11x11 search size, and it is skipped safely if the target code is missing in a game version.
- Mixin config is no longer mandatory: if a patch stops matching after a game update, it is skipped with a warning in the log instead of crashing the game.
- Version numbers in `build.gradle`, `mods.toml` and the jar are unified.

### Removed
- Light Engine Cache: it ran on the lighting thread and read blocks through the main-thread level, which can block the game.
- Pathfinding Cache: an integer overflow made it cancel every `recomputePath()` call, so mobs never recomputed their paths after block changes.
- Smart Chunk Culling: cancelling a rebuild when a section became empty left the old geometry on screen.

---

## What was in 1.2

- Light Engine Cache, Pathfinding Cache and Smart Chunk Culling (v1). All three were removed in 1.3, see above.

## What was in 1.1.1

- Entity Freeze (Items/XP): If no players are within 100 blocks, items and XP orbs are not ticked every tick.
- Sound Throttling: During explosions, excess sounds are cut off (default 8 per tick).

## What was in 1.1

- TNT Explosion Fix: TNT explodes in batches instead of all at once.
- Smart Mob AI Optimization: Distant mobs skip some AI ticks.
- Fast World Creation: Spawn search radius reduced to 7x7 chunks.
- Client Optimizations: Particle culling, entity culling, dynamic render distance, GPU buffer fix.

---

## Proof of Efficiency (1.1 test)

During a stress test with 2014 TNT blocks:
- The server experienced zero freezes.
- The mod deferred 1514 explosions to subsequent ticks, maintaining stable TPS.
- Mob AI load was reduced by 50% (55,984 ticks of AI skipped for distant mobs).

---

## Configuration

All settings can be changed in-game or via files:
- config/perfcore-common.toml — mob AI, TNT, resting items and XP, world creation.
- config/perfcore-client.toml — particles, rendering, sounds, GPU buffers.

Every optimization can be disabled individually. Enable logStats = true (common) or debugLog = true (client) for debug output.

---

## Installation
1. Install Minecraft Forge 26.3 (version 66.x) with Java 25.
2. Download perfcore-1.3.0.jar.
3. Place the file in .minecraft/mods.
4. Launch the game.

Compatibility: The Mixin config is not required, so a patch that no longer matches the game is skipped and reported in the log instead of crashing the game. If you use other optimizers (FerriteCore, Embeddium), conflicts are possible. Test in combination.
