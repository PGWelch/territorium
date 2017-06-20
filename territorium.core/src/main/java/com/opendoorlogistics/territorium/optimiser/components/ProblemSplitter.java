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
package com.opendoorlogistics.territorium.optimiser.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.Customer2CustomerClosestNgbMatrix;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.Solver;
import com.opendoorlogistics.territorium.optimiser.solver.SolverConfig;
import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;
import com.opendoorlogistics.territorium.utils.Pair;

import gnu.trove.list.array.TIntArrayList;

public class ProblemSplitter {
	private final Problem problem;
	private final Random random;
	private final ProblemSplitterConfig config;
	
	public static class ProblemSplitterConfig{
		private double minFractionOfAverage=0.5;
		private double maxFractionOfAverage=2;
		public double getMinFractionOfAverage() {
			return minFractionOfAverage;
		}
		public void setMinFractionOfAverage(double minFractionOfAverage) {
			this.minFractionOfAverage = minFractionOfAverage;
		}
		public double getMaxFractionOfAverage() {
			return maxFractionOfAverage;
		}
		public void setMaxFractionOfAverage(double maxFractionOfAverage) {
			this.maxFractionOfAverage = maxFractionOfAverage;
		}
		
	}
	
	public ProblemSplitter(Problem problem, Random random, ProblemSplitterConfig config) {
		this.problem = problem;
		this.random = random;
		this.config = config;
	}


	public TreeSet<Integer> getNbsOfSubproblems() {
		// initially choose the numbers
		TreeSet<Integer> subproblemNbs = new TreeSet<>();
		int maxNbSubproblems = problem.getClusters().size() / 2;
		maxNbSubproblems = Math.max(maxNbSubproblems, 1);
		int nbSubproblems = 1;
		while (nbSubproblems <= maxNbSubproblems) {
			subproblemNbs.add(nbSubproblems);
			nbSubproblems *= 2;
		}

		// then randomise them a bit, to help randomise the search
		for (int val : new ArrayList<Integer>(subproblemNbs)) {
			int offset = random.nextInt(3) - 1;
			int newVal = val + offset;
			if (!subproblemNbs.contains(newVal)) {
				subproblemNbs.remove(val);
				subproblemNbs.add(newVal);
			}
		}

		// Ensure we always have one...
		subproblemNbs.add(1);
		return subproblemNbs;
	}

	public static MutableSolution combineSubProblemSolutions(Problem problem, List<Pair<Subproblem,ImmutableSolution>> subproblemsWithSolutions){
		int [] assignments = new int[problem.getCustomers().size()];
		Arrays.fill(assignments, -1);
		//int customerCount=0;
		for(Pair<Subproblem,ImmutableSolution> pair: subproblemsWithSolutions){
		//	customerCount += pair.getA().getProblem().getCustomers().size();
			if(pair.getB().getNbUnassignedCustomers()>0){
				throw new RuntimeException("All subproblems must have all customers assigned");
			}
			int nc = pair.getA().getProblem().getCustomers().size();
			for(int i =0 ; i< nc ; i++){
				
				int newClusterIndex=pair.getB().getClusterIndex(i);
				if(newClusterIndex==-1){
					throw new RuntimeException();
				}
				
				int originalClusterIndex = pair.getA().getNewToOriginalClusterIndices()[newClusterIndex];
				if(originalClusterIndex==-1){
					throw new RuntimeException();					
				}
				int originalCustomerIndx = pair.getA().getNewToOriginalCustomerIndices()[i];
				assignments[originalCustomerIndx] = originalClusterIndex;
			}
		}
		
//		if(customerCount < problem.getCustomers().size()){
//			throw new RuntimeException("Subproblems did not contain all customers");
//		}
		MutableSolution ret= new MutableSolution(problem, assignments);
		if(ret.getNbUnassignedCustomers()>0){
			throw new RuntimeException();
		}
		return ret;
	}
	
	public static class Subproblem{
		private Problem problem;
		private ImmutableSolution solution;
		private int[] newToOriginalCustomerIndices;
		private int[] newToOriginalClusterIndices;
		private Customer2CustomerClosestNgbMatrix customer2CustomerClosestNgbMatrix;
		
