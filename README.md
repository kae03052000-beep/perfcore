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

## What's new in 1.2

### 1. Light Engine Cache
- Problem: When blocks change (explosions, /fill), the game recalculates light in a 15-block radius thousands of times, often for the same position.
- Solution: Deduplication of checkBlock calls within a single game tick. If the block hasn't changed since the last call, light is not recalculated. The cache lives for one tick, then clears.
- Effect: Fewer TPS drops during explosions and mass block updates.

### 2. Pathfinding Cache
- Problem: When a mob chases a player, recomputePath() is called every tick, even if the target moved half a block.
- Solution: Throttling recomputePath to once every 2 ticks. The mob doesn't teleport, it already has a valid path, it just recalculates a bit later.
- Effect: ~50% fewer pathfinding calls. Noticeable on mob farms.

### 3. Smart Chunk Culling (v1)
- What it does: Skips rebuilding sections that are guaranteed empty (air only, no block entities). Saves CPU at high altitudes and in oceans.
- What it doesn't do (planned for 1.3): Full occlusion culling for chunks hidden behind walls. Requires analysis of SectionOcclusionGraph and shader testing.

---

## What was in 1.1.1

- Entity Freeze (Items/XP): If no players are within 100 blocks, physics ticks once per second instead of 20 times.
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
- config/perfcore-common.toml — TNT, mob AI, world gen, light cache, pathfinding.
- config/perfcore-client.toml — particles, rendering, sounds, GPU buffers.

Every optimization can be disabled individually. Enable logStats = true for debug output.

---

## Installation
1. Install Minecraft Forge 26.3 (version 66.x).
2. Download perfcore-1.2.0.jar.
3. Place the file in .minecraft/mods.
4. Launch the game.

Compatibility: The mod uses Mixin injections with require = 0 for risky patches, making it compatible with most other mods. However, if you use other optimizers (FerriteCore, Embeddium), conflicts are possible. Test in combination.
