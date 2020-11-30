package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinSettings;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinUtil;

/**
 * This class represents the hourly timer task
 * It is generally set to run once per hour
 * This rate can be configured.
 *
 * @author Goosius
 */
public class HourlyTimerTask extends TownyTimerTask {

	public HourlyTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		if (TownRuinSettings.getTownRuinsEnabled()) {
			TownRuinUtil.evaluateRuinedTownRemovals();
		}
	}
}