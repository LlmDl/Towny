package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Towny message handling class
 *
 * @author ElgarL
 *
 */

public class TownyMessaging {
	private static final Logger LOGGER = LogManager.getLogger(Towny.class);
	private static final Logger LOGGER_DEBUG = LogManager.getLogger("com.palmergames.bukkit.towny.debug");

	/**
	 * Sends an error message to the log
	 *
	 * @param msg message to send
	 */
	public static void sendErrorMsg(String msg) {
		LOGGER.warn(ChatTools.stripColour("[Towny] Error: " + msg));
	}

	/**
	 * Sends an Error message (red) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param sender the Object sending the message
	 * @param msg the message to send
	 */
	public static void sendErrorMsg(Object sender, String msg) {
		if (sender != null) {
			CommandSender toSend = (CommandSender) sender;
			if (toSend instanceof ConsoleCommandSender) {
				toSend.sendMessage(ChatColor.stripColor(msg));
			} else {
				toSend.sendMessage(Translation.of("default_towny_prefix") + ChatColor.RED + msg);
			}
		} else {
			sendErrorMsg("Sender cannot be null!");
		}
		
		sendDevMsg(msg);
	}

	/**
	 * Sends an Error message (red) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param sender the Object sending the message
	 * @param msg the message array being sent.
	 */
	public static void sendErrorMsg(Object sender, String[] msg) {
		for (String line : msg) {
			sendErrorMsg(sender, line);
		}
	}

	/**
	 * Sends a message to console only
	 * prefixed by [Towny]
	 *
	 * @param msg the message to be sent
	 */
	public static void sendMsg(String msg) {
		
		LOGGER.info("[Towny] " + ChatTools.stripColour(msg));
	}

	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param sender the CommandSender receiving the msg
	 * @param msg the message being sent
	 */
	public static void sendMsg(CommandSender sender, String msg) {
		if (sender == null) {
			sendErrorMsg("Sender cannot be null!");
			return;
		}
		
		if (sender instanceof Player) {
			Player p = (Player)sender;
			p.sendMessage(Translation.of("default_towny_prefix") + ChatColor.GREEN + msg);
		} else if (sender instanceof ConsoleCommandSender) {
			sender.sendMessage(ChatColor.stripColor(msg));
		} else {
			sender.sendMessage(Translation.of("default_towny_prefix") + ChatColor.GREEN + msg);
		}
		
		sendDevMsg(msg);
	}
	
	/**
	 * Sends a message (green) to the resident
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param resident to receive the msg
	 * @param msg the message being sent
	 */
	public static void sendMsg(Resident resident, String msg) {
		if (BukkitTools.isOnline(resident.getName()))
			sendMsg(resident.getPlayer(), msg);
	}

	// todo: these two can probably be consolidated
	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param player the player to receive the message
	 * @param msg the message to be sent
	 */
	public static void sendMsg(Player player, String[] msg) {
		for (String line : msg) {
			sendMsg(player, line);
		}
	}
	
	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param player the player to receive the message
	 * @param msg the message to be sent
	 */
	public static void sendMsg(Player player, List<String> msg) {
		for (String line : msg) {
			sendMsg(player, line);
		}
	}

	/**
	 * Sends a message (red) to the named Dev (if DevMode is enabled)
	 * Uses default_towny_prefix
	 *
	 * @param msg the message to be sent
	 */
	public static void sendDevMsg(String msg) {
		if (TownySettings.isDevMode()) {
			Player townyDev = BukkitTools.getPlayer(TownySettings.getDevName());
			if (townyDev == null) {
				return;
			}
			townyDev.sendMessage(Translation.of("default_towny_prefix") + " DevMode: " + ChatColor.RED + msg);
		}
	}

	/**
	 * Sends a message (red) to the named Dev (if DevMode is enabled)
	 * Uses default_towny_prefix
	 *
	 * @param msg the message to be sent
	 */
	public static void sendDevMsg(String[] msg) {
		for (String line : msg) {
			sendDevMsg(line);
		}
	}

