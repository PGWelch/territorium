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
package com.opendoorlogistics.territorium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.Random;

import org.junit.Test;

import com.opendoorlogistics.territorium.examples.XYMinMaxQuantitiesHeterogeneousClusters;
import com.opendoorlogistics.territorium.optimiser.components.LocalSearch;
import com.opendoorlogistics.territorium.optimiser.components.RandomisedCentreSelector;
import com.opendoorlogistics.territorium.optimiser.components.RandomisedWeightBasedCustomerAssignment;
import com.opendoorlogistics.territorium.optimiser.components.Ruin;
import com.opendoorlogistics.territorium.optimiser.components.LocalSearch.LocalSearchConfig;
import com.opendoorlogistics.territorium.optimiser.components.LocalSearch.LocalSearchHeuristic;
import com.opendoorlogistics.territorium.optimiser.components.Ruin.RuinConfig;
import com.opendoorlogistics.territorium.optimiser.components.Ruin.RuinType;
import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.Customer2CustomerClosestNgbMatrixImpl;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.SearchComponentsTags;
import com.opendoorlogistics.territorium.optimiser.solver.SolutionBank;
import com.opendoorlogistics.territorium.optimiser.solver.SolutionBank.SolutionBankConfig;
import com.opendoorlogistics.territorium.problem.Problem;

public class TestHeuristics {
	private Random random = new Random(123);
	private Problem problem = buildProblem(random);

	private static Problem buildProblem(Random random) {
		return new XYMinMaxQuantitiesHeterogeneousClusters().setMaxCustomerQuantity(100).setMinCustomerQuantity(1).setNbCustomers(100)
				.setNbClusters(10).setCustomerQuantityDistributionRandPower(3).setTotalClusterCapacityMultiplier(1.2)
				.setTotalClusterMinQuantityMultiplier(0.8).build(random);
	}

	@Test
	public void testInitialConstructionOk() {

		double maxOk1stSolQuantityViolation = 35;
		double maxOk1stSolTravel = 50;

		class Helper {
			void checkOk(String type, ImmutableSolution solution) {
				// Check first solution quality is OK
				System.out.println(type + ": " + solution.getCost());
				assertTrue(solution.getCost().getTravel() < maxOk1stSolTravel);
				assertTrue(solution.getCost().getQuantityViolation() < maxOk1stSolQuantityViolation);
			}
		}
		Helper helper = new Helper();

		helper.checkOk("Randomised construction", constructUsingRandomisedWeighted(random, problem));

		helper.checkOk("Local search construction", constructUsingLocalSearch(random, problem));

	}

	@Test
	public void testLocalSearchImproves() {
		for (LocalSearchHeuristic heuristicType : LocalSearchHeuristic.values()) {

			// Create initial solution just using randomised weighted as this gives some initial quantity
			ImmutableSolution initial = constructUsingRandomisedWeighted(random, problem);

			// Setup local search using just the heuristic
			LocalSearchConfig config = getSingleHeuristicConfig(heuristicType);
			MutableSolution localSearchSolution = new MutableSolution(problem, initial.getCustomersToClusters());

			// Run iterations until no improvement (should happen quickly)
			System.out.println("");
			System.out.println("Initial cost before calling " + heuristicType.name() + ": "
					+ initial.getCost().toSingleLineSummary());
			int step = 0;
			Comparator<Cost> stdComparator = Cost.createApproxEqualComparator();
			boolean improved = true;
			Cost firstZeroQuantityCost=null;
			while (improved) {
				// save the first cost where we hit zero quantity violation
				if(firstZeroQuantityCost==null && localSearchSolution.getCost().getQuantityViolation()==0){
					firstZeroQuantityCost=new Cost(localSearchSolution.getCost());
				}
				improved = createLocalSearch(config).runSingleStep(step, stdComparator, localSearchSolution);
				System.out.println("... after step " + (step + 1) + " of " + heuristicType.name() + ": "
						+ localSearchSolution.getCost().toSingleLineSummary());

				step++;
			}

			// compare starting and final cost
			Cost startingCost = initial.getCost();
			Cost finalCost = localSearchSolution.getCost();

			if (heuristicType == LocalSearchHeuristic.NONE) {
				assertTrue("None should give no improvement as nothing should have run",
						Cost.isApproxEqual(startingCost, finalCost));
			} else {
				assertTrue(!Cost.isApproxEqual(startingCost, finalCost));
				assertTrue(heuristicType.name() + ": Travel should have reduced", finalCost.getTravel() < 0.99 * firstZeroQuantityCost.getTravel());
				assertTrue(heuristicType.name() + ": Initial quantity violation should be non-zero",
						startingCost.getQuantityViolation() > 0.0001);
				assertEquals(heuristicType.name() + ": Final quantity violation should be 0 ", 0, finalCost.getQuantityViolation(), 0);
			}
		}

	}

