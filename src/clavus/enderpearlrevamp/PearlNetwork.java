package clavus.enderpearlrevamp;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PearlNetwork
{
	public static PlayerPearlNetwork readPlayerPearlNetwork(Server server, Player pl, FileConfiguration pearlData)
	{
		PlayerPearlNetwork pn = new PlayerPearlNetwork();
		String name = pl.getDisplayName().toLowerCase();
		String basePath = "players." + name;
		
		if (pearlData.contains(basePath)) {
			ConfigurationSection section = pearlData.getConfigurationSection(basePath);
			for (String key : section.getKeys(false)) {
				int matId = section.getInt(key + ".mat");
				byte meta = (byte) section.getInt(key + ".meta");
				World world = server.getWorld(section.getString(key + ".world"));
				if (world == null) { continue; } // silently fail
				
				Vector vec = section.getVector(key + ".loc");
				Location loc = new Location(world, vec.getX(), vec.getY(), vec.getZ());
				pn.setMarkerLocation(loc, matId, meta);
			}
		}
		
		return pn;
	}
	
	public static void storePearlNetwork(HashMap<String, PlayerPearlNetwork> data, FileConfiguration pearlData)
	{ 
		for(String plName : data.keySet()) {
			PlayerPearlNetwork pn = data.get(plName);
			String basePath = "players." + plName;
			
			pearlData.set(basePath, null); // clear this player's section
			ConfigurationSection section = pearlData.createSection(basePath);
			
			int i = 0;
			for (BlockMarker marker : pn.getMarkers()) {
				Location loc = pn.getMarkerLocation(marker);
				section.set(i + ".mat", marker.getMaterialId());
				section.set(i + ".meta", (int)marker.getMetaData());
				section.set(i + ".world", loc.getWorld().getName());
				section.set(i + ".loc", loc.toVector());
				i++;
			}
			
		}
	}
	
}
