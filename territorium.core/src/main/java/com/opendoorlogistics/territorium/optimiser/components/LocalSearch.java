package com.opendoorlogistics.territorium.optimiser.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.Customer2CustomerClosestNgbMatrix;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.optimiser.utils.SolutionImprovementChecker;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.ObjectWithJSONToString;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.utils.NumberUtils;

import gnu.trove.list.array.TIntArrayList;

public class LocalSearch {
	private final Problem problem;
	private final LocalSearchConfig config;
	private final Random random;
	private ContinueLocalSearchCallback continueLocalSearchCallback;
//	private final LocalSearchContinueCallback cont;
//	private final Comparator<Cost> comparator = Cost.createApproxEqualComparator();
	private final Customer2CustomerClosestNgbMatrix closestNeighboursMatrix;
	
	public LocalSearch(Problem problem, LocalSearchConfig config, Customer2CustomerClosestNgbMatrix closestNeighboursMatrix, Random random) {
		this.problem = problem;
		this.config = config;
		this.random = random;
		this.closestNeighboursMatrix = closestNeighboursMatrix;
	}
	
	public static interface LocalSearchContinueCallback{
		boolean continueLocalSearch();
	}
	
	public enum LocalSearchHeuristic{
		INTERCLUSTER_SWAPS,
		INTERCLUSTER_MOVES,
		NEAREST_CUSTOMERS_MOVE,
		NONE
	}

	public static class LocalSearchConfig extends ObjectWithJSONToString{
		private boolean interclusterSwaps = true;
		private boolean interclusterMoves=true;
		private boolean customerNearestNeighbourSwaps=true;
		private boolean estimatedSwapCosts=true;
		private int interchangeNNearest = 5;

		
		@JsonIgnore
		public void setAllHeuristicsOff(){
			setInterclusterSwaps(false);
			setInterclusterMoves(false);
			setCustomerNearestNeighbourSwaps(false);
		}

		@JsonIgnore
		public void set(LocalSearchHeuristic h, boolean on){
			switch (h) {
			case INTERCLUSTER_SWAPS:
				setInterclusterSwaps(on);
				break;
				
			case INTERCLUSTER_MOVES:
				setInterclusterMoves(on);
				break;
				
			case NEAREST_CUSTOMERS_MOVE:
				setCustomerNearestNeighbourSwaps(on);
				break;
				
			default:
				break;
			}
		}
		
		public boolean isInterclusterSwaps() {
			return interclusterSwaps;
		}

		public void setInterclusterSwaps(boolean interclusterSwaps) {
			this.interclusterSwaps = interclusterSwaps;
		}

		public boolean isInterclusterMoves() {
			return interclusterMoves;
		}

		public void setInterclusterMoves(boolean interclusterMoves) {
			this.interclusterMoves = interclusterMoves;
		}

		public boolean isCustomerNearestNeighbourSwaps() {
			return customerNearestNeighbourSwaps;
		}

		public void setCustomerNearestNeighbourSwaps(boolean customerNearestestNeighbourSwaps) {
			this.customerNearestNeighbourSwaps = customerNearestestNeighbourSwaps;
		}

		public int getInterchangeNNearest() {
			return interchangeNNearest;
		}

		public void setInterchangeNNearest(int interchangeNNearest) {
			this.interchangeNNearest = interchangeNNearest;
		}

		public boolean isEstimatedSwapCosts() {
			return estimatedSwapCosts;
		}

		public void setEstimatedSwapCosts(boolean estimatedSwapCosts) {
			this.estimatedSwapCosts = estimatedSwapCosts;
		}

		
	}

//	public MutableLocalSearchSolution run(int[] customersToClusters) {
//		if (customersToClusters.length != problem.getCustomers().size()) {
//			throw new IllegalArgumentException();
//		}
//
//		MutableLocalSearchSolution sol = new MutableLocalSearchSolution(problem, customersToClusters);
//		int step=0;
//		while(cont.continueOptimisation(step, null, sol)==ContinueOption.KEEP_GOING){
//			runSingleStep(step, sol);
//			
//			// recreate the solution after each step to help prevent rounding errors accumulating
//			sol = new MutableLocalSearchSolution(problem, sol.getCustomersToClusters());
//		}
//		return sol;
//	}
	
