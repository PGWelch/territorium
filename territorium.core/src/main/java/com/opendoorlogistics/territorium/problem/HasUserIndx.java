package com.opendoorlogistics.territorium.problem;

public class HasUserIndx {
	private long externalIndex;

	/**
	 * The external index is not read or used by the solver.
	 * Use it to connect up data etc.
	 * @return
	 */
	public long getUserIndex() {
		return externalIndex;
	}

	public void setUserIndex(long externalIndex) {
		this.externalIndex = externalIndex;
	}
	
	
}
