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
package com.opendoorlogistics.territorium.optimiser.solver;

import java.util.DoubleSummaryStatistics;
import java.util.Stack;

import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;

public interface SolverStateSummary {
	long getNbOuterSteps();
	Stack<String> getOperationsStack();
	DoubleSummaryStatistics getOuterStepTimingsInSeconds();
	
	/**
	 * This can be null if we're still constructing the first solution
	 * @return
	 */
	ImmutableSolution getBestSolution();
	
	/**
	 * Can be null
	 * @return
	 */
	SearchComponentsTags getBestSolutionTags();
	
	long getBestSolutionNb();
}

