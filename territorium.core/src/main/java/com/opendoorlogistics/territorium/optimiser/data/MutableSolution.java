/*******************************************************************************
 * Copyright (c) 2014, 2017 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium.optimiser.data;

import java.util.ArrayList;
import java.util.Collections;

import com.opendoorlogistics.territorium.optimiser.utils.QuantityUtils;
import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.DistanceTime;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;

import gnu.trove.list.array.TIntArrayList;

/**
 * An evaluated solution which can be incrementally updated.
 * 
 * @author Phil
 *
 */
final public class MutableSolution implements ImmutableSolution {
	private final Cost cost = new Cost();
	private final Problem problem;
	private final CustomerRecord[] customers;
	private final ClusterRecord[] clusters;

	private static double calcPreferredPenalty(int clusterIndex,Customer rec) {
		int pref=rec.getPreferredClusterIndex();
		if(pref!=-1 && pref!=clusterIndex) {
			return rec.getPreferredClusterPenaltyCost();
		}
		return 0;
	}
	
	
	private class CustomerRecord {
		private final int index;
		private final Customer customer;
		private double clusterTravelCostIfCustomerIsCentre;
		private ClusterRecord assignedCluster;

		private CustomerRecord(int index) {
			super();
			this.index = index;
			this.customer = problem.getCustomers().get(index);
		}

		private double getQuantity() {
			return customer.getQuantity();
		}

		private int getClusterIndx() {
			return assignedCluster != null ? assignedCluster.clusterIndex : -1;
		}

		@Override
		public String toString() {
			return "id=" + index + ", travelSum=" + clusterTravelCostIfCustomerIsCentre + ", "
					+ (assignedCluster != null ? "cluster " + assignedCluster.clusterIndex : "unassigned");
		}

		// public void clear(){
		// travelSumToSameClusterCustomers=0;
		// cluster=null;
		// }
	}

	private class ClusterRecord {
		private final Cost cost = new Cost();
		private final int clusterIndex;
		private final Cluster cluster;
		private double quantity;
		private CustomerRecord centralCustomer;
		private ArrayList<CustomerRecord> assignedCustomers = new ArrayList<>();
		private double fixedCentreTravelCostToCustomers;

		private ClusterRecord(int id) {
			super();
			this.clusterIndex = id;
			this.cluster = problem.getClusters().get(clusterIndex);
		}

		private Location getCentre() {
			if (isImmutableCentre()) {
				return Cluster.getFixedCentre(cluster);
			}
			return centralCustomer != null ? centralCustomer.customer.getLocation() : null;
		}

		boolean isImmutableCentre() {
			return Cluster.getFixedCentre(cluster) != null;
		}

		@Override
		public String toString() {
			return "id=" + clusterIndex + ", " + cost.toString() + ", quantity=" + quantity + "/"
					+ problem.getClusters().get(clusterIndex) + ", nbCustomers=" + assignedCustomers.size()
					+ (centralCustomer != null ? ", centre=" + centralCustomer.index : "");
		}

		private void getCustomers(TIntArrayList out) {
			out.clear();
			int n = assignedCustomers.size();
			out.ensureCapacity(n);
			;
			for (int i = 0; i < n; i++) {
				out.add(assignedCustomers.get(i).index);
			}
		}
		
		private double getQuantityViolationChangeForQuantityChange(double quantityChange){
			double newQuant = quantity + quantityChange;
			double newViolation = QuantityUtils.getAbsQuantityViolation(cluster, newQuant,problem.getQuantityViolationType());
			double oldViolation = cost.getQuantityViolation();
			
			// check for equals to help ward off rounding error...
			if(oldViolation!=newViolation){
				return newViolation - oldViolation;
			}
			return 0;
		}

		private void updateAll() {
			// Sort assigned customers by index.
			// When we have a target centre and 2 customers, the
			// most central customer is arbitrary (either has equal cost)
			// and is determined by the order of customers in the assigned
			// list. This can create problems, as solution updates can give
			// different costs dependent on the ordering in the customer
			// list. This can cause an infinite loop in the local search
			// where we search until no improvement.
			// We therefore sort customers by index so the update
			// becomes deterministic.
			Collections.sort(assignedCustomers, (c1,c2)->Integer.compare(c1.index, c2.index));;
			
			quantity = 0;
			int nCustomersInCluster = assignedCustomers.size();
			fixedCentreTravelCostToCustomers = 0;
			for (int i = 0; i < nCustomersInCluster; i++) {
				CustomerRecord ci = assignedCustomers.get(i);
				quantity += ci.getQuantity();
				ci.clusterTravelCostIfCustomerIsCentre = 0;

				fixedCentreTravelCostToCustomers += getFixedLocationToCustomerTravelCost(ci);

				for (int j = 0; j < nCustomersInCluster; j++) {
					if (i != j) {
						CustomerRecord cj = assignedCustomers.get(j);
						ci.clusterTravelCostIfCustomerIsCentre += problem.getTravelCost(clusterIndex,ci.customer.getLocation(),
								cj.customer);
					}
				}
			}

			updateCentreAndCost();
			// assert changedCentre || isCostEqualToChecker();
		}

