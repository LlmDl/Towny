package com.palmergames.bukkit.towny.event.actions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.TownBlock;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * TownyExplosionDamagesEntityEvents are thrown when an explosion 
 * occurs in a Towny world, causing damage to an entity.
 * 
 * <br> - When an entity explosion, block explosion or lightning damages an entity.
 * <br> - When an explosion would damage a Hanging entity.
 * <br> - When a pig is zapped by lightning.
 * <br> - When an explosion would damage a Vehicle.
 * 
 * @param location - Location of the entity being damaged.
 * @param entity - Entity getting exploded.
 * @param cause - DamageCause.
 * @param cancelled - Whether Towny will cancel this already.
 * 
 * @author LlmDl
 */
public class TownyExplosionDamagesEntityEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Location location;
	private final Entity entity;
	private final DamageCause cause;
	private final TownBlock townblock;
	private boolean cancelled;

	/**
	 * Event thrown when an explosion damages an entity. 
	 * Use ignorecancelled = true in order to filter out explosions Towny will already have stopped.
	 * 
	 * @param location - Location of the entity being damaged.
	 * @param harmedEntity - Entity getting exploded.
	 * @param cause - DamageCause.
	 * @param townblock - TownBlock or null if in the wilderness.
	 * @param cancelled - Whether Towny will cancel this already.
	 */
	public TownyExplosionDamagesEntityEvent(Location location, Entity harmedEntity, DamageCause cause, TownBlock townblock, boolean cancelled) {
		this.location = location;
		this.entity = harmedEntity;
		this.cause = cause;
		this.townblock = townblock;
		this.cancelled = cancelled;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	/**
	 * @return location of the harmed entity.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * @return entity which will be damaged by the explosion.
	 */
	public Entity getEntity() {
		return entity;
	}
	
	/**
	 * The {@link com.palmergames.bukkit.towny.object.TownBlock} this explosion occured in,
	 * or null if in the wilderness.
	 * @return TownBlock or null. 
	 */
	@Nullable
	public TownBlock getTownBlock() {
		return townblock;
	}

	/**
	 * Did this action occur in the wilderness?
	 * 
	 * @return return true if this was in the wilderness.
	 */
	public boolean isInWilderness() {
		return townblock == null;
	}
	
	/**
	 * Did this action occur inside of a town's townblock?
	 * 
	 * @return true if this has a townblock.
	 */
	public boolean hasTownBlock() {
		return townblock != null;
	}
	
	/**
	 * @return cause of the explosion.
	 */
	public DamageCause getCause() {
		return cause;
	}
}
