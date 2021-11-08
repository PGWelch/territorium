package com.opendoorlogistics.territorium;

import java.util.Random;

import com.opendoorlogistics.territorium.optimiser.components.LocalSearch;
import com.opendoorlogistics.territorium.optimiser.components.RandomisedCentreSelector;
import com.opendoorlogistics.territorium.optimiser.components.RandomisedWeightBasedCustomerAssignment;
import com.opendoorlogistics.territorium.optimiser.components.LocalSearch.LocalSearchConfig;
import com.opendoorlogistics.territorium.optimiser.components.LocalSearch.LocalSearchHeuristic;
import com.opendoorlogistics.territorium.optimiser.data.Customer2CustomerClosestNgbMatrixImpl;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.problem.Problem;

public class TestUtils {
	static LocalSearchConfig getSingleHeuristicConfig(LocalSearchHeuristic heuristicType) {
		LocalSearchConfig config = new LocalSearchConfig();
		config.setAllHeuristicsOff();
		config.set(heuristicType, true);
		return config;
	}

	static LocalSearch createLocalSearch(Problem problem,LocalSearchConfig config, Random random) {
		return new LocalSearch(problem, config, new Customer2CustomerClosestNgbMatrixImpl(problem), random);
	}

	static MutableSolution constructUsingRandomisedWeighted(Random random, Problem problem) {
		int[] initialAssigned = new RandomisedWeightBasedCustomerAssignment(problem,
				new RandomisedWeightBasedCustomerAssignment.Config(), random)
						.run(new RandomisedCentreSelector(problem, random, new RandomisedCentreSelector.Config())
								.run(null, null), null);
		return new MutableSolution(problem, initialAssigned);
	}
}
