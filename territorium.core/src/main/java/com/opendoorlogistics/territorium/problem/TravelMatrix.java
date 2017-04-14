package com.opendoorlogistics.territorium.problem;

public interface TravelMatrix {
	DistanceTime get(int fromLocationIndex, int toLocationIndex);
}
