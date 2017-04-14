/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium.optimiser.data;

import java.text.DecimalFormat;
import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opendoorlogistics.territorium.problem.ObjectWithJSONToString;
import com.opendoorlogistics.territorium.utils.NumberUtils;

public class Cost extends ObjectWithJSONToString implements Comparable<Cost> {
	private double travel;
	private double quantityViolation;
	
	public double getTravel() {
		return travel;
	}
	public void setTravel(double travel) {
		this.travel = travel;
	}
	public double getQuantityViolation() {
		return quantityViolation;
	}
	public void setQuantityViolation(double capacityViolation) {
		this.quantityViolation = capacityViolation;
	}
	
	public Cost(){
	}
	
	public Cost(Cost copyThis){
		this.travel = copyThis.getTravel();
		this.quantityViolation = copyThis.getQuantityViolation();
	}
	
	@Override
	public int compareTo(Cost o) {
		int diff = Double.compare(quantityViolation, o.quantityViolation);
		if(diff==0){
			diff = Double.compare(travel, o.travel);
		}
		return diff;
	}
	
	@JsonIgnore
	public void setZero(){
		travel = 0;
		quantityViolation = 0;
	}
	
	@JsonIgnore
	public void setMax(){
		quantityViolation = Double.MAX_VALUE;
		travel = Double.MAX_VALUE;
	}
	
	@JsonIgnore
	public boolean isMax(){
		return quantityViolation == Double.MAX_VALUE;
	}
	
	@JsonIgnore
	public void add(Cost cost){
		travel += cost.travel;
		quantityViolation += cost.quantityViolation;
	}
	
	@JsonIgnore
	public void subtract(Cost cost){
		travel -= cost.travel;
		quantityViolation -= cost.quantityViolation;	
	}
	
	@JsonIgnore
	public void set(Cost cost){
		travel = cost.travel;
		quantityViolation = cost.quantityViolation;
	}
	
	@JsonIgnore
	public void negate(){
		travel = -travel;
		quantityViolation = -quantityViolation;
	}
	
	public static int getLowestCostIndx(Cost [] costs){
		int indx=-1;
		for(int i =0 ; i < costs.length ; i++){
			if(indx==-1 || costs[i].compareTo(costs[indx])<0){
				indx=i;
			}
		}
		return indx;
	}

	public String toSingleLineSummary() {
		return "[" + toSingleLineSummaryNoBrackets()+ "]";
	}
	
	private static final DecimalFormat MAX_3_DP =new DecimalFormat("#.###");
	
	public String toSingleLineSummaryNoBrackets() {
		return "trv=" + MAX_3_DP.format(travel) + ", qv=" + MAX_3_DP.format(quantityViolation) ;
	}
	
	private final static double ROUNDOFF_FRACTION = 0.000000001;

	public static boolean isApproxEqual(Cost a, Cost b){
		return isApproxEqualCapacityViolation(a, b)
			&& isApproxEqualTravelCost(a, b);
	}
	
	public static boolean isApproxEqualTravelCost(Cost a, Cost b) {
		return numbersAreApproxEqual(a.getTravel(), b.getTravel());
	}
	
	public static boolean isApproxEqualCapacityViolation(Cost a, Cost b) {
		return numbersAreApproxEqual(a.getQuantityViolation(), b.getQuantityViolation());
	}
	
	public static boolean numbersAreApproxEqual(double a, double b){
		return NumberUtils.numbersAreApproxEqual(a, b, ROUNDOFF_FRACTION, ROUNDOFF_FRACTION);		
	}
	
	/**
	 * Comparator which returns 0 when 2 costs are approximately equal
	 * @return
	 */
	public static Comparator<Cost> createApproxEqualComparator(){
		return new Comparator<Cost>() {
			
			@Override
			public int compare(Cost o1, Cost o2) {
				if(!isApproxEqualCapacityViolation(o1, o2)){
					return o1.getQuantityViolation() < o2.getQuantityViolation() ?-1:+1;
				}
				
				if(!isApproxEqualTravelCost(o1, o2)){
					return o1.getTravel() < o2.getTravel() ?-1:+1;					
				}
				return 0;
			}
		};
	}
	
}
