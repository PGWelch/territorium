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
