package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Item lists as Strings. Useful for groups that are missing from the Spigot Tags.
 * 
 * Did not use Materials because then we would be limited to specific versions of MC as new items are added.
 * 
 * @author LlmDl
 */
public interface ItemLists {

	/**
	 * List of Axe items.
	 */
	public static List<String> AXES = new ArrayList<>(Arrays.asList("WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLD_AXE", "DIAMOND_AXE", "NETHERITE_AXE"));

	/**
	 * List of Dye items.
	 */
	public static List<String> DYES = new ArrayList<>(Arrays.asList("BLACK_DYE","BLUE_DYE","BROWN_DYE","CYAN_DYE","GRAY_DYE","GREEN_DYE","LIGHT_BLUE_DYE","LIGHT_GRAY_DYE","LIME_DYE","MAGENTA_DYE","ORANGE_DYE","PINK_DYE","PURPLE_DYE","RED_DYE","WHITE_DYE","YELLOW_DYE"));
	
	/**
	 * List of Redstone blocks that can be interacted with.
	 */
	public static List<String> REDSTONE_INTERACTABLES = new ArrayList<>(Arrays.asList("COMPARATOR","REPEATER","DAYLIGHT_DETECTOR","NOTE_BLOCK"));

	/**
	 * List of Potted Plants.
	 */
	public static List<String> POTTED_PLANTS = new ArrayList<>(Arrays.asList("POTTED_ACACIA_SAPLING","POTTED_ALLIUM","POTTED_AZURE_BLUET","POTTED_BAMBOO","POTTED_BIRCH_SAPLING","POTTED_BLUE_ORCHID","POTTED_BROWN_MUSHROOM","POTTED_CACTUS","POTTED_CORNFLOWER","POTTED_DANDELION","POTTED_DARK_OAK_SAPLING","POTTED_DEAD_BUSH","POTTED_FERN","POTTED_JUNGLE_SAPLING","POTTED_LILY_OF_THE_VALLEY","POTTED_OAK_SAPLING","POTTED_ORANGE_TULIP","POTTED_OXEYE_DAISY","POTTED_PINK_TULIP","POTTED_POPPY","POTTED_RED_MUSHROOM","POTTED_RED_TULIP","POTTED_SPRUCE_SAPLING","POTTED_WHITE_TULIP","POTTED_WITHER_ROSE"));

	/**
	 * List of Boats.
	 */
	public static List<String> BOATS = new ArrayList<>(Arrays.asList("BIRCH_BOAT","ACACIA_BOAT","DARK_OAK_BOAT","JUNGLE_BOAT","OAK_BOAT","SPRUCE_BOAT"));
	
	/**
	 * List of Minecarts.
	 */
	public static List<String> MINECARTS = new ArrayList<>(Arrays.asList("MINECART","STORAGE_MINECART","POWERED_MINECART","COMMAND_MINECART","TNT_MINECART","HOPPER_MINECART"));
 	
	/**
	 * List of Wooden Doors.
	 */
	public static List<String> WOOD_DOORS = new ArrayList<>(Arrays.asList("ACACIA_DOOR","BIRCH_DOOR","DARK_OAK_DOOR","JUNGLE_DOOR","OAK_DOOR","SPRUCE_DOOR","CRIMSON_DOOR","WARPED_DOOR"));

	/**
	 * List of Fence Gates.
	 */
	public static List<String> FENCE_GATES = new ArrayList<>(Arrays.asList("ACACIA_FENCE_GATE","BIRCH_FENCE_GATE","DARK_OAK_FENCE_GATE","OAK_FENCE_GATE","JUNGLE_FENCE_GATE","SPRUCE_FENCE_GATE","CRIMSON_FENCE_GATE","WARPED_FENCE_GATE"));

	/**
	 * List of Trap Doors.
	 */
	public static List<String> TRAPDOORS = new ArrayList<>(Arrays.asList("ACACIA_TRAPDOOR","BIRCH_TRAPDOOR","DARK_OAK_TRAPDOOR","JUNGLE_TRAPDOOR","OAK_TRAPDOOR","SPRUCE_TRAPDOOR","CRIMSON_TRAPDOOR","WARPED_TRAPDOOR"));

	/**
	 * List of Shulker Boxes.
	 */
	public static List<String> SHULKER_BOXES = new ArrayList<>(Arrays.asList("SHULKER_BOX","WHITE_SHULKER_BOX","ORANGE_SHULKER_BOX","MAGENTA_SHULKER_BOX","LIGHT_BLUE_SHULKER_BOX","LIGHT_GRAY_SHULKER_BOX","YELLOW_SHULKER_BOX","LIME_SHULKER_BOX","PINK_SHULKER_BOX","GRAY_SHULKER_BOX","CYAN_SHULKER_BOX","PURPLE_SHULKER_BOX","BLUE_SHULKER_BOX","BROWN_SHULKER_BOX","GREEN_SHULKER_BOX","RED_SHULKER_BOX","BLACK_SHULKER_BOX"));

	/**
	 * List of Pressure Plates.
	 */
	public static List<String> PRESSURE_PLATES = new ArrayList<>(Arrays.asList("STONE_PRESSURE_PLATE","ACACIA_PRESSURE_PLATE","BIRCH_PRESSURE_PLATE","DARK_OAK_PRESSURE_PLATE","JUNGLE_PRESSURE_PLATE","OAK_PRESSURE_PLATE","SPRUCE_PRESSURE_PLATE","HEAVY_WEIGHTED_PRESSURE_PLATE","LIGHT_WEIGHTED_PRESSURE_PLATE","CRIMSON_PRESSURE_PLATE","WARPED_PRESSURE_PLATE","POLISHED_BLACKSTONE_PRESSURE_PLATE"));

	/**
	 * List of Buttons.
	 */
	public static List<String> BUTTONS = new ArrayList<>(Arrays.asList("STONE_BUTTON","ACACIA_BUTTON","BIRCH_BUTTON","DARK_OAK_BUTTON","JUNGLE_BUTTON","OAK_BUTTON","SPRUCE_BUTTON","CRIMSON_BUTTON","WARPED_BUTTON","POLISHED_BLACKSTONE_BUTTON"));
	
	/**
	 * Config-useable material groups.
	 */
	public static List<String> GROUPS = new ArrayList<>(Arrays.asList("BOATS","MINECARTS","WOOD_DOORS","PRESSURE_PLATES","FENCE_GATES","TRAPDOORS","SHULKER_BOXES","BUTTONS"));
	
	/**
	 * Returns a pre-configured list from the GROUPS.
	 * 
	 * @param groupName - String value of one of the {@link ItemLists#GROUPS}
	 * @return - List<String> grouping of materials.
	 */
	@Nullable
	public static List<String> getGrouping(String groupName) {
		switch(groupName) {
		case "BOATS":
			return BOATS;			
		case "MINECARTS":
			return MINECARTS;
		case "WOOD_DOORS":
			return WOOD_DOORS;
		case "PRESSURE_PLATES":
			return PRESSURE_PLATES;
		case "FENCE_GATES":
			return FENCE_GATES;
		case "TRAPDOORS":
			return TRAPDOORS;
		case "SHULKER_BOXES":
			return SHULKER_BOXES;
		case "BUTTONS":
			return BUTTONS;
		
		}
		return null;
	}
}
