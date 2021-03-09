package com.palmergames.bukkit.towny.war.eventwar;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.scheduler.BukkitScheduler;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.eventwar.tasks.WarTimerTask;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.ServerBroadCastTimerTask;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;

public class WarTaskManager {
	
	private War war;
	private List<Integer> warTaskIds = new ArrayList<>();
	
	public WarTaskManager(War war) {
		this.war = war;
	}

	/*
	 * Task Related
	 */
	public List<Integer> getTaskIds() {

		return new ArrayList<>(warTaskIds);
	}
	
	public void addTaskId(int id) {

		warTaskIds.add(id);
	}

	public void clearTaskIds() {

		warTaskIds.clear();
	}

	public void cancelTasks(BukkitScheduler scheduler) {

		for (Integer id : getTaskIds())
			scheduler.cancelTask(id);
		clearTaskIds();
	}
	
	/**
	 * When Townblocks have HP the WarTimerTask will make the
	 * healing and damaging of plots possible.
	 * 
	 * @param plugin Towny instance.
	 */
	public void scheduleWarTimerTask(Towny plugin) {

		int id = BukkitTools.scheduleAsyncRepeatingTask(new WarTimerTask(plugin, war), 0, TimeTools.convertToTicks(5));
		if (id == -1) {
			TownyMessaging.sendErrorMsg("Could not schedule war event loop.");
			war.end(false);
		} else {
			addTaskId(id);
		}
	}
	
	/**
	 * Creates a delay before war begins
	 * @param delay - Delay before war begins
	 */
	public void setupDelay(int delay) {

		if (delay <= 0)
			war.start();
		else {
			// Create a countdown timer
			for (Long t : TimeMgmt.getCountdownDelays(delay, TimeMgmt.defaultCountdownDelays)) {
				// TODO: Add the war name to the message since it spams all players online and/or don't make it spam all online players and only the ones in the war.
				int id = BukkitTools.scheduleAsyncDelayedTask(new ServerBroadCastTimerTask(war.getPlugin(), Translation.of("default_towny_prefix") + " " + Colors.Red + Translation.of("war_starts_in_x", TimeMgmt.formatCountdownTime(t))), TimeTools.convertToTicks((delay - t)));
				if (id == -1) {
					TownyMessaging.sendErrorMsg("Could not schedule a countdown message for war event.");
					war.end(false);
				} else
					addTaskId(id);
			}
			// Schedule set up delay
			int id = BukkitTools.scheduleAsyncDelayedTask(new Runnable() {
				
				@Override
				public void run() {
					war.start();
					
				}
			}, TimeTools.convertToTicks(delay));
			if (id == -1) {
				TownyMessaging.sendErrorMsg("Could not schedule setup delay for war event.");
				war.end(false);
			} else {
				addTaskId(id);
			}
		}
	}


}
