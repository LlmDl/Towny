package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;

import net.citizensnpcs.api.CitizensAPI;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

//import org.bukkit.event.entity.EntityDamageEvent;

/**
 * @author Shade &amp; ElgarL
 *
 *         This class handles Player deaths and associated costs.
 *
 */
public class TownyEntityMonitorListener implements Listener {

	private final Towny plugin;

	public TownyEntityMonitorListener(Towny instance) {

		plugin = instance;
	}

	/**
	 * Handles players who have taken damage having their spawn cancelled.
	 * 
	 * @param event EntityDamageEvent.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTakesDamage(EntityDamageEvent event) {
		if (!TownySettings.isDamageCancellingSpawnWarmup() 
				|| !event.getEntityType().equals(EntityType.PLAYER) 
				|| !TownyTimerHandler.isTeleportWarmupRunning() 
				|| (plugin.isCitizens2() && CitizensAPI.getNPCRegistry().isNPC(event.getEntity())))
			return;

		Resident resident = null;
		try {
			resident = TownyUniverse.getInstance().getDataSource().getResident(event.getEntity().getName());
		} catch (NotRegisteredException e) {
		}
		if (resident != null && resident.getTeleportRequestTime() > 0) {
			TeleportWarmupTimerTask.abortTeleportRequest(resident);
			TownyMessaging.sendMsg(resident, ChatColor.RED + Translation.of("msg_err_teleport_cancelled_damage"));
		}
	}
	
	/**
	 * This handles EntityDeathEvents on MONITOR in order to handle Towny features such as:
	 * - DeathPayments,
	 * - Jailing Players,
	 * - Awarding WarTimeDeathPoints.
	 * @param event The event.
	 * @throws NotRegisteredException When a towny object is not found.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event) throws NotRegisteredException {
		Entity defenderEntity = event.getEntity();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		// Was this a player death?
		if (defenderEntity instanceof Player) {
			Player defenderPlayer = (Player) defenderEntity;
			Resident defenderResident;
			try {
				defenderResident = townyUniverse.getDataSource().getResident(defenderPlayer);
			} catch (NotRegisteredException e1) {
				// Usually an NPC or a Bot of some kind.
				return;
			}
			
			// Killed by another entity?			
			if (defenderEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {

				EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) defenderEntity.getLastDamageCause();

				Entity attackerEntity = damageEvent.getDamager();
				Player attackerPlayer = null;
				Resident attackerResident = null;

				if (attackerEntity instanceof Projectile) { // Killed by projectile, try to narrow the true source of the kill.
					Projectile projectile = (Projectile) attackerEntity;
					if (projectile.getShooter() instanceof Player) { // Player shot a projectile.
						attackerPlayer = (Player) projectile.getShooter();
						try {
							attackerResident = townyUniverse.getDataSource().getResident(attackerPlayer);
						} catch (NotRegisteredException e) {
							// Usually an NPC or a Bot of some kind.
							attackerPlayer = null;
						}
					} else { // Something else shot a projectile.
						try {
							attackerEntity = (Entity) projectile.getShooter(); // Mob shot a projectile.
						} catch (Exception e) { // This would be a dispenser kill, should count as environmental death.
						}
					}

				} else if (attackerEntity instanceof Player) {
					// This was a player kill
					attackerPlayer = (Player) attackerEntity;
					try {
						attackerResident = townyUniverse.getDataSource().getResident(attackerPlayer);
					} catch (NotRegisteredException e) {
						// Usually an NPC or a Bot of some kind.
						attackerPlayer = null;
					}
				}

				/*
				 * Player has died by a player: 
				 * - charge death payment,
				 * - check for jailing attacking residents,
				 * - award wartime death points.
				 */
				if (attackerPlayer != null) {
					deathPayment(attackerPlayer, defenderPlayer, attackerResident, defenderResident);			
					isJailingAttackers(attackerPlayer, defenderPlayer, attackerResident, defenderResident);
					if (TownyAPI.getInstance().isWarTime())
						wartimeDeathPoints(attackerPlayer, defenderPlayer, attackerResident, defenderResident);
					
				/*
				 * Player has died from an entity but not a player & death price is not PVP only.
				 */
				} else if (!TownySettings.isDeathPricePVPOnly() && TownySettings.isChargingDeath()) {
					deathPayment(defenderPlayer, defenderResident);
				}

