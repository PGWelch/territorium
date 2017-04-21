/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium.problem;

import com.opendoorlogistics.territorium.problem.location.Location;

public class Cluster extends HasUserIndx{
	private double maxQuantity=1;
	private double minQuantity;
	private boolean fixCentreToTarget;
	//private Location fixedLocation;
	private Location targetCentre;
	private double targetCentreCostPerUnitDistance=0;
	private double targetCentreCostPerUnitTime=0;

	public double getMaxQuantity() {
		return maxQuantity;
	}
	
	public void setMaxQuantity(double capacity) {
		this.maxQuantity = capacity;
	}
	
//	public Location getFixedLocation() {
//		return fixedLocation;
//	}
//
//	public void setFixedLocation(Location fixedLocation) {
//		this.fixedLocation = fixedLocation;
//	}

	public Location getTargetCentre() {
		return targetCentre;
	}

	public void setTargetCentre(Location refLocation) {
		this.targetCentre = refLocation;
	}

	public double getMinQuantity() {
		return minQuantity;
	}

	public void setMinQuantity(double minQuantity) {
		this.minQuantity = minQuantity;
	}

	public double getTargetCentreCostPerUnitDistance() {
		return targetCentreCostPerUnitDistance;
	}

	public void setTargetCentreCostPerUnitDistance(double refLocationCostPerUnitDistance) {
		this.targetCentreCostPerUnitDistance = refLocationCostPerUnitDistance;
	}

	public double getTargetCentreCostPerUnitTime() {
		return targetCentreCostPerUnitTime;
	}

	public void setTargetCentreCostPerUnitTime(double refLocationCostPerUnitTime) {
		this.targetCentreCostPerUnitTime = refLocationCostPerUnitTime;
	}

	public boolean isFixCentreToTarget() {
		return fixCentreToTarget;
	}

	public void setFixCentreToTarget(boolean fixCentreToTarget) {
		this.fixCentreToTarget = fixCentreToTarget;
	}
	
	/**
	 * Return the target centre if fix centre to target is on
	 * @param cluster
	 * @return
	 */
	public static Location getFixedCentre(Cluster cluster){
		return cluster.isFixCentreToTarget()?cluster.getTargetCentre():null;
	}
	

}
