package com.opendoorlogistics.territorium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.opendoorlogistics.territorium.SVGWriter.XYCoordTransformer;
import com.opendoorlogistics.territorium.examples.XYMinMaxQuantitiesHeterogeneousClusters;
import com.opendoorlogistics.territorium.problem.data.Customer;
import com.opendoorlogistics.territorium.problem.data.Location;
import com.opendoorlogistics.territorium.problem.data.Problem;
import com.opendoorlogistics.territorium.problem.data.XYLocation;
import com.opendoorlogistics.territorium.solver.ImmutableSolution;
import com.opendoorlogistics.territorium.solver.LocalSearch;
import com.opendoorlogistics.territorium.solver.MutableSolution;
import com.opendoorlogistics.territorium.solver.ProblemSplitter;
import com.opendoorlogistics.territorium.solver.SolutionBank;
import com.opendoorlogistics.territorium.solver.SolutionImprovementChecker;
import com.opendoorlogistics.territorium.solver.Solver;
import com.opendoorlogistics.territorium.solver.SolverConfig;
import com.opendoorlogistics.territorium.solver.LocalSearch.LocalSearchConfig;
import com.opendoorlogistics.territorium.solver.ProblemSplitter.ProblemSplitterConfig;
import com.opendoorlogistics.territorium.solver.ProblemSplitter.Subproblem;
import com.opendoorlogistics.territorium.solver.data.Cost;
import com.opendoorlogistics.territorium.solver.data.Customer2CustomerClosestNgbMatrix;
import com.opendoorlogistics.territorium.solver.data.Customer2CustomerClosestNgbMatrixImpl;
import com.opendoorlogistics.territorium.utils.Pair;
import com.opendoorlogistics.territorium.utils.StringUtils;

public class TestProblemSplitting {
	private static Problem buildProblem(Random random) {
		return new XYMinMaxQuantitiesHeterogeneousClusters().setMaxQuantity(100).setMinQuantity(1).setNbCustomers(100)
				.setNbClusters(10).setQuantityDistributionRandPower(3).setTotalClusterCapacityMultiplier(1.2)
				.setTotalClusterMinQuantityMultiplier(0.8).build(random);
	}

