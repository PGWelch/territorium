package com.opendoorlogistics.territorium.optimiser.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;
import com.opendoorlogistics.territorium.utils.NumberUtils;

public class RandomisedCentreSelector {
	private final Problem problem;
	private final Random random;
	private final Config config;
	
	
	public RandomisedCentreSelector(Problem problem, Random random, Config config) {
		this.problem = problem;
		this.random = random;
		this.config = config;
	}


	public static class Config{
		public static double DEFAULT_BEST_CANDIDATE_SELECTIVITY= 0.5;
		private double bestCandidateSelectivity=DEFAULT_BEST_CANDIDATE_SELECTIVITY;
		public double getBestCandidateSelectivity() {
			return bestCandidateSelectivity;
		}
		public void setBestCandidateSelectivity(double bestCandidateSelectivity) {
			this.bestCandidateSelectivity = bestCandidateSelectivity;
		}
		
		
	}
	
	private static class CustomerRec{
		
		CustomerRec(int index, Customer customer) {
			this.index = index;
			this.customer = customer;
		}
		final int index;
		final Customer customer;
		double minCostToCluster;
	}
	
	public Location[] run(Location[] clusterLocations, int[] assignedClusterIndexByCustomer) {
		List<? extends Cluster> clusters = problem.getClusters();
		int p = clusters.size();
		if(clusterLocations!=null && clusterLocations.length!=p){
			throw new IllegalArgumentException();
		}
		
		// copy over already assigned ones
		Location [] ret = new Location[p];
		for(int clusterIndx =0;clusterIndx<p;clusterIndx++){
			ret[clusterIndx] = Cluster.getFixedCentre(clusters.get(clusterIndx));
			if(ret[clusterIndx]==null && clusterLocations!=null){
				ret[clusterIndx] = clusterLocations[clusterIndx];
			}
		}
		
		// init array of customers to min travel cost to existing cluster
		int nc = problem.getCustomers().size();
		CustomerRec [] customers = new CustomerRec[nc];
		for(int i=0;i< nc ; i++){
			customers[i] = new CustomerRec(i,problem.getCustomers().get(i));
			customers[i].minCostToCluster = Double.POSITIVE_INFINITY;
		}
		for(int clusterIndx =0;clusterIndx<p;clusterIndx++){
			if(ret[clusterIndx]!=null){
				for(int i=0;i< nc ; i++){
					double cost = problem.getTravelCost(ret[clusterIndx],customers[i].customer);
					customers[i].minCostToCluster = Math.min(customers[i].minCostToCluster, cost);
				}				
			}
		}

		// loop over all clusters in random order
		for(int clusterIndx : NumberUtils.getRandomOrder0ToNArray(random, p-1)){
			
			// skip already assigned
			if(ret[clusterIndx]!=null){
				continue;
			}
			
			// sort customers by best first
			Cluster cluster = problem.getClusters().get(clusterIndx);
			int[] orderedCustomerIndices = sortCandidateLocations(cluster, customers);
			
			// choose randomly from the best 0.5/nclusters
			int limit = (int)Math.ceil(config.getBestCandidateSelectivity() * orderedCustomerIndices.length / p);
			limit = Math.min(orderedCustomerIndices.length, limit);
			int customerIndex = orderedCustomerIndices[0];
			if(limit >=2){
				customerIndex = orderedCustomerIndices[random.nextInt(limit)];
			}
			
			// set the cluster
			ret[clusterIndx] = customers[customerIndex].customer.getLocation();
			
			// update the min distances
			for(int i=0;i< nc ; i++){
				customers[i].minCostToCluster = Math.min(customers[i].minCostToCluster, problem.getTravelCost(ret[clusterIndx],customers[i].customer));
			}
		}
		
		return ret;
	}
	
	private int [] sortCandidateLocations(Cluster cluster,CustomerRec [] customers){
		class SortRecord{
			CustomerRec customer;
			double normalisedClosestClusterScore;
			double dist2RefLocation;
			double normalisedClosest2RefLocScore;
			double randNb;
			
			double score(){
				return normalisedClosestClusterScore + normalisedClosest2RefLocScore;
			}
		}
		
		int nc = customers.length;
		ArrayList<SortRecord> recs = new ArrayList<>(nc);
		for(int i=0 ; i<nc;i++){
			SortRecord rec= new SortRecord();
			rec.customer = customers[i];
			rec.randNb= random.nextDouble();
			recs.add(rec);
		}
		
		recs.sort((o1,o2)->Double.compare(o2.customer.minCostToCluster, o1.customer.minCostToCluster));
		recs.sort(new Comparator<SortRecord>() {

			@Override
			public int compare(SortRecord o1, SortRecord o2) {
				// sort to get normalised score by min distance to cluster (1 = furthest from cluster) 
				// Highest distance comes first
				int diff = Double.compare(o2.customer.minCostToCluster, o1.customer.minCostToCluster);
				
				// If diff is the same (i.e. when no clusters exist yet), sort by the random number
				if(diff==0){
					diff = Double.compare(o1.randNb, o2.randNb);
				}
				return diff;
			}
		});
		int rank=0;
		double fraction = 1.0/ (recs.size()-1);
		for(SortRecord rec:recs){
			rec.normalisedClosestClusterScore= 1- (double) rank *fraction;
			rank++;
		}
		
		// get distance to ref location
		if(cluster.getTargetCentre()!=null){
			for(SortRecord rec:recs){
				rec.dist2RefLocation = problem.getTravelCost(cluster.getTargetCentre(), rec.customer.customer);			
			}
			// smallest distance comes first
			recs.sort((o1,o2)->Double.compare(o1.dist2RefLocation, o2.dist2RefLocation));
	
			rank =0 ;
			for(SortRecord rec:recs){
				rec.normalisedClosestClusterScore= 1- (double) rank *fraction;
				rank++;
			}
		}
		
		// now sort by total score, highest first
		recs.sort( (o1,o2) ->Double.compare(o2.score(), o1.score()));
		
		// return sorted customer indices
		int [] ret = new int[nc];
		for(int i =0 ; i<nc ; i++){
			ret[i] = recs.get(i).customer.index;
		}
		return ret;
	}

}
