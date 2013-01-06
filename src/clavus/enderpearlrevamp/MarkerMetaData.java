package clavus.enderpearlrevamp;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class MarkerMetaData
{
	private static ArrayList<Material> allowedData = new ArrayList<Material>(Arrays.asList(Material.WOOL, Material.LOG));
	private static ArrayList<String> woolPrefixes = new ArrayList<String>(
			Arrays.asList("white", "orange", "magenta", "light blue", "yellow", "lime", "pink", "gray", "light gray", "cyan",
					"purple", "blue", "brown", "green", "red", "black"));
	private static ArrayList<String> logPrefixes = new ArrayList<String>(
			Arrays.asList("oak", "spruce", "birch", "jungle"));
	
	private static ArrayList<Material> notMarkable = new ArrayList<Material>(Arrays.asList(Material.AIR));
	
	
	public static byte getUsableMetaData(int matId, byte metaData)
	{
		return getUsableMetaData(Material.getMaterial(matId), metaData);
	}
	
	public static byte getUsableMetaData(Material mat, byte metaData)
	{	
		if (allowedData.contains(mat)) {
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
		if (allowedData.contains(mat)) {
			metaData = getUsableMetaData(mat, metaData);
			if (mat == Material.WOOL) {
				return woolPrefixes.get(metaData) + " ";
			} 
			else if (mat == Material.LOG) {
				return logPrefixes.get(metaData) + " ";
			}
			return "";
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