	@Test
	public void testProblemSplittingWithoutOptimising() {
		Random random = new Random(123);
		Problem problem = buildProblem(random);
		class Helper {
			double meanTravelTimeBetweenClusterCentres(Problem problem, ImmutableSolution solution) {
				DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
				Location[] centres = solution.getClusterCentres();
				for (int i = 0; i < centres.length; i++) {
					for (int j = 0; j < centres.length; j++) {
						if (i != j) {
							stats.accept(problem.getTravelMatrix().get(centres[i].getIndex(), centres[j].getIndex())
									.getTime());
						}
					}
				}
				return stats.getAverage();
			}
		}
		Helper helper = new Helper();

		ProblemSplitter splitter = new ProblemSplitter(problem, random, new ProblemSplitterConfig());
		Customer2CustomerClosestNgbMatrix customer2CustomerClosestNgbMatrix = new Customer2CustomerClosestNgbMatrixImpl(
				problem);
		LocalSearch ls = new LocalSearch(problem, new LocalSearchConfig(), customer2CustomerClosestNgbMatrix,
				random);
		ImmutableSolution initialSolution = ls.constructNewSolution(Cost.createApproxEqualComparator());
		// while(ls.runSingleStep(0, Cost.createApproxEqualComparator(), solution));
		assertTrue(initialSolution.getCost().getTravel() > 0);

		int svgLen = 350;
		SVGWriter svgWriter = new SVGWriter(svgLen);
		svgWriter.addHTMLHeader();
		svgWriter.addHeader("Original problem", 1);
		svgWriter.addXYProblemSVG(problem, initialSolution.getClusterCentres(),
				initialSolution.getCustomersToClusters());

		// Split a problem.
		svgWriter.addHeader("Subproblems", 1);
		for (int nbSubProblems = 2; nbSubProblems <= 5; nbSubProblems++) {
			List<Subproblem> subproblems = splitter.splitIntoSubproblems(random,
					initialSolution.getCustomersToClusters(), nbSubProblems, customer2CustomerClosestNgbMatrix);
			assertEquals(nbSubProblems, subproblems.size());

			Cost sumCost = new Cost();
			sumCost.setZero();
			int sumCustomers = 0;
			int sumCluster = 0;
			int subproblemCount = 0;
			DoubleSummaryStatistics meanMeanTravelTimes = new DoubleSummaryStatistics();
			for (Subproblem subproblem : subproblems) {
				int nbClusters = subproblem.getProblem().getClusters().size();
				assertTrue(nbClusters >= 2);
				sumCluster += nbClusters;

				int nbCustomers = subproblem.getProblem().getCustomers().size();
				assertTrue(nbCustomers > 0);
				sumCustomers += nbCustomers;

				assertEquals(0, subproblem.getSolution().getNbUnassignedCustomers());
				sumCost.add(subproblem.getSolution().getCost());

				double meanTravelTime = helper.meanTravelTimeBetweenClusterCentres(subproblem.getProblem(),
						subproblem.getSolution());
				meanMeanTravelTimes.accept(meanTravelTime);
				System.out.println("Subproblem " + (subproblemCount + 1) + "/" + nbSubProblems + ", clusters="
						+ nbClusters + ", customers=" + nbCustomers + ", cost="
						+ subproblem.getSolution().getCost().toSingleLineSummary() + ", meanTravelTime="
						+ meanTravelTime);
				subproblemCount++;
			}

			// add SVG for the split
			String[] colors = SVGWriter.createRandomColours(random, subproblems.size());
			svgWriter.addHeader("NbSubproblems=" + nbSubProblems, 2);
			svgWriter.addSVGHeader();
			XYCoordTransformer transformer = new XYCoordTransformer(svgLen, problem);
			for (int isub = 0; isub < subproblems.size(); isub++) {
				for (Customer customer : subproblems.get(isub).getProblem().getCustomers()) {
					XYLocation loc = (XYLocation) customer.getLocation();
					svgWriter.addLine(
							SVGWriter.circle(transformer.cx(loc.getX()), transformer.cy(loc.getY()), 4, colors[isub]));
				}
			}
			svgWriter.addSVGFooter();

			// Test that sum of split costs, customers and clusters and original are the same.
			assertEquals(problem.getCustomers().size(), sumCustomers);
			assertEquals(problem.getClusters().size(), sumCluster);
			assertTrue(Cost.createApproxEqualComparator().compare(initialSolution.getCost(), sumCost) == 0);

			// Get mean travel time for whole problem and check its lower for subproblems
			double wholeProblemMeanTravelTime = helper.meanTravelTimeBetweenClusterCentres(problem, initialSolution);
			String message = "Whole problem mean travel time between clusters is " + wholeProblemMeanTravelTime
					+ " but for each subproblem this averages as " + meanMeanTravelTimes.getAverage();
			System.out.println(message);
			double[] limits = new double[] { 0, 0, 0.85, 0.75, 0.7, 0.6 };
			assertTrue(message, meanMeanTravelTimes.getAverage() < limits[nbSubProblems] * wholeProblemMeanTravelTime);

			// export the SVG to clipboard...
			svgWriter.addHTMLFooter();
			StringUtils.setClipboard(svgWriter.getSVG());

		}


	}
	
	private static class OptimiserHelper {
		SolutionBank bank;
		Solver solver;

		OptimiserHelper(Problem problem, ImmutableSolution solution, Random random) {
			SolverConfig config = new SolverConfig();
			config.getSolutionBankConfig().setTravelTarget(false);
			bank = new SolutionBank(config.getSolutionBankConfig(), problem, random);
			solver = new Solver(problem, config, null, random);
			bank.accept(solution, null);
			assertEquals("Should only be using main solution", 1, bank.getNbSolutionSlots());
		}

