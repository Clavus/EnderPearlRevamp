package clavus.enderpearlrevamp;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderPearlRevamp extends JavaPlugin
{
	private EnderPearlRevampListener epListener = new EnderPearlRevampListener(this);
	private String chatTag = ChatColor.AQUA + "~ ";
	private String consoleTag = "[EPR] ";
	
	private FileConfiguration config;
	private FileConfiguration playerData;
	private File playerDataFile;
	
	public Logger log;
	
	public EnderPearlRevamp()
	{
		log = Logger.getLogger("Minecraft");
	}
	
	public void onEnable() 
	{
		getDataFolder().mkdirs();
		loadConfig();
		loadPlayerData();
		
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(epListener, this);
		
		PluginDescriptionFile pdfFile = this.getDescription();
		print(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
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
		
		if (cmd.getName().equalsIgnoreCase("erp")) {
			
			if (args.length <= 0) { 
				sendMessageTo(pl, cmd.getUsage());
				return true;
			}
			
			String action = args[0];
			
			if (action.equalsIgnoreCase("reload")) {
								
			}
		
		}
		
		return false;
	}
	
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
