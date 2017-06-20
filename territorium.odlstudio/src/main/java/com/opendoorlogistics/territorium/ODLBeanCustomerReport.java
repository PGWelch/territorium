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
