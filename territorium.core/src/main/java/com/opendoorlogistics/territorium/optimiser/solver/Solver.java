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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.function.Consumer;

import com.opendoorlogistics.territorium.optimiser.components.LocalSearch;
import com.opendoorlogistics.territorium.optimiser.components.ProblemSplitter;
import com.opendoorlogistics.territorium.optimiser.components.RandomisedCentreSelector;
import com.opendoorlogistics.territorium.optimiser.components.RandomisedWeightBasedCustomerAssignment;
import com.opendoorlogistics.territorium.optimiser.components.Ruin;
import com.opendoorlogistics.territorium.optimiser.components.ProblemSplitter.Subproblem;
import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.Customer2CustomerClosestNgbMatrixImpl;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.ContinueCallback.ContinueOption;
import com.opendoorlogistics.territorium.optimiser.solver.SearchComponentsTags.TagType;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.utils.Pair;

public class Solver {
	private final Problem problem;
	private final SolverConfig config;
	private final ContinueCallback continueCallback;
	private final Random random;
	private final Customer2CustomerClosestNgbMatrixImpl customer2CustomerClosestNgbMatrix;
	private final LocalSearch localSearch;
	private final Ruin ruin;

	public Solver(Problem problem, SolverConfig config, ContinueCallback continueCallback, Random random) {
		this.problem = problem;
		this.config = config;
		this.continueCallback = continueCallback;
		this.random = random;

		ruin = new Ruin(problem, config.getRuinConfig(), random);
		customer2CustomerClosestNgbMatrix = new Customer2CustomerClosestNgbMatrixImpl(problem);
		localSearch = new LocalSearch(this.problem, this.config.getLocalSearchConfig(), this.customer2CustomerClosestNgbMatrix,
				this.random);

	}

//	/**
//	 * @return
//	 */
//	private LocalSearch createLocalSearch() {
//		return new LocalSearch(this.problem, this.config.getLocalSearchConfig(), this.customer2CustomerClosestNgbMatrix,
//				this.random);
//	}

	public enum SolutionConstructionType {
		RANDOM_WEIGHT_BASED_PLUS_LOCAL_SEARCH, LOCAL_SEARCH_CONSTRUCTION;

	}

	public synchronized void constructNewSolution(SolverStateSummaryImpl state) {
		//state.push("Create new sol");

		// choose comparator at random
		CostComparatorWithTags comparator = state.bank
				.getComparatorForSlot(random.nextInt(state.bank.getNbSolutionSlots()));
		SearchComponentsTags source = new SearchComponentsTags();
		MutableSolution ret = null;
		if (random.nextBoolean()) {

			state.push("Create new sol using randomised weighted");
			ret = constructRandWeightBasedPlusLocalSearchSingleStepSol(comparator);
			source.addTags(TagType.RAND_WEIGHT_LS_CONSTRUCT).addTags(comparator.getTags());
			state.bank.accept(ret, source);
			state.pop();
		} else {

			// construct 100% using local search
			state.push("Create new sol using local search");
			ret = localSearch.constructNewSolution(comparator);
			source.addTags(TagType.LS_CONSTRUCT).addTags(comparator.getTags());
			state.bank.accept(ret, source);
			state.pop();
		}

		// Now run until stagnation (should we swap between slots at this point???)
		state.push("Improving new sol");
		source.addTag(TagType.LS_OPT);
		runUntilLocalOptimum(state, comparator, localSearch, ret, s -> state.bank.accept(s, source));
		state.pop();


	}

	public synchronized MutableSolution constructRandWeightBasedPlusLocalSearchSingleStepSol(Comparator<Cost> comparator) {
		MutableSolution newSol;
		// construct using rand weight
		int[] initialAssigned = new RandomisedWeightBasedCustomerAssignment(problem, config.getWeightBasedAssigner(),
				random).run(new RandomisedCentreSelector(problem, random, config.getCentreSelector()).run(null, null),
						null);
		newSol = new MutableSolution(problem, initialAssigned);

		// clean up with local search...
		newSol = localSearch.runSingleStep(comparator, newSol.getCustomersToClusters());
		return newSol;
	}

	/**
	 * Create a solution using greedy local search only
	 * @return
	 */
	public synchronized ImmutableSolution greedyLocalSearchOnlySolve(){
		MutableSolution initialSol = new MutableSolution(problem, null);
		// Just return an empty solution if nothing is assignable as later heuristics will throw exceptions
		if(isEmptyProblem()){
			return initialSol;
		}
				
		SolverStateSummaryImpl state = new SolverStateSummaryImpl(
				new SolutionBank(config.getSolutionBankConfig(), problem, random));

		CostComparatorWithTags stdCmp = state.bank.getStandardComparator();
		localSearch.assignUnassignedCustomers(stdCmp, initialSol);

		runUntilLocalOptimum(state, stdCmp, localSearch, initialSol, s -> state.bank.accept(s,
				new SearchComponentsTags(TagType.INITIAL_LSOPT).addTags(stdCmp.getTags())));

		return state.getBestSolution();
	}
	