	/**
	 * Sends a message to the log and console
	 * prefixed by [Towny] Debug:
	 *
	 * @param msg the message to be sent
	 */
	public static void sendDebugMsg(String msg) {
		if (TownySettings.getDebug()) {
			LOGGER_DEBUG.info(ChatTools.stripColour("[Towny] Debug: " + msg));
		}
		sendDevMsg(msg);
	}

	/////////////////

	/**
	 * Send a message to a player
	 *
	 * @param sender the Object sending the message
	 * @param lines List of strings to send
	 */
	public static void sendMessage(Object sender, List<String> lines) {
		sendMessage(sender, lines.toArray(new String[0]));
	}

	/**
	 * Send a message to a player
	 *
	 * @param sender the Object sending the message
	 * @param line the String to send
	 */
	public static void sendMessage(Object sender, String line) {
		if ((sender instanceof Player)) {
			((Player) sender).sendMessage(line);
		} else if (sender instanceof CommandSender) {
			((CommandSender) sender).sendMessage(Colors.strip(line));
		} else if (sender instanceof Resident) {
			Player p = TownyAPI.getInstance().getPlayer((Resident) sender);
			if (p == null) {
				return;
			}
			p.sendMessage(Colors.strip(line));
		}
	}

	/**
	 * Send a message to a player
	 *
	 * @param sender the Object sending the message
	 * @param lines String array to send as message.
	 */
	public static void sendMessage(Object sender, String[] lines) {
		for (String line : lines)
			sendMessage(sender, line);
	}

	/**
	 * Send a message to ALL online players and the log.
	 * Uses default_towny_prefix
	 *
	 * @param lines String list to send as a message
	 */
	public static void sendGlobalMessage(List<String> lines) {
		sendGlobalMessage(lines.toArray(new String[0]));
	}