		private boolean isValidState() {
			if(isImmutableCentre()){
				return centralCustomer==null;
			}else{
				return (centralCustomer == null && assignedCustomers.size() == 0)
						|| (centralCustomer != null && assignedCustomers.size() > 0);				
			}
		}


		
		private void updateCentreAndCost() {
			cost.setZero();
			Cluster cluster = problem.getClusters().get(clusterIndex);
			double preferredPenalty =0;
			if (isImmutableCentre()) {
				// set the cost using the sum from the fixed centre
				cost.setCost(fixedCentreTravelCostToCustomers);
			} else {
				// find the central customer and calculate preferred cluster costs
				centralCustomer = null;
				int n = assignedCustomers.size();
				for (int i = 0; i < n; i++) {
					CustomerRecord rec = assignedCustomers.get(i);
					if (centralCustomer == null
							|| rec.clusterTravelCostIfCustomerIsCentre < centralCustomer.clusterTravelCostIfCustomerIsCentre) {
						centralCustomer = rec;
					}
					preferredPenalty += calcPreferredPenalty(clusterIndex,rec.customer);
				}

				// set the travel cost using the central customer
				if (centralCustomer != null) {
					cost.setCost(centralCustomer.clusterTravelCostIfCustomerIsCentre);
				}

			}

			// Add travel cost to the reference location if set
			Location centre = getCentre();
			cost.setCost(cost.getCost() + problem.getTargetToCentreTravelCost(centre, cluster)+ preferredPenalty);

			// Calculate capacity violation
			cost.setQuantityViolation(QuantityUtils.getAbsQuantityViolation(cluster, quantity,problem.getQuantityViolationType()));

			if (!isValidState()) {
				throw new RuntimeException();
			}

		}

		private double getFixedLocationToCustomerTravelCost(CustomerRecord customer) {
			return Cluster.getFixedCentre(cluster) != null
					? problem.getTravelCost(clusterIndex,Cluster.getFixedCentre(cluster), customer.customer) : 0;
		}

		private void insert(CustomerRecord newCustomer) {
			if (!isValidState()) {
				throw new RuntimeException();
			}

			if (newCustomer.assignedCluster != null) {
				throw new RuntimeException();
			}
			newCustomer.assignedCluster = this;
			newCustomer.clusterTravelCostIfCustomerIsCentre = 0;

			// update the distances on all customer records if not using immutable centres
			if (!isImmutableCentre()) {
				int n = assignedCustomers.size();
				for (int i = 0; i < n; i++) {
					CustomerRecord other = assignedCustomers.get(i);
					other.clusterTravelCostIfCustomerIsCentre += problem.getTravelCost(clusterIndex,other.customer.getLocation(),
							newCustomer.customer);
					newCustomer.clusterTravelCostIfCustomerIsCentre += problem
							.getTravelCost(clusterIndex,newCustomer.customer.getLocation(), other.customer);
				}
			}

			fixedCentreTravelCostToCustomers += getFixedLocationToCustomerTravelCost(newCustomer);

			// add the customer including the quantity
			assignedCustomers.add(newCustomer);
			quantity += newCustomer.getQuantity();

			updateCentreAndCost();
		}

		// private double getTravelCost(CustomerRecord cluster, CustomerRecord beingServed){
		// return problem.getTravelCost(problem.getCustomers().get(cluster.index).getLocation(), beingServed.customer);
		// }

