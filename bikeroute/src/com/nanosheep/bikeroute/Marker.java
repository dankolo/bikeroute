package com.nanosheep.bikeroute;

import com.google.android.maps.GeoPoint;

/**
 * Represents a cycle stand on a map.
 * 
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class Marker {

	/** Latitude in microdegrees. **/
	private int lat;
	/** Longitude in microdegrees. **/
	private int lng;
	/** Capacity of stand. **/
	private int capacity;

	/**
	 * The location of this marker.
	 * 
	 * @return location as a GeoPoint
	 */

	public GeoPoint getLocation() {
		return new GeoPoint(lat, lng);
	}

	/**
	 * Set latitude.
	 * 
	 * @param p Latitude in microdegrees.
	 */

	public final void setLat(final String p) {
		lat = (int) (Double.parseDouble(p) * Degrees.CNV);
	}

	/**
	 * Set longitude.
	 * 
	 * @param p Longitude in microdegrees.
	 */

	public final void setLng(final String p) {
		lng = (int) (Double.parseDouble(p) * Degrees.CNV);
	}

	/**
	 * The capacity of this stand.
	 * 
	 * @return integer capacity.
	 */

	public int getCapacity() {
		return capacity;
	}

	/**
	 * Set the capacity for this stand.
	 * 
	 * @param c the capacity.
	 */

	public final void setCapacity(final String c) {
		capacity = Integer.parseInt(c);
	}

	/**
	 * Create a Marker which is a copy of this one.
	 * 
	 * @return a copy of this marker.
	 */

	public final Marker copy() {
		final Marker copy = new Marker();
		copy.lat = lat;
		copy.lng = lng;
		copy.capacity = capacity;
		return copy;
	}

}
