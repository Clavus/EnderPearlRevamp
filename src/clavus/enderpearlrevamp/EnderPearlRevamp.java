package clavus.enderpearlrevamp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_4_6.CraftWorld;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftFirework;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import clavus.enderpearlrevamp.runnable.ParamRunnable;

public class EnderPearlRevamp extends JavaPlugin
{
	private EnderPearlRevampListener epListener = new EnderPearlRevampListener(this);
	private String chatTag = ChatColor.AQUA + "o ";
	private String consoleTag = "[EPR] ";
	
	private FileConfiguration config;
	private FileConfiguration playerData;
	private File playerDataFile;
	
	private HashMap<String, PlayerPearlNetwork> pearlNetwork = new HashMap<String, PlayerPearlNetwork>();
	private HashMap<Player, Integer> twisterTasks = new HashMap<Player, Integer>();
	
	private boolean craftBukkitUpToDate = true;
	private Logger log;
	
	public EnderPearlRevamp()
	{
		log = Logger.getLogger("Minecraft");
	}
	
	public void onEnable() 
	{
		getDataFolder().mkdirs();
		loadConfig();
		loadPlayerData();
		
		Settings.parseConfig(log, config);
		
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(epListener, this);
		
		PluginDescriptionFile pdfFile = this.getDescription();
		print(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
		
		craftBukkitUpToDate = checkClass("org.bukkit.craftbukkit.v1_4_6.CraftWorld");
	}
	
	public void onDisable() 
	{
		savePlayerData();
		
		PluginDescriptionFile pdfFile = this.getDescription();
        print("Closing " + pdfFile.getName() + "!");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) 
	{
		Player pl = null;
		if (sender instanceof Player) {
			pl = (Player) sender;
		}
		
		if (pl != null && !pl.isOp()) {
			sendMessageTo(pl, "Ops only ;)");
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("epr")) {
			
			if (args.length <= 0) { 
				sendMessageTo(pl, cmd.getUsage());
				return true;
			}
			
			String action = args[0];
			
			if (action.equalsIgnoreCase("reload")) {
				reloadConfig();
				config = getConfig();
				Settings.parseConfig(log, config);
				sendMessageTo(pl, "Config reloaded!");
				return true;
			}
		
		}
		
		return false;
	}
	
	//// Player pearl handling ////
	
	// I really oughta clean this up a bit but whatever
	
	public PlayerPearlNetwork getPN(Player pl)
	{
		String name = pl.getDisplayName().toLowerCase();
		PlayerPearlNetwork pn = pearlNetwork.get(name);
		if (pn == null) {
			pn = PearlNetwork.readPlayerPearlNetwork(getServer(), pl, playerData);
			pearlNetwork.put(name, pn);
		}
		return pn;
	}
	
	// mark the given block
	public void playerMarkBlock(Player pl, Block block)
	{
		PlayerPearlNetwork pn = getPN(pl);
		
		if (!MarkerMetaData.isMarkable(block.getType())) {
			sendMessageTo(pl, "Cannot mark this spot...");
			return;
		}
		
		// small smoke effect on selected block
		for (int i = 0; i < 5; i++) {
			double x = 0.2 + Math.random() * 0.6;
			double z = 0.2 + Math.random() * 0.6;
			block.getWorld().playEffect(block.getRelative(BlockFace.UP).getLocation().add(new Vector(x, 0, z)), Effect.SMOKE, 4);
		}
		
		pn.setMarkerLocation(block);
		sendMessageTo(pl, "Marked " + getBlockName(block));
	}
	
	// start teleport sequence, returns whether successful
	public boolean playerInitTeleportTo(Player pl, Block hitBlock)
	{
		Block toBlock = playerCheckTeleportPossible(pl, hitBlock);
		if (toBlock == null) {
			return false;
		}
		
		// in case we want the fancy player twister sequence
		if (Settings.spinPlayerOnTeleport) {
			
			if (twisterTasks.containsKey(pl)) {
				sendMessageTo(pl, "You are already teleporting!");
				return false;			
			}
			
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("plugin", this);
			params.put("player", pl);
			params.put("hitblock", hitBlock);
			params.put("twister", new PlayerTwister(pl));
			
			int task = getServer().getScheduler().scheduleSyncRepeatingTask(this, new ParamRunnable(params) {
				public void run()
				{
					EnderPearlRevamp plugin = (EnderPearlRevamp) getParam("plugin");
					Player pl = (Player) getParam("player");
					Block hitBlock = (Block) getParam("hitblock");
					PlayerTwister twister = (PlayerTwister) getParam("twister");
					
					if (pl.isDead() || !pl.isOnline()) {
						plugin.playerStopTwister(pl);
						return;
					}
					
					boolean terminate = twister.update();
					if (terminate) {
						playerStopTwister(pl);
						// have to check again cuz shit could've happened between the start of the teleport sequence and now
						Block toBlock = playerCheckTeleportPossible(pl, hitBlock);
						if (toBlock != null) {
							playerTeleport(pl, toBlock);
						}
					}
				}
			}, 1L,  1L);
			
			twisterTasks.put(pl, task);
			
		}
		else {
			playerTeleport(pl, toBlock);
		}
		
		sendMessageTo(pl, "Teleporting to " + getBlockName(toBlock) + "...");
		return true;
	}
	
	// teleportation shenanigans
	public void playerTeleport(Player pl, Block toBlock)
	{
		Location telLoc = toBlock.getRelative(BlockFace.UP).getLocation();
		telLoc.setPitch(pl.getLocation().getPitch());
		telLoc.setYaw(pl.getLocation().getYaw());
		telLoc.add(new Vector(0.5, 0, 0.5)); // adjust to center of block
		
		//pl.getWorld().playEffect(pl.getLocation(), Effect.SMOKE, 4);
		teleportEffect(pl.getLocation());
		pl.teleport(telLoc);
		
		// if you're so far away that it's likely you don't have the chunk loaded, delay the effect by a second so it'll show up when you arrive
		// TODO: doesn't seem to work yet? :(
		if (pl.getLocation().distanceSquared(telLoc) > getServer().getViewDistance() * 16) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("loc", telLoc);
			
			getServer().getScheduler().scheduleSyncDelayedTask(this, new ParamRunnable(params)
			{
				public void run()
				{
					Location telLoc = (Location) getParam("loc");
					teleportEffect(telLoc);
				}
				
			}, 10L); // 1/2th of second, if server's not delayed
		}
		else {
			teleportEffect(telLoc);
		}
		
		if (Settings.teleportPlayerDamageFraction > 0) {
			double damage = pl.getMaxHealth() * Settings.teleportPlayerDamageFraction;
			pl.damage((int) Math.ceil(damage));
		}
		
		if (Settings.removeMarkAfterTeleport) {
			PlayerPearlNetwork pn = getPN(pl);
			pn.removeMarkerLocation(new BlockMarker(toBlock));
			sendMessageTo(pl, "Your " + getBlockName(toBlock) + " mark has faded...");
		}
	}
	
