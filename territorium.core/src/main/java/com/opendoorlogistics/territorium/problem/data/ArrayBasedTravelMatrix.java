package com.opendoorlogistics.territorium.problem.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

	

}
