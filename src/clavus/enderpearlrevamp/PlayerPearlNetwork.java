package clavus.enderpearlrevamp;

import java.util.HashMap;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class PlayerPearlNetwork
{
	private HashMap<BlockMarker, Location> locs = new HashMap<BlockMarker, Location>();
	
	public void setMarkerLocation(Block block)
	{
		BlockMarker marker = new BlockMarker(block);
		locs.put(marker, block.getLocation());	
	}
	
	public void setMarkerLocation(Location loc, int matId, byte meta)
	{
		BlockMarker marker = new BlockMarker(loc.getWorld(), matId, meta);
		locs.put(marker, loc);
	}	
	
	public Location getMarkerLocation(BlockMarker marker)
	{
		return locs.get(marker);
	}
	
	public void removeMarkerLocation(BlockMarker marker)
	{
		locs.remove(marker);
	}
	
	public Set<BlockMarker> getMarkers()
	{
		return locs.keySet();
	}
	
}
