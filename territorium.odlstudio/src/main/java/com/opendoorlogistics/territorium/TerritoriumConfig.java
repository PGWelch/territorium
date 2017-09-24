/*******************************************************************************
 * Copyright (c) 2014, 2017 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.distances.DistancesConfiguration;

@XmlRootElement(name = "CapacitatedClustererConfig")
final public class TerritoriumConfig implements Serializable {
	private int maxSecondsOptimization = 120;
	private int maxStepsOptimization = 250;
	private boolean useInputClusterTable; 
	private int numberClusters  =10;
	private double minClusterQuantity=0;
	private double maxClusterQuantity=100;
	private boolean useSwapMoves=true;
	private boolean polygons = false;
	
	private DistancesConfiguration distancesConfig = new DistancesConfiguration(); 
	
	public int getMaxSecondsOptimization() {
		return maxSecondsOptimization;
	}

	@XmlAttribute
	public void setMaxSecondsOptimization(int maxSecondsOptimization) {
		this.maxSecondsOptimization = maxSecondsOptimization;
	}

	public int getMaxStepsOptimization() {
		return maxStepsOptimization;
	}

	@XmlAttribute
	public void setMaxStepsOptimization(int maxStepsOptimization) {
		this.maxStepsOptimization = maxStepsOptimization;
	}

	public boolean isUseInputClusterTable() {
		return useInputClusterTable;
	}

	@XmlAttribute
	public void setUseInputClusterTable(boolean useInputClusterTable) {
		this.useInputClusterTable = useInputClusterTable;
	}

	public int getNumberClusters() {
		return numberClusters;
	}

	@XmlAttribute
	public void setNumberClusters(int numberClusters) {
		this.numberClusters = numberClusters;
	}

	public boolean isUseSwapMoves() {
		return useSwapMoves;
	}

	@XmlAttribute
	public void setUseSwapMoves(boolean useSwaps) {
		this.useSwapMoves = useSwaps;
	}

	public DistancesConfiguration getDistancesConfig() {
		return distancesConfig;
	}

	@XmlElement
	public void setDistancesConfig(DistancesConfiguration distancesConfig) {
		this.distancesConfig = distancesConfig;
	}

	@XmlAttribute
	public double getMinClusterQuantity() {
		return minClusterQuantity;
	}

	public void setMinClusterQuantity(double minClusterQuantity) {
		this.minClusterQuantity = minClusterQuantity;
	}

	@XmlAttribute
	public double getMaxClusterQuantity() {
		return maxClusterQuantity;
	}

	public void setMaxClusterQuantity(double maxClusterQuantity) {
		this.maxClusterQuantity = maxClusterQuantity;
	}

	@XmlAttribute
	public boolean isPolygons() {
		return polygons;
	}

	public void setPolygons(boolean polygons) {
		this.polygons = polygons;
	}

	
	
}
