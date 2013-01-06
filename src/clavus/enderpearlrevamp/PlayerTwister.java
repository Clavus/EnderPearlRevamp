package clavus.enderpearlrevamp;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerTwister
{
	private Player pl;
	private EnderPearlRevamp plugin;
	
	private float yawAdd = 0f;
	private float yawSpeed = 0f;
	private float yawSpeedIncreasePerSec = 180f;
	private float yawSpeedMaxPerSec = 360f;
	
	private Location startLoc;
	private long terminateTime = 0;
	private long lastUpdate = 0;
	private long nextItemDrop = 0;
	
	public PlayerTwister(EnderPearlRevamp plugin, Player pl)
	{
		this.plugin = plugin;
		this.pl = pl;
		startLoc = pl.getLocation();
		nextItemDrop = System.currentTimeMillis() + 100;
		terminateTime = System.currentTimeMillis() + (long) (Settings.teleportDelay * 1000);
	}
	
	// returns true when it terminates
	public boolean update()
	{
		if (lastUpdate == 0) {
			lastUpdate = System.currentTimeMillis();
		}
		
		long timeDiff = System.currentTimeMillis() - lastUpdate;
		lastUpdate = System.currentTimeMillis();
		
		Location newLoc = pl.getLocation();
		float baseYaw = startLoc.getYaw();
		
		yawSpeed = Math.min(yawSpeedMaxPerSec * (timeDiff/1000f), yawSpeed + yawSpeedIncreasePerSec * (timeDiff/1000f));
		yawAdd = (yawAdd + yawSpeed) % 360;
		//plugin.print("Yawspeed: " + yawSpeed + ", yawadd: " + yawAdd + ", timediff: " + (timeDiff/1000f));
		
		newLoc.setYaw(baseYaw + yawAdd);
		pl.teleport(newLoc);
		
		if (Settings.dropShitWhileSpinning && nextItemDrop <= System.currentTimeMillis()) {
			double chance = Math.random();
			if (chance < Settings.dropChancePer10thSecond) {
				plugin.playerDropRandomItem(pl);
			}
			nextItemDrop = System.currentTimeMillis() + 100;
		}
		
		if (System.currentTimeMillis() > terminateTime) {
			return true;
		}
		else {
			return false;
		}
	}
	
}
