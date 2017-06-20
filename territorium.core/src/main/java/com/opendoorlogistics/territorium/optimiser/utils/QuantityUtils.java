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
