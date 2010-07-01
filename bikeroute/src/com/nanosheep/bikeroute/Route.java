package com.nanosheep.bikeroute;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

/**
 * @author jono@nanosheep.net
 * @version Jun 22, 2010
 */
public class Route implements Parcelable{
	/** Route parcel id. **/
	public static final String ROUTE = "com.nanosheep.bikeroute.Route";
	private String name;
	private final List<GeoPoint> points;
	private List<Segment> segments;
	private String copyright;
	private String warning;
	private String country;
	private int length;
	
	public Route() {
		points = new ArrayList<GeoPoint>();
		segments = new ArrayList<Segment>();
	}
	
	public Route(final Parcel in) {
		points = new ArrayList<GeoPoint>();
		readFromParcel(in);
	}
	
	public void addPoint(final GeoPoint p) {
		points.add(p);
	}
	
	public void addPoints(final List<GeoPoint> points) {
		this.points.addAll(points);
	}
	
	public List<GeoPoint> getPoints() {
		return points;
	}
	
	public void addSegment(final Segment s) {
		segments.add(s);
	}
	
	public List<Segment> getSegments() {
		return segments;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeString(name);
		dest.writeTypedList(segments);
		dest.writeString(copyright);
		dest.writeString(warning);
		dest.writeString(country);
		dest.writeInt(length);
		int pointSize = points.size();
		dest.writeInt(pointSize);
		for (int i = 0; i < pointSize; i++) {
			dest.writeInt(points.get(i).getLatitudeE6());
			dest.writeInt(points.get(i).getLongitudeE6());
		}
	}
	
	public void readFromParcel(final Parcel in) {
		name = in.readString();
		segments = new ArrayList<Segment>();
		in.readTypedList(segments, Segment.CREATOR);
		copyright = in.readString();
		warning = in.readString();
		country = in.readString();
		length = in.readInt();
		int pointSize = in.readInt();
		for (int i = 0; i < pointSize; i++) {
			points.add(new GeoPoint(in.readInt(), in.readInt()));
		}
	}
	
	/**
	 * @param copyright the copyright to set
	 */
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	/**
	 * @return the copyright
	 */
	public String getCopyright() {
		return copyright;
	}

	/**
	 * @param warning the warning to set
	 */
	public void setWarning(String warning) {
		this.warning = warning;
	}

	/**
	 * @return the warning
	 */
	public String getWarning() {
		return warning;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param length the length to set
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public Route createFromParcel(final Parcel in) {
                return new Route(in);
            }

			@Override
			public Route[] newArray(final int size) {
				return new Route[size];
			}
        };

}