		OptimiserHelper singleStep() {
			// SolutionImprovementChecker checker = new SolutionImprovementChecker(bank.getStandardSol());
			solver.runSingleOuterStep(bank);
			// return checker.isImproved(bank.getStandardSol());
			return this;
		}
		
		ImmutableSolution getSol(){
			return bank.getStandardSol();
		}
		
		Cost getCost(){
			return getSol().getCost();
		}
	}

//	/**
//	 * Test that splitting gives better solutions with using the same CPU time...
//	 */
//	@Test
//	public void testProblemSplittingGivesBetterEndSolutions() {
//		Random random = new Random(123);
//		
//		int nbTimesSplitBetter=0;
//		int nbTimesUnsplitBetter=0;
//		for (int iproblem = 0; iproblem < 100; iproblem++) {
//			Problem problem = buildProblem(random);
//
//			ProblemSplitter splitter = new ProblemSplitter(problem, random, new ProblemSplitterConfig());
//			Customer2CustomerClosestNgbMatrix customer2CustomerClosestNgbMatrix = new Customer2CustomerClosestNgbMatrixImpl(
//					problem);
//			LocalSearch ls = new LocalSearch(problem, new LocalSearchConfig(), null, new Customer2CustomerClosestNgbMatrixImpl(
//					problem),
//					random);
//			MutableSolution initialSolution = ls.constructNewSolution(Cost.createApproxEqualComparator());
//
//
//			OptimiserHelper nosplits = new OptimiserHelper(problem, initialSolution,random);
//			OptimiserHelper withsplits = new OptimiserHelper(problem, initialSolution,random);
//
//			int nbImprovesWithSplitsSolOnSplit=0;
//			int nbImprovesWithSplitsSolNotOnSplit = 0;
//			int nbImprovesNoSplitsSol=0;
//			for (int iteration = 0; iteration < 15; iteration++) {
//					
//				int nbSubProblems = 1 + iteration%5;
//				if(nbSubProblems==1){
//					SolutionImprovementChecker sip = new SolutionImprovementChecker(withsplits.getSol());
//					withsplits.singleStep();
//					if(sip.isImproved(withsplits.getSol())){
//						nbImprovesWithSplitsSolNotOnSplit++;
//					};			
//				}else{
//					SolutionImprovementChecker sip = new SolutionImprovementChecker(withsplits.getSol());
//					
//					List<Subproblem> subproblems = splitter.splitIntoSubproblems(random,
//							withsplits.getSol().getCustomersToClusters(), nbSubProblems,
//							customer2CustomerClosestNgbMatrix);
//					assertEquals(nbSubProblems, subproblems.size());
//					
//
//					// Run a single iteration ruin-recreate for each subproblem
//					List<Pair<Subproblem, MutableSolution>> subsolutions = new ArrayList<>();
//					subproblems.stream().forEach(s -> {
//						subsolutions.add(new Pair<ProblemSplitter.Subproblem, MutableSolution>(s,
//								new OptimiserHelper(s.getProblem(), s.getSolution(),random).singleStep().getSol()));
//					});
//
//					MutableSolution recombined = ProblemSplitter.combineSubProblemSolutions(problem, subsolutions);
//					if(sip.isImproved(recombined)){
//						nbImprovesWithSplitsSolOnSplit++;
//					}
//					withsplits.bank.accept(recombined, -1);					
//				}
//				
//				SolutionImprovementChecker sip = new SolutionImprovementChecker(nosplits.getSol());
//				nosplits.singleStep();
//				if(sip.isImproved(nosplits.getSol())){
//					nbImprovesNoSplitsSol++;
//				}
//
//
//			}
//			
//			int diff = Cost.createApproxEqualComparator().compare(withsplits.getCost(), nosplits.getCost());
//			if(diff<0){
//				nbTimesSplitBetter++;
//			}else if(diff>0){
//				nbTimesUnsplitBetter++;
//			}
//			
//			System.out.println("nbTimesSplitBetter=" +nbTimesSplitBetter + ", nbTimesUnsplitBetter="+nbTimesUnsplitBetter + 
//				", improves: "
//					+ " withSplitsSolOnSplit=" + nbImprovesWithSplitsSolOnSplit
//					+ ", withSplitsSolNotOnSplit=" + nbImprovesWithSplitsSolNotOnSplit
//					+ ", noSplitsSol=" + nbImprovesNoSplitsSol
//					);
//
//		}
//
//	}
	
