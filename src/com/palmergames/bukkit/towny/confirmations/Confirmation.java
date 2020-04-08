package com.palmergames.bukkit.towny.confirmations;

/**
 * An object which stores information about confirmations.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public class Confirmation {
	
	private Runnable handler;
	private String title;
	
	/**
	 * Creates a new confirmation object.
	 *
	 * @param handler The handler to run after accepting the command.
	 */
	public Confirmation(Runnable handler) {
		this.setHandler(handler);
	}

	/**
	 * Creates a new confirmation object.
	 *
	 * @param handler The handler to run after accepting the command.
	 * @param title The title of the confirmation message.   
	 */
	public Confirmation(Runnable handler, String title) {
		this(handler);
		this.title = title;
	}

	/**
	 * Gets the handler that contains the code to run on
	 * completion.
	 * 
	 * @return The handler
	 */
	public Runnable getHandler() {
		return handler;
	}

	/**
	 * Sets the handler for when the confirmation is accepted.
	 * 
	 * @param handler The handler to run when the command is accepted.
	 */
	public void setHandler(Runnable handler) {
		this.handler = handler;
	}

	/**
	 * Gets the title of the confirmation message.
	 * 
	 * @return The title of the confirmation message.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title of the confirmation message.
	 * 
	 * @param title The title to change to.
	 */
	public void setTitle(String title) {
		this.title = title;
	}
}
