package clavus.enderpearlrevamp;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BlockMarker
{
	private World world;
	private int matId;
	private byte metaData;
	
	public BlockMarker(Block block)
	{
		this.world = block.getWorld();
		this.matId = block.getTypeId();
		this.metaData = MarkerMetaData.getUsableMetaData(block.getType(), block.getData());
	}
	
	public BlockMarker(World world, int matId, byte metaData)
	{
		this.world = world;
		this.matId = matId;
		this.metaData = MarkerMetaData.getUsableMetaData(Material.getMaterial(matId), metaData);
	}
	
	public World getWorld()
	{
		return world;
	}
	
	public int getMaterialId()
	{
		return matId;
	}
	
	public byte getMetaData()
	{
		return metaData;
	}
	
	public boolean equals(Object o)
	{
		return (o instanceof BlockMarker) && world.equals(((BlockMarker) o).getWorld()) &&
				((BlockMarker) o).getMaterialId() == matId && ((BlockMarker) o).getMetaData() == metaData;
	}
	
	public int hashCode()
	{
		return 17 * matId + 17 * 17 * metaData + 17 * 17 * 17 * world.getUID().hashCode();
	}
	
}
