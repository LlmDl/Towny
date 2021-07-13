package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.map.TownyMapData;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import com.palmergames.bukkit.towny.event.asciimap.WildernessMapEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.Compass;

public class TownyAsciiMap {

	public static final int lineWidth = 27;
	public static final int halfLineWidth = lineWidth / 2;
	private static final int townBlockSize = TownySettings.getTownBlockSize();
	public static final String[] help = {
			"  " + Colors.Gray + "-" + Colors.LightGray + " = " + Translation.of("towny_map_unclaimed"),
			"  " + Colors.White + "+" + Colors.LightGray + " = " + Translation.of("towny_map_claimed"),
			"  " + Colors.White + "$" + Colors.LightGray + " = " + Translation.of("towny_map_forsale"),
			"  " + Colors.LightGreen + "+" + Colors.LightGray + " = " + Translation.of("towny_map_yourtown"),
			"  " + Colors.Yellow + "+" + Colors.LightGray + " = " + Translation.of("towny_map_yourplot"),
			"  " + Colors.Green + "+" + Colors.LightGray + " = " + Translation.of("towny_map_ally"),
			"  " + Colors.Red + "+" + Colors.LightGray + " = " + Translation.of("towny_map_enemy")};

	public static String[] generateCompass(Player player) {

		Compass.Point dir = Compass.getCompassPointForDirection(player.getLocation().getYaw());

		return new String[] {
				Colors.Black + "  -----  ",
				Colors.Black + "  -" + (dir == Compass.Point.NW ? Colors.Gold + "\\" : "-") + (dir == Compass.Point.N ? Colors.Gold : Colors.White) + "N" + (dir == Compass.Point.NE ? Colors.Gold + "/" + Colors.Black : Colors.Black + "-") + "-  ",
				Colors.Black + "  -" + (dir == Compass.Point.W ? Colors.Gold + "W" : Colors.White + "W") + Colors.LightGray + "+" + (dir == Compass.Point.E ? Colors.Gold : Colors.White) + "E" + Colors.Black + "-  ",
				Colors.Black + "  -" + (dir == Compass.Point.SW ? Colors.Gold + "/" : "-") + (dir == Compass.Point.S ? Colors.Gold : Colors.White) + "S" + (dir == Compass.Point.SE ? Colors.Gold + "\\" + Colors.Black : Colors.Black + "-") + "-  " };
	}

	public static void generateAndSend(Towny plugin, Player player, int lineHeight) {

		// Collect Sample Data
		boolean hasTown = false;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		if (resident == null) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered"));
			return;
		}
		
		if (resident.hasTown())
			hasTown = true;

		TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld().getName());
		if (world == null) { 
			TownyMessaging.sendErrorMsg(player, "You are not in a registered world.");
			return;
		}
		
		if (!world.isUsingTowny()) {
			TownyMessaging.sendErrorMsg(player, "This world is not using towny.");
			return;
		}
		Coord pos = Coord.parseCoord(plugin.getCache(player).getLastLocation());

		// Generate Map 
		int halfLineHeight = lineHeight / 2;
		TextComponent[][] townyMap = new TextComponent[lineWidth][lineHeight];
		int x, y = 0;
		for (int tby = pos.getX() + (lineWidth - halfLineWidth - 1); tby >= pos.getX() - halfLineWidth; tby--) {
			x = 0;
			for (int tbx = pos.getZ() - halfLineHeight; tbx <= pos.getZ() + (lineHeight - halfLineHeight - 1); tbx++) {
				try {
					townyMap[y][x] = Component.empty().color(NamedTextColor.WHITE);
					TownBlock townblock = world.getTownBlock(tby, tbx);
					if (!townblock.hasTown())
						throw new TownyException();
					Town town = townblock.getTownOrNull();
					if (x == halfLineHeight && y == halfLineWidth)
						// location
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GOLD);
					else if (hasTown) {
						if (resident.getTown() == town) {
							// own town
							townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GREEN);
							if (townblock.hasResident() && resident == townblock.getResidentOrNull())
								//own plot
								townyMap[y][x] = townyMap[y][x].color(NamedTextColor.YELLOW);
						} else if (resident.hasNation()) {
							if (resident.getTown().getNation().hasTown(town))
								// towns
								townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_GREEN);
							else if (town.hasNation()) {
								Nation nation = resident.getTown().getNation();
								if (nation.hasAlly(town.getNation()))
									townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GREEN);
								else if (nation.hasEnemy(town.getNation()))
									// towns
									townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_RED);
							}
						}
					}

					// Registered town block
					if (townblock.getPlotPrice() != -1 || townblock.hasPlotObjectGroup() && townblock.getPlotObjectGroup().getPrice() != -1) {
						// override the colour if it's a shop plot for sale
						if (townblock.getType().equals(TownBlockType.COMMERCIAL))
							townyMap[y][x] = townyMap[y][x].color(NamedTextColor.BLUE);
						townyMap[y][x] = townyMap[y][x].content("$");
					} else if (townblock.isHomeBlock())
						townyMap[y][x] = townyMap[y][x].content("H");
					else
						townyMap[y][x] = townyMap[y][x].content(townblock.getType().getAsciiMapKey());
					
					TextComponent forSaleComponent = Component.empty();
					TextComponent claimedAtComponent = Component.empty();
					TextComponent groupComponent = Component.empty();
					
					if (TownyEconomyHandler.isActive()) {
						double cost;
						if (townblock.hasPlotObjectGroup())
							cost = townblock.getPlotObjectGroup().getPrice();
						else 
							cost = townblock.getPlotPrice();
						
						if (cost > -1)
							forSaleComponent = Component.text(String.format(ChunkNotification.forSaleNotificationFormat, TownyEconomyHandler.getFormattedBalance(cost)).replaceAll("[\\[\\]]", "")).color(NamedTextColor.YELLOW).append(Component.newline());
					}
					
					if (townblock.getClaimedAt() > 0)
						claimedAtComponent = Component.text(Translation.of("msg_plot_perm_claimed_at", TownyFormatter.registeredFormat.format(townblock.getClaimedAt()))).append(Component.newline());

					if (townblock.hasPlotObjectGroup()) {
						groupComponent = Component.text(Translation.of("map_hover_plot_group")).color(NamedTextColor.DARK_GREEN)
							.append(Component.text(townblock.getPlotObjectGroup().getFormattedName()).color(NamedTextColor.GREEN)
							.append(Component.text(Translation.of("map_hover_plot_group_size")).color(NamedTextColor.DARK_GREEN)
							.append(Component.text(Translation.of("map_hover_plots", townblock.getPlotObjectGroup().getTownBlocks().size())).color(NamedTextColor.GREEN)
							.append(Component.newline()))));
					}

					
					TextComponent hoverComponent = Component.text(Translation.of("status_town") + town.getName() + (townblock.hasResident() ? " (" + townblock.getResidentOrNull().getName() + ")" : "")).color(NamedTextColor.GREEN).append(Component.text(" (" + tby + ", " + tbx + ")").color(NamedTextColor.WHITE)).append(Component.newline())
						.append(Component.text(Translation.of("status_plot_type")).color(NamedTextColor.DARK_GREEN).append(Component.text(townblock.getType().getName()).color(NamedTextColor.GREEN).append(Component.newline())
						.append(groupComponent)
						.append(forSaleComponent)
						.append(claimedAtComponent)
						.append(Component.text(Translation.of("towny_map_detailed_information")).color(NamedTextColor.DARK_GREEN))));

					townyMap[y][x] = townyMap[y][x].hoverEvent(HoverEvent.showText(hoverComponent)).clickEvent(ClickEvent.runCommand("/towny:plot perm " + tby + " " + tbx));
				} catch (TownyException e) {
					// Unregistered town block (Wilderness)

					if (x == halfLineHeight && y == halfLineWidth)
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GOLD);
					else
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_GRAY);

					WorldCoord wc = WorldCoord.parseWorldCoord(world.getName(), tby * townBlockSize , tbx* townBlockSize);
					String symbol;
					TextComponent hoverText;
					String clickCommand;
					// Cached TownyMapData is present and not old.
					if (getWildernessMapDataMap().containsKey(wc) && !getWildernessMapDataMap().get(wc).isOld()) {
						TownyMapData mapData = getWildernessMapDataMap().get(wc);
						symbol = mapData.getSymbol();
						hoverText = mapData.getHoverText();
						clickCommand = mapData.getClickCommand();
					// Cached TownyMapData is either not present or was considered old.
					} else {
						if (getWildernessMapDataMap().containsKey(wc))
							getWildernessMapDataMap().remove(wc);
						WildernessMapEvent wildMapEvent = new WildernessMapEvent(wc);
						Bukkit.getPluginManager().callEvent(wildMapEvent);
						symbol = wildMapEvent.getMapSymbol();
						hoverText = wildMapEvent.getHoverText();
						clickCommand = wildMapEvent.getClickCommand();
						getWildernessMapDataMap().put(wc, new TownyMapData(wc, symbol, hoverText, clickCommand));
						
						Bukkit.getScheduler().runTaskLater(Towny.getPlugin(), ()-> {
							if (getWildernessMapDataMap().containsKey(wc) && getWildernessMapDataMap().get(wc).isOld())
								getWildernessMapDataMap().remove(wc);
						}, 20 * 35);
					}

					townyMap[y][x] = townyMap[y][x].content(symbol)
							.clickEvent(ClickEvent.runCommand(clickCommand))
							.hoverEvent(HoverEvent.showText(hoverText));
				}
				x++;
			}
			y++;
		}

		String[] compass = generateCompass(player);

		// Output
		TownyMessaging.sendMessage(player, ChatTools.formatTitle(Translation.of("towny_map_header") + Colors.White + "(" + pos.toString() + ")"));
		String line;
		int lineCount = 0;
		// Variables have been rotated to fit N/S/E/W properly
		for (int my = 0; my < lineHeight; my++) {
			line = compass[0];
			if (lineCount < compass.length)
				line = compass[lineCount];

			TextComponent compassComponent = Component.text(line);
			TextComponent fullComponent = Component.empty();
			for (int mx = lineWidth - 1; mx >= 0; mx--)
				fullComponent = fullComponent.append(townyMap[mx][my]);

			if (lineCount < help.length)
				fullComponent = fullComponent.append(Component.text(help[lineCount]));

			Towny.getAdventure().player(player).sendMessage(compassComponent.append(fullComponent));
			lineCount++;
		}

		TownBlock townblock = TownyAPI.getInstance().getTownBlock(plugin.getCache(player).getLastLocation());
		TownyMessaging.sendMsg(player, (Translation.of("town_sing") + ": " + (townblock != null && townblock.hasTown() ? townblock.getTownOrNull().getName() : Translation.of("status_no_town")) + " : " + Translation.of("owner_status") + ": " + (townblock != null && townblock.hasResident() ? townblock.getResidentOrNull().getName() : Translation.of("status_no_town"))));
	}
	
	private static Map<WorldCoord, TownyMapData> getWildernessMapDataMap() {
		return TownyUniverse.getInstance().getWildernessMapDataMap();
	}
}
