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
package com.opendoorlogistics.territorium.problem;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.territorium.problem.location.Location;

public class Problem extends ObjectWithJSONToString{
	private QuantityViolationType quantityViolationType = QuantityViolationType.LINEAR;
	private List<Customer> customers = new ArrayList<>();
	private List<Cluster> clusters= new ArrayList<>();
	private TravelMatrix travelMatrix=null;

	public enum QuantityViolationType{
		LINEAR,
		SQUARE
	}
	
	public Problem(List<Customer> customers,List<Cluster> clusters,TravelMatrix travelMatrix) {
		this.customers= customers;
		this.clusters = clusters;
		this.travelMatrix = travelMatrix;

	}

	public Problem(){}
	
	public TravelMatrix getTravelMatrix() {
		return travelMatrix;
	}

	public double getTravelCost(int assignedClusterIndexOrMinus1,Location clusterLocation, Customer customer) {
		DistanceTime dt = getTravel(clusterLocation, customer);
		double cost= dt.getDistance() * customer.getCostPerUnitDistance() + dt.getTime() * customer.getCostPerUnitTime();
		if(assignedClusterIndexOrMinus1!=-1 && customer.getPreferredClusterIndex()!=-1) {
			
			// reduce the travel cost if its assigned to its preferred cluster
			if(assignedClusterIndexOrMinus1==customer.getPreferredClusterIndex()) {
				cost *= customer.getTravelCostMultiplier4PreferredClusterIndex();				
			}
		}
		return cost;
	}


	/**
	 * 
	 * @param clusterCentre Can be null (null is returned)
	 * @param cluster
	 * @return
	 */
	public DistanceTime getTargetToCentreTravel(Location clusterCentre, Cluster cluster){
		if (clusterCentre != null && cluster.getTargetCentre() != null) {
			return getTravelMatrix().get(cluster.getTargetCentre().getIndex(), clusterCentre.getIndex());
		}
		return null;
	}

	/**
	 * 
	 * @param clusterCentre
	 * @param cluster Can be null (0 is returned)
	 * @return
	 */
	public double getTargetToCentreTravelCost(Location clusterCentre, Cluster cluster){
		DistanceTime dt = getTargetToCentreTravel(clusterCentre, cluster);
		if(dt!=null){
			return cluster.getTargetCentreCostPerUnitDistance() * dt.getDistance()
					+ cluster.getTargetCentreCostPerUnitTime() * dt.getTime();
		}
		return 0;
	}

	public DistanceTime getTravel(Location clusterLocation, Customer customer) {
		DistanceTime dt = travelMatrix.get(clusterLocation.getIndex(), customer.getLocation().getIndex());
		return dt;
	}

	public List<Customer> getCustomers() {
		return customers;
	}

	public List<Cluster> getClusters() {
		return clusters;
	}

	public void setCustomers(List<Customer> customers) {
		this.customers = customers;
	}

	public void setClusters(List<Cluster> clusters) {
		this.clusters = clusters;
	}

	public void setTravelMatrix(TravelMatrix travelMatrix) {
		this.travelMatrix = travelMatrix;
	}
	
	public static List<Location> getAllLocations(Problem problem){
		ArrayList<Location> ret = new ArrayList<>();
		problem.getCustomers().forEach(c->ret.add(c.getLocation()));
		problem.getClusters().forEach(c->{
			if(c.getTargetCentre()!=null){
				ret.add(c.getTargetCentre());
			}
		});
		return ret;
	}

	public QuantityViolationType getQuantityViolationType() {
		return quantityViolationType;
	}

	public void setQuantityViolationType(QuantityViolationType quantityViolationType) {
		this.quantityViolationType = quantityViolationType;
	}
	

}