	// check if player has the option to teleport to a marked block of the same type as the hit block
	public Block playerCheckTeleportPossible(Player pl, Block hitBlock)
	{
		PlayerPearlNetwork pn = getPN(pl);
		
		if (!MarkerMetaData.isMarkable(hitBlock.getType())) {
			sendMessageTo(pl, "Ender Pearl did not hit a solid block...");
			return null;
		}
		
		Location loc = pn.getMarkerLocation(new BlockMarker(hitBlock));
		if (loc == null) {
			sendMessageTo(pl, "No marked spot for " + getBlockName(hitBlock));
			return null;
		}
		
		Block desBlock = loc.getBlock();
		if (!MarkerMetaData.isSameMarkerBlock(hitBlock, desBlock)) {
			sendMessageTo(pl, "Your marked " + getBlockName(hitBlock) + " was destroyed!" );
			return null;
		}
		
		boolean free = true;
		
		// Check if free (if not at top of world and at least two spaces of non-solid blocks)
		Block chBlock = desBlock.getRelative(BlockFace.UP);
		if (chBlock == null) {
			free = false;
		}
		else if (Settings.teleportRequireFreeSpot) {
			if (chBlock.getType().isSolid()) { free = false; }
			else {
				chBlock = chBlock.getRelative(BlockFace.UP);
				free = (chBlock != null && !chBlock.getType().isSolid());
			}
		}
		
		if (!free) {
			sendMessageTo(pl, "The marked " + getBlockName(desBlock) + " location does not have space!");
			return null;
		}
		
		return desBlock;
	}
	
	// stop the puke inducing twister
	public void playerStopTwister(Player pl)
	{
		Integer task = twisterTasks.get(pl);
		if (task != null) {
			getServer().getScheduler().cancelTask(task);
			twisterTasks.remove(pl);
		}
	}
	
	public boolean isPlayerSpinning(Player pl)
	{
		return twisterTasks.get(pl) != null;
	}
	
