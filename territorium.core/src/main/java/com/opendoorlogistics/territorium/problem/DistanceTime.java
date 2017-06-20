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
package com.opendoorlogistics.territorium.problem;

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
