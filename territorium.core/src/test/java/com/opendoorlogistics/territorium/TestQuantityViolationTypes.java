package com.opendoorlogistics.territorium;

import java.util.DoubleSummaryStatistics;
import java.util.Random;

import org.junit.Test;

import com.opendoorlogistics.territorium.examples.XYMinMaxQuantitiesHeterogeneousClusters;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.Solver;
import com.opendoorlogistics.territorium.optimiser.solver.SolverConfig;
import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.Problem.QuantityViolationType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestQuantityViolationTypes {
	@Test
	public void test() {
		// build a problem where clusters have half the capacity they need
		Random random = new Random(123);
		DoubleSummaryStatistics rangeLinear = new DoubleSummaryStatistics();
		DoubleSummaryStatistics rangeSquare = new DoubleSummaryStatistics();
		for (int i = 0; i < 5; i++) {
			Problem problem = new XYMinMaxQuantitiesHeterogeneousClusters()
					.setMaxCustomerQuantity(1).setMinCustomerQuantity(1).setNbCustomers(50)
					.setNbClusters(10).setTotalClusterCapacityMultiplier(0.5)
					.setTotalClusterMinQuantityMultiplier(0.25).build(random);

			// check quantities are as expected
			double sumClusterCap = 0, sumCustomerQuant = 0;
			for (Cluster cluster : problem.getClusters()) {
				sumClusterCap += cluster.getMaxQuantity();
			}
			for (Customer customer : problem.getCustomers()) {
				sumCustomerQuant += customer.getQuantity();
			}
			assertEquals(sumCustomerQuant, 2 * sumClusterCap, sumCustomerQuant * 0.01);

			// all clusters have same max quantity
			double clusterMaxQuant = problem.getClusters().get(0).getMaxQuantity();
			
			problem.setQuantityViolationType(QuantityViolationType.LINEAR);
			DoubleSummaryStatistics linearStats = solveGetQuantityStats(random, problem);
			assertTrue(linearStats.getMin() >= clusterMaxQuant *0.999);
			System.out.println("Cluster quant stats 4 linear " + linearStats);
			rangeLinear.accept(linearStats.getMax() - linearStats.getMin());

			problem.setQuantityViolationType(QuantityViolationType.SQUARE);
			DoubleSummaryStatistics squareStats = solveGetQuantityStats(random, problem);
			System.out.println("Cluster quant stats 4 square " + squareStats);
			rangeSquare.accept(squareStats.getMax() - squareStats.getMin());
		}
		
		assertTrue(rangeLinear.getAverage() > 1);
		assertTrue(rangeSquare.getAverage() < rangeLinear.getAverage()*0.01);
			
	}

	/**
	 * @param random
	 * @param problem
	 * @return
	 */
	private DoubleSummaryStatistics solveGetQuantityStats(Random random, Problem problem) {
		SolverConfig config = new SolverConfig();
		config.setNbOuterSteps(25);
		Solver solver = new Solver(problem, config, null, random);
		ImmutableSolution solution = solver.solve(null);
		DoubleSummaryStatistics stats = getClusterQuantityStats(solution);
		return stats;
	}

	private static DoubleSummaryStatistics getClusterQuantityStats(ImmutableSolution sol) {
		DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
		int nc = sol.getProblem().getClusters().size();
		for (int i = 0; i < nc; i++) {
			statistics.accept(sol.getClusterQuantity(i));
		}
		return statistics;
	}
}
