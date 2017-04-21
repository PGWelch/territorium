package com.opendoorlogistics.territorium;

import java.awt.Color;
import java.util.Random;

public class Utils {
	
	public static Color randomColour(Random random) {
		while (true) {
			int red = random.nextBoolean() ? random.nextInt(255) : 0;
			int blue = random.nextBoolean() ? random.nextInt(255) : 0;
			int green = random.nextBoolean() ? random.nextInt(255) : 0;
			int sum = red + blue + green;
			//int diff = Math.abs(red-blue) +Math.abs(red-green) + Math.abs(blue-green);  
			if (sum > 75 && sum < (240 * 3) ) {
				return new Color(red, blue, green);
			}
		}
		// Color col = new Color(0.1f + 0.8f * random.nextFloat(), 0.1f + 0.8f * random.nextFloat(), 0.1f + 0.8f * random.nextFloat());
		// return col;
	}

}
