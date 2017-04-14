package com.opendoorlogistics.territorium.optimiser.solver;

import java.util.Comparator;

import com.opendoorlogistics.territorium.optimiser.data.Cost;

public interface CostComparatorWithTags extends Comparator<Cost>{
	/**
	 * Short code for the comparator. The standard comparator's short code is an empty string
	 * @return
	 */
	SearchComponentsTags.TagType[] getTags();
}
