package com.opendoorlogistics.territorium.solver;

import com.opendoorlogistics.territorium.problem.data.Location;
import com.opendoorlogistics.territorium.problem.data.Problem;
import com.opendoorlogistics.territorium.solver.data.Cost;

import gnu.trove.list.array.TIntArrayList;

public interface ImmutableSolution {
	
	Problem getProblem();

	int getClusterIndex(int customerIndx);

	//int getClusterSize(int clusterIndx);

	int getCustomer(int clusterIndx, int indx);

	void getCustomers(int clusterIndx, TIntArrayList out);

	int[] getCustomersToClusters();

	Cost getCost();

	//Cost getClusterCost(int clusterIndx);

	//double getClusterQuantity(int clusterIndex);

	//int getClusterLocationCount(int clusterIndex);

	Location getClusterCentre(int i);

	Location[] getClusterCentres();

	//double getQuantity(int clusterIndx);

	int getNbUnassignedCustomers();

}