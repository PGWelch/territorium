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
package com.opendoorlogistics.territorium.problem.location;

import java.util.HashMap;

import com.opendoorlogistics.territorium.utils.Pair;

public abstract class LocationFactory<T extends Location> {
	protected HashMap<Pair<Double, Double>, T> preexisting = new HashMap<>();

	protected T internalCreateOrGet(double a, double b){
		Pair<Double, Double> pair = new Pair<Double, Double>(a,b);
		T ret = preexisting.get(pair);
		if(ret==null){
			ret = internalCreateNew(a, b);
			ret.setIndex(preexisting.size());
			preexisting.put(pair, ret);
		}
		return ret;
	}
	
	protected abstract T internalCreateNew(double a, double b);
	
	public int getMaxLocationIndex(){
		return preexisting.size()-1;
	}
}