		private void remove(CustomerRecord customer2Remove) {
			if (!isValidState()) {
				throw new RuntimeException();
			}

			if (customer2Remove.assignedCluster != this) {
				throw new RuntimeException();
			}

			// find record
			int indx = assignedCustomers.indexOf(customer2Remove);
			if (indx == -1) {
				throw new RuntimeException();
			}

			// remove quantity
			quantity -= customer2Remove.getQuantity();

			// remove record
			assignedCustomers.remove(indx);
			customer2Remove.assignedCluster = null;

			// update distances for all if not using immutable centres
			customer2Remove.clusterTravelCostIfCustomerIsCentre = 0;
			if (isImmutableCentre() == false) {
				int n = assignedCustomers.size();
				for (int i = 0; i < n; i++) {
					CustomerRecord other = assignedCustomers.get(i);
					other.clusterTravelCostIfCustomerIsCentre -= problem.getTravelCost(clusterIndex,other.customer.getLocation(),
							customer2Remove.customer);
				}
			}

			fixedCentreTravelCostToCustomers -= getFixedLocationToCustomerTravelCost(customer2Remove);

			updateCentreAndCost();
		}
	}

	// /**
	// * Copy constructor
	// * @param solution
	// */
	// public MutableLocalSearchSolution( MutableLocalSearchSolution solution){
	// this(solution.problem);
	//
	// // set customers to clusters
	// int nc= getNbCustomers();
	// for(int i =0 ; i< nc ; i++){
	// int clusterIndx = solution.getClusterIndex(i);
	// if(clusterIndx!=-1){
	// CustomerRecord customer = customers[i];
	// customer.cluster = clusters[clusterIndx];
	// customer.cluster.assignedCustomers.add(customer);
	// }
	// }
	//
	// // set cluster centres
	// for(int i =0 ; i<clusters.length;i++){
	// int customerIndx = solution.getClusterCentre(i);
	// if(customerIndx!=-1){
	// clusters[i].centralCustomer = customers[customerIndx];
	// }
	// }
	//
	// // update all counts etc
	// update();
	// }

	/**
	 * Constructor which does all object allocation
	 * 
	 * @param problem
	 */
	private MutableSolution(Problem problem) {
		// allocate all objects
		this.problem = problem;
		this.customers = new CustomerRecord[problem.getCustomers().size()];
		for (int i = 0; i < customers.length; i++) {
			customers[i] = new CustomerRecord(i);
		}

		this.clusters = new ClusterRecord[problem.getClusters().size()];
		for (int i = 0; i < clusters.length; i++) {
			clusters[i] = new ClusterRecord(i);
		}
	}

	/**
	 * Deep copy the solution. Cluster centres will be updated automatically and so can change. All stats will be
	 * recalculated.
	 * 
	 * @param deepCopyThis
	 */
	public MutableSolution(ImmutableSolution deepCopyThis) {
		this(deepCopyThis.getProblem(), deepCopyThis.getCustomersToClusters());
	}

	public MutableSolution(Problem problem, int[] customersToClusters) {
		this(problem);
		if (customersToClusters != null) {
			if (customersToClusters.length != customers.length) {
				throw new RuntimeException();
			}
			for (int i = 0; i < customers.length; i++) {
				int cluster = customersToClusters[i];
				if (cluster >= 0) {
					CustomerRecord customer = customers[i];
					customer.assignedCluster = clusters[cluster];
					customer.assignedCluster.assignedCustomers.add(customer);
				}
			}
		}
		// if(centres.length!=problem.getClusters().size()){
		// throw new RuntimeException();
		// }

		// for(int i =0 ; i < centres.length ; i++){
		// if(centres[i]!=-1){
		// int customerIndx = centres[i];
		// if(customers[customerIndx].cluster!=null){
		// throw new RuntimeException();
		// }
		//
		// // assign customer to cluster and set this as its centre
		// CustomerRecord customer = customers[customerIndx];
		// customer.cluster = clusters[i];
		// customer.cluster.assignedCustomers.add(customer);
		// clusters[i].centralCustomer = customer;
		// }
		// }
		//
		update();
	}

	// private void debugCheck(){
	// update();
	// }

	// private static int DEBUG_CALL_NB = 0;

	/**
	 * Get the cost of setting the customer to the cluster. If the cluster index =-1, then customer is unloaded.
	 * 
	 * @param customerIndx
	 * @param newClusterIndx
	 * @param outCostAfterSet
	 */
	public void evaluateSet(int customerIndx, int newClusterIndx, Cost outCostAfterSet) {

		// get the customer record
		CustomerRecord customer = customers[customerIndx];

		// save original cluster
		int originalClusterIndx = customer.getClusterIndx();

		// set new position
		setCustomerToCluster(customerIndx, newClusterIndx);

		// add new cost to the return object
		outCostAfterSet.set(cost);

		// revert
		setCustomerToCluster(customerIndx, originalClusterIndx);

	}


