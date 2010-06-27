package com.nanosheep.bikeroute;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

/**
 * Holds a segment of a route - a road name, the points
 * that make it up and the turn to be taken to reach the
 * next segment.
 * 
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */
public class Segment implements Parcelable {
	/** Points in this segment. **/
	private GeoPoint start;
	/** Road name for this segment. **/
	private String name;
	/** Turn instruction to reach next segment. **/
	private String turn;
	/** Walk switch. **/
	private boolean walk;
	/** Length of segment. **/
	private int length;
	
	/**
	 * Create an empty segment.
	 */
	
	public Segment() {
	}
	
	/**
	 * Creates a segment with a name.
	 * @param name Name for this segment.
	 */
	
	public Segment(final String name) {
		this.name = name;
	}
	
	/**
	 * Creates a segment with a name and turn instruction.
	 * @param name Name for this segment.
	 * @param turn Turn instruction to reach next.
	 */
	
	public Segment(final String name, final String turn) {
		this.name = name;
		this.turn = turn;
	}
	
	public Segment(final String name, final String turn, final GeoPoint point) {
		this.name = name;
		this.turn = turn;
		start = point;
	}
	
	/**
	 * Create a segment from a previously parcelled segment.
	 * @param in
	 */
	public Segment(final Parcel in) {
		readFromParcel(in);
	}

	/**
	 * Set the road name.
	 * @param name
	 */
	
	public void setName(final String name) {
		this.name = name;
	}
	
	/**
	 * Get the road name.
	 * @return the roadname as a string.
	 */
	
	public String getName() {
		return name;
	}
	
	/**
	 * Set the turn instruction.
	 * @param turn Turn instruction string.
	 */
	
	public void setTurn(final String turn) {
		this.turn = turn;
	}
	
	/**
	 * Get the turn instruction to reach next segment.
	 * @return a String of the turn instruction.
	 */
	
	public String getTurn() {
		return turn;
	}
	
	/**
	 * Add a point to this segment.
	 * @param point GeoPoint to add.
	 */
	
	public void setPoint(final GeoPoint point) {
		start = point;
	}
	
	/** Get the starting point of this 
	 * segment.
	 * @return a GeoPoint
	 */
	
	public GeoPoint startPoint() {
		return start;
	}
	
	/** Creates a segment which is a copy of this one.
	 * @return a Segment that is a copy of this one.
	 */
	
	public Segment copy() {
		final Segment copy = new Segment();
		copy.name = name;
		copy.start = start;
		copy.turn = turn;
		copy.length = length;
		copy.walk = walk;
		return copy;
	}

	/**
	 * @param walk Set whether this is a walking segment or not.
	 */
	public void setWalk(final boolean walk) {
		this.walk = walk;
	}

	/**
	 * @return the whether this segment is a walking one.
	 */
	public boolean isWalk() {
		return walk;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeString(name);
		dest.writeString(turn);
		dest.writeValue(walk);
		dest.writeInt(length);
		dest.writeInt(start.getLatitudeE6());
		dest.writeInt(start.getLongitudeE6());
	}
	
	/**
	 * Rehydrate a segment from a parcel.
	 * @param in The parcel to rehydrate.
	 */
	
	public void readFromParcel(final Parcel in) {
		name = in.readString();
		turn = in.readString();
		walk = (Boolean) in.readValue(Boolean.class.getClassLoader());
		length = in.readInt();
		start = new GeoPoint(in.readInt(), in.readInt());
	}
	
	/**
	 * @param length the length to set
	 */
	public void setLength(final int length) {
		this.length = length;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	public static final Parcelable.Creator<Segment> CREATOR =
    	new Parcelable.Creator<Segment>() {
            public Segment createFromParcel(final Parcel in) {
                return new Segment(in);
            }

			@Override
			public Segment[] newArray(final int size) {
				return new Segment[size];
			}
        };

}