	public MutableSolution runSingleStep(Comparator<Cost> comparator,int [] customersToClusters){
		MutableSolution sol = new MutableSolution(problem, customersToClusters);
		runSingleStep(0, comparator,sol);
		return sol;
	}
	

	private void interclusterMoves(Comparator<Cost> comparator,Random random, int clusteri, int clusterj, MutableSolution solution) {
		if(!config.isInterclusterMoves()){
			return;
		}
		
		Cost originalCost = new Cost();
		Cost newCost = new Cost();
		TIntArrayList customersi = new TIntArrayList();
		solution.getCustomers(clusteri, customersi);
		int n = customersi.size();
		customersi.shuffle(random);

		// loop over each customer in cluster i
		for (int i = 0; i < n; i++) {
			int customeri = customersi.get(i);
			originalCost.set(solution.getCost());
			solution.evaluateSet(customeri, clusterj, newCost);
			if (acceptMove(comparator,originalCost,newCost)) {
				solution.setCustomerToCluster(customeri, clusterj);
			}
		}
	}
	
	private boolean acceptMove(Comparator<Cost> comparator,Cost oldCost, Cost newCost){
		int diff = comparator.compare(oldCost, newCost);
		if(diff>0){
			// old cost worse than new
			return true;
		}
		if(diff<0){
			// old cost better than new
			return false;
		}
		
		// old cost ~ same as new. accept change randomly (to randomise the search)
		return random.nextBoolean();
	}
	
	public MutableSolution constructNewSolution(Comparator<Cost> comparator){
		MutableSolution sol = new MutableSolution(problem, null);
		assignUnassignedCustomers(comparator,sol);
		return sol;
	}

	public void assignUnassignedCustomers(Comparator<Cost> comparator,MutableSolution sol) {
		int p = problem.getClusters().size();
		Cost best= new Cost();
		Cost newCost= new Cost();
		for(int customerIndx: NumberUtils.getRandomOrder0ToNArray(random, problem.getCustomers().size()-1)){
			if(sol.getClusterIndex(customerIndx)!=-1){
				// already loaded
				continue;
			}
			best.setMax();
			int bestIndex=-1;
			for(int clusterIndx : NumberUtils.getRandomOrder0ToNArray(random, p-1)){
				sol.evaluateSet(customerIndx, clusterIndx, newCost);
				if(comparator.compare(newCost, best)<0){
					bestIndex = clusterIndx;
					best.set(newCost);
				}
			}
			sol.setCustomerToCluster(customerIndx, bestIndex);
		}
		
		// update sol before returning to stop any rounding errors
		sol.update();
	}

	
	/**
	 * Intra-cluster moves use a cluster's x nearest neighbouring clusters
	 * however this metric may not work well under certain circumstances
	 * so we also try moves based on nearest customers to customers (on other clusters)
	 * @param step
	 * @param solution
	 */
	private void nearestCustomerMoves(int step, Comparator<Cost> comparator,MutableSolution solution){
		if(!isContinue("NearCustMoves")){
			return;
		}
		
		if(!config.isCustomerNearestNeighbourSwaps()){
			return;
		}
		
		Cost newCost = new Cost();
		
		long lastCallbackTime=System.currentTimeMillis();
		boolean [] clusterTried = new boolean[problem.getClusters().size()];
		int nbDone=0;
		for(int customerIndx: NumberUtils.getRandomOrder0ToNArray(random, problem.getCustomers().size()-1)){
			int nbNearest = closestNeighboursMatrix.getNbClosestNeighbours(customerIndx);
			
			// init found clusters boolean
			Arrays.fill(clusterTried, false);
			int clusterIndx =solution.getClusterIndex(customerIndx);
			if(clusterIndx>=0){
				clusterTried[clusterIndx]=true;				
			}
			
			int nbClustersTried = 0;
			for(int iNear=0; iNear<nbNearest ; iNear++){
				
				int otherClusterIndx = closestNeighboursMatrix.getClusterIndexOfClosestNeighbour(solution, customerIndx, iNear);
				if(otherClusterIndx>=0 && !clusterTried[otherClusterIndx]){
					clusterTried[otherClusterIndx]=true;
					nbClustersTried++;
					solution.evaluateSet(customerIndx, otherClusterIndx, newCost);
					if (acceptMove(comparator,solution.getCost(),newCost)) {
						solution.setCustomerToCluster(customerIndx, otherClusterIndx);
					}
				}
				
				if(nbClustersTried>config.getInterchangeNNearest()){
					break;
				}
			}
			
			nbDone++;
			
			// do callback if its been over a second
			if(System.currentTimeMillis() - lastCallbackTime > 1000){
				if(!isContinue("Customer " + nbDone +"/" + problem.getCustomers().size())){
					break;
				}
				lastCallbackTime = System.currentTimeMillis();
			}
		}
	}

