package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

/**
 * @author ElgarL
 * 
 */
public class ResidentPurge extends Thread {

	Towny plugin;
	private CommandSender sender = null;
	long deleteTime;

	/**
	 * @param plugin reference to towny
	 */
	public ResidentPurge(Towny plugin, CommandSender sender, long deleteTime) {

		super();
		this.plugin = plugin;
		this.deleteTime = deleteTime;
		this.setPriority(NORM_PRIORITY);
	}

	@Override
	public void run() {

		int count = 0;

		message("Scanning for old residents...");
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		for (Resident resident : new ArrayList<>(townyUniverse.getDatabase().getResidents())) {
			if (!resident.isNPC() && (System.currentTimeMillis() - resident.getLastOnline() > (this.deleteTime)) && !BukkitTools.isOnline(resident.getName())) {
				count++;
				message("Deleting resident: " + resident.getName());
				townyUniverse.getDatabase().removeResident(resident);
				townyUniverse.getDatabase().removeResidentList(resident);
			}
		}

		message("Resident purge complete: " + count + " deleted.");

	}

	private void message(String msg) {

		if (this.sender != null)
			TownyMessaging.sendMessage(this.sender, msg);
		else
			TownyMessaging.sendMsg(msg);

	}
}
