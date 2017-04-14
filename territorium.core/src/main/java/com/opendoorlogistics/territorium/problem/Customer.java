/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
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


	
}
