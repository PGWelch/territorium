package com.opendoorlogistics.territorium.problem.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DistanceTime extends ObjectWithJSONToString{
	private double distance;
	private double time;
	
	public DistanceTime(){}

	public DistanceTime(double distance, double time) {
		this.distance = distance;
		this.time = time;
	}

	@JsonProperty("d")
	public double getDistance() {
		return distance;
	}


	public void setDistance(double distance) {
		this.distance = distance;
	}

	@JsonProperty("t")
	public double getTime() {
		return time;
	}


	public void setTime(double time) {
		this.time = time;
	}
	
	
	
}
