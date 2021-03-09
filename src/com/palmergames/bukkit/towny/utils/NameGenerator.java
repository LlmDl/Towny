package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class NameGenerator {

	static List<String> warFIRST = new ArrayList<>(Arrays.asList("Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
			"Red", "Yellow", "Blue", "Green", "Purple", "Orange", "Spring", "Summer", "Autumn", "Winter", "Hidden", "Striking", "Shadowed", "Bold", "Beleaguered",
			"Compassionate", "Strident"));
	static List<String> warSECOND = new ArrayList<>(Arrays.asList("Snakes", "Dragons", "Mountains", "Hillsides", "Sisters", "Brothers", "Sons", "Daughters",
			"Aggressors", "Lions", "Elephants", "Barrows", "Poets", "Bears", "Nations", "Labourers", "Upsetters"));


	public static String getRandomWarName() {
		String string1 = warFIRST.get(new Random().nextInt(warFIRST.size() - 1));		
		String string2 = warSECOND.get(new Random().nextInt(warSECOND.size() - 1));
		return string1 + " " + string2;
	}
}
