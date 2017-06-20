/*******************************************************************************
 * Copyright 2014-2017 Open Door Logistics Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.opendoorlogistics.territorium;

import java.awt.Color;
import java.util.Random;
import java.util.TreeMap;

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

	static public final TreeMap<String, Color>PREDEFINED_COLOURS_BY_NUMBER_STRING = new TreeMap<>();
	static{
		// See http://sashat.me/2017/01/11/list-of-20-simple-distinct-colors/
		String [] scols = new String[]{
				"0x800000",
				"0xFF0000",
				"0xFFC9DE",
				"0xAA6E28",
				"0xFF9900",
				"0xFFD8B1",
				"0x808000",
				"0xFFEA00",
				"0xBEFF00",
				"0x00BE00",
				"0xAAFFC3",
				"0x008080",
				"0x64FFFF",
				"0x000080",
				"0x4385FF",
				"0x820096",
				"0xFF00FF",
				"0x808080",
		};
		for(int i=0;i<scols.length;i++){
			PREDEFINED_COLOURS_BY_NUMBER_STRING.put(Integer.toString(i), Color.decode(scols[i]));
		}
	}
}
