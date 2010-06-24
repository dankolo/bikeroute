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
	private String name;
	private final List<GeoPoint> points;
	private List<Segment> segments;
	
	public Route() {
		points = new ArrayList<GeoPoint>();
		segments = new ArrayList<Segment>();
	}
	
	public Route(Parcel in) {
		points = new ArrayList<GeoPoint>();
		readFromParcel(in);
	}
	
	public void addPoint(final GeoPoint p) {
		points.add(p);
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
	public void setName(String name) {
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
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeTypedList(segments);
	}
	
	public void readFromParcel(Parcel in) {
		name = in.readString();
		segments = new ArrayList<Segment>();
		in.readTypedList(segments, Segment.CREATOR);
	}
	
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public Route createFromParcel(Parcel in) {
                return new Route(in);
            }

			@Override
			public Route[] newArray(int size) {
				return new Route[size];
			}
        };

}
