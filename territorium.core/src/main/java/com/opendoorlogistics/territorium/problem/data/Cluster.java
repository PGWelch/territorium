/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium.problem.data;

final public class Cluster extends HasExternalIndx{
	private double capacity=1;
	private double minQuantity;
	private Location fixedLocation;
	private Location refLocation;
	private double refLocationCostPerUnitDistance=0;
	private double refLocationCostPerUnitTime=1;

	public double getCapacity() {
		return capacity;
	}
	
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	
	public Location getFixedLocation() {
		return fixedLocation;
	}

	public void setFixedLocation(Location fixedLocation) {
		this.fixedLocation = fixedLocation;
	}

	public Location getRefLocation() {
		return refLocation;
	}

	public void setRefLocation(Location refLocation) {
		this.refLocation = refLocation;
	}

	public double getMinQuantity() {
		return minQuantity;
	}

	public void setMinQuantity(double minQuantity) {
		this.minQuantity = minQuantity;
	}

	public double getRefLocationCostPerUnitDistance() {
		return refLocationCostPerUnitDistance;
	}

	public void setRefLocationCostPerUnitDistance(double refLocationCostPerUnitDistance) {
		this.refLocationCostPerUnitDistance = refLocationCostPerUnitDistance;
	}

	public double getRefLocationCostPerUnitTime() {
		return refLocationCostPerUnitTime;
	}

	public void setRefLocationCostPerUnitTime(double refLocationCostPerUnitTime) {
		this.refLocationCostPerUnitTime = refLocationCostPerUnitTime;
	}

	

}
