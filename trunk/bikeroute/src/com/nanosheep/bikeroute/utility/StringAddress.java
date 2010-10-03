/**
 * 
 */
package com.nanosheep.bikeroute.utility;

import android.location.Address;

/**
 * @author jono@nanosheep.net
 * @version Jul 2, 2010
 */
public final class StringAddress {
	
	private StringAddress () { }
	
	public static String asString(final Address address) {
		final StringBuffer sBuffer = new StringBuffer();
		final int top = address.getMaxAddressLineIndex() + 1;
		for (int i = 0; i < top; i++) {
			sBuffer.append(address.getAddressLine(i));
			if (i != top - 1) {
				sBuffer.append(", ");
			}
		}
		return sBuffer.toString();
	}

}
