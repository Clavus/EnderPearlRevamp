package clavus.enderpearlrevamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class MarkerMetaData
{
	private static HashMap<Material, ArrayList<String>> allowedData;
	private static ArrayList<Material> notMarkable = new ArrayList<Material>(Arrays.asList(Material.AIR, Material.PISTON_EXTENSION));
	
	static {
		allowedData = new HashMap<Material, ArrayList<String>>();
		allowedData.put(Material.WOOL, new ArrayList<String>(
				Arrays.asList("white", "orange", "magenta", "light blue", "yellow", "lime", "pink", "gray", "light gray", "cyan",
						"purple", "blue", "brown", "green", "red", "black")));
		allowedData.put(Material.LOG, new ArrayList<String>(
				Arrays.asList("oak", "spruce", "birch", "jungle")));
		allowedData.put(Material.SANDSTONE, new ArrayList<String>(
				Arrays.asList("", "chiseled", "smooth")));
		allowedData.put(Material.SMOOTH_BRICK, new ArrayList<String>(
				Arrays.asList("", "mossy", "cracked", "chiseled")));
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
	
	public static String getMetaDataPrefix(int matId, byte metaData)
	{
		return getMetaDataPrefix(Material.getMaterial(matId), metaData);
	}
	
	public static String getMetaDataPrefix(Material mat, byte metaData)
	{
		if (allowedData.containsKey(mat)) {
			metaData = getUsableMetaData(mat, metaData);
			String res = allowedData.get(mat).get(metaData);
			if (res == null || res == "") { return ""; }
			else { return res + " "; }
		}
		
		return "";
	}
	
	public static boolean isSameMarkerBlock(Block bl1, Block bl2)
	{
		return bl1.getWorld().equals(bl2.getWorld()) && bl1.getType() == bl2.getType() && 
				getUsableMetaData(bl1.getType(), bl1.getData()) == getUsableMetaData(bl2.getType(), bl2.getData());
	}
	
	public static boolean isMarkable(Material mat)
	{
		return !notMarkable.contains(mat) && mat.isBlock() && mat.isSolid();
	}
	
}
