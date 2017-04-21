package com.opendoorlogistics.territorium;

import java.awt.Color;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnDescription;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultDoubleValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;

public class ODLBeanClusterReport implements BeanMappedRow{
	private final static int ID=0;
	private final static int MIN_QUANTITY=ID+1;
	private final static int MAX_QUANTITY=MIN_QUANTITY+1;
	private final static int FIX_CENTRE_TO_TARGET=MAX_QUANTITY+1;
	private final static int TARGET_LATITUDE=FIX_CENTRE_TO_TARGET+1;
	private final static int TARGET_LONGITUDE=TARGET_LATITUDE+1;
	private final static int TARGET_CENTRE_COST_PER_UNIT_DISTANCE=TARGET_LONGITUDE+1;
	private final static int TARGET_CENTRE_COST_PER_UNIT_TIME=TARGET_CENTRE_COST_PER_UNIT_DISTANCE+1;
	private final static int DISPLAY_COLOUR= TARGET_CENTRE_COST_PER_UNIT_TIME+1;
	private final static int ASSIGNED_LATITUDE=DISPLAY_COLOUR;
	private final static int ASSIGNED_LONGITUDE=ASSIGNED_LATITUDE+1;
	private final static int ASSIGNED_CUSTOMERS_COUNT=ASSIGNED_LONGITUDE+1;
	private final static int ASSIGNED_QUANTITY=ASSIGNED_CUSTOMERS_COUNT+1;
	private final static int ASSIGNED_QUANTITY_VIOLATION=ASSIGNED_QUANTITY+1;
	private final static int ASSIGNED_COST=ASSIGNED_QUANTITY_VIOLATION+1;
	private final static int SEL_CUST_TIME=ASSIGNED_COST+1;
	private final static int SEL_CUST_DIST=SEL_CUST_TIME+1;
	private final static int SEL_CUST_COST=SEL_CUST_DIST+1;
	private final static int SEL_TARG_TIME=SEL_CUST_COST+1;
	private final static int SEL_TARG_DIST=SEL_TARG_TIME+1;
	private final static int SEL_TARG_COST=SEL_TARG_DIST+1;
	
	private long globalRowId;
	private String id;
	private double maxQuantity;
	private double minQuantity;
	private Color displayColour;

	private Double assignedLatitude;
	private Double assignedLongitude;
	private double assignedCustomersCount;
	private double assignedQuantity;
	private double assignedQuantityViolation;
	private double assignedTotalCost;
	private double selCustomerTime=0;
	private double selCustomerDistance=0;
	private double selCustomerCost=0;
	private double selTargetTime;
	private double selTargetDistance;
	private double selTargetCost;
	
	@ODLIgnore
	public long getGlobalRowId() {
		return globalRowId;
	}

	public void setGlobalRowId(long globalRowId) {
		this.globalRowId = globalRowId;
	}


	@ODLColumnOrder(ID)
	@ODLColumnDescription("Identifier of this cluster; for example the (unique) name of a salesperson assigned to it.")
	@ODLColumnName(PredefinedTags.ID)
	public String getId() {
		return id;
	}

	public void setId(String clusterId) {
		this.id = clusterId;
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


	@ODLColumnOrder(DISPLAY_COLOUR)
	@ODLNullAllowed
	@ODLColumnName("display-colour")					
	public Color getDisplayColour() {
		return displayColour;
	}

	public void setDisplayColour(Color displayColour) {
		this.displayColour = displayColour;
	}

	

	@ODLColumnOrder(ASSIGNED_LATITUDE)
	@ODLNullAllowed
	@ODLColumnName("latitude")				
	public Double getAssignedLatitude() {
		return assignedLatitude;
	}

	public void setAssignedLatitude(Double assignedLatitude) {
		this.assignedLatitude = assignedLatitude;
	}

	@ODLColumnName("longitude")				
	@ODLColumnOrder(ASSIGNED_LONGITUDE)
	@ODLNullAllowed
	public Double getAssignedLongitude() {
		return assignedLongitude;
	}

	public void setAssignedLongitude(Double assignedLongitude) {
		this.assignedLongitude = assignedLongitude;
	}

	@ODLColumnName("#customers")				
	@ODLColumnOrder(ASSIGNED_CUSTOMERS_COUNT)
	@ODLNullAllowed
	public double getAssignedCustomersCount() {
		return assignedCustomersCount;
	}

	public void setAssignedCustomersCount(double assignedCustomersCount) {
		this.assignedCustomersCount = assignedCustomersCount;
	}

	@ODLColumnName("quantity")				
	@ODLColumnOrder(ASSIGNED_QUANTITY)
	@ODLNullAllowed
	public double getAssignedQuantity() {
		return assignedQuantity;
	}

	public void setAssignedQuantity(double assignedQuantity) {
		this.assignedQuantity = assignedQuantity;
	}

	@ODLColumnOrder(ASSIGNED_QUANTITY_VIOLATION)
	@ODLNullAllowed
	@ODLColumnName("quantity-violation")					
	public double getAssignedQuantityViolation() {
		return assignedQuantityViolation;
	}

	public void setAssignedQuantityViolation(double assignedCapacityViolation) {
		this.assignedQuantityViolation = assignedCapacityViolation;
	}

	@ODLColumnOrder(ASSIGNED_COST)
	@ODLColumnName("cost")					
	@ODLNullAllowed
	public double getAssignedTotalCost() {
		return assignedTotalCost;
	}

	public void setAssignedTotalCost(double assignedTravelCost) {
		this.assignedTotalCost = assignedTravelCost;
	}

	@ODLColumnOrder(SEL_CUST_TIME)
	@ODLColumnName("customer-time")
	public double getSelCustomerTime() {
		return selCustomerTime;
	}

	public void setSelCustomerTime(double selCustomerTime) {
		this.selCustomerTime = selCustomerTime;
	}

	@ODLColumnOrder(SEL_CUST_DIST)
	@ODLColumnName("customer-distance")
	public double getSelCustomerDistance() {
		return selCustomerDistance;
	}

	public void setSelCustomerDistance(double selCustomerDistance) {
		this.selCustomerDistance = selCustomerDistance;
	}

	@ODLColumnOrder(SEL_CUST_COST)
	@ODLColumnName("customer-cost")
	public double getSelCustomerCost() {
		return selCustomerCost;
	}

	public void setSelCustomerCost(double selCustomerCost) {
		this.selCustomerCost = selCustomerCost;
	}

	@ODLColumnOrder(SEL_TARG_TIME)
	@ODLColumnName("target-time")
	public double getSelTargetTime() {
		return selTargetTime;
	}

	public void setSelTargetTime(double selTargetTime) {
		this.selTargetTime = selTargetTime;
	}

	@ODLColumnOrder(SEL_TARG_DIST)
	@ODLColumnName("target-distance")
	public double getSelTargetDistance() {
		return selTargetDistance;
	}

	public void setSelTargetDistance(double selTargetDistance) {
		this.selTargetDistance = selTargetDistance;
	}

	@ODLColumnOrder(SEL_TARG_COST)
	@ODLColumnName("target-cost")
	public double getSelTargetCost() {
		return selTargetCost;
	}

	public void setSelTargetCost(double selTargetCost) {
		this.selTargetCost = selTargetCost;
	}

	

}
