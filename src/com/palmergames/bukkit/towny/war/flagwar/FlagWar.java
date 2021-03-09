package com.palmergames.bukkit.towny.war.flagwar;

import static org.bukkit.Bukkit.getServer;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.war.flagwar.events.CellAttackCanceledEvent;
import com.palmergames.bukkit.towny.war.flagwar.events.CellAttackEvent;
import com.palmergames.bukkit.towny.war.flagwar.events.CellDefendedEvent;
import com.palmergames.bukkit.towny.war.flagwar.events.CellWonEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.PluginManager;

public class FlagWar {

	private static Map<Cell, CellUnderAttack> cellsUnderAttack;
	private static Map<String, List<CellUnderAttack>> cellsUnderAttackByPlayer;
	private static Map<Town, Long> lastFlag;

	public static void onEnable() {

		cellsUnderAttack = new HashMap<>();
		cellsUnderAttackByPlayer = new HashMap<>();
		lastFlag = new HashMap<>();

		//Register Listeners
		registerEvents();
	}

	public static void onDisable() {

	
		try {
			for (CellUnderAttack cell : new ArrayList<>(cellsUnderAttack.values())) {
				attackCanceled(cell);
			}
		} catch (NullPointerException ignored) {
		}
	}

	public static void registerEvents() {
		@SuppressWarnings("unused")
		final PluginManager pluginManager = getServer().getPluginManager();
		// TODO: Register Listeners Post-Split
	}

	public static void registerAttack(CellUnderAttack cell) throws TownyException {

		CellUnderAttack currentData = cellsUnderAttack.get(cell);

		// Check if area is already under attack.
		if (currentData != null)
			throw new AlreadyRegisteredException(Translation.of("msg_err_enemy_war_cell_already_under_attack", currentData.getNameOfFlagOwner()));

		String playerName = cell.getNameOfFlagOwner();

		// Check that the user is under his limit of active warflags.
		int futureActiveFlagCount = getNumActiveFlags(playerName) + 1;
		if (futureActiveFlagCount > FlagWarConfig.getMaxActiveFlagsPerPerson())
			throw new TownyException(Translation.of("msg_err_enemy_war_reached_max_active_flags", FlagWarConfig.getMaxActiveFlagsPerPerson()));

		addFlagToPlayerCount(playerName, cell);
		cellsUnderAttack.put(cell, cell);
		cell.begin();
	}

	/**
	 * Get the number of flags actively owned by the specified player.
	 * 
	 * @param playerName the name of the player
	 * @return the number of flags active
	 */
	public static int getNumActiveFlags(String playerName) {

		List<CellUnderAttack> activeFlags = cellsUnderAttackByPlayer.get(playerName);
		return activeFlags == null ? 0 : activeFlags.size();
	}

	/**
	 * Get all cells currently under attack
	 * 
	 * @return all the cells currently under attack
	 */
	public static List<CellUnderAttack> getCellsUnderAttack() {
		return new ArrayList<>(cellsUnderAttack.values());
	}

	/**
	 * Get all cells currently under attack in the specified town
	 * 
	 * @param town the town to get cells under attack
	 * @return the cells under attack
	 */
	public static List<CellUnderAttack> getCellsUnderAttack(Town town) {
		List<CellUnderAttack> cells = new ArrayList<>();
		for(CellUnderAttack cua : cellsUnderAttack.values()) {
			try {
				Town townUnderAttack = TownyAPI.getInstance().getTownBlock(cua.getFlagBaseBlock().getLocation()).getTown();
				if (townUnderAttack == null) {
					continue;
				}
				if(townUnderAttack == town) {
					cells.add(cua);
				}
			}
			catch(NotRegisteredException ignored) {
			}
		}
		return cells;
	}
	
	public static boolean isUnderAttack(Town town) {
		for(CellUnderAttack cua : cellsUnderAttack.values()) {
			try {
				Town townUnderAttack = TownyAPI.getInstance().getTownBlock(cua.getFlagBaseBlock().getLocation()).getTown();
				if (townUnderAttack == null) {
					continue;
				}
				if(townUnderAttack == town) {
					return true;
				}
			}
			catch(NotRegisteredException ignored) {
			}
		}
		return false;
	}

	public static boolean isUnderAttack(Cell cell) {

		return cellsUnderAttack.containsKey(cell);
	}

	public static CellUnderAttack getAttackData(Cell cell) {

		return cellsUnderAttack.get(cell);
	}

	public static void removeCellUnderAttack(CellUnderAttack cell) {

		removeFlagFromPlayerCount(cell.getNameOfFlagOwner(), cell);
		cellsUnderAttack.remove(cell);
	}

	public static void attackWon(CellUnderAttack cell) {

		CellWonEvent cellWonEvent = new CellWonEvent(cell);
		getServer().getPluginManager().callEvent(cellWonEvent);
		cell.cancel();
		removeCellUnderAttack(cell);
	}

	public static void attackDefended(Player player, CellUnderAttack cell) {

		CellDefendedEvent cellDefendedEvent = new CellDefendedEvent(player, cell);
		getServer().getPluginManager().callEvent(cellDefendedEvent);
		cell.cancel();
		removeCellUnderAttack(cell);
	}