	/**
	 * Test that during an optimisation, given a solution if we (a) run an unsplit ruin 
	 * and recreate and (b) run a split ruin and recreate, we get improvements more often with the split rr
	 */
	@Test
	public void testProblemSplittingWithOptimisingSplitGivesMoreImprovements() {
		Random random = new Random(123);
		DoubleSummaryStatistics unsplitImprovesStats = new DoubleSummaryStatistics();
		DoubleSummaryStatistics splitImprovesStats = new DoubleSummaryStatistics();
		for (int iproblem = 0; iproblem < 20; iproblem++) {
			Problem problem = buildProblem(random);

			ProblemSplitter splitter = new ProblemSplitter(problem, random, new ProblemSplitterConfig());
			Customer2CustomerClosestNgbMatrix customer2CustomerClosestNgbMatrix = new Customer2CustomerClosestNgbMatrixImpl(
					problem);
			LocalSearch ls = new LocalSearch(problem, new LocalSearchConfig(), new Customer2CustomerClosestNgbMatrixImpl(
					problem),
					random);
			ImmutableSolution initialSolution = ls.constructNewSolution(Cost.createApproxEqualComparator());


			OptimiserHelper main = new OptimiserHelper(problem, initialSolution,random);

			int unsplitImproves = 0;
			int splitImproves = 0;
			for (int iteration = 0; iteration < 15; iteration++) {

				// Record initial cost
				SolutionImprovementChecker checker = new SolutionImprovementChecker(main.getSol());

				// Run a single step for the main
				main.singleStep();

				// Randomly split into different subproblems
				int nbSubProblems = 2 + random.nextInt(4);
				List<Subproblem> subproblems = splitter.splitIntoSubproblems(random,
						main.getSol().getCustomersToClusters(), nbSubProblems,
						customer2CustomerClosestNgbMatrix);
				assertEquals(nbSubProblems, subproblems.size());

				// Run a single iteration ruin-recreate for each subproblem
				List<Pair<Subproblem, ImmutableSolution>> subsolutions = new ArrayList<>();
				subproblems.stream().forEach(s -> {
					subsolutions.add(new Pair<ProblemSplitter.Subproblem, ImmutableSolution>(s,
							new OptimiserHelper(s.getProblem(), s.getSolution(),random).singleStep().getSol()));
				});

				// Recombine the subproblems
				ImmutableSolution recombined = ProblemSplitter.combineSubProblemSolutions(problem, subsolutions);

				// Count who improved
				if (checker.isImproved(main.getSol())) {
					unsplitImproves++;
				}
				if (checker.isImproved(recombined)) {
					splitImproves++;
				}

				System.out.println(
						"" + (iproblem+1) + "-"
						+ (iteration + 1) + ". " + "UnsplitImproves=" + unsplitImproves
						+ ", SplitImproves=" + splitImproves + ", Starting cost="
						+ checker.getReferenceCost().toSingleLineSummary() + ", Unsplit after iteration="
						+ main.getCost().toSingleLineSummary() + ", Split after iteration="
						+ recombined.getCost().toSingleLineSummary());

				// Add the recombined solution too
				main.bank.accept(recombined,null);

			}

			splitImprovesStats.accept(splitImproves);
			unsplitImprovesStats.accept(unsplitImproves);
			// throw new UnsupportedOperationException("We're not testing anything here yet...");
		}
		
		System.out.println("Mean split improves =" + splitImprovesStats.getAverage());
		System.out.println("Mean unsplit improves =" + unsplitImprovesStats.getAverage());
		assertTrue("Less unsplit improves than split improves", unsplitImprovesStats.getAverage() < 0.75 * splitImprovesStats.getAverage());
	}


}
