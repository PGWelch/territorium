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
