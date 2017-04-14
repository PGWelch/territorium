package com.opendoorlogistics.territorium.problem;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.territorium.problem.location.Location;

public class Problem extends ObjectWithJSONToString{

	private List<Customer> customers = new ArrayList<>();
	private List<Cluster> clusters= new ArrayList<>();
	private TravelMatrix travelMatrix=null;

	public Problem(List<Customer> customers,List<Cluster> clusters,TravelMatrix travelMatrix) {
		this.customers= customers;
		this.clusters = clusters;
		this.travelMatrix = travelMatrix;

	}

	public Problem(){}
	
	public TravelMatrix getTravelMatrix() {
		return travelMatrix;
	}

	public double getTravelCost(Location clusterLocation, Customer customer) {
		DistanceTime dt = travelMatrix.get(clusterLocation.getIndex(), customer.getLocation().getIndex());
		return dt.getDistance() * customer.getCostPerUnitDistance() + dt.getTime() * customer.getCostPerUnitTime();
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
	

}