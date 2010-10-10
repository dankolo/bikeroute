package com.nanosheep.bikeroute.utility;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.util.GeoPoint;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds a segment of a route - a road name, the points
 * that make it up and the turn to be taken to reach the
 * next segment.
 * 
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */
public class Segment implements Parcelable{
	/** Points in this segment. **/
	private List<GeoPoint> points;
	/** Turn instruction to reach next segment. **/
	private String instruction;
	/** Length of segment. **/
	private int length;
	/** Distance covered. **/
	private double distance;

	
	public Segment() {
		points = new ArrayList<GeoPoint>();
	}
	
	/**
     * Create a segment from a previously parcelled segment.
     * @param in
     */
    public Segment(final Parcel in) {
            readFromParcel(in);
    }
	
	/* (non-Javadoc)
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeString(instruction);
            dest.writeInt(length);
            dest.writeTypedList(points);
            dest.writeDouble(distance);
    }
    
    /**
     * Rehydrate a segment from a parcel.
     * @param in The parcel to rehydrate.
     */
    
    public void readFromParcel(final Parcel in) {
            instruction = in.readString();
            length = in.readInt();
            points = in.createTypedArrayList(GeoPoint.CREATOR);
            distance = in.readDouble();
    }

	/**
	 * Set the turn instruction.
	 * @param turn Turn instruction string.
	 */
	
	public void setInstruction(final String turn) {
		this.instruction = turn;
	}
	
	/**
	 * Get the turn instruction to reach next segment.
	 * @return a String of the turn instruction.
	 */
	
	public String getInstruction() {
		return instruction;
	}
	
	/**
	 * Clear the points out of a segment.
	 */
	
	public void clearPoints() {
		points.clear();
	}
	
	/**
	 * Add a point to this segment.
	 * @param point GeoPoint to add.
	 */
	
	public void addPoint(final GeoPoint point) {
		points.add(point);
	}
	
	/** Add a list of points to this segment.
	 * 
	 */
	
	public void addPoints(final List<GeoPoint> points) {
		this.points.addAll(points);
	}
	
	/** Get the starting point of this 
	 * segment.
	 * @return a GeoPoint
	 */
	
	public GeoPoint startPoint() {
		return points.get(0);
	}
	
	public List<GeoPoint> getPoints() {
		return points;
	}
	
	/** Creates a segment which is a copy of this one.
	 * @return a Segment that is a copy of this one.
	 */
	
	public Segment copy() {
		final Segment copy = new Segment();
		copy.points = new ArrayList<GeoPoint>(points);
		copy.instruction = instruction;
		copy.length = length;
		copy.distance = distance;
		return copy;
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

	/**
	 * @param distance the distance to set
	 */
	public void setDistance(final double distance) {
		this.distance = distance;
	}

	/**
	 * @return the distance
	 */
	public double getDistance() {
		return distance;
	}
	
	@Override
	public boolean equals(Object o) {
		if ((o instanceof Segment) && ((Segment)o).getInstruction().equals(instruction)) {
			return true;
		}
		return false;
	}
	
	 public static final Parcelable.Creator CREATOR =
	        new Parcelable.Creator() {
	            public Segment createFromParcel(final Parcel in) {
	                return new Segment(in);
	            }

	                        @Override
	                        public Segment[] newArray(final int size) {
	                                return new Segment[size];
	                        }
	        };


	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

}
