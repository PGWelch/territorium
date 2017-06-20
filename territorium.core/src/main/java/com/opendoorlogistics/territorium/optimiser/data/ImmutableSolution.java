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

import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;

import gnu.trove.list.array.TIntArrayList;

public interface ImmutableSolution {
	
	Problem getProblem();

	int getClusterIndex(int customerIndx);

	//int getClusterSize(int clusterIndx);

	int getCustomer(int clusterIndx, int indx);
	
	int getNbCustomers(int clusterIndx);

	void getCustomers(int clusterIndx, TIntArrayList out);

	int[] getCustomersToClusters();

	Cost getCost();

	//Cost getClusterCost(int clusterIndx);

	//double getClusterQuantity(int clusterIndex);

	//int getClusterLocationCount(int clusterIndex);

	Location getClusterCentre(int i);

	Location[] getClusterCentres();
	
	double getClusterQuantity(int clusterIndx);

	Cost getClusterCost(int clusterIndx);

	//double getQuantity(int clusterIndx);

	int getNbUnassignedCustomers();

}