	public synchronized ImmutableSolution solve(int[] startingAssignment) {
		
		// create initial solution object using input if we have it
		MutableSolution initialSol = null;
		// for (int i = 0; i < 100; i++) {
		if (startingAssignment != null) {
			initialSol = new MutableSolution(problem, startingAssignment);
		} else {
			initialSol = new MutableSolution(problem, null);
		}

		// Just return an empty solution if nothing is assignable as later heuristics will throw exceptions
		if(isEmptyProblem()){
			return initialSol;
		}
		
		SolverStateSummaryImpl state = new SolverStateSummaryImpl(
				new SolutionBank(config.getSolutionBankConfig(), problem, random));

		state.push("Create first solution");
		CostComparatorWithTags stdCmp = state.bank.getStandardComparator();
		localSearch.assignUnassignedCustomers(stdCmp, initialSol);
		state.bank.accept(initialSol, new SearchComponentsTags(TagType.INITIAL_CONSTRUCT).addTags(stdCmp.getTags()));
		state.pop();

		if (state.isContinue()) {
			state.push("Improving first solution");
			runUntilLocalOptimum(state, stdCmp, localSearch, initialSol, s -> state.bank.accept(s,
					new SearchComponentsTags(TagType.INITIAL_LSOPT).addTags(stdCmp.getTags())));
			state.pop();
		}

		while (state.isContinue() && state.nbOuterSteps < config.getNbOuterSteps()) {
			runSingleOuterStep(state);
			state.nbOuterSteps++;
		}

		return state.getBestSolution();

	}

	/**
	 * @return
	 */
	private boolean isEmptyProblem() {
		return problem.getCustomers().size()==0 || problem.getClusters().size()==0;
	}

	private class SolverStateSummaryImpl implements SolverStateSummary {
		final SolutionBank bank;
		private boolean keepGoing = true;
		private DoubleSummaryStatistics outerStepTimingsSecs = new DoubleSummaryStatistics();

		SolverStateSummaryImpl(SolutionBank bank) {
			this.bank = bank;
		}

		long nbOuterSteps;
		Stack<String> stack = new Stack<>();;

		@Override
		public long getNbOuterSteps() {
			return nbOuterSteps;
		}

		@Override
		public ImmutableSolution getBestSolution() {
			return bank.getStandardSol();
		}

		@Override
		public Stack<String> getOperationsStack() {
			return stack;
		}

		public void push(String s) {
			stack.push(s);
		}

		public void pop() {
			stack.pop();
		}

		public boolean isContinue() {
			if (!keepGoing) {
				return false;
			}

			if (continueCallback != null) {
				keepGoing = continueCallback.continueOptimisation(this) == ContinueOption.KEEP_GOING;
			}
			return keepGoing;
		}

		@Override
		public SearchComponentsTags getBestSolutionTags() {
			return bank.getSource(0);
		}

		@Override
		public long getBestSolutionNb() {
			return bank.getStandardSolutionNb();
		}

		@Override
		public DoubleSummaryStatistics getOuterStepTimingsInSeconds() {
			return outerStepTimingsSecs;
		}
	}

	public synchronized void runSingleOuterStep(SolutionBank bank) {
		runSingleOuterStep(new SolverStateSummaryImpl(bank));
	}

	private void runSingleOuterStep(SolverStateSummaryImpl state) {
		long start = System.currentTimeMillis();
		if (random.nextDouble() < config.getNewSolutionFraction()) {
			constructNewSolution(state);
		} else {
			ruinRecreateWithSplits(state);
		}
		long end = System.currentTimeMillis();
		double durationSecs = 0.001*(end - start);
		state.getOuterStepTimingsInSeconds().accept(durationSecs);

	}

	private static MutableSolution ruinRecreate(SolverStateSummaryImpl state, ImmutableSolution initialSol, Ruin ruin,
			LocalSearch localSearch, CostComparatorWithTags comparator,
			Consumer<MutableSolution> localSearchStepListener) {
		state.push("Ruin+recreate");

		// choose solution to be ruined at random and ruin it
		state.push("Ruining");
		MutableSolution newSol = null;
		if (state.isContinue()) {
			int[] ruined = ruin.ruin(initialSol.getCustomersToClusters());
			newSol = new MutableSolution(initialSol.getProblem(), ruined);
		}
		state.pop();

		state.push("Recreating");
		if (state.isContinue()) {
			localSearch.assignUnassignedCustomers(comparator, newSol);
		}
		state.pop();

		
		state.push("Improving recreated");
		if (state.isContinue()) {
			runUntilLocalOptimum(state, comparator, localSearch, newSol, localSearchStepListener);
		}
		state.pop();
		
		state.pop();

		return newSol;
	}

