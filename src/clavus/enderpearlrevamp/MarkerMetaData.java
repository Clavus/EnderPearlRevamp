package clavus.enderpearlrevamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class MarkerMetaData
{
	private static HashMap<Material, ArrayList<String>> allowedData;
	
	static {
		allowedData = new HashMap<Material, ArrayList<String>>();
		allowedData.put(Material.WOOL, new ArrayList<String>(
				Arrays.asList("white %m", "orange %m", "magenta %m", "light blue %m", "yellow %m", "lime %m", "pink %m", "gray %m", "light gray %m", "cyan %m",
						"purple %m", "blue %m", "brown %m", "green %m", "red %m", "black %m")));
		allowedData.put(Material.LOG, new ArrayList<String>(
				Arrays.asList("oak %m", "spruce %m", "birch %m", "jungle %m")));
		allowedData.put(Material.SANDSTONE, new ArrayList<String>(
				Arrays.asList("%m", "chiseled  %m", "smooth  %m")));
		allowedData.put(Material.SMOOTH_BRICK, new ArrayList<String>(
				Arrays.asList("%m", "mossy %m", "cracked %m", "chiseled %m")));
		allowedData.put(Material.WOOD, new ArrayList<String>(
				Arrays.asList("oak %m plank", "spruce %m plank", "birch %m plank", "jungle %m plank")));
		allowedData.put(Material.WOOD_STEP, new ArrayList<String>(
				Arrays.asList("oak %m", "spruce %m", "birch %m", "jungle %m")));
		allowedData.put(Material.WOOD_DOUBLE_STEP, new ArrayList<String>(
				Arrays.asList("oak %m", "spruce %m", "birch %m", "jungle %m")));
		allowedData.put(Material.STEP, new ArrayList<String>(
				Arrays.asList("stone %m", "sandstone %m", "wooden %m", "cobblestone %m", "brick %m", "stone brick %m", "nether brick %m")));
		allowedData.put(Material.DOUBLE_STEP, new ArrayList<String>(
				Arrays.asList("stone %m", "sandstone %m", "wooden %m", "cobblestone %m", "brick %m", "stone brick %m", "nether brick %m")));
	}
	
	public static byte getUsableMetaData(int matId, byte metaData)
	{
		return getUsableMetaData(Material.getMaterial(matId), metaData);
	}
	
	public static byte getUsableMetaData(Material mat, byte metaData)
	{	
		if (allowedData.containsKey(mat)) {
			if (mat == Material.LOG) {
				metaData = (byte) ((int) metaData % 4);
			}
			
			return metaData;
		}
		return 0;
	}
	
	public static String getMetaDataFormat(String name, int matId, byte metaData)
	{
		return getMetaDataFormat(name, Material.getMaterial(matId), metaData);
	}
	
	public static String getMetaDataFormat(String name, Material mat, byte metaData)
	{
		if (allowedData.containsKey(mat)) {
			metaData = getUsableMetaData(mat, metaData);
			String format = allowedData.get(mat).get(metaData);
			if (format == null || format == "") { return name; }
			else { return format.replace("%m", name); }
		}
		
		return name;
	}
	
	public static boolean isSameMarkerBlock(Block bl1, Block bl2)
	{
		return bl1.getType() == bl2.getType() && getUsableMetaData(bl1.getType(), bl1.getData()) == getUsableMetaData(bl2.getType(), bl2.getData());
	}
	
	public static boolean isBlockMarkable(Material mat)
	{
		return !Settings.notMarkable.contains(mat.toString()) && mat.isBlock() && mat.isSolid();
	}
	
}