			/*
			 * Player has died from non-entity cause, ie: Environmental.
			 */
			} else {
				if (!TownySettings.isDeathPricePVPOnly() && TownySettings.isChargingDeath()) {
					deathPayment(defenderPlayer, defenderResident);
				}
			}
		}

	}

	private void wartimeDeathPoints(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {

		if (attackerPlayer != null && defenderPlayer != null && TownyAPI.getInstance().isWarTime())
			try {
				if (CombatUtil.isAlly(attackerPlayer.getName(), defenderPlayer.getName()))
					return;

				if (attackerResident.hasTown() && War.isWarringTown(attackerResident.getTown()) && defenderResident.hasTown() && War.isWarringTown(defenderResident.getTown())){
					if (TownySettings.isRemovingOnMonarchDeath())
						monarchDeath(attackerPlayer, defenderPlayer, attackerResident, defenderResident);

					if (TownySettings.getWarPointsForKill() > 0){
						TownyUniverse.getInstance().getWarEvent().townScored(defenderResident.getTown(), attackerResident.getTown(), defenderPlayer, attackerPlayer, TownySettings.getWarPointsForKill());
					}
				}
			} catch (NotRegisteredException e) {
			}
	}

	private void monarchDeath(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {

		War warEvent = TownyUniverse.getInstance().getWarEvent();
		try {

			Nation defenderNation = defenderResident.getTown().getNation();
			Town defenderTown = defenderResident.getTown();
			if (warEvent.isWarringNation(defenderNation) && defenderResident.isKing()){
				TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeKingKilled(defenderNation));
				if (attackerResident != null)
					warEvent.remove(attackerResident.getTown(), defenderNation);
			}else if (warEvent.isWarringNation(defenderNation) && defenderResident.isMayor()) {
				TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeMayorKilled(defenderTown));
				if (attackerResident != null)
					warEvent.remove(attackerResident.getTown(), defenderResident.getTown());
			}
		} catch (NotRegisteredException e) {
		}
	}
	
	public void deathPayment(Player defenderPlayer, Resident defenderResident) throws NotRegisteredException {

		if (!TownySettings.isUsingEconomy())
			return;

		if (TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()) != null) {
			if (TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()).getType() == TownBlockType.ARENA || TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()).getType() == TownBlockType.JAIL)
				return;				
		}
		if (defenderResident.isJailed())
			return;

		double total = 0.0;

		try {
			if (TownySettings.getDeathPrice() > 0) {

				double price = TownySettings.getDeathPrice();

				if (!TownySettings.isDeathPriceType()) {
					price = defenderResident.getAccount().getHoldingBalance() * price;
					if (TownySettings.isDeathPricePercentageCapped())
						if (price > TownySettings.getDeathPricePercentageCap())
							price = TownySettings.getDeathPricePercentageCap();
				}

				if (!defenderResident.getAccount().canPayFromHoldings(price))
					price = defenderResident.getAccount().getHoldingBalance();

				if (!TownySettings.isEcoClosedEconomyEnabled())
					defenderResident.getAccount().payTo(price, new WarSpoils(), "Death Payment");
				else 
					defenderResident.getAccount().withdraw(price, "Death Payment");
				
				total = total + price;
				
				TownyMessaging.sendMsg(defenderPlayer, Translation.of("msg_you_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
			}
		} catch (EconomyException e) {
			TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_could_not_take_deathfunds"));
		}

		try {
			if (TownySettings.getDeathPriceTown() > 0) {

				double price = TownySettings.getDeathPriceTown();

				if (!TownySettings.isDeathPriceType()) {
					price = defenderResident.getTown().getAccount().getHoldingBalance() * price;
				}

				if (!defenderResident.getTown().getAccount().canPayFromHoldings(price))
					price = defenderResident.getTown().getAccount().getHoldingBalance();


				if (!TownySettings.isEcoClosedEconomyEnabled())
					defenderResident.getTown().getAccount().payTo(price, new WarSpoils(), "Death Payment Town");
				else 
					defenderResident.getTown().getAccount().withdraw(price, "Death Payment Town");

				total = total + price;

				TownyMessaging.sendTownMessagePrefixed(defenderResident.getTown(), Translation.of("msg_your_town_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
			}
		} catch (EconomyException e) {
			TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_couldnt_take_deathfunds"));
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_couldnt_take_town_deathfunds"));
		}

		try {
			if (TownySettings.getDeathPriceNation() > 0) {
				double price = TownySettings.getDeathPriceNation();

				if (!TownySettings.isDeathPriceType()) {
					price = defenderResident.getTown().getNation().getAccount().getHoldingBalance() * price;
				}

				if (!defenderResident.getTown().getNation().getAccount().canPayFromHoldings(price))
					price = defenderResident.getTown().getNation().getAccount().getHoldingBalance();

				if (!TownySettings.isEcoClosedEconomyEnabled())
					defenderResident.getTown().getNation().getAccount().payTo(price, new WarSpoils(), "Death Payment Nation");
				else 
					defenderResident.getTown().getNation().getAccount().withdraw(price, "Death Payment Nation");

				total = total + price;

				TownyMessaging.sendNationMessagePrefixed(defenderResident.getTown().getNation(), Translation.of("msg_your_nation_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
			}
		} catch (EconomyException e) {
			TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_couldnt_take_deathfunds"));
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_couldnt_take_nation_deathfunds"));
		}

	}

	public void deathPayment(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) throws NotRegisteredException {
		
		if (defenderPlayer != null)
			if (TownyUniverse.getInstance().getPermissionSource().testPermission(defenderPlayer, PermissionNodes.TOWNY_BYPASS_DEATH_COSTS.getNode()))
				return;

		if (attackerPlayer != null && TownyAPI.getInstance().isWarTime() && TownySettings.getWartimeDeathPrice() > 0 ) {
			try {
				if (attackerResident == null)
					throw new NotRegisteredException(String.format("The attackingResident %s has not been registered.", attackerPlayer.getName()));

				double price = TownySettings.getWartimeDeathPrice();
				double townPrice = 0;
				if (!defenderResident.getAccount().canPayFromHoldings(price)) {
					townPrice = price - defenderResident.getAccount().getHoldingBalance();
					price = defenderResident.getAccount().getHoldingBalance();
				}

				if (price > 0) {
					if (!TownySettings.isEcoClosedEconomyEnabled()){
						defenderResident.getAccount().payTo(price, attackerResident, "Death Payment (War)");
						TownyMessaging.sendMsg(attackerPlayer, Translation.of("msg_you_robbed_player", defenderResident.getName(), TownyEconomyHandler.getFormattedBalance(price)));
						TownyMessaging.sendMsg(defenderPlayer, Translation.of("msg_player_robbed_you", attackerResident.getName(), TownyEconomyHandler.getFormattedBalance(price)));
					} else {
						defenderResident.getAccount().withdraw(price, "Death Payment (War)");
						TownyMessaging.sendMsg(defenderPlayer, Translation.of("msg_you_lost_money", TownyEconomyHandler.getFormattedBalance(price)));
					}
				}

				// Resident doesn't have enough funds.
				if (townPrice > 0) {
					Town town = defenderResident.getTown();
					if (!town.getAccount().canPayFromHoldings(townPrice)) {
						// Town doesn't have enough funds.
						townPrice = town.getAccount().getHoldingBalance();
						try {
							TownyUniverse.getInstance().getWarEvent().remove(attackerResident.getTown(), town);
						} catch (NotRegisteredException e) {
							TownyUniverse.getInstance().getWarEvent().remove(town);
						}
					} else if (!TownySettings.isEcoClosedEconomyEnabled()){
						TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_couldnt_pay_player_town_bank_paying_instead", defenderResident.getName(), attackerResident.getName(), townPrice));
						town.getAccount().payTo(townPrice, attackerResident, String.format("Death Payment (War) (%s couldn't pay)", defenderResident.getName()));
					} else {
						TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_couldnt_pay_player_town_bank_paying_instead", defenderResident.getName(), attackerResident.getName(), townPrice));
						town.getAccount().withdraw(townPrice, String.format("Death Payment (War) (%s couldn't pay)", defenderResident.getName()));
					}
				}
			} catch (NotRegisteredException e) {
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(attackerPlayer, Translation.of("msg_err_wartime_could_not_take_deathfunds"));
				TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_wartime_could_not_take_deathfunds"));
			}			
		} else if (TownySettings.isChargingDeath() && attackerPlayer != null) {
			if (TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()) != null) {
				if (TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()).getType() == TownBlockType.ARENA || TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()).getType() == TownBlockType.JAIL)
					return;				
			}
			if (defenderResident.isJailed())
				return;

			double total = 0.0;

			try {
				if (TownySettings.getDeathPrice() > 0) {
					double price = TownySettings.getDeathPrice();

					if (!TownySettings.isDeathPriceType()) {
						price = defenderResident.getAccount().getHoldingBalance() * price;
						System.out.println("percentage death");
						if (TownySettings.isDeathPricePercentageCapped())
							if (price > TownySettings.getDeathPricePercentageCap())
								price = TownySettings.getDeathPricePercentageCap();
					}

					if (!defenderResident.getAccount().canPayFromHoldings(price))
						price = defenderResident.getAccount().getHoldingBalance();

					if (attackerResident == null) {
						if (!TownySettings.isEcoClosedEconomyEnabled())
							defenderResident.getAccount().payTo(price, new WarSpoils(), "Death Payment");
						else 
							defenderResident.getAccount().withdraw(price, "Death Payment");
					} else {
						if (!TownySettings.isEcoClosedEconomyEnabled())
							defenderResident.getAccount().payTo(price, attackerResident, "Death Payment");
						else 
							defenderResident.getAccount().withdraw(price, "Death Payment");
					}
					total = total + price;

					TownyMessaging.sendMsg(defenderPlayer, Translation.of("msg_you_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
				}
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_could_not_take_deathfunds"));
			}

			try {
				if (TownySettings.getDeathPriceTown() > 0) {

					double price = TownySettings.getDeathPriceTown();

					if (!TownySettings.isDeathPriceType()) {
						price = defenderResident.getTown().getAccount().getHoldingBalance() * price;
					}

					if (!defenderResident.getTown().getAccount().canPayFromHoldings(price))
						price = defenderResident.getTown().getAccount().getHoldingBalance();

					if (attackerResident == null) {
						if (!TownySettings.isEcoClosedEconomyEnabled())
							defenderResident.getTown().getAccount().payTo(price, new WarSpoils(), "Death Payment Town");
						else 
							defenderResident.getTown().getAccount().withdraw(price, "Death Payment Town");
					} else {
						if (!TownySettings.isEcoClosedEconomyEnabled())
							defenderResident.getTown().getAccount().payTo(price, attackerResident, "Death Payment Town");
						else 
							defenderResident.getTown().getAccount().withdraw(price, "Death Payment Town");
					}
					total = total + price;

					TownyMessaging.sendTownMessagePrefixed(defenderResident.getTown(), Translation.of("msg_your_town_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
				}
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_couldnt_take_deathfunds"));
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_couldnt_take_town_deathfunds"));
			}

			try {
				if (TownySettings.getDeathPriceNation() > 0) {
					double price = TownySettings.getDeathPriceNation();

					if (!TownySettings.isDeathPriceType()) {
						price = defenderResident.getTown().getNation().getAccount().getHoldingBalance() * price;
					}

					if (!defenderResident.getTown().getNation().getAccount().canPayFromHoldings(price))
						price = defenderResident.getTown().getNation().getAccount().getHoldingBalance();

					if (attackerResident == null) {
						if (!TownySettings.isEcoClosedEconomyEnabled())
							defenderResident.getTown().getNation().getAccount().payTo(price, new WarSpoils(), "Death Payment Nation");
						else 
							defenderResident.getTown().getNation().getAccount().withdraw(price, "Death Payment Nation");
					} else {
						if (!TownySettings.isEcoClosedEconomyEnabled())
							defenderResident.getTown().getNation().getAccount().payTo(price, attackerResident, "Death Payment Nation");
						else 
							defenderResident.getTown().getNation().getAccount().withdraw(price, "Death Payment Nation");
					}
					total = total + price;

					TownyMessaging.sendNationMessagePrefixed(defenderResident.getTown().getNation(), Translation.of("msg_your_nation_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
				}
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_couldnt_take_deathfunds"));
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, Translation.of("msg_err_couldnt_take_nation_deathfunds"));
			}

			if (attackerResident != null && !TownySettings.isEcoClosedEconomyEnabled()) {
				TownyMessaging.sendMsg(attackerResident, Translation.of("msg_you_gained_money_for_killing", TownyEconomyHandler.getFormattedBalance(total), defenderPlayer.getName()));

			}
		}
	}
	
	public void isJailingAttackers(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) throws NotRegisteredException {
		if (TownySettings.isJailingAttackingEnemies() || TownySettings.isJailingAttackingOutlaws()) {
			Location loc = defenderPlayer.getLocation();
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			if (!TownyAPI.getInstance().isTownyWorld(defenderPlayer.getLocation().getWorld()))
				return;
			if (TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()) == null)
				return;
			if (TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()).getType() == TownBlockType.ARENA)
				return;
			if (defenderResident.isJailed()) {
				if (TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation()).getType() != TownBlockType.JAIL) {
					TownyMessaging.sendGlobalMessage(Translation.of("msg_killed_attempting_to_escape_jail", defenderPlayer.getName()));
					return;
				}							
				return;			
			}
			if (!attackerResident.hasTown()) 
				return;

			// Try outlaw jailing first.
			if (TownySettings.isJailingAttackingOutlaws()) {
				Town attackerTown = null;
				try {					
					attackerTown = attackerResident.getTown();
				} catch (NotRegisteredException e1) {				
				}
				
				if (attackerTown.hasOutlaw(defenderResident)) {

					if (TownyAPI.getInstance().getTownBlock(loc) == null)
						return;

					try {
						if (TownyAPI.getInstance().getTownBlock(loc).getTown().getName() != attackerResident.getTown().getName())
							return;
					} catch (NotRegisteredException e1) {
						e1.printStackTrace();
					}

					if (!attackerTown.hasJailSpawn()) 
						return;

					if (!TownyAPI.getInstance().isWarTime()) {
						if (!townyUniverse.getPermissionSource().testPermission(attackerPlayer, PermissionNodes.TOWNY_OUTLAW_JAILER.getNode()))
							return;
						defenderResident.setJailed(1, attackerTown);
						return;
						
					} else {
						TownBlock jailBlock = null;
						Integer index = 1;
						for (Location jailSpawn : attackerTown.getAllJailSpawns()) {
							jailBlock = TownyAPI.getInstance().getTownBlock(jailSpawn);

							if (War.isWarZone(jailBlock.getWorldCoord())) {
								defenderResident.setJailed(index, attackerTown);
								try {
									TownyMessaging.sendTitleMessageToResident(defenderResident, "You have been jailed", "Run to the wilderness or wait for a jailbreak.");
								} catch (TownyException e) {
								}
								return;
							}
							index++;
							TownyMessaging.sendDebugMsg("A jail spawn was skipped because the plot has fallen in war.");
						}
						TownyMessaging.sendPrefixedTownMessage(attackerTown, TownySettings.getWarPlayerCannotBeJailedPlotFallenMsg());
						return;
					}
				}
			}
			
			// Try enemy jailing second
			Town town = null;
			try {					
				town = attackerResident.getTown();
			} catch (NotRegisteredException e1) {
				e1.printStackTrace();
			}			
		
			if (TownyAPI.getInstance().getTownBlock(loc) == null)
				return;
				
			try {
				if (TownyAPI.getInstance().getTownBlock(loc).getTown().getName() != attackerResident.getTown().getName())
					return;
			} catch (NotRegisteredException e1) {
				e1.printStackTrace();
			}
			if (!attackerResident.hasNation() || !defenderResident.hasNation()) 
				return;
			try {
				if (!attackerResident.getTown().getNation().getEnemies().contains(defenderResident.getTown().getNation())) 
					return;
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}								
			if (!town.hasJailSpawn()) 
				return;
			
			if (!TownyAPI.getInstance().isWarTime()) {
				defenderResident.setJailed(1, town);
			} else {
				TownBlock jailBlock = null;
				Integer index = 1;
				for (Location jailSpawn : town.getAllJailSpawns()) {
					jailBlock = TownyAPI.getInstance().getTownBlock(jailSpawn);
					if (jailBlock != null && War.isWarZone(jailBlock.getWorldCoord())) {
						defenderResident.setJailed(index, town);
						try {
							TownyMessaging.sendTitleMessageToResident(defenderResident, "You have been jailed", "Run to the wilderness or wait for a jailbreak.");
						} catch (TownyException e) {
						}
						return;
					}
					index++;
					TownyMessaging.sendDebugMsg("A jail spawn was skipped because the plot has fallen in war.");
				}
				TownyMessaging.sendPrefixedTownMessage(town, TownySettings.getWarPlayerCannotBeJailedPlotFallenMsg());
				return;
			}

		}
	}
}