	private LocalSearchConfig getSingleHeuristicConfig(LocalSearchHeuristic heuristicType) {
		LocalSearchConfig config = new LocalSearchConfig();
		config.setAllHeuristicsOff();
		config.set(heuristicType, true);
		return config;
	}

	private ImmutableSolution constructUsingLocalSearch(Random random, Problem problem) {
		return new LocalSearch(problem, new LocalSearchConfig(),
				new Customer2CustomerClosestNgbMatrixImpl(problem), random)
						.constructNewSolution(Cost.createApproxEqualComparator());
	}

	private MutableSolution constructUsingRandomisedWeighted(Random random, Problem problem) {
		int[] initialAssigned = new RandomisedWeightBasedCustomerAssignment(problem,
				new RandomisedWeightBasedCustomerAssignment.Config(), random)
						.run(new RandomisedCentreSelector(problem, random, new RandomisedCentreSelector.Config())
								.run(null, null), null);
		return new MutableSolution(problem, initialAssigned);
	}

	@Test
	public void testRuinRecreateImproves() {
		Comparator<Cost> stdComparator = Cost.createApproxEqualComparator();

		for (RuinType ruinType : RuinType.values()) {
			for (LocalSearchHeuristic ls : LocalSearchHeuristic.values()) {
				if (ls == LocalSearchHeuristic.NONE) {
					continue;
				}
				// Create initial solution just using randomised weighted as this gives some initial quantity
				// then run until the local search stagnates
				MutableSolution stagnated = constructUsingRandomisedWeighted(random, problem);
				runSingleLocalSearchTypeUntilStagnation(ls, stagnated);
				assertEquals("Quantity violation should be 0 by now", 0, stagnated.getCost().getQuantityViolation(), 0);

				// Now try 25 ruins checking we get a genuine improvement
				System.out.println("Trying ruin " + ruinType.name() + " with search type " + ls.name()
						+ " and stagnated solution " + stagnated.getCost().toSingleLineSummary());
				boolean improved = false;
				for (int i = 0; i < 1000 && !improved; i++) {
					String prefix = "..." + ruinType.name() + "_" + ls.name() + ", step " + i + ": ";

					int[] ruined = new Ruin(problem, new RuinConfig(), random).ruin(ruinType,
							stagnated.getCustomersToClusters());
					MutableSolution ruinedSol = new MutableSolution(problem, ruined);
					System.out.println(prefix + "after ruin, " + ruinedSol.getNbUnassignedCustomers()
							+ " unassigned customers, cost " + ruinedSol.getCost().toSingleLineSummary());

					// call construction first as we won't have stops loaded
					createLocalSearch(new LocalSearchConfig()).assignUnassignedCustomers(stdComparator, ruinedSol);
					System.out.println(prefix + "after reassigning, " + ruinedSol.getNbUnassignedCustomers()
							+ " unassigned customers, cost " + ruinedSol.getCost().toSingleLineSummary());

					runSingleLocalSearchTypeUntilStagnation(ls, ruinedSol);
					System.out
							.println(prefix + "after running until stagnation, " + ruinedSol.getNbUnassignedCustomers()
									+ " unassigned customers, cost " + ruinedSol.getCost().toSingleLineSummary());

					improved = ruinedSol.getCost().getTravel() < 0.99 * stagnated.getCost().getTravel();

				}

				assertTrue("Ruins should have improved stagnated solution", improved);
				System.out.println();
			}
		}

		// For each local search type run until stagnation,
		// then do some ruin/recreates and check we get better solutions
	}

	private void runSingleLocalSearchTypeUntilStagnation(LocalSearchHeuristic type, MutableSolution solution) {
		int step = 0;
		Comparator<Cost> stdComparator = Cost.createApproxEqualComparator();
		while (createLocalSearch(getSingleHeuristicConfig(type)).runSingleStep(step++, stdComparator, solution)) {
		}
		;
	}

	private LocalSearch createLocalSearch(LocalSearchConfig config) {
		return new LocalSearch(problem, config, new Customer2CustomerClosestNgbMatrixImpl(problem), random);
	}

