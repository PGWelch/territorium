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
package com.opendoorlogistics.territorium.optimiser.utils;

import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;

public class SolutionImprovementChecker {
	private final ImmutableSolution refSolution;
	public SolutionImprovementChecker(ImmutableSolution solution){
		// take deep copy
		refSolution = new MutableSolution(solution);
	}
	
	/**
	 * 
	 * @param newSolution
	 * @return True if new solution is definitely better
	 */
	public boolean isImproved(ImmutableSolution newSolution){
		// see if we've improved by more than the round-off limit
		Cost newCost = newSolution.getCost();
		if (!Cost.isApproxEqual(refSolution.getCost(), newCost)) {
			return newCost.compareTo(refSolution.getCost()) < 0;
		}
		return false;
		
	}
	
	public Cost getReferenceCost(){
		return refSolution.getCost();
	}
}
