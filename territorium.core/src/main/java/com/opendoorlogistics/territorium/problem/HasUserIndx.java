package com.opendoorlogistics.territorium.problem;

public class HasUserIndx {
	private int externalIndex;

	/**
	 * The external index is not read or used by the solver.
	 * Use it to connect up data etc.
	 * @return
	 */
	public int getUserIndex() {
		return externalIndex;
	}

	public void setUserIndex(int externalIndex) {
		this.externalIndex = externalIndex;
	}
	
	
}
