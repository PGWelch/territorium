package com.opendoorlogistics.territorium.solver;

import com.opendoorlogistics.territorium.problem.data.Cluster;

public class SolverUtils {
	public static double getAbsQuantityViolation(Cluster cluster, double quantity){
		if(quantity < cluster.getMinQuantity()){
			return cluster.getMinQuantity() - quantity;
		}
		else if(quantity > cluster.getCapacity()){
			return quantity - cluster.getCapacity();
		}
		return 0;
	}

}
