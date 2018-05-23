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

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.opendoorlogistics.territorium.optimiser.data.CustomerClusterCostMatrix;
import com.opendoorlogistics.territorium.optimiser.utils.QuantityUtils;
import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.ObjectWithJSONToString;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;
import com.opendoorlogistics.territorium.utils.NumberUtils;

public class RandomisedWeightBasedCustomerAssignment {
	private static final Logger LOGGER = Logger.getLogger(RandomisedWeightBasedCustomerAssignment.class.getName());

	private final Problem problem;
	private final Random random;
	private final Config config;

	public static class Config {
		public static double DEFAULT_MIN_MULTIPLIER = 0.1;
		public static double DEFAULT_MAX_MULTIPLIER = 10;
		public static double DEFAULT_MIN_STEP_SIZE = 0.01;
		public static double DEFAULT_STEP_SIZE_MULTIPLIER = 1.5;
		public static int DEFAULT_NB_ITERATIONS = 5;

		private double minMultiplier = DEFAULT_MIN_MULTIPLIER;
		private double maxMultiplier = DEFAULT_MAX_MULTIPLIER;
		private double minSepSize = DEFAULT_MIN_STEP_SIZE;
		private double stepSizeMultiplier = DEFAULT_STEP_SIZE_MULTIPLIER;
		private int nbIterations = DEFAULT_NB_ITERATIONS;

		public double getMinMultiplier() {
			return minMultiplier;
		}

		public void setMinMultiplier(double minMultiplier) {
			this.minMultiplier = minMultiplier;
		}

		public double getMaxMultiplier() {
			return maxMultiplier;
		}

		public void setMaxMultiplier(double maxMultiplier) {
			this.maxMultiplier = maxMultiplier;
		}

		public double getMinSepSize() {
			return minSepSize;
		}

		public void setMinSepSize(double minSepSize) {
			this.minSepSize = minSepSize;
		}

		public double getStepSizeMultiplier() {
			return stepSizeMultiplier;
		}

		public void setStepSizeMultiplier(double stepSizeMultiplier) {
			this.stepSizeMultiplier = stepSizeMultiplier;
		}

		public int getNbIterations() {
			return nbIterations;
		}

		public void setNbIterations(int nbIterations) {
			this.nbIterations = nbIterations;
		}

	}

	public RandomisedWeightBasedCustomerAssignment(Problem problem, Config config, Random random) {
		this.problem = problem;
		this.random = random;
		this.config = config;

		if (config.getMinSepSize() <= 0 || config.getStepSizeMultiplier() <= 1 || config.getNbIterations() < 0) {
			throw new IllegalArgumentException();
		}
	}

	private int getBestClusterIndex(double[] costs, double[] multiplier) {
		double best = Double.POSITIVE_INFINITY;
		int bestIndex = -1;
		for (int i = 0; i < costs.length; i++) {
			double cost = multiplier[i] * costs[i];
			if (cost < best) {
				bestIndex = i;
				best = cost;
			}
		}
		return bestIndex;
	}

	static class Blackboard extends ObjectWithJSONToString {
		private double[] multipliers;
		private ImmutableData immutable;
		private SolutionState solnState;

		public double[] getMultipliers() {
			return multipliers;
		}

		public void setMultipliers(double[] multipliers) {
			this.multipliers = multipliers;
		}

		public ImmutableData getImmutable() {
			return immutable;
		}

		public void setImmutable(ImmutableData immutable) {
			this.immutable = immutable;
		}

		public SolutionState getSolnState() {
			return solnState;
		}

		public void setSolnState(SolutionState solnState) {
			this.solnState = solnState;
		}

	}

	static class ImmutableData extends ObjectWithJSONToString {
		private List<? extends Cluster> clusters;
		private List<? extends Customer> customers;
		private Location[] clusterLocations;
		private int[] assignedClusterIndexByCustomer;
		private CustomerClusterCostMatrix noMultiplierCustomer2ClusterCost;

		public List<? extends Cluster> getClusters() {
			return clusters;
		}

		public void setClusters(List<? extends Cluster> clusters) {
			this.clusters = clusters;
		}

		public List<? extends Customer> getCustomers() {
			return customers;
		}

		public void setCustomers(List<? extends Customer> customers) {
			this.customers = customers;
		}

		public Location[] getClusterLocations() {
			return clusterLocations;
		}

