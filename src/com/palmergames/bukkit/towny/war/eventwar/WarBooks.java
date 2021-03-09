package com.palmergames.bukkit.towny.war.eventwar;

import java.text.SimpleDateFormat;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class WarBooks {

	private static String newline = "\n";
	private static final SimpleDateFormat warDateFormat = new SimpleDateFormat("MMM d yyyy '@' HH:mm");

	/**
	 * Creates the first book given to players in the war.
	 * 
	 * @param war War instance.
	 * @return String containing the raw text of what will become a book.
	 */
	public static String warStartBook(War war) {
		WarType warType = war.getWarType();
		/*
		 * Flashy Header.
		 */
		String text = "oOo War Declared! oOo" + newline;
		text += "-" + warDateFormat.format(System.currentTimeMillis()) + "-" + newline;
		text += "-------------------" + newline;
		
		/*
		 * Add who is involved.
		 */
		switch(warType) {
			case WORLDWAR:
				
				text += "War has broken out across all enemied nations!" + newline;
				text += newline;
				text += "The following nations have joined the battle: " + newline;
				for (Nation nation : war.getWarParticipants().getNations())
					text+= "* " + nation.getName() + newline;
				text += newline;
				text += "May the victors bring glory to their nation!";			
				break;
				
			case NATIONWAR:
				
				text += "War has broken out between two nations:" + newline;
				for (Nation nation : war.getWarParticipants().getNations())
					text+= "* " + nation.getName() + newline;
				text += newline;
				text += "May the victor bring glory to their nation!";
				break;
				
			case CIVILWAR:
				
				text += String.format("Civil war has broken out in the nation of %s!",war.getWarParticipants().getNations().get(0).getName()) + newline ;
				text += newline;
				text += "The following towns have joined the battle: " + newline;
				for (Town town : war.getWarParticipants().getTowns())
					text+= "* " + town.getName() + newline;
				text += newline;
				text += "May the victor bring peace to their nation!";
				break;
				
			case TOWNWAR:
				
				text += "War has broken out between two towns:";
				for (Town town : war.getWarParticipants().getTowns())
					text+= newline + "* " + town.getName();
				text += newline;
				text += "May the victor bring glory to their town!";
				break;
				
			case RIOT:
				
				text += String.format("A riot has broken out in the town of %s!", war.getWarParticipants().getTowns().get(0).getName()) + newline;
				text += newline;
				text += "The following residents have taken up arms: " + newline;
				for (Resident resident: war.getWarParticipants().getTowns().get(0).getResidents())
					text+= "* " + resident.getName() + newline;

				text += newline;
				text += "The last man standing will be the leader, but what will remain?!";
				break;
		}
		
		/*
		 * Add scoring types and winnings at stake.
		 */
		text += newline;
		text += "-------------------" + newline;
		text += "War Rules:" + newline;
		if (warType.hasTownBlockHP) {
			text += "Town blocks will have an HP stat. " + newline;
			text += "Regular Townblocks have an HP of " + TownySettings.getWarzoneTownBlockHealth() + ". ";
			text += "Homeblocks have an HP of " + TownySettings.getWarzoneHomeBlockHealth() + ". ";
			text += "Townblocks lose HP when enemies stand anywhere inside of the plot above Y level " + TownySettings.getMinWarHeight() + ". ";
			if (TownySettings.getPlotsHealableInWar())
				text += "Townblocks that have not dropped all the way to 0 hp are healable by town members and their allies. ";
			if (TownySettings.getOnlyAttackEdgesInWar())
				text += "Only edge plots will be attackable at first, so protect your borders and at all costs, do not let the enemy drop your homeblock to 0 hp! ";
			if (warType.hasTownBlocksSwitchTowns)
				text += "Townblocks which drop to 0 hp will be taken over by the attacker permanently! ";
			else
				text += "Townblocks which drop to 0 hp will not change ownership after the war. ";
			if (warType.hasTownConquering) {
				text += "Towns that have their homeblock drop to 0 hp will leave their nation and join the nation who conquered them. ";
				if (TownySettings.getWarEventConquerTime() > 0)
					text += "These towns will be conquered for " + TownySettings.getWarEventConquerTime() + " days. ";
			}
		}
		text += newline;
		if (warType.hasMonarchDeath && warType.lives > 0) {
			text += newline + "If your king or mayor runs out of lives your nation or town will be removed from the war! ";
		}
		if (warType.lives > 0)
			text += newline + "Everyone will start with " + warType.lives + (warType.lives == 1 ? " life.":" lives.") + " If you run out of lives and die again you will be removed from the war. ";
		else
			text += newline + "There are unlimited lives. ";
		text += newline;
		text += "WarSpoils up for grabs at the end of this war: " + TownyEconomyHandler.getFormattedBalance(war.warSpoilsAtStart);
		
		return text;
	}

}
