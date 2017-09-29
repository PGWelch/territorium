/*******************************************************************************
 * Copyright (c) 2014, 2017 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultDoubleValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;

public class ODLBeanCustomer implements BeanMappedRow{
	private final static int COL_ID=0;
	private final static int COL_LAT=COL_ID+1;
	private final static int COL_LNG=COL_LAT+1;
	private final static int COL_QUANT=COL_LNG+1;
	private final static int COL_CPUTT=COL_QUANT+1;
	private final static int COL_CPUTD=COL_CPUTT+1;
	private final static int COL_CLUSTER=COL_CPUTD+1;
	public final static int COL_MAX = COL_CLUSTER+1;
	
	private String id;
	private long globalRowId;
	private double latitude;
	private double longitude;
	private double quantity=1;
	private double costPerUnitTravelTime=1;
	private double costPerUnitTravelDistance=0;
	private String clusterId;
//	private String customerId;
	
	
	@ODLNullAllowed
	@ODLColumnOrder(COL_CLUSTER)	
	@ODLColumnName(ScriptBuilder.TERRITORY_ID_FIELD)
	public String getClusterId() {
		return clusterId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	
	@ODLColumnOrder(COL_ID)
	@ODLColumnName(PredefinedTags.ID)
	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	@ODLColumnOrder(COL_LAT)
	@ODLColumnName(PredefinedTags.LATITUDE)
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	@ODLColumnOrder(COL_LNG)
	@ODLColumnName(PredefinedTags.LONGITUDE)
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}


	@ODLColumnOrder(COL_QUANT)
	@ODLColumnName(PredefinedTags.QUANTITY)
	public double getQuantity() {
		return quantity;
	}
	
	@ODLDefaultDoubleValue(1.0)
	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}


	
	@Override
	@ODLIgnore
	public long getGlobalRowId() {
		return globalRowId;
	}

	@ODLNullAllowed
	@ODLColumnOrder(COL_CPUTT)	
	@ODLDefaultDoubleValue(1.0)
	@ODLColumnName("cost-time")
	public double getCostPerUnitTravelTime() {
		return costPerUnitTravelTime;
	}

	public void setCostPerUnitTravelTime(double costPerUnitTravelTime) {
		this.costPerUnitTravelTime = costPerUnitTravelTime;
	}

	@ODLNullAllowed
	@ODLColumnOrder(COL_CPUTD)	
	@ODLDefaultDoubleValue(0.0)	
	@ODLColumnName("cost-distance")
	public double getCostPerUnitTravelDistance() {
		return costPerUnitTravelDistance;
	}

	public void setCostPerUnitTravelDistance(double costPerUnitTravelDistance) {
		this.costPerUnitTravelDistance = costPerUnitTravelDistance;
	}

	@Override
	public void setGlobalRowId(long arg0) {
		this.globalRowId = arg0;
	}

}
