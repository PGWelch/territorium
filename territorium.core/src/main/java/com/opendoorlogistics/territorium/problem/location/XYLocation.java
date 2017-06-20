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
