package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.permissions.TownyPerms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class DailyTimerTask extends TownyTimerTask {

	public DailyTimerTask(Towny plugin) {

		super(plugin);
	}

	@Override
	public void run() {

		long start = System.currentTimeMillis();

		TownyMessaging.sendDebugMsg("New Day");

		// Collect taxes
		if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily()) {
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day_tax")));
			try {
				TownyMessaging.sendDebugMsg("Collecting Town Taxes");
				collectTownTaxes();
				TownyMessaging.sendDebugMsg("Collecting Nation Taxes");
				collectNationTaxes();
				TownyMessaging.sendDebugMsg("Collecting Town Costs");
				collectTownCosts();
				TownyMessaging.sendDebugMsg("Collecting Nation Costs");
				collectNationCosts();
			} catch (EconomyException ignored) {
			} catch (TownyException e) {
				// TODO king exception
				e.printStackTrace();
			}
		} else
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day")));

		// Automatically delete old residents
		if (TownySettings.isDeletingOldResidents()) {
			// Run a purge in it's own thread
			new ResidentPurge(plugin, null, TownySettings.getDeleteTime() * 1000).start();
		}

		// Backups
		TownyMessaging.sendDebugMsg("Cleaning up old backups.");
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		townyUniverse.getDatabase().cleanupBackups();
		if (TownySettings.isBackingUpDaily())
			try {
				TownyMessaging.sendDebugMsg("Making backup.");
				townyUniverse.getDatabase().backup();
			} catch (IOException e) {
				TownyMessaging.sendErrorMsg("Could not create backup.");
				e.printStackTrace();
			}

		TownyMessaging.sendDebugMsg("Finished New Day Code");
		TownyMessaging.sendDebugMsg("Universe Stats:");
		TownyMessaging.sendDebugMsg("    Residents: " + townyUniverse.getDatabase().getResidents().size());
		TownyMessaging.sendDebugMsg("    Towns: " + townyUniverse.getDatabase().getTowns().size());
		TownyMessaging.sendDebugMsg("    Nations: " + townyUniverse.getDatabase().getNations().size());
		for (TownyWorld world : townyUniverse.getDatabase().getWorlds())
			TownyMessaging.sendDebugMsg("    " + world.getName() + " (townblocks): " + world.getTownBlocks().size());

		TownyMessaging.sendDebugMsg("Memory (Java Heap):");
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (max)", Runtime.getRuntime().maxMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (total)", Runtime.getRuntime().totalMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (free)", Runtime.getRuntime().freeMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (used=total-free)", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024));
		TownyMessaging.sendDebugMsg("newDay took " + (System.currentTimeMillis() - start) + "ms");
	}

	/**
	 * Collect taxes for all nations due from their member towns
	 * 
	 * @throws EconomyException - EconomyException
	 */
	public void collectNationTaxes() throws EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Nation> nations = new ArrayList<>(townyUniverse.getDatabase().getNations());
		ListIterator<Nation> nationItr = nations.listIterator();
		Nation nation;

		while (nationItr.hasNext()) {
			nation = nationItr.next();
			/*
			 * Only collect tax for this nation if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDatabase().hasNation(nation.getName()))
				collectNationTaxes(nation);
		}
	}

	/**
	 * Collect taxes due to the nation from it's member towns.
	 * 
	 * @param nation - Nation to collect taxes from.
	 * @throws EconomyException - EconomyException
	 */
	protected void collectNationTaxes(Nation nation) throws EconomyException {
		if (nation.getTaxes() > 0) {

			List<Town> towns = new ArrayList<>(nation.getTowns());
			ListIterator<Town> townItr = towns.listIterator();
			Town town;
			TownyUniverse townyUniverse = TownyUniverse.getInstance();

			while (townItr.hasNext()) {
				town = townItr.next();

				/*
				 * Only collect nation tax from this town if it really still
				 * exists.
				 * We are running in an Async thread so MUST verify all objects.
				 */
				if (townyUniverse.getDatabase().hasTown(town.getName())) {
					if (town.isCapital() || !town.hasUpkeep())
						continue;
					if (!town.payTo(nation.getTaxes(), nation, "Nation Tax")) {
						try {
							TownyMessaging.sendNationMessage(nation, TownySettings.getCouldntPayTaxesMsg(town, "nation"));
							nation.removeTown(town);
						} catch (EmptyNationException e) {
							// Always has 1 town (capital) so ignore
						} catch (NotRegisteredException ignored) {
						}
						townyUniverse.getDatabase().saveTown(town);
						townyUniverse.getDatabase().saveNation(nation);
					}
				} else
					TownyMessaging.sendTownMessage(town, TownySettings.getPayedTownTaxMsg() + nation.getTaxes());
			}
		}

	}

	/**
	 * Collect taxes for all towns due from their residents.
	 * 
	 * @throws EconomyException - EconomyException
	 */
	public void collectTownTaxes() throws EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Town> towns = new ArrayList<>(townyUniverse.getDatabase().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only collect resident tax for this town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDatabase().hasTown(town.getName()))
				collectTownTaxes(town);
		}
	}

	/**
	 * Collect taxes due to the town from it's residents.
	 * 
	 * @param town - Town to collect taxes from
	 * @throws EconomyException - EconomyException
	 */
	protected void collectTownTaxes(Town town) throws EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		// Resident Tax
		if (town.getTaxes() > 0) {

			List<Resident> residents = new ArrayList<>(town.getResidents());
			ListIterator<Resident> residentItr = residents.listIterator();
			Resident resident;

			while (residentItr.hasNext()) {
				resident = residentItr.next();

				/*
				 * Only collect resident tax from this resident if it really
				 * still exists. We are running in an Async thread so MUST
				 * verify all objects.
				 */
				if (townyUniverse.getDatabase().hasResident(resident.getName())) {

					if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt") || resident.isNPC()) {
						try {
							TownyMessaging.sendResidentMessage(resident, TownySettings.getTaxExemptMsg());
						} catch (TownyException e) {
							// Player is not online
						}
						continue;
					} else if (town.isTaxPercentage()) {
						double cost = resident.getHoldingBalance() * town.getTaxes() / 100;
						resident.payTo(cost, town, "Town Tax (Percentage)");
						/*
						 * Don't send individual message anymore to ease up on
						 * the lag. try {
						 * TownyMessaging.sendResidentMessage(resident,
						 * TownySettings.getPayedResidentTaxMsg() + cost); }
						 * catch (TownyException e) { // Player is not online }
						 */
					} else if (!resident.payTo(town.getTaxes(), town, "Town Tax")) {
						TownyMessaging.sendTownMessage(town, TownySettings.getCouldntPayTaxesMsg(resident, "town"));
						
						try {
							
							// reset this resident and remove him from the town.
							resident.clear();
							townyUniverse.getDatabase().saveTown(town);
							
						} catch (EmptyTownException e) {
							
							// No mayor so remove the town.
							townyUniverse.getDatabase().removeTown(town);
							
						}
						
						townyUniverse.getDatabase().saveResident(resident);
						
					}// else
					/*
					 * Don't send individual message anymore to ease up on the
					 * lag. try { TownyMessaging.sendResidentMessage(resident,
					 * TownySettings.getPayedResidentTaxMsg() +
					 * town.getTaxes()); } catch (TownyException e) { // Player
					 * is not online }
					 */
				}
			}
		}

		// Plot Tax
		if (town.getPlotTax() > 0 || town.getCommercialPlotTax() > 0 || town.getEmbassyPlotTax() > 0) {
			// Hashtable<Resident, Integer> townPlots = new Hashtable<Resident,
			// Integer>();
			// Hashtable<Resident, Double> townTaxes = new Hashtable<Resident,
			// Double>();

			List<TownBlock> townBlocks = new ArrayList<>(town.getTownBlocks());
			ListIterator<TownBlock> townBlockItr = townBlocks.listIterator();
			TownBlock townBlock;

			while (townBlockItr.hasNext()) {
				townBlock = townBlockItr.next();

				if (!townBlock.hasResident())
					continue;
				try {
					Resident resident = townBlock.getResident();

					/*
					 * Only collect plot tax from this resident if it really
					 * still exists. We are running in an Async thread so MUST
					 * verify all objects.
					 */
					if (townyUniverse.getDatabase().hasResident(resident.getName())) {
						if (resident.hasTown())
							if (resident.getTown() == townBlock.getTown())
								if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt") || resident.isNPC())
									continue;
							
						if (!resident.payTo(townBlock.getType().getTax(town), town, String.format("Plot Tax (%s)", townBlock.getType()))) {
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_couldnt_pay_plot_taxes"), resident));

							townBlock.setResident(null);
							townBlock.setPlotPrice(-1);

							// Set the plot permissions to mirror the towns.
							townBlock.setType(townBlock.getType());
							
							townyUniverse.getDatabase().saveResident(resident);
							townyUniverse.getDatabase().saveTownBlock(townBlock);
						}// else {
							// townPlots.put(resident,
							// (townPlots.containsKey(resident) ?
							// townPlots.get(resident) : 0) + 1);
							// townTaxes.put(resident,
							// (townTaxes.containsKey(resident) ?
							// townTaxes.get(resident) : 0) +
							// townBlock.getType().getTax(town));
							// }
					}
				} catch (NotRegisteredException ignored) {
				}
			}
			/*
			 * Don't send individual message anymore to ease up on the lag. for
			 * (Resident resident : townPlots.keySet()) { try { int numPlots =
			 * townPlots.get(resident); double totalCost =
			 * townTaxes.get(resident);
			 * TownyMessaging.sendResidentMessage(resident,
			 * String.format(TownySettings.getLangString("msg_payed_plot_cost"),
			 * totalCost, numPlots, town.getName())); } catch (TownyException e)
			 * { // Player is not online } }
			 */

		}
	}

	/**
	 * Collect or pay upkeep for all towns.
	 * 
	 * @throws EconomyException
	 * @throws TownyException
	 */
	public void collectTownCosts() throws EconomyException, TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Town> towns = new ArrayList<>(townyUniverse.getDatabase().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();

			/*
			 * Only charge/pay upkeep for this town if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDatabase().hasTown(town.getName())) {

				if (town.hasUpkeep()) {
					double upkeep = TownySettings.getTownUpkeepCost(town);
				
					if (upkeep > 0) {
						// Town is paying upkeep
						if (!town.pay(upkeep, "Town Upkeep")) {
							townyUniverse.getDatabase().removeTown(town);
							TownyMessaging.sendGlobalMessage(town.getName() + TownySettings.getLangString("msg_bankrupt_town"));
						}
					} else if (upkeep < 0) {
						// Negative upkeep
						if (TownySettings.isUpkeepPayingPlots()) {
							// Pay each plot owner a share of the negative
							// upkeep
							List<TownBlock> plots = new ArrayList<>(town.getTownBlocks());

							for (TownBlock townBlock : plots) {
								if (townBlock.hasResident())
									townBlock.getResident().pay((upkeep / plots.size()), "Negative Town Upkeep - Plot income");
								else
									town.pay((upkeep / plots.size()), "Negative Town Upkeep - Plot income");
							}

						} else {
							// Not paying plot owners so just pay the town
							town.pay(upkeep, "Negative Town Upkeep");
						}

					}
				}
			}
		}
	}

	/**
	 * Collect upkeep due from all nations.
	 * 
	 * @throws EconomyException
	 */
	public void collectNationCosts() throws EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Nation> nations = new ArrayList<>(townyUniverse.getDatabase().getNations());
		ListIterator<Nation> nationItr = nations.listIterator();
		Nation nation;

		while (nationItr.hasNext()) {
			nation = nationItr.next();

			/*
			 * Only charge upkeep for this nation if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDatabase().hasNation(nation.getName())) {

				double upkeep = TownySettings.getNationUpkeepCost(nation);

				if (upkeep > 0) {
					// Town is paying upkeep

					if (!nation.pay(TownySettings.getNationUpkeepCost(nation), "Nation Upkeep")) {
						townyUniverse.getDatabase().removeNation(nation);
						TownyMessaging.sendGlobalMessage(nation.getName() + TownySettings.getLangString("msg_bankrupt_nation"));
					}
					if (nation.isNeutral()) {
						if (!nation.pay(TownySettings.getNationNeutralityCost(), "Nation Peace Upkeep")) {
							try {
								nation.setNeutral(false);
							} catch (TownyException e) {
								e.printStackTrace();
							}
							townyUniverse.getDatabase().saveNation(nation);
							TownyMessaging.sendNationMessage(nation, TownySettings.getLangString("msg_nation_not_peaceful"));
						}
					}
					
				} else if (upkeep < 0) {
					nation.pay(upkeep, "Negative Nation Upkeep");
				}
			}
		}
	}
}
