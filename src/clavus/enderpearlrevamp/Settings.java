package clavus.enderpearlrevamp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

public class Settings
{
	public static double teleportDelay = 2.5f;
	public static boolean spinPlayerOnTeleport = true;
	public static double teleportPlayerDamageFraction = 0.25f;
	public static boolean dropItemsOnDamageWhileSpinning = true;
	public static boolean removeMarkAfterTeleport = true;
	public static boolean teleportRequireFreeSpot = false;
	public static ArrayList<String> notMarkable = new ArrayList<String>();
	
	public static boolean factionsAllowMarkingInWarzone = true;
	public static boolean factionsAllowMarkingInSafezone = true;
	//public static boolean factionsAllowMarkingInAllyLand = true;
	//public static boolean factionsAllowMarkingInNeutralLand = true;
	//public static boolean factionsAllowMarkingInEnemyLand = true;
	public static boolean factionsAllowMarkingInWilderness = true;
	
	public static void parseConfig(Logger log, FileConfiguration config)
	{
		
		Field[] declaredFields = Settings.class.getDeclaredFields();
		for (Field field : declaredFields) {
		    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
		    	try {
			    	if (field.getType().equals(boolean.class)) {
			    		field.set(null, config.getBoolean(field.getName()));
			    	}
			    	else if (field.getType().equals(double.class)) {
			    		field.set(null, config.getDouble(field.getName()));
			    	}
			    	else if (field.getType().equals(ArrayList.class)) {
			    		field.set(null, new ArrayList<String>(config.getStringList(field.getName())));
			    	}
		    	}
		    	catch(Exception e) { log.log(Level.SEVERE, "Error while parsing field!", e); }
		    }
		}
		
	}
	
}
