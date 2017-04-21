package com.opendoorlogistics.territorium.optimiser.data;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;

public class Customer2CustomerClosestNgbMatrixImpl implements Customer2CustomerClosestNgbMatrix {
	private int [][]closestNeighbours;
	
	public Customer2CustomerClosestNgbMatrixImpl(Problem problem){
		List<Customer> customers = problem.getCustomers();
		int n = customers.size();
		closestNeighbours = new int[n][];

		ArrayList<Integer> tmpList = new ArrayList<>();
		for(int i =0 ; i<n;i++){
			tmpList.add(i);
		}
		
		for(int i =0 ; i<n;i++){
			double []costs = new double[n];
			Location from = customers.get(i).getLocation();
			for(int j =0 ; j<n;j++){
				costs[j] = problem.getTravelCost(from, customers.get(j));
			}
			
			tmpList.sort((k,l)->Double.compare(costs[k],costs[l]));
			closestNeighbours[i] = new int[n];
			for(int j =0 ; j<n;j++){
				closestNeighbours[i][j] = tmpList.get(j);
			}
		}
	}
	
//	public int[] getClosestNeighbours(int customerIndx){
//		return closestNeighbours[customerIndx];
//	}
	
	/* (non-Javadoc)
	 * @see com.opendoorlogistics.territorium.solver.data.Customer2CustomerClosestNgbMatrix#getNbClosestNeighbours(int)
	 */
	@Override
	public int getNbClosestNeighbours(int customerIndx){
		return closestNeighbours[customerIndx].length;		
	}
	
	/* (non-Javadoc)
	 * @see com.opendoorlogistics.territorium.solver.data.Customer2CustomerClosestNgbMatrix#getClusterIndexOfClosestNeighbour(com.opendoorlogistics.territorium.solver.MutableSolution, int, int)
	 */
	@Override
	public int getClusterIndexOfClosestNeighbour(ImmutableSolution solution, int customerIndex, int nearestNeighbourIndex){
		return solution.getClusterIndex(closestNeighbours[customerIndex][nearestNeighbourIndex]);
	}
}
