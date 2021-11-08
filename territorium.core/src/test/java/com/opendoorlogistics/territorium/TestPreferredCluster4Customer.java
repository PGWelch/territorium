package com.opendoorlogistics.territorium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import org.junit.Test;

import com.opendoorlogistics.territorium.examples.XYMinMaxQuantitiesHeterogeneousClusters;
import com.opendoorlogistics.territorium.optimiser.components.LocalSearch.LocalSearchConfig;
import com.opendoorlogistics.territorium.optimiser.components.LocalSearch.LocalSearchHeuristic;
import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.Solver;
import com.opendoorlogistics.territorium.optimiser.solver.SolverConfig;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Cluster;

public class TestPreferredCluster4Customer {
	private static int getNbCustomersAssigned2PrefCluster(ImmutableSolution solution) {
		assertEquals(0, solution.getNbUnassignedCustomers());
		Problem problem = solution.getProblem();
		int ret = 0;
		int nbCustomers = problem.getCustomers().size();
		for (int customerIndex = 0; customerIndex < nbCustomers; customerIndex++) {
			Customer customer = problem.getCustomers().get(customerIndex);
			int cluster = solution.getClusterIndex(customerIndex);
			if (cluster == customer.getPreferredClusterIndex()) {
				ret++;
			}
		}
		return ret;
	}

	@Test
	public void test() {
		XYMinMaxQuantitiesHeterogeneousClusters builder = new XYMinMaxQuantitiesHeterogeneousClusters();
		Random random = new Random(123);
		int nbClusters = 5;
		int nbCustomers = 50;
		Problem problem = builder.setMinCustomerQuantity(1).setMaxCustomerQuantity(10)
				.setNbCustomers(nbCustomers).setNbClusters(nbClusters).build(random);

		for (Customer customer : problem.getCustomers()) {
			customer.setPreferredClusterIndex(random.nextInt(nbClusters));
			assertEquals(1, customer.getTravelCostMultiplier4PreferredClusterIndex(), 0.0);
		}

		double[] prefMultiplierVals = new double[] { 10, 1, 0.1 };

		// test heuristics independently
		for (LocalSearchHeuristic heuristicType : LocalSearchHeuristic.values()) {
			if (heuristicType == LocalSearchHeuristic.NONE) {
				continue;
			}

			ArrayList<Integer> nbCustomersAssigned2Pref = new ArrayList<Integer>();
			for (double prefMultiplier : prefMultiplierVals) {
				for (Customer customer : problem.getCustomers()) {
					customer.setTravelCostMultiplier4PreferredClusterIndex(prefMultiplier);
				}

				// Create initial solution just using randomised weighted as this gives some initial quantity
				ImmutableSolution initial = TestUtils.constructUsingRandomisedWeighted(random, problem);

				// Setup local search using just the heuristic
				LocalSearchConfig config = TestUtils.getSingleHeuristicConfig(heuristicType);
				MutableSolution localSearchSolution = new MutableSolution(problem,
						initial.getCustomersToClusters());

				// Run iterations until no improvement (should happen quickly)
				System.out.println(heuristicType.name() + " prefMult=" + prefMultiplier);
				System.out.println("\tInitial cost before calling " + heuristicType.name() + ": "
						+ initial.getCost().toSingleLineSummary() + " #custAssigned2Pref="
						+ getNbCustomersAssigned2PrefCluster(initial) + "/" + nbCustomers);
				int step = 0;
				Comparator<Cost> stdComparator = Cost.createApproxEqualComparator();
				boolean improved = true;
				while (improved) {
					improved = TestUtils.createLocalSearch(problem, config, random).runSingleStep(step,
							stdComparator, localSearchSolution);
					int nbAssigned2Pref = getNbCustomersAssigned2PrefCluster(localSearchSolution);
					System.out.println("\t... after step " + (step + 1) + " of " + heuristicType.name() + ": "
							+ localSearchSolution.getCost().toSingleLineSummary() + " #custAssigned2Pref="
							+ nbAssigned2Pref + "/" + nbCustomers);

					step++;
				}

				assertEquals(0, localSearchSolution.getNbUnassignedCustomers());
				nbCustomersAssigned2Pref.add(getNbCustomersAssigned2PrefCluster(localSearchSolution));
			}

			assertAssigned2PrefFractionsOK(nbCustomers, nbCustomersAssigned2Pref);
			System.out.println();
		}

		// test whole solver running
		{
			System.out.println("Running whole solver");
			ArrayList<Integer> nbCustomersAssigned2Pref = new ArrayList<Integer>();
			for (double prefMultiplier : prefMultiplierVals) {
				for (Customer customer : problem.getCustomers()) {
					customer.setTravelCostMultiplier4PreferredClusterIndex(prefMultiplier);
				}
				ImmutableSolution sol = runGetSol(random, problem);
				assertEquals(0, sol.getNbUnassignedCustomers());

				nbCustomersAssigned2Pref.add(getNbCustomersAssigned2PrefCluster(sol));

			}
			assertAssigned2PrefFractionsOK(nbCustomers, nbCustomersAssigned2Pref);
			System.out.println();
		}

//		// test with larger cluster quantity range all get assigned
//		{
//			for (Customer customer : problem.getCustomers()) {
//				customer.setTravelCostMultiplier4PreferredClusterIndex(0);
//			}	
//			ArrayList<Integer> nbCustomersAssigned2Pref = new ArrayList<Integer>();
//			double minQuant = problem.getClusters().get(0).getMinQuantity();
//			double maxQuant = problem.getClusters().get(0).getMaxQuantity();
//			double[] quantMults = new double[] {1,2.5,5,10,10000000};
//			for(double quantMult:quantMults) {
//				for(Cluster cluster:problem.getClusters()) {
//					cluster.setMinQuantity(0);
//					cluster.setMaxQuantity(10000000);
//				}	
//				
//				ImmutableSolution sol = runGetSol(random, problem);
//				assertEquals(0, sol.getNbUnassignedCustomers());
//				int nbAssigned=getNbCustomersAssigned2PrefCluster(sol);
//				nbCustomersAssigned2Pref.add(nbAssigned);
//				System.out.println("quantMult=" + quantMult + " nbAssigned="+nbAssigned);
//			}
//
//		}
	}

	private ImmutableSolution runGetSol(Random random, Problem problem) {
		SolverConfig solverConfig = new SolverConfig();
		solverConfig.setNbOuterSteps(25);

		// check initially only the non-fixed clusters with the better locations get customers
		Solver solver = new Solver(problem, solverConfig, null, random);
		ImmutableSolution sol = solver.solve(null);
		return sol;
	}

	private void assertAssigned2PrefFractionsOK(int nbCustomers,
			ArrayList<Integer> nbCustomersAssigned2Pref) {
		assertEquals(3, nbCustomersAssigned2Pref.size());
		String msg = "nbCustomersAssigned2Pref=" + nbCustomersAssigned2Pref;
		System.out.println(msg);
		assertTrue(msg, nbCustomersAssigned2Pref.get(0) < 0.5 * nbCustomersAssigned2Pref.get(1));
		assertTrue(msg, nbCustomersAssigned2Pref.get(0) < 0.1 * nbCustomersAssigned2Pref.get(2));
		assertTrue(msg, nbCustomersAssigned2Pref.get(1) < 0.45 * nbCustomersAssigned2Pref.get(2));
		assertTrue(msg, nbCustomersAssigned2Pref.get(0) < 0.1 * nbCustomers);
		assertTrue(msg, nbCustomersAssigned2Pref.get(2) > 0.7 * nbCustomers);
	}
}