	@Test
	public void testUsingTargetTravel() {

		SolutionBankConfig bankConfig = new SolutionBankConfig();
		bankConfig.setTravelTarget(true);
		bankConfig.setTravelTargetImprovementFraction(0.999999);

		// Create initial solution using local search
		Random myRandom = new Random(878756);
		int nbWinsBiComp = 0;
		int nbWinsSingleComp = 0;
		int nbDraws = 0;
		for (int test = 0; test < 100; test++) {
			boolean printDetails = test < 3;
			LocalSearch ls = new LocalSearch(problem, new LocalSearchConfig(),
					new Customer2CustomerClosestNgbMatrixImpl(problem), myRandom);
			Comparator<Cost> stdComp = Cost.createApproxEqualComparator();
			ImmutableSolution initialSol = ls.constructNewSolution(stdComp);

			// Run a single comparator standard comparator search until stagnation
			MutableSolution stagnatedSingleComp = new MutableSolution(problem, initialSol.getCustomersToClusters());
			int step = 0;
			while (ls.runSingleStep(step++, stdComp, stagnatedSingleComp)) {
				if (printDetails) {
					System.out.println("\tStep " + step + " with single comparator "
							+ stagnatedSingleComp.getCost().toSingleLineSummary());
				}
			}

			// Create the solution bank with the initial solution
			SolutionBank bank = new SolutionBank(bankConfig, problem, random);
			assertEquals(2, bank.getNbSolutionSlots());
			bank.accept(initialSol, new SearchComponentsTags());
			assertNotNull(bank.getSolutionSlot(0));
			assertNotNull(bank.getSolutionSlot(1));

			// Now run in parallel with both comparators - standard and target travel, using the solution bank
			int nbStepsBothNotImproved = 0;
			step = 0;
			while (nbStepsBothNotImproved < 3) {

				int nbImproves = 0;
				// for(int option=0 ; option<3 ; option++){
				// int isol = option<=1 ? option:1;
				// int icomp = option<=1 ? option:0;
				// MutableSolution sol = bank.getSolutionSlot(isol);
				// sol = new MutableSolution(problem, sol.getCustomersToClusters());
				// ls.runSingleStep(step, bank.getComparatorForSlot(icomp), sol);
				// nbImproves += bank.accept(sol, isol);
				// }
				for (int iSol = 0; iSol < bank.getNbSolutionSlots(); iSol++) {
					for (int jComp = 0; jComp < bank.getNbSolutionSlots(); jComp++) {
						MutableSolution sol = new MutableSolution(bank.getSolutionSlot(iSol));
						ls.runSingleStep(step, bank.getComparatorForSlot(jComp), sol);
						nbImproves += bank.accept(sol, new SearchComponentsTags());
					}

				}

				if (nbImproves == 0) {
					nbStepsBothNotImproved++;
				} else {
					nbStepsBothNotImproved = 0;
				}

				if (printDetails) {
					System.out.println("\tStep " + step + " (no improves=" + nbStepsBothNotImproved + "). "
							+ bank.toSingleLineSummary());
				}
				step++;
			}

			// check the solution bank for the case where we have less travel but higher quantity
			Cost cost0 = bank.getSolutionSlot(0).getCost();
			Cost cost1 = bank.getSolutionSlot(1).getCost();
			double targetTravel = cost0.getTravel() * bankConfig.getTravelTargetImprovementFraction();
			assertTrue("Finishing bicomp alt soln should have less travel but more quantity violation ",
					cost0.getQuantityViolation() == 0 && cost1.getQuantityViolation() > 0
							&& cost1.getTravel() < targetTravel);

			// check single vs bicomp
			int diff = stdComp.compare(bank.getSolutionSlot(0).getCost(), stagnatedSingleComp.getCost());
			if (diff < 0) {
				nbWinsBiComp++;
			} else if (diff == 0) {
				nbDraws++;
			} else {
				nbWinsSingleComp++;
			}

			System.out.println("Test " + test + " bicompWins=" + nbWinsBiComp + " singleCompWins=" + nbWinsSingleComp
					+ " draws=" + nbDraws + " bicomp0=" + bank.getSolutionSlot(0).getCost().toSingleLineSummary()
					+ " bicomp1=" + bank.getSolutionSlot(1).getCost().toSingleLineSummary() + ", singleComp="
					+ stagnatedSingleComp.getCost().toSingleLineSummary());

			if (printDetails) {
				System.out.println();
			}
		}
		assertTrue(nbWinsBiComp > 10);
		assertTrue("Bi comp should dominate", nbWinsSingleComp < 0.33 * nbWinsBiComp);
		assertTrue(nbDraws < 0.2 * nbWinsBiComp);
	}


}
