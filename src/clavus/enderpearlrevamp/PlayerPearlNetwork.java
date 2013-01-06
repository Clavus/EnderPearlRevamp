package clavus.enderpearlrevamp;

import java.util.HashMap;

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
	
	public Location getMarkerLocation(BlockMarker marker)
	{
		return locs.get(marker);
	}
	
	public void removeMarkerLocation(BlockMarker marker)
	{
		locs.remove(marker);
	}
	
}
