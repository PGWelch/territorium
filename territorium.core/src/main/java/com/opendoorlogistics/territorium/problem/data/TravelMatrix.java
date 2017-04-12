package com.opendoorlogistics.territorium.problem.data;

public interface TravelMatrix {
	DistanceTime get(int fromLocationIndex, int toLocationIndex);
}
