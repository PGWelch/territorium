/*******************************************************************************
 * Copyright (c) 2014, 2017 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium.problem;

import com.opendoorlogistics.territorium.problem.location.Location;

public class Customer extends HasUserIndx{
	private double quantity=1;
	private Location location;
	private double costPerUnitDistance=0;
	private double costPerUnitTime=1;	
	private int preferredClusterIndex=-1;
	private double travelCostMultiplier4PreferredClusterIndex=1;
	private double preferredClusterPenaltyCost=0;
	
	public double getQuantity() {
		return quantity;
	}
	
	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public double getCostPerUnitDistance() {
		return costPerUnitDistance;
	}

	public void setCostPerUnitDistance(double costPerUnitDistance) {
		this.costPerUnitDistance = costPerUnitDistance;
	}

	public double getCostPerUnitTime() {
		return costPerUnitTime;
	}

	public void setCostPerUnitTime(double costPerUnitTime) {
		this.costPerUnitTime = costPerUnitTime;
	}

	/**
	 * Preferred cluster index or -1 if not defined
	 * @return
	 */
	public int getPreferredClusterIndex() {
		return preferredClusterIndex;
	}

	public void setPreferredClusterIndex(int favouriteClusterIndex) {
		this.preferredClusterIndex = favouriteClusterIndex;
	}

	/**
	 * The travel cost for the preferred cluster gets multiplied by this number.
	 * e.g. if this value is 0.5, it's 50% cheaper to assign customer to prefereed cluster
	 * @return
	 */
	public double getTravelCostMultiplier4PreferredClusterIndex() {
		return travelCostMultiplier4PreferredClusterIndex;
	}

	public void setTravelCostMultiplier4PreferredClusterIndex(double travelCostMultiplier4FavouriteClusterIndex) {
		this.travelCostMultiplier4PreferredClusterIndex = travelCostMultiplier4FavouriteClusterIndex;
	}

	/**
	 * Cost that gets added if a customer is assigned to a cluster which is not its preferred cluster
	 * @return
	 */
	public double getPreferredClusterPenaltyCost() {
		return preferredClusterPenaltyCost;
	}

	public void setPreferredClusterPenaltyCost(double preferredClusterPenaltyCost) {
		this.preferredClusterPenaltyCost = preferredClusterPenaltyCost;
	}


	
}
