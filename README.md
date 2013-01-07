EnderPearlRevamp
================

Bukkit plugin for Minecraft that changes the functionality of Ender Pearls.

My community (mrgreengaming.com) runs a Factions server, which centers a lot around PvP and travelling around the map. Using this plugin I want to enhance both these elements. Normally Ender Pearls are used to chicken out of fights or to cheaply invade enemy territory. With EnderPearlRevamp, I tried not to remove these practices but rather add a cost and risk.

Usage
---------
When holding an Ender Pearl, left click the top face of a solid block to mark it.

By throwing a pearl against a block of the same type as the marked block, you will teleport to the marked block.

Details
---------
You can only have on mark per type of block. However, you can mark different types of blocks to created a personal teleportation network. Some blocks with metadata (wool, logs, stone brick, sandstone) are also considered distinct.

By default, you do not teleport instantly when your pearl hits a block, but instead you start spinning. While spinning there is a chance you drop a few items from your inventory.

Configuration
---------
See the config.yml (auto-created in the plugins/EnderPearlRevamp folder when starting the server)

 * teleportDelay: 2.5 // Delay in seconds before you are teleported. Set to 0 to make teleporting instant.
 * teleportTwister: true // Whether to start spinning during the delay period.
 * teleportPlayerDamageFraction: 0.25 // Damage done to player after teleporting, as fraction of max health (ignores armor and other effects)
 * dropShitWhileSpinning: true // Whether to drop items when the player is spinning
 * dropChancePer10thSecond: 0.1 // Chance that you drop an item every 10th of a second (range 0.0 to 1.0)
 * removeMarkAfterTeleport: true // Whether to remove the used mark after teleporation. This forces players to re-apply the mark if they want to use the same spot again.
 * teleportRequireFreeSpot: false // Whether teleportation is aborted if there is no free spot at the marked block. If disabled, players could get stuck, but this could be considered a risk of teleporation.
 
Commands
---------
Type '/epr reload' to reload the config.yml

Known issues
---------
 
 * Firework teleport effect doesn't show if you teleport over long distance since the arrival chunk isn't loaded on the client
 * Firework teleport effect will likely break during CraftBukkit version updates because of how it's coded, can't fix that yet. There is a failsafe that should prevent the entire plugin from breaking along with it.

