package com.opendoorlogistics.territorium.problem.data;

public class LatLongLocation extends Location {

	private double latitude;
	private double longitude;

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
}
