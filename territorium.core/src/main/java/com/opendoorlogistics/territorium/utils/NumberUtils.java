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
package com.opendoorlogistics.territorium.utils;

import java.util.Random;

import gnu.trove.list.array.TIntArrayList;

public class NumberUtils {
	public static boolean numbersAreApproxEqual(double a, double b , double fractionalTolerance, double absoluteValueTolerance){
		if(Double.isNaN(a)){
			return Double.isNaN(b);
		}
		
		if(Double.isNaN(b)){
			return false;
		}
		
		if(Math.abs(a)<absoluteValueTolerance && Math.abs(b)<absoluteValueTolerance){
			return true;
		}
		return numbersAreApproxEqual(a, b, fractionalTolerance);
	}
	
	public static boolean numbersAreApproxEqual(double a, double b , double fractionalTolerance){
		double absDiff = Math.abs(a-b);
		
		if(a == 0 || b==0){
			// can't make a relative comparison.. make an absolute one
			return absDiff < fractionalTolerance;
		}
		
		if( absDiff > Math.abs(a) * fractionalTolerance){
			return false;
		}
		
		if( absDiff > Math.abs(b) * fractionalTolerance){
			return false;
		}
		return true;
	}
	
	public static boolean contains(int [] arr, int val){
		for(int i =0 ; i<arr.length ; i++){
			if(arr[i]==val){
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param random
	 * @param max Inclusive
	 * @return
	 */
	public static int [] getRandomOrder0ToNArray(Random random, int max){
		TIntArrayList list = new TIntArrayList(max+1);
		for(int i =0 ; i<=max ; i++){
			list.add(i);
		}
		list.shuffle(random);
		return list.toArray();
	}

}
