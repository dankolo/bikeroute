package com.nanosheep.bikeroute.utility;

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
	private GeoPoint start;
	/** Turn instruction to reach next segment. **/
	private String instruction;
	/** Length of segment. **/
	private int length;
	/** Distance covered. **/
	private double distance;

	
	public Segment() {
		
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
            //dest.writeInt(start.getLatitudeE6());
            //dest.writeInt(start.getLongitudeE6());
            dest.writeParcelable(start, 0);
            dest.writeDouble(distance);
    }
    
    /**
     * Rehydrate a segment from a parcel.
     * @param in The parcel to rehydrate.
     */
    
    public void readFromParcel(final Parcel in) {
            instruction = in.readString();
            length = in.readInt();
            //start = new GeoPoint(in.readInt(), in.readInt());
            start = in.readParcelable(GeoPoint.class.getClassLoader());
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
		copy.start = start;
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