	/**
	 * Get an estimate of the swap cost.
	 * Actual cost could be higher if (and only if) one of the customers is already the centre of the cluster.
	 * Otherwise it could be the same or lower.
	 * @param customerIndx1
	 * @param customerIndx2
	 * @param outCost
	 */
	public void estimateSwapCostWithoutChangingCentres(int customerIndx1, int customerIndx2, Cost outCost) {
		// set to current cost initially
		outCost.set(cost);
		
		// get current clusters and return if they're already in the same cluster
		int originalClustIndx4Cust1 = customers[customerIndx1].getClusterIndx();
		int originalClustIndx4Cust2 = customers[customerIndx2].getClusterIndx();
		if (originalClustIndx4Cust1 == originalClustIndx4Cust2) {
			return;
		}

		if (originalClustIndx4Cust1 == -1 || originalClustIndx4Cust2 == -1) {
			throw new RuntimeException("Can only swap customers already on customers");
		}

		Customer customer1 = problem.getCustomers().get(customerIndx1);
		Customer customer2 = problem.getCustomers().get(customerIndx2);

		ClusterRecord originalClustObj4Cust1 = clusters[originalClustIndx4Cust1];
		ClusterRecord originalClustObj4Cust2 = clusters[originalClustIndx4Cust2];
		
		// get change in quantity violation
		double quantViolation = outCost.getQuantityViolation();
		quantViolation += originalClustObj4Cust1.getQuantityViolationChangeForQuantityChange(customer2.getQuantity() - customer1.getQuantity());
		quantViolation += originalClustObj4Cust2.getQuantityViolationChangeForQuantityChange(customer1.getQuantity() - customer2.getQuantity());
		outCost.setQuantityViolation(quantViolation);
		
		// get change in travel cost
		double travelCost = outCost.getCost();
		travelCost -= getCostWithCentreUnchanged( customer1, originalClustObj4Cust1);
		
		travelCost -= getCostWithCentreUnchanged( customer2, originalClustObj4Cust2);
	
		travelCost += getCostWithCentreUnchanged( customer2, originalClustObj4Cust1);
				
		travelCost += getCostWithCentreUnchanged( customer1, originalClustObj4Cust2);
				
		outCost.setCost(travelCost);
		
	}

	private double getCostWithCentreUnchanged( Customer customer,
			ClusterRecord clusterRec) {
		return problem.getTravelCost(clusterRec.clusterIndex,clusterRec.getCentre(), customer)
				+ calcPreferredPenalty(clusterRec.clusterIndex,customer);
	}

