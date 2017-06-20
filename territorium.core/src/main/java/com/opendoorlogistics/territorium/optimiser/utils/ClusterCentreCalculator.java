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
package com.opendoorlogistics.territorium.optimiser.utils;

import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;

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
		if(Cluster.getFixedCentre(cluster)!=null){
			return Cluster.getFixedCentre(cluster);
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
