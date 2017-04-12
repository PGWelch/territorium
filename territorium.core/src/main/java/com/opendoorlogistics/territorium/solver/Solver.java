package com.opendoorlogistics.territorium.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.function.Consumer;

import com.opendoorlogistics.territorium.problem.data.Problem;
import com.opendoorlogistics.territorium.solver.ContinueCallback.ContinueOption;
import com.opendoorlogistics.territorium.solver.ProblemSplitter.Subproblem;
import com.opendoorlogistics.territorium.solver.construction.RandomisedCentreSelector;
import com.opendoorlogistics.territorium.solver.construction.RandomisedWeightBasedCustomerAssignment;
import com.opendoorlogistics.territorium.solver.data.Cost;
import com.opendoorlogistics.territorium.solver.data.Customer2CustomerClosestNgbMatrixImpl;
import com.opendoorlogistics.territorium.utils.Pair;

public class Solver {
	private final Problem problem;
	private final SolverConfig config;
	private final ContinueCallback continueCallback;
	private final Random random;
	private final Customer2CustomerClosestNgbMatrixImpl customer2CustomerClosestNgbMatrix;
	private final LocalSearch localSearch;
	private final Ruin ruin;

	// public enum HeuristicType {
	// LOCAL_SEARCH, REGRET_REASSIGN, INITIAL_ASSIGN
	// }

	public Solver(Problem problem, SolverConfig config, ContinueCallback continueCallback, Random random) {
		this.problem = problem;
		this.config = config;
		this.continueCallback = continueCallback;
		this.random = random;

		ruin = new Ruin(problem, config.getRuinConfig(), random);
		customer2CustomerClosestNgbMatrix = new Customer2CustomerClosestNgbMatrixImpl(problem);
		localSearch = new LocalSearch(problem, config.getLocalSearchConfig(), customer2CustomerClosestNgbMatrix,
				random);

	}

	public enum SolutionConstructionType {
		RANDOM_WEIGHT_BASED_PLUS_LOCAL_SEARCH, LOCAL_SEARCH_CONSTRUCTION;

	}
	
	private static String addBracketedComparatorCode(String s, CostComparatorWithName comparator){
		if(comparator.shortCode().length()>0){
			return s +"("+comparator.shortCode()+")";
		}
		return s;
	}

	public void constructNewSolution(SolverStateSummaryImpl state) {
		state.push("Constructing new solution");

		// choose comparator at random
		CostComparatorWithName comparator = state.bank.getComparatorForSlot(random.nextInt(state.bank.getNbSolutionSlots()));
		String source;
		MutableSolution ret = null;
		if (random.nextBoolean()) {

			state.push("Constructing new solution using randomised weighted");
			ret = constructRandWeightBasedPlusLocalSearchSingleStepSol(comparator);
			source = addBracketedComparatorCode("RandWeightConstruct+LocalSearch",comparator);
			state.pop();
		} else {

			// construct 100% using local search
			state.push("Constructing new solution using local search");
			ret = localSearch.constructNewSolution(comparator);
			source = addBracketedComparatorCode("LocalSearchConstruct+LocalSearch",comparator);
			state.pop();
		}


		// Now run until stagnation (should we swap between slots at this point???)
		state.push("Improving new solution");
		runUntilLocalOptimum(state, comparator, localSearch, ret, s->state.bank.accept(s, source));
		state.pop();

		state.pop();

	}

	public MutableSolution constructRandWeightBasedPlusLocalSearchSingleStepSol(Comparator<Cost> comparator) {
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

	public ImmutableSolution solve(int[] startingAssignment) {

		// create initial solution object using input if we have it
		MutableSolution initialSol = null;
		// for (int i = 0; i < 100; i++) {
		if (startingAssignment != null) {
			initialSol = new MutableSolution(problem, startingAssignment);
		} else {
			initialSol = new MutableSolution(problem, null);
		}

		SolverStateSummaryImpl state = new SolverStateSummaryImpl(
				new SolutionBank(config.getSolutionBankConfig(), problem, random));

		state.push("Constructing initial solution");
		localSearch.assignUnassignedCustomers(state.bank.getStandardComparator(), initialSol);
		state.bank.accept(initialSol, "InitialConstruct");
		state.pop();

		if(state.isContinue()){
			state.push("Improving initial solution");
			runUntilLocalOptimum(state, state.bank.getStandardComparator(), localSearch, initialSol, s->state.bank.accept(s, "InitialImprove"));
			state.pop();			
		}

		while (state.isContinue() && state.nbOuterSteps < config.getNbOuterSteps()) {
			runSingleOuterStep(state);
			state.nbOuterSteps++;
		}

		return state.getBestSolution();

	}

	private class SolverStateSummaryImpl implements SolverStateSummary {
		final SolutionBank bank;
		private boolean keepGoing = true;

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

			keepGoing = continueCallback.continueOptimisation(this) == ContinueOption.KEEP_GOING;
			return keepGoing;
		}

		@Override
		public String getBestSolutionSource() {
			return bank.getSource(0);
		}
	}