		public void setClusterLocations(Location[] clusterLocations) {
			this.clusterLocations = clusterLocations;
		}

		public int[] getAssignedClusterIndexByCustomer() {
			return assignedClusterIndexByCustomer;
		}

		public void setAssignedClusterIndexByCustomer(int[] assignedClusterIndexByCustomer) {
			this.assignedClusterIndexByCustomer = assignedClusterIndexByCustomer;
		}

		public CustomerClusterCostMatrix getNoMultiplierCustomer2ClusterCost() {
			return noMultiplierCustomer2ClusterCost;
		}

		public void setNoMultiplierCustomer2ClusterCost(CustomerClusterCostMatrix noMultiplierCustomer2ClusterCost) {
			this.noMultiplierCustomer2ClusterCost = noMultiplierCustomer2ClusterCost;
		}

	}

	static class SolutionState extends ObjectWithJSONToString {
		private double[] quantities;
		private int[] bestCluster;
		private double[] quantityViolations;
		private double sumAbsQuantityViolation;

		SolutionState(int nc, int p) {
			bestCluster = new int[nc];
			quantities = new double[p];
			quantityViolations = new double[p];
		}

		void copyOntoMe(SolutionState copyOntoMe) {
			System.arraycopy(copyOntoMe.quantities, 0, quantities, 0, copyOntoMe.quantities.length);
			System.arraycopy(copyOntoMe.bestCluster, 0, bestCluster, 0, copyOntoMe.bestCluster.length);
			System.arraycopy(copyOntoMe.quantityViolations, 0, quantityViolations, 0,
					copyOntoMe.quantityViolations.length);
			this.sumAbsQuantityViolation = copyOntoMe.sumAbsQuantityViolation;
		}

		public double[] getQuantities() {
			return quantities;
		}

		public void setQuantities(double[] quantities) {
			this.quantities = quantities;
		}

		public int[] getBestCluster() {
			return bestCluster;
		}

		public void setBestCluster(int[] bestCluster) {
			this.bestCluster = bestCluster;
		}

		public double[] getQuantityViolations() {
			return quantityViolations;
		}

		public void setQuantityViolations(double[] quantityViolations) {
			this.quantityViolations = quantityViolations;
		}

		public double getSumAbsQuantityViolation() {
			return sumAbsQuantityViolation;
		}

		public void setSumAbsQuantityViolation(double sumAbsQuantityViolation) {
			this.sumAbsQuantityViolation = sumAbsQuantityViolation;
		}

	}

	/**
	 * 
	 * @param clusterLocations
	 * @param assignedClusterIndexByCustomer
	 * @return Assigned cluster index for each customer
	 */
	public int[] run(Location[] clusterLocations, int[] assignedClusterIndexByCustomer) {
		if (clusterLocations.length != problem.getClusters().size()) {
			throw new IllegalArgumentException();
		}
		// create an empty solution with the fixed centres (this assigns the customers which are centres)

		// setup immutable data
		ImmutableData immutable = new ImmutableData();
		immutable.clusterLocations = clusterLocations;
		immutable.assignedClusterIndexByCustomer = assignedClusterIndexByCustomer;
		// initialise travel costs for all customers (cluster centres are already assigned)
		immutable.clusters = problem.getClusters();
		immutable.customers = problem.getCustomers();
		int p = immutable.clusters.size();
		int nc = immutable.customers.size();
		immutable.customers = problem.getCustomers();
		immutable.noMultiplierCustomer2ClusterCost  = new CustomerClusterCostMatrix(problem, clusterLocations);

		Blackboard bb = new Blackboard();
		bb.immutable = immutable;

		bb.multipliers = new double[p];
		Arrays.fill(bb.multipliers, 1);

		// init the solution state
		bb.solnState = new SolutionState(nc, p);
		updateState(bb.immutable, bb.multipliers, bb.solnState);

		for (int iteration = 0; iteration < config.getNbIterations(); iteration++) {
			singleStep(bb);
		}

		return bb.solnState.bestCluster;
	}

