/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium;

import java.awt.Color;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnDescription;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultDoubleValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultLongValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;

public class ODLBeanCluster  implements BeanMappedRow{
	private final static int ID=0;
	private final static int MIN_QUANTITY=ID+1;
	private final static int MAX_QUANTITY=MIN_QUANTITY+1;
	private final static int FIX_CENTRE_TO_TARGET=MAX_QUANTITY+1;
	private final static int TARGET_LATITUDE=FIX_CENTRE_TO_TARGET+1;
	private final static int TARGET_LONGITUDE=TARGET_LATITUDE+1;
	private final static int TARGET_CENTRE_COST_PER_UNIT_DISTANCE=TARGET_LONGITUDE+1;
	private final static int TARGET_CENTRE_COST_PER_UNIT_TIME=TARGET_CENTRE_COST_PER_UNIT_DISTANCE+1;
	private final static int DISPLAY_COLOUR= TARGET_CENTRE_COST_PER_UNIT_TIME+1;
	public static final int MAX_COL=DISPLAY_COLOUR+1;
	

	private long globalRowId;
	private String id;
	private double maxQuantity=100;
	private double minQuantity;
	private long fixCentreToTarget;
	//private Location fixedLocation;
	private Double targetLatitude;
	private Double targetLongitude;
	private double targetCentreCostPerUnitDistance=0;
	private double targetCentreCostPerUnitTime=0;

	private Color displayColour;

	@ODLColumnOrder(ID)
	@ODLColumnDescription("Identifier of this cluster; for example the (unique) name of a salesperson assigned to it.")
	@ODLColumnName(PredefinedTags.ID)
	public String getId() {
		return id;
	}

	public void setId(String clusterId) {
		this.id = clusterId;
	}

	@ODLIgnore
	public long getGlobalRowId() {
		return globalRowId;
	}

	public void setGlobalRowId(long globalRowId) {
		this.globalRowId = globalRowId;
	}

	@ODLColumnOrder(MAX_QUANTITY)
	@ODLDefaultDoubleValue(100)
	@ODLColumnName("max-quantity")
	public double getMaxQuantity() {
		return maxQuantity;
	}

	public void setMaxQuantity(double maxQuantity) {
		this.maxQuantity = maxQuantity;
	}

	@ODLColumnOrder(MIN_QUANTITY)
	@ODLDefaultDoubleValue(0)
	@ODLColumnName("min-quantity")
	public double getMinQuantity() {
		return minQuantity;
	}

	public void setMinQuantity(double minQuantity) {
		this.minQuantity = minQuantity;
	}

	@ODLColumnOrder(FIX_CENTRE_TO_TARGET)
	@ODLDefaultLongValue(0)
	@ODLColumnName("fix-to-target")	
	public long getFixCentreToTarget() {
		return fixCentreToTarget;
	}

	public void setFixCentreToTarget(long fixCentreToTarget) {
		this.fixCentreToTarget = fixCentreToTarget;
	}

	@ODLColumnOrder(TARGET_LATITUDE)
	@ODLDefaultDoubleValue(0)
	@ODLNullAllowed
	@ODLColumnName("target-latitude")		
	public Double getTargetLatitude() {
		return targetLatitude;
	}

	public void setTargetLatitude(Double targetLatitude) {
		this.targetLatitude = targetLatitude;
	}

	@ODLColumnOrder(TARGET_LONGITUDE)
	@ODLDefaultDoubleValue(0)
	@ODLNullAllowed
	@ODLColumnName("target-longitude")			
	public Double getTargetLongitude() {
		return targetLongitude;
	}

	public void setTargetLongitude(Double targetLongitude) {
		this.targetLongitude = targetLongitude;
	}

	@ODLColumnOrder(TARGET_CENTRE_COST_PER_UNIT_DISTANCE)
	@ODLDefaultDoubleValue(0)
	@ODLColumnName("target-cost-distance")			
	@ODLNullAllowed
	public double getTargetCentreCostPerUnitDistance() {
		return targetCentreCostPerUnitDistance;
	}

	public void setTargetCentreCostPerUnitDistance(double targetCentreCostPerUnitDistance) {
		this.targetCentreCostPerUnitDistance = targetCentreCostPerUnitDistance;
	}

	@ODLColumnOrder(TARGET_CENTRE_COST_PER_UNIT_TIME)
	@ODLDefaultDoubleValue(0)
	@ODLNullAllowed
	@ODLColumnName("target-cost-time")				
	public double getTargetCentreCostPerUnitTime() {
		return targetCentreCostPerUnitTime;
	}

	public void setTargetCentreCostPerUnitTime(double targetCentreCostPerUnitTime) {
		this.targetCentreCostPerUnitTime = targetCentreCostPerUnitTime;
	}

	@ODLColumnOrder(DISPLAY_COLOUR)
	@ODLNullAllowed
	@ODLColumnName("display-colour")					
	public Color getDisplayColour() {
		return displayColour;
	}

	public void setDisplayColour(Color displayColour) {
		this.displayColour = displayColour;
	}

	
	

}
