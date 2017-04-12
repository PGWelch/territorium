package com.opendoorlogistics.territorium.solver.data;

import com.opendoorlogistics.territorium.solver.ImmutableSolution;

public interface Customer2CustomerClosestNgbMatrix {

	int getNbClosestNeighbours(int customerIndx);

	int getClusterIndexOfClosestNeighbour(ImmutableSolution solution, int customerIndex, int nearestNeighbourIndex);

}