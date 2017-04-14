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
	
	//double getQuantity(int clusterIndx);

	int getNbUnassignedCustomers();

}