package clavus.enderpearlrevamp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import clavus.enderpearlrevamp.runnable.ParamRunnable;

import com.massivecraft.factions.entity.BoardColls;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.mcore.ps.PS;

public class EnderPearlRevamp extends JavaPlugin
{
	private EnderPearlRevampListener epListener = new EnderPearlRevampListener(this);
	private static String chatTag = ChatColor.AQUA + "o ";
	private static String consoleTag = "[EPR] ";
	
	private FileConfiguration config;
	private FileConfiguration playerData;
	private File playerDataFile;
	
	private HashMap<String, PlayerPearlNetwork> pearlNetwork = new HashMap<String, PlayerPearlNetwork>();
	private HashMap<Player, Integer> twisterTasks = new HashMap<Player, Integer>();
	
	private boolean factionsEnabled;
	private int savingTask;
	private boolean playerDataChanged = false;
	private static Logger log;
	
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
		
		// set up every-5-minutes saving task
		long every5mins = 20L * 60L * 5L;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("plugin", this);
		savingTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new ParamRunnable(params)
		{
			public void run()
			{
				((EnderPearlRevamp) getParam("plugin")).savePlayerData();
			}
		}, every5mins, every5mins);
		
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(epListener, this);
		
		// attempt to load the factions plugin
		Plugin factions = pm.getPlugin("Factions");
		factionsEnabled = (factions != null);
				
		PluginDescriptionFile pdfFile = this.getDescription();
		print(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
		
		if (factionsEnabled) {
			print("Detected Factions " + factions.getDescription().getVersion() + "!");
		}
	}
	
	public void onDisable() 
	{
		getServer().getScheduler().cancelTask(savingTask);
		
		playerDataChanged = true; // force saving just to be sure
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
		
		// small smoke effect on selected block
		for (int i = 0; i < 5; i++) {
			double x = 0.2 + Math.random() * 0.6;
			double z = 0.2 + Math.random() * 0.6;
			block.getWorld().playEffect(block.getRelative(BlockFace.UP).getLocation().add(new Vector(x, 0, z)), Effect.SMOKE, 4);
		}
		
		pn.setMarkerLocation(block);
		sendMessageTo(pl, "Marked " + getBlockName(block));
		
		playerDataChanged = true;
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
		if (pl.getLocation().distance(telLoc) > getServer().getViewDistance() * 16) {
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
			
			playerDataChanged = true;
		}
	}
	
	// check if player has the option to teleport to a marked block of the same type as the hit block
	public Block playerCheckTeleportPossible(Player pl, Block hitBlock)
	{
		PlayerPearlNetwork pn = getPN(pl);
		
		if (!MarkerMetaData.isBlockMarkable(hitBlock.getType())) {
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
	
	public boolean isFactionsMarkable(Player pl, Block bl)
	{
		if (!factionsEnabled) { return true; }
		
		Faction f = BoardColls.get().getFactionAt(PS.valueOf(bl.getLocation()));
		
		if (f.getName().toLowerCase().equals("safezone") && !Settings.factionsAllowMarkingInSafezone) {
			sendMessageTo(pl, "Can't place marks in safezones!");
			return false; 
		}
		if (f.getName().toLowerCase().equals("warzone") && !Settings.factionsAllowMarkingInWarzone) {
			sendMessageTo(pl, "Can't place marks in warzones!");
			return false;
		}
		/*if (f.getRelationTo(plf) == Relation.ALLY && !Settings.factionsAllowMarkingInAllyLand) {
			sendMessageTo(pl, "Can't place marks in ally faction territory!");
			return false;
		}
		if (f.getRelationTo(plf) == Relation.NEUTRAL && !f.isNone() && !Settings.factionsAllowMarkingInNeutralLand) {
			sendMessageTo(pl, "Can't place marks in neutral faction territory!");
			return false;
		}
		if (f.getRelationTo(plf) == Relation.ENEMY && !Settings.factionsAllowMarkingInEnemyLand) {
			sendMessageTo(pl, "Can't place marks in enemy faction territory!");
			return false;
		}*/
		if (f.isNone() && !Settings.factionsAllowMarkingInWilderness) {
			sendMessageTo(pl, "Can't place marks in the wilderness!");
			return false;
		}
		
		return true;
	}
	
	// fireworks!
	public void teleportEffect(Location loc)
	{
		Firework fw = loc.getWorld().spawn(loc, Firework.class);
		FireworkMeta fwm = fw.getFireworkMeta();
		FireworkEffect effect = FireworkEffect.builder().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build();
		fwm.clearEffects();
		fwm.addEffects(effect);

		try {
			// Thank you ReplacedExplosions plugin for this piece of code :D
			Field f = fwm.getClass().getDeclaredField("power");
			f.setAccessible(true);
			f.set(fwm, Integer.valueOf(-2));
	    }
	    catch (Exception e) {
	    	e.printStackTrace();
	    }
		
		fw.setFireworkMeta(fwm);
	}
	
	// drop an enderpearl on the given location
	public void dropPearl(Location loc)
	{
		ItemStack pearl = new ItemStack(Material.ENDER_PEARL, 1);
		loc.getWorld().dropItemNaturally(loc, pearl);
	}
	
	//// Helpers ////
	
	public String getBlockName(Material mat, Byte meta)
	{
		ItemStack stack = new ItemStack(mat, 1, (short)0);
		if (meta == null) { stack.setData(new MaterialData(mat)); }
		else { stack.setData(new MaterialData(mat, meta)); }
		
		return stack.hasItemMeta() ? stack.getItemMeta().getDisplayName() : 
			MarkerMetaData.getMetaDataFormat(stack.getType().name().replace("_", " ").toLowerCase(), mat, meta);
	}
	
	public String getBlockName(Block bl)
	{
		return getBlockName(bl.getType(), bl.getData());
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
	    playerDataChanged = false;
	}
	
	public void savePlayerData() {
	    if (playerData == null || playerDataFile == null || !playerDataChanged) {
	    	return;
	    }
	    
	    PearlNetwork.storePearlNetwork(pearlNetwork, playerData);
	    
	    try {
	        playerData.save(playerDataFile);
	    } catch (IOException ex) {
	        scream("Could not save player data!", ex);
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
