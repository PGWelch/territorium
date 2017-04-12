package com.opendoorlogistics.territorium.solver;

import com.opendoorlogistics.territorium.problem.data.Cluster;
import com.opendoorlogistics.territorium.problem.data.Customer;
import com.opendoorlogistics.territorium.problem.data.Location;
import com.opendoorlogistics.territorium.problem.data.Problem;

import gnu.trove.list.array.TIntArrayList;

public class ClusterCentreCalculator {
	/**
	 * Cluster centre is most central customer
	 * @param problem
	 * @param cluster
	 * @param assignedCustomers
	 * @return
	 */
	public static Location calculate(Problem problem,Cluster cluster, int [] assignedCustomers){
		if(cluster.getFixedLocation()!=null){
			return cluster.getFixedLocation();
		}
		
		if(assignedCustomers.length==0){
			return null;
		}

		// choose the customer with min travel including travel to reference location
		double minCost = Double.POSITIVE_INFINITY;
		Location minCostLoc=null;
		for(int i:assignedCustomers){
			Customer ci = problem.getCustomers().get(i);
			double cost=0;
			for(int j:assignedCustomers){
				cost += problem.getTravelCost(ci.getLocation(), problem.getCustomers().get(j));
			}
			
			if(cost < minCost){
				minCost = cost;
				minCostLoc = ci.getLocation();
			}
		}

		return minCostLoc;
	}
	
	public static int [][] splitCustomersByCluster(Problem problem, int []customerToClusterIndx){
		int p=problem.getClusters().size();
		TIntArrayList [] byCluster = new TIntArrayList[p];
		for(int i=0;i<p;i++){
			byCluster[i] = new TIntArrayList();
		}
		for(int i =0 ; i<customerToClusterIndx.length ; i++){
			byCluster[customerToClusterIndx[i]].add(i);
		}
		int [][] ret = new int[p][];
		for(int i=0;i<p;i++){
			ret[i] = byCluster[i].toArray();
		}
		return ret;
	}
	
	public static Location [] calculate(Problem problem, int []customerToClusterIndx){
		// split customer indices by cluster
		int p=problem.getClusters().size();
		int [][] split = splitCustomersByCluster(problem, customerToClusterIndx);
		Location [] ret = new Location[p];
		for(int i=0;i<p;i++){
			ret[i] = calculate(problem, problem.getClusters().get(i), split[i]);
		}
		return ret;
	}
}
