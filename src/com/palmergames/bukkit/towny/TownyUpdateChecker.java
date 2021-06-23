package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.tasks.UpdateCheckerTask;
import com.palmergames.bukkit.util.Version;

public class TownyUpdateChecker {
	private static boolean update = false;
	private static boolean checkedSuccessfully = false;
	private static Version newVersion;
	
	public static void checkForUpdates(Towny towny) {
		new UpdateCheckerTask(towny).start();
	}

	public static void setUpdate(boolean update) {
		TownyUpdateChecker.update = update;
	}

	public static boolean hasUpdate() {
		return update;
	}

	public static void setNewVersion(Version newVersion) {
		TownyUpdateChecker.newVersion = newVersion;
	}

	public static Version getNewVersion() {
		return newVersion;
	}
	
	public static String getUpdateURL() {
		if (newVersion == null)
			return "";
		
		return "https://github.com/TownyAdvanced/Towny/releases/tag/" + newVersion;
	}

	public static void setCheckedSuccessfully(boolean checkedSuccessfully) {
		TownyUpdateChecker.checkedSuccessfully = checkedSuccessfully;
	}

	public static boolean hasCheckedSuccessfully() {
		return checkedSuccessfully;
	}
}
