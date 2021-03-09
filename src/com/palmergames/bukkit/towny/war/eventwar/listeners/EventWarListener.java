package com.palmergames.bukkit.towny.war.eventwar.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.PreNewTownEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKilledPlayerEvent;
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreSetHomeBlockEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.eventwar.WarType;

public class EventWarListener implements Listener {

	public EventWarListener() {
		
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogin(PlayerLoginEvent event) {
		War war = TownyUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.getWarParticipants().addOnlineWarrior(event.getPlayer());
		war.getScoreManager().sendScores(event.getPlayer(), 3);
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		War war = TownyUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.getWarParticipants().removeOnlineWarrior(event.getPlayer());		
	}
	
	@EventHandler
	private void onPlayerKillsPlayer(PlayerKilledPlayerEvent event) {
		Resident killerRes = event.getKillerRes();
		Resident victimRes = event.getVictimRes();
		War war = TownyUniverse.getInstance().getWarEvent(event.getKiller());
		War victimWar = TownyUniverse.getInstance().getWarEvent(event.getVictim());

		if (war == null || victimWar == null)
			return; // One of the players is not in a war.
		if (war != victimWar)
			return; // The wars are not the same war.
		if (CombatUtil.isAlly(killerRes.getName(), victimRes.getName()) && war.getWarType() != WarType.RIOT)
			return; // They are allies and this was a friendly fire kill.
		
		/*
		 * Handle lives being lost, for wars without unlimited lives.
		 */
		if (war.getWarType().lives != -1){
			residentLosesALife(victimRes, killerRes, war, event);
		}
		
		/*
		 * Handle death payments. TODO: Move wartime death payments out of EntityMonitoryListener (after we figure out if we'd break flagwar.)
		 */
	}
	
	private void residentLosesALife(Resident victimRes, Resident killerRes, War war, PlayerKilledPlayerEvent event) {
	
		int victimLives = war.getWarParticipants().getLives(victimRes); // Use a variable for this because it will be lost once takeLife(victimRes) is called.

		/*
		 * Take a life off of the victim no matter what type of war it is.
		 */
		war.getWarParticipants().takeLife(victimRes);
		
		/*
		 * Someone is being removed from the war.
		 */
		if (victimLives == 0) {
			residentLostLastLife(victimRes, killerRes, war);
		}
	
		/*
		 * Give the killer some points. 
		 */
		if (war.getWarType().pointsPerKill > 0){
			war.getScoreManager().residentScoredKillPoints(victimRes, killerRes, event.getLocation());
		}
	}
	
	private void residentLostLastLife(Resident victimRes, Resident killerRes, War war) {

		/*
		 * Remove the resident from the war, handling kings and mayors if monarchdeath is enabled.
		 */
		switch (war.getWarType()) {
		
			case RIOT:
				try {
					TownyMessaging.sendPrefixedTownMessage(killerRes.getTown(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
				} catch (NotRegisteredException ignored) {}
				war.getWarParticipants().remove(victimRes);
				break;
			case NATIONWAR:
			case WORLDWAR:
				try {
					/*
					 * Look to see if the king's death would remove a nation from the war.
					 */
					if (war.getWarType().hasMonarchDeath && victimRes.hasNation() && victimRes.isKing()) {
						TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_KING_KILLED", victimRes.getTown().getNation().getName()));
						/*
						 * Remove the king's nation from the war. Where-in the king will be removed with the rest of the residents.
						 */
						war.getWarZoneManager().remove(victimRes.getTown().getNation(), killerRes.getTown());

					/*
					 * Look to see if the mayor's death would remove a town from the war.
					 */
					} else if (war.getWarType().hasMonarchDeath && victimRes.hasTown() && victimRes.isMayor()) {
						TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_MAYOR_KILLED", victimRes.getTown().getName()));
						/*
						 * Remove the mayor's town from the war. Where-in the mayor will be removed with the rest of the residents.
						 */
						war.getWarZoneManager().remove(victimRes.getTown(), killerRes.getTown());
						
					/*
					 * Handle regular resident removal when they've run out of lives.	
					 */
					} else {
						TownyMessaging.sendPrefixedTownMessage(victimRes.getTown(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
						TownyMessaging.sendPrefixedTownMessage(killerRes.getTown(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
						war.getWarParticipants().remove(victimRes);
					}
				} catch (NotRegisteredException ignored) {}
				break;
			case CIVILWAR:
			case TOWNWAR:
				try {
					/*
					 * Look to see if the mayor's death would remove a town from the war.
					 */
					if (war.getWarType().hasMonarchDeath && victimRes.hasTown() && victimRes.isMayor()) {
						TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_MAYOR_KILLED", victimRes.getTown().getName()));
						/*
						 * Remove the mayor's town from the war. Where-in the mayor will be removed with the rest of the residents.
						 */
						war.getWarZoneManager().remove(victimRes.getTown(), killerRes.getTown());

					/*
					 * Handle regular resident removal when they've run out of lives.	
					 */
					} else {
						TownyMessaging.sendPrefixedTownMessage(victimRes.getTown(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
						TownyMessaging.sendPrefixedTownMessage(killerRes.getTown(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
						war.getWarParticipants().remove(victimRes);
					}
				} catch (NotRegisteredException ignored) {}
				break;
		}
	}
	
	@EventHandler
	public void onTownMoveHomeblock(TownPreSetHomeBlockEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onNewTown(PreNewTownEvent event) { // TODO: Make this configurable based on whether there is a world-war or not.
		if (TownyAPI.getInstance().isWarTime()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onTownLeave(TownLeaveEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onTownClaim(TownPreClaimEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}

	@EventHandler
	public void onTownUnclaim(TownPreUnclaimCmdEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}

	@EventHandler
	public void onTownLeavesNation(NationPreTownLeaveEvent event) { // Also picks up towns being kicked using /n kick.
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
}