	private void ruinRecreateWithSplits(SolverStateSummaryImpl state) {
		// Choose number of subproblems
		int nbSubproblems = 1;
		if (config.getProblemSplitterConfig() != null && random.nextDouble() < config.getSplitProblemProbability()) {
			int maxSubproblems = problem.getClusters().size() / 2;
			if (maxSubproblems < 0) {
				maxSubproblems = 1;
			}
			int range = maxSubproblems - 1;
			if (range >= 1) {
				nbSubproblems = 1 + random.nextInt(range + 1);
			}
		}

		// Choose solution to ruin
		SolutionBank bank = state.bank;
		ImmutableSolution initialSol = bank.getSolutionSlot(random.nextInt(bank.getNbSolutionSlots()));

		// Simple ruin with no splitting
		if (nbSubproblems == 1) {
			CostComparatorWithTags comparator = bank.getComparatorForSlot(random.nextInt(bank.getNbSolutionSlots()));
			ruinRecreate(state, initialSol, ruin, localSearch, comparator, newSol -> bank.accept(newSol,
					new SearchComponentsTags(TagType.RUIN_RECREATE, TagType.LS_OPT).addTags(comparator.getTags())));
			return;
		}

		// Split problems
		state.push("Split");
		ProblemSplitter splitter = new ProblemSplitter(problem, random, config.getProblemSplitterConfig());
		List<Subproblem> subproblems = splitter.splitIntoSubproblems(random, initialSol.getCustomersToClusters(),
				nbSubproblems, customer2CustomerClosestNgbMatrix);

		// Run each problem
		List<Pair<Subproblem, ImmutableSolution>> subproblemSolutions = new ArrayList<>();
		for (int i = 0; i < subproblems.size(); i++) {
			subproblemSolutions.add(new Pair<ProblemSplitter.Subproblem, ImmutableSolution>(subproblems.get(i),
					subproblems.get(i).getSolution()));
		}

		for (int i = 0; i < subproblems.size() && state.isContinue(); i++) {
			Subproblem subproblem = subproblems.get(i);
			state.push("Subproblem " + (i + 1) + "/" + subproblems.size());
			Ruin spRuin = new Ruin(subproblem.getProblem(), config.getRuinConfig(), random);
			LocalSearch spLocalSearch = new LocalSearch(subproblem.getProblem(), config.getLocalSearchConfig(),
					subproblem.getCustomer2CustomerClosestNgbMatrix(), random);

			// the subproblem should have its own solution bank we accept and reject from 
			SolutionBank subproblemSolutionBank = new SolutionBank(config.getSolutionBankConfig(), subproblem.getProblem(), random);
			subproblemSolutionBank.accept(subproblem.getSolution(), new SearchComponentsTags());
			CostComparatorWithTags comparator = subproblemSolutionBank.getComparatorForSlot(random.nextInt(subproblemSolutionBank.getNbSolutionSlots()));
			
			// get new sol and accept/reject it by placing in the bank
			ImmutableSolution newSpSol = ruinRecreate(state, subproblem.getSolution(), spRuin, spLocalSearch,
					comparator, null);
			subproblemSolutionBank.accept(newSpSol, new SearchComponentsTags().addTags(comparator.getTags()));
			newSpSol = subproblemSolutionBank.getStandardSol();

//			CostComparatorWithTags comparator = bank.getComparatorForSlot(random.nextInt(bank.getNbSolutionSlots()));			
//			ImmutableSolution newSpSol = ruinRecreate(state, subproblem.getSolution(), spRuin, spLocalSearch,
//					comparator, null);
					
			// see if we have an improving solution (log individually for each subproblem - helps to analyse the
			// algorithm)
			subproblemSolutions.get(i).setB(newSpSol);
			MutableSolution newSol = ProblemSplitter.combineSubProblemSolutions(problem, subproblemSolutions);
			bank.accept(newSol, new SearchComponentsTags(TagType.SPLIT, TagType.RUIN_RECREATE, TagType.LS_OPT)
					.addTags(comparator.getTags()));
			state.pop();

		}

		state.pop();

	}

	private static void runUntilLocalOptimum(SolverStateSummaryImpl state, CostComparatorWithTags comparator,
			LocalSearch localSearch, MutableSolution newSol, Consumer<MutableSolution> stepEndedListener) {
		int innerStep = 1;

		boolean improved = true;
		boolean keepGoing = true;
		while (improved && keepGoing) {

			// check for user cancellation, updating message before cancellation
			state.push("LocalSearch step "	+ innerStep );
			keepGoing = state.isContinue();

			// run single step of local search
			state.push("");
			localSearch.setContinueLocalSearchCallback(s->{
				state.pop();
				state.push(s);
				return state.isContinue();
			});
			improved = localSearch.runSingleStep(innerStep++, comparator, newSol);
			state.pop();
			
			// remember to clear the continue search callback from the local
			// search so we don't accidently call it when we should be using
			// a different state and stack object later...
			localSearch.setContinueLocalSearchCallback(null);
			
			if (stepEndedListener != null) {
				stepEndedListener.accept(newSol);
			}

			state.pop();
		}
	}

}
