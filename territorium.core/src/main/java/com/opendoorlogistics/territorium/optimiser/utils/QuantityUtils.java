package com.opendoorlogistics.territorium.optimiser.utils;

import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Problem;

public class QuantityUtils {
	public static double getAbsQuantityViolation(Cluster cluster, double quantity){
		if(quantity < cluster.getMinQuantity()){
			return cluster.getMinQuantity() - quantity;
		}
		else if(quantity > cluster.getMaxQuantity()){
			return quantity - cluster.getMaxQuantity();
		}
		return 0;
	}

	public static double getTotalQuantity(Problem problem){
		return getTotalQuantity(problem.getCustomers());
	}
	
	public static double getTotalQuantity(Iterable<Customer> customers){
		double ret=0;
		for(Customer c:customers){
			ret+=c.getQuantity();
		}
		return ret;
	}
}
