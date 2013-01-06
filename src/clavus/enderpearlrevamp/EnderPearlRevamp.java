package clavus.enderpearlrevamp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_4_6.CraftWorld;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftFirework;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class EnderPearlRevamp extends JavaPlugin
{
	private EnderPearlRevampListener epListener = new EnderPearlRevampListener(this);
	private String chatTag = ChatColor.AQUA + "o ";
	private String consoleTag = "[EPR] ";
	
	private FileConfiguration config;
	private FileConfiguration playerData;
	private File playerDataFile;
	
	private HashMap<Player, PlayerPearlNetwork> pearlNetwork = new HashMap<Player, PlayerPearlNetwork>();
	
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
	
	public PlayerPearlNetwork getPN(Player pl)
	{
		PlayerPearlNetwork pn = pearlNetwork.get(pl);
		if (pn == null) {
			pn = new PlayerPearlNetwork();
			pearlNetwork.put(pl, pn);
		}
		return pn;
	}
	
	public void playerMarkBlock(Player pl, Block block)
	{
		PlayerPearlNetwork pn = getPN(pl);
		
		if (!MarkerMetaData.isMarkable(block.getType())) {
			sendMessageTo(pl, "Cannot mark this spot...");
			return;
		}
		
		pn.setMarkerLocation(block);
		sendMessageTo(pl, "Marked block " + getBlockName(block));
	}
	
	public void playerInitTeleportTo(Player pl, Block block)
	{
		PlayerPearlNetwork pn = getPN(pl);
		
		if (!MarkerMetaData.isMarkable(block.getType())) {
			sendMessageTo(pl, "Ender Pearl did not hit a solid block...");
			return;
		}
		
		Location loc = pn.getMarkerLocation(new BlockMarker(block));
		if (loc == null) {
			sendMessageTo(pl, "No marked spot for " + getBlockName(block));
			return;
		}
		
		Block desBlock = loc.getBlock();
		if (!MarkerMetaData.isSameMarkerBlock(block, desBlock)) {
			sendMessageTo(pl, "Your marked " + getBlockName(block) + " was destroyed!" );
			return;
		}
		
		boolean free = true;
		
		// Check if free
		desBlock = desBlock.getRelative(BlockFace.UP);
		if (desBlock == null) {
			free = false;
		}
		else if (Settings.teleportRequireFreeSpot) {
			if (desBlock.getType().isSolid()) { free = false; }
			else {
				Block topBlock = desBlock.getRelative(BlockFace.UP);
				free = (topBlock != null && !topBlock.getType().isSolid());
			}
		}
		
		if (!free) {
			sendMessageTo(pl, "The marked " + getBlockName(block) + " location does not have space!");
			return;
		}
		
		Location telLoc = desBlock.getRelative(BlockFace.UP).getLocation();
		telLoc.setPitch(pl.getLocation().getPitch());
		telLoc.setYaw(pl.getLocation().getYaw());
		
		//pl.getWorld().playEffect(pl.getLocation(), Effect.SMOKE, 4);
		if (craftBukkitUpToDate) {
			Firework fw = pl.getWorld().spawn(pl.getLocation(), Firework.class);
			FireworkMeta fwm = fw.getFireworkMeta();
			FireworkEffect effect = FireworkEffect.builder().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build();
			fwm.addEffects(effect);
			fwm.setPower(0);
			fw.setFireworkMeta(fwm);
			
			// Firework effect
			((CraftWorld) pl.getWorld()).getHandle().broadcastEntityEffect(
	                ((CraftFirework) fw).getHandle(), (byte)17);
			
			fw.remove();
		}
		
		pl.teleport(telLoc.add(new Vector(0.5, 0, 0.5)));
		
		if (Settings.teleportPlayerDamageFraction > 0) {
			double damage = pl.getMaxHealth() * Settings.teleportPlayerDamageFraction;
			pl.damage((int) Math.ceil(damage));
		}
		
		sendMessageTo(pl, "Teleporting to " + getBlockName(block) + "...");
	}
	
	public void playerTeleport()
	{
		
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
			scream(ChatColor.RED + "Class " + path + " does not exist! Make sure plugin is compiled with server version of CraftBukkit");
			return false;
		}
	}
	
	//// Messaging and print stuff ////
	
	public void sendMessageTo(Player pl, String msg)
	{
		pl.sendMessage(chatTag + ChatColor.GRAY + msg);
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
