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
package com.opendoorlogistics.territorium.optimiser.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.ObjectWithJSONToString;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;

public class CustomerClusterCostMatrix extends ObjectWithJSONToString{
	private double [][]costs;
	private Problem problem;
	
	public CustomerClusterCostMatrix(Problem problem, Location [] clusterCentres) {
		this.problem = problem;
		int nc = problem.getCustomers().size();

		costs = new double[nc][];
		
		int p = clusterCentres.length;

		for (int customerIndx = 0; customerIndx < nc; customerIndx++) {
			Customer customer = problem.getCustomers().get(customerIndx);
			costs[customerIndx] = new double[p];
			for (int clusterIndx = 0; clusterIndx < p; clusterIndx++) {
				costs[customerIndx][clusterIndx] = Double.POSITIVE_INFINITY;
				if (clusterCentres[clusterIndx] != null) {
					costs[customerIndx][clusterIndx] = problem
							.getTravelCost(clusterCentres[clusterIndx], customer);
				}
			}
		}
	}

	@JsonIgnore
	public double cost(int customerIndx, int clusterIndx){
		return costs[clusterIndx][clusterIndx];
	}

	public double[][] getCosts() {
		return costs;
	}

	public void setCosts(double[][] costs) {
		this.costs = costs;
	}

	public Problem getProblem() {
		return problem;
	}

	public void setProblem(Problem problem) {
		this.problem = problem;
	}
	
	@JsonIgnore
	public double [] getCosts(int customerIndx){
		return costs[customerIndx];
	}
	
	public void updateClusterCentre(int clusterIndx, Location location){
		int nc=problem.getCustomers().size();
		for (int customerIndx = 0; customerIndx < nc; customerIndx++) {
			if (location!= null) {
				costs[customerIndx][clusterIndx] = problem
						.getTravelCost(location, problem.getCustomers().get(customerIndx));
			}else{
				costs[customerIndx][clusterIndx] = Double.POSITIVE_INFINITY;
			}
		}
	}
	
}
