package com.nanosheep.bikeroute;

/**
 * Utility class to provide degrees-microdegrees conversion.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public final class Degrees {
	public static final double CNV = 1E6;

	private Degrees() {
	}
	
	/**
	 * Convert degrees to microdegrees.
	 * @param degrees
	 * @return integer microdegrees.
	 */
	
	public static int asMicroDegrees(final double degrees) {
		return (int) (degrees * CNV);
	}
	
	/**
	 * Convert microdegrees to degrees.
	 * @param mDegrees
	 * @return double type degrees.
	 */
	
	public static double asDegrees(final int mDegrees) {
		return mDegrees / CNV;
	}

}