	public static void attackCanceled(CellUnderAttack cell) {

		CellAttackCanceledEvent cellAttackCanceledEvent = new CellAttackCanceledEvent(cell);
		getServer().getPluginManager().callEvent(cellAttackCanceledEvent);
		cell.cancel();
		removeCellUnderAttack(cell);
	}

	public static void removeAttackerFlags(String playerName) {

		List<CellUnderAttack> cells = cellsUnderAttackByPlayer.get(playerName);
		if (cells != null)
			for (CellUnderAttack cell : cells)
				attackCanceled(cell);
	}

	public static List<CellUnderAttack> getCellsUnderAttackByPlayer(String playerName) {

		List<CellUnderAttack> cells = cellsUnderAttackByPlayer.get(playerName);
		if (cells == null)
			return null;
		else
			return new ArrayList<>(cells);
	}

	private static void addFlagToPlayerCount(String playerName, CellUnderAttack cell) {

		List<CellUnderAttack> activeFlags = getCellsUnderAttackByPlayer(playerName);
		if (activeFlags == null)
			activeFlags = new ArrayList<>();

		activeFlags.add(cell);
		cellsUnderAttackByPlayer.put(playerName, activeFlags);
	}

	private static void removeFlagFromPlayerCount(String playerName, Cell cell) {

		List<CellUnderAttack> activeFlags = cellsUnderAttackByPlayer.get(playerName);
		if (activeFlags != null) {
			if (activeFlags.size() <= 1)
				cellsUnderAttackByPlayer.remove(playerName);
			else {
				activeFlags.remove(cell);
				cellsUnderAttackByPlayer.put(playerName, activeFlags);
			}
		}
	}

	public static void checkBlock(Player player, Block block, Cancellable event) {

		if (FlagWarConfig.isAffectedMaterial(block.getType())) {
			Cell cell = Cell.parse(block.getLocation());
			if (cell.isUnderAttack()) {
				CellUnderAttack cellAttackData = cell.getAttackData();
				if (cellAttackData.isFlag(block)) {
					FlagWar.attackDefended(player, cellAttackData);
					event.setCancelled(true);
				} else if (cellAttackData.isUneditableBlock(block)) {
					event.setCancelled(true);
				}
			}
		}
	}

	public static boolean callAttackCellEvent(Towny plugin, Player player, Block block, WorldCoord worldCoord) throws TownyException {
		int topY = block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()) - 1;
		if (block.getY() < topY)
			throw new TownyException(Translation.of("msg_err_enemy_war_must_be_placed_above_ground"));

		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident attackingResident;
		Town landOwnerTown, attackingTown;
		Nation landOwnerNation, attackingNation;
		TownBlock townBlock;

		attackingResident = townyUniverse.getResident(player.getUniqueId());
		
		if (attackingResident == null || !attackingResident.hasNation())
			throw new TownyException(Translation.of("msg_err_dont_belong_nation"));
		
		try {
			attackingTown = attackingResident.getTown();
			attackingNation = attackingTown.getNation();
		} catch (NotRegisteredException ignore) {
			// Can never happen
			return false;
		}
		
		if (attackingTown.getTownBlocks().size() < 1)
			throw new TownyException(Translation.of("msg_err_enemy_war_your_town_has_no_claims"));

		try {
			landOwnerTown = worldCoord.getTownBlock().getTown();
			townBlock = worldCoord.getTownBlock();
			landOwnerNation = landOwnerTown.getNation();
		} catch (NotRegisteredException e) {
			throw new TownyException(Translation.of("msg_err_enemy_war_not_part_of_nation"));
		}

		// Check Peace
		if (landOwnerNation.isNeutral())
			throw new TownyException(Translation.of("msg_err_enemy_war_is_peaceful", landOwnerNation.getFormattedName()));
		if (!townyUniverse.getPermissionSource().isTownyAdmin(player) && attackingNation.isNeutral())
			throw new TownyException(Translation.of("msg_err_enemy_war_is_peaceful", attackingNation.getFormattedName()));

		// Check Minimum Players Online
		checkIfTownHasMinOnlineForWar(landOwnerTown);
		checkIfNationHasMinOnlineForWar(landOwnerNation);
		checkIfTownHasMinOnlineForWar(attackingTown);
		checkIfNationHasMinOnlineForWar(attackingNation);

		// Check that attack takes place on the edge of a town
		if (FlagWarConfig.isAttackingBordersOnly() && !AreaSelectionUtil.isOnEdgeOfOwnership(landOwnerTown, worldCoord))
			throw new TownyException(Translation.of("msg_err_enemy_war_not_on_edge_of_town"));