	// picks a few random items from a player's inventory and launches it away from him
	public void playerDropRandomItems(Player pl)
	{
		ItemStack[] contents = pl.getInventory().getContents();
		
		// fetch all occupied ids
		ArrayList<Integer> validIds = new ArrayList<Integer>();
		int i = 0;
		for(ItemStack stack : contents) {
			if (stack != null && stack.getAmount() > 0) {
				validIds.add(i);
			}
			i++;
		}
		
		if (validIds.size() == 0) { return; } // this guy is broke
		
		// pick random stack from inventory
		Random rand = new Random();
		
		int numDrops = 1 + rand.nextInt(3); // drop between 1 to 3 items
		
		for (int j = 0; j < numDrops; j++)
		{
			int num = rand.nextInt(validIds.size());
			ItemStack chosen = contents[validIds.get(num)].clone();
			if (chosen.getAmount() == 0) { continue; }
			
			chosen.setAmount(1);
			
			pl.getInventory().removeItem(chosen);
			Item item = pl.getWorld().dropItemNaturally(pl.getLocation(), chosen);
			//print("Player dropped " + chosen.getType().toString());
			
			// Randomize drop item velocity a bit
			Vector curVel = item.getVelocity();
			Vector addVel = new Vector(curVel.getX()*(1+rand.nextDouble()),0.4 + 0.2*rand.nextDouble(),curVel.getZ()*(1+rand.nextDouble()));
			item.setVelocity(curVel.add(addVel));
		}
	}
	
	// fireworks!
	public void teleportEffect(Location loc)
	{
		if (craftBukkitUpToDate) {
			Firework fw = loc.getWorld().spawn(loc, Firework.class);
			FireworkMeta fwm = fw.getFireworkMeta();
			FireworkEffect effect = FireworkEffect.builder().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build();
			fwm.addEffects(effect);
			fwm.setPower(0);
			fw.setFireworkMeta(fwm);
			
			// Firework effect
			((CraftWorld) loc.getWorld()).getHandle().broadcastEntityEffect(
	                ((CraftFirework) fw).getHandle(), (byte)17);
			
			fw.remove();
		}
	}
	
	// drop an enderpearl on the given location
	public void dropPearl(Location loc)
	{
		ItemStack pearl = new ItemStack(Material.ENDER_PEARL, 1);
		loc.getWorld().dropItemNaturally(loc, pearl);
	}
	
	//// Helpers ////
	
	private String getBlockName(Block bl)
	{
		ItemStack stack = new ItemStack(bl.getType(), 1, (short)0);
		stack.setData(new MaterialData(bl.getType(), bl.getData()));
		return stack.hasItemMeta() ? stack.getItemMeta().getDisplayName() : 
			MarkerMetaData.getMetaDataPrefix(bl.getType(), bl.getData()) + stack.getType().name().replace("_", " ").toLowerCase();
	}
	
	//// Config stuff ////
	
	private void loadConfig()
	{
		config = this.getConfig();
		if (config == null) { 
			this.saveDefaultConfig(); 
			config = this.getConfig();
		}
		
		config.options().copyDefaults(true);
		this.saveConfig();
	}
	
	private void loadPlayerData()
	{
	    if (playerDataFile == null) {
	    	playerDataFile = new File(getDataFolder(), "playerData.yml");
	    }
	    playerData = YamlConfiguration.loadConfiguration(playerDataFile);
	}
	
	public void savePlayerData() {
	    if (playerData == null || playerDataFile == null) {
	    	return;
	    }
	    
	    PearlNetwork.storePearlNetwork(pearlNetwork, playerData);
	    
	    try {
	        playerData.save(playerDataFile);
	    } catch (IOException ex) {
	        scream("Could not save player data!", ex);
	    }
	}
	
	//// Error checking ////
	
	public boolean checkClass(String path)
	{
		try {
			Class.forName( path );
			return true;
		} catch( ClassNotFoundException e ) {
			scream(ChatColor.RED + "Class " + path + " does not exist! Plugin needs to be compiled with the latest CraftBukkit");
			return false;
		}
	}
	
	//// Messaging and print stuff ////
	
	public void sendMessageTo(Player pl, String msg)
	{
		if (pl == null) {
			print(msg);
		}
		else {
			pl.sendMessage(chatTag + ChatColor.GRAY + msg);
		}
	}
	
	public void print(String msg)
	{
		log.info(consoleTag + msg);
	}
	
	public void scream(String msg)
	{
		log.log(Level.SEVERE, consoleTag + msg);
	}
	
	public void scream(String msg, Throwable thrown)
	{
		log.log(Level.SEVERE, consoleTag + msg, thrown);
	}
}
