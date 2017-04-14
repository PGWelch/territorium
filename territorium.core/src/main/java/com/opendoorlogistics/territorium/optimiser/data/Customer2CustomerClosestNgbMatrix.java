package com.opendoorlogistics.territorium.optimiser.data;

public interface Customer2CustomerClosestNgbMatrix {

	int getNbClosestNeighbours(int customerIndx);

	int getClusterIndexOfClosestNeighbour(ImmutableSolution solution, int customerIndex, int nearestNeighbourIndex);

}