		public Problem getProblem() {
			return problem;
		}
		public void setProblem(Problem problem) {
			this.problem = problem;
		}
		public ImmutableSolution getSolution() {
			return solution;
		}
		public void setSolution(ImmutableSolution solution) {
			this.solution = solution;
		}
		public int[] getNewToOriginalCustomerIndices() {
			return newToOriginalCustomerIndices;
		}
		public void setNewToOriginalCustomerIndices(int[] newToOriginalCustomerIndices) {
			this.newToOriginalCustomerIndices = newToOriginalCustomerIndices;
		}
		public int[] getNewToOriginalClusterIndices() {
			return newToOriginalClusterIndices;
		}
		public void setNewToOriginalClusterIndices(int[] newToOriginalClusterIndices) {
			this.newToOriginalClusterIndices = newToOriginalClusterIndices;
		}
		public Customer2CustomerClosestNgbMatrix getCustomer2CustomerClosestNgbMatrix() {
			return customer2CustomerClosestNgbMatrix;
		}
		public void setCustomer2CustomerClosestNgbMatrix(Customer2CustomerClosestNgbMatrix customer2CustomerClosestNgbMatrix) {
			this.customer2CustomerClosestNgbMatrix = customer2CustomerClosestNgbMatrix;
		}

	}

	/**
	 * Split the solution into the required number of subproblems
	 * @param random
	 * @param assignment
	 * @param nbSubproblems
	 * @return
	 */
	public List<Subproblem> splitIntoSubproblems(Random random, int[] assignment, int nbSubproblems, Customer2CustomerClosestNgbMatrix customer2CustomerClosestNgbMatrix) {
		ImmutableSolution originalSol= new MutableSolution(problem, assignment);
		if(originalSol.getNbUnassignedCustomers()>0){
			throw new RuntimeException("All customers must be assigned before splitting");
		}
		
		// Create the problem to split into different subproblems
		Problem splitProblem = createSplitterProblem(nbSubproblems, originalSol);

		// Construct using our specifically randomised constructor
		ImmutableSolution splitSolution = new Solver(splitProblem, new SolverConfig(), null, random).constructRandWeightBasedPlusLocalSearchSingleStepSol(Cost.createApproxEqualComparator());
		
		// Now create individual subproblem objects *together with the initial solution*
		ArrayList<Subproblem> ret = new ArrayList<>();
		TIntArrayList originalClusterIndices= new TIntArrayList();
		TIntArrayList originalCustomerIndices= new TIntArrayList();
		for(int i =0 ; i<splitProblem.getClusters().size() ; i++){
			Subproblem newProblem = new Subproblem();
			newProblem.setNewToOriginalClusterIndices(new int[problem.getClusters().size()]);
			newProblem.setNewToOriginalCustomerIndices(new int[problem.getCustomers().size()]);
			Arrays.fill(newProblem.getNewToOriginalClusterIndices(), -1);
			Arrays.fill(newProblem.getNewToOriginalCustomerIndices(), -1);

			// get the original cluster indices assigned to this subproblem
			originalClusterIndices.clear();
			splitSolution.getCustomers(i, originalClusterIndices);
			if(originalClusterIndices.size()==0){
				// empty subproblem
				continue;
			}
			// create new clusters, assignments and new customers
			TIntArrayList newAssignments = new TIntArrayList();
			ArrayList<Customer> newCustomers = new ArrayList<>();			
			ArrayList<Cluster> newClusters= new ArrayList<>();			
			for(int j = 0 ; j<originalClusterIndices.size(); j++){
				long originalClusterIndx =splitProblem.getCustomers().get(originalClusterIndices.get(j)).getUserIndex();
				int newClusterIndx = j;
				
				// process customers and assignments
				originalCustomerIndices.clear();
				originalSol.getCustomers((int)originalClusterIndx, originalCustomerIndices);
				for(int k=0;k<originalCustomerIndices.size() ; k++){
					int originalCustomerIndx = originalCustomerIndices.get(k);
					newProblem.getNewToOriginalCustomerIndices()[newCustomers.size()]= originalCustomerIndx;
					
					// we use the external index in the clone.
					Customer customer = problem.getCustomers().get(originalCustomerIndx);
					newCustomers.add(customer);
					newAssignments.add(newClusterIndx);
				}
				
				// process cluster record
				Cluster cluster = problem.getClusters().get((int)originalClusterIndx);
				newProblem.getNewToOriginalClusterIndices()[newClusters.size()]= (int)originalClusterIndx;
				newClusters.add(cluster);
			}
			
			// Reuse the travel matrix			
			newProblem.setProblem(new Problem(newCustomers, newClusters, problem.getTravelMatrix()));;
			newProblem.setSolution(new MutableSolution(newProblem.getProblem(), newAssignments.toArray()));
			
			// Remap the nearest customer to customer matrix
			newProblem.setCustomer2CustomerClosestNgbMatrix(new Customer2CustomerClosestNgbMatrix() {
				
				@Override
				public int getNbClosestNeighbours(int customerIndx) {
					return customer2CustomerClosestNgbMatrix.getNbClosestNeighbours(newProblem.getNewToOriginalCustomerIndices()[customerIndx]);
				}
				
				@Override
				public int getClusterIndexOfClosestNeighbour(ImmutableSolution solution, int customerIndex,
						int nearestNeighbourIndex) {
					int originalCustomerIndx = newProblem.getNewToOriginalCustomerIndices()[customerIndex];
					if(originalCustomerIndx==-1){
						// not in same subproblem
						return -1;
					}
					
					// get from subproblem
					return solution.getClusterIndex(customerIndex);
			//		throw new UnsupportedOperationException();
				}
			});
			
			ret.add(newProblem);
		}
		
		int checkCustomers=0;
		for(Subproblem subproblem : ret){
			checkCustomers += subproblem.getProblem().getCustomers().size();
		}
		if(checkCustomers < problem.getCustomers().size()){
			throw new RuntimeException();
		}
		
		return ret;
	}