	/**
	 * Run a single step on the input solution and return true if we improved
	 * @param step
	 * @param solution
	 * @return
	 */
	public boolean runSingleStep(int step, Comparator<Cost> comparator,MutableSolution solution) {
		SolutionImprovementChecker checker = new SolutionImprovementChecker(solution);

		if(random.nextBoolean()){
			nearestClusterSearch(step,comparator, solution);
			nearestCustomerMoves(step,comparator, solution);			
		}else{
			nearestCustomerMoves(step,comparator, solution);
			nearestClusterSearch(step,comparator, solution);			
		}

		// prevent rounding errors
		solution.update();
		
		// see if we've improved by more than the round-off limit
		return checker.isImproved(solution);
	}

	public static interface ContinueLocalSearchCallback{
		boolean continueSearch(String currentOperation);
	}
	
	private void nearestClusterSearch(int step,Comparator<Cost> comparator, MutableSolution solution) {
		if(!isContinue("NearClusterSearch")){
			return;
		}
		
		// calculate a cluster to cluster distance matrix using the minimum distance to
		// a point in another cluster
		int p = problem.getClusters().size();
		final double[][] matrix = new double[p][p];
		for (int i = 0; i < p; i++) {
			matrix[i] = new double[p];
			Arrays.fill(matrix[i], Double.POSITIVE_INFINITY);
		}

		List<? extends Customer> customers = problem.getCustomers();
		int nc = customers.size();
		for (int i = 0; i < nc; i++) {
			int clusterI = solution.getClusterIndex(i);
			if (clusterI != -1) {
				Customer coi = customers.get(i);
				for (int j = 0; j < nc; j++) {
					int clusterJ = solution.getClusterIndex(j);
					if (clusterJ != -1) {
						Customer coj = customers.get(j);
						double distance = problem.getTravelCost(coi.getLocation(), coj);
						distance += problem.getTravelCost(coj.getLocation(), coi);
						matrix[clusterI][clusterJ] = Math.min(matrix[clusterI][clusterJ], distance);
					}
				}
			}
		}

		// get a sorted list of nearest clusters for each cluster
		ArrayList<ArrayList<Integer>> nearestLists = new ArrayList<>();
		for (int i = 0; i < p; i++) {
			ArrayList<Integer> nearest = new ArrayList<>();
			nearestLists.add(nearest);
			for (int j = 0; j < p; j++) {
				if (i != j) {
					nearest.add(j);
				}
			}

			final int from = i;
			Collections.sort(nearest, new Comparator<Integer>() {

				@Override
				public int compare(Integer o1, Integer o2) {
					double d1 = matrix[from][o1];
					double d2 = matrix[from][o2];
					return Double.compare(d1, d2);
				}
			});

			while (nearest.size() > config.getInterchangeNNearest()) {
				nearest.remove(nearest.size() - 1);
			}
		}

		// get a randomly ordered list of cluster indices
		TIntArrayList list = new TIntArrayList(p);
		for (int i = 0; i < p; i++) {
			list.add(i);
		}
		list.shuffle(random);


		// loop over each cluster taking first improving moves
		long lastCallbackTime = System.currentTimeMillis();
		for (int i = 0; i < p; i++) {
			int cli = list.get(i);


			// shuffle its nearest clusters
			List<Integer> nearest = nearestLists.get(cli);
			Collections.shuffle(nearest, random);

			for (int j = 0; j < nearest.size(); j++) {
				int clj = nearest.get(j);
				if (cli == clj) {
					throw new RuntimeException();
				}

				if (random.nextBoolean()) {

					interclusterMoves(comparator,random,cli, clj, solution);
					interclusterSwaps(comparator,random,cli, clj, solution);

				} else {
					interclusterSwaps(comparator,random, cli, clj, solution);
					interclusterMoves(comparator,random, cli, clj, solution);
				}
			}

			// refresh solution after processing each cluster to help prevent round-off
			solution.update();

			// do callback if its been over a second
			if(System.currentTimeMillis() - lastCallbackTime > 1000){
				if(!isContinue("Cluster " + (i+1) +"/" + p)){
					break;
				}
				lastCallbackTime = System.currentTimeMillis();
			}
			
		}
	}
	private void interclusterSwaps(Comparator<Cost> comparator,Random random, int clusteri, int clusterj, MutableSolution solution) {
		if(!config.isInterclusterSwaps()){
			return;
		}
		
		TIntArrayList customersi = new TIntArrayList();
		solution.getCustomers(clusteri, customersi);
		customersi.shuffle(random);

		TIntArrayList customersj = new TIntArrayList();
		solution.getCustomers(clusterj, customersj);
		customersj.shuffle(random);

		solution.update();
		Cost cost = new Cost();
		Cost bestSwap = new Cost();

		int ni = customersi.size();
		int nj = customersj.size();
		for (int i = 0; i < ni; i++) {

			int customeri = customersi.get(i);
			
			// set best swap cost to the initial solution cost
			bestSwap.set(solution.getCost());
			
			int bestSwapCustomerIndx = -1;
			for (int j = 0; j < nj; j++) {
				// check still assigned to j before evaluating swap
				int customerj = customersj.get(j);
				if (solution.getClusterIndex(customerj) == clusterj) {

					// if we're estimating the swap costs we reject likely bad swaps
					// early using the crude estimate...
					if(config.isEstimatedSwapCosts()){
						solution.estimateSwapCostWithoutChangingCentres(customeri, customerj, cost);
						if(comparator.compare(cost, bestSwap)>0){
							continue;
						}
					}
					
					// do the 100% accurate estimation
					solution.evaluateSwap(customeri, customerj, cost);
					if (cost.compareTo(bestSwap) <= 0) {
						bestSwap.set(cost);
						bestSwapCustomerIndx = customerj;
					}
				}
			}

			// do swap if profitable
			if (bestSwapCustomerIndx!=-1 && acceptMove(comparator,solution.getCost(), bestSwap)) {
				solution.setCustomerToCluster(customeri, clusterj);
				solution.setCustomerToCluster(bestSwapCustomerIndx, clusteri);
			}
		}
	}


	public void setContinueLocalSearchCallback(ContinueLocalSearchCallback continueLocalSearchCallback) {
		this.continueLocalSearchCallback = continueLocalSearchCallback;
	}

	private boolean isContinue(String currentOperation){
		return continueLocalSearchCallback!=null?continueLocalSearchCallback.continueSearch(currentOperation):true;
	}
	
//	private ContinueOption getContinue(int step, ImmutableSolution best, HeuristicType currentHeuristic) {
//		if(cont==null){
//			return ContinueOption.KEEP_GOING;
//		}
//		
//		// always call the callback even on step 0 as it also reports cost
//		ContinueOption ret = cont.continueOptimisation(step, currentHeuristic, best);
//
//		// always ensure we have a solution if the user hasn't cancelled
//		if (best == null && ret == ContinueOption.FINISH_NOW) {
//			return ContinueOption.KEEP_GOING;
//		}
//
//		return ret;
//	}

	
}
