package com.opendoorlogistics.territorium.problem.location;

public class XYLocation extends Location {
	private double x;
	private double y;

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public static class XYLocationFactory extends LocationFactory<XYLocation> {

		/**
		 * Create the location unless it already exists, in which case return it. Allocated a unique index to each
		 * location
		 * 
		 * @param lat
		 * @param lng
		 * @return
		 */
		public XYLocation create(double x, double y) {
			return internalCreateOrGet(x, y);
		}

		@Override
		protected XYLocation internalCreateNew(double a, double b) {
			XYLocation ret = new XYLocation();
			ret.setX(a);
			ret.setY(b);
			return ret;
		}

	}
}