		// Check that the user can pay for the warflag + fines from losing/winning.
		double costToPlaceWarFlag = FlagWarConfig.getCostToPlaceWarFlag();
		if (TownyEconomyHandler.isActive()) {
			try {
				double requiredAmount = costToPlaceWarFlag;
				double balance = attackingResident.getAccount().getHoldingBalance();

				// Check that the user can pay for the warflag.
				if (balance < costToPlaceWarFlag)
					throw new TownyException(Translation.of("msg_err_insuficient_funds_warflag", TownyEconomyHandler.getFormattedBalance(costToPlaceWarFlag)));

				// Check that the user can pay the fines from losing/winning all future warflags.
				int activeFlagCount = getNumActiveFlags(attackingResident.getName());
				double defendedAttackCost = FlagWarConfig.getDefendedAttackReward() * (activeFlagCount + 1);
				double attackWinCost = 0;

				double amount;
				amount = FlagWarConfig.getWonHomeblockReward();
				double homeBlockFine = amount < 0 ? -amount : 0;
				amount = FlagWarConfig.getWonTownblockReward();
				double townBlockFine = amount < 0 ? -amount : 0;

				// Assume rest of attacks are townblocks.
				// Would be incorrect if is also currently attacking a homeblock.
				// Error would be caught when actually taking the money when the plot has been won.
				if (townBlock.isHomeBlock())
					attackWinCost = homeBlockFine + activeFlagCount * townBlockFine;
				else
					attackWinCost = (activeFlagCount + 1) * townBlockFine;

				if (defendedAttackCost > 0 && attackWinCost > 0) {
					// There could be a fine
					String reason;
					double cost;
					if (defendedAttackCost > attackWinCost) {
						// Worst case scenario that all attacks are defended.
						requiredAmount += defendedAttackCost;
						cost = defendedAttackCost;
						reason = Translation.of("name_defended_attack");
					} else {
						// Worst case scenario that all attacks go through, but is forced to pay a rebuilding fine.
						requiredAmount += attackWinCost;
						cost = attackWinCost;
						reason = Translation.of("name_rebuilding");
					}

					// Check if player can pay in worst case scenario.
					if (balance < requiredAmount)
						throw new TownyException(Translation.of("msg_err_insuficient_funds_future", TownyEconomyHandler.getFormattedBalance(cost), String.format("%d %s", activeFlagCount + 1, reason + "(s)")));
				}
			} catch (EconomyException e) {
				throw new TownyException(e.getError());
			}
		}

		// Call Event (and make sure an attack isn't already under way)
		CellAttackEvent cellAttackEvent = new CellAttackEvent(plugin, player, block);
		plugin.getServer().getPluginManager().callEvent(cellAttackEvent);

		if (cellAttackEvent.isCancelled()) {
			if (cellAttackEvent.hasReason())
				throw new TownyException(cellAttackEvent.getReason());
			else
				return false;
		}

		// Successful Attack

		// Pay for war flag
		if (TownyEconomyHandler.isActive()) {
			// Skip payment + message if no cost.
			if (costToPlaceWarFlag > 0) {
				attackingResident.getAccount().withdraw(costToPlaceWarFlag, "War - WarFlag Cost");
				TownyMessaging.sendResidentMessage(attackingResident, Translation.of("msg_enemy_war_purchased_warflag", TownyEconomyHandler.getFormattedBalance(costToPlaceWarFlag)));
			}
		}

		// Set yourself as target's enemy so they can retaliate.
		if (!landOwnerNation.hasEnemy(attackingNation)) {
			landOwnerNation.addEnemy(attackingNation);
			landOwnerNation.save();
		}

		// Update Cache
		townyUniverse.addFlagWarZone(worldCoord);
		plugin.updateCache(worldCoord);

		TownyMessaging.sendGlobalMessage(Translation.of("msg_enemy_war_area_under_attack", landOwnerTown.getFormattedName(), worldCoord.toString(), attackingResident.getFormattedName()));
		return true;
	}

	public static void checkIfTownHasMinOnlineForWar(Town town) throws TownyException {

		int requiredOnline = FlagWarConfig.getMinPlayersOnlineInTownForWar();
		int onlinePlayerCount = TownyAPI.getInstance().getOnlinePlayers(town).size();
		if (onlinePlayerCount < requiredOnline)
			throw new TownyException(Translation.of("msg_err_enemy_war_require_online", requiredOnline, town.getFormattedName()));
	}

	public static void checkIfNationHasMinOnlineForWar(Nation nation) throws TownyException {

		int requiredOnline = FlagWarConfig.getMinPlayersOnlineInNationForWar();
		int onlinePlayerCount = TownyAPI.getInstance().getOnlinePlayers(nation).size();
		if (onlinePlayerCount < requiredOnline)
			throw new TownyException(Translation.of("msg_err_enemy_war_require_online", requiredOnline, nation.getFormattedName()));
	}

	public static WorldCoord cellToWorldCoord(Cell cell) throws NotRegisteredException {

		return new WorldCoord(cell.getWorldName(), cell.getX(), cell.getZ());
	}

	public static long lastFlagged(Town town) {
		if (lastFlag.containsKey(town))
			return lastFlag.get(town);
		else
			return 0;
	}

	public static void townFlagged(Town town) {
		if (lastFlag.containsKey(town))
			lastFlag.replace(town, System.currentTimeMillis());
		else
			lastFlag.put(town, System.currentTimeMillis());
	}
}
