package com.opendoorlogistics.territorium.solver;

import java.util.Stack;

public interface SolverStateSummary {
	long getNbOuterSteps();
	Stack<String> getOperationsStack();
	
	/**
	 * This can be null if we're still constructing the first solution
	 * @return
	 */
	ImmutableSolution getBestSolution();
	
	/**
	 * Can be null
	 * @return
	 */
	String getBestSolutionSource();
}