	/**
	 * Loop over all clusters in random order, systematically varying their
	 * transport cost multipliers so if all customers are assigned to their 
	 * cluster with cheapest transport cost, the quantity violation is minimised
	 * @param bb
	 */
	private void singleStep(Blackboard bb) {
		ImmutableData immutable = bb.immutable;
		int p = immutable.clusters.size();
		int nc = immutable.customers.size();
		SolutionState testState = new SolutionState(nc, p);

		// loop over clusters in random order
		for (int clusterIndx : NumberUtils.getRandomOrder0ToNArray(random, p - 1)) {
			double currentMultiplier = bb.multipliers[clusterIndx];
			double bestMultiplier = currentMultiplier;
			double bestAbsQuantityViolation = Double.POSITIVE_INFINITY;

			// get multipliers to try
			TreeSet<Double> toTry = new TreeSet<>();
			for (int sign = -1; sign <= +1; sign += 2) {
				double low = 0;
				double high = config.getMinSepSize() * config.getStepSizeMultiplier();
				double testMultiplier = currentMultiplier;
				while (true) {

					// test for exit
					double minChange = testMultiplier + sign * low;
					if (minChange < config.getMinMultiplier() || minChange > config.getMaxMultiplier()) {
						break;
					}

					double rand = random.nextDouble() * (high - low) + low;
					testMultiplier += sign * rand;

					toTry.add(testMultiplier);

					low = high;
					high = low * config.getStepSizeMultiplier();
				}
			}
			
			// include current otherwise solution can get worse!
			toTry.add(currentMultiplier);

			for (double testMultiplier : toTry) {
				// copy over test quantities
				testState.copyOntoMe(bb.solnState);

				bb.multipliers[clusterIndx] = testMultiplier;

				// check for any reassignment
				for (int i = 0; i < nc; i++) {
					if (immutable.assignedClusterIndexByCustomer == null
							|| immutable.assignedClusterIndexByCustomer[i] == -1) {
						int oldClusterIndx = bb.solnState.bestCluster[i];
						int newClusterIndx = getBestClusterIndex(immutable.noMultiplierCustomer2ClusterCost.getCosts(i),
								bb.multipliers);
						if (newClusterIndx != oldClusterIndx) {
							if (oldClusterIndx != -1) {
								testState.quantities[oldClusterIndx] -= immutable.customers.get(i).getQuantity();
							}
							if (newClusterIndx != -1) {
								testState.quantities[newClusterIndx] += immutable.customers.get(i).getQuantity();
							}
						}
					}
				}

				updateQuantityViolation(immutable, testState);

				// LOGGER.info("...Cluster " + clusterIndx + " multiplier=" + testMultiplier + " quantviolation=" +
				// testState.sumAbsQuantityViolation);

				if (testState.sumAbsQuantityViolation < bestAbsQuantityViolation) {
					bestAbsQuantityViolation = testState.sumAbsQuantityViolation;
					bestMultiplier = testMultiplier;
				}

			}

		//	LOGGER.info("Cluster " + clusterIndx + " oldMultiplier=" + currentMultiplier + " newMultiplier="
		//			+ bestMultiplier + " newQuantViolation=" + bb.solnState.sumAbsQuantityViolation);

			bb.multipliers[clusterIndx] = bestMultiplier;
			updateState(immutable, bb.multipliers, bb.solnState);

		}
	}

	private void updateState(ImmutableData immutable, double[] multipliers, SolutionState state) {
		int nc = state.bestCluster.length;
		for (int i = 0; i < nc; i++) {
			state.bestCluster[i] = immutable.assignedClusterIndexByCustomer != null
					? immutable.assignedClusterIndexByCustomer[i] : -1;
			if (state.bestCluster[i] == -1) {
				state.bestCluster[i] = getBestClusterIndex(immutable.noMultiplierCustomer2ClusterCost.getCosts(i), multipliers);
			}
		}

		// calculate initial quantity assuming each customer assigned to its best cluster
		Arrays.fill(state.quantities, 0);
		for (int i = 0; i < nc; i++) {
			if (state.bestCluster[i] != -1) {
				state.quantities[state.bestCluster[i]] += immutable.customers.get(i).getQuantity();
			}
		}

		updateQuantityViolation(immutable, state);
	}

	private void updateQuantityViolation(ImmutableData immutable, SolutionState state) {
		// get initial violations
		state.sumAbsQuantityViolation = 0;
		for (int i = 0; i < state.quantityViolations.length; i++) {
			state.quantityViolations[i] = QuantityUtils.getAbsQuantityViolation(immutable.clusters.get(i),
					state.quantities[i], problem.getQuantityViolationType());
			state.sumAbsQuantityViolation += state.quantityViolations[i];
		}
	}



}
