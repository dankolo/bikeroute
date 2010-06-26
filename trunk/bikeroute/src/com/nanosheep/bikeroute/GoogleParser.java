package com.nanosheep.bikeroute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;

/**
 * Parse a google directions json object to a route.
 * 
 * @author jono@nanosheep.net
 * @version Jun 25, 2010
 */
public class GoogleParser extends XMLParser implements Parser {
	public GoogleParser(String feedUrl) {
		super(feedUrl);
	}
	
	public Route parse() {
		String result = convertStreamToString(this.getInputStream());
		Route route = new Route();
		Segment segment = new Segment();
		try {
			JSONObject json = new JSONObject(result);
			JSONObject r = json.getJSONArray("routes").getJSONObject(0);
			JSONObject leg = r.getJSONArray("legs").getJSONObject(0);
			JSONArray steps = leg.getJSONArray("steps");
			int numSteps = steps.length();
			route.setName(leg.getString("start_address") + " to " + leg.getString("end_address"));
			route.setCopyright(r.getString("copyrights"));
			if (!r.getJSONArray("warnings").isNull(0)) {
				route.setWarning(r.getJSONArray("warnings").getString(0));
			}
			
			for (int i = 0; i < numSteps; i++) {
				JSONObject j = steps.getJSONObject(i);
				JSONObject start = j.getJSONObject("start_location");
				GeoPoint p = new GeoPoint(Degrees.asMicroDegrees(start.getDouble("lat")), 
						Degrees.asMicroDegrees(start.getDouble("lng")));
				segment.setPoint(p);
				segment.setLength(j.getJSONObject("distance").getInt("value"));
				segment.setTurn(j.getString("html_instructions").replaceAll("<(.*?)*>", ""));
				JSONObject poly = j.getJSONObject("polyline");
				route.addPoints(decodePolyLine(poly.getString("points")));
				route.addSegment(segment.copy());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return route;
	}

	/**
	 * Convert an inputstream to a string.
	 * @param input inputstream to convert.
	 * @return a String of the inputstream.
	 */
	
	private static String convertStreamToString(InputStream input) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder sBuf = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sBuf.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sBuf.toString();
    }
	
	/**
	 * Decode a polyline string into a list of GeoPoints.
	 * @param poly polyline encoded string to decode.
	 * @return the list of GeoPoints represented by this polystring.
	 */
	
	private List<GeoPoint> decodePolyLine(final String poly) {
		int len = poly.length();
		int index = 0;
		List<GeoPoint> decoded = new ArrayList<GeoPoint>();
		int lat = 0;
		int lng = 0;

		while (index < len) {
		int b;
		int shift = 0;
		int result = 0;
		do {
			b = poly.charAt(index++) - 63;
			result |= (b & 0x1f) << shift;
			shift += 5;
		} while (b >= 0x20);
		int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
		lat += dlat;

		shift = 0;
		result = 0;
		do {
			b = poly.charAt(index++) - 63;
			result |= (b & 0x1f) << shift;
			shift += 5;
		} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

		decoded.add(new GeoPoint(
				Degrees.asMicroDegrees(lat / 1E5), Degrees.asMicroDegrees(lng / 1E5)));
		}

		return decoded;
		}
}
