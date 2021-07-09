package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

/**
 * A class to hold and calculate coordinates in a grid according to the size
 * defined in
 * the static field size.
 * 
 * @author Shade
 */
public class Coord {

	private static final int cellSize = TownySettings.getTownBlockSize(); // This should never change during runtime.
	private final int x;
	private final int z;

	public Coord(int x, int z) {

		this.x = x;
		this.z = z;
	}

	public Coord(Coord coord) {

		this.x = coord.getX();
		this.z = coord.getZ();
	}

	public int getX() {

		return x;
	}

	public int getZ() {

		return z;
	}

	public Coord add(int xOffset, int zOffset) {

		return new Coord(getX() + xOffset, getZ() + zOffset);
	}

	@Override
	public int hashCode() {

		int result = 17;
		result = 27 * result + x;
		result = 27 * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == this)
			return true;
		if (!(obj instanceof Coord))
			return false;

		Coord o = (Coord) obj;
		return this.x == o.x && this.z == o.z;
	}

	/*
	 * OLD METHOD
	 * 
	 * public static Coord parseCoord(int x, int z) {
	 * return new Coord(x / getCellSize() - (x < 0 ? 1 : 0), z / getCellSize() -
	 * (z < 0 ? 1 : 0));
	 * }
	 */

	/**
	 * Convert a value to the grid cell counterpart
	 * @param value x/z integer
	 * @return cell position
	 */
	protected static int toCell(int value) {
		// Floor divides means that for negative values will round to the next negative value
		// and positive value to the previous positive value.
		return Math.floorDiv(value, getCellSize());
	}

	/**
	 * Convert regular grid coordinates to their grid cell's counterparts.
	 * 
	 * @param x - X int (Coordinates)
	 * @param z - Z int (Coordinates)
	 * @return a new instance of Coord.
	 * 
	 */
	public static Coord parseCoord(int x, int z) {
		return new Coord(toCell(x), toCell(z));
	}

	public static Coord parseCoord(Entity entity) {

		return parseCoord(entity.getLocation());
	}

	public static Coord parseCoord(Location loc) {

		return parseCoord(loc.getBlockX(), loc.getBlockZ());
	}

	public static Coord parseCoord(Block block) {

		return parseCoord(block.getX(), block.getZ());
	}

	@Override
	public String toString() {

		return getX() + ", " + getZ();
	}

	public static int getCellSize() {

		return cellSize;
	}
}