	/**
	 * Send a message to ALL online players and the log.
	 * Uses default_towny_prefix
	 *
	 * @param lines String array to send as a message
	 */
	public static void sendGlobalMessage(String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Global Msg] " + line));
		}
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				for (String line : lines) {
					player.sendMessage(Translation.of("default_towny_prefix") + line);
				}
			}
		}
	}

	/**
	 * Send a message to All online players and the log.
	 * Uses default_towny_prefix
	 *
	 * @param line the message to send
	 */
	public static void sendGlobalMessage(String line) {
		LOGGER.info(ChatTools.stripColour("[Global Message] " + line));
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null && TownyAPI.getInstance().isTownyWorld(player.getWorld()))
				player.sendMessage(Translation.of("default_towny_prefix") + line);
		}
	}
	
	/**
	 * Send a message to All online players and the log.
	 * Does not use the default_towny_prefix.
	 * 
	 * @param line the message to send.
	 */
	public static void sendPlainGlobalMessage(String line) {
		LOGGER.info(ChatTools.stripColour("[Global Message] " + line));
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null && TownyAPI.getInstance().isTownyWorld(player.getWorld()))
				player.sendMessage(line);
		}		
	}

	/**
	 * Send a message to a specific resident
	 * preceded by the default_towny_prefix
	 *
	 * @param resident the resident to receive the message
	 * @param line message String to send
	 * @throws TownyException if the player is null
	 */
	public static void sendResidentMessage(Resident resident, String line) throws TownyException {
		LOGGER.info(ChatTools.stripColour("[Resident Msg] " + resident.getName() + ": " + line));
		Player player = TownyAPI.getInstance().getPlayer(resident);
		if (player == null) {
			throw new TownyException("Player could not be found!");
		}
		player.sendMessage(Translation.of("default_towny_prefix") + line);
	}

	/**
	 * Send a message to All online residents of a town and log, 
	 * preceded by the default_towny_prefix
	 *
	 * @param town town to receive the message
	 * @param line the message
	 */
	public static void sendTownMessagePrefixed(Town town, String line) {
		LOGGER.info(ChatTools.stripColour(line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendMessage(Translation.of("default_towny_prefix") + line);
	}

	/**
	 * Send a message to All online residents of a town and log
	 * preceded by the [Townname]
	 *
	 * @param town the town to pass the message to, and prefix message with
	 * @param line the actual message
	 */
	public static void sendPrefixedTownMessage(Town town, String line) {
		LOGGER.info(ChatTools.stripColour("[Town Msg] " + StringMgmt.remUnderscore(town.getName()) + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendMessage(Translation.of("default_town_prefix", StringMgmt.remUnderscore(town.getName())) + line);
	}

	/**
	 * Send a multi-line message to All online residents of a town and log, 
	 * preceded by the [Townname]
	 *
	 * @param town town to receive the message
	 * @param lines Array of Strings constituting the message.
	 */
	public static void sendPrefixedTownMessage(Town town, String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour(line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			for (String line : lines) {
				player.sendMessage(Translation.of("default_town_prefix", StringMgmt.remUnderscore(town.getName())) + line);
			}
	}
	
	/**
	 * Send a multi-line message to All online residents of a town and log, 
	 * preceded by the [Townname]
	 *
	 * @param town town to receive the message
	 * @param lines List of Strings constituting the message.
	 */
	public static void sendPrefixedTownMessage(Town town, List<String> lines) {
		sendPrefixedTownMessage(town, lines.toArray(new String[0]));
	}
	
	/**
	 * Send a message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation nation to send to, and prefix message with
	 * @param line the message
	 */
	public static void sendPrefixedNationMessage(Nation nation, String line) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendMessage(Translation.of("default_nation_prefix", StringMgmt.remUnderscore(nation.getName())) + line);
	}

	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation the nation to send to
	 * @param lines list of Strings containing the message
	 */
	public static void sendPrefixedNationMessage(Nation nation, List<String> lines) {
		sendPrefixedNationMessage(nation, lines.toArray(new String[0]));
	}

	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation the nation to send to
	 * @param lines array of Strings containing the message
	 */
	public static void sendPrefixedNationMessage(Nation nation, String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation)) {
			for (String line : lines) {
				player.sendMessage(Translation.of("default_nation_prefix", StringMgmt.remUnderscore(nation.getName())) + line);
			}
		}
	}
	
	/**
	 * Send a message to All online residents of a nation and log
	 * Uses default_towny_prefix
	 *
	 * @param nation the nation to send message to
	 * @param line the message
	 */
	public static void sendNationMessagePrefixed(Nation nation, String line) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendMessage(Translation.of("default_towny_prefix") + line);
	}
	
	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * Uses default_towny_prefix
	 *
	 * @param nation the nation to send message to
	 * @param lines the list of lines of the message
	 */
	public static void sendNationMessagePrefixed(Nation nation, List<String> lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			for (String line : lines) {
				player.sendMessage(Translation.of("default_towny_prefix") + line);
			}
	}

	/**
	 * Send the town board to a player (in yellow)
	 *
	 * @param player player to show to
	 * @param town the town for which to show it's board
	 */
	public static void sendTownBoard(Player player, Town town) {
		String tbColor1 = Translation.of("townboard_message_colour_1");
		String tbColor2 = Translation.of("townboard_message_colour_2");
		
		player.sendMessage(tbColor1 + "[" + StringMgmt.remUnderscore(town.getName()) + "] " + tbColor2 + town.getBoard());
	}
	
	/**
	 * Send the nation board to a player (in yellow)
	 *
	 * @param player player to show to
	 * @param nation the nation for which to show it's board
	 */
	public static void sendNationBoard(Player player, Nation nation) {
		String nbColor1 = Translation.of("nationboard_message_colour_1");
		String nbColor2 = Translation.of("nationboard_message_colour_2");

		player.sendMessage(nbColor1 + "[" + StringMgmt.remUnderscore(nation.getName()) + "] " + nbColor2 + nation.getBoard());
	}
	
	/**
	 * Send a message to all residents in the list with the required mode
	 *
	 * @param residents List of residents to show the message to
	 * @param msg the message to send
	 * @param modeRequired a resident mode required for the resident to receive the message.
	 */
	public static void sendMessageToMode(ResidentList residents, String msg, String modeRequired) {

		for (Resident resident : TownyAPI.getInstance().getOnlineResidents(residents))
			if (resident.hasMode(modeRequired))
				sendMessage(resident, msg);
	}
	
	/**
	 * Send a message to all residents in the town with the required mode
	 * no prefix used
	 * 
	 * @param town the town to send message to
	 * @param msg the message to send
	 * @param modeRequired mode a resident must have to receive message
	 */
	public static void sendMessageToMode(Town town, String msg, String modeRequired) {

		for (Resident resident : town.getResidents())
			if (BukkitTools.isOnline(resident.getName()))
				sendMessage(resident,msg);
	}
	
	/**
	 * Send a message to all residents in the nation with the required mode
	 * no prefix used
	 * 
	 * @param nation the nation to receive the message
	 * @param msg the message to send
	 * @param modeRequired mode a resident must have to receive message
	 */
	public static void sendMessageToMode(Nation nation, String msg, String modeRequired) {

		for (Resident resident : nation.getResidents())
			if (BukkitTools.isOnline(resident.getName()))
				sendMessage(resident,msg);
	}
	
	/**
	 * Send a Title and Subtitle to a resident
	 *
	 * @param resident resident to receive title &amp; subtitle message
	 * @param title title message to send
	 * @param subtitle subtitle message to send
	 */
	public static void sendTitleMessageToResident(Resident resident, String title, String subtitle) {
		Player player = TownyAPI.getInstance().getPlayer(resident);
		if (player == null) {
			return;
		}
		player.sendTitle(title, subtitle, 10, 70, 10);
	}
	
	/**
	 * Send a Title and Subtitle to a town
	 *
	 * @param town town to receive title &amp; subtitle messages
	 * @param title title message to send
	 * @param subtitle subtitle message to send
	 */
	public static void sendTitleMessageToTown(Town town, String title, String subtitle) {
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendTitle(title, subtitle, 10, 70, 10);
	}

	/**
	 * Send a Title and Subtitle to a nation
	 *
	 * @param nation   - Nation object
	 * @param title    - Title
	 * @param subtitle - Subtitle
	 */
	public static void sendTitleMessageToNation(Nation nation, String title, String subtitle) {
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendTitle(title, subtitle, 10, 70, 10);
	}

	public static void sendRequestMessage(CommandSender player, Invite invite) {
		if (invite.getSender() instanceof Town) { // Town invited Resident
			String firstline = Translation.of("invitation_prefix") + Translation.of("you_have_been_invited_to_join2", invite.getSender().getName());
			String secondline = "/" + TownySettings.getAcceptCommand() + " " + invite.getSender().getName();
			String thirdline = "/" + TownySettings.getDenyCommand() + " " + invite.getSender().getName();
			sendConfirmationMessage(player, firstline, secondline, thirdline, "");
		}
		if (invite.getSender() instanceof Nation) {
			if (invite.getReceiver() instanceof Town) { // Nation invited Town
				String firstline = Translation.of("invitation_prefix") + Translation.of("your_town_has_been_invited_to_join_nation", invite.getSender().getName());
				String secondline = "/t invite accept " + invite.getSender().getName();
				String thirdline = "/t invite deny " + invite.getSender().getName();
				sendConfirmationMessage(player, firstline, secondline, thirdline, "");
			}
			if (invite.getReceiver() instanceof Nation) { // Nation allied Nation
				String firstline = Translation.of("invitation_prefix") + Translation.of("you_have_been_requested_to_ally2", invite.getSender().getName());
				String secondline = "/n ally accept " + invite.getSender().getName();
				String thirdline = "/n ally deny " + invite.getSender().getName();
				sendConfirmationMessage(player, firstline, secondline, thirdline, "");
			}
		}
	}

	/**
	 * Sends a player click-able confirmation messages.
	 * @param player - The player (CommandSender) to send the confirmation
	 * @param firstline - The question regarding the confirmation.
	 * @param confirmline - Line for sending the confirmation.
	 * @param cancelline - Line for sending the cancellation.
	 * @param lastline - If null, announces that the message will expire. Otherwise, ignored.
	 */
	public static void sendConfirmationMessage(CommandSender player, String firstline, String confirmline, String cancelline, String lastline) {

		if (firstline == null) {
			firstline = Translation.of("are_you_sure_you_want_to_continue");
		}
		if (confirmline == null) {
			confirmline = "/" + TownySettings.getConfirmCommand();
		}
		if (cancelline == null) {
			cancelline = "/" + TownySettings.getCancelCommand();
		}
		
		TextComponent lastLineComponent;
		if (lastline == null) {
			lastLineComponent = Component.newline().append(Component.text(Translation.of("this_message_will_expire2")));
		} else
			lastLineComponent = Component.newline().append(Component.text(lastline));

		// Create confirm button based on given params.
		TextComponent confirmComponent = Component.text(confirmline.replace("/", "[/").concat("]"))
			.color(NamedTextColor.GREEN)
			.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_confirmation_spigot_click_accept", confirmline.replace("/", ""), confirmline))))
			.clickEvent(ClickEvent.runCommand("/towny:" + confirmline.replace("/","")));

		// Create cancel button based on given params.
		TextComponent cancelComponent = Component.text(cancelline.replace("/", "[/").concat("]"))
			.color(NamedTextColor.RED)
			.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_confirmation_spigot_click_cancel", cancelline.replace("/", ""), cancelline))))
			.clickEvent(ClickEvent.runCommand("/towny:" + cancelline.replace("/","")));
		
		Towny.getAdventure().sender(player).sendMessage(Component.text(Translation.of("confirmation_prefix") + firstline).append(Component.newline())
			.append(confirmComponent).append(Component.text(" ")).append(cancelComponent)
			.append(lastLineComponent)
		);
	}

	public static void sendTownList(CommandSender sender, List<TextComponent> towns, ComparatorType compType, int page, int total) {
		int iMax = Math.min(page * 10, towns.size());

		TextComponent[] townsformatted;
		
		if ((page * 10) > towns.size()) {
			townsformatted = new TextComponent[towns.size() % 10];
		} else {
			townsformatted = new TextComponent[10];
		}
		
		// Populate the page with TextComponents.
		for (int i = (page - 1) * 10; i < iMax; i++) {
			townsformatted[i % 10] = towns.get(i);
		}
		
		Audience audience = Towny.getAdventure().sender(sender);
		sender.sendMessage(ChatTools.formatTitle(Translation.of("town_plu")));
		sender.sendMessage(Colors.Blue + Translation.of("town_name") + (TownySettings.isTownListRandom() ? "" : Colors.Gray + " - " + Colors.LightBlue + Translation.of(compType.getName())));
		for (TextComponent textComponent : townsformatted)
			audience.sendMessage(textComponent);
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town", page, compType.getCommandString(), total);
		audience.sendMessage(pageFooter);
	}

	public static TextComponent getPageNavigationFooter(String prefix, int page, String arg, int total) {
		TextComponent backButton = Component.text("<<<")
			.color(NamedTextColor.GOLD)
			.clickEvent(ClickEvent.runCommand("/" + prefix + " list " + (arg.isEmpty() ? "" : arg + " ") + (page - 1)))
			.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_hover_previous_page"))));
		
		TextComponent forwardButton = Component.text(">>>")
			.color(NamedTextColor.GOLD)
			.clickEvent(ClickEvent.runCommand("/" + prefix + " list " + (arg.isEmpty() ? "" : arg + " ") + (page + 1)))
			.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_hover_next_page"))));
		
		TextComponent pageText = Component.text("   " + Translation.of("LIST_PAGE", page, total) + "   ");

		if (page == 1 && page == total) {
			backButton = backButton.clickEvent(null).hoverEvent(null).color(NamedTextColor.DARK_GRAY);
			forwardButton = forwardButton.clickEvent(null).hoverEvent(null).color(NamedTextColor.DARK_GRAY);
		} else if (page == 1) {
			backButton = backButton.clickEvent(null).hoverEvent(null).color(NamedTextColor.DARK_GRAY);
		} else if (page == total) {
			forwardButton = forwardButton.clickEvent(null).hoverEvent(null).color(NamedTextColor.DARK_GRAY);
		}

		return backButton.append(pageText).append(forwardButton);
	}

	public static void sendNationList(CommandSender sender, List<TextComponent> nations, ComparatorType compType, int page, int total) {
		int iMax = Math.min(page * 10, nations.size());

		TextComponent[] nationsformatted;
		if ((page * 10) > nations.size()) {
			nationsformatted = new TextComponent[nations.size() % 10];
		} else {
			nationsformatted = new TextComponent[10];
		}
		
		// Populate the page with TextComponents.
		for (int i = (page - 1) * 10; i < iMax; i++) {
			nationsformatted[i % 10] = nations.get(i);
		}

		sender.sendMessage(ChatTools.formatTitle(Translation.of("nation_plu")));
		sender.sendMessage(Colors.Blue + Translation.of("nation_name") + Colors.Gray + " - " + Colors.LightBlue + Translation.of(compType.getName()));
		Audience audience = Towny.getAdventure().sender(sender);
		for (TextComponent textComponent : nationsformatted) {
			audience.sendMessage(textComponent);
		}

		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:nation", page, compType.getCommandString(), total);
		audience.sendMessage(pageFooter);
	}

	public static void sendOutpostList(Player player, Town town, int page, int total) {
		int outpostsCount = town.getAllOutpostSpawns().size();
		int iMax = Math.min(page * 10, outpostsCount);
		List<Location> outposts = town.getAllOutpostSpawns();
		
		TextComponent[] outpostsFormatted;
		
		if ((page * 10) > outpostsCount) {
			outpostsFormatted = new TextComponent[outpostsCount % 10];
		} else {
			outpostsFormatted = new TextComponent[10];
		}
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Location outpost = outposts.get(i);
			TownBlock tb = TownyAPI.getInstance().getTownBlock(outpost);
			if (tb == null)
				continue;
			String name = !tb.hasPlotObjectGroup() ? tb.getName() : tb.getPlotObjectGroup().getName();
			TextComponent dash = Component.text(" - ").color(NamedTextColor.DARK_GRAY);		
			TextComponent line = Component.text(Integer.toString(i + 1))
				.color(NamedTextColor.GOLD)
				.clickEvent(ClickEvent.runCommand("/towny:town outpost " + (i + 1)))
				.append(dash);

			TextComponent outpostName = Component.text(name).color(NamedTextColor.GREEN);
			TextComponent worldName = Component.text(outpost.getWorld().getName()).color(NamedTextColor.BLUE);
			TextComponent coords = Component.text("(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")").color(NamedTextColor.BLUE);

			if (!name.equalsIgnoreCase("")) {
				line = line.append(outpostName).append(dash);
			}
			line = line.append(worldName).append(dash).append(coords);
			
			String spawnCost = "Free";

			if (TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

			line = line.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_click_spawn", name.equalsIgnoreCase("") ? "outpost" : name) + "\n" + spawnCost).color(NamedTextColor.GOLD)));
			outpostsFormatted[i % 10] = line;
		}
		
		Audience audience = Towny.getAdventure().player(player);
		player.sendMessage(ChatTools.formatTitle(Translation.of("outpost_plu")));
		for (TextComponent textComponent : outpostsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town outpost", page, "", total);
		audience.sendMessage(pageFooter);
	}
	
	public static void sendJailList(Player player, Town town, int page, int total) {
		int jailCount = town.getJails().size();
		int iMax = Math.min(page * 10, jailCount);
		List<Jail> jails = new ArrayList<>(town.getJails());
		
		TextComponent[] jailsFormatted;
		
		if ((page * 10) > jailCount) {
			jailsFormatted = new TextComponent[jailCount % 10];
		} else {
			jailsFormatted = new TextComponent[10];
		}
		String headerMsg = ChatColor.GOLD + "# " +
							ChatColor.DARK_GRAY + "- "+
							ChatColor.GREEN + "Jail Name " +
							ChatColor.DARK_GRAY + "- "+
							ChatColor.BLUE + "Coord " +
							ChatColor.DARK_GRAY + "- " +
							ChatColor.YELLOW + "Cell Count " +
							ChatColor.DARK_GRAY + "- " +
							ChatColor.RED + "Primary Jail";
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Jail jail = jails.get(i);

			TextComponent name = Component.text(jail.getName()).color(NamedTextColor.GREEN);
			TextComponent coord = Component.text(jail.getTownBlock().getWorldCoord().toString()).color(NamedTextColor.BLUE);
			TextComponent cellCount = Component.text(String.valueOf(jail.getJailCellLocations().size())).color(NamedTextColor.YELLOW);
			TextComponent dash = Component.text(" - ").color(NamedTextColor.DARK_GRAY);

			TextComponent line = Component.text(Integer.toString(i + 1)).color(NamedTextColor.GOLD);
			if (jail.hasName())
				line = line.append(dash).append(name);
			line = line.append(dash).append(coord).append(dash).append(cellCount);
				
			if (town.getPrimaryJail().getUUID().equals(jail.getUUID()))
				line = line.append(dash).append(Component.text("(Primary Jail)").color(NamedTextColor.RED));

			jailsFormatted[i % 10] = line;
		}
		Audience audience = Towny.getAdventure().player(player);
		player.sendMessage(ChatTools.formatTitle(Translation.of("jail_plu")));
		player.sendMessage(headerMsg);
		for (TextComponent textComponent : jailsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town jail list", page, "", total);
		audience.sendMessage(pageFooter);
	}
	
	public static void sendPlotGroupList(CommandSender sender, Town town, int page, int total) {
		int groupCount = town.getPlotGroups().size();
		int iMax = Math.min(page * 10,  groupCount);
		List<PlotGroup> groups = new ArrayList<>(town.getPlotGroups());
		
		TextComponent[] groupsFormatted;
		if ((page * 10) > groupCount) {
			groupsFormatted = new TextComponent[groupCount % 10];
		} else {
			groupsFormatted = new TextComponent[10];
		}
		
		String headerMsg = ChatColor.GOLD + "# " +
				ChatColor.DARK_GRAY + "- "+
				ChatColor.GREEN + "Group Name " +
				ChatColor.DARK_GRAY + "- " +
				ChatColor.YELLOW + "Plot Size " +
				ChatColor.DARK_GRAY + "- " +
				ChatColor.BLUE + "For Sale";
		for (int i = (page - 1) * 10; i < iMax; i++) {
			PlotGroup group = groups.get(i);
			TextComponent name = Component.text(group.getFormattedName()).color(NamedTextColor.GREEN);
			TextComponent size = Component.text(String.valueOf(group.getTownBlocks().size())).color(NamedTextColor.YELLOW);
			TextComponent dash = Component.text(" - ").color(NamedTextColor.DARK_GRAY);
			TextComponent line = Component.text(Integer.toString(i + 1)).color(NamedTextColor.GOLD);
			line = line.append(dash).append(name).append(dash).append(size);
			
			if (TownyEconomyHandler.isActive() && group.getPrice() != -1)
				line = line.append(dash).append(Component.text("(" + Translation.of("towny_map_forsale") + ": " + TownyEconomyHandler.getFormattedBalance(group.getPrice()) + ")").color(NamedTextColor.BLUE));

			groupsFormatted[i % 10] = line;
		}
		Audience audience = Towny.getAdventure().sender(sender);
		sender.sendMessage(ChatTools.formatTitle(town.getName() + " " + Translation.of("plotgroup_plu")));
		sender.sendMessage(headerMsg);
		for (TextComponent textComponent : groupsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town plotgrouplist " + town.getName(), page, "", total);
		audience.sendMessage(pageFooter);
	}
	
	/**
	 * @param object - One receiving the message.
	 * @param message - Message being sent.
	 * 
	 * @deprecated Deprecated as of 0.96.2.13 use {@link #sendMsg(CommandSender, String)} instead.
	 */
	@Deprecated
	public static void sendMsg(Object object, String message) {
		sendMsg((CommandSender) object, message);
	}
}