	private Problem createSplitterProblem(int nbSubproblems, ImmutableSolution originalSol) {
		// get existing cluster centres
		Location [] centres = originalSol.getClusterCentres();
		
		// get mean cost per unit time and distance for customers
		DoubleSummaryStatistics costsPerUnitTime = new DoubleSummaryStatistics();
		DoubleSummaryStatistics costsPerUnitDistance = new DoubleSummaryStatistics();
		for (Customer customer : problem.getCustomers()) {
			costsPerUnitTime.accept(customer.getCostPerUnitTime());
			costsPerUnitDistance.accept(customer.getCostPerUnitDistance());
		}

		// Create a pseudo customer for each cluster
		int p = centres.length;
		ArrayList<Customer> pseudoCustomers = new ArrayList<>(p);
		for (int i = 0; i < p; i++) {
			if (centres[i] != null) {
				Customer pseudoCustomer = new Customer();
				pseudoCustomer.setLocation(centres[i]);
				pseudoCustomer.setQuantity(1);
				pseudoCustomer.setCostPerUnitTime(costsPerUnitTime.getAverage());
				pseudoCustomer.setCostPerUnitDistance(costsPerUnitDistance.getAverage());
				
				// save the original cluster index in the user index
				pseudoCustomer.setUserIndex(i);
				
				pseudoCustomers.add(pseudoCustomer);
			}
		}
		
		// get min and max quantities based on the average quantity and ensuring at least 2 per cluster
		double averageQuantity = (double)pseudoCustomers.size() / nbSubproblems;
		double minQuantity = Math.max(2,averageQuantity * config.getMinFractionOfAverage());
		double maxQuantity = averageQuantity * config.getMaxFractionOfAverage();

		// Create an input cluster for each subproblem
		ArrayList<Cluster> clusters = new ArrayList<>();
		for (int i = 0; i < nbSubproblems; i++) {
			Cluster cluster = new Cluster();
			cluster.setMinQuantity(minQuantity);
			cluster.setMaxQuantity(maxQuantity);
			clusters.add(cluster);
		}

		// Original travel matrix is reusable...
		Problem splitProblem = new Problem(pseudoCustomers, clusters, problem.getTravelMatrix());
		return splitProblem;
	}
	
	

}
