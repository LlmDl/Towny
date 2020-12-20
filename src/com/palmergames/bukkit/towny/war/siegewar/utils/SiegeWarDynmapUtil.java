package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.objects.HeldItemsCombination;
import com.palmergames.bukkit.towny.war.siegewar.siege.SiegeController;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * This class contains utility functions related to the dynmap
 *
 * @author Goosius
 */
public class SiegeWarDynmapUtil {

	public static String TACTICAL_INVISIBILITY_METADATA_ID = "tacticallyInvisible";
	public static FixedMetadataValue TACTICAL_INVISIBILITY_FIXED_METADATA_VALUE = new FixedMetadataValue(Towny.getPlugin(), true);
	
	/**
	 * Evaluate players to see if they are 'tactically' invisible
	 * 
	 * Tactical invisibility makes a player invisible on the dynmap
	 * It is triggered if the player sets their main/off hand combinations 
	 * to one of the specified combinations (set in config file).
	 *
	 * Players in banner control sessions cannot be tactically invisible
	 */
	public static void evaluatePlayerTacticalInvisibility() {
		boolean invisibleOnDynmap;

		for(Player player: BukkitTools.getOnlinePlayers()) {
			try {
				//Player is visible by default
				invisibleOnDynmap = false;

				//Check if player is invisible
				if (!SiegeController.getPlayersInBannerControlSessions().contains(player)) {

					//Check item combinations
					for(HeldItemsCombination heldItemsCombination: SiegeWarSettings.getWarSiegeTacticalVisibilityItems()) {

						//Off Hand
						if(!heldItemsCombination.isIgnoreOffHand() && player.getInventory().getItemInOffHand().getType() != heldItemsCombination.getOffHandItemType())
							continue;  //off hand does not match. Try next combo

						//Main hand
						if(!heldItemsCombination.isIgnoreMainHand() && player.getInventory().getItemInMainHand().getType() != heldItemsCombination.getMainHandItemType())
							continue; //main hand does not match. Try next combo

						//Player invisible on map
						invisibleOnDynmap = true;
						break;
					}
				}

				if(invisibleOnDynmap) {
					if(!player.hasMetadata(TACTICAL_INVISIBILITY_METADATA_ID)) {
						player.setMetadata(TACTICAL_INVISIBILITY_METADATA_ID, TACTICAL_INVISIBILITY_FIXED_METADATA_VALUE);
					}
				} else {
					if (player.hasMetadata(TACTICAL_INVISIBILITY_METADATA_ID)) {
						player.removeMetadata(TACTICAL_INVISIBILITY_METADATA_ID, Towny.getPlugin());
					}
				}

			} catch (Exception e) {
				try {
					System.out.println("Problem evaluating tactical invisibility for player " + player.getName());
				} catch (Exception e2) {
					System.out.println("Problem evaluating tactical invisibility for a player (could not read player name)");
				}
				e.printStackTrace();
			}
		}
	}
}
