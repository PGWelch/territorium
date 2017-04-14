package com.opendoorlogistics.territorium;

import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.Random;
import java.util.TreeMap;

import org.junit.Test;

import com.opendoorlogistics.territorium.examples.XYMinMaxQuantitiesHeterogeneousClusters;
import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.ContinueCallback;
import com.opendoorlogistics.territorium.optimiser.solver.SearchComponentsTags.TagType;
import com.opendoorlogistics.territorium.optimiser.solver.Solver;
import com.opendoorlogistics.territorium.optimiser.solver.SolverConfig;
import com.opendoorlogistics.territorium.optimiser.solver.SolverStateSummary;
import com.opendoorlogistics.territorium.problem.Problem;

public class TestSolver {
	private static Problem buildProblem(Random random) {
		return new XYMinMaxQuantitiesHeterogeneousClusters().setMaxQuantity(100).setMinQuantity(1).setNbCustomers(100)
				.setNbClusters(10).setQuantityDistributionRandPower(3).setTotalClusterCapacityMultiplier(1.2)
				.setTotalClusterMinQuantityMultiplier(0.8).build(random);
	}

	@Test
	public void testSolverComponentsGiveImprovements(){
		Random random = new Random(234);
		
		TreeMap<TagType, Integer> countByTag = new TreeMap<>();
		for(TagType tagType : TagType.values()){
			countByTag.put(tagType, 0);
		}

		for(int i =0 ; i< 10 ; i++){
			Problem problem = buildProblem(random);
			SolverConfig config = new SolverConfig();
			config.setNbOuterSteps(25);
			
			
			ImmutableSolution solution = new Solver(problem, config, new ContinueCallback() {
				long lastSolNb=-1;
				
				@Override
				public ContinueOption continueOptimisation(SolverStateSummary summary) {
					if(summary.getBestSolution()!=null && summary.getBestSolutionNb()!=lastSolNb){
						for(TagType tagType :summary.getBestSolutionTags().getTags()){
							countByTag.put(tagType, countByTag.get(tagType)+1);
						}
						lastSolNb = summary.getBestSolutionNb();
					}
					return ContinueOption.KEEP_GOING;
				}

			}, random).solve(null);
			
			System.out.println("Test " + i  +" Solution " + solution.getCost().toSingleLineSummary());
			
			assertTrue("Should be able to find 0 quantity violation",solution.getCost().getQuantityViolation()==0);
			assertTrue(solution.getCost().getTravel() < 24);
			assertTrue(solution.getCost().getTravel() >18);
		}
		
		for(TagType tagType : TagType.values()){
			int count = countByTag.get(tagType);
			System.out.println("TagType " + tagType.getCode() +"  contributed towards an improving solution " + count + " times");
			assertTrue(count>0);
		}
	}
	
	@Test
	public void testRestartFromExisting() {
		Random random = new Random(234);
		Problem problem = buildProblem(random);

		SolverConfig config = new SolverConfig();
		config.setNbOuterSteps(25);
		TreeMap<Long,Cost> costByStep = new TreeMap<>();
		Solver solver = new Solver(problem, config, new ContinueCallback() {

			@Override
			public ContinueOption continueOptimisation(SolverStateSummary summary) {
				System.out.println(summary.getNbOuterSteps() + ". "
						+ (summary.getBestSolution() != null ? summary.getBestSolution().getCost().toSingleLineSummary() : ""));
				
				if(summary.getBestSolution()!=null){
					costByStep.put(summary.getNbOuterSteps(),new Cost(summary.getBestSolution().getCost()));					
				}
				return ContinueOption.KEEP_GOING;
			}

		}, random);
		
		// solve with no starting solution
		ImmutableSolution solution = solver.solve(null);
		TreeMap<Long,Cost> firstRunCostByStep = new TreeMap<>(costByStep);
		costByStep.clear();

		// solve with a starting solution
		System.out.println(solution.getCost().toSingleLineSummary());
		solver.solve(solution.getCustomersToClusters());
		TreeMap<Long,Cost> secondRunCostByStep = new TreeMap<>(costByStep);
		costByStep.clear();
		
		// get the last cost of the first run
		Cost lastCostFirstRun = firstRunCostByStep.get((long)(firstRunCostByStep.size()-1));
		long firstEncounteredBestSol=-1;
		Comparator<Cost> approxComparator=Cost.createApproxEqualComparator();
		for(long step : firstRunCostByStep.keySet()){
			if(approxComparator.compare(firstRunCostByStep.get(step), lastCostFirstRun)<=0){
				firstEncounteredBestSol = step;
				break;
			}
		}
		assertTrue("Should take a few iterations to find the best sol on the first run", firstEncounteredBestSol>=5);

		assertTrue("On the second run we should be as good as, or better than, the best cost straight away",approxComparator.compare(secondRunCostByStep.get(0L), lastCostFirstRun)<=0);
		// check t
	}
}
