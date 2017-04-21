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

