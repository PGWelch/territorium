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

import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;

public class ODLBeanSolutionReport implements BeanMappedRow{
	private static final int COL_NB_NON_EMPTY_CLUSTERS=0;
//	private static final int COL_NB_EMPTY_CLUSTERS=COL_NB_NON_EMPTY_CLUSTERS+1;
	private static final int COL_NB_ASSIGNED_CUSTOMERS=COL_NB_NON_EMPTY_CLUSTERS+1;
	private static final int COL_NB_UNASSIGNED_CUSTOMERS=COL_NB_ASSIGNED_CUSTOMERS+1;
	private static final int COL_ASSIGNED_QUANTITY=COL_NB_UNASSIGNED_CUSTOMERS+1;
	private static final int COL_QUANTITY_VIOLATION=COL_ASSIGNED_QUANTITY+1;
	private static final int COL_COST=COL_QUANTITY_VIOLATION+1;
	private static final int COL_CUSTOMER_TIME=COL_COST+1;
	private static final int COL_CUSTOMER_DISTANCE=COL_CUSTOMER_TIME+1;
	private static final int COL_CUSTOMER_COST=COL_CUSTOMER_DISTANCE+1;
	private static final int COL_TARGET_TIME=COL_CUSTOMER_COST+1;
	private static final int COL_TARGET_DISTANCE=COL_TARGET_TIME+1;
	private static final int COL_TARGET_COST=COL_TARGET_DISTANCE+1;
	 	
	private long globalRowId;
	private int nbNonEmptyClusters;
	//private int nbEmptyClusters;
	private int nbAssignedCustomers;
	private int nbUnassignedCustomers;
	private double assignedQuantity;
	private double quantityViolation;
	private double cost;
	private double customerTime;
	private double customerDistance;
	private double customerCost;
	private double targetTime;
	private double targetDistance;
	private double targetCost;
	
	@ODLColumnOrder(COL_NB_NON_EMPTY_CLUSTERS)
	@ODLColumnName("#used-clusters")		
	public int getNbNonEmptyClusters() {
		return nbNonEmptyClusters;
	}
	public void setNbNonEmptyClusters(int nbClusters) {
		this.nbNonEmptyClusters = nbClusters;
	}

	@ODLColumnOrder(COL_NB_ASSIGNED_CUSTOMERS)
	@ODLColumnName("#assigned-customers")		
	public int getNbAssignedCustomers() {
		return nbAssignedCustomers;
	}
	public void setNbAssignedCustomers(int nbCustomers) {
		this.nbAssignedCustomers = nbCustomers;
	}
	
	@ODLColumnOrder(COL_NB_UNASSIGNED_CUSTOMERS)
	@ODLColumnName("#unassigned-customers")		
	public int getNbUnassignedCustomers() {
		return nbUnassignedCustomers;
	}
	public void setNbUnassignedCustomers(int nbUnassignedCustomers) {
		this.nbUnassignedCustomers = nbUnassignedCustomers;
	}
	
	@ODLColumnOrder(COL_ASSIGNED_QUANTITY)
	@ODLColumnName("assigned-quantity")	
	public double getAssignedQuantity() {
		return assignedQuantity;
	}
	public void setAssignedQuantity(double quantity) {
		this.assignedQuantity = quantity;
	}
	
	@ODLColumnOrder(COL_QUANTITY_VIOLATION)
	@ODLColumnName("quantity-violation")	
	public double getQuantityViolation() {
		return quantityViolation;
	}
	public void setQuantityViolation(double quantityViolation) {
		this.quantityViolation = quantityViolation;
	}
	
	@ODLColumnOrder(COL_COST)
	@ODLColumnName("cost")
	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	
	@ODLColumnOrder(COL_CUSTOMER_TIME)
	@ODLColumnName("customer-time")
	public double getCustomerTime() {
		return customerTime;
	}
	public void setCustomerTime(double customerTime) {
		this.customerTime = customerTime;
	}
	
	@ODLColumnOrder(COL_CUSTOMER_DISTANCE)
	@ODLColumnName("customer-distance")
	public double getCustomerDistance() {
		return customerDistance;
	}
	public void setCustomerDistance(double customerDistance) {
		this.customerDistance = customerDistance;
	}
	
	@ODLColumnOrder(COL_CUSTOMER_COST)
	@ODLColumnName("customer-cost")
	public double getCustomerCost() {
		return customerCost;
	}
	public void setCustomerCost(double customerCost) {
		this.customerCost = customerCost;
	}
	
	@ODLColumnOrder(COL_TARGET_TIME)
	@ODLColumnName("target-time")
	public double getTargetTime() {
		return targetTime;
	}
	public void setTargetTime(double targetTime) {
		this.targetTime = targetTime;
	}
	
	@ODLColumnOrder(COL_TARGET_DISTANCE)
	@ODLColumnName("target-distance")
	public double getTargetDistance() {
		return targetDistance;
	}
	public void setTargetDistance(double targetDistance) {
		this.targetDistance = targetDistance;
	}
	
	@ODLColumnOrder(COL_TARGET_COST)
	@ODLColumnName("target-cost")
	public double getTargetCost() {
		return targetCost;
	}
	public void setTargetCost(double targetCost) {
		this.targetCost = targetCost;
	}
	
	@ODLIgnore
	@Override
	public long getGlobalRowId() {
		return globalRowId;
	}
	@Override
	public void setGlobalRowId(long arg0) {
		this.globalRowId = arg0;
	}
	
//	@ODLColumnOrder(COL_NB_EMPTY_CLUSTERS)
//	@ODLColumnName("#empty-clusters")
//	public int getNbEmptyClusters() {
//		return nbEmptyClusters;
//	}
//	public void setNbEmptyClusters(int nbEmptyClusters) {
//		this.nbEmptyClusters = nbEmptyClusters;
//	}
//	
	
}