	/**
	 * Evaluate the swap between 2 customers currently on clusters
	 * 
	 * @param customerIndx1
	 * @param customerIndx2
	 * @param outCostAfterSwap
	 */
	public void evaluateSwap(int customerIndx1, int customerIndx2, Cost outCostAfterSwap) {

		// get current clusters
		int originalCluster1 = customers[customerIndx1].getClusterIndx();
		int originalCluster2 = customers[customerIndx2].getClusterIndx();
		if (originalCluster1 == originalCluster2) {
			outCostAfterSwap.set(cost);
			return;
		}

		if (originalCluster1 == -1 || originalCluster2 == -1) {
			throw new RuntimeException();
		}

		// swap
		setCustomerToCluster(customerIndx1, originalCluster2);
		setCustomerToCluster(customerIndx2, originalCluster1);

		// set new cost to the return object
		outCostAfterSwap.set(cost);

		// finally revert
		setCustomerToCluster(customerIndx1, originalCluster1);
		setCustomerToCluster(customerIndx2, originalCluster2);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getClusterIndex(int)
	 */
	@Override
	public int getClusterIndex(int customerIndx) {
		return customers[customerIndx].getClusterIndx();
		// if(clusterIndx!=-1){
		// return clusters[clusterIndx].centre.id;
		// }
		// return -1;
	}

	// /* (non-Javadoc)
	// * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getClusterSize(int)
	// */
	// @Override
	// public int getClusterSize(int clusterIndx){
	// return clusters[clusterIndx].assignedCustomers.size();
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getCustomer(int, int)
	 */
	@Override
	public int getCustomer(int clusterIndx, int indx) {
		return clusters[clusterIndx].assignedCustomers.get(indx).index;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getCustomers(int,
	 * gnu.trove.list.array.TIntArrayList)
	 */
	@Override
	public void getCustomers(int clusterIndx, TIntArrayList out) {
		clusters[clusterIndx].getCustomers(out);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getCustomersToClusters()
	 */
	@Override
	public int[] getCustomersToClusters() {
		int[] ret = new int[customers.length];
		for (CustomerRecord rec : customers) {
			ret[rec.index] = rec.getClusterIndx();
		}
		return ret;
	}

	public void setCustomerToCluster(int customerIndx, int cluster) {
		// debugCheck();

		// get customer record and original and destination records
		CustomerRecord customer = customers[customerIndx];
		ClusterRecord original = customer.assignedCluster;
		ClusterRecord destination = cluster == -1 ? null : clusters[cluster];

		if (original == destination) {
			// do nothing
			return;
		}

		// // check we're not moving a fixed centre to a different cluster
		// int fixedClusterIndx = problem.getFixedClusterIndexByLocationIndex(customerIndx);
		// if(fixedClusterIndx!=-1 && fixedClusterIndx!=cluster){
		// throw new RuntimeException();
		// }

		// remove costs of involved clusters
		if (original != null) {
			cost.subtract(original.cost);
		}
		if (destination != null) {
			cost.subtract(destination.cost);
		}

		// remove if needed
		if (original != null) {
			original.remove(customer);
		}

		// insert if needed
		if (destination != null) {
			destination.insert(customer);
		}

		// re-add costs of involved clusters
		if (original != null) {
			cost.add(original.cost);
		}
		if (destination != null) {
			cost.add(destination.cost);
		}

		// debugCheck();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getCost()
	 */
	@Override
	public Cost getCost() {
		return cost;
	}

	// /* (non-Javadoc)
	// * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getClusterCost(int)
	// */
	// @Override
	// public Cost getClusterCost(int clusterIndx){
	// return clusters[clusterIndx].cost;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getClusterQuantity(int)
	 */
	// @Override
	// public double getClusterQuantity(int clusterIndex){
	// return clusters[clusterIndex].quantity;
	// }

	// /* (non-Javadoc)
	// * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getClusterLocationCount(int)
	// */
	// @Override
	// public int getClusterLocationCount(int clusterIndex){
	// return clusters[clusterIndex].assignedCustomers.size();
	// }

	/**
	 * Update all internal variables except assignment of customers to clusters. Cluster centres can change and hence
	 * cost can change.
	 */
	public void update() {
		// assert countImmutableCentres()==0 || saveCostChecker();

		cost.setZero();
		for (ClusterRecord cluster : clusters) {
			cluster.updateAll();
			cost.add(cluster.cost);
		}

		// assert countImmutableCentres()==0 ||isCostEqualToChecker();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(cost.toString() + System.lineSeparator());
		builder.append(System.lineSeparator());
		for (ClusterRecord cluster : clusters) {
			builder.append(cluster + System.lineSeparator());
			builder.append("Centre:" + cluster.getCentre() + System.lineSeparator());
			for (CustomerRecord customer : cluster.assignedCustomers) {
				builder.append("\t" + customer + System.lineSeparator());
			}
			builder.append(System.lineSeparator());
		}
		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getClusterCentre(int)
	 */
	@Override
	public Location getClusterCentre(int i) {
		return clusters[i].getCentre();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getClusterCentres()
	 */
	@Override
	public Location[] getClusterCentres() {
		Location[] ret = new Location[clusters.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = clusters[i].getCentre();
		}
		return ret;
	}

	// /* (non-Javadoc)
	// * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getQuantity(int)
	// */
	// @Override
	// public double getQuantity(int clusterIndx){
	// return clusters[clusterIndx].quantity;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.territorium.solver.ImmutableSolution#getNbUnassignedCustomers()
	 */
	@Override
	public int getNbUnassignedCustomers() {
		int ret = 0;
		for (CustomerRecord rec : customers) {
			if (rec.assignedCluster == null) {
				ret++;
			}
		}
		return ret;
	}

	@Override
	public Problem getProblem() {
		return problem;
	}

	@Override
	public double getClusterQuantity(int clusterIndx) {
		return clusters[clusterIndx].quantity;
	}

	@Override
	public int getNbCustomers(int clusterIndx) {
		return clusters[clusterIndx].assignedCustomers.size();
	}

	@Override
	public Cost getClusterCost(int clusterIndx) {
		return clusters[clusterIndx].cost;
	}

}
