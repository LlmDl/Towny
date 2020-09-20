package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InviteCommand extends BaseCommand implements CommandExecutor {

	private static final List<String> inviteTabCompletes = Arrays.asList(
		TownySettings.getAcceptCommand(),
		TownySettings.getDenyCommand()
	);
	
	private static Towny plugin;

	public InviteCommand(Towny instance) {
		plugin = instance;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args.length) {
			case 1:
				return NameUtil.filterByStart(inviteTabCompletes, args[0]);
			case 2:
				switch (args[0].toLowerCase()) {
					case "accept":
					case "deny":
						try {
							return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getResident(sender.getName()).getReceivedInvites()
								.stream()
								.map(Invite::getSender)
								.map(InviteSender::getName)
								.collect(Collectors.toList()), args[1]);
						} catch (TownyException ignored) {}
				}
		}
		
		return Collections.emptyList();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (sender instanceof Player) {
			if (plugin.isError()) {
				sender.sendMessage(Colors.Rose + "[Towny Error] Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			if (command.getName().equalsIgnoreCase("invite")) {
				parseInviteCommand(player, args);
			}
		} else
			// Console
			HelpMenu.INVITE_HELP.send(sender);
		
		return true;
	}

	private void parseInviteCommand(final Player player, String[] split) {
		// So they just type /invite , We should probably send them to the help menu. Done.
		if (split.length == 0 || split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase("?")) {
			HelpMenu.INVITE_HELP.send(player);
		} else if (split[0].equalsIgnoreCase("list")) {
			parseInviteList(player, split);
		} else if (split[0].equalsIgnoreCase(TownySettings.getAcceptCommand())) {
			parseAccept(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase(TownySettings.getDenyCommand())) {
			parseDeny(player, StringMgmt.remFirstArg(split));
		}

	}

	private static void parseInviteList(Player player, String[] split) {
		// Now we check the size of the player invites, if there is more than 10 invites (not possible), We only displayed the first 10.
		// /invite args[0] args[1}
		Resident resident;
		try {
			resident = TownyUniverse.getInstance().getDataSource().getResident(player);
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		String received = Translation.of("player_received_invites")
				.replace("%a", Integer.toString(resident.getReceivedInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident)));
		try {
			if (resident.getReceivedInvites().size() > 0) {
				int page = 1;
				if (split != null) {
					if (split.length >= 2) {
						try {
							page = Integer.parseInt(split[1]);
						} catch (NumberFormatException ignored) {
						}
					}
				}
				sendInviteList(player, resident.getReceivedInvites(), page, false);
				player.sendMessage(received);

			} else {
				throw new TownyException(Translation.of("msg_err_player_no_invites"));
			}
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

	}

	public static void parseDeny(Player player, String[] args) {
		Resident resident;
		Town town;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			resident = townyUniverse.getDataSource().getResident(player);
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		List<Invite> invites = resident.getReceivedInvites();

		if (invites.size() == 0) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_player_no_invites"));
			return;
		}
		if (args.length >= 1) {
			// We cut the first argument out of it so /invite *accept* args[1]
			// SO now args[0] is always the Town, we should check if the argument length is >= 1
			try {
				town = townyUniverse.getDataSource().getTown(args[0]);
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
				return;
			}
		} else {
			if (invites.size() == 1) { // Only 1 Invite.
				town = (Town) invites.get(0).getSender();
			} else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_player_has_multiple_invites"));
				parseInviteList(player, null);
				return;
			}
		}
		
		Invite toDecline = null;

		for (Invite invite : InviteHandler.getActiveInvites()) {
			if (invite.getSender().equals(town) && invite.getReceiver().equals(resident)) {
				toDecline = invite;
				break;
			}
		}
		if (toDecline != null) {
			try {
				InviteHandler.declineInvite(toDecline, false);
			} catch (InvalidObjectException e) {
				e.printStackTrace();
			}
		} else
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));			


	}

	public static void parseAccept(Player player, String[] args) {
		Resident resident;
		Town town;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			resident = townyUniverse.getDataSource().getResident(player);
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		List<Invite> invites = resident.getReceivedInvites();
		if (invites.size() == 0) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_player_no_invites"));
			return;
		}
		if (args.length >= 1) {
			try {
				town = townyUniverse.getDataSource().getTown(args[0]);
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
				return;
			}
		} else {
			if (invites.size() == 1) {
				town = (Town) invites.get(0).getSender();
			} else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_player_has_multiple_invites"));
				parseInviteList(player, null);
				return;
			}
		}
		// At this point I consider having a valid Town & a valid Player so a final check is ran:

		Invite toAccept = null;

		for (Invite invite : InviteHandler.getActiveInvites()) {
			if (invite.getSender().equals(town) && invite.getReceiver().equals(resident)) {
				toAccept = invite;
				break;
			}
		}

		if (toAccept != null) {
			try {
				InviteHandler.acceptInvite(toAccept);
			} catch (TownyException | InvalidObjectException e) {
				e.printStackTrace();
			}
		} else
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));


	}

	public static void sendInviteList(Player player, List<Invite> list, int page, boolean fromSender) {

		if (page < 0) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative"));
			return;
		} else if (page == 0) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
			return;
		}
		int total = (int) Math.ceil(((double) list.size()) / ((double) 10));
		if (page > total) {
			return;
		}
		List<String> invitesFormatted = new ArrayList<>();
		int iMax = page * 10;
		if ((page * 10) > list.size()) {
			iMax = list.size();
		}
		String object = null;
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Invite invite = list.get(i);
			String name = invite.getDirectSender().getName();
			
			// If it's from the sender, do it differently
			String output = null;
			if (fromSender) {
				output = Colors.Blue + invite.getReceiver().getName() + Colors.Gray + " - " + Colors.Green + name;
				if (invite.getSender() instanceof Town) { // If it's sent by a town to a resident
					object = Translation.of("player_sing");
				}
				if (invite.getSender() instanceof Nation) {
					if (invite.getReceiver() instanceof Town) {
						object = Translation.of("town_sing");
					}
					if (invite.getReceiver() instanceof Nation) {
						object = Translation.of("nation_sing");
					}
				}
			} else { // So it's not from the sender, then it's from the receiver so
				output = Colors.Blue + invite.getSender().getName() + Colors.Gray + " - " + Colors.Green + name;
				if (invite.getReceiver() instanceof Resident) {
					object = Translation.of("town_sing");
				}
				if (invite.getReceiver() instanceof Town || invite.getReceiver() instanceof Nation) {
					object = Translation.of("nation_sing");
				}
			}
			invitesFormatted.add(output);
		}

		player.sendMessage(ChatTools.formatList(Translation.of("invite_plu"),
				Colors.Blue + object + Colors.Gray + " - " + Colors.LightBlue + Translation.of("invite_sent_by"),
				invitesFormatted, TownySettings.getListPageMsg(page, total)
		));
	}
}
