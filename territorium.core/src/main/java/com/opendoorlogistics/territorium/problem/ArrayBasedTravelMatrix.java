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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opendoorlogistics.territorium.problem.location.Location;
import com.opendoorlogistics.territorium.problem.location.XYLocation;

public class ArrayBasedTravelMatrix extends ObjectWithJSONToString implements TravelMatrix{
	private DistanceTime matrix[][];
	
	public ArrayBasedTravelMatrix(int maxLocIndex){
		matrix = new DistanceTime[maxLocIndex+1][];
		for(int i =0 ; i<matrix.length ; i++){
			matrix [i] = new DistanceTime[maxLocIndex+1];
		}
	}
	
	@JsonIgnore
	@Override
	public DistanceTime get(int fromLocationIndex, int toLocationIndex) {
		return matrix[fromLocationIndex][toLocationIndex];
	}

	@JsonIgnore
	public void set(int fromLocationIndex, int toLocationIndex, double metres, double seconds){
		set(fromLocationIndex, toLocationIndex, new DistanceTime(metres, seconds));
	}
	
	@JsonIgnore
	public void set(int fromLocationIndex, int toLocationIndex, DistanceTime ms){
		matrix[fromLocationIndex][toLocationIndex]=ms;
	}

	public DistanceTime[][] getMatrix() {
		return matrix;
	}

	public void setMatrix(DistanceTime[][] matrix) {
		this.matrix = matrix;
	}

	public static ArrayBasedTravelMatrix buildForXYProblem(Problem problem, double speed){
		int maxLocIndx = 0;
		List<Location> locs = Problem.getAllLocations(problem);
		for(Location location : locs){
			maxLocIndx = Math.max(location.getIndex(), maxLocIndx);
		}
		ArrayBasedTravelMatrix ret = new  ArrayBasedTravelMatrix(maxLocIndx);

		double invSpeed = 1.0/speed;
		for(Location from: locs){
			XYLocation xyFrom = (XYLocation)from;
			for(Location to: locs){
				XYLocation xyTo = (XYLocation)to;
				DistanceTime dt = new DistanceTime();
				double dx = xyFrom.getX() - xyTo.getX();
				double dy = xyFrom.getY() - xyTo.getY();
				dt.setDistance(Math.sqrt(dx*dx+dy*dy));
				dt.setTime(dt.getDistance()* invSpeed);
				ret.set(xyFrom.getIndex(), xyTo.getIndex(), dt);
			}

		}
		return ret;
	}

}
