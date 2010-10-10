/**
 * Modified to make parcelable & use GeoPoints from:
 * 
 * This file is part of the Java Machine Learning Library
 * 
 * The Java Machine Learning Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * The Java Machine Learning Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Java Machine Learning Library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Copyright (c) 2006-2009, Thomas Abeel
 * 
 * Project: http://java-ml.sourceforge.net/
 * 
 * 
 * based on work by Simon Levy
 * http://www.cs.wlu.edu/~levy/software/kd/
 */
package com.nanosheep.bikeroute.utility.kdtree;

import android.os.Parcel;
import android.os.Parcelable;

// Hyper-Point class supporting KDTree class

class HPoint implements Parcelable {

    protected double[] coord;

    protected HPoint(int n) {
        coord = new double[n];
    }

    protected HPoint(double[] x) {

        coord = new double[x.length];
        for (int i = 0; i < x.length; ++i)
            coord[i] = x[i];
    }

    protected Object clone() {
        return new HPoint(coord);
    }

    protected boolean equals(HPoint p) {

        // seems faster than java.util.Arrays.equals(), which is not
        // currently supported by Matlab anyway
        for (int i = 0; i < coord.length; ++i)
            if (coord[i] != p.coord[i])
                return false;

        return true;
    }

    protected static double sqrdist(HPoint x, HPoint y) {

        double dist = 0;

        for (int i = 0; i < x.coord.length; ++i) {
            double diff = (x.coord[i] - y.coord[i]);
            dist += (diff * diff);
        }

        return dist;

    }

    protected static double eucdist(HPoint x, HPoint y) {
        return Math.sqrt(sqrdist(x, y));
    }

    public String toString() {
        String s = "";
        for (int i = 0; i < coord.length; ++i) {
            s = s + coord[i] + " ";
        }
        return s;
    }
    
    public HPoint(Parcel in) {
    	readFromParcel(in);
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
		dest.writeDoubleArray(coord);
	}
	
	/**
     * Rehydrate a segment from a parcel.
     * @param in The parcel to rehydrate.
     */
    
    public void readFromParcel(final Parcel in) {
            coord = in.createDoubleArray();
    }
    
    public static final Parcelable.Creator CREATOR =
        new Parcelable.Creator() {
            public HPoint createFromParcel(final Parcel in) {
                return new HPoint(in);
            }

                        @Override
                        public HPoint[] newArray(final int size) {
                                return new HPoint[size];
                        }
        };

}
