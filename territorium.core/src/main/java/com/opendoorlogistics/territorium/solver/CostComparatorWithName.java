package com.opendoorlogistics.territorium.solver;

import java.util.Comparator;

import com.opendoorlogistics.territorium.solver.data.Cost;

public interface CostComparatorWithName extends Comparator<Cost>{
	/**
	 * Short code for the comparator. The standard comparator's short code is an empty string
	 * @return
	 */
	String shortCode();
}