	public void runSingleOuterStep(SolutionBank bank) {
		runSingleOuterStep(new SolverStateSummaryImpl(bank));
	}

	private void runSingleOuterStep(SolverStateSummaryImpl state) {
		if (random.nextDouble() < config.getNewSolutionFraction()) {
			constructNewSolution(state);
		} else {
			ruinRecreateWithSplits(state);
		}

	}

	private static MutableSolution ruinRecreate(SolverStateSummaryImpl state, ImmutableSolution initialSol, Ruin ruin,
			LocalSearch localSearch, CostComparatorWithName comparator, Consumer<MutableSolution> localSearchStepListener) {
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

		if (state.isContinue()) {
			runUntilLocalOptimum(state, comparator, localSearch, newSol,localSearchStepListener);
		}
	//	state.pop();

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
			CostComparatorWithName comparator = bank.getComparatorForSlot(random.nextInt(bank.getNbSolutionSlots()));
			ruinRecreate(state, initialSol, ruin, localSearch, comparator, newSol->bank.accept(newSol,addBracketedComparatorCode("Ruin+Recreate+LocalSearch",comparator)));
			return;
		}

		// Split problems
		state.push("Running split");
		ProblemSplitter splitter = new ProblemSplitter(problem, random, config.getProblemSplitterConfig());
		List<Subproblem> subproblems = splitter.splitIntoSubproblems(random, initialSol.getCustomersToClusters(),
				nbSubproblems, customer2CustomerClosestNgbMatrix);
		
		// Run each problem
		List<Pair<Subproblem,ImmutableSolution>> subproblemSolutions = new ArrayList<>();
		for (int i = 0; i < subproblems.size() && state.isContinue(); i++) {
			Subproblem subproblem = subproblems.get(i);
			state.push("Subproblem " + (i + 1) + "/" + subproblems.size());
			Ruin spRuin = new Ruin(subproblem.getProblem(), config.getRuinConfig(), random);
			LocalSearch spLocalSearch = new LocalSearch(subproblem.getProblem(), config.getLocalSearchConfig(), subproblem.getCustomer2CustomerClosestNgbMatrix(), random);
			CostComparatorWithName comparator = bank.getComparatorForSlot(random.nextInt(bank.getNbSolutionSlots()));
			ImmutableSolution newSpSol=ruinRecreate(state, subproblem.getSolution(), spRuin, spLocalSearch, comparator,null);
			subproblemSolutions.add(new Pair<ProblemSplitter.Subproblem, ImmutableSolution>(subproblem, newSpSol));
			state.pop();
		}

		if(state.isContinue()){
			MutableSolution newSol=ProblemSplitter.combineSubProblemSolutions(problem, subproblemSolutions);			
			bank.accept(newSol, "Split+Ruin+Recreate+LocalSearch");
		}

		state.pop();

	}

	private static void runUntilLocalOptimum(SolverStateSummaryImpl state, CostComparatorWithName comparator,
			LocalSearch localSearch, MutableSolution newSol, Consumer<MutableSolution> stepEndedListener) {
		int innerStep = 1;

		boolean improved = true;
		boolean keepGoing = true;
		while (improved && keepGoing) {

			// check for user cancellation, updating message before cancellation
			state.push(comparator.shortCode() + "LocalSearch" + addBracketedComparatorCode("", comparator) + " step " + innerStep + " (" + newSol.getCost().toSingleLineSummaryNoBrackets() + ")");
			keepGoing = state.isContinue();

			// run single step of local search
			improved = localSearch.runSingleStep(innerStep++, comparator, newSol);
			
			if(stepEndedListener!=null){
				stepEndedListener.accept(newSol);
			}
			
			state.pop();
		}
	}

}
