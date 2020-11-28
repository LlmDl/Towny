package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import org.bukkit.entity.Player;

public class SiegeWarMoneyUtil {

	/**
	 * This method gives the war chest to the attacking nation
	 *
	 * @param siege the siege
	 */
	public static void giveWarChestToAttackingNation(Siege siege) {
		Nation winnerNation = siege.getAttackingNation();
		if (TownySettings.isUsingEconomy()) {
			try {
				winnerNation.getAccount().deposit(siege.getWarChestAmount(), "War Chest Captured/Returned");
				String message =
					String.format(
						Translation.of("msg_siege_war_attack_recover_war_chest"),
						winnerNation.getFormattedName(),
						TownyEconomyHandler.getFormattedBalance(siege.getWarChestAmount()));

				//Send message to nation(
				TownyMessaging.sendPrefixedNationMessage(winnerNation, message);
				//Send message to town
				TownyMessaging.sendPrefixedTownMessage(siege.getDefendingTown(), message);
			} catch (Exception e) {
				System.out.println("Problem paying war chest(s) to winner nation");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method gives one war chest to the defending town
	 *
	 * @param siege the siege zone
	 */
	public static void giveWarChestToDefendingTown(Siege siege) {
		Town winnerTown= siege.getDefendingTown();
		if(TownySettings.isUsingEconomy()) {
			try {
				winnerTown.getAccount().deposit(siege.getWarChestAmount(), "War Chest Captured");
				String message =
					String.format(
						Translation.of("msg_siege_war_attack_recover_war_chest"),
						winnerTown.getFormattedName(),
						TownyEconomyHandler.getFormattedBalance(siege.getWarChestAmount()));

				//Send message to nation
				TownyMessaging.sendPrefixedNationMessage(siege.getAttackingNation(), message);
				//Send message to town
				TownyMessaging.sendPrefixedTownMessage(winnerTown, message);
			} catch (EconomyException e) {
				System.out.println("Problem paying war chest(s) to winner town");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Gets the siegewar money multiplier for the given town
	 *
	 * @param town the town to consider
	 * @return the multiplier
	 */
	public static double getMoneyMultiplier(Town town) {
		double extraMoneyPercentage = SiegeWarSettings.getWarSiegeExtraMoneyPercentagePerTownLevel();

		if(extraMoneyPercentage == 0) {
			return 1;
		} else {
			return 1 + ((extraMoneyPercentage / 100) * (TownySettings.calcTownLevelId(town) -1));
		}
	}

	/**
	 * If the player is due a nation refund, pays the refund to the player
	 *
	 * @param player
	 */
	public static void claimNationRefund(Player player) throws Exception {
		if(!(SiegeWarSettings.getWarSiegeEnabled() && SiegeWarSettings.getWarSiegeRefundInitialNationCostOnDelete())) {
			throw new TownyException(Translation.of("msg_err_command_disable"));
		}

		TownyDataSource townyDataSource = TownyUniverse.getInstance().getDataSource();
		Resident formerKing = townyDataSource.getResident(player.getName());

		if(formerKing.getNationRefundAmount() != 0) {
			int refundAmount = formerKing.getNationRefundAmount();
			formerKing.getAccount().deposit(refundAmount, "Nation Refund");
			formerKing.setNationRefundAmount(0);
			townyDataSource.saveResident(formerKing);
			TownyMessaging.sendMsg(player, Translation.of("msg_siege_war_nation_refund_claimed", TownyEconomyHandler.getFormattedBalance(refundAmount)));
		} else {
			throw new TownyException(Translation.of("msg_err_siege_war_nation_refund_unavailable"));
		}
	}

	public static void makeNationRefundAvailable(Resident king) {
		//Refund some of the initial setup cost to the king
		if (SiegeWarSettings.getWarSiegeEnabled()
			&& TownySettings.isUsingEconomy()
			&& SiegeWarSettings.getWarSiegeRefundInitialNationCostOnDelete()) {

			//Make the nation refund available
			//The player can later do "/n claim refund" to receive the money
			int amountToRefund = (int)(TownySettings.getNewNationPrice() * 0.01 * SiegeWarSettings.getWarSiegeNationCostRefundPercentageOnDelete());
			king.addToNationRefundAmount(amountToRefund);
			TownyUniverse.getInstance().getDataSource().saveResident(king);

			TownyMessaging.sendMsg(
				king,
				String.format(
					Translation.of("msg_siege_war_nation_refund_available"),
					TownyEconomyHandler.getFormattedBalance(amountToRefund)));
		}
	}
}
