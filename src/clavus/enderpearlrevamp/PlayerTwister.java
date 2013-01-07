package clavus.enderpearlrevamp;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerTwister
{
	private Player pl;

	private float yawAdd = 0f;
	private float yawSpeed = 0f;
	private float yawSpeedIncreasePerSec = 180f;
	private float yawSpeedMaxPerSec = 360f;
	
	private Location startLoc;
	private long terminateTime = 0;
	private long lastUpdate = 0;
	
	public PlayerTwister(Player pl)
	{
		this.pl = pl;
		startLoc = pl.getLocation();
		terminateTime = System.currentTimeMillis() + (long) (Settings.teleportDelay * 1000);
	}
	
	// returns true when it terminates
	public boolean update()
	{
		if (pl == null) { return true; }
		
		if (lastUpdate == 0) {
			lastUpdate = System.currentTimeMillis();
		}
		
		long timeDiff = System.currentTimeMillis() - lastUpdate;
		lastUpdate = System.currentTimeMillis();
		
		Location newLoc = pl.getLocation();
		float baseYaw = startLoc.getYaw();
		
		yawSpeed = Math.min(yawSpeedMaxPerSec * (timeDiff/1000f), yawSpeed + yawSpeedIncreasePerSec * (timeDiff/1000f));
		yawAdd = (yawAdd + yawSpeed) % 360;
		
		newLoc.setYaw(baseYaw + yawAdd);
		pl.teleport(newLoc);

		if (System.currentTimeMillis() > terminateTime) {
			return true;
		}
		else {
			return false;
		}
	}
	
}
