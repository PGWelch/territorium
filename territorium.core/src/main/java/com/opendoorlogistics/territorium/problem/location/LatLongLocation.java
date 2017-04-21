package com.opendoorlogistics.territorium.problem.location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.opendoorlogistics.territorium.utils.Pair;

public class LatLongLocation extends Location {
	private double latitude;
	private double longitude;

	public LatLongLocation(){
		
	}
	
	
	public LatLongLocation(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}


	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	@Override
	public String toString(){
		return "Indx="+getIndex() + ", lat=" + getLatitude() + ", lng=" + getLongitude();
	}
	
	public static class LatLongLocationFactory extends LocationFactory<LatLongLocation>{
		
		/**
		 * Create the location unless it already exists, in which case return it.
		 * Allocated a unique index to each location
		 * @param lat
		 * @param lng
		 * @return
		 */
		public LatLongLocation create(double lat, double lng){
			return internalCreateOrGet(lat, lng);
		}
		
		@Override
		protected LatLongLocation internalCreateNew(double a, double b){
			LatLongLocation ret = new LatLongLocation();
			ret.setLatitude(a);
			ret.setLongitude(b);
			return ret;
		}
		
		public List<LatLongLocation> getLocations(){
			return preexisting.keySet().stream().map(pair->new LatLongLocation(pair.getA(),pair.getB())).collect(Collectors.toList());
		}

	}
}
