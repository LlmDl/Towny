package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Nation;

public class NationToggleOpenEvent extends NationToggleEvent {

	private final boolean state;
	
	public NationToggleOpenEvent(CommandSender sender, Nation nation, boolean admin) {
		super(sender, nation, admin);
		state = nation.isOpen();
	}
	
	/**
	 * @return the current toggle's state.
	 */
	public boolean getCurrentState() {
		return state;
	}
	
	/**
	 * @return the future state of the toggle after the event.
	 */
	public boolean getFutureState() {
		return !state;
	}
	
}
