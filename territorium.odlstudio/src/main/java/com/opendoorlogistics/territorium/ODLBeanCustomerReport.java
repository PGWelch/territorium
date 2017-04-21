package com.opendoorlogistics.territorium;

import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;

public class ODLBeanCustomerReport extends ODLBeanCustomer {
	private final static int COL_TT=ODLBeanCustomer.COL_MAX+1;
	private final static int COL_TD=COL_TT+1;
	private final static int COL_TC=COL_TD+1;
	private final static int COL_PCCL=COL_TC+1;

	private Double travelTime;
	private Double travelDistance;
	private Double travelCost;
	private Double PCOfClusterQuantity;
	
	@ODLColumnOrder(COL_TT)
	@ODLColumnName("travel-time")
	@ODLNullAllowed
	public Double getTravelTime() {
		return travelTime;
	}
	public void setTravelTime(Double travelTime) {
		this.travelTime = travelTime;
	}
	
	@ODLColumnOrder(COL_TD)
	@ODLColumnName("travel-distance")
	@ODLNullAllowed
	public Double getTravelDistance() {
		return travelDistance;
	}
	public void setTravelDistance(Double travelDistance) {
		this.travelDistance = travelDistance;
	}
	
	@ODLColumnOrder(COL_TC)
	@ODLColumnName("travel-cost")
	@ODLNullAllowed
	public Double getTravelCost() {
		return travelCost;
	}
	public void setTravelCost(Double travelCost) {
		this.travelCost = travelCost;
	}
	
	@ODLColumnName("%cluster-quantity")
	@ODLColumnOrder(COL_PCCL)
	public Double getPCOfClusterQuantity() {
		return PCOfClusterQuantity;
	}
	public void setPCOfClusterQuantity(Double pCOfClusterQuantity) {
		PCOfClusterQuantity = pCOfClusterQuantity;
	}
	
	
}
