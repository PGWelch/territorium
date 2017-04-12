package com.opendoorlogistics.territorium.solver.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opendoorlogistics.territorium.problem.data.Customer;
import com.opendoorlogistics.territorium.problem.data.Location;
import com.opendoorlogistics.territorium.problem.data.ObjectWithJSONToString;
import com.opendoorlogistics.territorium.problem.data.Problem;

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
