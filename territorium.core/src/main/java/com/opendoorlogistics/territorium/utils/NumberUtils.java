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
